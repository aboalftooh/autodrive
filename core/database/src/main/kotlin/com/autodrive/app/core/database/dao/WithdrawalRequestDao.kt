package com.autodrive.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.autodrive.app.core.database.entities.WithdrawalRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WithdrawalRequestDao {

    @Upsert
    suspend fun upsertAll(requests: List<WithdrawalRequestEntity>)

    @Upsert
    suspend fun upsert(request: WithdrawalRequestEntity)

    @Query("SELECT * FROM withdrawal_requests WHERE user_id = :userId ORDER BY created_at DESC LIMIT 20")
    fun observe(userId: String): Flow<List<WithdrawalRequestEntity>>

    @Query("SELECT * FROM withdrawal_requests WHERE user_id = :userId ORDER BY created_at DESC LIMIT 20")
    suspend fun getByUserId(userId: String): List<WithdrawalRequestEntity>

    @Query("SELECT * FROM withdrawal_requests WHERE user_id = :userId ORDER BY created_at, id")
    suspend fun getAllByUserIdForSync(userId: String): List<WithdrawalRequestEntity>

    @Query("SELECT * FROM withdrawal_requests WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WithdrawalRequestEntity?

    @Query("SELECT * FROM withdrawal_requests WHERE sync_status = 'PENDING'")
    suspend fun getPending(): List<WithdrawalRequestEntity>

    @Query("UPDATE withdrawal_requests SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("UPDATE withdrawal_requests SET id = :newId, sync_status = 'SYNCED' WHERE id = :tempId")
    suspend fun confirmSynced(tempId: String, newId: String)

    @Query("DELETE FROM withdrawal_requests WHERE id = :id AND user_id = :userId")
    suspend fun deleteByIdForUser(id: String, userId: String)

    @Query("DELETE FROM withdrawal_requests WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM withdrawal_requests WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query("DELETE FROM withdrawal_requests WHERE user_id = :userId AND status = 'PENDING'")
    suspend fun deletePendingByUserId(userId: String)
}
