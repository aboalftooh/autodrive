package com.autodrive.app.feature.balance.domain.repository

import com.autodrive.app.feature.balance.domain.model.BalanceTransaction
import com.autodrive.app.feature.balance.domain.model.MarketerBalance
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.feature.balance.domain.model.WithdrawalRequest
import com.autodrive.app.feature.balance.domain.model.WithdrawalSubmitResult
import com.autodrive.app.core.model.money.Money
import kotlinx.coroutines.flow.Flow

interface BalanceRepository {
    fun observeBalance(userId: String): Flow<MarketerBalance>
    fun observeTransactions(userId: String, clientId: String): Flow<List<BalanceTransaction>>
    fun observeWithdrawalRequests(userId: String): Flow<List<WithdrawalRequest>>
    suspend fun requestWithdrawal(amount: Money, note: String?, clientId: String): Result<WithdrawalSubmitResult>
    suspend fun cancelAllPendingWithdrawals(): Result<Int>
}
