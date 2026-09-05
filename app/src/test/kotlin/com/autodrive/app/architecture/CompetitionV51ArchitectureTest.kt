package com.autodrive.app.architecture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompetitionV51ArchitectureTest {
    private val root = ProjectLayout.projectRoot
    private fun app(path: String) = root.resolve("app/src/main/kotlin/com/autodrive/app/$path").readText()

    @Test
    fun `competition ranking and history use server RPCs only`() {
        val repository = app("feature/competition/data/WeeklyCompetitionRepositoryImpl.kt")
        assertTrue(repository.contains("rpc(\"get_weekly_competition\")"))
        assertTrue(repository.contains("rpc(\"get_my_competition_history\""))
        assertTrue(repository.contains("rpc(\"get_my_win_weeks\")"))
        listOf(
            "fetchLeaderboardDirectly",
            "fetchCompetitionHistoryDirectly",
            "fetchWinWeeksDirectly",
            "postgrest[\"invoices\"]",
            "weekly_competition_results",
            "weekly_competition_weeks",
            "currentFriday9AM",
            "WEEK_MS",
        ).forEach { forbidden -> assertFalse("Forbidden source: $forbidden", repository.contains(forbidden)) }
    }

    @Test
    fun `repository has no polling or repository owned coroutine scope`() {
        val repository = app("feature/competition/data/WeeklyCompetitionRepositoryImpl.kt")
        assertFalse(repository.contains("startPolling"))
        assertFalse(repository.contains("delay(60_000"))
        assertFalse(repository.contains("CoroutineScope("))
        assertFalse(repository.contains("SupervisorJob"))
    }

    @Test
    fun `rpc failure cannot clear cached leaderboard before success`() {
        val repository = app("feature/competition/data/WeeklyCompetitionRepositoryImpl.kt")
        val fetch = repository.indexOf("fetchLeaderboardByRpc()")
        val cache = repository.indexOf("cacheLeaderboard(leaderboard)")
        assertTrue(fetch >= 0)
        assertTrue(cache > fetch)
        assertTrue(repository.contains("throw error"))
        assertFalse(repository.substring(0, fetch).contains("weeklyLeaderboardDao().clear()"))
    }

    @Test
    fun `history preserves null rank and win count is nullable`() {
        val repository = app("feature/competition/data/WeeklyCompetitionRepositoryImpl.kt")
        val model = app("feature/competition/domain/model/WeeklyCompetition.kt")
        val history = app("feature/reports/presentation/log/CompetitionHistoryScreen.kt")
        assertTrue(repository.contains("myRank = dto.myRank?.toInt()"))
        assertFalse(repository.contains("dto.myRank?.toInt() ?: return@mapNotNull null"))
        assertTrue(model.contains("val myWinCount: Int?"))
        assertTrue(history.contains("لم تشارك"))
        assertFalse(history.contains("#null"))
    }

    @Test
    fun `active screen implements personal hero top five cache and actions`() {
        val screen = app("feature/competition/presentation/WeeklyCompetitionScreen.kt")
        assertTrue(screen.contains("مركزك هذا الأسبوع"))
        assertTrue(screen.contains("لم تدخل المنافسة بعد"))
        assertTrue(screen.contains("ordered.take(5)"))
        assertTrue(screen.contains("it.rank > 5"))
        assertTrue(screen.contains("آخر ترتيب محفوظ"))
        assertTrue(screen.contains("سجل مشاركاتي"))
        assertTrue(screen.contains("أسابيع الفوز"))
        assertTrue(screen.contains("PullToRefreshBox"))
        assertTrue(screen.contains("difference?.takeIf { it.isPositive() }"))
        assertFalse(screen.contains("Color(0x"))
    }

    @Test
    fun `view model owns loading refresh cache warning and infrastructure boundary`() {
        val vm = app("feature/competition/presentation/WeeklyCompetitionViewModel.kt")
        val state = app("feature/competition/presentation/WeeklyCompetitionUiState.kt")
        assertTrue(state.contains("val isLoading: Boolean = true"))
        assertTrue(state.contains("val isRefreshing: Boolean = false"))
        assertTrue(state.contains("val data: WeeklyCompetitionData? = null"))
        assertTrue(state.contains("val errorMessage: String? = null"))
        assertTrue(vm.contains("viewModelScope"))
        assertTrue(vm.contains("onActiveEntry"))
        assertTrue(vm.contains("errorMessage = error.message"))
        assertFalse(vm.contains("supabase"))
        assertFalse(vm.contains("DataStore"))
        assertFalse(vm.contains("Room"))
    }

    @Test
    fun `navigation wires active competition actions to original routes`() {
        val graph = app("navigation/NavigationGraphs.kt")
        assertTrue(graph.contains("onNavigateCompetitionHistory = { navController.navigate(Screen.CompetitionHistory.route) }"))
        assertTrue(graph.contains("onNavigateWinWeeks = { navController.navigate(Screen.WinWeeks.route) }"))
    }
}
