package com.autodrive.app.feature.home.data

import com.autodrive.app.core.observability.AppLogger
import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.feature.home.domain.model.AiInsight
import com.autodrive.app.feature.home.domain.repository.AiInsightRepository
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.call.body
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AiInsightRepo"

@Serializable
private data class InsightPayload(
    val title: String = "",
    val message: String = "",
    val type: String = "",
)

@Serializable
private data class GenerateInsightResponse(
    val success: Boolean = false,
    val cached: Boolean = false,
    val insight: InsightPayload? = null,
)

@Serializable
private data class AiInsightDto(
    val id: String = "",
    @SerialName("user_id")    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "",
    @SerialName("created_at") val createdAt: String = "",
)

@Singleton
class AiInsightRepositoryImpl @Inject constructor(
    private val supabase: AutoDriveSupabase,
) : AiInsightRepository {

    // يستدعي الـ Edge Function — تتولى الـ cooldown والتوليد داخلياً
    override suspend fun triggerGenerate(userId: String): AiInsight? = withContext(Dispatchers.IO) {
        runCatching {
            val response = supabase.client.functions.invoke(
                function = "generate-ai-insight",
                body = buildJsonObject { put("user_id", userId) },
            )
            if (!response.status.isSuccess()) {
                AppLogger.w(TAG,"triggerGenerate HTTP ${response.status.value}")
                return@runCatching null
            }
            val result = response.body<GenerateInsightResponse>()
            result.insight?.let { AiInsight(title = it.title, message = it.message, type = it.type) }
        }.onFailure { AppLogger.e(TAG,"triggerGenerate failed: ${it.message}") }.getOrNull()
    }

    // يقرأ آخر insight مخزَّن مباشرة من الجدول — بدون استدعاء OpenAI
    override suspend fun getLatest(userId: String): AiInsight? = withContext(Dispatchers.IO) {
        runCatching {
            supabase.client.postgrest["ai_insights"]
                .select(Columns.ALL) {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                    limit(1)
                }
                .decodeList<AiInsightDto>()
                .firstOrNull()
                ?.let { AiInsight(title = it.title, message = it.message, type = it.type) }
        }.onFailure { AppLogger.e(TAG,"getLatest failed: ${it.message}") }.getOrNull()
    }

    // يقرأ آخر N رسائل — لعرض التناوب في الـ UI
    override suspend fun getLatestN(userId: String, n: Int): List<AiInsight> = withContext(Dispatchers.IO) {
        runCatching {
            supabase.client.postgrest["ai_insights"]
                .select(Columns.ALL) {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                    limit(n.toLong())
                }
                .decodeList<AiInsightDto>()
                .map { AiInsight(title = it.title, message = it.message, type = it.type) }
        }.onFailure { AppLogger.e(TAG,"getLatestN failed: ${it.message}") }.getOrElse { emptyList() }
    }
}
