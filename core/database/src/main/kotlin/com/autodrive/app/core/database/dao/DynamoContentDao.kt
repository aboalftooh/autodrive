package com.autodrive.app.core.database.dao

import androidx.room.*
import com.autodrive.app.core.database.entities.DynamoContentEntity

@Dao
abstract class DynamoContentDao {

    @Query("SELECT * FROM dynamo_content WHERE is_active = 1 ORDER BY RANDOM() LIMIT 1")
    abstract suspend fun getRandomMessage(): DynamoContentEntity?

    // استدعاء هذه الدالة فقط عندما تكون excludedIds غير فارغة
    @Query("SELECT * FROM dynamo_content WHERE is_active = 1 AND id NOT IN (:excludedIds) ORDER BY RANDOM() LIMIT 1")
    abstract suspend fun getRandomMessageExcluding(excludedIds: List<String>): DynamoContentEntity?

    @Query("SELECT COUNT(*) FROM dynamo_content WHERE is_active = 1")
    abstract suspend fun getActiveCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(messages: List<DynamoContentEntity>)

    @Query("DELETE FROM dynamo_content")
    abstract suspend fun clearAll()

    @Transaction
    open suspend fun clearAndInsert(messages: List<DynamoContentEntity>) {
        clearAll()
        insertAll(messages)
    }
}
