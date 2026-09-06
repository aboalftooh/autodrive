package com.autodrive.app.core.sync.diagnostics

import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.database.entities.SyncObservabilityStateEntity
import com.autodrive.app.core.sync.data.SyncScope
import com.autodrive.app.core.sync.data.UnifiedChangeResult
import com.autodrive.app.core.sync.data.UnifiedSyncContract
import com.autodrive.app.core.sync.domain.SyncFailure
import com.autodrive.app.core.sync.domain.SyncStatus
import com.autodrive.app.core.sync.outbox.PendingOperationStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Best-effort diagnostic persistence. Callers must wrap failures so telemetry cannot become a
 * correctness dependency. Updates are serialized only to avoid losing diagnostic counters.
 */
@Singleton
class SyncObservabilityStore @Inject constructor(
    private val db: AutoDriveDatabase,
    private val fingerprintProvider: ScopeFingerprintProvider,
) {
    private val mutex = Mutex()

    suspend fun runStarted(scope: SyncScope, context: SyncRunContext) = mutate(scope) { prior, now ->
        prior.copy(
            lastSyncRunId = context.syncRunId,
            lastSyncStatus = "RUNNING",
            lastSyncStartedAtLocal = context.startedAtLocal,
            lastSyncCompletedAtLocal = null,
            lastFailurePhase = null,
            lastFailureCode = null,
            updatedAtLocal = now,
        )
    }

    suspend fun runFinished(scope: SyncScope, status: SyncStatus, failures: List<SyncFailure>) = mutate(scope) { prior, now ->
        val last = failures.lastOrNull()
        prior.copy(
            lastSyncStatus = status.name,
            lastSyncCompletedAtLocal = now,
            lastSuccessAtLocal = if (status == SyncStatus.SUCCESS) now else prior.lastSuccessAtLocal,
            lastFailurePhase = last?.phase?.name,
            lastFailureCode = stableFailureCode(last?.message),
            updatedAtLocal = now,
        )
    }

    suspend fun hintAccepted(scope: SyncScope, trailing: Boolean) = mutate(scope) { prior, now ->
        prior.copy(
            hintReceivedCount = prior.hintReceivedCount + 1,
            hintTrailingRunCount = prior.hintTrailingRunCount + if (trailing) 1 else 0,
            updatedAtLocal = now,
        )
    }

    suspend fun cursorExpired(scope: SyncScope) = mutate(scope) { prior, now ->
        prior.copy(cursorExpiryCount = prior.cursorExpiryCount + 1, updatedAtLocal = now)
    }

    suspend fun feedObserved(scope: SyncScope, result: UnifiedChangeResult) = mutate(scope) { prior, now ->
        prior.copy(
            lastLocalCursorRevision = result.cursorRevision.toString(),
            lastServerHeadRevision = result.headRevision.toString(),
            lastServerHeadObservedAt = now,
            updatedAtLocal = now,
        )
    }

    suspend fun bootstrapCompleted(scope: SyncScope, durationMs: Long) = mutate(scope) { prior, now ->
        prior.copy(
            lastSuccessfulBootstrapAt = now,
            lastBootstrapDurationMs = durationMs.coerceAtLeast(0L),
            bootstrapCount = prior.bootstrapCount + 1,
            updatedAtLocal = now,
        )
    }

    suspend fun reconciliation(scope: SyncScope, result: String, repairedRows: Int, rebootstrap: Boolean = false) = mutate(scope) { prior, now ->
        val mismatch = result == "REPAIRED" || result == "REBOOTSTRAP_REQUIRED" || repairedRows > 0
        prior.copy(
            lastReconciliationAt = now,
            lastReconciliationResult = result,
            reconciliationMismatchCount = prior.reconciliationMismatchCount + if (mismatch) 1 else 0,
            reconciliationRepairCount = prior.reconciliationRepairCount + if (repairedRows > 0) 1 else 0,
            rebootstrapCount = prior.rebootstrapCount + if (rebootstrap) 1 else 0,
            updatedAtLocal = now,
        )
    }

    suspend fun outboxConflicts(scope: SyncScope, count: Int) {
        if (count <= 0) return
        mutate(scope) { prior, now ->
            prior.copy(outboxConflictCount = prior.outboxConflictCount + count, updatedAtLocal = now)
        }
    }

    suspend fun realtime(scope: SyncScope, state: String, failedParticipants: Int?) = mutate(scope) { prior, now ->
        prior.copy(
            lastRealtimeState = state,
            lastRealtimeStateAt = now,
            lastFailedRealtimeParticipants = failedParticipants,
            updatedAtLocal = now,
        )
    }

    suspend fun snapshot(scope: SyncScope, now: Long = System.currentTimeMillis()): SyncHealthSnapshot {
        val row = db.syncObservabilityDao().get(scope.userId, scope.clientId, scope.orgId, UnifiedSyncContract.STREAM)
        val dao = db.pendingOperationDao()
        val pending = dao.countByStatus(scope.userId, scope.clientId, scope.orgId, PendingOperationStatus.PENDING)
        val dead = dao.countByStatus(scope.userId, scope.clientId, scope.orgId, PendingOperationStatus.DEAD_LETTER)
        val oldest = dao.oldestActiveCreatedAt(scope.userId, scope.clientId, scope.orgId)
        val retries = dao.sumActiveAttemptCount(scope.userId, scope.clientId, scope.orgId)
        val local = row?.lastLocalCursorRevision?.toLongOrNull()
            ?: db.syncCursorDao().get(scope.userId, scope.clientId, scope.orgId, UnifiedSyncContract.STREAM)?.cursorToken?.toLongOrNull()
        val head = row?.lastServerHeadRevision?.toLongOrNull()
        val lag = if (local != null && head != null) (head - local).coerceAtLeast(0L) else null
        return SyncHealthSnapshot(
            scopeFingerprint = fingerprintProvider.fingerprint(scope),
            syncStatus = row?.lastSyncStatus,
            localCursorRevision = local,
            serverHeadRevision = head,
            revisionLag = lag,
            serverHeadObservedAt = row?.lastServerHeadObservedAt,
            pendingCount = pending,
            oldestOutboxAgeMs = oldest?.let { (now - it).coerceAtLeast(0L) } ?: 0L,
            retryCount = retries,
            deadLetterCount = dead,
            conflictCount = row?.outboxConflictCount ?: 0,
            failedRealtimeParticipants = row?.lastFailedRealtimeParticipants,
            lastSuccessfulBootstrapAt = row?.lastSuccessfulBootstrapAt,
            lastReconciliationAt = row?.lastReconciliationAt,
            lastReconciliationResult = row?.lastReconciliationResult,
            cursorExpiryCount = row?.cursorExpiryCount ?: 0,
            hintReceivedCount = row?.hintReceivedCount ?: 0,
            hintTrailingRunCount = row?.hintTrailingRunCount ?: 0,
            hintDroppedCount = row?.hintDroppedCount ?: 0,
            lastFailurePhase = row?.lastFailurePhase,
            lastFailureCode = row?.lastFailureCode,
            lastRealtimeState = row?.lastRealtimeState,
            lastRealtimeStateAt = row?.lastRealtimeStateAt,
            freshness = if (row?.lastServerHeadObservedAt == null) "UNKNOWN_REMOTE_HEAD" else "OBSERVED_AT:${row.lastServerHeadObservedAt}",
        )
    }

    private fun stableFailureCode(value: String?): String? = value?.takeIf {
        it.matches(Regex("[A-Z0-9_:-]{2,80}"))
    } ?: value?.let { "UNKNOWN" }

    private suspend fun mutate(
        scope: SyncScope,
        block: (SyncObservabilityStateEntity, Long) -> SyncObservabilityStateEntity,
    ) = mutex.withLock {
        val now = System.currentTimeMillis()
        val prior = db.syncObservabilityDao().get(scope.userId, scope.clientId, scope.orgId, UnifiedSyncContract.STREAM)
            ?: SyncObservabilityStateEntity(
                userId = scope.userId,
                clientId = scope.clientId,
                orgId = scope.orgId,
                stream = UnifiedSyncContract.STREAM,
                contractVersion = OBSERVABILITY_CONTRACT_VERSION,
                updatedAtLocal = now,
            )
        db.syncObservabilityDao().upsert(block(prior, now))
    }

    companion object { const val OBSERVABILITY_CONTRACT_VERSION = 1 }
}
