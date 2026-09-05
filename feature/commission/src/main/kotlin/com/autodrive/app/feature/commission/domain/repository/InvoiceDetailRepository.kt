package com.autodrive.app.feature.commission.domain.repository

import com.autodrive.app.feature.commission.domain.model.Invoice
import com.autodrive.app.feature.commission.domain.model.InvoiceItem

data class InvoiceDetails(
    val invoice: Invoice?,
    val items: List<InvoiceItem>,
)

interface InvoiceDetailRepository {
    suspend fun load(invoiceId: String): InvoiceDetails
}
