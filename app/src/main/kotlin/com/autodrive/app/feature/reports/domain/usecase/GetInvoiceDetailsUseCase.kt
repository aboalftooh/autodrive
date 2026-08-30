package com.autodrive.app.feature.reports.domain.usecase

import com.autodrive.app.feature.reports.domain.repository.InvoiceDetailRepository
import com.autodrive.app.feature.reports.domain.repository.InvoiceDetails
import javax.inject.Inject

class GetInvoiceDetailsUseCase @Inject constructor(
    private val repository: InvoiceDetailRepository,
) {
    suspend operator fun invoke(invoiceId: String): InvoiceDetails {
        require(invoiceId.isNotBlank()) { "invoiceId must not be blank" }
        return repository.load(invoiceId)
    }
}
