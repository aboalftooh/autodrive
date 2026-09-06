package com.autodrive.app.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Durable state for one canonical bootstrap. The primary key is the exact sync scope + stream;
 * a bootstrap id is never shared between accounts or organisations.
 */
@Entity(
    tableName = "sync_bootstrap_state",
    primaryKeys = ["user_id", "client_id", "org_id", "stream"],
    indices = [
        Index(
            value = ["user_id", "client_id", "org_id", "bootstrap_id"],
            unique = true,
            name = "index_sync_bootstrap_state_scope_bootstrap",
        ),
    ],
)
data class SyncBootstrapStateEntity(
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "client_id") val clientId: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    val stream: String,
    @ColumnInfo(name = "bootstrap_id") val bootstrapId: String,
    @ColumnInfo(name = "baseline_revision") val baselineRevision: String,
    val status: String,
    @ColumnInfo(name = "contract_version") val contractVersion: Int,
    @ColumnInfo(name = "next_page_token") val nextPageToken: String?,
    @ColumnInfo(name = "started_at_local") val startedAtLocal: Long,
    @ColumnInfo(name = "updated_at_local") val updatedAtLocal: Long,
)

/**
 * Immutable canonical rows downloaded for a bootstrap. They are not Inbox events and carry no
 * fabricated event id/revision/transaction group.
 */
@Entity(
    tableName = "sync_bootstrap_staging",
    primaryKeys = ["user_id", "client_id", "org_id", "bootstrap_id", "entity_type", "entity_id"],
    indices = [
        Index(
            value = ["user_id", "client_id", "org_id", "bootstrap_id", "entity_type"],
            name = "index_sync_bootstrap_staging_scope_bootstrap_entity",
        ),
    ],
)
data class SyncBootstrapStagingEntity(
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "client_id") val clientId: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    @ColumnInfo(name = "bootstrap_id") val bootstrapId: String,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    @ColumnInfo(name = "canonical_payload") val canonicalPayload: String,
    @ColumnInfo(name = "canonical_digest") val canonicalDigest: String?,
)

/** Minimal durable reconciliation scheduling/result state. Metrics remain a Session 73 concern. */
@Entity(
    tableName = "sync_reconciliation_state",
    primaryKeys = ["user_id", "client_id", "org_id", "stream"],
)
data class SyncReconciliationStateEntity(
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "client_id") val clientId: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    val stream: String,
    @ColumnInfo(name = "last_checked_revision") val lastCheckedRevision: String?,
    @ColumnInfo(name = "last_result") val lastResult: String,
    @ColumnInfo(name = "contract_version") val contractVersion: Int,
    @ColumnInfo(name = "next_due_at_local") val nextDueAtLocal: Long,
    @ColumnInfo(name = "updated_at_local") val updatedAtLocal: Long,
)
