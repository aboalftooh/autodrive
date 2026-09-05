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
}
