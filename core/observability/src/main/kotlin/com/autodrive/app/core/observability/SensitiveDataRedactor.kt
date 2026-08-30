package com.autodrive.app.core.observability

object SensitiveDataRedactor {
    private const val REDACTED = "[REDACTED]"

    private val sensitiveKeyFragments = setOf(
        "authorization",
        "user_id",
        "client_id",
        "org_id",
        "email",
        "access_token",
        "refresh_token",
        "token",
        "otp",
        "password",
        "secret",
        "anon_key",
        "api_key",
        "phone",
        "account",
        "bank",
        "payload",
        "message_body",
        "content",
        "note",
        "amount",
        "balance",
        "commission",
        "invoice_total",
    )

    private val bearerRegex = Regex("(?i)bearer\\s+[a-z0-9._~+/=-]+")
    private val jwtRegex = Regex("eyJ[a-zA-Z0-9_-]{8,}\\.[a-zA-Z0-9_-]{8,}\\.[a-zA-Z0-9_-]{8,}")
    private val phoneRegex = Regex("(?<![A-Za-z0-9])\\+?\\d{9,15}(?![A-Za-z0-9])")
    private val sensitiveAssignmentRegex = Regex(
        "(?i)\"?(otp|code|token|password|secret|authorization|api[_-]?key|user[_-]?id|client[_-]?id|org[_-]?id|email|phone|account|bank|payload|message_body|content|note|amount|balance|commission|invoice_total)\"?\\s*[:=]\\s*\"?[^,;}\\s\"]+\"?",
    )

    fun sanitizeFields(fields: Map<String, Any?>): Map<String, String> =
        fields.entries.associate { (key, value) ->
            val normalizedKey = key.trim().lowercase()
            val safeValue = if (sensitiveKeyFragments.any(normalizedKey::contains)) {
                REDACTED
            } else {
                sanitizeText(value?.toString().orEmpty()).take(MAX_FIELD_LENGTH)
            }
            key.take(MAX_KEY_LENGTH) to safeValue
        }

    fun sanitizeText(text: String): String = text
        .replace(bearerRegex, REDACTED)
        .replace(jwtRegex, REDACTED)
        .replace(sensitiveAssignmentRegex) { match ->
            val key = match.groupValues[1]
            "$key=$REDACTED"
        }
        .replace(phoneRegex, REDACTED)
        .take(MAX_MESSAGE_LENGTH)

    fun sanitizedThrowable(error: Throwable?): Throwable? {
        error ?: return null
        return SanitizedDiagnosticException(error.javaClass.simpleName).also {
            it.stackTrace = error.stackTrace
        }
    }

    private class SanitizedDiagnosticException(type: String) :
        RuntimeException("Diagnostic failure: $type")

    private const val MAX_KEY_LENGTH = 40
    private const val MAX_FIELD_LENGTH = 120
    private const val MAX_MESSAGE_LENGTH = 500
}
