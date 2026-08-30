package com.autodrive.app.feature.profile.data

import com.autodrive.app.core.common.result.Result
import com.autodrive.app.core.model.account.AutoDriveUser
import com.autodrive.app.core.common.registration.RegistrationProfileWriter
import com.autodrive.app.feature.profile.domain.repository.ProfileRepository
import javax.inject.Inject

class RegistrationProfileWriterAdapter @Inject constructor(
    private val profileRepository: ProfileRepository,
) : RegistrationProfileWriter {
    override suspend fun saveRegisteredUser(user: AutoDriveUser): Result<Unit> =
        profileRepository.saveUser(user)
}
