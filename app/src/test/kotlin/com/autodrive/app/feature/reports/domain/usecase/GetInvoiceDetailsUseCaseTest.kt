package com.autodrive.app.feature.reports.domain.usecase

import com.autodrive.app.feature.commission.domain.model.Invoice
import com.autodrive.app.feature.commission.domain.model.InvoiceCategory
import com.autodrive.app.feature.commission.domain.model.InvoiceStatus
import com.autodrive.app.core.model.money.Money
import com.autodrive.app.feature.reports.domain.repository.InvoiceDetailRepository
import com.autodrive.app.feature.reports.domain.repository.InvoiceDetails
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class GetInvoiceDetailsUseCaseTest {

    @Test
    fun `delegates valid invoice id to repository`() = runBlocking {
        val expected = InvoiceDetails(invoice = sampleInvoice(), items = emptyList())
        val repository = FakeInvoiceDetailRepository(expected)
        val useCase = GetInvoiceDetailsUseCase(repository)

        val actual = useCase("invoice-7")

        assertSame(expected, actual)
        assertEquals("invoice-7", repository.lastInvoiceId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects blank invoice id`() = runBlocking {
        GetInvoiceDetailsUseCase(FakeInvoiceDetailRepository(InvoiceDetails(null, emptyList())))("   ")
    }

    @Test
    fun `preserves missing local invoice with empty items`() = runBlocking {
        val expected = InvoiceDetails(invoice = null, items = emptyList())
        val actual = GetInvoiceDetailsUseCase(FakeInvoiceDetailRepository(expected))("missing")

        assertEquals(expected, actual)
    }

    private fun sampleInvoice() = Invoice(
        id = "invoice-7",
        clientId = "client-1",
        commission = Money.of(50L),
        status = InvoiceStatus.OPEN,
        category = InvoiceCategory.SALE,
        totalAmount = Money.of(500L),
        invoiceNumber = 7,
        createdAt = "2026-07-25T00:00:00Z",
    )

    private class FakeInvoiceDetailRepository(
        private val result: InvoiceDetails,
    ) : InvoiceDetailRepository {
        var lastInvoiceId: String? = null

        override suspend fun load(invoiceId: String): InvoiceDetails {
            lastInvoiceId = invoiceId
            return result
        }
    }
}
