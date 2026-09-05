package com.autodrive.app.feature.chat.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetOrCreateConversationParams(
    @SerialName("p_client_id") val clientId: String
)

@Serializable
data class CreateNewConversationParams(
    @SerialName("p_client_id") val clientId: String,
    @SerialName("p_subject")   val subject: String = ""
)

@Serializable
data class ChatMessageInsertDto(
    val id: String,
    @SerialName("org_id")            val orgId: String,
    @SerialName("client_id")         val clientId: String,
    @SerialName("sender_id")         val senderId: String,
    @SerialName("sender_type")       val senderType: String,
    val type: String = "TEXT",
    val body: String,
    @SerialName("media_url")         val mediaUrl: String?      = null,
    @SerialName("media_mime")        val mediaMime: String?     = null,
    @SerialName("media_duration_ms") val mediaDurationMs: Long? = null,
    @SerialName("conversation_id")   val conversationId: String? = null,
)
