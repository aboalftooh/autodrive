package com.autodrive.app.core.sync.data

import androidx.room.withTransaction
import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.database.entities.ChatMessageEntity
import com.autodrive.app.core.database.entities.ChatRecoveryCheckpointEntity
import com.autodrive.app.core.database.entities.ConversationEntity
import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.core.network.dto.chat.ChatMessageDto
import com.autodrive.app.core.network.dto.chat.ConversationDto
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.sync.outbox.OutboxEntityType
import com.autodrive.app.core.sync.outbox.OutboxOperationType
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Session 71 compatibility chat recovery. It is deliberately conversation-scoped and does not
 * pretend to be the Session 72 global change-feed cursor.
 */
@Singleton
class ChatRecoverySynchronizer @Inject constructor(
    private val supabase: AutoDriveSupabase,
    private val db: AutoDriveDatabase,
    private val sessionReader: SessionReader,
) {
    suspend fun recover(scope: SyncScope) {
        requireCurrentScope(scope)
        val conversations = supabase.client.postgrest["conversations"]
            .select(Columns.ALL) { filter { eq("client_id", scope.clientId) } }
            .decodeList<ConversationDto>()
            .distinctBy { it.id }

        conversations.forEach { validateConversation(scope, it) }
        db.withTransaction {
            requireCurrentScope(scope)
            conversations.forEach { mergeConversationSnapshot(scope, it) }
        }

        conversations.forEach { conversation ->
            recoverConversation(scope, conversation.id)
        }
    }

    private suspend fun recoverConversation(scope: SyncScope, conversationId: String) {
        var checkpoint = db.chatRecoveryCheckpointDao().get(
            scope.userId, scope.clientId, scope.orgId, conversationId,
        )
        var previousSequence = checkpoint?.lastServerSequence ?: 0L

        while (true) {
            requireCurrentScope(scope)
            val page = supabase.client.postgrest.rpc(
                "autodrive_chat_recovery_page_v1",
                ChatRecoveryPageParams(
                    conversationId = conversationId,
                    afterServerSequence = previousSequence,
                    limit = PAGE_SIZE,
                ),
            ).decodeList<ChatMessageDto>()

            if (page.isEmpty()) break
            val validated = validatePage(scope, conversationId, page, previousSequence)
            val last = validated.last()

            db.withTransaction {
                requireCurrentScope(scope)
                val current = db.chatRecoveryCheckpointDao().get(
                    scope.userId, scope.clientId, scope.orgId, conversationId,
                )
                val expected = checkpoint?.lastServerSequence ?: 0L
                val actual = current?.lastServerSequence ?: 0L
                check(actual == expected) { "CHAT_CHECKPOINT_CONCURRENT_ADVANCE" }

                validated.forEach { row -> mergeMessage(scope, row.dto, row.createdAtMillis) }
                db.chatRecoveryCheckpointDao().upsert(
                    ChatRecoveryCheckpointEntity(
                        userId = scope.userId,
                        clientId = scope.clientId,
                        orgId = scope.orgId,
                        conversationId = conversationId,
                        lastCreatedAtServer = last.dto.createdAt,
                        lastMessageId = last.dto.id,
                        lastServerSequence = last.serverSequence,
                    ),
                )
                reconcileConversationSummary(scope, conversationId)
            }

            checkpoint = ChatRecoveryCheckpointEntity(
                userId = scope.userId,
                clientId = scope.clientId,
                orgId = scope.orgId,
                conversationId = conversationId,
                lastCreatedAtServer = last.dto.createdAt,
                lastMessageId = last.dto.id,
                lastServerSequence = last.serverSequence,
            )
            previousSequence = last.serverSequence
        }
    }

    private fun validatePage(
        scope: SyncScope,
        conversationId: String,
        page: List<ChatMessageDto>,
        previousSequence: Long,
    ): List<ValidatedMessage> {
        var cursor = previousSequence
        return page.map { dto ->
            require(dto.id.isNotBlank()) { "CHAT_MESSAGE_ID_MISSING" }
            require(dto.conversationId == conversationId) { "CHAT_CONVERSATION_SCOPE_MISMATCH" }
            require(dto.clientId == scope.clientId && dto.orgId == scope.orgId) { "CHAT_TENANT_SCOPE_MISMATCH" }
            require(dto.senderId.isNotBlank()) { "CHAT_SENDER_ID_MISSING" }
            require(dto.senderType == "ADMIN" || dto.senderType == "MARKETER") { "CHAT_SENDER_TYPE_INVALID" }
            val created = parseServerTimeStrict(dto.createdAt)
            val next = requireNotNull(dto.chatRecoverySequence) { "CHAT_SERVER_SEQUENCE_MISSING" }
            check(next > cursor) { "CHAT_CURSOR_NOT_STRICTLY_INCREASING" }
            cursor = next
            ValidatedMessage(dto, created, next)
        }
    }

    private suspend fun mergeMessage(scope: SyncScope, dto: ChatMessageDto, createdAtMillis: Long) {
        val conversationId = requireNotNull(dto.conversationId)
        val existingConversation = db.conversationDao().getById(conversationId)
            ?: error("CHAT_CONVERSATION_NOT_FOUND")
        check(existingConversation.clientId == scope.clientId && existingConversation.marketerId == scope.userId) {
            "CHAT_LOCAL_CONVERSATION_SCOPE_MISMATCH"
        }

        val existing = db.chatMessageDao().getById(dto.id)
        val remoteStatus = if (dto.senderType == "MARKETER" && dto.isRead) "READ" else "SENT"
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
                    createdAt = createdAtMillis,
                    status = remoteStatus,
                    mediaUrl = dto.mediaUrl,
                    mediaMime = dto.mediaMime,
                    mediaDurationMs = dto.mediaDurationMs,
                    mediaObjectPath = dto.mediaObjectPath,
                ),
            )
            return
        }

        check(existing.conversationId == conversationId) { "CHAT_MESSAGE_IDENTITY_CONFLICT" }
        check(existing.senderId == dto.senderId && existing.senderType == dto.senderType) { "CHAT_MESSAGE_SENDER_CONFLICT" }
        check(existing.createdAt == createdAtMillis || existing.status == "PENDING") { "CHAT_MESSAGE_CREATED_AT_CONFLICT" }

        val activeSend = if (dto.senderType == "MARKETER") {
            db.pendingOperationDao().findActiveByMutationId(scope.userId, scope.clientId, scope.orgId, dto.id)
        } else null
        if (activeSend != null) {
            check(existing.content == dto.body && existing.type == dto.type) { "CHAT_PENDING_INTENT_CONFLICT" }
            if (dto.isRead && existing.status != "READ") {
                db.chatMessageDao().updateRemoteState(
                    dto.id, true, "READ", existing.mediaUrl, existing.mediaMime, existing.mediaDurationMs, existing.mediaObjectPath,
                )
            }
            return
        }

        val monotonicStatus = if (existing.status == "READ") "READ" else remoteStatus
        db.chatMessageDao().updateRemoteState(
            dto.id,
            existing.isRead || dto.isRead,
            monotonicStatus,
            dto.mediaUrl ?: existing.mediaUrl,
            dto.mediaMime ?: existing.mediaMime,
            dto.mediaDurationMs ?: existing.mediaDurationMs,
            dto.mediaObjectPath ?: existing.mediaObjectPath,
        )
    }

    private suspend fun mergeConversationSnapshot(scope: SyncScope, dto: ConversationDto) {
        val existing = db.conversationDao().getById(dto.id)
        val createdAt = parseServerTimeStrict(dto.createdAt)
        val remoteLastAt = dto.lastMessageAt?.let(::parseServerTimeStrict) ?: 0L
        val remoteLastMessage = dto.lastMessage
        if (existing == null) {
            db.conversationDao().upsert(
                ConversationEntity(
                    id = dto.id,
                    marketerId = scope.userId,
                    clientId = scope.clientId,
                    title = dto.subject.ifBlank { "الإدارة" },
                    subject = dto.subject,
                    lastMessage = dto.lastMessage.orEmpty(),
                    lastMessageAt = remoteLastAt,
                    unreadCount = dto.marketerUnread,
                    createdAt = createdAt,
                ),
            )
        } else {
            check(existing.clientId == scope.clientId && existing.marketerId == scope.userId) { "CHAT_CONVERSATION_IDENTITY_CONFLICT" }
            if (remoteLastAt > existing.lastMessageAt && remoteLastMessage != null) {
                db.conversationDao().updateLastMessage(dto.id, remoteLastMessage, remoteLastAt, 0)
            }
        }
    }

    private suspend fun reconcileConversationSummary(scope: SyncScope, conversationId: String) {
        val latest = db.chatMessageDao().getLatestByConversation(conversationId)
        if (latest != null) {
            val preview = when (latest.type) {
                "IMAGE" -> "📷 صورة"
                "VOICE" -> "🎤 رسالة صوتية"
                else -> latest.content
            }
            db.conversationDao().updateLastMessage(conversationId, preview, latest.createdAt, 0)
        }
        val pendingRead = db.pendingOperationDao().findActiveForEntity(
            scope.userId, scope.clientId, scope.orgId,
            OutboxEntityType.CHAT_MESSAGE, conversationId, OutboxOperationType.MARK_CHAT_READ,
        )
        val unread = if (pendingRead != null) 0 else db.chatMessageDao().countUnreadAdmin(conversationId)
        db.conversationDao().setUnreadCount(conversationId, unread)
    }

    private fun validateConversation(scope: SyncScope, dto: ConversationDto) {
        require(dto.id.isNotBlank()) { "CHAT_CONVERSATION_ID_MISSING" }
        require(dto.clientId == scope.clientId && dto.orgId == scope.orgId) { "CHAT_CONVERSATION_SCOPE_MISMATCH" }
        parseServerTimeStrict(dto.createdAt)
        dto.lastMessageAt?.let(::parseServerTimeStrict)
    }

    private fun parseServerTimeStrict(value: String): Long =
        OffsetDateTime.parse(value).toInstant().toEpochMilli()

    private fun requireCurrentScope(scope: SyncScope) {
        if (SyncScope.from(sessionReader.currentSession()) != scope) throw StaleSyncScopeException()
    }

    private data class ValidatedMessage(
        val dto: ChatMessageDto,
        val createdAtMillis: Long,
        val serverSequence: Long,
    )

    @Serializable
    private data class ChatRecoveryPageParams(
        @SerialName("p_conversation_id") val conversationId: String,
        @SerialName("p_after_seq") val afterServerSequence: Long = 0L,
        @SerialName("p_limit") val limit: Int,
    )

    private companion object {
        const val PAGE_SIZE = 200
    }
}
