package com.autodrive.app.feature.balance.domain.usecase

import com.autodrive.app.feature.balance.domain.model.MarketerBalance
import com.autodrive.app.feature.balance.domain.repository.BalanceRepository
import com.autodrive.app.core.session.domain.SessionReader
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveBalanceUseCase @Inject constructor(
    private val repository: BalanceRepository,
    private val sessionReader: SessionReader
) {
    operator fun invoke(): Flow<MarketerBalance> =
        repository.observeBalance(sessionReader.currentSession().userId.orEmpty())
}
