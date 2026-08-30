package com.autodrive.app.core.sync.fault

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncFaultInjectorTest {
    @Test
    fun `deterministic injector fails exactly configured occurrence`() = runTest {
        val injector = DeterministicSyncFaultInjector(
            target = SyncFaultPoint.CHANGE_PAGE_AFTER_COMMIT_BEFORE_NEXT_FETCH,
            occurrence = 2,
        )
        injector.hit(SyncFaultPoint.CHANGE_PAGE_AFTER_COMMIT_BEFORE_NEXT_FETCH, FaultContext(revision = 10))
        val failure = runCatching {
            injector.hit(SyncFaultPoint.CHANGE_PAGE_AFTER_COMMIT_BEFORE_NEXT_FETCH, FaultContext(revision = 20))
        }.exceptionOrNull()
        assertTrue(failure is InjectedSyncFault)
        assertEquals(2, injector.hits)
    }
}

private class InjectedSyncFault(val point: SyncFaultPoint, val occurrence: Int) :
    IllegalStateException("INJECTED_SYNC_FAULT:${point.name}:$occurrence")

private class DeterministicSyncFaultInjector(
    private val target: SyncFaultPoint,
    private val occurrence: Int,
) : SyncFaultInjector {
    var hits: Int = 0
        private set

    override suspend fun hit(point: SyncFaultPoint, context: FaultContext) {
        if (point != target) return
        hits += 1
        if (hits == occurrence) throw InjectedSyncFault(point, occurrence)
    }
}
