package com.autodrive.app.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Durable exact-scope sync observability state. Diagnostic metadata only: no field in this table
 * is permitted to decide whether business data is sent, applied, checkpointed, or repaired.
 */
@Entity(
    tableName = "sync_observability_state",
    primaryKeys = ["user_id", "client_id", "org_id", "stream"],
)
data class SyncObservabilityStateEntity(
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "client_id") val clientId: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    val stream: String,
    @ColumnInfo(name = "contract_version") val contractVersion: Int,
    @ColumnInfo(name = "last_sync_run_id") val lastSyncRunId: String? = null,
    @ColumnInfo(name = "last_sync_status") val lastSyncStatus: String? = null,
    @ColumnInfo(name = "last_sync_started_at_local") val lastSyncStartedAtLocal: Long? = null,
    @ColumnInfo(name = "last_sync_completed_at_local") val lastSyncCompletedAtLocal: Long? = null,
    @ColumnInfo(name = "last_success_at_local") val lastSuccessAtLocal: Long? = null,
    @ColumnInfo(name = "last_failure_phase") val lastFailurePhase: String? = null,
    @ColumnInfo(name = "last_failure_code") val lastFailureCode: String? = null,
    @ColumnInfo(name = "last_local_cursor_revision") val lastLocalCursorRevision: String? = null,
    @ColumnInfo(name = "last_server_head_revision") val lastServerHeadRevision: String? = null,
    @ColumnInfo(name = "last_server_head_observed_at") val lastServerHeadObservedAt: Long? = null,
    @ColumnInfo(name = "last_successful_bootstrap_at") val lastSuccessfulBootstrapAt: Long? = null,
    @ColumnInfo(name = "last_bootstrap_duration_ms") val lastBootstrapDurationMs: Long? = null,
    @ColumnInfo(name = "bootstrap_count") val bootstrapCount: Long = 0,
    @ColumnInfo(name = "cursor_expiry_count") val cursorExpiryCount: Long = 0,
    @ColumnInfo(name = "last_reconciliation_at") val lastReconciliationAt: Long? = null,
    @ColumnInfo(name = "last_reconciliation_result") val lastReconciliationResult: String? = null,
    @ColumnInfo(name = "reconciliation_mismatch_count") val reconciliationMismatchCount: Long = 0,
    @ColumnInfo(name = "reconciliation_repair_count") val reconciliationRepairCount: Long = 0,
    @ColumnInfo(name = "rebootstrap_count") val rebootstrapCount: Long = 0,
    @ColumnInfo(name = "outbox_conflict_count") val outboxConflictCount: Long = 0,
    @ColumnInfo(name = "hint_received_count") val hintReceivedCount: Long = 0,
    @ColumnInfo(name = "hint_trailing_run_count") val hintTrailingRunCount: Long = 0,
    @ColumnInfo(name = "hint_dropped_count") val hintDroppedCount: Long = 0,
    @ColumnInfo(name = "last_realtime_state") val lastRealtimeState: String? = null,
    @ColumnInfo(name = "last_realtime_state_at") val lastRealtimeStateAt: Long? = null,
    @ColumnInfo(name = "last_failed_realtime_participants") val lastFailedRealtimeParticipants: Int? = null,
    @ColumnInfo(name = "updated_at_local") val updatedAtLocal: Long,
)
