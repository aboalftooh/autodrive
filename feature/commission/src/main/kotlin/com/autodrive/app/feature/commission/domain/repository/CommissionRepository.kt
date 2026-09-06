package com.autodrive.app.feature.commission.domain.repository

import com.autodrive.app.feature.commission.domain.model.CommissionSnapshot
import com.autodrive.app.feature.commission.domain.model.CommissionPage
import com.autodrive.app.feature.commission.domain.model.CommissionPageCursor
import com.autodrive.app.feature.commission.domain.model.CommissionListSummary
import com.autodrive.app.feature.commission.domain.model.Invoice
import com.autodrive.app.feature.commission.domain.model.InvoiceItem
import kotlinx.coroutines.flow.Flow

interface CommissionRepository {
    /** Room-only observable source. Remote refresh is owned by the shared SyncCoordinator. */
    fun observeCommissions(clientId: String): Flow<CommissionSnapshot>
    fun observeInvoices(clientId: String): Flow<List<Invoice>>
    suspend fun getInvoiceItems(invoiceId: String): List<InvoiceItem>
    suspend fun getCommissionPage(cursor: CommissionPageCursor?, limit: Int = 10): CommissionPage
    suspend fun getCommissionListSummary(): CommissionListSummary
    suspend fun getPendingCommissionPage(cursor: CommissionPageCursor?, limit: Int = 10): CommissionPage
    suspend fun getPendingCommissionListSummary(): CommissionListSummary
}
