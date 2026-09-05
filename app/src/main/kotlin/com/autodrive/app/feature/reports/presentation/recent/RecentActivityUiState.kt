package com.autodrive.app.feature.reports.presentation.recent

import com.autodrive.app.feature.chat.domain.model.Conversation

data class RecentActivityUiState(
    val conversations: List<Conversation> = emptyList(),
    val filteredConversations: List<Conversation> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
)
