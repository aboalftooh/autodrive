package com.autodrive.app.feature.auth.domain.repository

import com.autodrive.app.core.common.result.Result
import com.autodrive.app.feature.auth.domain.model.JoinRequestStatus
import com.autodrive.app.feature.auth.domain.model.PhoneEntryResult

interface AuthRepository {
    suspend fun enterPhone(phone: String): PhoneEntryResult
    suspend fun sendPhoneOtp(phone: String): Result<String?>
    suspend fun verifyPhoneOtp(phone: String, otp: String): Result<Unit>

    suspend fun submitJoinRequest(
        phone: String,
        fullName: String,
        accountType: String,
    ): Result<String>

    suspend fun getJoinRequestStatus(requestId: String): Result<JoinRequestStatus>
    suspend fun sendApprovedPhoneOtp(phone: String, requestId: String): Result<Unit>
    suspend fun verifyApprovedPhoneOtp(phone: String, otp: String, requestId: String): Result<Unit>

    suspend fun restoreSession(): Boolean
    suspend fun signOut()
    fun isLoggedIn(): Boolean
    fun isRegistrationComplete(): Boolean
    fun getCurrentUserId(): String
}
