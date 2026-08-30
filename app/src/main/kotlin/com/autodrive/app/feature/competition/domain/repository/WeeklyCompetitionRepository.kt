package com.autodrive.app.feature.competition.domain.repository

import com.autodrive.app.feature.competition.domain.model.WeeklyCompetitionData
import com.autodrive.app.feature.competition.domain.model.WeeklyRankingRow
import com.autodrive.app.feature.competition.domain.model.WinWeek
import kotlinx.coroutines.flow.Flow

interface WeeklyCompetitionRepository {
    fun observeLeaderboard(): Flow<WeeklyCompetitionData>
    suspend fun refresh()
    suspend fun getCompetitionHistory(limit: Int, offset: Int): List<WeeklyRankingRow>
    suspend fun getWinWeeks(): List<WinWeek>
}
