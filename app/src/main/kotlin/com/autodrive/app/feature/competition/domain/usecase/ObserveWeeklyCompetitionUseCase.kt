package com.autodrive.app.feature.competition.domain.usecase

import com.autodrive.app.feature.competition.domain.model.WeeklyCompetitionData
import com.autodrive.app.feature.competition.domain.repository.WeeklyCompetitionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveWeeklyCompetitionUseCase @Inject constructor(
    private val repository: WeeklyCompetitionRepository
) {
    operator fun invoke(): Flow<WeeklyCompetitionData> = repository.observeLeaderboard()
    suspend fun refresh() = repository.refresh()
}
