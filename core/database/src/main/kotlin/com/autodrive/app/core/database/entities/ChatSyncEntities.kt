package com.autodrive.app.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "chat_recovery_checkpoints",
    primaryKeys = ["user_id", "client_id", "org_id", "conversation_id"],
)
data class ChatRecoveryCheckpointEntity(
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "client_id") val clientId: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    @ColumnInfo(name = "last_created_at_server") val lastCreatedAtServer: String,
    @ColumnInfo(name = "last_message_id") val lastMessageId: String,
    @ColumnInfo(name = "last_server_sequence") val lastServerSequence: Long = 0L,
    @ColumnInfo(name = "contract_version") val contractVersion: Int = 2,
    @ColumnInfo(name = "updated_at_local") val updatedAtLocal: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "chat_media_transfers",
    indices = [
        Index(
            value = ["user_id", "client_id", "org_id", "status", "next_retry_at", "created_at"],
            name = "index_chat_media_transfers_scope_status_retry_created",
        ),
        Index(
            value = ["user_id", "client_id", "org_id", "message_id"],
            unique = true,
            name = "index_chat_media_transfers_scope_message",
        ),
    ],
)
data class ChatMediaTransferEntity(
    @androidx.room.PrimaryKey
    @ColumnInfo(name = "transfer_id") val transferId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "client_id") val clientId: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    @ColumnInfo(name = "message_id") val messageId: String,
    val direction: String = "OUTBOUND",
    @ColumnInfo(name = "local_path") val localPath: String,
    @ColumnInfo(name = "media_mime") val mediaMime: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "content_sha256") val contentSha256: String,
    val bucket: String,
    @ColumnInfo(name = "object_path") val objectPath: String,
    @ColumnInfo(name = "remote_reference") val remoteReference: String? = null,
    val status: String = "PENDING",
    @ColumnInfo(name = "attempt_count") val attemptCount: Int = 0,
    @ColumnInfo(name = "next_retry_at") val nextRetryAt: Long = 0L,
    @ColumnInfo(name = "lease_until") val leaseUntil: Long = 0L,
    @ColumnInfo(name = "last_error_code") val lastErrorCode: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = createdAt,
)

object ChatMediaTransferStatus {
    const val PENDING = "PENDING"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val COMPLETE = "COMPLETE"
    const val DEAD_LETTER = "DEAD_LETTER"
}
