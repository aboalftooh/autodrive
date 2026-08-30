package com.autodrive.app.core.sync.data

import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.sync.diagnostics.SyncDiagnostics
import com.autodrive.app.core.sync.diagnostics.SyncObservabilityStore
import com.autodrive.app.core.sync.diagnostics.SyncRunContext
import com.autodrive.app.core.sync.domain.SyncFailure
import com.autodrive.app.core.sync.domain.SyncPhase
import com.autodrive.app.core.sync.fault.FaultContext
import com.autodrive.app.core.sync.fault.SyncFaultInjector
import com.autodrive.app.core.sync.fault.SyncFaultPoint
import io.github.jan.supabase.auth.auth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class SyncManager @Inject constructor(
    private val supabase: AutoDriveSupabase,
    private val sessionReader: SessionReader,
    private val presenceReporter: PresenceReporter,
    private val outboxSynchronizer: OutboxSynchronizer,
    private val unifiedChangeSynchronizer: UnifiedChangeSynchronizer,
    private val safeBootstrapSynchronizer: SafeBootstrapSynchronizer,
    private val antiEntropyReconciler: AntiEntropyReconciler,
    private val localDataCleaner: LocalDataCleaner,
    private val diagnostics: SyncDiagnostics,
    private val observabilityStore: SyncObservabilityStore,
    private val faultInjector: SyncFaultInjector,
) : SyncEngine {

    private val lifecycleMutex = Mutex()
    @Volatile private var blockedLogoutScope: SyncScope? = null

    override suspend fun synchronize(onPhase: suspend (SyncPhase) -> Unit): SyncEngineResult =
        synchronizeInternal(null, onPhase)

    override suspend fun synchronize(
        context: SyncRunContext,
        onPhase: suspend (SyncPhase) -> Unit,
    ): SyncEngineResult = synchronizeInternal(context, onPhase)

    private suspend fun synchronizeInternal(
        context: SyncRunContext?,
        onPhase: suspend (SyncPhase) -> Unit,
    ): SyncEngineResult = lifecycleMutex.withLock {
        val scope = SyncScope.from(sessionReader.currentSession())
            ?: return@withLock SyncEngineResult(0, skippedReason = "Missing sync scope")
        if (blockedLogoutScope == scope) return@withLock SyncEngineResult(0, skippedReason = "Sync scope is quiesced for logout")
        synchronizeUnlocked(scope, context, onPhase)
    }

    private suspend fun synchronizeUnlocked(
        scope: SyncScope,
        context: SyncRunContext?,
        onPhase: suspend (SyncPhase) -> Unit,
    ): SyncEngineResult {
        onPhase(SyncPhase.AUTH)
        val authStartedAt = System.nanoTime()
        try {
            supabase.client.auth.awaitInitialization()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            phase(context, SyncPhase.AUTH, authStartedAt, false, error, "AUTH")
            return SyncEngineResult(0, failures = listOf(SyncFailure(SyncPhase.AUTH, "AUTH")))
        }
        phase(context, SyncPhase.AUTH, authStartedAt, true)
        if (supabase.client.auth.currentSessionOrNull() == null) return SyncEngineResult(0, skippedReason = "Supabase session is not available")

        presenceReporter.touch()
        val failures = mutableListOf<SyncFailure>()
        var completed = 1

        onPhase(SyncPhase.PENDING_OPERATIONS)
        val pushStarted = System.nanoTime()
        try {
            faultInjector.hit(SyncFaultPoint.COORDINATOR_DURING_PUSH, FaultContext(context?.syncRunId))
            outboxSynchronizer.recoverExpiredLeases(scope)
            outboxSynchronizer.flush(scope, recoverExpiredClaims = false, context = context)
            completed += 1
            phase(context, SyncPhase.PENDING_OPERATIONS, pushStarted, true)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            val code = stableCode(error, "OUTBOX_TRANSIENT")
            failures += SyncFailure(SyncPhase.PENDING_OPERATIONS, code)
            phase(context, SyncPhase.PENDING_OPERATIONS, pushStarted, false, error, code)
        }

        onPhase(SyncPhase.PROFILE)
        val pullStarted = System.nanoTime()
        var canonicalResult: UnifiedChangeResult? = null
        try {
            faultInjector.hit(SyncFaultPoint.COORDINATOR_DURING_PULL, FaultContext(context?.syncRunId))
            safeBootstrapSynchronizer.ensureCanonicalCursor(scope, context)
            canonicalResult = try {
                unifiedChangeSynchronizer.synchronize(scope, context)
            } catch (_: CursorExpiredException) {
                recordSafely { observabilityStore.cursorExpired(scope) }
                safeBootstrapSynchronizer.bootstrap(scope, context)
                unifiedChangeSynchronizer.synchronize(scope, context)
            }
            completed += 1
            if (canonicalResult.hasMore) failures += SyncFailure(SyncPhase.PROFILE, "CANONICAL_MORE_WORK_REQUIRED")
            phase(context, SyncPhase.PROFILE, pullStarted, true)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (expired: BootstrapExpiredException) {
            failures += SyncFailure(SyncPhase.PROFILE, "BOOTSTRAP_EXPIRED")
            phase(context, SyncPhase.PROFILE, pullStarted, false, expired, "BOOTSTRAP_EXPIRED")
        } catch (error: Throwable) {
            val code = stableCode(error, "CHANGE_FEED_PROTOCOL")
            failures += SyncFailure(SyncPhase.PROFILE, code)
            phase(context, SyncPhase.PROFILE, pullStarted, false, error, code)
        }

        onPhase(SyncPhase.DELETIONS)
        if (canonicalResult != null) completed += 1

        onPhase(SyncPhase.RECONCILE)
        val reconcileStarted = System.nanoTime()
        val reconciliationBase = canonicalResult
        if (reconciliationBase != null && !reconciliationBase.hasMore) {
            try {
                try {
                    antiEntropyReconciler.reconcileIfDue(scope, reconciliationBase.cursorRevision, context = context)
                } catch (_: RebootstrapRequiredException) {
                    safeBootstrapSynchronizer.bootstrap(scope, context)
                    canonicalResult = unifiedChangeSynchronizer.synchronize(scope, context)
                }
                completed += 1
                phase(context, SyncPhase.RECONCILE, reconcileStarted, true)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                val code = stableCode(error, "RECONCILIATION_PROTOCOL")
                failures += SyncFailure(SyncPhase.RECONCILE, code)
                phase(context, SyncPhase.RECONCILE, reconcileStarted, false, error, code)
            }
        }

        return SyncEngineResult(completedPhases = completed, failures = failures)
    }

    suspend fun touchPresence(force: Boolean = false) = presenceReporter.touch(force)

    override suspend fun flushPendingOperations() = lifecycleMutex.withLock {
        val scope = SyncScope.from(sessionReader.currentSession()) ?: return@withLock
        if (blockedLogoutScope == scope) return@withLock
        outboxSynchronizer.flush(scope)
    }

    fun beginLogout(scope: SyncScope) { blockedLogoutScope = scope }

    suspend fun quiesceAndClearForLogout(scope: SyncScope) {
        beginLogout(scope)
        lifecycleMutex.withLock {
            faultInjector.hit(SyncFaultPoint.LOGOUT_DURING_ACTIVE_SYNC, FaultContext())
            localDataCleaner.clearAccount(scope)
        }
    }

    fun releaseLogoutBarrier(scope: SyncScope) { if (blockedLogoutScope == scope) blockedLogoutScope = null }

    private fun phase(
        context: SyncRunContext?, phase: SyncPhase, startedAt: Long, successful: Boolean,
        error: Throwable? = null, failureCode: String? = null,
    ) {
        val duration = ((System.nanoTime() - startedAt) / 1_000_000L).coerceAtLeast(0L)
        if (context == null) diagnostics.phaseFinished(phase, duration, successful, error)
        else diagnostics.phaseFinished(context, phase, duration, successful, error, failureCode)
    }

    private fun stableCode(error: Throwable, fallback: String): String =
        error.message?.takeIf { it.matches(Regex("[A-Z0-9_:-]{2,80}")) } ?: error::class.simpleName ?: fallback

    private suspend fun recordSafely(block: suspend () -> Unit) { runCatching { block() } }
}
