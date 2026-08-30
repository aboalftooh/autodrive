package com.autodrive.app.feature.profile.di

import com.autodrive.app.core.common.registration.RegistrationProfileWriter
import com.autodrive.app.feature.profile.data.ProfileRepositoryImpl
import com.autodrive.app.feature.profile.data.RegistrationProfileWriterAdapter
import com.autodrive.app.feature.profile.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileFeatureModule {
    @Binds @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds @Singleton
    abstract fun bindRegistrationProfileWriter(
        impl: RegistrationProfileWriterAdapter,
    ): RegistrationProfileWriter
}
