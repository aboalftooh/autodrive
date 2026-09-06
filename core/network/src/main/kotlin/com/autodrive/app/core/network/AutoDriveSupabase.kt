package com.autodrive.app.core.network

import com.autodrive.app.core.network.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.serializer.KotlinXSerializer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json

/**
 * AutoDrive يستخدم anon key فقط — خاضع لـ RLS بالكامل.
 * لا تضع service_role key هنا أبداً.
 */
@Singleton
class AutoDriveSupabase @Inject constructor() {

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl    = BuildConfig.SUPABASE_URL,
        supabaseKey    = BuildConfig.SUPABASE_ANON_KEY
    ) {
        requestTimeout = 30.seconds
        defaultSerializer = KotlinXSerializer(Json {
            coerceInputValues = true
            ignoreUnknownKeys = true
        })
        install(Postgrest)
        install(Auth)
        install(Realtime)
        install(Storage)
        install(Functions)
    }
}
