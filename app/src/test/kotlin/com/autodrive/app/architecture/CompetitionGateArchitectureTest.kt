package com.autodrive.app.architecture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompetitionGateArchitectureTest {
    private val projectRoot: File = ProjectLayout.projectRoot
    private val appRoot = projectRoot.resolve("app/src/main/kotlin/com/autodrive/app")

    private fun source(path: String): String = appRoot.resolve(path).readText()

    @Test
    fun `home omits teaser for disabled and exposes locked and active copy`() {
        val home = source("feature/home/presentation/HomeScreen.kt")
        val teaser = source("feature/home/presentation/HomeSupportCards.kt")

        assertTrue(home.contains("competitionAvailability != CompetitionAvailability.DISABLED"))
        assertTrue(home.contains("CompetitionAvailability.LOCKED"))
        assertTrue(home.contains("قريباً"))
        assertTrue(home.contains("تحقق من مركزك هذا الأسبوع"))
        assertTrue(teaser.contains("text = description"))
    }

    @Test
    fun `reports competition cards render only for active`() {
        val reports = source("feature/reports/presentation/log/ActivityLogScreen.kt")
        val activeGate = reports.indexOf("if (competitionAvailability == CompetitionAvailability.ACTIVE)")
        val leader = reports.indexOf("label = \"شارة الزعيم\"")
        val competition = reports.indexOf("label = \"المسابقة الأسبوعية\"")

        assertTrue(activeGate >= 0)
        assertTrue(leader > activeGate)
        assertTrue(competition > activeGate)
    }

    @Test
    fun `locked and disabled competition shell keeps refresh behind active branch`() {
        val screen = source("feature/competition/presentation/WeeklyCompetitionScreen.kt")

        assertTrue(screen.contains("CompetitionAvailability.DISABLED"))
        assertTrue(screen.contains("المسابقة غير متاحة حالياً"))
        assertTrue(screen.contains("CompetitionAvailability.LOCKED"))
        assertTrue(screen.contains("نجهز منافسة عادلة وممتعة."))
        assertTrue(screen.contains("CompetitionAvailability.ACTIVE"))
        assertTrue(screen.contains("viewModel.onActiveEntry()"))
        assertTrue(screen.contains("WeeklyCompetitionViewModel"))
        assertTrue(screen.contains("hiltViewModel"))
        assertFalse(screen.contains("جاري تجهيز المسابقة"))
    }

    @Test
    fun `navigation fixes history route and guards direct competition routes`() {
        val graph = source("navigation/NavigationGraphs.kt")

        assertTrue(graph.contains("onNavigateCompetitionHistory= { navController.navigate(Screen.CompetitionHistory.route) }"))
        assertFalse(graph.contains("onNavigateCompetitionHistory= { navController.navigate(Screen.WeeklyCompetition.route) }"))
        assertTrue(graph.contains("composable(Screen.WinWeeks.route)"))
        assertTrue(graph.contains("composable(Screen.CompetitionHistory.route)"))
        assertTrue(graph.contains("competitionAvailability == CompetitionAvailability.ACTIVE"))
        assertTrue(graph.contains("availability = competitionAvailability"))
    }

    @Test
    fun `navigation view model owns one safe availability state and explicit refresh`() {
        val vm = source("navigation/AppNavigationViewModel.kt")
        val navigation = source("navigation/AppNavigation.kt")

        assertTrue(vm.contains("StateFlow<CompetitionAvailability>"))
        assertTrue(vm.contains("CompetitionAvailability.DISABLED"))
        assertTrue(vm.contains("refreshCompetitionAvailability()"))
        assertTrue(navigation.contains("navVm.competitionAvailability.collectAsState()"))
        assertTrue(navigation.contains("onRefreshCompetitionAvailability = navVm::refreshCompetitionAvailability"))
    }

    @Test
    fun `about and faq gate competition marketing`() {
        val about = source("feature/info/presentation/AboutAppScreen.kt")
        val faq = source("feature/info/presentation/FaqScreen.kt")

        assertTrue(about.contains("CompetitionAvailability.DISABLED -> Unit"))
        assertTrue(about.contains("CompetitionAvailability.LOCKED"))
        assertTrue(about.contains("ستتوفر لاحقاً"))
        assertTrue(faq.contains("COMPETITION_FAQ_QUESTIONS"))
        assertTrue(faq.contains("CompetitionAvailability.DISABLED -> withoutCompetition"))
        assertTrue(faq.contains("CompetitionAvailability.LOCKED -> withoutCompetition + FaqItem"))
    }
}
