package com.autodrive.app.feature.competition.domain.repository

import com.autodrive.app.feature.competition.domain.model.CompetitionAvailability
import kotlinx.coroutines.flow.Flow

interface CompetitionAvailabilityRepository {
    fun observeAvailability(): Flow<CompetitionAvailability>
    suspend fun refreshAvailability()
}
