package com.autodrive.app.core.sync.outbox

import com.autodrive.app.core.observability.SensitiveDataRedactor
import java.io.IOException
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

@Deprecated("Use OUTBOX_CONTRACT_VERSION")
const val OUTBOX_PAYLOAD_VERSION = OUTBOX_CONTRACT_VERSION
const val OUTBOX_MAX_ATTEMPTS = 5

object PendingOperationStatus {
    const val PENDING = "PENDING"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val SUCCEEDED = "SUCCEEDED"
    const val DEAD_LETTER = "DEAD_LETTER"
}

/** v69: retry decisions are made from typed/structured facts only, never human-readable text. */
enum class OutboxFailureCategory {
    TRANSIENT,
    AUTH,
    PERMISSION,
    VALIDATION,
    CONFLICT,
    ALREADY_COMMITTED,
    AMBIGUOUS,
    PERMANENT_PROTOCOL,
}

open class TypedOutboxException(
    val category: OutboxFailureCategory,
    val stableCode: String,
    message: String = stableCode,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

class HttpStatusOutboxException(
    val statusCode: Int,
    cause: Throwable? = null,
) : TypedOutboxException(
    category = OutboxErrorClassifier.categoryForHttpStatus(statusCode),
    stableCode = "HTTP_$statusCode",
    cause = cause,
)

class PermanentOutboxException(
    message: String,
    cause: Throwable? = null,
) : TypedOutboxException(
    category = OutboxFailureCategory.PERMANENT_PROTOCOL,
    stableCode = "PERMANENT_PROTOCOL",
    message = message,
    cause = cause,
)

class InvalidServerReceiptException(code: String) : TypedOutboxException(
    category = OutboxFailureCategory.PERMANENT_PROTOCOL,
    stableCode = code,
)

class ServerCommandRejectedException(code: String) : TypedOutboxException(
    category = OutboxFailureCategory.VALIDATION,
    stableCode = code,
)

class ServerCommandConflictException(code: String) : TypedOutboxException(
    category = OutboxFailureCategory.CONFLICT,
    stableCode = code,
)

class AmbiguousCommandOutcomeException(code: String, cause: Throwable? = null) : TypedOutboxException(
    category = OutboxFailureCategory.AMBIGUOUS,
    stableCode = code,
    cause = cause,
)

data class OutboxFailureDecision(
    val status: String,
    val attemptCount: Int,
    val nextRetryAt: Long,
    val errorCode: String,
    val errorMessage: String,
    val category: OutboxFailureCategory,
)

class OutboxRetryPolicy(
    private val maxAttempts: Int = OUTBOX_MAX_ATTEMPTS,
    private val baseDelayMillis: Long = 60_000L,
    private val maxDelayMillis: Long = 60L * 60L * 1_000L,
    private val randomUnit: () -> Double = { Random.nextDouble() },
) {
    init {
        require(maxAttempts > 0)
        require(baseDelayMillis > 0)
        require(maxDelayMillis >= baseDelayMillis)
    }

    fun onFailure(
        currentAttemptCount: Int,
        nowMillis: Long,
        error: Throwable,
    ): OutboxFailureDecision {
        val classification = OutboxErrorClassifier.classify(error)
        val nextAttempt = currentAttemptCount + 1
        val terminal = when (classification.category) {
            OutboxFailureCategory.PERMISSION,
            OutboxFailureCategory.VALIDATION,
            OutboxFailureCategory.CONFLICT,
            OutboxFailureCategory.PERMANENT_PROTOCOL -> true

            OutboxFailureCategory.TRANSIENT -> nextAttempt >= maxAttempts

            // AUTH pauses for session recovery. AMBIGUOUS must keep the original mutation alive so
            // the next same-id RPC can reconcile/replay a commit whose response may have been lost.
            OutboxFailureCategory.AUTH,
            OutboxFailureCategory.AMBIGUOUS -> false

            // A committed receipt is a success path and should not normally reach onFailure.
            OutboxFailureCategory.ALREADY_COMMITTED -> false
        }
        val nextRetryAt = if (terminal) Long.MAX_VALUE else nowMillis + jitteredDelay(nextAttempt)

        return OutboxFailureDecision(
            status = if (terminal) PendingOperationStatus.DEAD_LETTER else PendingOperationStatus.PENDING,
            attemptCount = nextAttempt,
            nextRetryAt = nextRetryAt,
            errorCode = classification.errorCode,
            errorMessage = OutboxErrorClassifier.errorMessage(error),
            category = classification.category,
        )
    }

    private fun jitteredDelay(attemptCount: Int): Long {
        val exponent = (attemptCount - 1).coerceAtLeast(0)
        val raw = min(
            maxDelayMillis.toDouble(),
            baseDelayMillis.toDouble() * 2.0.pow(exponent.toDouble()),
        )
        val unit = randomUnit().coerceIn(0.0, 1.0)
        val factor = 0.8 + (0.4 * unit)
        return (raw * factor).toLong().coerceAtLeast(baseDelayMillis / 2)
    }
}

data class OutboxFailureClassification(
    val category: OutboxFailureCategory,
    val errorCode: String,
)

object OutboxErrorClassifier {
    fun classify(error: Throwable): OutboxFailureClassification {
        val chain = generateSequence(error) { it.cause }.toList()
        chain.filterIsInstance<TypedOutboxException>().firstOrNull()?.let {
            return OutboxFailureClassification(it.category, it.stableCode)
        }
        if (chain.any { it is IOException }) {
            // An I/O failure after send is commit-ambiguous for a mutation. The same mutationId
            // is retried against the receipt-aware RPC, which reconciles before any new effect.
            return OutboxFailureClassification(OutboxFailureCategory.AMBIGUOUS, "TRANSPORT_IO_AMBIGUOUS")
        }
        return OutboxFailureClassification(OutboxFailureCategory.AMBIGUOUS, "UNCLASSIFIED_AMBIGUOUS")
    }

    fun isRetryable(error: Throwable): Boolean = when (classify(error).category) {
        OutboxFailureCategory.TRANSIENT,
        OutboxFailureCategory.AUTH,
        OutboxFailureCategory.ALREADY_COMMITTED,
        OutboxFailureCategory.AMBIGUOUS -> true
        OutboxFailureCategory.PERMISSION,
        OutboxFailureCategory.VALIDATION,
        OutboxFailureCategory.CONFLICT,
        OutboxFailureCategory.PERMANENT_PROTOCOL -> false
    }

    fun categoryForHttpStatus(status: Int): OutboxFailureCategory = when (status) {
        401 -> OutboxFailureCategory.AUTH
        403 -> OutboxFailureCategory.PERMISSION
        408, 425, 429 -> OutboxFailureCategory.TRANSIENT
        409 -> OutboxFailureCategory.CONFLICT
        400, 404, 422 -> OutboxFailureCategory.VALIDATION
        in 500..599 -> OutboxFailureCategory.TRANSIENT
        else -> OutboxFailureCategory.AMBIGUOUS
    }

    fun errorMessage(error: Throwable): String = SensitiveDataRedactor.sanitizeText(
        generateSequence(error) { it.cause }
            .mapNotNull { it.message?.trim()?.takeIf(String::isNotEmpty) }
            .firstOrNull()
            ?: "Unknown outbox failure",
    )
}
