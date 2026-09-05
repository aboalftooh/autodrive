package com.autodrive.app.feature.home.data

import com.autodrive.app.core.observability.AppLogger
import com.autodrive.app.core.database.dao.DynamoContentDao
import com.autodrive.app.core.database.entities.DynamoContentEntity
import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.feature.home.domain.model.DynamoContentMessage
import com.autodrive.app.feature.home.domain.repository.DynamoContentRepository
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DynamoContentRepo"

@Serializable
private data class DynamoContentDto(
    val id: String = "",
    @SerialName("content_type")   val contentType: String = "",
    @SerialName("audience_type")  val audienceType: String = "",
    val specialty: String = "general",
    val message: String = "",
    val priority: Int = 1,
    @SerialName("is_active")      val isActive: Boolean = true,
    @SerialName("created_at")     val createdAt: String = "",
)

private fun DynamoContentDto.toEntity() = DynamoContentEntity(
    id           = id,
    contentType  = contentType,
    audienceType = audienceType,
    specialty    = specialty.ifBlank { "general" },
    message      = message,
    priority     = priority,
    isActive     = isActive,
    createdAt    = createdAt,
)

private fun DynamoContentEntity.toDomain() = DynamoContentMessage(
    id          = id,
    contentType = contentType,
    message     = message,
)

@Singleton
class DynamoContentRepositoryImpl @Inject constructor(
    private val supabase: AutoDriveSupabase,
    private val dao: DynamoContentDao,
) : DynamoContentRepository {

    /**
     * يجلب كل الرسائل النشطة من Supabase ثم يُصفّيها محلياً حسب
     * audienceType وspecialty، ثم يستبدل الكاش المحلي بها.
     */
    override suspend fun syncMessages(
        audienceType: String,
        specialty: String,
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            AppLogger.d(TAG,"fetching dynamo_content from Supabase")
            val all = supabase.client.postgrest["dynamo_content"]
                .select(Columns.ALL) { filter { eq("is_active", true) } }
                .decodeList<DynamoContentDto>()
            val filtered = all.filter { row ->
                (row.audienceType == audienceType || row.audienceType == "both") &&
                    (row.specialty == specialty || row.specialty == "general")
            }
            // Empty is authoritative too: remove stale content from another audience/account.
            dao.clearAndInsert(filtered.map { it.toEntity() })
            AppLogger.d(TAG,"Room scoped cache replaced: ${filtered.size} messages")
            true
        }.onFailure {
            AppLogger.e(TAG,"syncMessages failed: ${it.message}")
        }.getOrDefault(false)
    }

    override suspend fun getRandomLocalMessage(
        audienceType: String,
        specialty: String,
    ): DynamoContentMessage? = withContext(Dispatchers.IO) {
        runCatching { dao.getRandomMessage(audienceType, specialty)?.toDomain() }
            .onFailure { AppLogger.e(TAG,"getRandomLocalMessage error: ${it.message}") }
            .getOrNull()
    }

    override suspend fun getRandomLocalMessageExcluding(
        audienceType: String,
        specialty: String,
        ids: List<String>,
    ): DynamoContentMessage? = withContext(Dispatchers.IO) {
        runCatching { dao.getRandomMessageExcluding(audienceType, specialty, ids)?.toDomain() }
            .onFailure { AppLogger.e(TAG,"getRandomMessageExcluding error: ${it.message}") }
            .getOrNull()
    }
}
