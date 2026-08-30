package com.autodrive.app.feature.profile.domain.repository

import com.autodrive.app.core.model.account.AutoDriveUser
import com.autodrive.app.core.common.result.Result
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeUser(userId: String): Flow<AutoDriveUser?>
    suspend fun saveUser(user: AutoDriveUser): Result<Unit>
    suspend fun updateUser(user: AutoDriveUser): Result<Unit>
}
