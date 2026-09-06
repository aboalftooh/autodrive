package com.autodrive.app.core.sync.domain

import kotlinx.coroutines.flow.StateFlow

enum class SyncReason {
    APP_START,
    NETWORK_RESTORED,
    USER_REFRESH,
    FCM_HINT,
    REALTIME_HINT,
    LOGIN_SUCCESS
}

enum class SyncPhase {
    IDLE,
    AUTH,
    PROFILE,
    INVOICES,
    PAYMENTS,
    COMMISSIONS,
    BALANCE,
    TRANSACTIONS,
    WITHDRAWALS,
    NOTIFICATIONS,
    CHAT,
    PENDING_OPERATIONS,
    DELETIONS,
    RECONCILE,
    REALTIME,
    COMPLETED
}

enum class SyncStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILED,
    SKIPPED
}

data class SyncFailure(
    val phase: SyncPhase,
    val message: String
)

data class SyncResult(
    val status: SyncStatus,
    val failures: List<SyncFailure> = emptyList()
)

data class SyncState(
    val status: SyncStatus = SyncStatus.IDLE,
    val reason: SyncReason? = null,
    val phase: SyncPhase = SyncPhase.IDLE,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val lastSuccessAt: Long? = null,
    val failures: List<SyncFailure> = emptyList(),
    val requestedGeneration: Long = 0L,
    val completedGeneration: Long = 0L,
)

/**
 * Public owner of synchronization scheduling and observable run state.
 *
 * [start] installs lifecycle/connectivity observation once; [requestSync] requests authoritative
 * coordinator work for a reason and may be coalesced with an already active run by the production
 * implementation. Consumers must use [state] for status and must not treat Realtime hints as state
 * authority themselves.
 */
interface SyncCoordinator {
    val state: StateFlow<SyncState>

    fun start()

    suspend fun requestSync(reason: SyncReason): SyncResult
}
