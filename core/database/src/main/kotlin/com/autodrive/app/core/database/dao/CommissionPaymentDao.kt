package com.autodrive.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.autodrive.app.core.database.entities.CommissionPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommissionPaymentDao {

    @Upsert
    suspend fun upsertAll(payments: List<CommissionPaymentEntity>)

    @Upsert
    suspend fun upsert(payment: CommissionPaymentEntity)

    @Query("SELECT * FROM commission_payments WHERE client_id = :clientId")
    fun observeByClientId(clientId: String): Flow<List<CommissionPaymentEntity>>

    @Query("SELECT * FROM commission_payments WHERE client_id = :clientId")
    suspend fun getByClientId(clientId: String): List<CommissionPaymentEntity>

    @Query("SELECT * FROM commission_payments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CommissionPaymentEntity?

    @Query("DELETE FROM commission_payments WHERE id = :id AND client_id = :clientId")
    suspend fun deleteByIdForClient(id: String, clientId: String)

    @Query("DELETE FROM commission_payments WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM commission_payments WHERE client_id = :clientId")
    suspend fun deleteByClientId(clientId: String)
}
