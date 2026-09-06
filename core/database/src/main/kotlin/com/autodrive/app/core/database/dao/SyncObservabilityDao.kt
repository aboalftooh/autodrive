package com.autodrive.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.autodrive.app.core.database.entities.SyncObservabilityStateEntity

@Dao
interface SyncObservabilityDao {
    @Query(
        """SELECT * FROM sync_observability_state
           WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId AND stream=:stream
           LIMIT 1""",
    )
    suspend fun get(userId: String, clientId: String, orgId: String, stream: String): SyncObservabilityStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: SyncObservabilityStateEntity)

    @Query(
        """DELETE FROM sync_observability_state
           WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId""",
    )
    suspend fun deleteForScope(userId: String, clientId: String, orgId: String): Int
}
