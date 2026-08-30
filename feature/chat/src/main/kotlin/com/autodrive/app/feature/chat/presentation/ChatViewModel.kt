package com.autodrive.app.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.feature.chat.domain.model.MessageType
import com.autodrive.app.feature.chat.domain.repository.ChatRepository
import com.autodrive.app.core.session.domain.SessionReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val sessionReader: SessionReader
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    fun init(conversationId: String, title: String) {
        _uiState.update { it.copy(conversationTitle = title) }

        viewModelScope.launch {
            repository.observeMessages(conversationId).collect { msgs ->
                _uiState.update { it.copy(messages = msgs, isLoading = false) }
            }
        }

        viewModelScope.launch {
            val clientId = sessionReader.currentSession().clientId ?: return@launch
            repository.syncMessages(clientId)
            repository.markMessagesAsRead(conversationId)
        }
    }

    fun send(conversationId: String, type: MessageType, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val uid = sessionReader.currentSession().userId.orEmpty()
            if (uid.isBlank()) {
                _uiState.update { it.copy(error = "خطأ في جلسة المستخدم، أعد تسجيل الدخول") }
                return@launch
            }
            when (val result = repository.sendMessage(conversationId, uid, type, content)) {
                is com.autodrive.app.core.common.result.Result.Error ->
                    _uiState.update { it.copy(error = result.message) }
                else -> Unit
            }
        }
    }

    fun retry(messageId: String) {
        viewModelScope.launch {
            when (val result = repository.retrySend(messageId)) {
                is com.autodrive.app.core.common.result.Result.Error ->
                    _uiState.update { it.copy(error = result.message) }
                else -> Unit
            }
        }
    }

    fun markRead(conversationId: String) {
        viewModelScope.launch { repository.markMessagesAsRead(conversationId) }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
