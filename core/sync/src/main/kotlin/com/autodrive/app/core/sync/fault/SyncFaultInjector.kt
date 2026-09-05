package com.autodrive.app.core.sync.fault

/** Test seam only. Production DI binds [NoOpSyncFaultInjector]. */
enum class SyncFaultPoint {
    OUTBOX_AFTER_LOCAL_COMMIT_BEFORE_SEND,
    OUTBOX_AFTER_SERVER_COMMIT_BEFORE_RESPONSE,
    OUTBOX_BEFORE_FINALIZE_LOCAL_RECEIPT,
    CHANGE_FEED_AFTER_FETCH_BEFORE_APPLY,
    CHANGE_GROUP_AFTER_ENTITY_APPLY_BEFORE_CURSOR_COMMIT,
    CHANGE_PAGE_AFTER_COMMIT_BEFORE_NEXT_FETCH,
    BOOTSTRAP_AFTER_BEGIN,
    BOOTSTRAP_AFTER_STAGE_PAGE_COMMIT,
    BOOTSTRAP_BEFORE_INSTALL_COMMIT,
    BOOTSTRAP_AFTER_INSTALL_BEFORE_DELTA_RESUME,
    RECONCILE_AFTER_MANIFEST,
    RECONCILE_AFTER_TARGETED_REPAIR_BEFORE_RECHECK,
    COORDINATOR_DURING_PUSH,
    COORDINATOR_DURING_PULL,
    LOGOUT_DURING_ACTIVE_SYNC,
    WORKER_AFTER_LEASE_CLAIM,
}

data class FaultContext(
    val syncRunId: String? = null,
    val mutationId: String? = null,
    val eventId: String? = null,
    val revision: Long? = null,
)

fun interface SyncFaultInjector {
    suspend fun hit(point: SyncFaultPoint, context: FaultContext)
}

class NoOpSyncFaultInjector : SyncFaultInjector {
    override suspend fun hit(point: SyncFaultPoint, context: FaultContext) = Unit
}
