package com.autodrive.app.core.observability

import android.util.Log
import com.autodrive.app.core.observability.BuildConfig
import com.autodrive.app.core.observability.DiagnosticEvent
import com.autodrive.app.core.observability.DiagnosticLevel
import com.autodrive.app.core.observability.DiagnosticsReporter
import com.autodrive.app.core.observability.SensitiveDataRedactor

object AppLogger {
    @Volatile
    private var reporter: DiagnosticsReporter = DiagnosticsReporter { }

    fun install(reporter: DiagnosticsReporter) {
        this.reporter = reporter
    }

    fun d(tag: String, msg: String, fields: Map<String, Any?> = emptyMap()) {
        log(DiagnosticLevel.DEBUG, tag, msg, null, fields)
    }

    fun event(tag: String, name: String, fields: Map<String, Any?> = emptyMap()) {
        log(DiagnosticLevel.INFO, tag, name, null, fields)
    }

    fun w(tag: String, msg: String, fields: Map<String, Any?> = emptyMap()) {
        log(DiagnosticLevel.WARNING, tag, msg, null, fields)
    }

    fun e(
        tag: String,
        msg: String,
        throwable: Throwable? = null,
        fields: Map<String, Any?> = emptyMap(),
    ) {
        log(DiagnosticLevel.ERROR, tag, msg, throwable, fields)
    }

    private fun log(
        level: DiagnosticLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
        fields: Map<String, Any?>,
    ) {
        val safeTag = tag.take(40)
        val safeMessage = SensitiveDataRedactor.sanitizeText(message)
        val safeFields = SensitiveDataRedactor.sanitizeFields(fields)
        val safeThrowable = SensitiveDataRedactor.sanitizedThrowable(throwable)

        if (BuildConfig.DEBUG) {
            val rendered = if (safeFields.isEmpty()) safeMessage else "$safeMessage $safeFields"
            when (level) {
                DiagnosticLevel.DEBUG, DiagnosticLevel.INFO -> Log.d(safeTag, rendered)
                DiagnosticLevel.WARNING -> Log.w(safeTag, rendered)
                DiagnosticLevel.ERROR -> Log.e(safeTag, rendered, safeThrowable)
            }
        }

        runCatching {
            reporter.report(
                DiagnosticEvent(
                    level = level,
                    tag = safeTag,
                    message = safeMessage,
                    fields = safeFields,
                    throwableType = throwable?.javaClass?.simpleName,
                    syncRunId = safeFields["sync_run_id"],
                    stackTrace = throwable?.stackTrace?.toList().orEmpty(),
                ),
            )
        }
    }
}
