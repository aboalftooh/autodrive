package com.autodrive.app.core.sync.data

import com.autodrive.app.core.sync.diagnostics.SyncRunContext
import com.autodrive.app.core.sync.domain.SyncFailure
import com.autodrive.app.core.sync.domain.SyncPhase

data class SyncEngineResult(
    val completedPhases: Int,
    val failures: List<SyncFailure> = emptyList(),
    val skippedReason: String? = null
)

interface SyncEngine {
    suspend fun synchronize(onPhase: suspend (SyncPhase) -> Unit): SyncEngineResult

    /** v73 correlation-aware path. Legacy fakes remain source-compatible through the default. */
    suspend fun synchronize(context: SyncRunContext, onPhase: suspend (SyncPhase) -> Unit): SyncEngineResult =
        synchronize(onPhase)

    suspend fun flushPendingOperations()
}
