package com.autodrive.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.autodrive.app.core.database.entities.SyncCursorEntity

@Dao
interface SyncCursorDao {
    @Query(
        """
        SELECT * FROM sync_cursors
        WHERE user_id = :userId
          AND client_id = :clientId
          AND org_id = :orgId
          AND stream = :stream
        LIMIT 1
        """,
    )
    suspend fun get(
        userId: String,
        clientId: String,
        orgId: String,
        stream: String,
    ): SyncCursorEntity?

    @Upsert
    suspend fun upsert(cursor: SyncCursorEntity)

    @Query(
        """
        DELETE FROM sync_cursors
        WHERE user_id = :userId
          AND client_id = :clientId
          AND org_id = :orgId
        """,
    )
    suspend fun deleteForScope(userId: String, clientId: String, orgId: String)

    @Query(
        """
        SELECT COUNT(*) FROM sync_cursors
        WHERE user_id = :userId
          AND client_id = :clientId
          AND org_id = :orgId
        """,
    )
    suspend fun countForScope(userId: String, clientId: String, orgId: String): Int
}
