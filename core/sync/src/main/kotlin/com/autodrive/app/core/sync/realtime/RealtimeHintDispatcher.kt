package com.autodrive.app.core.sync.realtime

import com.autodrive.app.core.observability.AppLogger
import com.autodrive.app.core.sync.domain.SyncCoordinator
import com.autodrive.app.core.sync.domain.SyncReason
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Realtime is a wake-up signal only. The coordinator owns authoritative pull/apply correctness. */
@Singleton
class RealtimeHintDispatcher @Inject constructor(
    private val syncCoordinatorProvider: Provider<SyncCoordinator>,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun requestSync() {
        scope.launch {
            runCatching { syncCoordinatorProvider.get().requestSync(SyncReason.REALTIME_HINT) }
                .onFailure { error -> AppLogger.w(TAG, "Realtime hint sync request failed: ${error::class.simpleName}") }
        }
    }

    private companion object { const val TAG = "RealtimeHintDispatcher" }
}
