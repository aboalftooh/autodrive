package com.autodrive.app.feature.reports.data

import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.database.entities.InvoiceEntity
import com.autodrive.app.feature.commission.domain.model.Invoice
import com.autodrive.app.feature.commission.domain.model.InvoiceCategory
import com.autodrive.app.feature.commission.domain.model.InvoiceStatus
import com.autodrive.app.core.model.money.Money
import com.autodrive.app.feature.commission.domain.repository.CommissionRepository
import com.autodrive.app.feature.reports.domain.repository.InvoiceDetailRepository
import com.autodrive.app.feature.reports.domain.repository.InvoiceDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceDetailRepositoryImpl @Inject constructor(
    private val db: AutoDriveDatabase,
    private val commissionRepository: CommissionRepository,
) : InvoiceDetailRepository {

    override suspend fun load(invoiceId: String): InvoiceDetails = withContext(Dispatchers.IO) {
        val invoice = db.invoiceDao().getById(invoiceId)?.toDomain()
        val items = runCatching {
            commissionRepository.getInvoiceItems(invoiceId)
        }.getOrDefault(emptyList())
        InvoiceDetails(invoice = invoice, items = items)
    }

    private fun InvoiceEntity.toDomain() = Invoice(
        id = id,
        clientId = clientId,
        commission = Money.of(commission),
        status = runCatching {
            InvoiceStatus.valueOf(status.uppercase())
        }.getOrDefault(InvoiceStatus.OPEN),
        category = runCatching {
            InvoiceCategory.valueOf(category.uppercase())
        }.getOrDefault(InvoiceCategory.OTHER),
        totalAmount = Money.of(totalAmount),
        invoiceNumber = invoiceNumber,
        createdAt = createdAt,
    )
}
