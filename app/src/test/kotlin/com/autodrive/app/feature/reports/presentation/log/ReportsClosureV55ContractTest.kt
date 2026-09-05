package com.autodrive.app.feature.reports.presentation.log

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportsClosureV55ContractTest {

    private fun projectFile(relative: String): String {
        val direct = File(relative)
        val fromRoot = File("app/$relative")
        return (direct.takeIf { it.exists() } ?: fromRoot).readText()
    }

    private val activity by lazy {
        projectFile("src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt")
    }

    private val navigation by lazy {
        projectFile("src/main/kotlin/com/autodrive/app/navigation/NavigationGraphs.kt")
    }

    @Test
    fun `reports routes remain wired to balance current invoices and weekly commissions`() {
        assertTrue(activity.contains("onNavigateBalance = onNavigateBalance"))
        assertTrue(activity.contains("onNavigateInvoiceList(\"current\")"))
        assertTrue(activity.contains("onNavigateWeeklyCommissions = onNavigateWeeklyCommissions"))

        assertTrue(navigation.contains("onNavigateBalance           = { navController.navigate(Screen.Balance.route) }"))
        assertTrue(navigation.contains("Screen.InvoiceList.createRoute(weekMode)"))
        assertTrue(navigation.contains("onNavigateWeeklyCommissions = { navController.navigate(Screen.WeeklyCommissions.route) }"))
    }

    @Test
    fun `competition history and win weeks remain active only`() {
        val winRoute = navigation.substringAfter("composable(Screen.WinWeeks.route)")
            .substringBefore("composable(Screen.WeeklyCommissions.route)")
        val historyRoute = navigation.substringAfter("composable(Screen.CompetitionHistory.route)")
            .substringBefore("composable(Screen.Profile.route)")

        assertTrue(winRoute.contains("competitionAvailability == CompetitionAvailability.ACTIVE"))
        assertTrue(winRoute.contains("WinWeeksScreen"))
        assertTrue(winRoute.contains("WeeklyCompetitionScreen"))
        assertTrue(historyRoute.contains("competitionAvailability == CompetitionAvailability.ACTIVE"))
        assertTrue(historyRoute.contains("CompetitionHistoryScreen"))
        assertTrue(historyRoute.contains("WeeklyCompetitionScreen"))
    }

    @Test
    fun `dependent report screens use consistent titles and back header`() {
        val invoices = projectFile("src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/InvoiceListScreen.kt")
        val commissions = projectFile("src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/WeeklyCommissionsScreen.kt")
        val wins = projectFile("src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/WinWeeksScreen.kt")
        val history = projectFile("src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/CompetitionHistoryScreen.kt")

        assertTrue(invoices.contains("title = \"فواتير هذا الأسبوع\""))
        assertTrue(commissions.contains("title = \"العمولات الأسبوعية\""))
        assertTrue(wins.contains("title = \"أسابيع الفوز\""))
        assertTrue(history.contains("title = \"سجل مشاركاتي\""))

        listOf(invoices, commissions, wins, history).forEach { source ->
            assertTrue(source.contains("ScreenHeader("))
            assertTrue(source.contains("onBack = onBack"))
        }
    }

    @Test
    fun `competition history preserves null rank as did not participate`() {
        val history = projectFile("src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/CompetitionHistoryScreen.kt")
        val nullRank = history.substringAfter("if (row.myRank == null)").substringBefore("} else {")

        assertTrue(nullRank.contains("\"لم تشارك\""))
        assertTrue(!nullRank.contains("#${'$'}{row.myRank}"))
    }

    @Test
    fun `remote competition subpages distinguish loading error and empty`() {
        val wins = projectFile("src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/WinWeeksScreen.kt")
        val history = projectFile("src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/CompetitionHistoryScreen.kt")

        listOf(wins, history).forEach { source ->
            assertTrue(source.contains("CircularProgressIndicator"))
            assertTrue(source.contains("ErrorScreen("))
            assertTrue(source.contains("errorMessage"))
            assertTrue(source.contains("إعادة المحاولة"))
        }
        assertTrue(wins.contains("لم تحصل على شارة زعيم الأسبوع بعد"))
        assertTrue(history.contains("لا توجد مشاركات سابقة"))
    }
}
