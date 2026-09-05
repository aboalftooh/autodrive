package com.autodrive.app.feature.chat.domain.repository

import com.autodrive.app.core.common.result.Result
import com.autodrive.app.feature.chat.domain.model.ChatMessage
import com.autodrive.app.feature.chat.domain.model.Conversation
import com.autodrive.app.feature.chat.domain.model.MessageType
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeConversations(marketerId: String): Flow<List<Conversation>>
    fun observeUnreadConversationsCount(marketerId: String): Flow<Int>
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>
    suspend fun searchMessageContent(query: String): List<String>
    suspend fun getOrCreateConversation(marketerId: String, clientId: String): Result<Conversation>
    suspend fun createNewConversation(marketerId: String, clientId: String, subject: String): Result<Conversation>
    suspend fun sendMessage(conversationId: String, senderId: String, type: MessageType, content: String): Result<Unit>
    suspend fun retrySend(messageId: String): Result<Unit>
    suspend fun markMessagesAsRead(conversationId: String): Result<Unit>
    suspend fun syncMessages(clientId: String): Result<Unit>
}
