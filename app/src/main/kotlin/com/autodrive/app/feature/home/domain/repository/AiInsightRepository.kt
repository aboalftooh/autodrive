package com.autodrive.app.feature.home.domain.repository

import com.autodrive.app.feature.home.domain.model.AiInsight

interface AiInsightRepository {
    suspend fun triggerGenerate(userId: String): AiInsight?
    suspend fun getLatest(userId: String): AiInsight?
    suspend fun getLatestN(userId: String, n: Int = 5): List<AiInsight>
}
