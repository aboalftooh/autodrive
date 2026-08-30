package com.autodrive.app.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Durable opaque server cursor scoped to one authenticated principal and logical stream.
 * The token is never interpreted by Android; ordering semantics belong to the server contract.
 */
@Entity(
    tableName = "sync_cursors",
    primaryKeys = ["user_id", "client_id", "org_id", "stream"],
)
data class SyncCursorEntity(
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "client_id") val clientId: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    val stream: String,
    @ColumnInfo(name = "cursor_token") val cursorToken: String,
    @ColumnInfo(name = "contract_version") val contractVersion: Int = 1,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
