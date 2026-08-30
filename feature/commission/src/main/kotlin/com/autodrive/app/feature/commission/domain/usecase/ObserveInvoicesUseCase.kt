package com.autodrive.app.feature.commission.domain.usecase

import com.autodrive.app.feature.commission.domain.model.Invoice
import com.autodrive.app.feature.commission.domain.repository.CommissionRepository
import com.autodrive.app.core.session.domain.SessionReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class ObserveInvoicesUseCase @Inject constructor(
    private val repository: CommissionRepository,
    private val sessionReader: SessionReader
) {
    operator fun invoke(): Flow<List<Invoice>> {
        val clientId = sessionReader.currentSession().clientId ?: return flowOf(emptyList())
        return repository.observeInvoices(clientId)
    }
}
