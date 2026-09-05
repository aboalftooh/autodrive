package com.autodrive.app.core.sync.data

import androidx.room.withTransaction
import com.autodrive.app.core.database.AutoDriveDatabase
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

class RebootstrapRequiredException : IllegalStateException("REBOOTSTRAP_REQUIRED")
class ReconciliationContractException(code: String) : IllegalStateException(code)

data class ReconciliationResult(val status: String, val repairedRows: Int = 0)

/**
 * Detects and repairs canonical projection drift against a server manifest at a stable revision.
 *
 * Reconciliation is deferred while local durable mutations are active or when cursor/head state is
 * unsafe. Repairs are exact-scope Room transactions and are rechecked against the same manifest
 * revision. A result that cannot be proven convergent escalates to rebootstrap instead of silently
 * accepting divergent local state.
 */
@Singleton
class AntiEntropyReconciler @Inject constructor(
    private val db: AutoDriveDatabase,
    private val source: ReconciliationManifestSource,
    private val digester: CanonicalProjectionDigester,
    private val applier: ChangeEventApplier,
    private val sessionReader: SessionReader,
    private val diagnostics: SyncDiagnostics,
    private val observabilityStore: SyncObservabilityStore,
    private val faultInjector: SyncFaultInjector,
) {
    private val intervalMs = 24L * 60L * 60L * 1000L

    suspend fun reconcileIfDue(
        scope: SyncScope,
        cursorRevision: Long,
        force: Boolean = false,
        context: SyncRunContext? = null,
    ): ReconciliationResult {
        val now = System.currentTimeMillis()
        val prior = db.syncReconciliationStateDao().get(scope.userId, scope.clientId, scope.orgId, UnifiedSyncContract.STREAM)
        if (!force && prior != null && prior.nextDueAtLocal > now) return record(scope, context, ReconciliationResult("NOT_DUE"))
        if (db.pendingOperationDao().countActiveForScope(scope.userId, scope.clientId, scope.orgId) > 0) {
            save(scope, cursorRevision, "RECONCILIATION_DEFERRED_PENDING_LOCAL", now + intervalMs / 8)
            return record(scope, context, ReconciliationResult("RECONCILIATION_DEFERRED_PENDING_LOCAL"))
        }

        val manifest = source.manifest(scope)
        faultInjector.hit(SyncFaultPoint.RECONCILE_AFTER_MANIFEST, FaultContext(syncRunId = context?.syncRunId, revision = manifest.manifestRevision))
        if (manifest.status != "OK") throw ReconciliationContractException("RECONCILIATION_${manifest.status}")
        if (manifest.contractVersion != UnifiedSyncContract.RECONCILIATION_CONTRACT_VERSION) throw ReconciliationContractException("RECONCILIATION_PROTOCOL_VERSION_UNSUPPORTED")
        if (manifest.manifestRevision != cursorRevision) {
            save(scope, cursorRevision, "RECONCILIATION_DEFERRED_CURSOR_NOT_AT_HEAD", 0L)
            return record(scope, context, ReconciliationResult("RECONCILIATION_DEFERRED_CURSOR_NOT_AT_HEAD"))
        }

        val local = digester.rows(scope)
        val localRowsByPartition = local.groupBy { it.entityType to it.partition }
        val localPartitions = localRowsByPartition
            .mapValues { (_, rows) -> ManifestPartitionDto(rows.first().entityType, rows.first().partition, rows.size, digester.partitionDigest(rows)) }
        val serverPartitions = manifest.partitions.associateBy { it.entityType to it.partition }
        val keys = localPartitions.keys + serverPartitions.keys
        val mismatches = keys.filter { key ->
            val l = localPartitions[key]; val s = serverPartitions[key]
            l?.count != s?.count || l?.digest != s?.digest
        }
        if (mismatches.isEmpty()) {
            save(scope, manifest.manifestRevision, "CLEAN", now + intervalMs)
            return record(scope, context, ReconciliationResult("CLEAN"))
        }

        var repaired = 0
        for ((entityType, partition) in mismatches.sortedWith(compareBy({ it.first }, { it.second }))) {
            val remote = source.partition(scope, manifest.manifestRevision, entityType, partition)
            if (remote.status == "STALE_MANIFEST") {
                save(scope, cursorRevision, "RECONCILIATION_STALE_MANIFEST", 0L)
                return record(scope, context, ReconciliationResult("RECONCILIATION_STALE_MANIFEST", repaired))
            }
            if (remote.status != "OK" || remote.contractVersion != UnifiedSyncContract.RECONCILIATION_CONTRACT_VERSION ||
                remote.manifestRevision != manifest.manifestRevision || remote.entityType != entityType || remote.partition != partition) {
                throw ReconciliationContractException("RECONCILIATION_PARTITION_INVALID")
            }
            val localIds = localRowsByPartition[entityType to partition].orEmpty().associateBy { it.entityId }
            val remoteIds = remote.rows.associateBy { it.entityId }
            db.withTransaction {
                requireCurrentScope(scope)
                for (row in remote.rows) {
                    val existing = localIds[row.entityId]
                    if (existing == null || existing.digest != row.digest) {
                        applier.applyBootstrapRow(scope, entityType, row.entityId, row.payload)
                        repaired += 1
                    }
                }
                for (extraId in localIds.keys - remoteIds.keys) {
                    applier.applyDelete(scope, entityType, extraId)
                    repaired += 1
                }
            }
        }

        faultInjector.hit(SyncFaultPoint.RECONCILE_AFTER_TARGETED_REPAIR_BEFORE_RECHECK, FaultContext(syncRunId = context?.syncRunId, revision = manifest.manifestRevision))
        val verifyManifest = source.manifest(scope)
        if (verifyManifest.status != "OK" || verifyManifest.manifestRevision != manifest.manifestRevision) {
            save(scope, cursorRevision, "RECONCILIATION_STALE_AFTER_REPAIR", 0L)
            return record(scope, context, ReconciliationResult("RECONCILIATION_STALE_AFTER_REPAIR", repaired))
        }
        val after = digester.rows(scope).groupBy { it.entityType to it.partition }
            .mapValues { (_, rows) -> ManifestPartitionDto(rows.first().entityType, rows.first().partition, rows.size, digester.partitionDigest(rows)) }
        val expected = verifyManifest.partitions.associateBy { it.entityType to it.partition }
        val verifyKeys = after.keys + expected.keys
        if (verifyKeys.any { k -> after[k]?.count != expected[k]?.count || after[k]?.digest != expected[k]?.digest }) {
            save(scope, cursorRevision, "REBOOTSTRAP_REQUIRED", 0L)
            record(scope, context, ReconciliationResult("REBOOTSTRAP_REQUIRED", repaired), rebootstrap = true)
            throw RebootstrapRequiredException()
        }
        save(scope, manifest.manifestRevision, "REPAIRED", now + intervalMs)
        return record(scope, context, ReconciliationResult("REPAIRED", repaired))
    }

    private suspend fun record(
        scope: SyncScope,
        context: SyncRunContext?,
        result: ReconciliationResult,
        rebootstrap: Boolean = false,
    ): ReconciliationResult {
        diagnostics.reconciliation(context, result.status, result.repairedRows)
        runCatching { observabilityStore.reconciliation(scope, result.status, result.repairedRows, rebootstrap) }
        return result
    }

    private suspend fun save(scope: SyncScope, revision: Long, result: String, nextDue: Long) {
        val now = System.currentTimeMillis()
        db.syncReconciliationStateDao().upsert(
            SyncReconciliationStateEntity(
                scope.userId, scope.clientId, scope.orgId, UnifiedSyncContract.STREAM,
                revision.toString(), result, UnifiedSyncContract.RECONCILIATION_CONTRACT_VERSION,
                nextDue, now,
            ),
        )
    }

    private fun requireCurrentScope(scope: SyncScope) {
        if (SyncScope.from(sessionReader.currentSession()) != scope) throw ReconciliationContractException("STALE_SYNC_SCOPE")
    }
}
