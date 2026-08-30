package com.autodrive.app.feature.auth.di

import com.autodrive.app.core.common.session.SignOutAction
import com.autodrive.app.feature.auth.data.AuthRepositoryImpl
import com.autodrive.app.feature.auth.domain.usecase.AuthSignOutAction
import com.autodrive.app.feature.auth.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthFeatureModule {
    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindSignOutAction(impl: AuthSignOutAction): SignOutAction
}
