package com.autodrive.app.core.sync.data

import androidx.room.withTransaction
import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.database.entities.PendingOperationEntity
import com.autodrive.app.core.network.dto.AutoDriveUserUpdateDto
import com.autodrive.app.core.network.dto.RequestWithdrawalParams
import com.autodrive.app.core.observability.AppLogger
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.sync.diagnostics.SyncDiagnostics
import com.autodrive.app.core.sync.diagnostics.SyncObservabilityStore
import com.autodrive.app.core.sync.diagnostics.SyncRunContext
import com.autodrive.app.core.sync.fault.SyncFaultInjector
import com.autodrive.app.core.sync.outbox.COMMAND_RECEIPT_REVISION_KIND
import com.autodrive.app.core.sync.outbox.ChatReadOutboxPayload
import com.autodrive.app.core.sync.outbox.CreateChatConversationOutboxPayload
import com.autodrive.app.core.sync.outbox.ChatSendOutboxPayload
import com.autodrive.app.core.sync.outbox.IdempotentServerCommandGateway
import com.autodrive.app.core.sync.outbox.InvalidServerReceiptException
import com.autodrive.app.core.sync.outbox.NotificationReadOutboxPayload
import com.autodrive.app.core.sync.outbox.OUTBOX_CONTRACT_VERSION
import com.autodrive.app.core.sync.outbox.OutboxDeliveryReceipt
import com.autodrive.app.core.sync.outbox.OutboxEntityType
import com.autodrive.app.core.sync.outbox.OutboxOperationType
import com.autodrive.app.core.sync.outbox.PendingOperationFinalizer
import com.autodrive.app.core.sync.outbox.PendingOperationProcessor
import com.autodrive.app.core.sync.outbox.PendingOperationSender
import com.autodrive.app.core.sync.outbox.PendingOperationStatus
import com.autodrive.app.core.sync.outbox.PermanentOutboxException
import com.autodrive.app.core.sync.outbox.ServerCommandConflictException
import com.autodrive.app.core.sync.outbox.ServerCommandRejectedException
import com.autodrive.app.core.sync.outbox.ServerCommandResultStatus
import com.autodrive.app.core.sync.outbox.ServerCommandType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

class StaleOutboxScopeException : IllegalStateException("STALE_OUTBOX_SCOPE")

@Singleton
class OutboxSynchronizer @Inject constructor(
    private val commandGateway: IdempotentServerCommandGateway,
    private val db: AutoDriveDatabase,
    private val sessionReader: SessionReader,
    private val diagnostics: SyncDiagnostics,
    private val observabilityStore: SyncObservabilityStore,
    private val faultInjector: SyncFaultInjector,
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    suspend fun recoverExpiredLeases(scope: SyncScope): Int =
        db.pendingOperationDao().releaseExpiredClaims(
            scope.userId,
            scope.clientId,
            scope.orgId,
            System.currentTimeMillis(),
        )

    suspend fun flush(
        scope: SyncScope,
        recoverExpiredClaims: Boolean = true,
        context: SyncRunContext? = null,
    ) {
        requireCurrentScope(scope)
        val summary = PendingOperationProcessor(
            dao = db.pendingOperationDao(),
            sender = PendingOperationSender { operation -> deliver(operation, scope) },
            finalizer = PendingOperationFinalizer { operation, receipt ->
                finalizeSuccess(operation, receipt, scope)
            },
            diagnosticLog = { message, error -> AppLogger.e("Outbox", message, error) },
            faultInjector = faultInjector,
            operationDiagnostic = { operation, status, failureCategory, errorCode ->
                AppLogger.event(
                    tag = "Outbox",
                    name = "outbox_operation",
                    fields = buildMap {
                        context?.let {
                            put("sync_run_id", it.syncRunId)
                            put("scope_fingerprint", it.scopeFingerprint)
                        }
                        put("mutation_id", operation.mutationId)
                        put("operation_type", operation.operation)
                        put("status", status)
                        failureCategory?.let { put("failure_category", it) }
                        errorCode?.let { put("stable_error_code", it) }
                    },
                )
            },
        ).flush(scope = scope, recoverExpiredClaims = recoverExpiredClaims, context = context)

        val pendingCount = db.pendingOperationDao().countByStatus(
            scope.userId, scope.clientId, scope.orgId, PendingOperationStatus.PENDING,
        )
        val inProgressCount = db.pendingOperationDao().countByStatus(
            scope.userId, scope.clientId, scope.orgId, PendingOperationStatus.IN_PROGRESS,
        )
        val deadLetterCount = db.pendingOperationDao().countByStatus(
            scope.userId, scope.clientId, scope.orgId, PendingOperationStatus.DEAD_LETTER,
        )
        val retryCount = db.pendingOperationDao().sumActiveAttemptCount(scope.userId, scope.clientId, scope.orgId)
        val oldest = db.pendingOperationDao().oldestActiveCreatedAt(scope.userId, scope.clientId, scope.orgId)
        val oldestAge = oldest?.let { (System.currentTimeMillis() - it).coerceAtLeast(0L) } ?: 0L
        diagnostics.outboxState(
            context = context,
            pendingCount = pendingCount,
            inProgressCount = inProgressCount,
            deadLetterCount = deadLetterCount,
            retryCount = retryCount,
            oldestAgeMs = oldestAge,
            conflictCount = summary.conflicts,
        )
        runCatching { observabilityStore.outboxConflicts(scope, summary.conflicts) }
        AppLogger.event(
            tag = "Outbox",
            name = "outbox_flush_finished",
            fields = mapOf(
                "examined" to summary.examined,
                "succeeded" to summary.succeeded,
                "retry" to summary.scheduledForRetry,
                "dead" to summary.deadLettered,
            ),
        )
    }

    private suspend fun deliver(
        operation: PendingOperationEntity,
        scope: SyncScope,
    ): OutboxDeliveryReceipt {
        requireOperationScope(operation, scope)
        requireCurrentScope(scope)
        if (operation.contractVersion != OUTBOX_CONTRACT_VERSION) {
            throw PermanentOutboxException(
                "Unsupported payload version ${operation.contractVersion} for ${operation.operation}",
            )
        }

        val receipt = when (operation.operation) {
            OutboxOperationType.UPDATE_PROFILE -> {
                if (operation.entityType != OutboxEntityType.PROFILE || operation.entityId != scope.userId) {
                    throw PermanentOutboxException("Invalid entity identity for UPDATE_PROFILE")
                }
                val dto = decode<AutoDriveUserUpdateDto>(operation, OutboxOperationType.UPDATE_PROFILE)
                commandGateway.updateProfile(operation.mutationId, dto)
                    .validatedFor(operation, ServerCommandType.UPDATE_PROFILE, requiredEntityId = scope.userId)
            }

            OutboxOperationType.REQUEST_WITHDRAWAL_RPC -> {
                if (operation.entityType != OutboxEntityType.WITHDRAWAL) {
                    throw PermanentOutboxException("Invalid entity identity for REQUEST_WITHDRAWAL_RPC")
                }
                val params = decode<RequestWithdrawalParams>(operation, OutboxOperationType.REQUEST_WITHDRAWAL_RPC)
                if (params.clientRequestId != operation.mutationId || operation.entityId != operation.mutationId) {
                    throw PermanentOutboxException("Invalid mutation identity for withdrawal")
                }
                commandGateway.requestWithdrawal(operation.mutationId, params)
                    .validatedFor(operation, ServerCommandType.REQUEST_WITHDRAWAL, requireEntity = true)
            }


            OutboxOperationType.CREATE_CHAT_CONVERSATION -> {
                if (operation.entityType != OutboxEntityType.CONVERSATION || operation.entityId != operation.mutationId) {
                    throw PermanentOutboxException("Invalid entity identity for CREATE_CHAT_CONVERSATION")
                }
                val payload = decode<CreateChatConversationOutboxPayload>(operation, OutboxOperationType.CREATE_CHAT_CONVERSATION)
                if (payload.localConversationId != operation.entityId) {
                    throw PermanentOutboxException("Invalid local conversation identity")
                }
                commandGateway.createChatConversation(operation.mutationId, payload)
                    .validatedFor(operation, ServerCommandType.CREATE_CHAT_CONVERSATION, requireEntity = true)
            }

            OutboxOperationType.SEND_CHAT_MESSAGE -> {
                if (operation.entityType != OutboxEntityType.CHAT_MESSAGE || operation.entityId != operation.mutationId) {
                    throw PermanentOutboxException("Invalid entity identity for SEND_CHAT_MESSAGE")
                }
                val payload = decode<ChatSendOutboxPayload>(operation, OutboxOperationType.SEND_CHAT_MESSAGE)
                if (
                    payload.id != operation.entityId ||
                    payload.senderId != scope.userId ||
                    payload.clientId != scope.clientId ||
                    payload.orgId != scope.orgId
                ) {
                    throw PermanentOutboxException("Invalid scope identity for SEND_CHAT_MESSAGE")
                }
                commandGateway.sendChatMessage(operation.mutationId, payload)
                    .validatedFor(operation, ServerCommandType.SEND_CHAT_MESSAGE, requiredEntityId = payload.id)
            }

            OutboxOperationType.MARK_CHAT_READ -> {
                if (operation.entityType != OutboxEntityType.CHAT_MESSAGE) {
                    throw PermanentOutboxException("Invalid entity identity for MARK_CHAT_READ")
                }
                val payload = decode<ChatReadOutboxPayload>(operation, OutboxOperationType.MARK_CHAT_READ)
                if (payload.conversationId != operation.entityId) {
                    throw PermanentOutboxException("Invalid conversation identity for MARK_CHAT_READ")
                }
                commandGateway.markChatRead(operation.mutationId, payload.conversationId)
                    .validatedFor(operation, ServerCommandType.MARK_CHAT_READ, requiredEntityId = payload.conversationId)
            }

            OutboxOperationType.MARK_NOTIFICATION_READ -> {
                if (operation.entityType != OutboxEntityType.NOTIFICATION) {
                    throw PermanentOutboxException("Invalid entity identity for MARK_NOTIFICATION_READ")
                }
                val payload = decode<NotificationReadOutboxPayload>(operation, OutboxOperationType.MARK_NOTIFICATION_READ)
                if (payload.notificationId != operation.entityId) {
                    throw PermanentOutboxException("Invalid notification identity for MARK_NOTIFICATION_READ")
                }
                commandGateway.markNotificationRead(operation.mutationId, payload.notificationId)
                    .validatedFor(operation, ServerCommandType.MARK_NOTIFICATION_READ, requiredEntityId = payload.notificationId)
            }

            else -> throw PermanentOutboxException("Unknown pending operation: ${operation.operation}")
        }

        // A response for A must never acknowledge local state after the session switched to B.
        requireCurrentScope(scope)
        return receipt
    }

    private fun OutboxDeliveryReceipt.validatedFor(
        operation: PendingOperationEntity,
        expectedCommand: String,
        requiredEntityId: String? = null,
        requireEntity: Boolean = false,
    ): OutboxDeliveryReceipt {
        if (mutationId != operation.mutationId) throw InvalidServerReceiptException("INVALID_SERVER_RECEIPT_MUTATION")
        if (commandType != expectedCommand) throw InvalidServerReceiptException("INVALID_SERVER_RECEIPT_COMMAND")
        if (revisionKind != COMMAND_RECEIPT_REVISION_KIND) {
            throw InvalidServerReceiptException("INVALID_SERVER_RECEIPT_REVISION_KIND")
        }

        when (resultStatus) {
            ServerCommandResultStatus.APPLIED -> {
                if (serverRevision <= 0L) throw InvalidServerReceiptException("INVALID_SERVER_RECEIPT_REVISION")
                if (requireEntity && serverEntityId.isNullOrBlank()) {
                    throw InvalidServerReceiptException("INVALID_SERVER_RECEIPT_ENTITY")
                }
                if (requiredEntityId != null && serverEntityId != requiredEntityId) {
                    throw InvalidServerReceiptException("INVALID_SERVER_RECEIPT_ENTITY")
                }
                return this
            }

            ServerCommandResultStatus.REJECTED -> throw ServerCommandRejectedException(
                errorCode ?: "SERVER_COMMAND_REJECTED",
            )

            ServerCommandResultStatus.CONFLICT -> throw ServerCommandConflictException(
                errorCode ?: "SERVER_COMMAND_CONFLICT",
            )

            else -> throw InvalidServerReceiptException("UNSUPPORTED_SERVER_RECEIPT")
        }
    }

    private suspend fun finalizeSuccess(
        operation: PendingOperationEntity,
        receipt: OutboxDeliveryReceipt,
        scope: SyncScope,
    ) {
        requireOperationScope(operation, scope)
        requireCurrentScope(scope)
        db.withTransaction {
            requireCurrentScope(scope)
            when (operation.operation) {
                OutboxOperationType.UPDATE_PROFILE -> {
                    val newer = db.pendingOperationDao().countOtherActiveForEntity(
                        scope.userId,
                        scope.clientId,
                        scope.orgId,
                        operation.entityType,
                        operation.entityId,
                        operation.operation,
                        operation.id,
                    )
                    if (newer == 0) {
                        db.autoDriveUserDao().updateSyncStatus(scope.userId, "SYNCED")
                    }
                }

                OutboxOperationType.REQUEST_WITHDRAWAL_RPC -> {
                    val serverId = receipt.serverEntityId
                        ?: throw IllegalStateException("MISSING_WITHDRAWAL_SERVER_ID")
                    val local = db.withdrawalRequestDao().getById(operation.entityId)
                    if (local != null && local.userId == scope.userId && local.clientId == scope.clientId) {
                        db.withdrawalRequestDao().confirmSynced(operation.entityId, serverId)
                    }
                }

    
                OutboxOperationType.CREATE_CHAT_CONVERSATION -> {
                    val serverId = receipt.serverEntityId
                        ?: throw IllegalStateException("MISSING_CONVERSATION_SERVER_ID")
                    val local = db.conversationDao().getById(operation.entityId)
                        ?: throw IllegalStateException("TARGET_NOT_FOUND")
                    if (local.marketerId != scope.userId || local.clientId != scope.clientId) {
                        throw StaleOutboxScopeException()
                    }
                    if (serverId != local.id) {
                        val alreadyRemote = db.conversationDao().getById(serverId)
                        if (alreadyRemote != null) {
                            if (alreadyRemote.marketerId != scope.userId || alreadyRemote.clientId != scope.clientId) {
                                throw StaleOutboxScopeException()
                            }
                            db.chatMessageDao().remapConversationId(local.id, serverId)
                            check(db.conversationDao().deleteExact(local.id, scope.userId, scope.clientId) == 1) {
                                "LOCAL_CONVERSATION_REMAP_DELETE_FAILED"
                            }
                        } else {
                            check(db.conversationDao().remapId(local.id, serverId, scope.userId, scope.clientId) == 1) {
                                "LOCAL_CONVERSATION_REMAP_FAILED"
                            }
                            db.chatMessageDao().remapConversationId(local.id, serverId)
                        }
                    }
                    val children = db.pendingOperationDao().getChildrenByDependency(
                        scope.userId, scope.clientId, scope.orgId, operation.mutationId,
                    )
                    children.forEach { child ->
                        if (child.operation != OutboxOperationType.SEND_CHAT_MESSAGE) {
                            throw PermanentOutboxException("Unsupported conversation child operation")
                        }
                        val payload = decode<ChatSendOutboxPayload>(child, OutboxOperationType.SEND_CHAT_MESSAGE)
                        check(payload.conversationId == operation.entityId || payload.conversationId == serverId) {
                            "CONVERSATION_CHILD_PAYLOAD_CONFLICT"
                        }
                        val changed = db.pendingOperationDao().updatePayloadBeforeFirstAttempt(
                            child.id, scope.userId, scope.clientId, scope.orgId,
                            json.encodeToString(ChatSendOutboxPayload.serializer(), payload.copy(conversationId = serverId)),
                        )
                        check(changed == 1) { "CONVERSATION_CHILD_ALREADY_ATTEMPTED" }
                    }
                }

            OutboxOperationType.SEND_CHAT_MESSAGE -> {
                    val message = db.chatMessageDao().getById(operation.entityId)
                        ?: throw IllegalStateException("TARGET_NOT_FOUND")
                    val conversation = db.conversationDao().getById(message.conversationId)
                        ?: throw IllegalStateException("TARGET_NOT_FOUND")
                    if (message.senderId != scope.userId || conversation.clientId != scope.clientId) {
                        throw StaleOutboxScopeException()
                    }
                    db.chatMessageDao().updateStatus(message.id, "SENT")
                    receipt.serverCreatedAt?.let { serverMs ->
                        db.chatMessageDao().updateCreatedAt(message.id, serverMs)
                        val preview = when (message.type) {
                            "IMAGE" -> "📷 صورة"
                            "VOICE" -> "🎤 رسالة صوتية"
                            else -> message.content
                        }
                        db.conversationDao().updateLastMessage(message.conversationId, preview, serverMs, 0)
                    }
                }

                OutboxOperationType.MARK_CHAT_READ -> Unit

                OutboxOperationType.MARK_NOTIFICATION_READ -> {
                    db.notificationDao().confirmReadSynced(operation.entityId, scope.userId)
                }

                else -> throw PermanentOutboxException("Unknown pending operation: ${operation.operation}")
            }

            val deleted = db.pendingOperationDao().deleteClaimedById(
                operation.id,
                scope.userId,
                scope.clientId,
                scope.orgId,
            )
            check(deleted == 1) { "OUTBOX_FINALIZE_SCOPE_MISMATCH" }
        }
    }

    private inline fun <reified T> decode(operation: PendingOperationEntity, label: String): T =
        runCatching { json.decodeFromString<T>(operation.payload) }
            .getOrElse { error -> throw PermanentOutboxException("Invalid payload for $label", error) }

    private fun requireOperationScope(operation: PendingOperationEntity, scope: SyncScope) {
        if (
            operation.userId != scope.userId ||
            operation.clientId != scope.clientId ||
            operation.orgId != scope.orgId
        ) throw StaleOutboxScopeException()
    }

    private fun requireCurrentScope(scope: SyncScope) {
        if (SyncScope.from(sessionReader.currentSession()) != scope) throw StaleOutboxScopeException()
    }
}
