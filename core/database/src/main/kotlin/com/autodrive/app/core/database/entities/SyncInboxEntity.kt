package com.autodrive.app.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/** Durable scoped inbound event ledger. Raw server payloads are intentionally not stored. */
@Entity(
    tableName = "sync_inbox",
    primaryKeys = ["user_id", "client_id", "org_id", "stream", "event_id"],
    indices = [
        Index(
            value = ["user_id", "client_id", "org_id", "stream", "applied_at"],
            name = "index_sync_inbox_scope_stream_applied",
        ),
    ],
)
data class SyncInboxEntity(
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "client_id") val clientId: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    val stream: String,
    @ColumnInfo(name = "event_id") val eventId: String,
    @ColumnInfo(name = "server_revision") val serverRevision: String?,
    @ColumnInfo(name = "revision_kind") val revisionKind: String,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    val operation: String,
    @ColumnInfo(name = "transaction_group_id") val transactionGroupId: String?,
    @ColumnInfo(name = "received_at") val receivedAt: Long,
    @ColumnInfo(name = "applied_at") val appliedAt: Long?,
    @ColumnInfo(name = "contract_version") val contractVersion: Int = 1,
)
