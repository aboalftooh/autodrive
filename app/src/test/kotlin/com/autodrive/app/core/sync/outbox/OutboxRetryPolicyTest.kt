package com.autodrive.app.core.sync.outbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OutboxRetryPolicyTest {

    private val policy = OutboxRetryPolicy(
        maxAttempts = 3,
        baseDelayMillis = 1_000L,
        maxDelayMillis = 8_000L,
        randomUnit = { 0.5 },
    )

    @Test
    fun transientFailure_usesExponentialBackoff() {
        val first = policy.onFailure(
            currentAttemptCount = 0,
            nowMillis = 10_000L,
            error = TypedOutboxException(OutboxFailureCategory.TRANSIENT, "HTTP_503"),
        )
        val second = policy.onFailure(
            currentAttemptCount = 1,
            nowMillis = 10_000L,
            error = TypedOutboxException(OutboxFailureCategory.TRANSIENT, "HTTP_503"),
        )

        assertEquals(PendingOperationStatus.PENDING, first.status)
        assertEquals(1, first.attemptCount)
        assertEquals(11_000L, first.nextRetryAt)
        assertEquals(12_000L, second.nextRetryAt)
    }

    @Test
    fun maxAttempt_movesOperationToDeadLetter() {
        val decision = policy.onFailure(
            currentAttemptCount = 2,
            nowMillis = 10_000L,
            error = TypedOutboxException(OutboxFailureCategory.TRANSIENT, "HTTP_503"),
        )

        assertEquals(PendingOperationStatus.DEAD_LETTER, decision.status)
        assertEquals(3, decision.attemptCount)
        assertEquals(Long.MAX_VALUE, decision.nextRetryAt)
    }

    @Test
    fun permanentFailure_isNotRetried() {
        val decision = policy.onFailure(
            currentAttemptCount = 0,
            nowMillis = 10_000L,
            error = PermanentOutboxException("Unsupported payload version"),
        )

        assertEquals(PendingOperationStatus.DEAD_LETTER, decision.status)
        assertEquals(1, decision.attemptCount)
        assertTrue(decision.errorMessage.contains("Unsupported payload"))
    }

    @Test
    fun ambiguousFailure_neverDeadLettersOnlyBecauseAttemptLimitWasReached() {
        val decision = policy.onFailure(
            currentAttemptCount = 99,
            nowMillis = 10_000L,
            error = IllegalStateException("opaque failure text"),
        )

        assertEquals(PendingOperationStatus.PENDING, decision.status)
        assertEquals(OutboxFailureCategory.AMBIGUOUS, decision.category)
    }

    @Test
    fun httpStatusMapping_isTypedAndDeterministic() {
        assertEquals(OutboxFailureCategory.AUTH, OutboxErrorClassifier.categoryForHttpStatus(401))
        assertEquals(OutboxFailureCategory.PERMISSION, OutboxErrorClassifier.categoryForHttpStatus(403))
        assertEquals(OutboxFailureCategory.CONFLICT, OutboxErrorClassifier.categoryForHttpStatus(409))
        assertEquals(OutboxFailureCategory.VALIDATION, OutboxErrorClassifier.categoryForHttpStatus(422))
        assertEquals(OutboxFailureCategory.TRANSIENT, OutboxErrorClassifier.categoryForHttpStatus(503))
    }
}
