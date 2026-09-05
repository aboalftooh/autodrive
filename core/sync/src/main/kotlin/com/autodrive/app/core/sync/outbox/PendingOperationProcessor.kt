package com.autodrive.app.core.sync.outbox

import com.autodrive.app.core.database.dao.PendingOperationDao
import com.autodrive.app.core.database.entities.PendingOperationEntity
import com.autodrive.app.core.sync.data.SyncScope
import com.autodrive.app.core.sync.fault.FaultContext
import com.autodrive.app.core.sync.fault.NoOpSyncFaultInjector
import com.autodrive.app.core.sync.fault.SyncFaultInjector
import com.autodrive.app.core.sync.fault.SyncFaultPoint
import com.autodrive.app.core.sync.diagnostics.SyncRunContext
import kotlin.coroutines.cancellation.CancellationException

/** Result of a network delivery. It contains only reconciliation metadata, never raw payloads. */
data class OutboxDeliveryReceipt(
    val mutationId: String,
    val commandType: String,
    val resultStatus: String,
    val serverEntityId: String? = null,
    val serverRevision: Long,
    val revisionKind: String,
    val replayed: Boolean = false,
    val errorCode: String? = null,
    val serverCreatedAt: Long? = null,
    val resultCount: Int? = null,
)

fun interface PendingOperationSender {
    suspend fun send(operation: PendingOperationEntity): OutboxDeliveryReceipt
}

fun interface PendingOperationFinalizer {
    suspend fun finalize(operation: PendingOperationEntity, receipt: OutboxDeliveryReceipt)
}

data class OutboxFlushSummary(
    val examined: Int,
    val succeeded: Int,
    val scheduledForRetry: Int,
    val deadLettered: Int,
    val conflicts: Int = 0,
)

class PendingOperationProcessor(
    private val dao: PendingOperationDao,
    private val sender: PendingOperationSender,
    private val finalizer: PendingOperationFinalizer,
    private val retryPolicy: OutboxRetryPolicy = OutboxRetryPolicy(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val diagnosticLog: (String, Throwable?) -> Unit = { _, _ -> },
    private val faultInjector: SyncFaultInjector = NoOpSyncFaultInjector(),
    private val operationDiagnostic: (PendingOperationEntity, String, String?, String?) -> Unit = { _, _, _, _ -> },
) {
    suspend fun flush(
        scope: SyncScope,
        limit: Int = DEFAULT_BATCH_SIZE,
        recoverExpiredClaims: Boolean = true,
        context: SyncRunContext? = null,
    ): OutboxFlushSummary {
        val now = clock()
        if (recoverExpiredClaims) {
            dao.releaseExpiredClaims(scope.userId, scope.clientId, scope.orgId, now)
        }
        val due = dao.getDue(scope.userId, scope.clientId, scope.orgId, now, limit)

        var succeeded = 0
        var scheduledForRetry = 0
        var deadLettered = 0
        var conflicts = 0

        due.forEach { operation ->
            if (!operation.belongsTo(scope)) return@forEach
            val leaseUntil = clock() + CLAIM_LEASE_MILLIS
            val claimed = dao.claim(
                id = operation.id,
                userId = scope.userId,
                clientId = scope.clientId,
                orgId = scope.orgId,
                now = clock(),
                leaseUntil = leaseUntil,
            )
            if (claimed != 1) return@forEach
            faultInjector.hit(SyncFaultPoint.WORKER_AFTER_LEASE_CLAIM, FaultContext(syncRunId = context?.syncRunId, mutationId = operation.mutationId))
            faultInjector.hit(SyncFaultPoint.OUTBOX_AFTER_LOCAL_COMMIT_BEFORE_SEND, FaultContext(syncRunId = context?.syncRunId, mutationId = operation.mutationId))

            try {
                val receipt = sender.send(operation)
                faultInjector.hit(SyncFaultPoint.OUTBOX_BEFORE_FINALIZE_LOCAL_RECEIPT, FaultContext(syncRunId = context?.syncRunId, mutationId = operation.mutationId, revision = receipt.serverRevision))
                finalizer.finalize(operation, receipt)
                operationDiagnostic(operation, "SUCCEEDED", null, null)
                succeeded += 1
            } catch (cancelled: CancellationException) {
                dao.releaseClaim(
                    id = operation.id,
                    userId = scope.userId,
                    clientId = scope.clientId,
                    orgId = scope.orgId,
                    nextRetryAt = clock(),
                )
                throw cancelled
            } catch (error: Throwable) {
                val decision = retryPolicy.onFailure(
                    currentAttemptCount = operation.attemptCount,
                    nowMillis = clock(),
                    error = error,
                )
                dao.recordFailure(
                    id = operation.id,
                    userId = scope.userId,
                    clientId = scope.clientId,
                    orgId = scope.orgId,
                    status = decision.status,
                    attemptCount = decision.attemptCount,
                    nextRetryAt = decision.nextRetryAt,
                    errorCode = decision.errorCode,
                    errorMessage = decision.errorMessage,
                )

                operationDiagnostic(operation, decision.status, decision.category.name, decision.errorCode)
                if (decision.category == OutboxFailureCategory.CONFLICT) conflicts += 1
                if (decision.status == PendingOperationStatus.DEAD_LETTER) {
                    deadLettered += 1
                    diagnosticLog(
                        "Outbox operation moved to DEAD_LETTER: operation=${operation.operation}, code=${decision.errorCode}",
                        error,
                    )
                } else {
                    scheduledForRetry += 1
                }
            }
        }

        return OutboxFlushSummary(
            examined = due.size,
            succeeded = succeeded,
            scheduledForRetry = scheduledForRetry,
            deadLettered = deadLettered,
            conflicts = conflicts,
        )
    }

    private fun PendingOperationEntity.belongsTo(scope: SyncScope): Boolean =
        userId == scope.userId && clientId == scope.clientId && orgId == scope.orgId

    companion object {
        const val DEFAULT_BATCH_SIZE = 50
        const val CLAIM_LEASE_MILLIS = 5L * 60L * 1_000L
    }
}
