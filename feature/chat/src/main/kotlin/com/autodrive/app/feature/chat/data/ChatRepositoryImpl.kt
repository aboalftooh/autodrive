package com.autodrive.app.feature.chat.data
import androidx.room.withTransaction
import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.database.entities.ChatMessageEntity
import com.autodrive.app.core.database.entities.ChatMediaTransferEntity
import com.autodrive.app.core.database.entities.PendingOperationEntity
import com.autodrive.app.core.database.entities.ConversationEntity
import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.core.network.dto.chat.ConversationDto
import com.autodrive.app.feature.chat.data.remote.dto.GetOrCreateConversationParams
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.feature.chat.domain.model.*
import com.autodrive.app.feature.chat.domain.repository.ChatRepository
import com.autodrive.app.feature.chat.data.worker.ChatMediaTransferScheduler
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.sync.data.SyncScope
import com.autodrive.app.core.sync.data.ChatRecoverySynchronizer
import com.autodrive.app.core.sync.outbox.ChatReadOutboxPayload
import com.autodrive.app.core.sync.outbox.ChatSendOutboxPayload
import com.autodrive.app.core.sync.outbox.CreateChatConversationOutboxPayload
import com.autodrive.app.core.sync.outbox.OUTBOX_CONTRACT_VERSION
import com.autodrive.app.core.sync.outbox.OutboxEntityType
import com.autodrive.app.core.sync.outbox.OutboxOperationType
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val supabase: AutoDriveSupabase,
    private val db: AutoDriveDatabase,
    private val sessionReader: SessionReader,
    private val mediaManager: ChatMediaManager,
    private val chatRecovery: ChatRecoverySynchronizer,
    private val mediaTransferScheduler: ChatMediaTransferScheduler,
) : ChatRepository {
    private val outboxJson = Json { encodeDefaults = true; explicitNulls = true }
    override fun observeConversations(marketerId: String): Flow<List<Conversation>> {
        if (marketerId.isBlank()) return flowOf(emptyList())
        return db.conversationDao().observeAll(marketerId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }
    override fun observeUnreadConversationsCount(marketerId: String): Flow<Int> {
        if (marketerId.isBlank()) return flowOf(0)
        return db.conversationDao().observeTotalUnread(marketerId).flowOn(Dispatchers.IO)
    }
    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        db.chatMessageDao().observeByConversation(conversationId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    override suspend fun searchMessageContent(query: String): List<String> =
        withContext(Dispatchers.IO) {
            db.chatMessageDao().findConversationIdsByContent(query)
        }
    override suspend fun getOrCreateConversation(
        marketerId: String, clientId: String
    ): Result<Conversation> = withContext(Dispatchers.IO) {
        runCatching {
            val local = db.conversationDao().getByMarketer(marketerId)
            if (local != null) return@runCatching Result.Success(local.toDomain())
            val dto = supabase.client.postgrest.rpc(
                "get_or_create_conversation",
                GetOrCreateConversationParams(clientId)
            ).decodeAs<ConversationDto>()
            val entity = ConversationEntity(
                id         = dto.id,
                marketerId = marketerId,
                clientId   = dto.clientId,
                title      = "الإدارة",
                subject    = dto.subject,
                createdAt  = System.currentTimeMillis()
            )
            db.conversationDao().upsert(entity)
            Result.Success(entity.toDomain())
        }.getOrElse { Result.Error(it.message ?: "خطأ في فتح المحادثة", it) }
    }
    override suspend fun createNewConversation(
        marketerId: String, clientId: String, subject: String
    ): Result<Conversation> = withContext(Dispatchers.IO) {
        val scope = SyncScope.from(sessionReader.currentSession())
            ?: return@withContext Result.Error("جلسة منتهية. أعد تسجيل الدخول.")
        if (marketerId != scope.userId || clientId != scope.clientId) {
            return@withContext Result.Error("المحادثة لا تخص الجلسة الحالية")
        }
        val mutationId = UUID.randomUUID().toString()
        val localConversationId = mutationId
        val now = System.currentTimeMillis()
        val entity = ConversationEntity(
            id = localConversationId,
            marketerId = scope.userId,
            clientId = scope.clientId,
            subject = subject,
            title = subject.ifBlank { "الإدارة" },
            createdAt = now,
        )
        runCatching {
            db.withTransaction {
                check(SyncScope.from(sessionReader.currentSession()) == scope) { "STALE_LOCAL_MUTATION_SCOPE" }
                check(db.conversationDao().getById(localConversationId) == null) { "CONVERSATION_ID_COLLISION" }
                db.conversationDao().upsert(entity)
                db.pendingOperationDao().insert(
                    PendingOperationEntity(
                        id = "chat_create_$mutationId",
                        mutationId = mutationId,
                        userId = scope.userId,
                        clientId = scope.clientId,
                        orgId = scope.orgId,
                        entityType = OutboxEntityType.CONVERSATION,
                        entityId = localConversationId,
                        operation = OutboxOperationType.CREATE_CHAT_CONVERSATION,
                        payload = outboxJson.encodeToString(
                            CreateChatConversationOutboxPayload(localConversationId, subject),
                        ),
                        contractVersion = OUTBOX_CONTRACT_VERSION,
                    ),
                )
            }
            Result.Success(entity.toDomain())
        }.getOrElse { Result.Error(it.message ?: "تعذّر حفظ المحادثة", it) }
    }
    override suspend fun sendMessage(
        conversationId: String, senderId: String,
        type: MessageType, content: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val scope = SyncScope.from(sessionReader.currentSession())
            ?: return@withContext Result.Error("جلسة منتهية. أعد تسجيل الدخول.")
        if (senderId != scope.userId) {
            return@withContext Result.Error("جلسة منتهية. أعد تسجيل الدخول.")
        }
        val conversation = db.conversationDao().getById(conversationId)
            ?: return@withContext Result.Error("المحادثة غير موجودة")
        if (conversation.clientId != scope.clientId || conversation.marketerId != scope.userId) {
            return@withContext Result.Error("المحادثة لا تخص الجلسة الحالية")
        }

        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val preparedMedia = try {
            mediaManager.stageOutgoing(type, content, id, scope.orgId)
        } catch (error: Throwable) {
            return@withContext Result.Error(
                ChatMediaErrorMapper.userMessage(error.message.orEmpty()),
                error,
            )
        }
        val entity = ChatMessageEntity(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            senderType = SenderType.MARKETER.name,
            content = preparedMedia.displayBody,
            type = type.name,
            isRead = false,
            createdAt = now,
            status = "PENDING",
            mediaUrl = preparedMedia.mediaUrl,
            mediaMime = preparedMedia.mediaMime,
            mediaDurationMs = preparedMedia.mediaDurationMs,
            mediaObjectPath = preparedMedia.transfer?.objectPath,
            localPath = preparedMedia.localPath,
        )
        val preview = when (type) {
            MessageType.IMAGE -> "📷 صورة"
            MessageType.VOICE -> "🎤 رسالة صوتية"
            MessageType.TEXT -> preparedMedia.displayBody
        }
        val payload = ChatSendOutboxPayload(
            id = id,
            orgId = scope.orgId,
            clientId = scope.clientId,
            senderId = scope.userId,
            senderType = SenderType.MARKETER.name,
            type = type.name,
            body = preparedMedia.displayBody,
            mediaUrl = preparedMedia.mediaUrl,
            mediaMime = preparedMedia.mediaMime,
            mediaDurationMs = preparedMedia.mediaDurationMs,
            mediaObjectPath = preparedMedia.transfer?.objectPath,
            conversationId = conversationId,
        )

        return@withContext runCatching {
            var hasMediaTransfer = false
            db.withTransaction {
                check(SyncScope.from(sessionReader.currentSession()) == scope) { "STALE_LOCAL_MUTATION_SCOPE" }
                val currentConversation = db.conversationDao().getById(conversationId)
                    ?: error("CHAT_CONVERSATION_NOT_FOUND")
                check(currentConversation.clientId == scope.clientId && currentConversation.marketerId == scope.userId) {
                    "CHAT_SCOPE_MISMATCH"
                }
                val createParent = db.pendingOperationDao().findActiveForEntity(
                    scope.userId, scope.clientId, scope.orgId,
                    OutboxEntityType.CONVERSATION, conversationId, OutboxOperationType.CREATE_CHAT_CONVERSATION,
                )
                db.chatMessageDao().insert(entity)
                db.conversationDao().updateLastMessage(conversationId, preview, now, 0)
                db.pendingOperationDao().insert(
                    PendingOperationEntity(
                        id = "chat_${UUID.randomUUID()}",
                        mutationId = id,
                        userId = scope.userId,
                        clientId = scope.clientId,
                        orgId = scope.orgId,
                        entityType = OutboxEntityType.CHAT_MESSAGE,
                        entityId = id,
                        operation = OutboxOperationType.SEND_CHAT_MESSAGE,
                        payload = outboxJson.encodeToString(payload),
                        contractVersion = OUTBOX_CONTRACT_VERSION,
                        dependsOnMutationId = createParent?.mutationId,
                    )
                )
                preparedMedia.transfer?.let { staged ->
                    db.chatMediaTransferDao().insert(
                        ChatMediaTransferEntity(
                            transferId = "media_$id",
                            userId = scope.userId,
                            clientId = scope.clientId,
                            orgId = scope.orgId,
                            messageId = id,
                            localPath = staged.localPath,
                            mediaMime = staged.mediaMime,
                            sizeBytes = staged.sizeBytes,
                            contentSha256 = staged.contentSha256,
                            bucket = staged.bucket,
                            objectPath = staged.objectPath,
                        ),
                    )
                    hasMediaTransfer = true
                }
            }
            if (hasMediaTransfer) mediaTransferScheduler.enqueue()
            Result.Success(Unit)
        }.getOrElse { Result.Error(it.message ?: "تعذّر حفظ الرسالة للإرسال", it) }
    }

    override suspend fun retrySend(messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val scope = SyncScope.from(sessionReader.currentSession())
            ?: return@withContext Result.Error("جلسة منتهية. أعد تسجيل الدخول.")
        val msg = db.chatMessageDao().getById(messageId)
            ?: return@withContext Result.Error("الرسالة غير موجودة")
        val conversation = db.conversationDao().getById(msg.conversationId)
            ?: return@withContext Result.Error("المحادثة غير موجودة")
        if (
            msg.senderId != scope.userId ||
            conversation.clientId != scope.clientId ||
            conversation.marketerId != scope.userId
        ) return@withContext Result.Error("الرسالة لا تخص الجلسة الحالية")
        if (msg.status == "SENT" || msg.status == "READ") return@withContext Result.Success(Unit)

        runCatching {
            db.withTransaction {
                check(SyncScope.from(sessionReader.currentSession()) == scope) { "STALE_LOCAL_MUTATION_SCOPE" }
                val existing = db.pendingOperationDao().findActiveByMutationId(
                    scope.userId, scope.clientId, scope.orgId, msg.id,
                )
                db.chatMessageDao().updateStatus(msg.id, "PENDING")
                val transfer = db.chatMediaTransferDao().getForMessage(
                    scope.userId, scope.clientId, scope.orgId, msg.id,
                )
                if (transfer != null) {
                    db.chatMediaTransferDao().reactivateForMessage(
                        scope.userId, scope.clientId, scope.orgId, msg.id, System.currentTimeMillis(),
                    )
                }
                if (existing != null) {
                    db.pendingOperationDao().reactivate(
                        existing.id, scope.userId, scope.clientId, scope.orgId,
                    )
                } else {
                    val payload = ChatSendOutboxPayload(
                        id = msg.id,
                        orgId = scope.orgId,
                        clientId = scope.clientId,
                        senderId = msg.senderId,
                        senderType = msg.senderType,
                        type = msg.type,
                        body = msg.content,
                        mediaUrl = msg.mediaUrl,
                        mediaMime = msg.mediaMime,
                        mediaDurationMs = msg.mediaDurationMs,
                        mediaObjectPath = msg.mediaObjectPath,
                        conversationId = msg.conversationId,
                    )
                    val createParent = db.pendingOperationDao().findActiveForEntity(
                        scope.userId, scope.clientId, scope.orgId,
                        OutboxEntityType.CONVERSATION, msg.conversationId, OutboxOperationType.CREATE_CHAT_CONVERSATION,
                    )
                    db.pendingOperationDao().insert(
                        PendingOperationEntity(
                            id = "chat_${UUID.randomUUID()}",
                            mutationId = msg.id,
                            userId = scope.userId,
                            clientId = scope.clientId,
                            orgId = scope.orgId,
                            entityType = OutboxEntityType.CHAT_MESSAGE,
                            entityId = msg.id,
                            operation = OutboxOperationType.SEND_CHAT_MESSAGE,
                            payload = outboxJson.encodeToString(payload),
                            contractVersion = OUTBOX_CONTRACT_VERSION,
                            dependsOnMutationId = createParent?.mutationId,
                        )
                    )
                }
            }
            val transfer = db.chatMediaTransferDao().getForMessage(
                scope.userId, scope.clientId, scope.orgId, msg.id,
            )
            if (transfer != null && transfer.status != "COMPLETE") mediaTransferScheduler.enqueue()
            Result.Success(Unit)
        }.getOrElse { Result.Error(it.message ?: "تعذّر إعادة جدولة الرسالة", it) }
    }

    override suspend fun markMessagesAsRead(conversationId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val scope = SyncScope.from(sessionReader.currentSession())
                ?: return@withContext Result.Error("جلسة منتهية. أعد تسجيل الدخول.")
            runCatching {
                db.withTransaction {
                    check(SyncScope.from(sessionReader.currentSession()) == scope) { "STALE_LOCAL_MUTATION_SCOPE" }
                    val conversation = db.conversationDao().getById(conversationId)
                        ?: error("CHAT_CONVERSATION_NOT_FOUND")
                    check(conversation.clientId == scope.clientId && conversation.marketerId == scope.userId) {
                        "CHAT_SCOPE_MISMATCH"
                    }
                    db.chatMessageDao().markAdminMessagesRead(conversationId)
                    db.conversationDao().resetUnreadCount(conversationId)
                    val existing = db.pendingOperationDao().findActiveForEntity(
                        scope.userId,
                        scope.clientId,
                        scope.orgId,
                        OutboxEntityType.CHAT_MESSAGE,
                        conversationId,
                        OutboxOperationType.MARK_CHAT_READ,
                    )
                    if (existing == null) {
                        val mutationId = UUID.randomUUID().toString()
                        db.pendingOperationDao().insert(
                            PendingOperationEntity(
                                id = "chat_read_${UUID.randomUUID()}",
                                mutationId = mutationId,
                                userId = scope.userId,
                                clientId = scope.clientId,
                                orgId = scope.orgId,
                                entityType = OutboxEntityType.CHAT_MESSAGE,
                                entityId = conversationId,
                                operation = OutboxOperationType.MARK_CHAT_READ,
                                payload = outboxJson.encodeToString(ChatReadOutboxPayload(conversationId)),
                                contractVersion = OUTBOX_CONTRACT_VERSION,
                            )
                        )
                    }
                }
                Result.Success(Unit)
            }.getOrElse { Result.Error(it.message ?: "خطأ", it) }
        }

    override suspend fun syncMessages(clientId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val scope = SyncScope.from(sessionReader.currentSession())
                ?: return@withContext Result.Error("جلسة منتهية. أعد تسجيل الدخول.")
            if (scope.clientId != clientId) return@withContext Result.Error("المحادثة لا تخص الجلسة الحالية")
            runCatching {
                chatRecovery.recover(scope)
                mediaManager.cachePendingAdminMedia()
                Result.Success(Unit)
            }.getOrElse { Result.Error(it.message ?: "خطأ في جلب الرسائل", it) }
        }
    private fun ConversationEntity.toDomain() = Conversation(
        id            = id,
        marketerId    = marketerId,
        clientId      = clientId,
        title         = subject.ifBlank { title },
        subject       = subject,
        lastMessage   = lastMessage,
        lastMessageAt = lastMessageAt,
        unreadCount   = unreadCount,
        createdAt     = createdAt
    )
    private fun ChatMessageEntity.toDomain() = ChatMessage(
        id              = id,
        conversationId  = conversationId,
        senderId        = senderId,
        senderType      = runCatching { SenderType.valueOf(senderType) }.getOrDefault(SenderType.MARKETER),
        content         = content,
        type            = runCatching { MessageType.valueOf(type) }.getOrDefault(MessageType.TEXT),
        isRead          = isRead,
        createdAt       = createdAt,
        status          = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.PENDING),
        mediaUrl        = mediaManager.resolveMediaUrl(localPath, mediaUrl),
        mediaMime       = mediaMime,
        mediaDurationMs = mediaDurationMs,
    )
}
