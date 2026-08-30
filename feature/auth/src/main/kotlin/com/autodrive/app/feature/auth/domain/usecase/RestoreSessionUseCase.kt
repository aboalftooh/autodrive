package com.autodrive.app.feature.auth.domain.usecase

import com.autodrive.app.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class RestoreSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Boolean = authRepository.restoreSession()
}
