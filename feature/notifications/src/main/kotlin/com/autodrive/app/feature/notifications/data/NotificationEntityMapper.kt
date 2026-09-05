package com.autodrive.app.feature.notifications.data

import com.autodrive.app.core.database.entities.NotificationEntity
import com.autodrive.app.core.network.dto.NotificationDto

internal fun NotificationDto.toEntity() = NotificationEntity(
    id = id,
    userId = userId,
    clientId = clientId,
    type = type,
    title = title,
    body = body,
    isRead = isRead,
    createdAt = createdAt,
)
