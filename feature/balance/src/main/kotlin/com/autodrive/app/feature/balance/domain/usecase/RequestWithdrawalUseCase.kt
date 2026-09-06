package com.autodrive.app.feature.balance.domain.usecase

import com.autodrive.app.core.common.result.Result
import com.autodrive.app.feature.balance.domain.model.WithdrawalSubmitResult
import com.autodrive.app.feature.balance.domain.repository.BalanceRepository
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.model.money.Money
import javax.inject.Inject

class RequestWithdrawalUseCase @Inject constructor(
    private val repository: BalanceRepository,
    private val sessionReader: SessionReader
) {
    suspend operator fun invoke(amount: Money, note: String?): Result<WithdrawalSubmitResult> {
        val clientId = sessionReader.currentSession().clientId
            ?: return Result.Error("لم يتم تسجيل الدخول")
        return repository.requestWithdrawal(amount, note, clientId)
    }
}
