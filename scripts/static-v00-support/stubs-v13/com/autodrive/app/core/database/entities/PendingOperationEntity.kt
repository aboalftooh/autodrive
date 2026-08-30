package com.autodrive.app.core.database.entities
data class PendingOperationEntity(
    val id: String,
    val tableName: String,
    val operation: String,
    val payload: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING",
    val attemptCount: Int = 0,
    val nextRetryAt: Long = 0L,
    val lastErrorCode: String? = null,
    val lastErrorMessage: String? = null,
    val payloadVersion: Int = 1,
    val idempotencyKey: String = id,
)
