package com.autodrive.app.core.network

import com.autodrive.app.core.network.serialization.BigDecimalSerializer
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Server-authoritative weekly performance/goal contract.
 *
 * The Android client only renders the server snapshot. Week boundaries,
 * same-period comparison and goal suggestions stay on the server so every
 * device uses the same rules.
 */
@Singleton
class WeeklyPerformanceApi @Inject constructor(
    private val supabase: AutoDriveSupabase,
) {
    /**
     * [legacyTarget] is sent during normal app reads so the backend can perform
     * the one-time migration from the pre-server local preference. The backend
     * only accepts it while the account target is uninitialized; afterwards the
     * server value always wins, including across devices.
     */
    suspend fun getSnapshot(legacyTarget: BigDecimal? = null): WeeklyPerformanceDto {
        val response = if (legacyTarget == null) {
            supabase.client.postgrest.rpc("autodrive_weekly_performance_v1")
        } else {
            supabase.client.postgrest.rpc(
                "autodrive_weekly_performance_v1",
                LegacyWeeklyTargetParams(legacyTarget),
            )
        }
        return response.decodeList<WeeklyPerformanceDto>().single()
    }

    suspend fun setWeeklyTarget(target: BigDecimal): WeeklyTargetUpdateDto =
        supabase.client.postgrest
            .rpc(
                "autodrive_set_weekly_target_v1",
                SetWeeklyTargetParams(target),
            )
            .decodeList<WeeklyTargetUpdateDto>()
            .single()

    suspend fun snoozeTargetSuggestion(days: Int = 14) {
        supabase.client.postgrest.rpc(
            "autodrive_snooze_weekly_target_suggestion_v1",
            SnoozeWeeklyTargetSuggestionParams(days),
        )
    }
}

@Serializable
data class WeeklyPerformanceDto(
    @SerialName("week_start") val weekStart: String,
    @SerialName("week_end") val weekEnd: String,
    @SerialName("as_of") val asOf: String,
    @SerialName("current_amount")
    @Serializable(with = BigDecimalSerializer::class)
    val currentAmount: BigDecimal,
    @SerialName("current_count") val currentCount: Long,
    @SerialName("previous_same_period_amount")
    @Serializable(with = BigDecimalSerializer::class)
    val previousSamePeriodAmount: BigDecimal,
    @SerialName("previous_same_period_count") val previousSamePeriodCount: Long,
    @SerialName("change_percent")
    @Serializable(with = BigDecimalSerializer::class)
    val changePercent: BigDecimal? = null,
    val trend: String,
    @SerialName("weekly_target")
    @Serializable(with = BigDecimalSerializer::class)
    val weeklyTarget: BigDecimal,
    @SerialName("progress_percent")
    @Serializable(with = BigDecimalSerializer::class)
    val progressPercent: BigDecimal,
    @SerialName("remaining_to_target")
    @Serializable(with = BigDecimalSerializer::class)
    val remainingToTarget: BigDecimal,
    @SerialName("days_remaining") val daysRemaining: Int,
    @SerialName("required_daily_average")
    @Serializable(with = BigDecimalSerializer::class)
    val requiredDailyAverage: BigDecimal,
    @SerialName("target_achieved") val targetAchieved: Boolean,
    @SerialName("target_achieved_early") val targetAchievedEarly: Boolean,
    @SerialName("target_is_too_easy") val targetIsTooEasy: Boolean,
    @SerialName("target_suggestion_visible") val targetSuggestionVisible: Boolean,
    @SerialName("suggested_target")
    @Serializable(with = BigDecimalSerializer::class)
    val suggestedTarget: BigDecimal? = null,
)

@Serializable
private data class LegacyWeeklyTargetParams(
    @SerialName("p_legacy_target")
    @Serializable(with = BigDecimalSerializer::class)
    val target: BigDecimal,
)

@Serializable
private data class SetWeeklyTargetParams(
    @SerialName("p_target")
    @Serializable(with = BigDecimalSerializer::class)
    val target: BigDecimal,
)

@Serializable
data class WeeklyTargetUpdateDto(
    @SerialName("weekly_target")
    @Serializable(with = BigDecimalSerializer::class)
    val weeklyTarget: BigDecimal,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
private data class SnoozeWeeklyTargetSuggestionParams(
    @SerialName("p_days") val days: Int,
)
