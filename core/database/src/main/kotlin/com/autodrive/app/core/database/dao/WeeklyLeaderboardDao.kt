package com.autodrive.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.autodrive.app.core.database.entities.WeeklyLeaderboardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyLeaderboardDao {

    @Query("SELECT * FROM weekly_leaderboard_cache ORDER BY rank ASC")
    fun observeAll(): Flow<List<WeeklyLeaderboardEntity>>

    @Query("SELECT * FROM weekly_leaderboard_cache ORDER BY rank ASC")
    suspend fun getAll(): List<WeeklyLeaderboardEntity>

    @Upsert
    suspend fun upsertAll(entries: List<WeeklyLeaderboardEntity>)

    @Query("DELETE FROM weekly_leaderboard_cache")
    suspend fun clear()
}
