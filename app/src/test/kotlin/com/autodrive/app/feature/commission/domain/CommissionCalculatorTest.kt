package com.autodrive.app.feature.commission.domain

import com.autodrive.app.core.model.money.Money
import com.autodrive.app.feature.commission.domain.model.CommissionEntry
import com.autodrive.app.feature.commission.domain.model.CommissionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class CommissionCalculatorTest {
    private val calculator = CommissionCalculator()

    @Test
    fun summarize_usesServerWeekStartAndExcludesRowsOutsideThatWeek() {
        val serverWeekStart = calculator.parseIsoMs("2026-05-29T06:00:00Z")
        val entries = listOf(
            entry("old", "2026-05-16T15:41:40Z", "15000"),
            entry("current-a", "2026-06-01T08:00:00Z", "0.1"),
            entry("current-b", "2026-06-01T09:00:00Z", "0.2"),
            entry("future", "2026-06-22T00:00:00Z", "15000")
        )

        val summary = calculator.summarize(entries, serverWeekStart)

        assertEquals(Money.of("0.3"), summary.weeklyTotal)
    }

    private fun entry(id: String, createdAt: String, amount: String) = CommissionEntry(
        invoiceId = id,
        invoiceNumber = 1,
        amount = Money.of(amount),
        status = CommissionStatus.WITHDRAWABLE,
        createdAt = createdAt
    )
    @Test
    fun summarize_partialPayoutUsesRemainingAmountsWithoutInflatingBalance() {
        val partial = CommissionEntry(
            invoiceId = "partial",
            invoiceNumber = 7,
            amount = Money.of("100"),
            status = CommissionStatus.WITHDRAWABLE,
            createdAt = "2026-01-01T00:00:00Z",
            paidOutAmount = Money.of("40"),
            remainingAmount = Money.of("60"),
            withdrawableAmount = Money.of("60"),
            pendingAmount = Money.ZERO,
        )
        val pending = CommissionEntry(
            invoiceId = "pending",
            invoiceNumber = 8,
            amount = Money.of("50"),
            status = CommissionStatus.PENDING,
            createdAt = "2026-01-01T00:00:00Z",
            pendingAmount = Money.of("50"),
        )

        val summary = calculator.summarize(listOf(partial, pending), 1L)

        assertEquals(Money.of("60"), summary.withdrawable)
        assertEquals(Money.of("50"), summary.pending)
        assertEquals(Money.of("40"), summary.paid)
    }

}
