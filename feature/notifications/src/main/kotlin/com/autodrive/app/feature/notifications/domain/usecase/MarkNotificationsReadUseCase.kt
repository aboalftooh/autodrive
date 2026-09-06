package com.autodrive.app.feature.notifications.domain.usecase

import com.autodrive.app.core.common.result.Result
import com.autodrive.app.feature.notifications.domain.repository.NotificationRepository
import com.autodrive.app.core.session.domain.SessionReader
import javax.inject.Inject

class MarkNotificationsReadUseCase @Inject constructor(
    private val repository: NotificationRepository,
    private val sessionReader: SessionReader
) {
    suspend operator fun invoke(): Result<Unit> {
        val userId = sessionReader.currentSession().userId
            ?: return Result.Error("لم يتم تسجيل الدخول")
        return repository.markAllRead(userId)
    }
}
