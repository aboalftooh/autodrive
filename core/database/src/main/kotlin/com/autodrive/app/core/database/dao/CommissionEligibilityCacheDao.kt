package com.autodrive.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.autodrive.app.core.database.entities.CommissionEligibilityCacheEntity
import com.autodrive.app.core.database.entities.CommissionEligibilitySyncStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CommissionEligibilityCacheDao {

    @Query("SELECT * FROM commission_eligibility_cache WHERE client_id = :clientId ORDER BY created_at DESC")
    abstract fun observeByClientId(clientId: String): Flow<List<CommissionEligibilityCacheEntity>>

    @Query("SELECT * FROM commission_eligibility_sync_state WHERE client_id = :clientId LIMIT 1")
    abstract fun observeSyncState(clientId: String): Flow<CommissionEligibilitySyncStateEntity?>

    @Upsert
    protected abstract suspend fun upsertAll(rows: List<CommissionEligibilityCacheEntity>)

    @Upsert
    protected abstract suspend fun upsertSyncState(state: CommissionEligibilitySyncStateEntity)

    @Query("DELETE FROM commission_eligibility_cache WHERE client_id = :clientId")
    protected abstract suspend fun clearClient(clientId: String)

    @Query("DELETE FROM commission_eligibility_sync_state WHERE client_id = :clientId")
    protected abstract suspend fun clearSyncState(clientId: String)

    @Transaction
    open suspend fun clearScope(clientId: String) {
        clearClient(clientId)
        clearSyncState(clientId)
    }

    @Transaction
    open suspend fun replaceSnapshot(
        clientId: String,
        rows: List<CommissionEligibilityCacheEntity>,
        state: CommissionEligibilitySyncStateEntity,
    ) {
        clearClient(clientId)
        if (rows.isNotEmpty()) upsertAll(rows)
        upsertSyncState(state)
    }
}
