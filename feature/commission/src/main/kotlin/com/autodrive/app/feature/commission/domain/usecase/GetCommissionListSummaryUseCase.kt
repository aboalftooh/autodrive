package com.autodrive.app.feature.commission.domain.usecase

import com.autodrive.app.feature.commission.domain.model.CommissionListSummary
import com.autodrive.app.feature.commission.domain.repository.CommissionRepository
import javax.inject.Inject

class GetCommissionListSummaryUseCase @Inject constructor(
    private val repository: CommissionRepository,
) {
    suspend operator fun invoke(): CommissionListSummary = repository.getCommissionListSummary()
}
