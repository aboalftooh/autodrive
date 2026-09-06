package com.autodrive.app.core.sync.data

import androidx.room.withTransaction
import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.database.entities.SyncCursorEntity
import com.autodrive.app.core.database.entities.SyncInboxEntity
import com.autodrive.app.core.session.domain.SessionReader
import javax.inject.Inject
import javax.inject.Singleton

class TombstoneValidationException(message: String) : IllegalStateException(message)
class PendingLocalConflictException(message: String) : IllegalStateException(message)
class StaleSyncScopeException : IllegalStateException("STALE_SYNC_SCOPE")
class InboxEventIdentityConflictException : IllegalStateException("INBOX_EVENT_IDENTITY_CONFLICT")

@Singleton
class DeletionSynchronizer @Inject constructor(
    private val db: AutoDriveDatabase,
    private val sessionReader: SessionReader,
    private val feed: DeletionFeed,
) {
    suspend fun synchronize(scope: SyncScope): Int {
        var applied = 0
        repeat(MAX_PAGES_PER_CYCLE) {
            val current = db.syncCursorDao().get(
                scope.userId,
                scope.clientId,
                scope.orgId,
                TOMBSTONE_STREAM,
            )?.cursorToken

            // Network I/O stays outside Room. The fetched page is validated before any local mutation.
            val batch = feed.changesSince(scope, current, PAGE_SIZE)
            validate(batch, scope, current)
            val next = batch.nextCursor
            if (batch.deletions.isEmpty() && next == null) return applied

            var newlyApplied = 0
            db.withTransaction {
                requireCurrentScope(scope)
                batch.deletions.forEach { deletion ->
                    val existing = db.syncInboxDao().get(
                        scope.userId,
                        scope.clientId,
                        scope.orgId,
                        TOMBSTONE_STREAM,
                        deletion.eventId,
                    )
                    if (existing != null) {
                        requireSameIdentity(existing, deletion)
                        if (existing.appliedAt != null) return@forEach
                    } else {
                        db.syncInboxDao().insert(deletion.toInbox(receivedAt = System.currentTimeMillis()))
                    }

                    applyDeletion(scope, deletion)
                    db.syncInboxDao().markApplied(
                        scope.userId,
                        scope.clientId,
                        scope.orgId,
                        TOMBSTONE_STREAM,
                        deletion.eventId,
                        System.currentTimeMillis(),
                    )
                    newlyApplied += 1
                }
                if (next != null) {
                    db.syncCursorDao().upsert(
                        SyncCursorEntity(
                            userId = scope.userId,
                            clientId = scope.clientId,
                            orgId = scope.orgId,
                            stream = TOMBSTONE_STREAM,
                            cursorToken = next,
                            contractVersion = 1,
                            updatedAt = System.currentTimeMillis(), // diagnostics only; never ordering authority
                        ),
                    )
                }
            }
            applied += newlyApplied
            if (batch.deletions.size < PAGE_SIZE) return applied
        }
        throw TombstoneValidationException("TOMBSTONE_PAGE_LIMIT_EXCEEDED")
    }

    private fun validate(batch: DeletionBatch, scope: SyncScope, current: String?) {
        if (batch.deletions.isNotEmpty() && batch.nextCursor.isNullOrBlank()) {
            throw TombstoneValidationException("INVALID_CURSOR_BATCH")
        }
        if (batch.deletions.isNotEmpty() && batch.nextCursor == current) {
            throw TombstoneValidationException("NON_ADVANCING_CURSOR")
        }
        batch.deletions.forEach { deletion ->
            if (deletion.eventId.isBlank() || deletion.entityId.isBlank()) {
                throw TombstoneValidationException("REMOTE_BATCH_VALIDATION_FAILED")
            }
            if (deletion.scope != scope) throw TombstoneValidationException("TOMBSTONE_SCOPE_MISMATCH")
            if (deletion.entityType !in SUPPORTED_ENTITY_TYPES) {
                throw TombstoneValidationException("UNSUPPORTED_TOMBSTONE_ENTITY")
            }
        }
    }

    private fun requireCurrentScope(scope: SyncScope) {
        if (SyncScope.from(sessionReader.currentSession()) != scope) throw StaleSyncScopeException()
    }

    private fun requireSameIdentity(existing: SyncInboxEntity, deletion: DeletionEnvelope) {
        val expectedRevisionKind = if (deletion.serverRevision == null) REVISION_NONE else REVISION_DATA_CHANGE
        if (
            existing.entityType != deletion.entityType ||
            existing.entityId != deletion.entityId ||
            existing.operation != OPERATION_DELETE ||
            existing.serverRevision != deletion.serverRevision ||
            existing.revisionKind != expectedRevisionKind ||
            existing.transactionGroupId != deletion.transactionGroupId
        ) {
            throw InboxEventIdentityConflictException()
        }
    }

    private fun DeletionEnvelope.toInbox(receivedAt: Long) = SyncInboxEntity(
        userId = scope.userId,
        clientId = scope.clientId,
        orgId = scope.orgId,
        stream = TOMBSTONE_STREAM,
        eventId = eventId,
        serverRevision = serverRevision,
        revisionKind = if (serverRevision == null) REVISION_NONE else REVISION_DATA_CHANGE,
        entityType = entityType,
        entityId = entityId,
        operation = OPERATION_DELETE,
        transactionGroupId = transactionGroupId,
        receivedAt = receivedAt,
        appliedAt = null,
        contractVersion = 1,
    )

    private suspend fun applyDeletion(scope: SyncScope, deletion: DeletionEnvelope) {
        val id = deletion.entityId
        when (deletion.entityType) {
            "autodrive_users" -> {
                val local = db.autoDriveUserDao().get(scope.userId)
                if (local?.id == id && local.syncStatus != "SYNCED") {
                    throw PendingLocalConflictException("PENDING_LOCAL_CONFLICT")
                }
                db.autoDriveUserDao().deleteByIdForUser(id, scope.userId)
            }
            "invoices" -> db.invoiceDao().deleteByIdForClient(id, scope.clientId)
            "payments" -> db.paymentDao().deleteById(id)
            "commission_payments" -> db.commissionPaymentDao().deleteByIdForClient(id, scope.clientId)
            "marketer_balance" -> {
                val local = db.marketerBalanceDao().get(scope.userId)
                if (local?.id == id) db.marketerBalanceDao().deleteByUserId(scope.userId)
            }
            "balance_transactions" -> {
                val local = db.balanceTransactionDao().getById(id)
                if (local?.syncStatus != null && local.syncStatus != "SYNCED") {
                    throw PendingLocalConflictException("PENDING_LOCAL_CONFLICT")
                }
                db.balanceTransactionDao().deleteByIdForUser(id, scope.userId)
            }
            "withdrawal_requests" -> {
                val local = db.withdrawalRequestDao().getById(id)
                if (local?.syncStatus != null && local.syncStatus != "SYNCED") {
                    throw PendingLocalConflictException("PENDING_LOCAL_CONFLICT")
                }
                db.withdrawalRequestDao().deleteByIdForUser(id, scope.userId)
            }
            "notifications" -> {
                val local = db.notificationDao().getById(id, scope.userId)
                if (local?.readSynced == false) throw PendingLocalConflictException("PENDING_LOCAL_CONFLICT")
                db.notificationDao().deleteById(id, scope.userId)
            }
            "conversations" -> {
                val pendingMessages = db.chatMessageDao().getByStatus("PENDING")
                    .any { it.conversationId == id }
                if (pendingMessages) throw PendingLocalConflictException("PENDING_LOCAL_CONFLICT")
                db.chatMessageDao().deleteByConversationIds(listOf(id))
                db.conversationDao().deleteByIdForClient(id, scope.clientId)
            }
            "internal_messages" -> {
                val local = db.chatMessageDao().getById(id)
                if (local?.status == "PENDING" || local?.status == "SENDING") {
                    throw PendingLocalConflictException("PENDING_LOCAL_CONFLICT")
                }
                db.chatMessageDao().deleteById(id)
            }
            else -> throw TombstoneValidationException("UNKNOWN_ENTITY_TYPE")
        }
    }

    companion object {
        const val TOMBSTONE_STREAM = "core-tombstones-v1"
        const val PAGE_SIZE = 200
        const val MAX_PAGES_PER_CYCLE = 50
        const val OPERATION_DELETE = "DELETE"
        const val REVISION_NONE = "NONE"
        const val REVISION_DATA_CHANGE = "DATA_CHANGE"
        val SUPPORTED_ENTITY_TYPES = setOf(
            "autodrive_users",
            "invoices",
            "payments",
            "commission_payments",
            "marketer_balance",
            "balance_transactions",
            "withdrawal_requests",
            "notifications",
            "conversations",
            "internal_messages",
        )
    }
}
