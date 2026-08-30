package com.autodrive.app.feature.auth.domain.usecase

import com.autodrive.app.feature.auth.domain.model.CodeVerificationResult
import com.autodrive.app.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyInviteCodeUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(code: String): CodeVerificationResult =
        authRepository.verifyInviteCode(code)
}
