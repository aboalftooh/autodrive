package com.autodrive.app.feature.competition.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.core.observability.AppLogger
import com.autodrive.app.feature.competition.data.remote.dto.CompetitionAvailabilityDto
import com.autodrive.app.feature.competition.domain.model.CompetitionAvailability
import com.autodrive.app.feature.competition.domain.repository.CompetitionAvailabilityRepository
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CompetitionAvailability"
private const val WEEKLY_COMPETITION_FEATURE_KEY = "weekly_competition"

private val availabilityStateKey = stringPreferencesKey("competition_availability_state")
private val availabilityUpdatedAtKey = stringPreferencesKey("competition_availability_updated_at")

internal fun parseCompetitionAvailability(rawState: String?): CompetitionAvailability =
    when (rawState) {
        CompetitionAvailability.DISABLED.name -> CompetitionAvailability.DISABLED
        CompetitionAvailability.LOCKED.name -> CompetitionAvailability.LOCKED
        CompetitionAvailability.ACTIVE.name -> CompetitionAvailability.ACTIVE
        else -> CompetitionAvailability.DISABLED
    }

internal sealed interface CompetitionAvailabilityRefreshResult {
    data class RemoteSuccess(
        val dto: CompetitionAvailabilityDto?
    ) : CompetitionAvailabilityRefreshResult

    data object NetworkFailure : CompetitionAvailabilityRefreshResult
}

internal data class CompetitionAvailabilityCacheUpdate(
    val availability: CompetitionAvailability,
    val updatedAt: String?
)

internal fun CompetitionAvailabilityRefreshResult.toCacheUpdateOrNull(): CompetitionAvailabilityCacheUpdate? =
    when (this) {
        CompetitionAvailabilityRefreshResult.NetworkFailure -> null
        is CompetitionAvailabilityRefreshResult.RemoteSuccess -> CompetitionAvailabilityCacheUpdate(
            availability = parseCompetitionAvailability(dto?.state),
            updatedAt = dto?.updatedAt?.takeIf { it.isNotBlank() }
        )
    }

@Singleton
class CompetitionAvailabilityRepositoryImpl @Inject constructor(
    private val supabase: AutoDriveSupabase,
    private val dataStore: DataStore<Preferences>
) : CompetitionAvailabilityRepository {

    override fun observeAvailability(): Flow<CompetitionAvailability> =
        dataStore.data
            .catch { error ->
                AppLogger.w(TAG, "availability_cache_read_failed: ${error.message}")
                emit(emptyPreferences())
            }
            .map { preferences ->
                parseCompetitionAvailability(preferences[availabilityStateKey])
            }

    override suspend fun refreshAvailability() = withContext(Dispatchers.IO) {
        val result = try {
            CompetitionAvailabilityRefreshResult.RemoteSuccess(fetchRemoteAvailability())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppLogger.w(TAG, "availability_remote_refresh_failed: ${error.message}")
            CompetitionAvailabilityRefreshResult.NetworkFailure
        }

        val cacheUpdate = result.toCacheUpdateOrNull() ?: return@withContext
        runCatching {
            dataStore.edit { preferences ->
                preferences[availabilityStateKey] = cacheUpdate.availability.name
                if (cacheUpdate.updatedAt == null) {
                    preferences.remove(availabilityUpdatedAtKey)
                } else {
                    preferences[availabilityUpdatedAtKey] = cacheUpdate.updatedAt
                }
            }
        }.onFailure { error ->
            AppLogger.w(TAG, "availability_cache_write_failed: ${error.message}")
        }
    }

    private suspend fun fetchRemoteAvailability(): CompetitionAvailabilityDto? =
        supabase.client.postgrest["autodrive_feature_flags"]
            .select(Columns.ALL) {
                filter { eq("feature_key", WEEKLY_COMPETITION_FEATURE_KEY) }
                limit(1)
            }
            .decodeSingleOrNull<CompetitionAvailabilityDto>()
}
