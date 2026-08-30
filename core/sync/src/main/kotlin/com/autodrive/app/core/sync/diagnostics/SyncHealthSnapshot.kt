package com.autodrive.app.core.sync.diagnostics

/** Read-only diagnostic projection. UNKNOWN remote freshness is represented by null, never fabricated. */
data class SyncHealthSnapshot(
    val scopeFingerprint: String,
    val syncStatus: String?,
    val localCursorRevision: Long?,
    val serverHeadRevision: Long?,
    val revisionLag: Long?,
    val serverHeadObservedAt: Long?,
    val pendingCount: Int,
    val oldestOutboxAgeMs: Long,
    val retryCount: Long,
    val deadLetterCount: Int,
    val conflictCount: Long,
    val failedRealtimeParticipants: Int?,
    val lastSuccessfulBootstrapAt: Long?,
    val lastReconciliationAt: Long?,
    val lastReconciliationResult: String?,
    val cursorExpiryCount: Long,
    val hintReceivedCount: Long,
    val hintTrailingRunCount: Long,
    val hintDroppedCount: Long,
    val lastFailurePhase: String?,
    val lastFailureCode: String?,
    val lastRealtimeState: String?,
    val lastRealtimeStateAt: Long?,
    val freshness: String,
)
