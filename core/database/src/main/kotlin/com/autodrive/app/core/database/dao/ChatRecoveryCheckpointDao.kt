package com.autodrive.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.autodrive.app.core.database.entities.ChatRecoveryCheckpointEntity

@Dao
interface ChatRecoveryCheckpointDao {
    @Query(
        """SELECT * FROM chat_recovery_checkpoints
           WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId AND conversation_id=:conversationId
           LIMIT 1""",
    )
    suspend fun get(userId: String, clientId: String, orgId: String, conversationId: String): ChatRecoveryCheckpointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(checkpoint: ChatRecoveryCheckpointEntity)

    @Query("DELETE FROM chat_recovery_checkpoints WHERE user_id=:userId AND client_id=:clientId AND org_id=:orgId")
    suspend fun deleteForScope(userId: String, clientId: String, orgId: String): Int
}
