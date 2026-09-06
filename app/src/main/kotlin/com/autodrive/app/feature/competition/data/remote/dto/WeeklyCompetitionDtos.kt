package com.autodrive.app.feature.competition.data.remote.dto

import com.autodrive.app.core.network.serialization.BigDecimalSerializer
import java.math.BigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeeklyEntryDto(
    val rank: Int = 0,
    @SerialName("total_amount")
    @Serializable(with = BigDecimalSerializer::class)
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    @SerialName("is_me") val isMe: Boolean = false
)

@Serializable
data class WeeklyCompetitionMetaDto(
    @SerialName("week_number") val weekNumber: Int = 0,
    @SerialName("week_start")  val weekStart: String = "",
    @SerialName("week_end")    val weekEnd: String = ""
)
