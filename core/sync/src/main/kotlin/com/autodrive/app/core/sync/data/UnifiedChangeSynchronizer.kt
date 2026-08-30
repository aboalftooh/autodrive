package com.autodrive.app.core.sync.data

import androidx.room.withTransaction
import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.database.entities.SyncCursorEntity
import com.autodrive.app.core.database.entities.SyncInboxEntity
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.sync.diagnostics.SyncDiagnostics
import com.autodrive.app.core.sync.diagnostics.SyncObservabilityStore
import com.autodrive.app.core.sync.diagnostics.SyncRunContext
import com.autodrive.app.core.sync.fault.FaultContext
import com.autodrive.app.core.sync.fault.SyncFaultInjector
import com.autodrive.app.core.sync.fault.SyncFaultPoint
import javax.inject.Inject
import javax.inject.Singleton

class CanonicalCursorMissingException : IllegalStateException("CANONICAL_CURSOR_MISSING")
class CursorExpiredException : IllegalStateException("CURSOR_EXPIRED")
class RemoteChangeContractException(code: String) : IllegalStateException(code)

data class UnifiedChangeResult(
    val appliedEvents: Int,
    val cursorRevision: Long,
    val headRevision: Long,
    val hasMore: Boolean,
)

/**
 * Applies the canonical server-revision change feed to Room for one exact [SyncScope].
 *
 * Event identity is persisted in the Inbox and each contiguous server transaction group is applied
 * in a Room transaction with cursor advancement. Replays with the same event identity are safe only
 * when their immutable identity fields match. Scope, protocol, monotonic revision, entity/operation,
 * and group-boundary violations fail closed; a missing/expired cursor is recovered by bootstrap.
 */
@Singleton
class UnifiedChangeSynchronizer @Inject constructor(
    private val db: AutoDriveDatabase,
    private val feed: UnifiedChangeFeed,
    private val applier: ChangeEventApplier,
    private val sessionReader: SessionReader,
    private val diagnostics: SyncDiagnostics,
    private val observabilityStore: SyncObservabilityStore,
    private val faultInjector: SyncFaultInjector,
) {
    suspend fun synchronize(scope: SyncScope, context: SyncRunContext? = null): UnifiedChangeResult {
        var cursor = readCursor(scope)
        var applied = 0
        var head = cursor
        var hasMore = false
        var pageCount = 0

        do {
            if (++pageCount > UnifiedSyncContract.MAX_PAGES_PER_CYCLE) {
                return finish(scope, context, UnifiedChangeResult(applied, cursor, head, hasMore = true))
            }
            val page = feed.fetch(scope, cursor, UnifiedSyncContract.PAGE_SIZE)
            faultInjector.hit(
                SyncFaultPoint.CHANGE_FEED_AFTER_FETCH_BEFORE_APPLY,
                FaultContext(syncRunId = context?.syncRunId, revision = page.headRevision),
            )
            validatePage(scope, cursor, page)
            if (page.status == "CURSOR_EXPIRED") throw CursorExpiredException()
            head = page.headRevision

            var lastApplied = cursor
            val groups = contiguousGroups(page.events)
            for (group in groups) {
                val groupHigh = group.maxOf { it.revision }
                db.withTransaction {
                    requireCurrentScope(scope)
                    for (event in group) applyOne(scope, event)
                    faultInjector.hit(
                        SyncFaultPoint.CHANGE_GROUP_AFTER_ENTITY_APPLY_BEFORE_CURSOR_COMMIT,
                        FaultContext(
                            syncRunId = context?.syncRunId,
                            eventId = group.lastOrNull()?.eventId,
                            revision = groupHigh,
                        ),
                    )
                    db.syncCursorDao().upsert(cursor(scope, groupHigh))
                }
                diagnostics.changeGroup(
                    context = context,
                    transactionGroupId = group.first().transactionGroupId,
                    firstEventId = group.first().eventId,
                    lastEventId = group.last().eventId,
                    firstRevision = group.first().revision,
                    lastRevision = groupHigh,
                    eventCount = group.size,
                    entityTypes = group.map { it.entityType }.distinct().sorted().joinToString("|"),
                    operations = group.map { it.operation }.distinct().sorted().joinToString("|"),
                )
                lastApplied = groupHigh
                applied += group.size
            }

            if (page.nextRevision > lastApplied) {
                db.withTransaction {
                    requireCurrentScope(scope)
                    db.syncCursorDao().upsert(cursor(scope, page.nextRevision))
                }
                lastApplied = page.nextRevision
            }
            cursor = lastApplied
            hasMore = page.hasMore
            faultInjector.hit(
                SyncFaultPoint.CHANGE_PAGE_AFTER_COMMIT_BEFORE_NEXT_FETCH,
                FaultContext(syncRunId = context?.syncRunId, revision = cursor),
            )
        } while (hasMore)

        return finish(scope, context, UnifiedChangeResult(applied, cursor, head, hasMore = false))
    }

    suspend fun currentRevision(scope: SyncScope): Long? = db.syncCursorDao().get(
        scope.userId, scope.clientId, scope.orgId, UnifiedSyncContract.STREAM,
    )?.let(::parseCursor)

    private suspend fun finish(scope: SyncScope, context: SyncRunContext?, result: UnifiedChangeResult): UnifiedChangeResult {
        diagnostics.changeFeed(context, result.appliedEvents, result.cursorRevision, result.headRevision, result.hasMore)
        runCatching { observabilityStore.feedObserved(scope, result) }
        return result
    }

    private suspend fun readCursor(scope: SyncScope): Long {
        val row = db.syncCursorDao().get(scope.userId, scope.clientId, scope.orgId, UnifiedSyncContract.STREAM)
            ?: throw CanonicalCursorMissingException()
        if (row.contractVersion != UnifiedSyncContract.CONTRACT_VERSION) {
            throw RemoteChangeContractException("SYNC_PROTOCOL_VERSION_UNSUPPORTED")
        }
        return parseCursor(row)
    }

    private fun parseCursor(row: SyncCursorEntity): Long = row.cursorToken.toLongOrNull()
        ?.takeIf { it >= 0L }
        ?: throw RemoteChangeContractException("REVISION_OUT_OF_RANGE")

    private fun cursor(scope: SyncScope, revision: Long) = SyncCursorEntity(
        userId = scope.userId,
        clientId = scope.clientId,
        orgId = scope.orgId,
        stream = UnifiedSyncContract.STREAM,
        cursorToken = revision.toString(),
        contractVersion = UnifiedSyncContract.CONTRACT_VERSION,
        updatedAt = System.currentTimeMillis(),
    )

    private fun validatePage(scope: SyncScope, cursor: Long, page: ChangeFeedPageDto) {
        if (page.status == "CURSOR_EXPIRED") return
        if (page.status != "OK") throw RemoteChangeContractException("REMOTE_CHANGE_STATUS:${page.status}")
        if (page.contractVersion != UnifiedSyncContract.CONTRACT_VERSION) throw RemoteChangeContractException("SYNC_PROTOCOL_VERSION_UNSUPPORTED")
        if (page.nextRevision < cursor || page.headRevision < page.nextRevision) throw RemoteChangeContractException("NON_ADVANCING_CURSOR")
        var previous = cursor
        val closedGroups = mutableSetOf<String>()
        var openGroup: String? = null
        val seenRevisions = mutableSetOf<Long>()
        for (event in page.events) {
            if (event.contractVersion != UnifiedSyncContract.CONTRACT_VERSION) throw RemoteChangeContractException("SYNC_PROTOCOL_VERSION_UNSUPPORTED")
            if (event.userId != scope.userId || event.clientId != scope.clientId || event.orgId != scope.orgId) throw RemoteScopeMismatchException()
            if (event.eventId.isBlank() || event.entityId.isBlank() || event.transactionGroupId.isBlank()) throw RemoteChangeContractException("MALFORMED_EVENT")
            if (event.entityType !in applier.supportedEntityTypes) throw UnsupportedChangeEntityException(event.entityType)
            if (event.operation != ChangeOperation.UPSERT && event.operation != ChangeOperation.DELETE) throw RemoteChangeContractException("UNSUPPORTED_CHANGE_OPERATION")
            if (event.revision <= previous) throw RemoteChangeContractException("REMOTE_REVISION_NOT_MONOTONIC")
            if (!seenRevisions.add(event.revision)) throw RemoteChangeContractException("DUPLICATE_DATA_REVISION")
            if (openGroup != event.transactionGroupId) {
                openGroup?.let(closedGroups::add)
                if (event.transactionGroupId in closedGroups) throw RemoteChangeContractException("SERVER_GROUP_BOUNDARY_CONTRACT")
                openGroup = event.transactionGroupId
            }
            previous = event.revision
        }
        val last = page.events.lastOrNull()?.revision ?: cursor
        if (page.nextRevision < last) throw RemoteChangeContractException("NON_ADVANCING_CURSOR")
    }

    private fun contiguousGroups(events: List<ChangeEventDto>): List<List<ChangeEventDto>> {
        if (events.isEmpty()) return emptyList()
        val groups = mutableListOf<MutableList<ChangeEventDto>>()
        for (event in events) {
            val current = groups.lastOrNull()
            if (current == null || current.first().transactionGroupId != event.transactionGroupId) groups += mutableListOf(event)
            else current += event
        }
        return groups
    }

    private suspend fun applyOne(scope: SyncScope, event: ChangeEventDto) {
        val inbox = db.syncInboxDao().get(scope.userId, scope.clientId, scope.orgId, UnifiedSyncContract.STREAM, event.eventId)
        if (inbox != null) {
            if (!sameIdentity(inbox, event)) throw InboxEventIdentityConflictException()
            if (inbox.appliedAt != null) return
        } else {
            db.syncInboxDao().insert(
                SyncInboxEntity(
                    userId = scope.userId, clientId = scope.clientId, orgId = scope.orgId,
                    stream = UnifiedSyncContract.STREAM, eventId = event.eventId,
                    serverRevision = event.revision.toString(), revisionKind = "DATA_CHANGE",
                    entityType = event.entityType, entityId = event.entityId, operation = event.operation,
                    transactionGroupId = event.transactionGroupId, receivedAt = System.currentTimeMillis(),
                    appliedAt = null, contractVersion = UnifiedSyncContract.CONTRACT_VERSION,
                ),
            )
        }
        applier.apply(scope, event)
        db.syncInboxDao().markApplied(
            scope.userId, scope.clientId, scope.orgId, UnifiedSyncContract.STREAM, event.eventId,
            System.currentTimeMillis(),
        )
    }

    private fun sameIdentity(inbox: SyncInboxEntity, event: ChangeEventDto): Boolean =
        inbox.serverRevision == event.revision.toString() && inbox.revisionKind == "DATA_CHANGE" &&
            inbox.entityType == event.entityType && inbox.entityId == event.entityId &&
            inbox.operation == event.operation && inbox.transactionGroupId == event.transactionGroupId &&
            inbox.contractVersion == event.contractVersion

    private fun requireCurrentScope(scope: SyncScope) {
        if (SyncScope.from(sessionReader.currentSession()) != scope) throw RemoteChangeContractException("STALE_SYNC_SCOPE")
    }
}
