package com.autodrive.app.core.platform.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalNotificationPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun publishChatMessage(conversationId: String, title: String, body: String) {
        val route = "chat/$conversationId?title=${URLEncoder.encode(title, "UTF-8")}"
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            putExtra(AutoDriveNotificationConstants.EXTRA_NAV_ROUTE, route)
            flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        } ?: return
        val pendingIntent = PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify("chat", conversationId.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "autodrive_general"
    }
}
