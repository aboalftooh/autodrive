package com.autodrive.app.core.database.dao

import androidx.room.*
import com.autodrive.app.core.database.entities.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages WHERE conversation_id = :conversationId ORDER BY created_at ASC")
    fun observeByConversation(conversationId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversation_id IN (:conversationIds) ORDER BY conversation_id, created_at, id")
    suspend fun getAllByConversationIdsForSync(conversationIds: List<String>): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)

    // تُستخدم في المزامنة فقط — رسالة موجودة تبقى في محادثتها الأصلية بدون تغيير
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET is_read = 1 WHERE conversation_id = :conversationId AND sender_type = 'ADMIN'")
    suspend fun markAdminMessagesRead(conversationId: String)

    @Query("UPDATE chat_messages SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE chat_messages SET created_at = :createdAt WHERE id = :id")
    suspend fun updateCreatedAt(id: String, createdAt: Long)

    @Query("SELECT DISTINCT conversation_id FROM chat_messages WHERE content LIKE '%' || :query || '%'")
    suspend fun findConversationIdsByContent(query: String): List<String>

    @Query("SELECT * FROM chat_messages WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ChatMessageEntity?

    @Query("SELECT * FROM chat_messages WHERE status = :status ORDER BY created_at ASC")
    suspend fun getByStatus(status: String): List<ChatMessageEntity>

    @Query("""UPDATE chat_messages
        SET is_read=:isRead, status=:status, media_url=:mediaUrl, media_mime=:mediaMime,
            media_duration_ms=:mediaDurationMs, media_object_path=:mediaObjectPath
        WHERE id=:id""")
    suspend fun updateRemoteState(
        id: String, isRead: Boolean, status: String, mediaUrl: String?, mediaMime: String?, mediaDurationMs: Long?,
        mediaObjectPath: String?,
    )

    @Query("SELECT * FROM chat_messages WHERE conversation_id=:conversationId ORDER BY created_at DESC, id DESC LIMIT 1")
    suspend fun getLatestByConversation(conversationId: String): ChatMessageEntity?

    @Query("SELECT COUNT(*) FROM chat_messages WHERE conversation_id=:conversationId AND sender_type='ADMIN' AND is_read=0")
    suspend fun countUnreadAdmin(conversationId: String): Int

    @Query("UPDATE chat_messages SET conversation_id=:newConversationId WHERE conversation_id=:oldConversationId")
    suspend fun remapConversationId(oldConversationId: String, newConversationId: String): Int

    @Query("""UPDATE chat_messages
        SET media_url=:compatibilityUrl, media_object_path=:objectPath
        WHERE id=:messageId AND sender_id=:senderId AND status='PENDING'""")
    suspend fun finalizePendingMediaReference(
        messageId: String, senderId: String, compatibilityUrl: String?, objectPath: String,
    ): Int

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM chat_messages WHERE conversation_id IN (:conversationIds)")
    suspend fun deleteByConversationIds(conversationIds: List<String>)

    @Query("UPDATE chat_messages SET local_path = :path WHERE id = :id")
    suspend fun updateLocalPath(id: String, path: String)

    @Query("""
        SELECT * FROM chat_messages
        WHERE (type = 'IMAGE' OR type = 'VOICE')
        AND (media_url IS NOT NULL OR media_object_path IS NOT NULL)
        AND local_path IS NULL
        AND status = 'SENT'
    """)
    suspend fun getAdminMediaNeedingDownload(): List<ChatMessageEntity>
}
