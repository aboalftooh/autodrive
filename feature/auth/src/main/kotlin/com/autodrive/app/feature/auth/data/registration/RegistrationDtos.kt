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
    @SerialName("request_id") val requestId: String? = null,
    @SerialName("request_status") val requestStatus: String? = null,
    @SerialName("account_type") val accountType: String? = null,
)

@Serializable
data class JoinRequestDto(
    @SerialName("request_id") val requestId: String,
    val status: String,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class JoinRequestStatusDto(
    @SerialName("request_id") val requestId: String,
    val status: String,
    @SerialName("account_type") val accountType: String? = null,
    @SerialName("organization_name") val organizationName: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
)

@Serializable
data class ApprovedOtpResponse(
    val success: Boolean = false,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class ApprovedOtpVerificationResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long = 3600L,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("user_id") val userId: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("org_id") val orgId: String,
    @SerialName("account_type") val accountType: String,
    @SerialName("full_name") val fullName: String,
    val phone: String,
    @SerialName("onboarding_completed") val onboardingCompleted: Boolean = false,
)
