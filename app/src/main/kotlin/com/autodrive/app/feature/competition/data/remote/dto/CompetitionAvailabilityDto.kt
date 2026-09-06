package com.autodrive.app.feature.competition.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CompetitionAvailabilityDto(
    @SerialName("feature_key")
    val featureKey: String = "",
    val state: String = "",
    @SerialName("updated_at")
    val updatedAt: String? = null
)
