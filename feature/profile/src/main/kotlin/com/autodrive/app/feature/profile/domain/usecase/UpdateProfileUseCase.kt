package com.autodrive.app.feature.profile.domain.usecase

import com.autodrive.app.core.model.account.AutoDriveUser
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.feature.profile.domain.repository.ProfileRepository
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(user: AutoDriveUser): Result<Unit> =
        repository.updateUser(user)
}
