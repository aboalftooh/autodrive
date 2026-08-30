package com.autodrive.app.feature.reports.presentation.log

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportsV62ContractTest {
    private fun source(name: String): String {
        val relative = "src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/$name"
        return (File(relative).takeIf { it.exists() } ?: File("app/$relative")).readText()
    }

    @Test
    fun `REPORTS V1 owns stat tile dashboard width and responsive fallback`() {
        val activity = source("ActivityLogScreen.kt")
        val content = activity.substringAfter("private fun ReportsContent").substringBefore("private fun CurrentWeekHero")
        val pair = activity.substringAfter("private fun ResponsiveReportPair").substringBefore("private fun TrendCard")
        val financial = activity.substringAfter("private fun FinancialStatus").substringBefore("private fun ReportDetails")

        assertTrue(content.contains("widthIn(max = AutoDriveContentWidth.Dashboard)"))
        assertTrue(financial.contains("ReportStatTile("))
        assertTrue(pair.contains("maxWidth >= AutoDriveContentWidth.ReportTwoColumn"))
        assertTrue(pair.contains("Row("))
        assertTrue(pair.contains("Column("))
        assertFalse(activity.contains("840.dp"))
        assertFalse(activity.contains("360.dp"))
    }

    @Test
    fun `report behavior and section order stay stable`() {
        val activity = source("ActivityLogScreen.kt")
        val content = activity.substringAfter("private fun ReportsContent").substringBefore("private fun CurrentWeekHero")
        val details = activity.substringAfter("private fun ReportDetails").substringBefore("private fun HistoricalAchievement")
        val names = listOf("CurrentWeekHero", "PreviousWeekComparison", "FinancialStatus", "ReportDetails", "HistoricalAchievement")
        val positions = names.map(content::indexOf)
        assertTrue(positions.all { it >= 0 })
        assertTrue(positions.zipWithNext().all { (a, b) -> a < b })
        assertTrue(details.contains("competitionAvailability == CompetitionAvailability.ACTIVE"))
        assertTrue(details.contains("onNavigateInvoiceList(\"current\")"))
    }

    @Test
    fun `confirmed Material bypasses use governed replacements`() {
        val competition = source("CompetitionHistoryScreen.kt")
        val detail = source("InvoiceDetailScreen.kt")
        val list = source("InvoiceListScreen.kt")
        val weekly = source("WeeklyCommissionsScreen.kt")

        assertTrue(competition.split("AutoDriveTextButton(").size - 1 == 2)
        assertTrue(detail.contains("AutoDriveIconButton("))
        assertTrue(detail.contains("AutoDriveFab("))
        assertTrue(detail.contains("AutoDriveDivider()"))
        assertTrue(list.contains("AutoDriveTextButton("))
        assertTrue(weekly.contains("AutoDriveTextButton("))
    }
}
