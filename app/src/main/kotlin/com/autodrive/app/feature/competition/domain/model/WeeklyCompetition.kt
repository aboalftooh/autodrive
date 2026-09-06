package com.autodrive.app.feature.competition.domain.model

import com.autodrive.app.core.model.money.Money

data class LeaderboardEntry(
    val rank: Int,
    val totalAmount: Money,
    val isMe: Boolean
)

data class WeeklyCompetitionData(
    val entries: List<LeaderboardEntry>,
    val myWinCount: Int?,
    val weekNumber: Int = 0,
    val isFromCache: Boolean = false
)

data class WeeklyRankingRow(
    val weekStartLabel: String,
    val weekEndLabel: String,
    val myTotal: Money,
    val myRank: Int?
)

data class WinWeek(
    val weekStartLabel: String,
    val weekEndLabel: String
)
