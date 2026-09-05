package com.autodrive.app.core.sync.di

import com.autodrive.app.core.sync.data.DefaultSyncCoordinator
import com.autodrive.app.core.sync.realtime.RealtimeManager
import com.autodrive.app.core.sync.domain.RealtimeConnectionObserver
import com.autodrive.app.core.sync.domain.RealtimeController
import com.autodrive.app.core.sync.domain.SyncConnectivity
import com.autodrive.app.core.sync.domain.SyncCoordinator
import com.autodrive.app.core.sync.data.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindSyncCoordinator(impl: DefaultSyncCoordinator): SyncCoordinator


    @Binds
    @Singleton
    abstract fun bindRealtimeConnectionObserver(impl: RealtimeManager): RealtimeConnectionObserver

    @Binds
    @Singleton
    abstract fun bindRealtimeController(impl: RealtimeManager): RealtimeController

    @Binds
    @Singleton
    abstract fun bindSyncConnectivity(impl: NetworkMonitor): SyncConnectivity
}
