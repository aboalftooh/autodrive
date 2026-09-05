package com.autodrive.app.core.session.di

import com.autodrive.app.core.session.domain.DashboardPreferences
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.session.domain.SessionWriter
import com.autodrive.app.core.session.data.PreferencesManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionModule {

    @Binds
    @Singleton
    abstract fun bindSessionReader(impl: PreferencesManager): SessionReader

    @Binds
    @Singleton
    abstract fun bindSessionWriter(impl: PreferencesManager): SessionWriter

    @Binds
    @Singleton
    abstract fun bindDashboardPreferences(impl: PreferencesManager): DashboardPreferences
}
