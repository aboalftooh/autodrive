package com.autodrive.app.feature.commission.domain.usecase

import com.autodrive.app.feature.commission.domain.model.CommissionEntry
import com.autodrive.app.feature.commission.domain.model.CommissionSummary
import com.autodrive.app.feature.commission.domain.repository.CommissionRepository
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.model.money.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class ObserveCommissionsUseCase @Inject constructor(
    private val repository: CommissionRepository,
    private val sessionReader: SessionReader
) {
    operator fun invoke(): Flow<Pair<CommissionSummary, List<CommissionEntry>>> {
        val clientId = sessionReader.currentSession().clientId ?: return flowOf(
            Pair(CommissionSummary(Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, "", 0L), emptyList())
        )
        return repository.observeCommissions(clientId)
    }
}
