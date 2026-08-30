package com.autodrive.app.core.network.dto.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageDto(
    val id: String = "",
    @SerialName("client_id") val clientId: String = "",
    @SerialName("org_id") val orgId: String = "",
    @SerialName("sender_id") val senderId: String = "",
    @SerialName("sender_type") val senderType: String = "ADMIN",
    val type: String = "TEXT",
    val body: String = "",
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("media_mime") val mediaMime: String? = null,
    @SerialName("media_duration_ms") val mediaDurationMs: Long? = null,
    @SerialName("media_object_path") val mediaObjectPath: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("chat_recovery_seq") val chatRecoverySequence: Long? = null,
    @SerialName("conversation_id") val conversationId: String? = null,
)

@Serializable
data class ConversationDto(
    val id: String = "",
    @SerialName("org_id") val orgId: String = "",
    @SerialName("client_id") val clientId: String = "",
    val subject: String = "",
    @SerialName("last_message") val lastMessage: String? = null,
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("marketer_unread") val marketerUnread: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
)
