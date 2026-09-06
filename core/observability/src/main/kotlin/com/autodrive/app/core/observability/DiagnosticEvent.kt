package com.autodrive.app.core.observability

enum class DiagnosticLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
}

data class DiagnosticEvent(
    val level: DiagnosticLevel,
    val tag: String,
    val message: String,
    val fields: Map<String, String> = emptyMap(),
    val throwableType: String? = null,
    val syncRunId: String? = null,
    val stackTrace: List<StackTraceElement> = emptyList(),
)

fun interface DiagnosticsReporter {
    fun report(event: DiagnosticEvent)
}
