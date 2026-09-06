package com.autodrive.app.feature.competition.data.remote.dto

import com.autodrive.app.core.network.serialization.BigDecimalSerializer
import java.math.BigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeeklyRankingDto(
    @SerialName("week_start") val weekStart: String = "",
    @SerialName("week_end")   val weekEnd:   String = "",
    @SerialName("my_total")
    @Serializable(with = BigDecimalSerializer::class)
    val myTotal: BigDecimal = BigDecimal.ZERO,
    @SerialName("my_rank")    val myRank:    Long?  = null
)

@Serializable
data class WinWeekDto(
    @SerialName("week_start")   val weekStart:   String = "",
    @SerialName("week_end")     val weekEnd:     String = "",
    @SerialName("total_amount")
    @Serializable(with = BigDecimalSerializer::class)
    val totalAmount: BigDecimal = BigDecimal.ZERO
)

@Serializable
data class CompetitionHistoryParams(
    @SerialName("p_limit")  val limit:  Int,
    @SerialName("p_offset") val offset: Int
)

@Serializable
data class WeeklyCompetitionWeekDto(
    val id: Long = 0,
    @SerialName("org_id") val orgId: String = "",
    @SerialName("week_number") val weekNumber: Int = 0,
    @SerialName("week_start") val weekStart: String = "",
    @SerialName("week_end") val weekEnd: String? = null,
    @SerialName("is_closed") val isClosed: Boolean = false
)

@Serializable
data class WeeklyCompetitionResultDto(
    val id: Long = 0,
    @SerialName("org_id") val orgId: String = "",
    @SerialName("week_id") val weekId: Long = 0,
    @SerialName("client_id") val clientId: String = "",
    val rank: Int = 0,
    @SerialName("total_amount")
    @Serializable(with = BigDecimalSerializer::class)
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val badge: String? = null
)
