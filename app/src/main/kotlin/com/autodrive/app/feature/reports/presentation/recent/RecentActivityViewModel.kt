package com.autodrive.app.feature.reports.presentation.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.feature.chat.domain.repository.ChatRepository
import com.autodrive.app.core.session.domain.SessionReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class RecentActivityViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val sessionReader: SessionReader
) : ViewModel() {

    val userId: String get() = sessionReader.currentSession().userId.orEmpty()
    val clientId: String get() = sessionReader.currentSession().clientId.orEmpty()

    private val _searchQuery = MutableStateFlow("")
    private val _uiState     = MutableStateFlow(RecentActivityUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val uid = userId
        if (uid.isNotBlank()) {
            viewModelScope.launch {
                val cid = clientId
                if (cid.isNotBlank()) {
                    when (val r = repository.syncMessages(cid)) {
                        is com.autodrive.app.core.common.result.Result.Error ->
                            _uiState.update { it.copy(error = r.message) }
                        else -> Unit
                    }
                }
            }
            viewModelScope.launch {
                repository.observeConversations(uid)
                    .combine(_searchQuery.debounce(200)) { convs, query ->
                        Pair(convs, query)
                    }
                    .collect { (convs, query) ->
                        val filtered = if (query.isBlank()) convs
                        else {
                            val msgIds = repository.searchMessageContent(query).toSet()
                            convs.filter { conv ->
                                conv.title.contains(query, ignoreCase = true) ||
                                conv.lastMessage.contains(query, ignoreCase = true) ||
                                conv.id in msgIds
                            }
                        }
                        _uiState.update {
                            it.copy(
                                conversations         = convs,
                                filteredConversations = filtered,
                                searchQuery           = query,
                                isLoading             = false
                            )
                        }
                    }
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onSearchQuery(q: String) {
        _searchQuery.value = q
        _uiState.update { it.copy(searchQuery = q) }
    }

    fun openOrCreateConversation(onReady: (conversationId: String) -> Unit) {
        viewModelScope.launch {
            val existing = uiState.value.conversations.firstOrNull()
            if (existing != null) { onReady(existing.id); return@launch }
            when (val result = repository.getOrCreateConversation(userId, clientId)) {
                is com.autodrive.app.core.common.result.Result.Success -> onReady(result.data.id)
                is com.autodrive.app.core.common.result.Result.Error ->
                    _uiState.update { it.copy(error = result.message) }
                else -> Unit
            }
        }
    }

    fun refresh() {
        val cid = clientId
        if (cid.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            when (val r = repository.syncMessages(cid)) {
                is com.autodrive.app.core.common.result.Result.Error ->
                    _uiState.update { it.copy(error = r.message) }
                else -> Unit
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
