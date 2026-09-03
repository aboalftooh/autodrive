package com.autodrive.app.feature.auth.data.registration

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegistrationEnvelope<T>(
    val data: T? = null,
    val error: String? = null,
    val code: String? = null,
)

@Serializable
data class PhoneEntryDto(
    @SerialName("next_action") val nextAction: String,
    @SerialName("account_type") val accountType: String? = null,
)

@Serializable
data class JoinCodeVerificationDto(
    @SerialName("is_valid") val isValid: Boolean,
    val reason: String,
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("org_id") val orgId: String? = null,
    @SerialName("account_type") val accountType: String? = null,
)
