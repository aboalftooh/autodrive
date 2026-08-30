package com.autodrive.app.feature.reports.di

import com.autodrive.app.feature.reports.data.InvoiceDetailRepositoryImpl
import com.autodrive.app.feature.reports.domain.repository.InvoiceDetailRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReportsFeatureModule {
    @Binds @Singleton
    abstract fun bindInvoiceDetailRepository(
        impl: InvoiceDetailRepositoryImpl
    ): InvoiceDetailRepository
}
