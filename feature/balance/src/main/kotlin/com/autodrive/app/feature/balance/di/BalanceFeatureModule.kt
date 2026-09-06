package com.autodrive.app.feature.balance.di

import com.autodrive.app.feature.balance.data.BalanceRepositoryImpl
import com.autodrive.app.feature.balance.domain.repository.BalanceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BalanceFeatureModule {
    @Binds @Singleton
    abstract fun bindBalanceRepository(impl: BalanceRepositoryImpl): BalanceRepository
}
