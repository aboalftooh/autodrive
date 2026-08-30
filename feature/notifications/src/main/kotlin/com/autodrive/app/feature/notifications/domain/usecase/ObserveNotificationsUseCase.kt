package com.autodrive.app.feature.notifications.domain.usecase

import com.autodrive.app.feature.notifications.domain.model.AppNotification
import com.autodrive.app.feature.notifications.domain.repository.NotificationRepository
import com.autodrive.app.core.session.domain.SessionReader
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveNotificationsUseCase @Inject constructor(
    private val repository: NotificationRepository,
    private val sessionReader: SessionReader
) {
    operator fun invoke(): Flow<List<AppNotification>> =
        repository.observeNotifications(sessionReader.currentSession().userId.orEmpty())
}
