package com.autodrive.app.architecture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClosureCleanupArchitectureTest {
    private val root = ProjectLayout.projectRoot

    @Test
    fun `registration bridge is removed from app`() {
        assertFalse(root.resolve("app/src/main/kotlin/com/autodrive/app/di/RegistrationBridgeModule.kt").exists())
        assertFalse(root.resolve("feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/domain/repository/RegistrationProfileWriter.kt").exists())
    }

    @Test
    fun `registration port is owned by core common`() {
        val port = root.resolve(
            "core/common/src/main/kotlin/com/autodrive/app/core/common/registration/RegistrationProfileWriter.kt"
        )
        assertTrue(port.isFile)
        assertTrue(port.readText().contains("interface RegistrationProfileWriter"))
    }

    @Test
    fun `profile no longer depends on auth module`() {
        val build = root.resolve("feature/profile/build.gradle.kts").readText()
        assertFalse(build.contains("project(\":feature:auth\")"))
    }

    @Test
    fun `session compatibility clear alias is removed`() {
        val preferences = root.resolve(
            "core/session/src/main/kotlin/com/autodrive/app/core/session/data/PreferencesManager.kt"
        ).readText()
        assertFalse(preferences.contains("fun clear() = clearSession()"))
    }

    @Test
    fun `competition presentation state is explicit and screen uses view model`() {
        val presentation = root.resolve(
            "app/src/main/kotlin/com/autodrive/app/feature/competition/presentation"
        )
        val viewModel = presentation.resolve("WeeklyCompetitionViewModel.kt")
        val uiState = presentation.resolve("WeeklyCompetitionUiState.kt")
        val screen = presentation.resolve("WeeklyCompetitionScreen.kt")
        assertTrue(viewModel.isFile)
        assertTrue(uiState.isFile)
        assertTrue(screen.isFile)
        assertTrue(viewModel.readText().contains("@HiltViewModel"))
        assertTrue(screen.readText().contains("WeeklyCompetitionViewModel"))
        assertFalse(viewModel.readText().contains("supabase"))
        assertFalse(viewModel.readText().contains("Room"))
    }

    @Test
    fun `production sources contain no stale bridge imports`() {
        val offenders = ProjectLayout.allProductionFiles()
            .filter { file ->
                val text = file.readText()
                text.contains("feature.auth.domain.repository.RegistrationProfileWriter") ||
                    text.contains("RegistrationBridgeModule")
            }
            .map { it.relativeTo(root).path }
            .toList()
        assertTrue("Stale registration bridge references: $offenders", offenders.isEmpty())
    }
    @Test
    fun `wrapper uses stable Gradle compatible with AGP`() {
        val wrapper = root.resolve("gradle/wrapper/gradle-wrapper.properties").readText()
        assertTrue(wrapper.contains("gradle-8.7-bin.zip"))
        assertFalse(wrapper.contains("milestone"))
    }

}
