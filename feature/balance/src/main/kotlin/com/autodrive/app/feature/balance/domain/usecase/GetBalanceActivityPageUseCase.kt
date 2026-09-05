package com.autodrive.app.feature.balance.domain.usecase

import com.autodrive.app.feature.balance.domain.model.BalanceActivityPage
import com.autodrive.app.feature.balance.domain.model.BalanceActivityPageCursor
import com.autodrive.app.feature.balance.domain.repository.BalanceRepository
import javax.inject.Inject

class GetBalanceActivityPageUseCase @Inject constructor(
    private val repository: BalanceRepository,
) {
    suspend operator fun invoke(
        cursor: BalanceActivityPageCursor?,
        limit: Int,
    ): BalanceActivityPage = repository.getBalanceActivityPage(cursor, limit)
}
