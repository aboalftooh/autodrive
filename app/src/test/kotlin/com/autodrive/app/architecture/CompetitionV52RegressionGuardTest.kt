package com.autodrive.app.architecture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v52 closes the competition phase with source-level regression guards only.
 * It intentionally does not change competition feature behavior.
 */
class CompetitionV52RegressionGuardTest {
    private val root = ProjectLayout.projectRoot
    private val appRoot = root.resolve("app/src/main/kotlin/com/autodrive/app")
    private val competitionRoot = appRoot.resolve("feature/competition")

    private fun app(path: String): String = appRoot.resolve(path).readText()

    @Test
    fun `competition feature contains no legacy polling or direct ranking sources`() {
        val forbidden = listOf(
            "startPolling",
            "delay(60_000",
            "fetchLeaderboardDirectly",
            "fetchCompetitionHistoryDirectly",
            "fetchWinWeeksDirectly",
            "currentFriday9AM",
            "postgrest[\"invoices\"]",
        )

        val offenders = competitionRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                val source = file.readText()
                forbidden.asSequence()
                    .filter(source::contains)
                    .map { token -> "${file.relativeTo(root).path}: $token" }
            }
            .toList()

        assertTrue("Forbidden competition regressions: $offenders", offenders.isEmpty())
    }

    @Test
    fun `server availability has exactly disabled locked active states`() {
        val model = app("feature/competition/domain/model/CompetitionAvailability.kt")
        val enumBody = model.substringAfter("enum class CompetitionAvailability {")
            .substringBefore("}")
        val states = enumBody
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        assertEquals(listOf("DISABLED", "LOCKED", "ACTIVE"), states)
    }

    @Test
    fun `disabled locked active matrix remains wired at presentation boundaries`() {
        val home = app("feature/home/presentation/HomeScreen.kt")
        val reports = app("feature/reports/presentation/log/ActivityLogScreen.kt")
        val screen = app("feature/competition/presentation/WeeklyCompetitionScreen.kt")
        val graph = app("navigation/NavigationGraphs.kt")

        // DISABLED: hidden from normal Home and Reports competition surfaces.
        assertTrue(home.contains("competitionAvailability != CompetitionAvailability.DISABLED"))
        assertTrue(reports.contains("competitionAvailability == CompetitionAvailability.ACTIVE"))

        // LOCKED: Home teaser only, direct competition routes render locked content.
        assertTrue(home.contains("CompetitionAvailability.LOCKED"))
        assertTrue(home.contains("قريباً"))
        assertTrue(screen.contains("CompetitionAvailability.LOCKED -> LockedCompetitionContent"))

        // ACTIVE: full competition screen and protected secondary routes.
        assertTrue(screen.contains("CompetitionAvailability.ACTIVE ->"))
        assertTrue(screen.contains("viewModel.onActiveEntry()"))
        assertTrue(graph.contains("composable(Screen.WinWeeks.route)"))
        assertTrue(graph.contains("composable(Screen.CompetitionHistory.route)"))
        assertTrue(graph.contains("competitionAvailability == CompetitionAvailability.ACTIVE"))
    }

    @Test
    fun `leaderboard refresh cannot run from disabled or locked branches`() {
        val screen = app("feature/competition/presentation/WeeklyCompetitionScreen.kt")
        val activeBranch = screen.substringAfter("CompetitionAvailability.ACTIVE ->")
            .substringBefore("            }\n        }\n    }")

        assertTrue(activeBranch.contains("viewModel.onActiveEntry()"))

        val beforeActive = screen.substringBefore("CompetitionAvailability.ACTIVE ->")
        assertFalse(beforeActive.contains("onActiveEntry()"))
        assertFalse(beforeActive.contains("viewModel.refresh"))
        assertFalse(beforeActive.contains("observeWeeklyCompetition.refresh"))
    }

    @Test
    fun `feature flag remains server controlled and android read only`() {
        val migration = root.resolve(
            "supabase/migrations/20260813070000_weekly_competition_feature_gate.sql"
        ).readText().lowercase()
        val repository = app("feature/competition/data/CompetitionAvailabilityRepositoryImpl.kt")

        assertTrue(migration.contains("grant select on table public.autodrive_feature_flags to anon, authenticated"))
        assertTrue(migration.contains("for select\nto anon, authenticated"))
        assertTrue(migration.contains("values ('weekly_competition', 'disabled')"))
        assertFalse(migration.contains("grant update on table public.autodrive_feature_flags"))
        assertFalse(migration.contains("grant insert on table public.autodrive_feature_flags"))
        assertFalse(migration.contains("grant delete on table public.autodrive_feature_flags"))

        assertTrue(repository.contains("postgrest[\"autodrive_feature_flags\"]"))
        assertTrue(repository.contains("eq(\"feature_key\", WEEKLY_COMPETITION_FEATURE_KEY)"))
        assertFalse(repository.contains("update("))
        assertFalse(repository.contains("insert("))
        assertFalse(repository.contains("delete("))
    }

    @Test
    fun `winner and rank remain independent from rollout flag`() {
        val availabilityRepository = app("feature/competition/data/CompetitionAvailabilityRepositoryImpl.kt")
        val competitionRepository = app("feature/competition/data/WeeklyCompetitionRepositoryImpl.kt")

        assertFalse(availabilityRepository.contains("get_weekly_competition"))
        assertFalse(availabilityRepository.contains("get_my_competition_history"))
        assertFalse(availabilityRepository.contains("get_my_win_weeks"))
        assertFalse(availabilityRepository.contains("rank"))
        assertFalse(availabilityRepository.contains("winner"))

        assertTrue(competitionRepository.contains("rpc(\"get_weekly_competition\")"))
        assertFalse(competitionRepository.contains("CompetitionAvailability"))
    }
}
