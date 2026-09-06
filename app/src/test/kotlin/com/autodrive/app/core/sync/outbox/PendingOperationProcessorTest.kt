package com.autodrive.app.core.sync.outbox

import com.autodrive.app.core.database.dao.PendingOperationDao
import com.autodrive.app.core.database.entities.PendingOperationEntity
import com.autodrive.app.core.sync.data.SyncScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingOperationProcessorTest {
    private val scope = SyncScope("user-a", "client-a", "org-a")

    @Test
    fun success_isFinalizedOnlyAfterRemoteConfirmation() = runTest {
        val dao = FakePendingOperationDao(pending("op-1"))
        var observedStatusDuringSend: String? = null
        val processor = processor(dao) { operation ->
            observedStatusDuringSend = dao.row(operation.id)?.status
            receipt(operation)
        }

        val summary = processor.flush(scope)

        assertEquals(PendingOperationStatus.IN_PROGRESS, observedStatusDuringSend)
        assertNull(dao.row("op-1"))
        assertEquals(1, summary.succeeded)
    }

    @Test
    fun transientFailure_remainsPendingWithScheduledRetry_andClearedLease() = runTest {
        val dao = FakePendingOperationDao(pending("op-1"))
        val processor = processor(dao) { throw IllegalStateException("network timeout") }

        val summary = processor.flush(scope)
        val stored = requireNotNull(dao.row("op-1"))

        assertEquals(PendingOperationStatus.PENDING, stored.status)
        assertEquals(1, stored.attemptCount)
        assertEquals(NOW + 1_000L, stored.nextRetryAt)
        assertEquals(0L, stored.leaseUntil)
        assertEquals(1, summary.scheduledForRetry)
    }

    @Test
    fun expiredClaim_isRecoveredWithoutOverwritingRetrySchedule() = runTest {
        val claimed = pending("op-1").copy(
            status = PendingOperationStatus.IN_PROGRESS,
            nextRetryAt = 1234L,
            leaseUntil = NOW - 1,
        )
        val dao = FakePendingOperationDao(claimed)
        val processor = processor(dao) { operation -> receipt(operation) }

        val summary = processor.flush(scope)

        assertNull(dao.row("op-1"))
        assertEquals(1, summary.succeeded)
        assertEquals(1234L, dao.lastRecoveredRetrySchedule)
    }

    @Test
    fun scopeA_neverClaimsScopeB() = runTest {
        val foreign = pending("op-b").copy(userId = "user-b", clientId = "client-b", orgId = "org-b")
        val dao = FakePendingOperationDao(foreign)
        val processor = processor(dao) { operation -> receipt(operation) }

        val summary = processor.flush(scope)

        assertEquals(0, summary.examined)
        assertEquals(PendingOperationStatus.PENDING, dao.row("op-b")?.status)
    }

    @Test
    fun permanentFailure_movesDirectlyToDeadLetter() = runTest {
        val dao = FakePendingOperationDao(pending("op-1"))
        val diagnostics = mutableListOf<String>()
        val processor = processor(dao, diagnosticLog = { message, _ -> diagnostics += message }) {
            throw PermanentOutboxException("Unknown pending operation")
        }

        val summary = processor.flush(scope)
        val stored = requireNotNull(dao.row("op-1"))

        assertEquals(PendingOperationStatus.DEAD_LETTER, stored.status)
        assertEquals(Long.MAX_VALUE, stored.nextRetryAt)
        assertEquals(0L, stored.leaseUntil)
        assertEquals(1, summary.deadLettered)
        assertTrue(diagnostics.single().contains("operation=TEST"))
    }

    private fun receipt(operation: PendingOperationEntity) = OutboxDeliveryReceipt(
        mutationId = operation.mutationId,
        commandType = operation.operation,
        resultStatus = "APPLIED",
        serverEntityId = operation.entityId,
        serverRevision = 1L,
        revisionKind = "COMMAND_RECEIPT",
        replayed = false,
    )

    private fun processor(
        dao: FakePendingOperationDao,
        diagnosticLog: (String, Throwable?) -> Unit = { _, _ -> },
        sender: suspend (PendingOperationEntity) -> OutboxDeliveryReceipt,
    ) = PendingOperationProcessor(
        dao = dao,
        sender = PendingOperationSender(sender),
        finalizer = PendingOperationFinalizer { operation, _ ->
            check(
                dao.deleteClaimedById(
                    operation.id,
                    operation.userId,
                    operation.clientId,
                    operation.orgId,
                ) == 1
            )
        },
        retryPolicy = OutboxRetryPolicy(
            maxAttempts = 3,
            baseDelayMillis = 1_000L,
            maxDelayMillis = 8_000L,
            randomUnit = { 0.5 },
        ),
        clock = { NOW },
        diagnosticLog = diagnosticLog,
    )

    private fun pending(id: String) = PendingOperationEntity(
        id = id,
        mutationId = "mutation-$id",
        userId = scope.userId,
        clientId = scope.clientId,
        orgId = scope.orgId,
        entityType = "test",
        entityId = id,
        operation = "TEST",
        payload = "{}",
        createdAt = NOW - 10,
        nextRetryAt = 0,
        leaseUntil = 0,
    )

    private class FakePendingOperationDao(vararg initial: PendingOperationEntity) : PendingOperationDao {
        private val rows = linkedMapOf<String, PendingOperationEntity>().apply {
            initial.forEach { put(it.id, it) }
        }
        var lastRecoveredRetrySchedule: Long? = null

        fun row(id: String): PendingOperationEntity? = rows[id]

        override suspend fun insert(op: PendingOperationEntity) {
            require(rows.values.none {
                it.userId == op.userId && it.clientId == op.clientId && it.orgId == op.orgId &&
                    it.mutationId == op.mutationId && it.id != op.id
            })
            rows[op.id] = op
        }

        override suspend fun getDue(
            userId: String, clientId: String, orgId: String, now: Long, limit: Int,
        ): List<PendingOperationEntity> = rows.values
            .filter {
                it.userId == userId && it.clientId == clientId && it.orgId == orgId &&
                    it.status == PendingOperationStatus.PENDING && it.nextRetryAt <= now
            }
            .sortedBy { it.createdAt }
            .take(limit)

        override suspend fun claim(
            id: String, userId: String, clientId: String, orgId: String, now: Long, leaseUntil: Long,
        ): Int {
            val row = rows[id] ?: return 0
            if (
                row.userId != userId || row.clientId != clientId || row.orgId != orgId ||
                row.status != PendingOperationStatus.PENDING || row.nextRetryAt > now
            ) return 0
            rows[id] = row.copy(status = PendingOperationStatus.IN_PROGRESS, leaseUntil = leaseUntil)
            return 1
        }

        override suspend fun releaseExpiredClaims(userId: String, clientId: String, orgId: String, now: Long): Int {
            val expired = rows.values.filter {
                it.userId == userId && it.clientId == clientId && it.orgId == orgId &&
                    it.status == PendingOperationStatus.IN_PROGRESS && it.leaseUntil in 1..now
            }
            expired.forEach {
                lastRecoveredRetrySchedule = it.nextRetryAt
                rows[it.id] = it.copy(status = PendingOperationStatus.PENDING, leaseUntil = 0)
            }
            return expired.size
        }

        override suspend fun releaseClaim(
            id: String, userId: String, clientId: String, orgId: String, nextRetryAt: Long,
        ): Int {
            val row = rows[id] ?: return 0
            if (row.userId != userId || row.clientId != clientId || row.orgId != orgId || row.status != PendingOperationStatus.IN_PROGRESS) return 0
            rows[id] = row.copy(status = PendingOperationStatus.PENDING, nextRetryAt = nextRetryAt, leaseUntil = 0)
            return 1
        }

        override suspend fun deleteClaimedById(id: String, userId: String, clientId: String, orgId: String): Int {
            val row = rows[id] ?: return 0
            if (row.userId != userId || row.clientId != clientId || row.orgId != orgId || row.status != PendingOperationStatus.IN_PROGRESS) return 0
            rows.remove(id)
            return 1
        }

        override suspend fun recordFailure(
            id: String, userId: String, clientId: String, orgId: String,
            status: String, attemptCount: Int, nextRetryAt: Long, errorCode: String, errorMessage: String,
        ): Int {
            val row = rows[id] ?: return 0
            if (row.userId != userId || row.clientId != clientId || row.orgId != orgId || row.status != PendingOperationStatus.IN_PROGRESS) return 0
            rows[id] = row.copy(
                status = status,
                attemptCount = attemptCount,
                nextRetryAt = nextRetryAt,
                leaseUntil = 0,
                lastErrorCode = errorCode,
                lastErrorMessage = errorMessage,
            )
            return 1
        }

        override suspend fun getDeadLetters(userId: String, clientId: String, orgId: String, limit: Int) =
            rows.values.filter {
                it.userId == userId && it.clientId == clientId && it.orgId == orgId && it.status == PendingOperationStatus.DEAD_LETTER
            }.take(limit)

        override suspend fun countByStatus(userId: String, clientId: String, orgId: String, status: String) =
            rows.values.count { it.userId == userId && it.clientId == clientId && it.orgId == orgId && it.status == status }

        override suspend fun findActiveByMutationId(userId: String, clientId: String, orgId: String, mutationId: String) =
            rows.values.firstOrNull {
                it.userId == userId && it.clientId == clientId && it.orgId == orgId && it.mutationId == mutationId && it.active()
            }

        override suspend fun getChildrenByDependency(
            userId: String, clientId: String, orgId: String, mutationId: String,
        ) = rows.values.filter {
            it.userId == userId && it.clientId == clientId && it.orgId == orgId &&
                it.dependsOnMutationId == mutationId && it.active()
        }.sortedBy { it.createdAt }

        override suspend fun findActiveForEntity(
            userId: String, clientId: String, orgId: String, entityType: String, entityId: String, operation: String,
        ) = rows.values.firstOrNull {
            it.userId == userId && it.clientId == clientId && it.orgId == orgId &&
                it.entityType == entityType && it.entityId == entityId && it.operation == operation && it.active()
        }

        override suspend fun findAnyActiveForEntity(
            userId: String, clientId: String, orgId: String, entityType: String, entityId: String,
        ) = rows.values.firstOrNull {
            it.userId == userId && it.clientId == clientId && it.orgId == orgId &&
                it.entityType == entityType && it.entityId == entityId && it.active()
        }

        override suspend fun countOtherActiveForEntity(
            userId: String, clientId: String, orgId: String, entityType: String, entityId: String,
            operation: String, excludeId: String,
        ) = rows.values.count {
            it.id != excludeId && it.userId == userId && it.clientId == clientId && it.orgId == orgId &&
                it.entityType == entityType && it.entityId == entityId && it.operation == operation && it.active()
        }

        override suspend fun deleteByMutationId(userId: String, clientId: String, orgId: String, mutationId: String): Int {
            val ids = rows.values.filter {
                it.userId == userId && it.clientId == clientId && it.orgId == orgId && it.mutationId == mutationId
            }.map { it.id }
            ids.forEach(rows::remove)
            return ids.size
        }

        override suspend fun reactivate(id: String, userId: String, clientId: String, orgId: String): Int {
            val row = rows[id] ?: return 0
            if (row.userId != userId || row.clientId != clientId || row.orgId != orgId || row.status !in setOf(PendingOperationStatus.PENDING, PendingOperationStatus.DEAD_LETTER)) return 0
            rows[id] = row.copy(status = PendingOperationStatus.PENDING, attemptCount = 0, nextRetryAt = 0, leaseUntil = 0, lastErrorCode = null, lastErrorMessage = null)
            return 1
        }

        override suspend fun updatePayloadBeforeFirstAttempt(
            id: String, userId: String, clientId: String, orgId: String, payload: String,
        ): Int {
            val row = rows[id] ?: return 0
            if (row.userId != userId || row.clientId != clientId || row.orgId != orgId ||
                row.status != PendingOperationStatus.PENDING || row.attemptCount != 0
            ) return 0
            rows[id] = row.copy(payload = payload)
            return 1
        }

        override suspend fun deleteForScope(userId: String, clientId: String, orgId: String): Int {
            val ids = rows.values.filter { it.userId == userId && it.clientId == clientId && it.orgId == orgId }.map { it.id }
            ids.forEach(rows::remove)
            return ids.size
        }

        override suspend fun countActiveForScope(userId: String, clientId: String, orgId: String) =
            rows.values.count {
                it.userId == userId && it.clientId == clientId && it.orgId == orgId && it.active()
            }

        override suspend fun oldestActiveCreatedAt(userId: String, clientId: String, orgId: String) =
            rows.values.filter {
                it.userId == userId && it.clientId == clientId && it.orgId == orgId && it.active()
            }.minOfOrNull { it.createdAt }

        override suspend fun sumActiveAttemptCount(userId: String, clientId: String, orgId: String) =
            rows.values.filter {
                it.userId == userId && it.clientId == clientId && it.orgId == orgId && it.active()
            }.sumOf { it.attemptCount.toLong() }

        private fun PendingOperationEntity.active() = status in setOf(
            PendingOperationStatus.PENDING,
            PendingOperationStatus.IN_PROGRESS,
            PendingOperationStatus.DEAD_LETTER,
        )
    }

    private companion object {
        const val NOW = 10_000L
    }
}
