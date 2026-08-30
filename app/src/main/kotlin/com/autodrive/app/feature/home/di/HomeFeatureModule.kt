package com.autodrive.app.feature.home.di

import com.autodrive.app.feature.home.data.AiInsightRepositoryImpl
import com.autodrive.app.feature.home.data.DynamoContentRepositoryImpl
import com.autodrive.app.feature.home.domain.repository.AiInsightRepository
import com.autodrive.app.feature.home.domain.repository.DynamoContentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeFeatureModule {
    @Binds @Singleton
    abstract fun bindAiInsightRepository(impl: AiInsightRepositoryImpl): AiInsightRepository

    @Binds @Singleton
    abstract fun bindDynamoContentRepository(
        impl: DynamoContentRepositoryImpl
    ): DynamoContentRepository
}
