package com.autodrive.app.core.sync.di

import com.autodrive.app.core.sync.fault.NoOpSyncFaultInjector
import com.autodrive.app.core.sync.fault.SyncFaultInjector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncFaultModule {
    @Provides
    @Singleton
    fun provideSyncFaultInjector(): SyncFaultInjector = NoOpSyncFaultInjector()
}
