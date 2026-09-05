package com.autodrive.app.feature.home.domain.repository

import com.autodrive.app.feature.home.domain.model.DynamoContentMessage

interface DynamoContentRepository {

    /** جلب رسائل مناسبة للمستخدم من Supabase وحفظها في Room */
    suspend fun syncMessages(audienceType: String, specialty: String)

    /** رسالة عشوائية من Room (أي رسالة) */
    suspend fun getRandomLocalMessage(): DynamoContentMessage?

    /** رسالة عشوائية من Room باستثناء قائمة ids محددة — استدعِ فقط مع ids غير فارغة */
    suspend fun getRandomLocalMessageExcluding(ids: List<String>): DynamoContentMessage?
}
