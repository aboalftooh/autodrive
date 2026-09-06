package com.autodrive.app.core.sync.data

/** Ordinary absence from a bounded/RLS-filtered pull is never a deletion signal. */
internal sealed interface RemoteSyncChange<out T> {
    data class Upsert<T>(val value: T) : RemoteSyncChange<T>
    data object Unchanged : RemoteSyncChange<Nothing>
    data class ExplicitDeletion(val remoteId: String) : RemoteSyncChange<Nothing>
    data class AbsentOrUnknown(val remoteId: String? = null) : RemoteSyncChange<Nothing>
}

/** Canonical Android-side tombstone. Server-specific field names must be mapped by a verified adapter. */
data class DeletionEnvelope(
    val eventId: String,
    val entityType: String,
    val entityId: String,
    val scope: SyncScope,
    /** Authoritative data revision only when supplied by the deletion feed. Never synthesized. */
    val serverRevision: String? = null,
    /** Server transaction-group identity only when supplied by the deletion feed. Never synthesized. */
    val transactionGroupId: String? = null,
)

data class DeletionBatch(
    val deletions: List<DeletionEnvelope>,
    /** Opaque server-owned resume token. Android never derives or interprets it. */
    val nextCursor: String?,
)

/**
 * Server deletion feed boundary. Implementations may populate [DeletionBatch.nextCursor] only
 * when the verified server contract says it is safe to resume from that token.
 */
fun interface DeletionFeed {
    suspend fun changesSince(scope: SyncScope, cursor: String?, limit: Int): DeletionBatch
}

class ServerTombstoneContractUnavailableException : IllegalStateException(
    "SERVER_TOMBSTONE_CONTRACT_UNAVAILABLE",
)

/**
 * Deliberate fail-closed binding for v67 when no authoritative sync_tombstones schema/RPC contract
 * is present in the source archive. Replacing this requires verified server field/cursor semantics.
 */
class BlockedServerDeletionFeed : DeletionFeed {
    override suspend fun changesSince(scope: SyncScope, cursor: String?, limit: Int): DeletionBatch {
        throw ServerTombstoneContractUnavailableException()
    }
}
