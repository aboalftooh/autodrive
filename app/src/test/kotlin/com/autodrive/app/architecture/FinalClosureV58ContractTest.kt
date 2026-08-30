package com.autodrive.app.architecture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Final cross-feature regression contract for AutoDrive v58. No production behavior is introduced here. */
class FinalClosureV58ContractTest {
    private val root = ProjectLayout.projectRoot
    private val appRoot = root.resolve("app/src/main/kotlin/com/autodrive/app")

    private fun app(path: String): String = appRoot.resolve(path).readText()

    @Test
    fun `competition remains server ranked gated and free from local fallbacks`() {
        val repository = app("feature/competition/data/WeeklyCompetitionRepositoryImpl.kt")
        val availability = app("feature/competition/data/CompetitionAvailabilityRepositoryImpl.kt")
        val home = app("feature/home/presentation/HomeScreen.kt")
        val screen = app("feature/competition/presentation/WeeklyCompetitionScreen.kt")

        listOf(
            "startPolling",
            "fetchLeaderboardDirectly",
            "currentFriday9AM",
            "postgrest[\"invoices\"]",
        ).forEach { assertFalse("Forbidden competition token: $it", repository.contains(it)) }

        assertTrue(repository.contains("rpc(\"get_weekly_competition\")"))
        assertTrue(repository.contains("rpc(\"get_my_competition_history\""))
        assertTrue(repository.contains("rpc(\"get_my_win_weeks\")"))
        assertFalse(repository.contains("CompetitionAvailability"))
        assertTrue(availability.contains("postgrest[\"autodrive_feature_flags\"]"))
        assertTrue(home.contains("competitionAvailability != CompetitionAvailability.DISABLED"))
        assertTrue(home.contains("CompetitionAvailability.LOCKED"))
        assertTrue(screen.contains("CompetitionAvailability.ACTIVE ->"))
    }

    @Test
    fun `reports retain weekly semantics hierarchy and no eligibility implementation`() {
        val viewModel = app("feature/reports/presentation/log/ReportsViewModel.kt")
        val screen = app("feature/reports/presentation/log/ActivityLogScreen.kt")

        assertTrue(viewModel.contains("val currentWeekStart = summary.weekStartMs"))
        assertTrue(viewModel.contains("currentWeekPurchases"))
        assertTrue(viewModel.contains("previousWeekPurchases"))
        assertTrue(viewModel.contains("currentWeekCommissions"))
        assertTrue(viewModel.contains("previousWeekCommissions"))
        assertTrue(viewModel.contains("BigDecimal"))
        assertTrue(viewModel.contains("RoundingMode.HALF_UP"))
        assertFalse(viewModel.contains("commission_eligibility"))
        assertFalse(viewModel.contains("calculateEligibility"))
        assertFalse(viewModel.contains("isEligible"))

        listOf(
            "هذا الأسبوع",
            "مقارنة بالأسبوع السابق",
            "الحالة المالية",
            "التفاصيل",
            "منذ انضمامك",
        ).forEach { assertTrue("Missing reports section: $it", screen.contains(it)) }
        assertTrue(screen.contains("competitionAvailability == CompetitionAvailability.ACTIVE"))
        assertFalse(screen.contains("Color(0x"))
    }

    @Test
    fun `settings retain section nullable payout workshop target and logout semantics`() {
        val uiState = root.resolve("feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileUiState.kt").readText()
        val viewModel = root.resolve("feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileViewModel.kt").readText()
        val repository = root.resolve("feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/data/ProfileRepositoryImpl.kt").readText()
        val screen = root.resolve("feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt").readText()

        assertTrue(uiState.contains("editingSection: ProfileEditSection?"))
        assertFalse(uiState.contains("isEditing"))
        assertTrue(viewModel.contains("bankName.trim().ifBlank { null }"))
        assertTrue(viewModel.contains("bankAccount.trim().ifBlank { null }"))
        assertTrue(viewModel.contains("current.accountType != AccountType.WORKSHOP_OWNER"))
        assertTrue(viewModel.contains("dashboardPreferences.weeklyTarget = clamped"))
        assertTrue(viewModel.contains("SignOutAction"))
        assertTrue(screen.contains("KeyboardType.Ascii"))
        assertFalse(screen.contains("AutoDriveNumericField(bankAccount"))

        assertTrue(repository.contains("syncStatus   = \"PENDING\""))
        assertTrue(repository.contains("db.withTransaction"))
        assertTrue(repository.contains("PendingOperationEntity("))
        assertTrue(repository.contains("OutboxOperationType.UPDATE_PROFILE"))
        assertTrue(repository.contains("mutationId = mutationId"))
    }

    @Test
    fun `feature domain and presentation remain infrastructure free and concrete data does not cross features`() {
        val forbiddenInfrastructure = listOf(
            "com.autodrive.app.core.database",
            "AutoDriveDatabase",
            "io.github.jan.supabase",
            "androidx.work",
            "com.google.firebase",
        )
        val featureFiles = root.resolve("feature").walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.invariantSeparatorsPath.contains("/src/main/kotlin/") }
            .toList() + appRoot.resolve("feature").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

        val infrastructureOffenders = featureFiles.filter { file ->
            val path = file.invariantSeparatorsPath
            (path.contains("/domain/") || path.contains("/presentation/")) &&
                forbiddenInfrastructure.any(file.readText()::contains)
        }
        assertTrue("Infrastructure leaked into Domain/Presentation: $infrastructureOffenders", infrastructureOffenders.isEmpty())

        val crossData = featureFiles.flatMap { file ->
            val normalized = file.invariantSeparatorsPath
            val owner = normalized.substringAfter("/feature/", "").substringBefore('/')
            file.readLines().filter { line ->
                line.startsWith("import com.autodrive.app.feature.") &&
                    line.contains(".data.") &&
                    owner.isNotBlank() &&
                    !line.startsWith("import com.autodrive.app.feature.$owner.")
            }.map { "${file.relativeTo(root).path}: $it" }
        }
        assertTrue("Cross-feature data imports: $crossData", crossData.isEmpty())
    }

    @Test
    fun `navigation routes required by closed features remain present`() {
        val destinations = app("navigation/AppDestinations.kt")
        val graphs = app("navigation/NavigationGraphs.kt")
        listOf(
            "home",
            "profile",
            "balance",
            "weekly_competition",
            "invoice_list?weekMode={weekMode}",
            "win_weeks",
            "weekly_commissions",
            "competition_history",
            "about_app",
            "privacy_policy",
            "faq",
        ).forEach { assertTrue("Missing route: $it", destinations.contains("\"$it\"")) }
        listOf(
            "Screen.Home.route",
            "Screen.Profile.route",
            "Screen.Balance.route",
            "Screen.WeeklyCompetition.route",
            "Screen.InvoiceList.route",
            "Screen.WinWeeks.route",
            "Screen.WeeklyCommissions.route",
            "Screen.CompetitionHistory.route",
            "Screen.AboutApp.route",
            "Screen.PrivacyPolicy.route",
            "Screen.Faq.route",
        ).forEach { assertTrue("Missing navigation destination: $it", graphs.contains(it)) }
    }

    @Test
    fun `android production code contains no service role credential path`() {
        val offenders = ProjectLayout.allProductionFiles().flatMap { file ->
            file.readLines().asSequence().mapIndexedNotNull { index, raw ->
                val line = raw.trim()
                if (line.startsWith("//") || line.startsWith("*") || line.startsWith("/*")) return@mapIndexedNotNull null
                val lowered = line.lowercase()
                if (lowered.contains("service_role") ||
                    lowered.contains("supabase_service") ||
                    lowered.contains("jwt_secret")) {
                    "${file.relativeTo(root).path}:${index + 1}:$line"
                } else null
            }
        }.toList()
        assertTrue("Service-role credential path found: $offenders", offenders.isEmpty())
    }

    @Test
    fun `gradle sdk and wrapper versions remain locked to v57`() {
        val versions = root.resolve("gradle/libs.versions.toml").readText()
        val wrapper = root.resolve("gradle/wrapper/gradle-wrapper.properties").readText()
        val appBuild = root.resolve("app/build.gradle.kts").readText()

        assertTrue(versions.contains("agp = \"8.5.2\""))
        assertTrue(versions.contains("kotlin = \"2.0.21\""))
        assertTrue(wrapper.contains("gradle-8.7-bin.zip"))
        assertTrue(appBuild.contains("compileSdk = 35"))
        assertTrue(appBuild.contains("minSdk = 26"))
        assertTrue(appBuild.contains("targetSdk = 35"))
    }
}
