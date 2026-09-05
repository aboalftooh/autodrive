package com.autodrive.app.core.sync.diagnostics

import com.autodrive.app.core.observability.AppLogger
import com.autodrive.app.core.sync.domain.RealtimeConnectionState
import com.autodrive.app.core.sync.domain.SyncPhase
import com.autodrive.app.core.sync.domain.SyncReason
import com.autodrive.app.core.sync.domain.SyncStatus
import javax.inject.Inject
import javax.inject.Singleton

/** Structured, privacy-safe sink. Legacy methods remain for non-run callers and old tests. */
interface SyncDiagnostics {
    fun syncStarted(reason: SyncReason, startedAt: Long)
    fun syncStarted(context: SyncRunContext) = syncStarted(context.reason, context.startedAtLocal)

    fun phaseFinished(phase: SyncPhase, durationMs: Long, successful: Boolean, error: Throwable? = null)
    fun phaseFinished(
        context: SyncRunContext,
        phase: SyncPhase,
        durationMs: Long,
        successful: Boolean,
        error: Throwable? = null,
        failureCode: String? = null,
    ) = phaseFinished(phase, durationMs, successful, error)

    fun syncFinished(reason: SyncReason, status: SyncStatus, durationMs: Long, failureCount: Int, lastSuccessAt: Long?)
    fun syncFinished(
        context: SyncRunContext,
        status: SyncStatus,
        durationMs: Long,
        failureCount: Int,
        lastSuccessAt: Long?,
        lastFailureCode: String? = null,
    ) = syncFinished(context.reason, status, durationMs, failureCount, lastSuccessAt)

    fun outboxState(pendingCount: Int, inProgressCount: Int, deadLetterCount: Int)
    fun outboxState(
        context: SyncRunContext?,
        pendingCount: Int,
        inProgressCount: Int,
        deadLetterCount: Int,
        retryCount: Long,
        oldestAgeMs: Long,
        conflictCount: Int,
    ) = outboxState(pendingCount, inProgressCount, deadLetterCount)

    fun changeFeed(context: SyncRunContext?, appliedEvents: Int, localCursor: Long, serverHead: Long, hasMore: Boolean) = Unit
    fun changeGroup(
        context: SyncRunContext?, transactionGroupId: String, firstEventId: String, lastEventId: String,
        firstRevision: Long, lastRevision: Long, eventCount: Int, entityTypes: String, operations: String,
    ) = Unit
    fun bootstrap(context: SyncRunContext?, status: String, durationMs: Long, stagedRows: Int, baselineRevision: Long?) = Unit
    fun reconciliation(context: SyncRunContext?, status: String, repairedRows: Int) = Unit

    fun realtimeState(state: RealtimeConnectionState, reconnectDelayMs: Long? = null)
}

object NoOpSyncDiagnostics : SyncDiagnostics {
    override fun syncStarted(reason: SyncReason, startedAt: Long) = Unit
    override fun phaseFinished(phase: SyncPhase, durationMs: Long, successful: Boolean, error: Throwable?) = Unit
    override fun syncFinished(reason: SyncReason, status: SyncStatus, durationMs: Long, failureCount: Int, lastSuccessAt: Long?) = Unit
    override fun outboxState(pendingCount: Int, inProgressCount: Int, deadLetterCount: Int) = Unit
    override fun realtimeState(state: RealtimeConnectionState, reconnectDelayMs: Long?) = Unit
}

@Singleton
class DefaultSyncDiagnostics @Inject constructor() : SyncDiagnostics {
    override fun syncStarted(reason: SyncReason, startedAt: Long) {
        AppLogger.event(TAG, "sync_started", mapOf("reason" to reason.name, "started_at" to startedAt))
    }

    override fun syncStarted(context: SyncRunContext) {
        AppLogger.event(TAG, "sync_started", runFields(context) + mapOf("started_at" to context.startedAtLocal))
    }

    override fun phaseFinished(phase: SyncPhase, durationMs: Long, successful: Boolean, error: Throwable?) {
        phaseFinishedInternal(null, phase, durationMs, successful, error, null)
    }

    override fun phaseFinished(
        context: SyncRunContext,
        phase: SyncPhase,
        durationMs: Long,
        successful: Boolean,
        error: Throwable?,
        failureCode: String?,
    ) = phaseFinishedInternal(context, phase, durationMs, successful, error, failureCode)

    private fun phaseFinishedInternal(
        context: SyncRunContext?, phase: SyncPhase, durationMs: Long, successful: Boolean,
        error: Throwable?, failureCode: String?,
    ) {
        val fields = buildMap<String, Any?> {
            context?.let { putAll(runFields(it)) }
            put("phase", phase.name); put("duration_ms", durationMs); put("success", successful)
            failureCode?.let { put("failure_code", it) }
        }
        if (successful) AppLogger.event(TAG, "sync_phase_finished", fields)
        else AppLogger.e(TAG, "sync_phase_failed", error, fields)
    }

    override fun syncFinished(reason: SyncReason, status: SyncStatus, durationMs: Long, failureCount: Int, lastSuccessAt: Long?) {
        syncFinishedInternal(null, reason, status, durationMs, failureCount, lastSuccessAt, null)
    }

    override fun syncFinished(
        context: SyncRunContext, status: SyncStatus, durationMs: Long, failureCount: Int,
        lastSuccessAt: Long?, lastFailureCode: String?,
    ) = syncFinishedInternal(context, context.reason, status, durationMs, failureCount, lastSuccessAt, lastFailureCode)

    private fun syncFinishedInternal(
        context: SyncRunContext?, reason: SyncReason, status: SyncStatus, durationMs: Long,
        failureCount: Int, lastSuccessAt: Long?, lastFailureCode: String?,
    ) {
        val fields = buildMap<String, Any?> {
            context?.let { putAll(runFields(it)) }
            put("reason", reason.name); put("status", status.name); put("duration_ms", durationMs)
            put("failure_count", failureCount); lastSuccessAt?.let { put("last_success_at", it) }
            lastFailureCode?.let { put("failure_code", it) }
        }
        if (status == SyncStatus.FAILED) AppLogger.w(TAG, "sync_finished", fields) else AppLogger.event(TAG, "sync_finished", fields)
    }

    override fun outboxState(pendingCount: Int, inProgressCount: Int, deadLetterCount: Int) {
        AppLogger.event(TAG, "outbox_state", mapOf("pending_count" to pendingCount, "in_progress_count" to inProgressCount, "dead_letter_count" to deadLetterCount))
    }

    override fun outboxState(
        context: SyncRunContext?, pendingCount: Int, inProgressCount: Int, deadLetterCount: Int,
        retryCount: Long, oldestAgeMs: Long, conflictCount: Int,
    ) {
        AppLogger.event(TAG, "outbox_state", fields(context, mapOf(
            "pending_count" to pendingCount, "in_progress_count" to inProgressCount,
            "dead_letter_count" to deadLetterCount, "retry_count" to retryCount,
            "oldest_outbox_age_ms" to oldestAgeMs, "conflict_count" to conflictCount,
        )))
    }

    override fun changeFeed(context: SyncRunContext?, appliedEvents: Int, localCursor: Long, serverHead: Long, hasMore: Boolean) {
        AppLogger.event(TAG, "change_feed_state", fields(context, mapOf(
            "applied_event_count" to appliedEvents, "local_cursor_revision" to localCursor,
            "server_head_revision" to serverHead, "revision_lag" to (serverHead-localCursor).coerceAtLeast(0L), "has_more" to hasMore,
        )))
    }

    override fun changeGroup(
        context: SyncRunContext?, transactionGroupId: String, firstEventId: String, lastEventId: String,
        firstRevision: Long, lastRevision: Long, eventCount: Int, entityTypes: String, operations: String,
    ) {
        AppLogger.event(TAG, "change_group_applied", fields(context, mapOf(
            "transaction_group_id" to transactionGroupId, "first_event_id" to firstEventId,
            "last_event_id" to lastEventId, "first_revision" to firstRevision,
            "last_revision" to lastRevision, "event_count" to eventCount,
            "entity_type" to entityTypes, "operation" to operations,
        )))
    }

    override fun bootstrap(context: SyncRunContext?, status: String, durationMs: Long, stagedRows: Int, baselineRevision: Long?) {
        AppLogger.event(TAG, "bootstrap_state", fields(context, mapOf(
            "status" to status, "duration_ms" to durationMs, "staged_row_count" to stagedRows,
            "baseline_revision" to baselineRevision,
        )))
    }

    override fun reconciliation(context: SyncRunContext?, status: String, repairedRows: Int) {
        AppLogger.event(TAG, "reconciliation_state", fields(context, mapOf("status" to status, "repaired_row_count" to repairedRows)))
    }

    override fun realtimeState(state: RealtimeConnectionState, reconnectDelayMs: Long?) {
        AppLogger.event(TAG, "realtime_state", mapOf("state" to state.name, "reconnect_delay_ms" to reconnectDelayMs))
    }

    private fun runFields(context: SyncRunContext): Map<String, Any?> = mapOf(
        "sync_run_id" to context.syncRunId,
        "reason" to context.reason.name,
        "requested_generation" to context.requestedGeneration,
        "scope_fingerprint" to context.scopeFingerprint,
    )

    private fun fields(context: SyncRunContext?, additional: Map<String, Any?>): Map<String, Any?> =
        if (context == null) additional else runFields(context) + additional

    private companion object { const val TAG = "SyncDiagnostics" }
}
