package com.autodrive.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.autodrive.app.core.database.entities.SyncInboxEntity

@Dao
interface SyncInboxDao {
    @Query(
        """
        SELECT * FROM sync_inbox
        WHERE user_id = :userId
          AND client_id = :clientId
          AND org_id = :orgId
          AND stream = :stream
          AND event_id = :eventId
        LIMIT 1
        """,
    )
    suspend fun get(
        userId: String,
        clientId: String,
        orgId: String,
        stream: String,
        eventId: String,
    ): SyncInboxEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: SyncInboxEntity)

    @Query(
        """
        UPDATE sync_inbox SET applied_at = :appliedAt
        WHERE user_id = :userId
          AND client_id = :clientId
          AND org_id = :orgId
          AND stream = :stream
          AND event_id = :eventId
        """,
    )
    suspend fun markApplied(
        userId: String,
        clientId: String,
        orgId: String,
        stream: String,
        eventId: String,
        appliedAt: Long,
    )

    @Query(
        """
        SELECT COUNT(*) FROM sync_inbox
        WHERE user_id = :userId
          AND client_id = :clientId
          AND org_id = :orgId
        """,
    )
    suspend fun countForScope(userId: String, clientId: String, orgId: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM sync_inbox
        WHERE user_id = :userId
          AND client_id = :clientId
          AND org_id = :orgId
          AND applied_at IS NULL
        """,
    )
    suspend fun countUnappliedForScope(userId: String, clientId: String, orgId: String): Int

    @Query(
        """
        DELETE FROM sync_inbox
        WHERE user_id = :userId
          AND client_id = :clientId
          AND org_id = :orgId
        """,
    )
    suspend fun deleteForScope(userId: String, clientId: String, orgId: String)
}
