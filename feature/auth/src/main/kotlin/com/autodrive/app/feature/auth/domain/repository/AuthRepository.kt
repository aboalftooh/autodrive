package com.autodrive.app.feature.auth.domain.repository

import com.autodrive.app.core.common.result.Result
import com.autodrive.app.feature.auth.domain.model.JoinCodeVerificationResult
import com.autodrive.app.feature.auth.domain.model.PhoneEntryResult

interface AuthRepository {
    suspend fun enterPhone(phone: String): PhoneEntryResult
    suspend fun verifyJoinCode(phone: String, code: String): JoinCodeVerificationResult
    suspend fun sendPhoneOtp(phone: String): Result<String?>
    suspend fun verifyPhoneOtp(phone: String, otp: String): Result<Unit>

    suspend fun restoreSession(): Boolean
    suspend fun signOut()
    fun isLoggedIn(): Boolean
    fun isRegistrationComplete(): Boolean
    fun getCurrentUserId(): String
}
