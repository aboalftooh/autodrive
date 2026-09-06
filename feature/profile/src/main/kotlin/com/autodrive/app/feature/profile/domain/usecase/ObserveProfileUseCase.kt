package com.autodrive.app.feature.profile.domain.usecase

import com.autodrive.app.core.model.account.AutoDriveUser
import com.autodrive.app.feature.profile.domain.repository.ProfileRepository
import com.autodrive.app.core.session.domain.SessionReader
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveProfileUseCase @Inject constructor(
    private val repository: ProfileRepository,
    private val sessionReader: SessionReader
) {
    operator fun invoke(): Flow<AutoDriveUser?> =
        repository.observeUser(sessionReader.currentSession().userId.orEmpty())
}
