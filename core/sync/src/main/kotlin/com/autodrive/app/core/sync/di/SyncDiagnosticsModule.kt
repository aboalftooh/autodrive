package com.autodrive.app.core.sync.di

import com.autodrive.app.core.sync.diagnostics.DefaultSyncDiagnostics
import com.autodrive.app.core.sync.diagnostics.SyncDiagnostics
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncDiagnosticsModule {
    @Binds
    abstract fun bindSyncDiagnostics(impl: DefaultSyncDiagnostics): SyncDiagnostics
}
