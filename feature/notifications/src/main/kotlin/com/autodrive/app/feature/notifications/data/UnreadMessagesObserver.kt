package com.autodrive.app.feature.notifications.data

import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.feature.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/** Feature-owned unread source. App shell will collect it and pass presentation data to DS. */
class UnreadMessagesObserver @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sessionReader: SessionReader,
) {
    fun observe(): Flow<Int> =
        sessionReader.currentSession().userId
            ?.takeIf(String::isNotBlank)
            ?.let(chatRepository::observeUnreadConversationsCount)
            ?: flowOf(0)
}
