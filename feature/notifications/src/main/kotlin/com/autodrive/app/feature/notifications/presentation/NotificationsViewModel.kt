package com.autodrive.app.feature.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.feature.notifications.domain.model.AppNotification
import com.autodrive.app.feature.notifications.domain.repository.NotificationRepository
import com.autodrive.app.feature.notifications.domain.usecase.MarkNotificationsReadUseCase
import com.autodrive.app.feature.notifications.domain.usecase.ObserveNotificationsUseCase
import com.autodrive.app.core.session.domain.SessionReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val notifications: List<AppNotification> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val observeNotifications: ObserveNotificationsUseCase,
    private val markAllRead: MarkNotificationsReadUseCase,
    private val notificationRepository: NotificationRepository,
    private val sessionReader: SessionReader
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeNotifications().collect { items ->
                _state.update { it.copy(isLoading = false, notifications = items) }
            }
        }
        viewModelScope.launch {
            sessionReader.currentSession().userId?.takeIf { it.isNotBlank() }?.let { userId ->
                when (val r = notificationRepository.syncNotifications(userId)) {
                    is com.autodrive.app.core.common.result.Result.Error ->
                        _state.update { s -> s.copy(error = r.message) }
                    else -> Unit
                }
            }
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            val userId = sessionReader.currentSession().userId ?: return@launch
            notificationRepository.markAsRead(id, userId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            markAllRead()
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
