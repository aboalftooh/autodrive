package com.autodrive.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.autodrive.app.core.database.entities.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Upsert
    suspend fun upsertAll(notifications: List<NotificationEntity>)

    @Upsert
    suspend fun upsert(notification: NotificationEntity)

    @Query("SELECT * FROM notifications WHERE user_id = :userId ORDER BY created_at DESC LIMIT 50")
    fun observeByUserId(userId: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND is_read = 0")
    fun observeUnreadCount(userId: String): Flow<Int>

    @Query("SELECT * FROM notifications WHERE id = :id AND user_id = :userId LIMIT 1")
    suspend fun getById(id: String, userId: String): NotificationEntity?

    @Query("SELECT * FROM notifications WHERE user_id = :userId AND is_read = 0")
    suspend fun getUnread(userId: String): List<NotificationEntity>

    @Query("SELECT * FROM notifications WHERE user_id = :userId ORDER BY created_at, id")
    suspend fun getAllByUserIdForSync(userId: String): List<NotificationEntity>

    @Query("UPDATE notifications SET is_read = 1, read_synced = 0 WHERE id = :id AND user_id = :userId")
    suspend fun markAsRead(id: String, userId: String)

    @Query("UPDATE notifications SET is_read = 1, read_synced = 0 WHERE user_id = :userId AND is_read = 0")
    suspend fun markAllAsRead(userId: String)

    @Query("UPDATE notifications SET read_synced = 1 WHERE id = :id AND user_id = :userId")
    suspend fun confirmReadSynced(id: String, userId: String)

    @Query("UPDATE notifications SET read_synced = 1 WHERE user_id = :userId")
    suspend fun confirmAllReadSynced(userId: String)

    @Query("SELECT * FROM notifications WHERE user_id = :userId AND is_read = 1 AND read_synced = 0")
    suspend fun getUnsynced(userId: String): List<NotificationEntity>

    @Query("DELETE FROM notifications WHERE id = :id AND user_id = :userId")
    suspend fun deleteById(id: String, userId: String)

    @Query("DELETE FROM notifications WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String)
}
