package com.autodrive.app.core.sync.data

import com.autodrive.app.core.session.domain.CurrentSession
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.sync.diagnostics.NoOpSyncDiagnostics
import com.autodrive.app.core.sync.domain.RealtimeController
import com.autodrive.app.core.sync.domain.SyncConnectivity
import com.autodrive.app.core.sync.domain.SyncFailure
import com.autodrive.app.core.sync.domain.SyncPhase
import com.autodrive.app.core.sync.domain.SyncReason
import com.autodrive.app.core.sync.domain.SyncStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class DefaultSyncCoordinatorTest {

    @Test
    fun `hint arriving during active cycle creates follow-up generation`() = runTest {
        FakeRealtimeController.reset()
        val engine = BlockingFirstCycleEngine()
        val coordinator = coordinator(engine)

        val first = async { coordinator.requestSync(SyncReason.USER_REFRESH) }
        engine.firstStarted.await()
        val second = async { coordinator.requestSync(SyncReason.FCM_HINT) }
        runCurrent()
        assertEquals(1, engine.syncCalls)

        engine.releaseFirst.complete(Unit)
        val firstResult = first.await()
        val secondResult = second.await()

        assertSame(firstResult, secondResult)
        assertEquals(SyncStatus.SUCCESS, firstResult.status)
        assertEquals(2, engine.syncCalls)
        assertEquals(2, FakeRealtimeController.restartCalls)
        assertEquals(2L, coordinator.state.value.requestedGeneration)
        assertEquals(2L, coordinator.state.value.completedGeneration)
    }

    @Test
    fun `burst hints coalesce into one follow-up cycle and drain latest generation`() = runTest {
        FakeRealtimeController.reset()
        val engine = BlockingFirstCycleEngine()
        val coordinator = coordinator(engine)
        val first = async { coordinator.requestSync(SyncReason.USER_REFRESH) }
        engine.firstStarted.await()
        val burst = (1..20).map { async { coordinator.requestSync(SyncReason.REALTIME_HINT) } }
        runCurrent()
        engine.releaseFirst.complete(Unit)
        first.await(); burst.forEach { it.await() }

        assertEquals(2, engine.syncCalls)
        assertEquals(21L, coordinator.state.value.requestedGeneration)
        assertEquals(21L, coordinator.state.value.completedGeneration)
    }

    @Test
    fun `independent section failure produces partial success and remains observable`() = runTest {
        FakeRealtimeController.reset()
        val expectedFailure = SyncFailure(SyncPhase.PROFILE, "profile unavailable")
        val engine = ImmediateSyncEngine(
            SyncEngineResult(completedPhases = 4, failures = listOf(expectedFailure)),
        )
        val coordinator = coordinator(engine)

        val result = coordinator.requestSync(SyncReason.APP_START)

        assertEquals(SyncStatus.PARTIAL_SUCCESS, result.status)
        assertEquals(listOf(expectedFailure), result.failures)
        assertEquals(SyncStatus.PARTIAL_SUCCESS, coordinator.state.value.status)
        assertEquals(SyncPhase.COMPLETED, coordinator.state.value.phase)
        assertEquals(1, FakeRealtimeController.restartCalls)
    }

    private fun coordinator(engine: SyncEngine) = DefaultSyncCoordinator(
        engine = engine,
        connectivity = OfflineConnectivity,
        sessionReader = LoggedInSessionReader,
        realtimeController = FakeRealtimeController,
        diagnostics = NoOpSyncDiagnostics,
    )

    private class BlockingFirstCycleEngine : SyncEngine {
        var syncCalls = 0
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()

        override suspend fun synchronize(onPhase: suspend (SyncPhase) -> Unit): SyncEngineResult {
            syncCalls += 1
            onPhase(SyncPhase.PROFILE)
            if (syncCalls == 1) {
                firstStarted.complete(Unit)
                releaseFirst.await()
            }
            return SyncEngineResult(completedPhases = 1)
        }

        override suspend fun flushPendingOperations() = Unit
    }

    private class ImmediateSyncEngine(private val result: SyncEngineResult) : SyncEngine {
        override suspend fun synchronize(onPhase: suspend (SyncPhase) -> Unit): SyncEngineResult {
            onPhase(SyncPhase.PROFILE)
            return result
        }
        override suspend fun flushPendingOperations() = Unit
    }

    private object FakeRealtimeController : RealtimeController {
        var restartCalls = 0
        override fun restart() { restartCalls += 1 }
        override fun stop() = Unit
        fun reset() { restartCalls = 0 }
    }

    private object OfflineConnectivity : SyncConnectivity {
        override val isConnected: Flow<Boolean> = emptyFlow()
        override fun isConnectedNow(): Boolean = false
    }

    private object LoggedInSessionReader : SessionReader {
        override fun currentSession(): CurrentSession = CurrentSession(
            isLoggedIn = true,
            userId = "user-1",
            clientId = "client-1",
            orgId = "org-1",
        )
    }
}
