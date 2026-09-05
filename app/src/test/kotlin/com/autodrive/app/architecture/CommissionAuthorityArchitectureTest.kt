package com.autodrive.app.architecture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommissionAuthorityArchitectureTest {
    @Test
    fun `commission eligibility is server authoritative`() {
        val repository = ProjectLayout.source("feature/commission/data/CommissionRepositoryImpl.kt").readText()
        val invoiceVm = ProjectLayout.source("feature/commission/presentation/InvoiceDetailViewModel.kt").readText()
        val dto = ProjectLayout.source("core/network/dto/Dtos.kt").readText()

        assertFalse(repository.contains("buildLocalCommissionSnapshot"))
        assertFalse(repository.contains("LOCAL_FALLBACK"))
        assertFalse(invoiceVm.contains("commissionStatus(invoice"))
        assertTrue(dto.contains("remainingAmount"))
        assertTrue(dto.contains("reasonCode"))
        assertTrue(dto.contains("reasonMessage"))
    }
}
