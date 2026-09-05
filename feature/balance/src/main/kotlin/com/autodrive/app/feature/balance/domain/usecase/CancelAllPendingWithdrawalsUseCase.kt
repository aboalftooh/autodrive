package com.autodrive.app.feature.balance.domain.usecase

import com.autodrive.app.core.common.result.Result
import com.autodrive.app.feature.balance.domain.repository.BalanceRepository
import javax.inject.Inject

class CancelAllPendingWithdrawalsUseCase @Inject constructor(
    private val repository: BalanceRepository
) {
    suspend operator fun invoke(): Result<Int> = repository.cancelAllPendingWithdrawals()
}
