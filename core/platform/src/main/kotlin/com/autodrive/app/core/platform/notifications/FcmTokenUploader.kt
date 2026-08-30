package com.autodrive.app.core.platform.notifications

import android.content.Context
import com.autodrive.app.core.observability.AppLogger
import com.autodrive.app.core.platform.notifications.PushTokenRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Fire-and-forget رفع توكن FCM الحالي للسيرفر. يُستدعى بعد إكمال تسجيل الدخول/التسجيل. */
object FcmTokenUploader {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun trigger(context: Context, repository: PushTokenRepository) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                AppLogger.w("FcmTokenUploader","تعذّر جلب التوكن: ${task.exception?.message}")
                return@addOnCompleteListener
            }
            val token = task.result ?: return@addOnCompleteListener
            // Persist before upload so process death/server downtime cannot lose a refreshed token.
            FcmTokenStore.setPending(context, token)
            scope.launch {
                repository.upsertCurrentUserToken(token)
                    .onSuccess {
                        FcmTokenStore.setLastUploaded(context, token)
                        FcmTokenStore.setPending(context, null)
                    }
                    .onFailure {
                        FcmTokenStore.setPending(context, token)
                        AppLogger.w("FcmTokenUploader","فشل رفع التوكن: ${it.message}")
                    }
            }
        }
    }
}
