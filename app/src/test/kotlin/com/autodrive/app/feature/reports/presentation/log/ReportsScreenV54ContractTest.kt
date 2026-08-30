package com.autodrive.app.feature.reports.presentation.log

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportsScreenV54ContractTest {

    private val source: String by lazy {
        val file = File("src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt")
            .takeIf { it.exists() }
            ?: File("app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt")
        file.readText()
    }

    @Test
    fun `hero is current week and never lifetime`() {
        val hero = source.substringAfter("private fun CurrentWeekHero").substringBefore("private fun HeroSupportingMetric")

        assertTrue(hero.contains("DashboardHero"))
        assertTrue(hero.contains("\"هذا الأسبوع\""))
        assertTrue(hero.contains("state.currentWeekPurchases"))
        assertTrue(hero.contains("state.currentWeekCommissions"))
        assertTrue(hero.contains("state.currentWeekInvoiceCount"))
        assertTrue(!hero.contains("lifetimeCommissions"))
    }

    @Test
    fun `competition report actions are active only`() {
        val details = source.substringAfter("private fun ReportDetails").substringBefore("private fun HistoricalAchievement")

        assertTrue(details.contains("competitionAvailability == CompetitionAvailability.ACTIVE"))
        assertTrue(details.contains("\"المسابقة الأسبوعية\""))
        assertTrue(details.contains("\"أسابيع الفوز\""))
        assertTrue(!details.contains("CompetitionAvailability.LOCKED"))
        assertTrue(!details.contains("CompetitionAvailability.DISABLED"))
    }

    @Test
    fun `report routes remain wired`() {
        assertTrue(source.contains("onClick = onNavigateBalance"))
        assertTrue(source.contains("onNavigateInvoiceList(\"current\")"))
        assertTrue(source.contains("onClick = onNavigateWeeklyCommissions"))
        assertTrue(source.contains("onClick = onNavigateCompetitionHistory"))
        assertTrue(source.contains("onClick = onNavigateWinWeeks"))
    }

    @Test
    fun `reports screen uses design system without raw colors or decorative canvas`() {
        assertTrue(source.contains("DashboardHero"))
        assertTrue(source.contains("AutoDriveMetricCard"))
        assertTrue(source.contains("AutoDriveCard"))
        assertTrue(source.contains("AutoDriveBottomNavigation"))
        assertTrue(!source.contains("Color(0x"))
        assertTrue(!source.contains("Canvas("))
        assertTrue(!source.contains("drawWithCache"))
        assertTrue(!source.contains("background dots"))
    }

    @Test
    fun `loading and error states never render report money as fake zero content`() {
        val loading = source.substringAfter("private fun ReportsLoading").substringBefore("private fun ReportsError")
        val error = source.substringAfter("private fun ReportsError").substringBefore("private fun ReportsContent")

        assertTrue(loading.contains("LoadingScreen"))
        assertTrue(error.contains("ErrorScreen"))
        assertTrue(error.contains("onRetry"))
        assertTrue(!loading.contains("Money.ZERO"))
        assertTrue(!error.contains("Money.ZERO"))
        assertTrue(!loading.contains("FormatUtils.formatSar"))
        assertTrue(!error.contains("FormatUtils.formatSar"))
    }

    @Test
    fun `required report hierarchy is present in order`() {
        val content = source.substringAfter("private fun ReportsContent").substringBefore("private fun CurrentWeekHero")
        val hero = content.indexOf("CurrentWeekHero")
        val comparison = content.indexOf("PreviousWeekComparison")
        val financial = content.indexOf("FinancialStatus")
        val details = content.indexOf("ReportDetails")
        val historical = content.indexOf("HistoricalAchievement")

        assertTrue(hero >= 0)
        assertTrue(hero < comparison)
        assertTrue(comparison < financial)
        assertTrue(financial < details)
        assertTrue(details < historical)
    }
}
