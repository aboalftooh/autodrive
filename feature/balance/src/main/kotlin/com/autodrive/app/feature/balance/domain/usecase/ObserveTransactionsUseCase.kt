package com.autodrive.app.feature.balance.domain.usecase

import com.autodrive.app.feature.balance.domain.model.BalanceTransaction
import com.autodrive.app.feature.balance.domain.repository.BalanceRepository
import com.autodrive.app.core.session.domain.SessionReader
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTransactionsUseCase @Inject constructor(
    private val repository: BalanceRepository,
    private val sessionReader: SessionReader
) {
    operator fun invoke(): Flow<List<BalanceTransaction>> {
        val session = sessionReader.currentSession()
        return repository.observeTransactions(
            userId = session.userId.orEmpty(),
            clientId = session.clientId.orEmpty()
        )
    }
}
