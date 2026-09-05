package com.autodrive.app.feature.home.domain.repository

import com.autodrive.app.feature.home.domain.model.DynamoContentMessage

interface DynamoContentRepository {
    /** Replaces the scoped Room cache even when the server result is empty. */
    suspend fun syncMessages(audienceType: String, specialty: String): Boolean
    suspend fun getRandomLocalMessage(audienceType: String, specialty: String): DynamoContentMessage?
    suspend fun getRandomLocalMessageExcluding(
        audienceType: String,
        specialty: String,
        ids: List<String>,
    ): DynamoContentMessage?
}
