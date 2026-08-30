package com.autodrive.app.core.observability

import com.autodrive.app.core.observability.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics

class FirebaseCrashlyticsReporter(
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance(),
    private val enabled: Boolean = BuildConfig.CRASH_REPORTING_ENABLED,
) : DiagnosticsReporter {

    init {
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
        if (enabled) {
            crashlytics.setCustomKey("environment", BuildConfig.ENVIRONMENT)
        }
    }

    override fun report(event: DiagnosticEvent) {
        if (!enabled || event.level == DiagnosticLevel.DEBUG) return

        event.fields.entries.take(MAX_CUSTOM_KEYS).forEach { (key, value) ->
            crashlytics.setCustomKey("obs_${key.take(36)}", value.take(100))
        }
        val run = event.syncRunId?.let { ":run=$it" }.orEmpty()
        crashlytics.log("${event.level}:${event.tag}:${event.message}$run")

        if (event.level == DiagnosticLevel.ERROR) {
            val failure = RuntimeException(
                "${event.tag}:${event.throwableType ?: "DiagnosticError"}",
            ).also { it.stackTrace = event.stackTrace.toTypedArray() }
            crashlytics.recordException(failure)
        }
    }

    private companion object {
        const val MAX_CUSTOM_KEYS = 16
    }
}
