package com.autodrive.app.core.database.dao

import androidx.room.*
import com.autodrive.app.core.database.entities.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations WHERE marketer_id = :marketerId ORDER BY last_message_at DESC")
    fun observeAll(marketerId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE marketer_id = :marketerId ORDER BY created_at DESC LIMIT 1")
    suspend fun getByMarketer(marketerId: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE client_id = :clientId LIMIT 1")
    suspend fun getByClientId(clientId: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE client_id = :clientId ORDER BY created_at ASC")
    suspend fun getAllByClientId(clientId: String): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE marketer_id = :marketerId")
    suspend fun getAllByMarketer(marketerId: String): List<ConversationEntity>

    @Query("UPDATE conversations SET id=:newId WHERE id=:oldId AND marketer_id=:marketerId AND client_id=:clientId")
    suspend fun remapId(oldId: String, newId: String, marketerId: String, clientId: String): Int

    @Query("DELETE FROM conversations WHERE id=:id AND marketer_id=:marketerId AND client_id=:clientId")
    suspend fun deleteExact(id: String, marketerId: String, clientId: String): Int

    @Query("DELETE FROM conversations WHERE id = :id AND client_id = :clientId")
    suspend fun deleteByIdForClient(id: String, clientId: String)

    @Query("DELETE FROM conversations WHERE marketer_id = :marketerId")
    suspend fun deleteByMarketer(marketerId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: ConversationEntity)

    @Query("""
        UPDATE conversations
        SET last_message = :lastMessage, last_message_at = :lastMessageAt,
            unread_count = unread_count + :unreadDelta
        WHERE id = :id
    """)
    suspend fun updateLastMessage(id: String, lastMessage: String, lastMessageAt: Long, unreadDelta: Int)

    @Query("UPDATE conversations SET unread_count = 0 WHERE id = :id")
    suspend fun resetUnreadCount(id: String)

    @Query("UPDATE conversations SET unread_count = :count WHERE id = :id")
    suspend fun setUnreadCount(id: String, count: Int)

    @Query("SELECT COALESCE(SUM(unread_count), 0) FROM conversations WHERE marketer_id = :marketerId")
    fun observeTotalUnread(marketerId: String): Flow<Int>
}
