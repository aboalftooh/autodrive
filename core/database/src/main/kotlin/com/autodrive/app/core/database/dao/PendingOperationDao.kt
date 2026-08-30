package com.autodrive.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.autodrive.app.core.database.entities.PendingOperationEntity

@Dao
interface PendingOperationDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(op: PendingOperationEntity)

    @Query(
        """
        SELECT * FROM pending_operations
        WHERE user_id = :userId
          AND client_id = :clientId
          AND org_id = :orgId
          AND status = 'PENDING'
          AND next_retry_at <= :now
          AND (depends_on_mutation_id IS NULL OR NOT EXISTS (
              SELECT 1 FROM pending_operations parent
              WHERE parent.user_id = pending_operations.user_id
                AND parent.client_id = pending_operations.client_id
                AND parent.org_id = pending_operations.org_id
                AND parent.mutation_id = pending_operations.depends_on_mutation_id
          ))
          AND (operation != 'SEND_CHAT_MESSAGE' OR NOT EXISTS (
              SELECT 1 FROM chat_media_transfers transfer
              WHERE transfer.user_id = pending_operations.user_id
                AND transfer.client_id = pending_operations.client_id
                AND transfer.org_id = pending_operations.org_id
                AND transfer.message_id = pending_operations.entity_id
                AND transfer.status != 'COMPLETE'
          ))
        ORDER BY created_at ASC
        LIMIT :limit
        """,
    )
    suspend fun getDue(
        userId: String,
        clientId: String,
        orgId: String,
        now: Long,
        limit: Int,
    ): List<PendingOperationEntity>

    @Query(
        """
        UPDATE pending_operations
        SET status = 'IN_PROGRESS', lease_until = :leaseUntil
        WHERE id = :id
          AND user_id = :userId
          AND client_id = :clientId
          AND org_id = :orgId
          AND status = 'PENDING'
          AND next_retry_at <= :now
          AND (depends_on_mutation_id IS NULL OR NOT EXISTS (
              SELECT 1 FROM pending_operations parent
              WHERE parent.user_id = pending_operations.user_id
                AND parent.client_id = pending_operations.client_id
                AND parent.org_id = pending_operations.org_id
                AND parent.mutation_id = pending_operations.depends_on_mutation_id
          ))
          AND (operation != 'SEND_CHAT_MESSAGE' OR NOT EXISTS (
              SELECT 1 FROM chat_media_transfers transfer
              WHERE transfer.user_id = pending_operations.user_id
                AND transfer.client_id = pending_operations.client_id
                AND transfer.org_id = pending_operations.org_id
                AND transfer.message_id = pending_operations.entity_id
                AND transfer.status != 'COMPLETE'
          ))
        """,
    )
    suspend fun claim(
        id: String,
        userId: String,
        clientId: String,
        orgId: String,
        now: Long,
        leaseUntil: Long,
    ): Int

    @Query(
        """
        UPDATE pending_operations
        SET status = 'PENDING', lease_until = 0
        WHERE user_id = :userId
          AND client_id = :clientId
          AND org_id = :orgId
          AND status = 'IN_PROGRESS'
          AND lease_until > 0
          AND lease_until <= :now
        """,
    )
    suspend fun releaseExpiredClaims(userId: String, clientId: String, orgId: String, now: Long): Int

    @Query(
        """
        UPDATE pending_operations
        SET status = 'PENDING', next_retry_at = :nextRetryAt, lease_until = 0
        WHERE id = :id
          AND user_id = :userId
          AND client_id = :clientId
          AND org_id = :orgId
          AND status = 'IN_PROGRESS'
        """,
    )
    suspend fun releaseClaim(
        id: String,
        userId: String,
        clientId: String,
        orgId: String,
        nextRetryAt: Long,
    ): Int

    @Query(
        """
        DELETE FROM pending_operations
        WHERE id = :id
          AND user_id = :userId
          AND client_id = :clientId
          AND org_id = :orgId
          AND status = 'IN_PROGRESS'
        """,
    )
    suspend fun deleteClaimedById(id: String, userId: String, clientId: String, orgId: String): Int

    @Query(
        """
        UPDATE pending_operations
        SET status = :status,
            attempt_count = :attemptCount,
            next_retry_at = :nextRetryAt,
            lease_until = 0,
            last_error_code = :errorCode,
            last_error_message = :errorMessage
        WHERE id = :id
          AND user_id = :userId
          AND client_id = :clientId
          AND org_id = :orgId
          AND status = 'IN_PROGRESS'
        """,
    )
    suspend fun recordFailure(
        id: String,
        userId: String,
        clientId: String,
        orgId: String,
        status: String,
        attemptCount: Int,
        nextRetryAt: Long,
        errorCode: String,
        errorMessage: String,
    ): Int

    @Query(
        """
        SELECT * FROM pending_operations
        WHERE user_id = :userId
          AND client_id = :clientId
          AND org_id = :orgId
          AND status = 'DEAD_LETTER'
        ORDER BY created_at DESC LIMIT :limit
        """,
    )
    suspend fun getDeadLetters(userId: String, clientId: String, orgId: String, limit: Int): List<PendingOperationEntity>

    @Query(
        """
        SELECT COUNT(*) FROM pending_operations
        WHERE user_id = :userId
          AND client_id = :clientId
          AND org_id = :orgId
          AND status = :status
        """,
    )
    suspend fun countByStatus(userId: String, clientId: String, orgId: String, status: String): Int

    @Query(
        """
        SELECT * FROM pending_operations
        WHERE user_id = :userId
          AND client_id = :clientId
          AND org_id = :orgId
          AND mutation_id = :mutationId
          AND status IN ('PENDING', 'IN_PROGRESS', 'DEAD_LETTER')
        LIMIT 1
        """,
    )
    suspend fun findActiveByMutationId(
        userId: String,
        clientId: String,
        orgId: String,
        mutationId: String,
    ): PendingOperationEntity?

    @Query(
        """
        SELECT * FROM pending_operations
        WHERE user_id = :userId
          AND client_id = :clientId
          AND org_id = :orgId
          AND entity_type = :entityType
          AND entity_id = :entityId
          AND operation = :operation
          AND status IN ('PENDING', 'IN_PROGRESS', 'DEAD_LETTER')
        ORDER BY created_at DESC
        LIMIT 1
        """,
    )
    suspend fun findActiveForEntity(
        userId: String,
        clientId: String,
        orgId: String,
        entityType: String,
        entityId: String,
        operation: String,
    ): PendingOperationEntity?

    @Query(
        """
        SELECT COUNT(*) FROM pending_operations
        WHERE user_id = :userId
          AND client_id = :clientId
          AND org_id = :orgId
          AND entity_type = :entityType
          AND entity_id = :entityId
          AND operation = :operation
          AND status IN ('PENDING', 'IN_PROGRESS', 'DEAD_LETTER')
          AND id != :excludeId
        """,
    )
    suspend fun countOtherActiveForEntity(
        userId: String,
        clientId: String,
        orgId: String,
        entityType: String,
        entityId: String,
        operation: String,
        excludeId: String,
    ): Int

    @Query(
        """
        SELECT * FROM pending_operations
        WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId
          AND depends_on_mutation_id=:mutationId
          AND status IN ('PENDING','IN_PROGRESS','DEAD_LETTER')
        ORDER BY created_at ASC
        """,
    )
    suspend fun getChildrenByDependency(
        userId: String, clientId: String, orgId: String, mutationId: String,
    ): List<PendingOperationEntity>

    @Query(
        """UPDATE pending_operations SET payload=:payload
        WHERE id=:id AND user_id=:userId AND client_id=:clientId AND org_id=:orgId
          AND status='PENDING' AND attempt_count=0""",
    )
    suspend fun updatePayloadBeforeFirstAttempt(
        id: String, userId: String, clientId: String, orgId: String, payload: String,
    ): Int

    @Query(
        """
        DELETE FROM pending_operations
        WHERE user_id = :userId
          AND client_id = :clientId
          AND org_id = :orgId
          AND mutation_id = :mutationId
        """,
    )
    suspend fun deleteByMutationId(userId: String, clientId: String, orgId: String, mutationId: String): Int

    @Query(
        """
        UPDATE pending_operations
        SET status = 'PENDING', attempt_count = 0, next_retry_at = 0, lease_until = 0,
            last_error_code = NULL, last_error_message = NULL
        WHERE id = :id
          AND user_id = :userId
          AND client_id = :clientId
          AND org_id = :orgId
          AND status IN ('PENDING', 'DEAD_LETTER')
        """,
    )
    suspend fun reactivate(id: String, userId: String, clientId: String, orgId: String): Int

    @Query(
        """
        SELECT * FROM pending_operations
        WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId
          AND entity_type=:entityType AND entity_id=:entityId
          AND status IN ('PENDING','IN_PROGRESS','DEAD_LETTER')
        ORDER BY created_at DESC LIMIT 1
        """,
    )
    suspend fun findAnyActiveForEntity(
        userId: String, clientId: String, orgId: String, entityType: String, entityId: String,
    ): PendingOperationEntity?

    @Query(
        """
        SELECT COUNT(*) FROM pending_operations
        WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId
          AND status IN ('PENDING','IN_PROGRESS','DEAD_LETTER')
        """,
    )
    suspend fun countActiveForScope(userId: String, clientId: String, orgId: String): Int

    @Query(
        """SELECT MIN(created_at) FROM pending_operations
           WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId
             AND status IN ('PENDING','IN_PROGRESS','DEAD_LETTER')""",
    )
    suspend fun oldestActiveCreatedAt(userId: String, clientId: String, orgId: String): Long?

    @Query(
        """SELECT COALESCE(SUM(attempt_count), 0) FROM pending_operations
           WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId
             AND status IN ('PENDING','IN_PROGRESS','DEAD_LETTER')""",
    )
    suspend fun sumActiveAttemptCount(userId: String, clientId: String, orgId: String): Long

    @Query(
        """
        DELETE FROM pending_operations
        WHERE user_id = :userId
          AND client_id = :clientId
          AND org_id = :orgId
        """,
    )
    suspend fun deleteForScope(userId: String, clientId: String, orgId: String): Int
}
