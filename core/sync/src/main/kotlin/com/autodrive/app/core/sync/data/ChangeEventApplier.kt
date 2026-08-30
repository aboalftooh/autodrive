package com.autodrive.app.core.sync.data

import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.database.entities.ChatMessageEntity
import com.autodrive.app.core.database.entities.ConversationEntity
import com.autodrive.app.core.network.dto.AutoDriveUserDto
import com.autodrive.app.core.network.dto.BalanceTransactionDto
import com.autodrive.app.core.network.dto.CommissionPaymentDto
import com.autodrive.app.core.network.dto.InvoiceDto
import com.autodrive.app.core.network.dto.MarketerBalanceDto
import com.autodrive.app.core.network.dto.NotificationDto
import com.autodrive.app.core.network.dto.PaymentDto
import com.autodrive.app.core.network.dto.WithdrawalRequestDto
import com.autodrive.app.core.network.dto.chat.ChatMessageDto
import com.autodrive.app.core.network.dto.chat.ConversationDto
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class UnsupportedChangeEntityException(entityType: String) :
    IllegalStateException("UNSUPPORTED_CHANGE_ENTITY:$entityType")

class RemoteChangePayloadInvalidException(entityType: String) :
    IllegalStateException("REMOTE_CHANGE_PAYLOAD_INVALID:$entityType")

/**
 * Explicit entity registry for canonical v72 data changes and bootstrap rows.
 *
 * There is intentionally no reflection/generic Room writer. Every replicated type has reviewed
 * scope, identity, pending-local and delete semantics.
 */
@Singleton
class ChangeEventApplier @Inject constructor(
    private val db: AutoDriveDatabase,
    private val guard: PendingLocalMutationGuard,
) {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    val supportedEntityTypes: Set<String> = setOf(
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

    suspend fun apply(scope: SyncScope, event: ChangeEventDto) {
        if (event.entityType !in supportedEntityTypes) throw UnsupportedChangeEntityException(event.entityType)
        when (event.operation) {
            ChangeOperation.UPSERT -> applyUpsert(scope, event.entityType, event.entityId, requireNotNull(event.payload) {
                "REMOTE_CHANGE_PAYLOAD_INVALID:${event.entityType}"
            })
            ChangeOperation.DELETE -> applyDelete(scope, event.entityType, event.entityId)
            else -> error("UNSUPPORTED_CHANGE_OPERATION:${event.operation}")
        }
    }

    /** Bootstrap rows are canonical snapshot rows, not fabricated Inbox events. */
    suspend fun applyBootstrapRow(
        scope: SyncScope,
        entityType: String,
        entityId: String,
        payload: JsonObject,
    ) {
        if (entityType !in supportedEntityTypes) throw UnsupportedChangeEntityException(entityType)
        applyUpsert(scope, entityType, entityId, payload)
    }

    private suspend fun applyUpsert(
        scope: SyncScope,
        entityType: String,
        entityId: String,
        payload: JsonObject,
    ) {
        try {
            when (entityType) {
                "autodrive_users" -> {
                    val dto = decode<AutoDriveUserDto>(payload)
                    requireIdentity(entityId, dto.id)
                    require(dto.userId == scope.userId && dto.clientId == scope.clientId && dto.orgId == scope.orgId)
                    db.autoDriveUserDao().upsert(guard.profile(scope, dto))
                }
                "invoices" -> {
                    val dto = decode<InvoiceDto>(payload)
                    requireIdentity(entityId, dto.id)
                    guard.requireClient(scope, dto.clientId)
                    db.invoiceDao().upsert(dto.toEntity())
                }
                "payments" -> {
                    val dto = decode<PaymentDto>(payload)
                    requireIdentity(entityId, dto.id)
                    guard.requireClient(scope, dto.clientId)
                    db.paymentDao().upsert(dto.toEntity())
                }
                "commission_payments" -> {
                    val dto = decode<CommissionPaymentDto>(payload)
                    requireIdentity(entityId, dto.id)
                    guard.requireClient(scope, dto.clientId)
                    db.commissionPaymentDao().upsert(dto.toEntity())
                }
                "marketer_balance" -> {
                    val dto = decode<MarketerBalanceDto>(payload)
                    requireIdentity(entityId, dto.id)
                    guard.requireClientOrg(scope, dto.clientId, dto.orgId)
                    val current = db.marketerBalanceDao().get(scope.userId)
                    if (current == null || current.id == dto.id) {
                        db.marketerBalanceDao().upsert(dto.toEntity(scope.userId))
                    } else {
                        // One balance account per exact marketer scope.
                        db.marketerBalanceDao().deleteByUserId(scope.userId)
                        db.marketerBalanceDao().upsert(dto.toEntity(scope.userId))
                    }
                }
                "balance_transactions" -> {
                    val dto = decode<BalanceTransactionDto>(payload)
                    requireIdentity(entityId, dto.id)
                    guard.requireClientOrg(scope, dto.clientId, dto.orgId)
                    db.balanceTransactionDao().upsert(guard.balanceTransaction(scope, dto))
                }
                "withdrawal_requests" -> {
                    val dto = decode<WithdrawalRequestDto>(payload)
                    requireIdentity(entityId, dto.id)
                    guard.requireClientOrg(scope, dto.clientId, dto.orgId)
                    guard.withdrawal(scope, dto)?.let { db.withdrawalRequestDao().upsert(it) }
                }
                "notifications" -> {
                    val dto = decode<NotificationDto>(payload)
                    requireIdentity(entityId, dto.id)
                    require(dto.userId == scope.userId && dto.clientId == scope.clientId)
                    db.notificationDao().upsert(guard.notification(scope, dto))
                }
                "conversations" -> applyConversation(scope, entityId, decode(payload))
                "internal_messages" -> applyMessage(scope, entityId, decode(payload))
                else -> throw UnsupportedChangeEntityException(entityType)
            }
        } catch (scopeMismatch: RemoteScopeMismatchException) {
            throw scopeMismatch
        } catch (illegal: IllegalStateException) {
            throw illegal
        } catch (bad: Throwable) {
            throw RemoteChangePayloadInvalidException(entityType)
        }
    }

    private suspend fun applyConversation(scope: SyncScope, entityId: String, dto: ConversationDto) {
        requireIdentity(entityId, dto.id)
        require(dto.clientId == scope.clientId && dto.orgId == scope.orgId) { "REMOTE_SCOPE_MISMATCH" }
        val createdAt = parseTime(dto.createdAt)
        val lastAt = dto.lastMessageAt?.let(::parseTime) ?: 0L
        val existing = db.conversationDao().getById(dto.id)
        val pending = db.pendingOperationDao().findAnyActiveForEntity(
            scope.userId, scope.clientId, scope.orgId, "conversations", dto.id,
        )
        if (existing != null && pending != null) return
        if (existing != null) {
            require(existing.clientId == scope.clientId && existing.marketerId == scope.userId) {
                "REMOTE_SCOPE_MISMATCH"
            }
        }
        db.conversationDao().upsert(
            ConversationEntity(
                id = dto.id,
                marketerId = scope.userId,
                clientId = scope.clientId,
                title = dto.subject.ifBlank { "الإدارة" },
                subject = dto.subject,
                lastMessage = dto.lastMessage.orEmpty(),
                lastMessageAt = lastAt,
                unreadCount = dto.marketerUnread,
                createdAt = createdAt,
            ),
        )
    }

    private suspend fun applyMessage(scope: SyncScope, entityId: String, dto: ChatMessageDto) {
        requireIdentity(entityId, dto.id)
        require(dto.clientId == scope.clientId && dto.orgId == scope.orgId) { "REMOTE_SCOPE_MISMATCH" }
        val conversationId = requireNotNull(dto.conversationId) { "CHAT_CONVERSATION_ID_MISSING" }
        val conversation = db.conversationDao().getById(conversationId)
            ?: error("CHAT_CONVERSATION_NOT_FOUND")
        require(conversation.clientId == scope.clientId && conversation.marketerId == scope.userId) {
            "REMOTE_SCOPE_MISMATCH"
        }
        val createdAt = parseTime(dto.createdAt)
        val existing = db.chatMessageDao().getById(dto.id)
        val active = db.pendingOperationDao().findAnyActiveForEntity(
            scope.userId, scope.clientId, scope.orgId, "internal_messages", dto.id,
        )
        if (existing != null && active != null) {
            require(existing.conversationId == conversationId && existing.content == dto.body) {
                "CHAT_PENDING_INTENT_CONFLICT"
            }
            return
        }
        val status = if (dto.isRead) "READ" else "SENT"
        if (existing == null) {
            db.chatMessageDao().insertOrIgnore(
                ChatMessageEntity(
                    id = dto.id,
                    conversationId = conversationId,
                    senderId = dto.senderId,
                    senderType = dto.senderType,
                    content = dto.body,
                    type = dto.type.ifBlank { "TEXT" },
                    isRead = dto.isRead,
                    createdAt = createdAt,
                    status = status,
                    mediaUrl = dto.mediaUrl,
                    mediaMime = dto.mediaMime,
                    mediaDurationMs = dto.mediaDurationMs,
                    mediaObjectPath = dto.mediaObjectPath,
                ),
            )
        } else {
            require(existing.conversationId == conversationId) { "CHAT_MESSAGE_IDENTITY_CONFLICT" }
            require(existing.senderId == dto.senderId && existing.senderType == dto.senderType) {
                "CHAT_MESSAGE_SENDER_CONFLICT"
            }
            db.chatMessageDao().updateRemoteState(
                dto.id,
                existing.isRead || dto.isRead,
                if (existing.status == "READ") "READ" else status,
                dto.mediaUrl ?: existing.mediaUrl,
                dto.mediaMime ?: existing.mediaMime,
                dto.mediaDurationMs ?: existing.mediaDurationMs,
                dto.mediaObjectPath ?: existing.mediaObjectPath,
            )
        }
    }

    suspend fun applyDelete(scope: SyncScope, entityType: String, id: String) {
        when (entityType) {
            "autodrive_users" -> {
                val local = db.autoDriveUserDao().get(scope.userId)
                val pending = db.pendingOperationDao().findAnyActiveForEntity(
                    scope.userId, scope.clientId, scope.orgId, entityType, scope.userId,
                )
                if (local?.id == id && (local.syncStatus != "SYNCED" || pending != null)) {
                    throw PendingLocalConflictException("PENDING_LOCAL_CONFLICT")
                }
                db.autoDriveUserDao().deleteByIdForUser(id, scope.userId)
            }
            "invoices" -> db.invoiceDao().deleteByIdForClient(id, scope.clientId)
            "payments" -> db.paymentDao().deleteByIdForClient(id, scope.clientId)
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
                val pending = db.pendingOperationDao().findAnyActiveForEntity(
                    scope.userId, scope.clientId, scope.orgId, entityType, id,
                )
                if ((local?.syncStatus != null && local.syncStatus != "SYNCED") || pending != null) {
                    throw PendingLocalConflictException("PENDING_LOCAL_CONFLICT")
                }
                db.withdrawalRequestDao().deleteByIdForUser(id, scope.userId)
            }
            "notifications" -> {
                val local = db.notificationDao().getById(id, scope.userId)
                val pending = db.pendingOperationDao().findAnyActiveForEntity(
                    scope.userId, scope.clientId, scope.orgId, entityType, id,
                )
                if (local?.readSynced == false || pending != null) {
                    throw PendingLocalConflictException("PENDING_LOCAL_CONFLICT")
                }
                db.notificationDao().deleteById(id, scope.userId)
            }
            "conversations" -> {
                val pending = db.pendingOperationDao().findAnyActiveForEntity(
                    scope.userId, scope.clientId, scope.orgId, entityType, id,
                )
                val pendingMessages = db.chatMessageDao().getByStatus("PENDING").any { it.conversationId == id } ||
                    db.chatMessageDao().getByStatus("SENDING").any { it.conversationId == id }
                if (pending != null || pendingMessages) throw PendingLocalConflictException("PENDING_LOCAL_CONFLICT")
                db.chatMessageDao().deleteByConversationIds(listOf(id))
                db.conversationDao().deleteByIdForClient(id, scope.clientId)
            }
            "internal_messages" -> {
                val local = db.chatMessageDao().getById(id)
                val pending = db.pendingOperationDao().findAnyActiveForEntity(
                    scope.userId, scope.clientId, scope.orgId, entityType, id,
                )
                if (local?.status == "PENDING" || local?.status == "SENDING" || pending != null) {
                    throw PendingLocalConflictException("PENDING_LOCAL_CONFLICT")
                }
                db.chatMessageDao().deleteById(id)
            }
            else -> throw UnsupportedChangeEntityException(entityType)
        }
    }

    /**
     * Removes server-owned local rows absent from a complete consistent bootstrap snapshot.
     * Pending local intent is preserved and therefore can still be delivered after install.
     */
    suspend fun removeBootstrapStaleRows(scope: SyncScope, serverIds: Map<String, Set<String>>) {
        val profile = db.autoDriveUserDao().get(scope.userId)
        if (profile != null && profile.id !in serverIds.orEmpty("autodrive_users")) {
            val pending = db.pendingOperationDao().findAnyActiveForEntity(
                scope.userId, scope.clientId, scope.orgId, "autodrive_users", scope.userId,
            )
            if (profile.syncStatus == "SYNCED" && pending == null) db.autoDriveUserDao().deleteByUserId(scope.userId)
        }

        db.invoiceDao().getAllByClientIdForSync(scope.clientId)
            .filter { it.id !in serverIds.orEmpty("invoices") }
            .forEach { db.invoiceDao().deleteByIdForClient(it.id, scope.clientId) }

        db.paymentDao().getAllByClientIdForSync(scope.clientId)
            .filter { it.id !in serverIds.orEmpty("payments") }
            .forEach { db.paymentDao().deleteByIdForClient(it.id, scope.clientId) }

        db.commissionPaymentDao().getByClientId(scope.clientId)
            .filter { it.id !in serverIds.orEmpty("commission_payments") }
            .forEach { db.commissionPaymentDao().deleteByIdForClient(it.id, scope.clientId) }

        db.marketerBalanceDao().get(scope.userId)?.let {
            if (it.id !in serverIds.orEmpty("marketer_balance")) db.marketerBalanceDao().deleteByUserId(scope.userId)
        }

        db.balanceTransactionDao().getAllByUserIdForSync(scope.userId)
            .filter { it.id !in serverIds.orEmpty("balance_transactions") && it.syncStatus == "SYNCED" }
            .forEach { db.balanceTransactionDao().deleteByIdForUser(it.id, scope.userId) }

        db.withdrawalRequestDao().getAllByUserIdForSync(scope.userId)
            .filter { it.id !in serverIds.orEmpty("withdrawal_requests") }
            .forEach {
                val pending = db.pendingOperationDao().findAnyActiveForEntity(
                    scope.userId, scope.clientId, scope.orgId, "withdrawal_requests", it.id,
                )
                if (it.syncStatus == "SYNCED" && pending == null) {
                    db.withdrawalRequestDao().deleteByIdForUser(it.id, scope.userId)
                }
            }

        db.notificationDao().getAllByUserIdForSync(scope.userId)
            .filter { it.id !in serverIds.orEmpty("notifications") }
            .forEach {
                val pending = db.pendingOperationDao().findAnyActiveForEntity(
                    scope.userId, scope.clientId, scope.orgId, "notifications", it.id,
                )
                if (it.readSynced && pending == null) db.notificationDao().deleteById(it.id, scope.userId)
            }

        val conversations = db.conversationDao().getAllByMarketer(scope.userId)
        val staleConversations = conversations.filter { it.id !in serverIds.orEmpty("conversations") }
        for (conversation in staleConversations) {
            val pending = db.pendingOperationDao().findAnyActiveForEntity(
                scope.userId, scope.clientId, scope.orgId, "conversations", conversation.id,
            )
            val pendingMessages = db.chatMessageDao().getByStatus("PENDING")
                .any { it.conversationId == conversation.id }
            if (pending == null && !pendingMessages) {
                db.chatMessageDao().deleteByConversationIds(listOf(conversation.id))
                db.conversationDao().deleteExact(
                    conversation.id, scope.userId, scope.clientId,
                )
            }
        }

        val remainingConversations = db.conversationDao().getAllByMarketer(scope.userId)
        val remainingIds = remainingConversations.map { it.id }
        if (remainingIds.isNotEmpty()) {
            db.chatMessageDao().getAllByConversationIdsForSync(remainingIds)
                .filter { it.id !in serverIds.orEmpty("internal_messages") }
                .forEach {
                    val pending = db.pendingOperationDao().findAnyActiveForEntity(
                        scope.userId, scope.clientId, scope.orgId, "internal_messages", it.id,
                    )
                    if (it.status != "PENDING" && it.status != "SENDING" && pending == null) {
                        db.chatMessageDao().deleteById(it.id)
                    }
                }
        }
    }

    private inline fun <reified T> decode(payload: JsonObject): T =
        json.decodeFromJsonElement(payload)

    private fun requireIdentity(expected: String, actual: String) {
        require(actual.isNotBlank() && expected == actual) { "REMOTE_CHANGE_IDENTITY_MISMATCH" }
    }

    private fun parseTime(value: String): Long =
        OffsetDateTime.parse(value).toInstant().toEpochMilli()

    private fun Map<String, Set<String>>.orEmpty(key: String): Set<String> = get(key).orEmpty()
}
