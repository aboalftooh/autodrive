package com.autodrive.app.core.sync.di

import com.autodrive.app.core.sync.data.SyncManager
import com.autodrive.app.core.sync.data.SyncEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncEngineModule {
    @Binds
    @Singleton
    abstract fun bindSyncEngine(impl: SyncManager): SyncEngine
}
