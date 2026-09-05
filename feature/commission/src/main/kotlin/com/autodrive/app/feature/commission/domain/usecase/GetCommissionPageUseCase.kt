package com.autodrive.app.feature.commission.domain.usecase

import com.autodrive.app.feature.commission.domain.model.CommissionPage
import com.autodrive.app.feature.commission.domain.model.CommissionPageCursor
import com.autodrive.app.feature.commission.domain.repository.CommissionRepository
import javax.inject.Inject

class GetCommissionPageUseCase @Inject constructor(
    private val repository: CommissionRepository,
) {
    suspend operator fun invoke(cursor: CommissionPageCursor?, limit: Int = 10): CommissionPage =
        repository.getCommissionPage(cursor = cursor, limit = limit)
}
