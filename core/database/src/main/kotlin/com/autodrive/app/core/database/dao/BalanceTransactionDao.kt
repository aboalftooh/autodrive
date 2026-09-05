package com.autodrive.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.autodrive.app.core.database.entities.BalanceTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BalanceTransactionDao {

    @Upsert
    suspend fun upsertAll(transactions: List<BalanceTransactionEntity>)

    @Upsert
    suspend fun upsert(transaction: BalanceTransactionEntity)

    @Query("SELECT * FROM balance_transactions WHERE user_id = :userId ORDER BY created_at DESC LIMIT 50")
    fun observe(userId: String): Flow<List<BalanceTransactionEntity>>

    @Query("SELECT * FROM balance_transactions WHERE user_id = :userId ORDER BY created_at DESC LIMIT 50")
    suspend fun getByUserId(userId: String): List<BalanceTransactionEntity>

    @Query("SELECT * FROM balance_transactions WHERE user_id = :userId ORDER BY created_at, id")
    suspend fun getAllByUserIdForSync(userId: String): List<BalanceTransactionEntity>

    @Query("SELECT * FROM balance_transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BalanceTransactionEntity?

    @Query("SELECT * FROM balance_transactions WHERE sync_status = 'PENDING'")
    suspend fun getPending(): List<BalanceTransactionEntity>

    @Query("UPDATE balance_transactions SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("DELETE FROM balance_transactions WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query("DELETE FROM balance_transactions WHERE id = :id AND user_id = :userId")
    suspend fun deleteByIdForUser(id: String, userId: String)

    @Query("DELETE FROM balance_transactions WHERE id = :id")
    suspend fun deleteById(id: String)
}
