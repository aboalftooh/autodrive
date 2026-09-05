package com.autodrive.app.coordinator.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.autodrive.app.core.observability.AppLogger
import androidx.core.app.NotificationCompat
import com.autodrive.app.AutoDriveApp
import com.autodrive.app.MainActivity
import com.autodrive.app.core.platform.notifications.AutoDriveNotificationConstants
import com.autodrive.app.core.platform.notifications.FcmTokenStore
import com.autodrive.app.core.platform.notifications.PushTokenRepository
import com.autodrive.app.core.sync.domain.SyncCoordinator
import com.autodrive.app.core.sync.domain.SyncReason
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AutoDriveFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var pushTokenRepository: PushTokenRepository
    @Inject lateinit var syncCoordinator: SyncCoordinator

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FcmTokenStore.setPending(applicationContext, token)
        scope.launch {
            pushTokenRepository.upsertCurrentUserToken(token)
                .onSuccess {
                    FcmTokenStore.setLastUploaded(applicationContext, token)
                    FcmTokenStore.setPending(applicationContext, null)
                }
                .onFailure { AppLogger.w(TAG, "تعذّر رفع توكن FCM (سيُعاد عند التشغيل التالي): ${it.message}") }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val notif = message.notification
        val type  = data[AutoDriveNotificationConstants.DATA_TYPE]
        val title = if (type == "NEW_CHAT_MESSAGE") {
            data["conversation_title"] ?: data["subject"] ?: notif?.title ?: data["title"] ?: "رسالة جديدة"
        } else {
            notif?.title ?: data["title"] ?: "إشعار"
        }
        val body  = notif?.body  ?: data["body"]  ?: ""
        val navRoute = data[AutoDriveNotificationConstants.DATA_NAV_ROUTE]
            ?: AutoDriveNotificationConstants.routeForType(type)

        val notificationKey = message.messageId
            ?: data["notification_id"]
            ?: data["request_id"]
            ?: "${type.orEmpty()}:${navRoute.orEmpty()}:${title}:${body}"

        // The server payload is authoritative for notification content in every app state.
        // Foreground and background must therefore show the same title/body; sync is only a data hint.
        scope.launch { runCatching { syncCoordinator.requestSync(SyncReason.FCM_HINT) } }
        showNotification(title, body, navRoute, notificationKey)
    }

    private fun showNotification(title: String, body: String, navRoute: String?, notificationKey: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (navRoute != null) {
                putExtra(AutoDriveNotificationConstants.EXTRA_NAV_ROUTE, navRoute)
            }
        }
        // A distinct requestCode keeps each notification's deep-link isolated from later notifications.
        val requestCode = notificationKey.hashCode()
        val pi = PendingIntent.getActivity(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(this, AutoDriveApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(System.currentTimeMillis().toInt(), n)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.coroutineContext[Job]?.cancel()
    }

    private companion object {
        const val TAG = "AutoDriveFcmService"
    }
}
