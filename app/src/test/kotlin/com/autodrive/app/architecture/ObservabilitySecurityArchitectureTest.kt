package com.autodrive.app.architecture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObservabilitySecurityArchitectureTest {
    private val moduleDir: File = sequenceOf(
        File(System.getProperty("user.dir")),
        File(System.getProperty("user.dir"), "app"),
    ).first { File(it, "src/main").isDirectory }

    private val projectDir = moduleDir.parentFile
    private val mainRoot = ProjectLayout.mergedAppRoot

    @Test
    fun `sms permissions are removed`() {
        val manifest = moduleDir.resolve("src/main/AndroidManifest.xml").readText()
        assertFalse(manifest.contains("READ_SMS"))
        assertFalse(manifest.contains("RECEIVE_SMS"))
    }

    @Test
    fun `otp autofill uses sms retriever only`() {
        val coordinator = mainRoot
            .resolve("feature/auth/data/sms/SmsOtpAutofillCoordinator.kt")
            .readText()
        val screen = mainRoot
            .resolve("feature/auth/presentation/login/OtpInputScreen.kt")
            .readText()
        val combined = coordinator + "\n" + screen

        assertTrue(coordinator.contains("SmsRetriever.SMS_RETRIEVED_ACTION"))
        assertTrue(coordinator.contains("startSmsRetriever()"))
        assertTrue(coordinator.contains("startSmsUserConsent(null)"))
        assertFalse(combined.contains("Telephony.SMS_RECEIVED"))
        assertFalse(combined.contains("SmsMessage.createFromPdu"))
    }

    @Test
    fun `release crash reporting is installed`() {
        val app = mainRoot.resolve("AutoDriveApp.kt").readText()
        val build = projectDir.resolve("core/observability/build.gradle.kts").readText()
        assertTrue(app.contains("FirebaseCrashlyticsReporter"))
        assertTrue(build.contains("libs.firebase.crashlytics"))
        assertTrue(build.contains("CRASH_REPORTING_ENABLED"))
    }

    @Test
    fun `structured logger redacts before reporting`() {
        val logger = mainRoot.resolve("core/observability/AppLogger.kt").readText()
        assertTrue(logger.contains("SensitiveDataRedactor.sanitizeText"))
        assertTrue(logger.contains("SensitiveDataRedactor.sanitizeFields"))
        assertTrue(logger.indexOf("sanitizeFields") < logger.indexOf("reporter.report"))
    }

    @Test
    fun `sync diagnostics cover required production signals`() {
        val diagnostics = mainRoot.resolve("core/sync/diagnostics/SyncDiagnostics.kt").readText()
        val coordinator = mainRoot.resolve("core/sync/data/DefaultSyncCoordinator.kt").readText()
        val steps = mainRoot.resolve("core/sync/data/SyncStepExecutor.kt").readText()
        val realtime = mainRoot.resolve("core/sync/realtime/RealtimeManager.kt").readText()
        val outbox = mainRoot.resolve("core/sync/data/OutboxSynchronizer.kt").readText()

        assertTrue(coordinator.contains("diagnostics.syncStarted"))
        assertTrue(coordinator.contains("diagnostics.syncFinished"))
        assertTrue(steps.contains("diagnostics.phaseFinished"))
        assertTrue(realtime.contains("diagnostics.realtimeState"))
        assertTrue(outbox.contains("diagnostics.outboxState"))
        assertTrue(diagnostics.contains("last_success_at"))
    }

    @Test
    fun `environment values are not hardcoded in build script`() {
        val build = listOf(
            moduleDir.resolve("build.gradle.kts"),
            projectDir.resolve("core/network/build.gradle.kts"),
            projectDir.resolve("core/platform/build.gradle.kts"),
        ).joinToString("\n") { it.readText() }
        assertFalse(build.contains("madkfvggyolmdberzmtb.supabase.co"))
        assertFalse(build.contains("eyJhbGciOiJIUzI1Ni"))
        assertTrue(build.contains("configurationValue"))
        assertTrue(projectDir.resolve("local.properties.example").isFile)
    }

    @Test
    fun `raw android logging is confined to logger and debug sms hash utility`() {
        val offenders = mainRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file ->
                file.name !in setOf("AppLogger.kt", "SmsHashLogger.kt") &&
                    Regex("\\bLog\\.(d|i|w|e|v)\\(").containsMatchIn(file.readText())
            }
            .toList()
        assertTrue("Raw Log usage: $offenders", offenders.isEmpty())
    }

    @Test
    fun `rls verification contract is versioned`() {
        val review = projectDir.resolve("docs/refactor/rls-review-v11.md")
        val verifier = projectDir.resolve("tools/verify_rls_v11.sql")
        assertTrue(review.isFile)
        assertTrue(verifier.isFile)
        assertTrue(verifier.readText().contains("pg_policies"))
        assertTrue(verifier.readText().contains("relrowsecurity"))
    }
}
