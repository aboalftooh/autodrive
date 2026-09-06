package com.autodrive.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.autodrive.app.core.database.entities.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Upsert
    suspend fun upsertAll(payments: List<PaymentEntity>)

    @Upsert
    suspend fun upsert(payment: PaymentEntity)

    @Query("SELECT * FROM payments WHERE invoice_id IN (:invoiceIds)")
    fun observeByInvoiceIds(invoiceIds: List<String>): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE invoice_id IN (:invoiceIds)")
    suspend fun getByInvoiceIds(invoiceIds: List<String>): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE client_id = :clientId ORDER BY created_at, id")
    suspend fun getAllByClientIdForSync(clientId: String): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PaymentEntity?

    @Query("DELETE FROM payments WHERE id = :id AND client_id = :clientId")
    suspend fun deleteByIdForClient(id: String, clientId: String)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM payments WHERE client_id = :clientId")
    suspend fun deleteByClientId(clientId: String)

    @Query("DELETE FROM payments WHERE invoice_id IN (:invoiceIds)")
    suspend fun deleteByInvoiceIds(invoiceIds: List<String>)
}
