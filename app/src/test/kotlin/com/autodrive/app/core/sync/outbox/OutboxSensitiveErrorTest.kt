package com.autodrive.app.core.sync.outbox

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutboxSensitiveErrorTest {
    @Test
    fun `persisted failure message removes financial and phone values`() {
        val message = OutboxErrorClassifier.errorMessage(
            IllegalStateException("amount=9000 phone=+249123456789"),
        )

        assertFalse(message.contains("9000"))
        assertFalse(message.contains("249123456789"))
        assertTrue(message.contains("[REDACTED]"))
    }

    @Test
    fun `error code keeps exception type without business payload`() {
        val code = OutboxErrorClassifier.classify(
            IllegalArgumentException("otp=123456"),
        ).errorCode

        assertTrue(code == "IllegalArgumentException")
        assertFalse(code.contains("123456"))
    }
}
