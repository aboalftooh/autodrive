package com.autodrive.app.feature.chat.data

import com.autodrive.app.core.database.entities.ChatMessageEntity
import com.autodrive.app.core.network.dto.chat.ChatMessageDto
import java.time.OffsetDateTime

internal fun ChatMessageDto.toChatEntity(conversationId: String) = ChatMessageEntity(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    senderType = senderType,
    content = body,
    type = type.ifBlank { "TEXT" },
    isRead = isRead,
    createdAt = runCatching {
        OffsetDateTime.parse(createdAt).toInstant().toEpochMilli()
    }.getOrDefault(System.currentTimeMillis()),
    status = "SENT",
    mediaUrl = mediaUrl,
    mediaMime = mediaMime,
    mediaDurationMs = mediaDurationMs,
)
