package com.autodrive.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.autodrive.app.core.database.entities.InvoiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    @Upsert
    suspend fun upsertAll(invoices: List<InvoiceEntity>)

    @Upsert
    suspend fun upsert(invoice: InvoiceEntity)

    @Query("SELECT * FROM invoices WHERE client_id = :clientId AND category = 'SALE' AND CAST(commission AS NUMERIC) > 0")
    fun observeByClientId(clientId: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE client_id = :clientId AND category = 'SALE' AND CAST(commission AS NUMERIC) > 0")
    suspend fun getByClientId(clientId: String): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE client_id = :clientId ORDER BY id")
    suspend fun getAllByClientIdForSync(clientId: String): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): InvoiceEntity?

    @Query("DELETE FROM invoices WHERE id = :id AND client_id = :clientId")
    suspend fun deleteByIdForClient(id: String, clientId: String)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM invoices WHERE client_id = :clientId")
    suspend fun deleteByClientId(clientId: String)
}
