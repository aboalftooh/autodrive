package com.autodrive.app.core.sync.data

import com.autodrive.app.core.sync.domain.SyncFailure
import com.autodrive.app.core.sync.domain.SyncPhase
import com.autodrive.app.core.observability.SensitiveDataRedactor
import com.autodrive.app.core.sync.diagnostics.SyncDiagnostics
import kotlinx.coroutines.CancellationException

class SyncStepExecutor(
    private val onPhase: suspend (SyncPhase) -> Unit,
    private val diagnostics: SyncDiagnostics,
    private val nanoTime: () -> Long = System::nanoTime,
) {
    private val mutableFailures = mutableListOf<SyncFailure>()

    val failures: List<SyncFailure>
        get() = mutableFailures.toList()

    var completedPhases: Int = 0
        private set

    suspend fun run(
        phase: SyncPhase,
        block: suspend () -> Unit
    ): Boolean {
        onPhase(phase)
        val startedAt = nanoTime()
        return try {
            block()
            completedPhases += 1
            diagnostics.phaseFinished(
                phase = phase,
                durationMs = elapsedMillis(startedAt),
                successful = true,
            )
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            diagnostics.phaseFinished(
                phase = phase,
                durationMs = elapsedMillis(startedAt),
                successful = false,
                error = error,
            )
            mutableFailures += SyncFailure(
                phase = phase,
                message = SensitiveDataRedactor.sanitizeText(
                    error.message ?: error::class.simpleName.orEmpty(),
                ),
            )
            false
        }
    }

    fun recordFailure(phase: SyncPhase, message: String) {
        mutableFailures += SyncFailure(phase, SensitiveDataRedactor.sanitizeText(message))
    }

    private fun elapsedMillis(startedAt: Long): Long =
        ((nanoTime() - startedAt) / 1_000_000L).coerceAtLeast(0L)
}
