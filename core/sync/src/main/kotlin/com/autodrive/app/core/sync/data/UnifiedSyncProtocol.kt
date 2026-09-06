package com.autodrive.app.core.sync.data

import com.autodrive.app.core.network.AutoDriveSupabase
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Canonical client constants for the server-revision synchronization protocol.
 *
 * These values participate in persisted cursor/bootstrap compatibility and remote RPC validation;
 * changing them is a protocol change, not a tuning-only edit. Server scope authority remains the
 * authenticated identity rather than user/client/org values supplied by the Android client.
 */
object UnifiedSyncContract {
    const val STREAM = "autodrive-global-change-v1"
    const val CONTRACT_VERSION = 2
    const val PAGE_SIZE = 200
    const val MAX_PAGES_PER_CYCLE = 50
    const val BOOTSTRAP_PAGE_SIZE = 500
    const val RECONCILIATION_CONTRACT_VERSION = 1
}

object ChangeOperation {
    const val UPSERT = "UPSERT"
    const val DELETE = "DELETE"
}

@Serializable
data class ChangeEventDto(
    @SerialName("event_id") val eventId: String,
    val revision: Long,
    @SerialName("entity_type") val entityType: String,
    @SerialName("entity_id") val entityId: String,
    val operation: String,
    @SerialName("transaction_group_id") val transactionGroupId: String,
    @SerialName("occurred_at") val occurredAt: String,
    @SerialName("contract_version") val contractVersion: Int,
    @SerialName("user_id") val userId: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("org_id") val orgId: String,
    val payload: JsonObject? = null,
)

@Serializable
data class ChangeFeedPageDto(
    val status: String = "OK",
    @SerialName("contract_version") val contractVersion: Int,
    @SerialName("head_revision") val headRevision: Long,
    @SerialName("minimum_available_revision") val minimumAvailableRevision: Long,
    val events: List<ChangeEventDto> = emptyList(),
    @SerialName("next_revision") val nextRevision: Long,
    @SerialName("has_more") val hasMore: Boolean,
)

@Serializable
data class BootstrapBeginDto(
    val status: String = "OK",
    @SerialName("bootstrap_id") val bootstrapId: String,
    @SerialName("baseline_revision") val baselineRevision: Long,
    @SerialName("contract_version") val contractVersion: Int,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class BootstrapRowDto(
    @SerialName("entity_type") val entityType: String,
    @SerialName("entity_id") val entityId: String,
    val payload: JsonObject,
    val digest: String,
)

@Serializable
data class BootstrapPageDto(
    val status: String = "OK",
    @SerialName("bootstrap_id") val bootstrapId: String,
    @SerialName("contract_version") val contractVersion: Int,
    val rows: List<BootstrapRowDto> = emptyList(),
    @SerialName("next_page_token") val nextPageToken: String? = null,
    @SerialName("has_more") val hasMore: Boolean,
)

@Serializable
data class ManifestPartitionDto(
    @SerialName("entity_type") val entityType: String,
    val partition: String,
    val count: Int,
    val digest: String,
)

@Serializable
data class ReconciliationManifestDto(
    @SerialName("contract_version") val contractVersion: Int,
    @SerialName("manifest_revision") val manifestRevision: Long,
    val status: String = "OK",
    val partitions: List<ManifestPartitionDto>,
)

@Serializable
data class PartitionInventoryRowDto(
    @SerialName("entity_id") val entityId: String,
    val digest: String,
    val payload: JsonObject,
)

@Serializable
data class PartitionInventoryDto(
    val status: String = "OK",
    @SerialName("contract_version") val contractVersion: Int,
    @SerialName("manifest_revision") val manifestRevision: Long,
    @SerialName("entity_type") val entityType: String,
    val partition: String,
    val rows: List<PartitionInventoryRowDto>,
)

/**
 * Source boundary for ordered canonical delta pages after a persisted server revision.
 *
 * Implementations must not treat device wall-clock time as ordering authority. Returned events are
 * validated for exact [SyncScope], protocol version, monotonic revision, and transaction grouping
 * before they may advance the durable Room cursor.
 */
interface UnifiedChangeFeed {
    suspend fun fetch(scope: SyncScope, afterRevision: Long, limit: Int): ChangeFeedPageDto
}

/**
 * Source boundary for a restartable snapshot tied to a server-issued bootstrap identity/baseline.
 *
 * A bootstrap is only safe when every page belongs to the same bootstrap id/contract and the full
 * staged snapshot is installed transactionally before delta synchronization resumes.
 */
interface BootstrapSnapshotSource {
    suspend fun begin(scope: SyncScope): BootstrapBeginDto
    suspend fun page(scope: SyncScope, bootstrapId: String, pageToken: String?, limit: Int): BootstrapPageDto
}

/**
 * Source boundary for anti-entropy comparison at one stable server manifest revision.
 *
 * Partition inventories are meaningful only for the manifest revision/entity/partition requested;
 * stale or mismatched responses must not be applied as canonical repair data.
 */
interface ReconciliationManifestSource {
    suspend fun manifest(scope: SyncScope): ReconciliationManifestDto
    suspend fun partition(scope: SyncScope, manifestRevision: Long, entityType: String, partition: String): PartitionInventoryDto
}

@Serializable
private data class ChangeFeedParams(
    @SerialName("p_after_revision") val afterRevision: Long,
    @SerialName("p_page_limit") val pageLimit: Int,
)

@Serializable
private data class BootstrapBeginParams(
    // Deliberately empty from an authority perspective. Server derives scope from auth.uid().
    @SerialName("p_contract_version") val contractVersion: Int = UnifiedSyncContract.CONTRACT_VERSION,
)

@Serializable
private data class BootstrapPageParams(
    @SerialName("p_bootstrap_id") val bootstrapId: String,
    @SerialName("p_after_token") val afterToken: String?,
    @SerialName("p_page_limit") val pageLimit: Int,
)

@Serializable
private data class ManifestParams(
    @SerialName("p_contract_version") val contractVersion: Int = UnifiedSyncContract.RECONCILIATION_CONTRACT_VERSION,
)

@Serializable
private data class PartitionParams(
    @SerialName("p_manifest_revision") val manifestRevision: Long,
    @SerialName("p_entity_type") val entityType: String,
    @SerialName("p_partition") val partition: String,
)

/**
 * Supabase RPC implementation of the canonical change-feed, bootstrap, and reconciliation sources.
 *
 * Exact tenant scope is intentionally not serialized for these RPCs: the server is expected to
 * derive scope from the authenticated identity. The [SyncScope] argument is retained as the local
 * correctness boundary consumed by higher layers, not as client-authoritative tenant input.
 */
@Singleton
class SupabaseUnifiedSyncGateway @Inject constructor(
    private val supabase: AutoDriveSupabase,
) : UnifiedChangeFeed, BootstrapSnapshotSource, ReconciliationManifestSource {
    override suspend fun fetch(scope: SyncScope, afterRevision: Long, limit: Int): ChangeFeedPageDto {
        require(limit in 1..1000) { "INVALID_CHANGE_PAGE_LIMIT" }
        // [scope] is intentionally not serialized. The RPC derives exact scope from auth.uid().
        return supabase.client.postgrest.rpc(
            "autodrive_sync_changes_v1",
            ChangeFeedParams(afterRevision, limit),
        ).decodeAs<ChangeFeedPageDto>()
    }

    override suspend fun begin(scope: SyncScope): BootstrapBeginDto =
        supabase.client.postgrest.rpc(
            "autodrive_sync_bootstrap_begin_v1",
            BootstrapBeginParams(),
        ).decodeAs<BootstrapBeginDto>()

    override suspend fun page(
        scope: SyncScope,
        bootstrapId: String,
        pageToken: String?,
        limit: Int,
    ): BootstrapPageDto {
        require(bootstrapId.isNotBlank()) { "BOOTSTRAP_ID_MISSING" }
        require(limit in 1..1000) { "INVALID_BOOTSTRAP_PAGE_LIMIT" }
        return supabase.client.postgrest.rpc(
            "autodrive_sync_bootstrap_page_v1",
            BootstrapPageParams(bootstrapId, pageToken, limit),
        ).decodeAs<BootstrapPageDto>()
    }

    override suspend fun manifest(scope: SyncScope): ReconciliationManifestDto =
        supabase.client.postgrest.rpc(
            "autodrive_sync_manifest_v1",
            ManifestParams(),
        ).decodeAs<ReconciliationManifestDto>()

    override suspend fun partition(
        scope: SyncScope,
        manifestRevision: Long,
        entityType: String,
        partition: String,
    ): PartitionInventoryDto =
        supabase.client.postgrest.rpc(
            "autodrive_sync_partition_v1",
            PartitionParams(manifestRevision, entityType, partition),
        ).decodeAs<PartitionInventoryDto>()
}
