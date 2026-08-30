package com.autodrive.app.feature.auth.domain.model

sealed class CodeVerificationResult {
    data class Success(
        val clientId: String,
        val orgId: String,
        val isExistingUser: Boolean = false
    ) : CodeVerificationResult()

    data object Invalid : CodeVerificationResult()
    data object Expired : CodeVerificationResult()
    data object AlreadyUsed : CodeVerificationResult()
    data class Error(val message: String) : CodeVerificationResult()
}
