package com.autodrive.app.feature.commission.domain.repository

import com.autodrive.app.feature.commission.domain.model.CommissionEntry
import com.autodrive.app.feature.commission.domain.model.CommissionSummary
import com.autodrive.app.feature.commission.domain.model.Invoice
import com.autodrive.app.feature.commission.domain.model.InvoiceItem
import kotlinx.coroutines.flow.Flow

interface CommissionRepository {
    fun observeCommissions(clientId: String): Flow<Pair<CommissionSummary, List<CommissionEntry>>>
    fun observeInvoices(clientId: String): Flow<List<Invoice>>
    /** FIX-7: حساب الأهلية من السيرفر — يستبدل CommissionCalculator.calculate() محلياً */
    suspend fun getEligibilities(clientId: String): List<CommissionEntry>
    suspend fun getInvoiceItems(invoiceId: String): List<InvoiceItem>
}
