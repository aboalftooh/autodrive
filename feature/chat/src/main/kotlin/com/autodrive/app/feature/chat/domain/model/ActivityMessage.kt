package com.autodrive.app.feature.chat.domain.model

// ─── Enums ────────────────────────────────────
enum class MessageType   { TEXT, VOICE, IMAGE }
enum class SenderType    { MARKETER, ADMIN }
enum class MessageStatus { PENDING, SENT, READ, FAILED }

// ─── Conversation ─────────────────────────────
data class Conversation(
    val id: String,
    val marketerId: String,
    val clientId: String,
    val title: String,
    val subject: String = "",
    val lastMessage: String,
    val lastMessageAt: Long,
    val unreadCount: Int,
    val createdAt: Long
)

// ─── ChatMessage ──────────────────────────────
data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderType: SenderType,
    val content: String,
    val type: MessageType,
    val isRead: Boolean,
    val createdAt: Long,
    val status: MessageStatus,
    val mediaUrl: String? = null,
    val mediaMime: String? = null,
    val mediaDurationMs: Long? = null,
)
