package com.autodrive.app.feature.notifications.domain.repository

import com.autodrive.app.feature.notifications.domain.model.AppNotification
import com.autodrive.app.core.common.result.Result
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeNotifications(userId: String): Flow<List<AppNotification>>
    fun observeUnreadCount(userId: String): Flow<Int>
    suspend fun markAsRead(notificationId: String, userId: String)
    suspend fun markAllRead(userId: String): Result<Unit>
    suspend fun syncNotifications(userId: String): Result<Unit>
}
