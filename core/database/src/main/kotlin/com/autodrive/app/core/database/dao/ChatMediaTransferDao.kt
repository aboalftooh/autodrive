package com.autodrive.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.autodrive.app.core.database.entities.ChatMediaTransferEntity

@Dao
interface ChatMediaTransferDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transfer: ChatMediaTransferEntity)

    @Query("""SELECT * FROM chat_media_transfers
        WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId
          AND status='PENDING' AND next_retry_at<=:now
        ORDER BY created_at ASC LIMIT :limit""")
    suspend fun getDue(userId: String, clientId: String, orgId: String, now: Long, limit: Int): List<ChatMediaTransferEntity>

    @Query("""UPDATE chat_media_transfers
        SET status='IN_PROGRESS', lease_until=:leaseUntil, updated_at=:now
        WHERE transfer_id=:transferId AND user_id=:userId AND client_id=:clientId AND org_id=:orgId
          AND status='PENDING' AND next_retry_at<=:now""")
    suspend fun claim(transferId: String, userId: String, clientId: String, orgId: String, now: Long, leaseUntil: Long): Int

    @Query("""UPDATE chat_media_transfers
        SET status='PENDING', lease_until=0, updated_at=:now
        WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId
          AND status='IN_PROGRESS' AND lease_until>0 AND lease_until<=:now""")
    suspend fun releaseExpiredClaims(userId: String, clientId: String, orgId: String, now: Long): Int

    @Query("""UPDATE chat_media_transfers
        SET status='PENDING', attempt_count=:attemptCount, next_retry_at=:nextRetryAt, lease_until=0,
            last_error_code=:errorCode, updated_at=:now
        WHERE transfer_id=:transferId AND user_id=:userId AND client_id=:clientId AND org_id=:orgId
          AND status='IN_PROGRESS'""")
    suspend fun retry(
        transferId: String, userId: String, clientId: String, orgId: String,
        attemptCount: Int, nextRetryAt: Long, errorCode: String, now: Long,
    ): Int

    @Query("""UPDATE chat_media_transfers
        SET status='DEAD_LETTER', attempt_count=:attemptCount, lease_until=0,
            last_error_code=:errorCode, updated_at=:now
        WHERE transfer_id=:transferId AND user_id=:userId AND client_id=:clientId AND org_id=:orgId
          AND status='IN_PROGRESS'""")
    suspend fun deadLetter(
        transferId: String, userId: String, clientId: String, orgId: String,
        attemptCount: Int, errorCode: String, now: Long,
    ): Int

    @Query("""UPDATE chat_media_transfers
        SET status='COMPLETE', remote_reference=:remoteReference, lease_until=0,
            last_error_code=NULL, updated_at=:now
        WHERE transfer_id=:transferId AND user_id=:userId AND client_id=:clientId AND org_id=:orgId
          AND status='IN_PROGRESS'""")
    suspend fun complete(
        transferId: String, userId: String, clientId: String, orgId: String,
        remoteReference: String, now: Long,
    ): Int

    @Query("""SELECT * FROM chat_media_transfers
        WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId AND message_id=:messageId LIMIT 1""")
    suspend fun getForMessage(userId: String, clientId: String, orgId: String, messageId: String): ChatMediaTransferEntity?

    @Query("""UPDATE chat_media_transfers
        SET status='PENDING', attempt_count=0, next_retry_at=0, lease_until=0, last_error_code=NULL, updated_at=:now
        WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId AND message_id=:messageId
          AND status IN ('PENDING','DEAD_LETTER')""")
    suspend fun reactivateForMessage(
        userId: String, clientId: String, orgId: String, messageId: String, now: Long,
    ): Int

    @Query("DELETE FROM chat_media_transfers WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId")
    suspend fun deleteForScope(userId: String, clientId: String, orgId: String): Int

    @Query("SELECT local_path FROM chat_media_transfers WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId")
    suspend fun getLocalPathsForScope(userId: String, clientId: String, orgId: String): List<String>
}
