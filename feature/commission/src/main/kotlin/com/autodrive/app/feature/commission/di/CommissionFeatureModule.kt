package com.autodrive.app.feature.commission.di

import com.autodrive.app.feature.commission.data.CommissionRepositoryImpl
import com.autodrive.app.feature.commission.data.InvoiceDetailRepositoryImpl
import com.autodrive.app.feature.commission.domain.repository.CommissionRepository
import com.autodrive.app.feature.commission.domain.repository.InvoiceDetailRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CommissionFeatureModule {
    @Binds @Singleton
    abstract fun bindCommissionRepository(impl: CommissionRepositoryImpl): CommissionRepository

    @Binds @Singleton
    abstract fun bindInvoiceDetailRepository(impl: InvoiceDetailRepositoryImpl): InvoiceDetailRepository
}
