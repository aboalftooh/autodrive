package com.autodrive.app.feature.competition.data

import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.database.entities.WeeklyLeaderboardEntity
import com.autodrive.app.core.model.money.Money
import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.core.observability.AppLogger
import com.autodrive.app.feature.competition.data.remote.dto.CompetitionHistoryParams
import com.autodrive.app.feature.competition.data.remote.dto.WeeklyEntryDto
import com.autodrive.app.feature.competition.data.remote.dto.WeeklyRankingDto
import com.autodrive.app.feature.competition.data.remote.dto.WinWeekDto
import com.autodrive.app.feature.competition.domain.model.LeaderboardEntry
import com.autodrive.app.feature.competition.domain.model.WeeklyCompetitionData
import com.autodrive.app.feature.competition.domain.model.WeeklyRankingRow
import com.autodrive.app.feature.competition.domain.model.WinWeek
import com.autodrive.app.feature.competition.domain.repository.WeeklyCompetitionRepository
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "WeeklyCompetition"
private const val NETWORK_TIMEOUT_MS = 15_000L

@Singleton
class WeeklyCompetitionRepositoryImpl @Inject constructor(
    private val supabase: AutoDriveSupabase,
    private val db: AutoDriveDatabase,
) : WeeklyCompetitionRepository {

    private val myWinCount = MutableStateFlow<Int?>(null)
    private val remoteRefreshSucceeded = MutableStateFlow(false)

    override fun observeLeaderboard(): Flow<WeeklyCompetitionData> =
        combine(
            db.weeklyLeaderboardDao().observeAll(),
            myWinCount,
            remoteRefreshSucceeded,
        ) { cached, wins, remoteSucceeded ->
            WeeklyCompetitionData(
                entries = cached.map { it.toDomain() },
                myWinCount = wins,
                weekNumber = cached.firstOrNull()?.weekNumber ?: 0,
                isFromCache = cached.isNotEmpty() && !remoteSucceeded,
            )
        }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        remoteRefreshSucceeded.value = false
        withTimeout(NETWORK_TIMEOUT_MS) {
            val leaderboard = try {
                fetchLeaderboardByRpc()
            } catch (error: Throwable) {
                AppLogger.w(TAG, "leaderboard_rpc_failed: ${error.message}")
                throw error
            }

            runCatching { fetchWinWeeksByRpc().size }
                .onSuccess { myWinCount.value = it }
                .onFailure { AppLogger.w(TAG, "win_weeks_rpc_failed: ${it.message}") }

            cacheLeaderboard(leaderboard)
            remoteRefreshSucceeded.value = true
            AppLogger.d(
                TAG,
                "refresh → winCount=${myWinCount.value} leaderboard.size=${leaderboard.entries.size}",
            )
        }
    }

    override suspend fun getCompetitionHistory(limit: Int, offset: Int): List<WeeklyRankingRow> =
        withContext(Dispatchers.IO) {
            withTimeout(NETWORK_TIMEOUT_MS) {
                supabase.client.postgrest
                    .rpc("get_my_competition_history", CompetitionHistoryParams(limit, offset))
                    .decodeList<WeeklyRankingDto>()
                    .map { dto ->
                        WeeklyRankingRow(
                            weekStartLabel = formatDate(parseIso(dto.weekStart)),
                            weekEndLabel = formatDate(parseIso(dto.weekEnd)),
                            myTotal = Money.of(dto.myTotal),
                            myRank = dto.myRank?.toInt(),
                        )
                    }
            }
        }

    override suspend fun getWinWeeks(): List<WinWeek> =
        withContext(Dispatchers.IO) {
            withTimeout(NETWORK_TIMEOUT_MS) { fetchWinWeeksByRpc() }
        }

    private suspend fun fetchLeaderboardByRpc(): WeeklyCompetitionData {
        val entries = supabase.client.postgrest
            .rpc("get_weekly_competition")
            .decodeList<WeeklyEntryDto>()
            .map { dto ->
                LeaderboardEntry(
                    rank = dto.rank,
                    totalAmount = Money.of(dto.totalAmount),
                    isMe = dto.isMe,
                )
            }
        return WeeklyCompetitionData(entries = entries, myWinCount = myWinCount.value)
    }

    private suspend fun fetchWinWeeksByRpc(): List<WinWeek> =
        supabase.client.postgrest
            .rpc("get_my_win_weeks")
            .decodeList<WinWeekDto>()
            .map { dto ->
                WinWeek(
                    weekStartLabel = formatDate(parseIso(dto.weekStart)),
                    weekEndLabel = formatDate(parseIso(dto.weekEnd)),
                )
            }

    private suspend fun cacheLeaderboard(data: WeeklyCompetitionData) {
        db.weeklyLeaderboardDao().clear()
        if (data.entries.isNotEmpty()) {
            db.weeklyLeaderboardDao().upsertAll(
                data.entries.map { entry ->
                    WeeklyLeaderboardEntity(
                        id = entry.rank.toString(),
                        rank = entry.rank,
                        totalAmount = entry.totalAmount.amount,
                        isMe = entry.isMe,
                        weekNumber = data.weekNumber,
                    )
                },
            )
        }
    }

    private fun WeeklyLeaderboardEntity.toDomain() = LeaderboardEntry(
        rank = rank,
        totalAmount = Money.of(totalAmount),
        isMe = isMe,
    )

    private fun parseIso(iso: String): Long {
        if (iso.isBlank()) return 0L
        return runCatching { Instant.parse(iso).toEpochMilli() }
            .recoverCatching {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(iso)?.time ?: 0L
            }
            .getOrDefault(0L)
    }

    private fun formatDate(ms: Long): String {
        if (ms <= 0L) return ""
        return SimpleDateFormat("d/M/yyyy", Locale("ar")).format(Date(ms))
    }
}
