package com.autodrive.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.autodrive.app.core.database.entities.AutoDriveUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoDriveUserDao {

    @Upsert
    suspend fun upsert(user: AutoDriveUserEntity)

    @Query("SELECT * FROM autodrive_users WHERE user_id = :userId LIMIT 1")
    fun observe(userId: String): Flow<AutoDriveUserEntity?>

    @Query("SELECT * FROM autodrive_users WHERE user_id = :userId LIMIT 1")
    suspend fun get(userId: String): AutoDriveUserEntity?

    @Query("UPDATE autodrive_users SET sync_status = :status WHERE user_id = :userId")
    suspend fun updateSyncStatus(userId: String, status: String)

    @Query("DELETE FROM autodrive_users WHERE id = :id AND user_id = :userId")
    suspend fun deleteByIdForUser(id: String, userId: String)

    @Query("DELETE FROM autodrive_users WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String)
}
