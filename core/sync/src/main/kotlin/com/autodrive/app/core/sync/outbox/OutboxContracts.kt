package com.autodrive.app.core.sync.outbox

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object OutboxOperationType {
    const val UPDATE_PROFILE = "UPDATE_PROFILE"
    const val REQUEST_WITHDRAWAL_RPC = "REQUEST_WITHDRAWAL_RPC"
    const val SEND_CHAT_MESSAGE = "SEND_CHAT_MESSAGE"
    const val CREATE_CHAT_CONVERSATION = "CREATE_CHAT_CONVERSATION"
    const val MARK_CHAT_READ = "MARK_CHAT_READ"
    const val MARK_NOTIFICATION_READ = "MARK_NOTIFICATION_READ"
}

object OutboxEntityType {
    const val PROFILE = "autodrive_users"
    const val WITHDRAWAL = "withdrawal_requests"
    const val CHAT_MESSAGE = "internal_messages"
    const val CONVERSATION = "conversations"
    const val NOTIFICATION = "notifications"
}

const val OUTBOX_CONTRACT_VERSION = 1

@Serializable
data class ChatSendOutboxPayload(
    val id: String,
    @SerialName("org_id") val orgId: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("sender_type") val senderType: String,
    val type: String,
    val body: String,
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("media_mime") val mediaMime: String? = null,
    @SerialName("media_duration_ms") val mediaDurationMs: Long? = null,
    @SerialName("media_object_path") val mediaObjectPath: String? = null,
    @SerialName("conversation_id") val conversationId: String,
)


@Serializable
data class CreateChatConversationOutboxPayload(
    @SerialName("local_conversation_id") val localConversationId: String,
    val subject: String,
)

@Serializable
data class ChatReadOutboxPayload(
    @SerialName("conversation_id") val conversationId: String,
)

@Serializable
data class NotificationReadOutboxPayload(
    @SerialName("notification_id") val notificationId: String,
)
