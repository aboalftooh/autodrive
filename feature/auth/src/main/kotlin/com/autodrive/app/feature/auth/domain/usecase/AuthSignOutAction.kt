package com.autodrive.app.feature.auth.domain.usecase

import com.autodrive.app.core.common.session.SignOutAction
import com.autodrive.app.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class AuthSignOutAction @Inject constructor(
    private val authRepository: AuthRepository,
) : SignOutAction {
    override suspend fun invoke() = authRepository.signOut()
}
