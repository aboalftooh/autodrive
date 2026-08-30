package com.autodrive.app.feature.reports.presentation.log

import com.autodrive.app.core.model.money.Money
import com.autodrive.app.feature.balance.domain.model.MarketerBalance
import com.autodrive.app.feature.commission.domain.CommissionCalculator
import com.autodrive.app.feature.commission.domain.model.CommissionEntry
import com.autodrive.app.feature.commission.domain.model.CommissionStatus
import com.autodrive.app.feature.commission.domain.model.CommissionSummary
import com.autodrive.app.feature.commission.domain.model.Invoice
import com.autodrive.app.feature.commission.domain.model.InvoiceCategory
import com.autodrive.app.feature.commission.domain.model.InvoiceStatus
import java.io.File
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportsViewModelTest {

    private val calculator = CommissionCalculator()
    private val weekStart = Instant.parse("2026-08-07T06:00:00Z").toEpochMilli()
    private val weekMs = 7L * 24L * 60L * 60L * 1_000L

    @Test
    fun `current week boundaries use summary weekStartMs`() {
        val state = build(
            invoices = listOf(
                invoice("at-start", weekStart, 100),
                invoice("before-end", weekStart + weekMs - 1, 200),
                invoice("at-end", weekStart + weekMs, 400),
            )
        )

        assertEquals(2, state.currentWeekInvoiceCount)
        assertEquals(Money.of(300L), state.currentWeekPurchases)
    }

    @Test
    fun `previous week boundaries are the seven days before current start`() {
        val state = build(
            invoices = listOf(
                invoice("previous-start", weekStart - weekMs, 100),
                invoice("previous-end", weekStart - 1, 200),
                invoice("too-old", weekStart - weekMs - 1, 400),
            )
        )

        assertEquals(2, state.previousWeekInvoiceCount)
        assertEquals(Money.of(300L), state.previousWeekPurchases)
    }

    @Test
    fun `purchase totals sum invoice totalAmount for each week`() {
        val state = build(
            invoices = listOf(
                invoice("current-1", weekStart + 1, 125),
                invoice("current-2", weekStart + 2, 375),
                invoice("previous", weekStart - 1, 250),
            )
        )

        assertEquals(Money.of(500L), state.currentWeekPurchases)
        assertEquals(Money.of(250L), state.previousWeekPurchases)
    }

    @Test
    fun `commission totals sum entries by createdAt without redefining eligibility`() {
        val state = build(
            entries = listOf(
                entry("current-pending", weekStart + 1, 20, CommissionStatus.PENDING),
                entry("current-paid", weekStart + 2, 30, CommissionStatus.PAID),
                entry("previous", weekStart - 1, 15, CommissionStatus.WITHDRAWABLE),
            )
        )

        assertEquals(Money.of(50L), state.currentWeekCommissions)
        assertEquals(Money.of(15L), state.previousWeekCommissions)
    }

    @Test
    fun `invalid dates do not enter current or previous week`() {
        val state = build(
            invoices = listOf(invalidInvoice()),
            entries = listOf(invalidEntry()),
        )

        assertEquals(0, state.currentWeekInvoiceCount)
        assertEquals(0, state.previousWeekInvoiceCount)
        assertEquals(Money.ZERO, state.currentWeekPurchases)
        assertEquals(Money.ZERO, state.previousWeekPurchases)
        assertEquals(Money.ZERO, state.currentWeekCommissions)
        assertEquals(Money.ZERO, state.previousWeekCommissions)
    }

    @Test
    fun `previous zero and current positive is NEW`() {
        assertEquals(
            TrendComparison(TrendDirection.NEW, null),
            compareTrend(Money.of(100L), Money.ZERO)
        )
    }

    @Test
    fun `equal current and previous is FLAT zero percent`() {
        assertEquals(
            TrendComparison(TrendDirection.FLAT, 0),
            compareTrend(Money.of(100L), Money.of(100L))
        )
    }

    @Test
    fun `negative trend is DOWN with HALF_UP integer percent`() {
        assertEquals(
            TrendComparison(TrendDirection.DOWN, 33),
            compareTrend(Money.of(100L), Money.of(150L))
        )
    }

    @Test
    fun `reports trend arithmetic contains no Double conversions`() {
        val source = File("src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ReportsViewModel.kt")
            .takeIf { it.exists() }
            ?: File("app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ReportsViewModel.kt")
        val text = source.readText()

        assertTrue("ReportsViewModel must use BigDecimal", text.contains("BigDecimal"))
        assertTrue("ReportsViewModel must use HALF_UP", text.contains("RoundingMode.HALF_UP"))
        assertTrue("ReportsViewModel must not use Double arithmetic", !text.contains("toDouble()"))
        assertTrue("ReportsViewModel must not use legacy Double money conversion", !text.contains("fromLegacyDouble"))
    }

    @Test
    fun `initial failure becomes ERROR and not content with fake zeros`() {
        val failed = ReportsUiState().withReportsFailure("offline")

        assertEquals(ReportsLoadState.ERROR, failed.loadState)
        assertEquals("offline", failed.errorMessage)
    }

    @Test
    fun `failure after content preserves content values`() {
        val content = build(invoices = listOf(invoice("current", weekStart + 1, 500)))
        val failed = content.withReportsFailure("offline")

        assertEquals(ReportsLoadState.CONTENT, failed.loadState)
        assertEquals(Money.of(500L), failed.currentWeekPurchases)
        assertEquals("offline", failed.errorMessage)
    }

    @Test
    fun `competition RPC is gated by ACTIVE signal from reports screen`() {
        val projectRoot = File("app/src/main/kotlin/com/autodrive/app")
            .takeIf { it.exists() }
            ?: File("src/main/kotlin/com/autodrive/app")
        val viewModelSource = projectRoot.resolve("feature/reports/presentation/log/ReportsViewModel.kt").readText()
        val screenSource = projectRoot.resolve("feature/reports/presentation/log/ActivityLogScreen.kt").readText()
        val initSection = viewModelSource.substringAfter("init {").substringBefore("fun setCompetitionActive")

        assertTrue(!initSection.contains("observeWeeklyCompetition.refresh()"))
        assertTrue(screenSource.contains("viewModel.setCompetitionActive(competitionAvailability == CompetitionAvailability.ACTIVE)"))
    }

    @Test
    fun `winCount null remains unknown`() {
        val state = build(winCount = null)

        assertNull(state.winCount)
    }

    @Test
    fun `pending uses commission summary without local eligibility recomputation`() {
        val state = build(
            summary = summary(pending = Money.of(777L)),
            entries = listOf(entry("pending", weekStart + 1, 10, CommissionStatus.PENDING)),
        )

        assertEquals(Money.of(777L), state.pending)
    }

    private fun build(
        summary: CommissionSummary = summary(),
        invoices: List<Invoice> = emptyList(),
        entries: List<CommissionEntry> = emptyList(),
        winCount: Int? = null,
    ): ReportsUiState = buildReportsContent(
        summary = summary,
        allEntries = entries,
        allInvoices = invoices,
        balance = MarketerBalance(
            balance = Money.of(900L),
            pendingWithdrawal = Money.ZERO,
            updatedAt = "2026-08-13T00:00:00Z",
        ),
        calculator = calculator,
        joinDate = "1/1/2026",
        winCount = winCount,
    )

    private fun summary(pending: Money = Money.of(70L)) = CommissionSummary(
        withdrawable = Money.ZERO,
        pending = pending,
        paid = Money.ZERO,
        weeklyTotal = Money.ZERO,
        lastFriday9AmLabel = "",
        weekStartMs = weekStart,
    )

    private fun invoice(id: String, createdAtMs: Long, total: Long) = Invoice(
        id = id,
        clientId = "client",
        commission = Money.ZERO,
        status = InvoiceStatus.CLOSED_CASH,
        category = InvoiceCategory.SALE,
        totalAmount = Money.of(total),
        invoiceNumber = id.hashCode(),
        createdAt = Instant.ofEpochMilli(createdAtMs).toString(),
    )

    private fun entry(
        id: String,
        createdAtMs: Long,
        amount: Long,
        status: CommissionStatus,
    ) = CommissionEntry(
        invoiceId = id,
        invoiceNumber = id.hashCode(),
        amount = Money.of(amount),
        status = status,
        createdAt = Instant.ofEpochMilli(createdAtMs).toString(),
    )

    private fun invalidInvoice() = Invoice(
        id = "invalid",
        clientId = "client",
        commission = Money.ZERO,
        status = InvoiceStatus.CLOSED_CASH,
        category = InvoiceCategory.SALE,
        totalAmount = Money.of(999L),
        invoiceNumber = 999,
        createdAt = "not-a-date",
    )

    private fun invalidEntry() = CommissionEntry(
        invoiceId = "invalid",
        invoiceNumber = 999,
        amount = Money.of(999L),
        status = CommissionStatus.PENDING,
        createdAt = "not-a-date",
    )
}
