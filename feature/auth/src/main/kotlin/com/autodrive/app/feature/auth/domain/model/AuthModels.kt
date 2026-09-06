package com.autodrive.app.feature.auth.domain.model

sealed class PhoneEntryResult {
    data object LoginOtp : PhoneEntryResult()
    data object JoinCodeRequired : PhoneEntryResult()
    data object AccountSelectionRequired : PhoneEntryResult()
    data class Error(val message: String) : PhoneEntryResult()
}

sealed class JoinCodeVerificationResult {
    data class Valid(
        val clientId: String,
        val orgId: String,
        val accountType: String,
    ) : JoinCodeVerificationResult()
    data class Invalid(val reason: String) : JoinCodeVerificationResult()
    data class Error(val message: String) : JoinCodeVerificationResult()
}
