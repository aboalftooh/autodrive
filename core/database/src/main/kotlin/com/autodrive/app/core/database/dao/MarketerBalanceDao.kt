package com.autodrive.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.autodrive.app.core.database.entities.MarketerBalanceEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface MarketerBalanceDao {

    @Upsert
    suspend fun upsert(balance: MarketerBalanceEntity)

    @Query("SELECT * FROM marketer_balance WHERE user_id = :userId LIMIT 1")
    fun observe(userId: String): Flow<MarketerBalanceEntity?>

    @Query("SELECT * FROM marketer_balance WHERE user_id = :userId LIMIT 1")
    suspend fun get(userId: String): MarketerBalanceEntity?

    @Query("UPDATE marketer_balance SET balance = :balance, updated_at = :updatedAt WHERE user_id = :userId")
    suspend fun updateBalance(userId: String, balance: BigDecimal, updatedAt: String)

    @Query("DELETE FROM marketer_balance WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String)
}
