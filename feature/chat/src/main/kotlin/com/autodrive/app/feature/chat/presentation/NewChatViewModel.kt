package com.autodrive.app.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.feature.chat.domain.model.MessageType
import com.autodrive.app.feature.chat.domain.repository.ChatRepository
import com.autodrive.app.core.session.domain.SessionReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class NewChatState(
    val isCreating: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NewChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val sessionReader: SessionReader
) : ViewModel() {

    private val _state = MutableStateFlow(NewChatState())
    val state: StateFlow<NewChatState> = _state.asStateFlow()

    fun createAndSend(
        text: String,
        mediaType: MessageType? = null,
        mediaContent: String? = null,
        onReady: (conversationId: String) -> Unit
    ) {
        if (_state.value.isCreating) return
        val session = sessionReader.currentSession()
        val userId = session.userId
        val clientId = session.clientId
        if (userId.isNullOrBlank() || clientId.isNullOrBlank()) {
            _state.update { it.copy(error = "جلسة منتهية. أعد تسجيل الدخول.") }
            return
        }

        val subject = when {
            text.isNotBlank() -> text.take(40).trim()
            mediaType == MessageType.IMAGE -> "صورة — ${dateLabel()}"
            mediaType == MessageType.VOICE -> "رسالة صوتية — ${dateLabel()}"
            else -> "محادثة — ${dateLabel()}"
        }

        _state.value = NewChatState(isCreating = true)
        viewModelScope.launch {
            when (val r = repository.createNewConversation(userId, clientId, subject)) {
                is Result.Success -> {
                    val convId = r.data.id
                    if (text.isNotBlank()) {
                        repository.sendMessage(convId, userId, MessageType.TEXT, text.trim())
                    }
                    if (mediaType != null && !mediaContent.isNullOrBlank()) {
                        repository.sendMessage(convId, userId, mediaType, mediaContent)
                    }
                    _state.value = NewChatState(isCreating = false)
                    onReady(convId)
                }
                is Result.Error -> {
                    _state.value = NewChatState(isCreating = false, error = r.message)
                }
                else -> {
                    _state.value = NewChatState(isCreating = false)
                }
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun dateLabel(): String =
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
}
