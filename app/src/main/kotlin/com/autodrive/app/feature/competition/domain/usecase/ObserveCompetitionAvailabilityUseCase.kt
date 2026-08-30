package com.autodrive.app.feature.competition.domain.usecase

import com.autodrive.app.feature.competition.domain.model.CompetitionAvailability
import com.autodrive.app.feature.competition.domain.repository.CompetitionAvailabilityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveCompetitionAvailabilityUseCase @Inject constructor(
    private val repository: CompetitionAvailabilityRepository
) {
    operator fun invoke(): Flow<CompetitionAvailability> =
        repository.observeAvailability()

    suspend fun refresh() =
        repository.refreshAvailability()
}
