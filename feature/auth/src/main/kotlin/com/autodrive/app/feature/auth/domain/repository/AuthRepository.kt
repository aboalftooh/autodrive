package com.autodrive.app.feature.auth.domain.repository

import com.autodrive.app.feature.auth.domain.model.CodeVerificationResult
import com.autodrive.app.core.common.result.Result

/**
 * Authentication/session lifecycle boundary used by auth UI and use cases.
 *
 * OTP send/verify and invite operations are remote contracts; successful session verification is
 * distinct from registration completion. Session restore/sign-out own authentication lifecycle,
 * while tenant/client scope is established only by the implemented registration/linking flow.
 * Failures must remain explicit [Result] or domain results rather than being interpreted from UI state.
 */
interface AuthRepository {
    suspend fun sendPhoneOtp(phone: String): Result<String?>
    suspend fun verifyPhoneOtp(phone: String, otp: String): Result<Unit>
    suspend fun verifyInviteCode(code: String): CodeVerificationResult
    suspend fun restoreSession(): Boolean
    suspend fun signOut()
    fun isLoggedIn(): Boolean
    fun isRegistrationComplete(): Boolean
    fun getCurrentUserId(): String
}
