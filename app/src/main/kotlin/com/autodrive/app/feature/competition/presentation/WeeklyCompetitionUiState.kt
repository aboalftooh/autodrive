package com.autodrive.app.feature.competition.presentation

import com.autodrive.app.feature.competition.domain.model.WeeklyCompetitionData

data class WeeklyCompetitionUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val data: WeeklyCompetitionData? = null,
    val errorMessage: String? = null,
)
