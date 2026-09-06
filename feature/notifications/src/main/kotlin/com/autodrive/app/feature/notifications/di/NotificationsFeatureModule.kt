package com.autodrive.app.feature.notifications.di

import com.autodrive.app.feature.notifications.data.NotificationRepositoryImpl
import com.autodrive.app.feature.notifications.domain.repository.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsFeatureModule {
    @Binds @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
}
