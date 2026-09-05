package com.autodrive.app.architecture

import com.autodrive.app.feature.competition.data.CompetitionAvailabilityRefreshResult
import com.autodrive.app.feature.competition.data.parseCompetitionAvailability
import com.autodrive.app.feature.competition.data.toCacheUpdateOrNull
import com.autodrive.app.feature.competition.data.remote.dto.CompetitionAvailabilityDto
import com.autodrive.app.feature.competition.domain.model.CompetitionAvailability
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompetitionAvailabilityArchitectureTest {

    private val projectRoot: File = ProjectLayout.projectRoot
    private val competitionRoot = projectRoot.resolve(
        "app/src/main/kotlin/com/autodrive/app/feature/competition"
    )
    private val repositorySource = competitionRoot.resolve(
        "data/CompetitionAvailabilityRepositoryImpl.kt"
    )
    private val migration = projectRoot.resolve(
        "supabase/migrations/20260813070000_weekly_competition_feature_gate.sql"
    )

    @Test
    fun `unknown state uses disabled safe default`() {
        assertEquals(CompetitionAvailability.DISABLED, parseCompetitionAvailability("UNKNOWN"))
    }

    @Test
    fun `missing row uses disabled safe default`() {
        val update = CompetitionAvailabilityRefreshResult.RemoteSuccess(null)
            .toCacheUpdateOrNull()

        assertEquals(CompetitionAvailability.DISABLED, update?.availability)
        assertNull(update?.updatedAt)
    }

    @Test
    fun `cached locked remains unchanged on network failure`() {
        val cached = CompetitionAvailability.LOCKED
        val update = CompetitionAvailabilityRefreshResult.NetworkFailure.toCacheUpdateOrNull()
        val resolved = update?.availability ?: cached

        assertEquals(CompetitionAvailability.LOCKED, resolved)
    }

    @Test
    fun `cached active remains unchanged on network failure`() {
        val cached = CompetitionAvailability.ACTIVE
        val update = CompetitionAvailabilityRefreshResult.NetworkFailure.toCacheUpdateOrNull()
        val resolved = update?.availability ?: cached

        assertEquals(CompetitionAvailability.ACTIVE, resolved)
    }

    @Test
    fun `remote success produces cache update`() {
        val update = CompetitionAvailabilityRefreshResult.RemoteSuccess(
            CompetitionAvailabilityDto(
                featureKey = "weekly_competition",
                state = "ACTIVE",
                updatedAt = "2026-08-13T07:00:00Z"
            )
        ).toCacheUpdateOrNull()

        assertEquals(CompetitionAvailability.ACTIVE, update?.availability)
        assertEquals("2026-08-13T07:00:00Z", update?.updatedAt)
    }

    @Test
    fun `android has read only feature flag grant`() {
        val sql = migration.readText().lowercase()

        assertTrue(sql.contains("revoke all on table public.autodrive_feature_flags from anon, authenticated"))
        assertTrue(sql.contains("grant select on table public.autodrive_feature_flags to anon, authenticated"))
        assertTrue(sql.contains("for select\nto anon, authenticated"))
        assertFalse(sql.contains("grant insert on table public.autodrive_feature_flags"))
        assertFalse(sql.contains("grant update on table public.autodrive_feature_flags"))
        assertFalse(sql.contains("grant delete on table public.autodrive_feature_flags"))
        assertFalse(sql.contains("for insert\nto anon, authenticated"))
        assertFalse(sql.contains("for update\nto anon, authenticated"))
        assertFalse(sql.contains("for delete\nto anon, authenticated"))
    }

    @Test
    fun `feature gate repository does not depend on session`() {
        val source = repositorySource.readText()

        assertFalse(source.contains("SessionReader"))
        assertFalse(source.contains("currentSession"))
    }

    @Test
    fun `competition presentation does not import supabase or datastore`() {
        val presentationRoot = competitionRoot.resolve("presentation")
        val offenders = presentationRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file ->
                val source = file.readText()
                source.contains("io.github.jan.supabase") ||
                    source.contains("androidx.datastore")
            }
            .toList()

        assertTrue("Presentation infrastructure imports: $offenders", offenders.isEmpty())
    }
}
