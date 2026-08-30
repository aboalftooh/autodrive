package com.autodrive.app.core.sync.data

import androidx.room.withTransaction
import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.database.entities.SyncBootstrapStagingEntity
import com.autodrive.app.core.database.entities.SyncBootstrapStateEntity
import com.autodrive.app.core.database.entities.SyncCursorEntity
import com.autodrive.app.core.database.entities.SyncReconciliationStateEntity
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.sync.diagnostics.SyncDiagnostics
import com.autodrive.app.core.sync.diagnostics.SyncObservabilityStore
import com.autodrive.app.core.sync.diagnostics.SyncRunContext
import com.autodrive.app.core.sync.fault.FaultContext
import com.autodrive.app.core.sync.fault.SyncFaultInjector
import com.autodrive.app.core.sync.fault.SyncFaultPoint
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class BootstrapExpiredException : IllegalStateException("BOOTSTRAP_EXPIRED")
class BootstrapContractException(code: String) : IllegalStateException(code)

data class BootstrapResult(val baselineRevision: Long, val stagedRows: Int)

/**
 * Establishes a canonical cursor through a restartable, durably staged server snapshot.
 *
 * Pages are persisted under the exact [SyncScope] and bootstrap id. Only a complete
 * `READY_TO_INSTALL` snapshot is transactionally applied, stale local rows removed against that
 * complete inventory, and the cursor set to the server baseline. Scope is rechecked before sensitive
 * commits; expiration clears staged state and is surfaced instead of installing partial data.
 */
@Singleton
class SafeBootstrapSynchronizer @Inject constructor(
    private val db: AutoDriveDatabase,
    private val source: BootstrapSnapshotSource,
    private val applier: ChangeEventApplier,
    private val sessionReader: SessionReader,
    private val diagnostics: SyncDiagnostics,
    private val observabilityStore: SyncObservabilityStore,
    private val faultInjector: SyncFaultInjector,
) {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    suspend fun ensureCanonicalCursor(scope: SyncScope, context: SyncRunContext? = null): BootstrapResult? {
        val cursor = db.syncCursorDao().get(scope.userId, scope.clientId, scope.orgId, UnifiedSyncContract.STREAM)
        if (cursor != null && cursor.contractVersion == UnifiedSyncContract.CONTRACT_VERSION && cursor.cursorToken.toLongOrNull() != null) return null
        return bootstrap(scope, context)
    }

    suspend fun bootstrap(scope: SyncScope, context: SyncRunContext? = null): BootstrapResult {
        val startedAt = System.nanoTime()
        val persistedState = db.syncBootstrapDao().getState(scope.userId, scope.clientId, scope.orgId, UnifiedSyncContract.STREAM)
        val state: SyncBootstrapStateEntity
        if (persistedState == null || persistedState.contractVersion != UnifiedSyncContract.CONTRACT_VERSION) {
            db.withTransaction {
                requireCurrentScope(scope)
                db.syncBootstrapDao().deleteStagingForScope(scope.userId, scope.clientId, scope.orgId)
                db.syncBootstrapDao().deleteStateForScope(scope.userId, scope.clientId, scope.orgId)
            }
            state = begin(scope)
            faultInjector.hit(SyncFaultPoint.BOOTSTRAP_AFTER_BEGIN, FaultContext(syncRunId = context?.syncRunId, revision = state.baselineRevision.toLongOrNull()))
        } else {
            state = persistedState
        }

        var currentState = state
        while (currentState.status == "DOWNLOADING") {
            val page = source.page(scope, currentState.bootstrapId, currentState.nextPageToken, UnifiedSyncContract.BOOTSTRAP_PAGE_SIZE)
            if (page.status == "BOOTSTRAP_EXPIRED") {
                db.withTransaction {
                    requireCurrentScope(scope)
                    db.syncBootstrapDao().deleteStagingForScope(scope.userId, scope.clientId, scope.orgId)
                    db.syncBootstrapDao().deleteStateForScope(scope.userId, scope.clientId, scope.orgId)
                }
                diagnostics.bootstrap(context, "BOOTSTRAP_EXPIRED", elapsedMs(startedAt), 0, null)
                throw BootstrapExpiredException()
            }
            validatePage(currentState, page)
            val rows = page.rows.map { row ->
                if (row.entityType !in applier.supportedEntityTypes || row.entityId.isBlank()) throw BootstrapContractException("BOOTSTRAP_PAYLOAD_INVALID")
                SyncBootstrapStagingEntity(
                    userId = scope.userId, clientId = scope.clientId, orgId = scope.orgId,
                    bootstrapId = currentState.bootstrapId, entityType = row.entityType, entityId = row.entityId,
                    canonicalPayload = row.payload.toString(), canonicalDigest = row.digest,
                )
            }
            val now = System.currentTimeMillis()
            val next = currentState.copy(
                status = if (page.hasMore) "DOWNLOADING" else "READY_TO_INSTALL",
                nextPageToken = page.nextPageToken,
                updatedAtLocal = now,
            )
            db.withTransaction {
                requireCurrentScope(scope)
                if (rows.isNotEmpty()) db.syncBootstrapDao().upsertStaging(rows)
                db.syncBootstrapDao().upsertState(next)
            }
            faultInjector.hit(SyncFaultPoint.BOOTSTRAP_AFTER_STAGE_PAGE_COMMIT, FaultContext(syncRunId = context?.syncRunId, revision = next.baselineRevision.toLongOrNull()))
            currentState = next
        }

        if (currentState.status != "READY_TO_INSTALL") throw BootstrapContractException("BOOTSTRAP_STATE_INVALID")
        val staged = db.syncBootstrapDao().getStaging(scope.userId, scope.clientId, scope.orgId, currentState.bootstrapId)
        val ids = staged.groupBy { it.entityType }.mapValues { (_, rows) -> rows.mapTo(linkedSetOf()) { it.entityId } }
        val baseline = currentState.baselineRevision.toLongOrNull()?.takeIf { it >= 0 } ?: throw BootstrapContractException("REVISION_OUT_OF_RANGE")
        faultInjector.hit(SyncFaultPoint.BOOTSTRAP_BEFORE_INSTALL_COMMIT, FaultContext(syncRunId = context?.syncRunId, revision = baseline))
        db.withTransaction {
            requireCurrentScope(scope)
            for (row in staged) {
                val payload = json.parseToJsonElement(row.canonicalPayload) as? JsonObject ?: throw BootstrapContractException("BOOTSTRAP_PAYLOAD_INVALID")
                applier.applyBootstrapRow(scope, row.entityType, row.entityId, payload)
            }
            applier.removeBootstrapStaleRows(scope, ids)
            db.syncCursorDao().upsert(
                SyncCursorEntity(
                    scope.userId, scope.clientId, scope.orgId, UnifiedSyncContract.STREAM,
                    baseline.toString(), UnifiedSyncContract.CONTRACT_VERSION, System.currentTimeMillis(),
                ),
            )
            val now = System.currentTimeMillis()
            db.syncReconciliationStateDao().upsert(
                SyncReconciliationStateEntity(
                    scope.userId, scope.clientId, scope.orgId, UnifiedSyncContract.STREAM,
                    lastCheckedRevision = null, lastResult = "BOOTSTRAP_INSTALLED",
                    contractVersion = UnifiedSyncContract.RECONCILIATION_CONTRACT_VERSION,
                    nextDueAtLocal = 0L, updatedAtLocal = now,
                ),
            )
            db.syncBootstrapDao().deleteStaging(scope.userId, scope.clientId, scope.orgId, currentState.bootstrapId)
            db.syncBootstrapDao().deleteStateForScope(scope.userId, scope.clientId, scope.orgId)
        }
        faultInjector.hit(SyncFaultPoint.BOOTSTRAP_AFTER_INSTALL_BEFORE_DELTA_RESUME, FaultContext(syncRunId = context?.syncRunId, revision = baseline))
        val duration = elapsedMs(startedAt)
        diagnostics.bootstrap(context, "COMPLETED", duration, staged.size, baseline)
        runCatching { observabilityStore.bootstrapCompleted(scope, duration) }
        return BootstrapResult(baseline, staged.size)
    }

    private suspend fun begin(scope: SyncScope): SyncBootstrapStateEntity {
        val begin = source.begin(scope)
        if (begin.status != "OK") throw BootstrapContractException("BOOTSTRAP_BEGIN_${begin.status}")
        if (begin.contractVersion != UnifiedSyncContract.CONTRACT_VERSION) throw BootstrapContractException("BOOTSTRAP_PROTOCOL_VERSION_UNSUPPORTED")
        if (begin.bootstrapId.isBlank() || begin.baselineRevision < 0L) throw BootstrapContractException("BOOTSTRAP_BEGIN_INVALID")
        val now = System.currentTimeMillis()
        val state = SyncBootstrapStateEntity(
            userId = scope.userId, clientId = scope.clientId, orgId = scope.orgId,
            stream = UnifiedSyncContract.STREAM, bootstrapId = begin.bootstrapId,
            baselineRevision = begin.baselineRevision.toString(), status = "DOWNLOADING",
            contractVersion = begin.contractVersion, nextPageToken = null,
            startedAtLocal = now, updatedAtLocal = now,
        )
        db.withTransaction {
            requireCurrentScope(scope)
            db.syncBootstrapDao().upsertState(state)
        }
        return state
    }

    private fun validatePage(state: SyncBootstrapStateEntity, page: BootstrapPageDto) {
        if (page.status != "OK") throw BootstrapContractException("BOOTSTRAP_PAGE_${page.status}")
        if (page.contractVersion != UnifiedSyncContract.CONTRACT_VERSION) throw BootstrapContractException("BOOTSTRAP_PROTOCOL_VERSION_UNSUPPORTED")
        if (page.bootstrapId != state.bootstrapId) throw BootstrapContractException("BOOTSTRAP_ID_MISMATCH")
        if (page.hasMore && page.nextPageToken.isNullOrBlank()) throw BootstrapContractException("BOOTSTRAP_PAGE_TOKEN_MISSING")
    }

    private fun requireCurrentScope(scope: SyncScope) {
        if (SyncScope.from(sessionReader.currentSession()) != scope) throw BootstrapContractException("STALE_SYNC_SCOPE")
    }

    private fun elapsedMs(started: Long) = ((System.nanoTime() - started) / 1_000_000L).coerceAtLeast(0L)
}
