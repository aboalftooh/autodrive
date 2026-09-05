package com.autodrive.app.core.sync.data

import com.autodrive.app.core.observability.AppLogger
import com.autodrive.app.core.observability.SensitiveDataRedactor
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.sync.diagnostics.ScopeFingerprintProvider
import com.autodrive.app.core.sync.diagnostics.SyncDiagnostics
import com.autodrive.app.core.sync.diagnostics.SyncObservabilityStore
import com.autodrive.app.core.sync.diagnostics.SyncRunContext
import com.autodrive.app.core.sync.domain.RealtimeController
import com.autodrive.app.core.sync.domain.SyncConnectivity
import com.autodrive.app.core.sync.domain.SyncCoordinator
import com.autodrive.app.core.sync.domain.SyncFailure
import com.autodrive.app.core.sync.domain.SyncPhase
import com.autodrive.app.core.sync.domain.SyncReason
import com.autodrive.app.core.sync.domain.SyncResult
import com.autodrive.app.core.sync.domain.SyncState
import com.autodrive.app.core.sync.domain.SyncStatus
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serializes/coalesces synchronization requests into one active generation-draining run.
 *
 * Concurrent hints share the active result while incrementing the requested generation; the owner
 * drains trailing generations before completing the shared deferred. Cancellation is propagated,
 * while non-cancellation failures become explicit sync failures. Realtime restart occurs only after
 * engine work and does not become canonical data authority.
 */
@Singleton
class DefaultSyncCoordinator @Inject constructor(
    private val engine: SyncEngine,
    private val connectivity: SyncConnectivity,
    private val sessionReader: SessionReader,
    private val realtimeController: RealtimeController,
    private val diagnostics: SyncDiagnostics,
    private val fingerprintProvider: ScopeFingerprintProvider? = null,
    private val observabilityStore: SyncObservabilityStore? = null,
) : SyncCoordinator {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)
    private val generationMutex = Mutex()
    private var activeSync: CompletableDeferred<SyncResult>? = null
    private var requestedGeneration = 0L
    private var completedGeneration = 0L
    private var latestReason = SyncReason.APP_START

    private val _state = MutableStateFlow(SyncState())
    override val state: StateFlow<SyncState> = _state.asStateFlow()

    override fun start() {
        if (!started.compareAndSet(false, true)) return
        if (sessionReader.currentSession().isLoggedIn && connectivity.isConnectedNow()) {
            scope.launch { requestSync(SyncReason.APP_START) }
        }
        var initialized = false
        var wasConnected = false
        connectivity.isConnected
            .onEach { connected ->
                if (!initialized) {
                    initialized = true
                    wasConnected = connected
                    return@onEach
                }
                val restored = connected && !wasConnected
                wasConnected = connected
                if (restored && sessionReader.currentSession().isLoggedIn) requestSync(SyncReason.NETWORK_RESTORED)
            }
            .catch { error -> logWarning("Connectivity observation failed: ${error::class.simpleName}") }
            .launchIn(scope)
    }

    override suspend fun requestSync(reason: SyncReason): SyncResult {
        var owner = false
        var trailing = false
        val shared = generationMutex.withLock {
            trailing = activeSync != null
            requestedGeneration += 1
            latestReason = reason
            publishGenerations()
            activeSync ?: CompletableDeferred<SyncResult>().also {
                activeSync = it
                owner = true
            }
        }
        currentScope()?.let { recordSafely { observabilityStore?.hintAccepted(it, trailing) } }
        if (!owner) return shared.await()

        try {
            return drainGenerations(shared)
        } catch (cancellation: CancellationException) {
            generationMutex.withLock {
                if (activeSync === shared) activeSync = null
                shared.completeExceptionally(cancellation)
            }
            throw cancellation
        }
    }

    private suspend fun drainGenerations(shared: CompletableDeferred<SyncResult>): SyncResult {
        var lastResult = SyncResult(SyncStatus.SKIPPED)
        while (true) {
            val (generationToService, reason) = generationMutex.withLock { requestedGeneration to latestReason }
            lastResult = executeSafely(reason, generationToService)
            val drained = generationMutex.withLock {
                completedGeneration = maxOf(completedGeneration, generationToService)
                publishGenerations()
                if (requestedGeneration <= completedGeneration && activeSync === shared) {
                    activeSync = null
                    shared.complete(lastResult)
                    true
                } else false
            }
            if (drained) return lastResult
        }
    }

    private suspend fun executeSafely(reason: SyncReason, generation: Long): SyncResult = try {
        execute(reason, generation)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        val failure = SyncFailure(
            SyncPhase.IDLE,
            error::class.simpleName.orEmpty().ifBlank { "UNKNOWN" },
        )
        finish(reason, SyncStatus.FAILED, listOf(failure), null, currentScope())
    }

    private suspend fun execute(reason: SyncReason, generation: Long): SyncResult {
        val previousSuccess = _state.value.lastSuccessAt
        val startedAt = System.currentTimeMillis()
        val syncScope = currentScope()
        val context = SyncRunContext(
            syncRunId = UUID.randomUUID().toString(),
            reason = reason,
            requestedGeneration = generation,
            startedAtLocal = startedAt,
            scopeFingerprint = syncScope?.let { fingerprintProvider?.fingerprint(it) } ?: "scope-unavailable",
        )
        diagnostics.syncStarted(context)
        syncScope?.let { recordSafely { observabilityStore?.runStarted(it, context) } }
        _state.value = _state.value.copy(
            status = SyncStatus.RUNNING,
            reason = reason,
            phase = SyncPhase.AUTH,
            startedAt = startedAt,
            completedAt = null,
            lastSuccessAt = previousSuccess,
            failures = emptyList(),
        )

        val engineResult = engine.synchronize(context) { phase -> _state.value = _state.value.copy(phase = phase) }
        if (engineResult.skippedReason != null) {
            logWarning("Sync skipped [$reason]")
            return finish(reason, SyncStatus.SKIPPED, emptyList(), context, syncScope)
        }

        val failures = engineResult.failures.toMutableList()
        _state.value = _state.value.copy(phase = SyncPhase.REALTIME)
        val realtimeStarted = System.nanoTime()
        try {
            realtimeController.restart()
            diagnostics.phaseFinished(context, SyncPhase.REALTIME, elapsedMs(realtimeStarted), true)
        } catch (error: Throwable) {
            val code = error::class.simpleName.orEmpty().ifBlank { "REALTIME_DEGRADED" }
            diagnostics.phaseFinished(context, SyncPhase.REALTIME, elapsedMs(realtimeStarted), false, error, code)
            failures += SyncFailure(SyncPhase.REALTIME, code)
        }

        val status = when {
            failures.isEmpty() -> SyncStatus.SUCCESS
            engineResult.completedPhases > 0 -> SyncStatus.PARTIAL_SUCCESS
            else -> SyncStatus.FAILED
        }
        return finish(reason, status, failures, context, syncScope)
    }

    private suspend fun finish(
        reason: SyncReason,
        status: SyncStatus,
        failures: List<SyncFailure>,
        context: SyncRunContext?,
        syncScope: SyncScope?,
    ): SyncResult {
        val now = System.currentTimeMillis()
        val previous = _state.value
        _state.value = previous.copy(
            status = status,
            reason = reason,
            phase = SyncPhase.COMPLETED,
            completedAt = now,
            lastSuccessAt = if (status == SyncStatus.SUCCESS) now else previous.lastSuccessAt,
            failures = failures,
        )
        if (context != null) {
            diagnostics.syncFinished(
                context = context,
                status = status,
                durationMs = (now - context.startedAtLocal).coerceAtLeast(0L),
                failureCount = failures.size,
                lastSuccessAt = _state.value.lastSuccessAt,
                lastFailureCode = stableFailureCode(failures.lastOrNull()?.message),
            )
        } else {
            diagnostics.syncFinished(reason, status, (now - (previous.startedAt ?: now)).coerceAtLeast(0L), failures.size, _state.value.lastSuccessAt)
        }
        syncScope?.let { recordSafely { observabilityStore?.runFinished(it, status, failures) } }
        if (failures.isEmpty()) logDebug("Sync finished [$reason]: $status") else logWarning("Sync finished [$reason]: $status, failures=${failures.size}")
        return SyncResult(status, failures)
    }

    private fun stableFailureCode(value: String?): String? = value?.takeIf {
        it.matches(Regex("[A-Z0-9_:-]{2,80}"))
    } ?: value?.let { "UNKNOWN" }

    private fun publishGenerations() {
        _state.value = _state.value.copy(requestedGeneration = requestedGeneration, completedGeneration = completedGeneration)
    }

    private fun currentScope(): SyncScope? = SyncScope.from(sessionReader.currentSession())
    private suspend fun recordSafely(block: suspend () -> Unit) { runCatching { block() } }
    private fun elapsedMs(started: Long) = ((System.nanoTime() - started) / 1_000_000L).coerceAtLeast(0L)
    private fun logDebug(message: String) = runCatching { AppLogger.d(TAG, message) }.let { Unit }
    private fun logWarning(message: String) = runCatching { AppLogger.w(TAG, message) }.let { Unit }

    private companion object { const val TAG = "SyncCoordinator" }
}
