package com.autodrive.app.core.database.dao

import androidx.room.*
import com.autodrive.app.core.database.entities.DynamoContentEntity

@Dao
abstract class DynamoContentDao {

    @Query("""
        SELECT * FROM dynamo_content
        WHERE is_active = 1
          AND (audience_type = :audienceType OR audience_type = 'both')
          AND (specialty = :specialty OR specialty = 'general')
        ORDER BY RANDOM() LIMIT 1
    """)
    abstract suspend fun getRandomMessage(audienceType: String, specialty: String): DynamoContentEntity?

    @Query("""
        SELECT * FROM dynamo_content
        WHERE is_active = 1
          AND (audience_type = :audienceType OR audience_type = 'both')
          AND (specialty = :specialty OR specialty = 'general')
          AND id NOT IN (:excludedIds)
        ORDER BY RANDOM() LIMIT 1
    """)
    abstract suspend fun getRandomMessageExcluding(
        audienceType: String,
        specialty: String,
        excludedIds: List<String>,
    ): DynamoContentEntity?

    @Query("SELECT COUNT(*) FROM dynamo_content WHERE is_active = 1")
    abstract suspend fun getActiveCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(messages: List<DynamoContentEntity>)

    @Query("DELETE FROM dynamo_content")
    abstract suspend fun clearAll()

    @Transaction
    open suspend fun clearAndInsert(messages: List<DynamoContentEntity>) {
        clearAll()
        if (messages.isNotEmpty()) insertAll(messages)
    }
}
