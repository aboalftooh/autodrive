package com.autodrive.app.feature.chat.presentation

import com.autodrive.app.feature.chat.domain.model.ChatMessage

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val conversationTitle: String   = "الإدارة",
    val isAdminTyping: Boolean      = false,
    val isLoading: Boolean          = true,
    val error: String?              = null
)
