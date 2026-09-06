package com.autodrive.app.core.database.dao
import com.autodrive.app.core.database.entities.PendingOperationEntity
interface PendingOperationDao {
    suspend fun insert(op: PendingOperationEntity)
    suspend fun getDue(now: Long, limit: Int): List<PendingOperationEntity>
    suspend fun claim(id: String, now: Long, leaseUntil: Long): Int
    suspend fun releaseExpiredClaims(now: Long): Int
    suspend fun releaseClaim(id: String, nextRetryAt: Long): Int
    suspend fun markSucceeded(id: String)
    suspend fun deleteSucceededById(id: String)
    suspend fun recordFailure(id: String, status: String, attemptCount: Int, nextRetryAt: Long, errorCode: String, errorMessage: String)
    suspend fun getDeadLetters(limit: Int): List<PendingOperationEntity>
    suspend fun countByStatus(status: String): Int
    suspend fun deleteByIdempotencyKey(idempotencyKey: String)
    suspend fun deleteAll()
}
