package com.autodrive.app.core.observability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveDataRedactorTest {

    @Test
    fun `jwt bearer and phone values are removed from text`() {
        val jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.signatureValue"
        val result = SensitiveDataRedactor.sanitizeText(
            "Authorization: Bearer $jwt phone=+249123456789",
        )

        assertFalse(result.contains(jwt))
        assertFalse(result.contains("249123456789"))
        assertTrue(result.contains("[REDACTED]"))
    }

    @Test
    fun `otp and financial assignments are removed from text`() {
        val result = SensitiveDataRedactor.sanitizeText(
            "otp=123456 amount:9000 balance=12000 commission=300",
        )

        assertFalse(result.contains("123456"))
        assertFalse(result.contains("9000"))
        assertFalse(result.contains("12000"))
        assertFalse(result.contains("300"))
    }

    @Test
    fun `sensitive fields are replaced regardless of value shape`() {
        val result = SensitiveDataRedactor.sanitizeFields(
            mapOf(
                "token" to "opaque-token",
                "bank_account" to "123456",
                "amount" to "55.40",
                "phase" to "CHAT",
            ),
        )

        assertEquals("[REDACTED]", result["token"])
        assertEquals("[REDACTED]", result["bank_account"])
        assertEquals("[REDACTED]", result["amount"])
        assertEquals("CHAT", result["phase"])
    }

    @Test
    fun `safe operational metrics remain visible`() {
        val result = SensitiveDataRedactor.sanitizeFields(
            mapOf(
                "duration_ms" to 125,
                "failure_count" to 2,
                "state" to "CONNECTED",
            ),
        )

        assertEquals("125", result["duration_ms"])
        assertEquals("2", result["failure_count"])
        assertEquals("CONNECTED", result["state"])
    }

    @Test
    fun `throwable message is not copied`() {
        val source = IllegalStateException("otp=654321 amount=900")
        val safe = SensitiveDataRedactor.sanitizedThrowable(source)

        assertTrue(safe != null)
        assertFalse(safe!!.message.orEmpty().contains("654321"))
        assertFalse(safe.message.orEmpty().contains("900"))
        assertEquals(source.stackTrace.toList(), safe.stackTrace.toList())
    }
    @Test
    fun `raw scope identities and nested diagnostic text are removed`() {
        val fields = SensitiveDataRedactor.sanitizeFields(
            mapOf(
                "user_id" to "user-secret-123",
                "client_id" to "client-secret-456",
                "org_id" to "org-secret-789",
                "scope_fingerprint" to "ab12cd34ef56",
                "error" to "authorization=Bearer abcdefghijklmnopqrstuvwxyz user_id=user-secret-123",
            ),
        )

        assertEquals("[REDACTED]", fields["user_id"])
        assertEquals("[REDACTED]", fields["client_id"])
        assertEquals("[REDACTED]", fields["org_id"])
        assertEquals("ab12cd34ef56", fields["scope_fingerprint"])
        assertFalse(fields["error"].orEmpty().contains("user-secret-123"))
        assertFalse(fields["error"].orEmpty().contains("Bearer"))
    }


    @Test
    fun scopeFingerprint_remainsVisibleWhileStandalonePhoneIsRedacted() {
        val fingerprint = "abcdef0123456789abcd"
        val safe = SensitiveDataRedactor.sanitizeFields(
            mapOf("scope_fingerprint" to fingerprint, "phone" to "+249912345678"),
        )
        assertEquals(fingerprint, safe["scope_fingerprint"])
        assertEquals("[REDACTED]", safe["phone"])
    }
}
