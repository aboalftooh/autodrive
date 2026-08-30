package com.autodrive.app.core.sync.di

import com.autodrive.app.core.sync.data.BlockedServerDeletionFeed
import com.autodrive.app.core.sync.data.DeletionFeed
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DeletionFeedModule {
    @Provides
    @Singleton
    fun provideDeletionFeed(): DeletionFeed = BlockedServerDeletionFeed()
}
