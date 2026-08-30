package com.autodrive.app.core.sync.data

import com.autodrive.app.core.sync.domain.RealtimeConnectionState
import com.autodrive.app.core.sync.domain.SyncPhase
import com.autodrive.app.core.sync.domain.SyncReason
import com.autodrive.app.core.sync.domain.SyncStatus
import com.autodrive.app.core.sync.diagnostics.NoOpSyncDiagnostics
import com.autodrive.app.core.sync.diagnostics.SyncDiagnostics
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncStepExecutorTest {

    @Test
    fun `failure in one step does not prevent the next independent step`() = runTest {
        val visited = mutableListOf<SyncPhase>()
        val executor = SyncStepExecutor(
            onPhase = { visited += it },
            diagnostics = NoOpSyncDiagnostics,
        )
        var secondStepRan = false

        val firstSucceeded = executor.run(SyncPhase.PROFILE) {
            error("profile failed")
        }
        val secondSucceeded = executor.run(SyncPhase.NOTIFICATIONS) {
            secondStepRan = true
        }

        assertFalse(firstSucceeded)
        assertTrue(secondSucceeded)
        assertTrue(secondStepRan)
        assertEquals(listOf(SyncPhase.PROFILE, SyncPhase.NOTIFICATIONS), visited)
        assertEquals(1, executor.completedPhases)
        assertEquals(1, executor.failures.size)
        assertEquals(SyncPhase.PROFILE, executor.failures.single().phase)
    }
    @Test
    fun `step duration and result are reported without business payload`() = runTest {
        val diagnostics = RecordingDiagnostics()
        val clock = ArrayDeque(listOf(1_000_000L, 6_000_000L))
        val executor = SyncStepExecutor(
            onPhase = { },
            diagnostics = diagnostics,
            nanoTime = { clock.removeFirst() },
        )

        executor.run(SyncPhase.BALANCE) { Unit }

        assertEquals(listOf(Triple(SyncPhase.BALANCE, 5L, true)), diagnostics.phases)
    }

    private class RecordingDiagnostics : SyncDiagnostics {
        val phases = mutableListOf<Triple<SyncPhase, Long, Boolean>>()

        override fun syncStarted(reason: SyncReason, startedAt: Long) = Unit

        override fun phaseFinished(
            phase: SyncPhase,
            durationMs: Long,
            successful: Boolean,
            error: Throwable?,
        ) {
            phases += Triple(phase, durationMs, successful)
        }

        override fun syncFinished(
            reason: SyncReason,
            status: SyncStatus,
            durationMs: Long,
            failureCount: Int,
            lastSuccessAt: Long?,
        ) = Unit

        override fun outboxState(
            pendingCount: Int,
            inProgressCount: Int,
            deadLetterCount: Int,
        ) = Unit

        override fun realtimeState(
            state: RealtimeConnectionState,
            reconnectDelayMs: Long?,
        ) = Unit
    }

}
