package com.autodrive.app.core.sync.di

import com.autodrive.app.core.sync.data.BootstrapSnapshotSource
import com.autodrive.app.core.sync.data.ReconciliationManifestSource
import com.autodrive.app.core.sync.data.SupabaseUnifiedSyncGateway
import com.autodrive.app.core.sync.data.UnifiedChangeFeed
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UnifiedSyncModule {
    @Binds @Singleton abstract fun bindUnifiedChangeFeed(impl: SupabaseUnifiedSyncGateway): UnifiedChangeFeed
    @Binds @Singleton abstract fun bindBootstrapSnapshotSource(impl: SupabaseUnifiedSyncGateway): BootstrapSnapshotSource
    @Binds @Singleton abstract fun bindReconciliationManifestSource(impl: SupabaseUnifiedSyncGateway): ReconciliationManifestSource
}
