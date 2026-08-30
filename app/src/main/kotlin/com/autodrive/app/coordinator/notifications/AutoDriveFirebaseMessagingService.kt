package com.autodrive.app.coordinator.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.autodrive.app.core.observability.AppLogger
import androidx.core.app.NotificationCompat
import com.autodrive.app.AutoDriveApp
import com.autodrive.app.MainActivity
import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.platform.notifications.AutoDriveNotificationConstants
import com.autodrive.app.core.platform.notifications.FcmTokenStore
import com.autodrive.app.core.platform.notifications.PushTokenRepository
import com.autodrive.app.core.sync.domain.SyncCoordinator
import com.autodrive.app.core.sync.domain.SyncReason
import com.autodrive.app.feature.commission.domain.CommissionCalculator
import com.autodrive.app.feature.commission.domain.repository.CommissionRepository
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.model.money.Money
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class AutoDriveFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var pushTokenRepository: PushTokenRepository
    @Inject lateinit var syncCoordinator: SyncCoordinator
    @Inject lateinit var db: AutoDriveDatabase
    @Inject lateinit var sessionReader: SessionReader
    @Inject lateinit var calculator: CommissionCalculator
    @Inject lateinit var commissionRepository: CommissionRepository

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

        if (type in FINANCE_NOTIFICATION_TYPES) {
            // مزامنة أولاً ثم عرض الإشعار بأرقام محلية (مصدر الحقيقة = Room)
            scope.launch {
                runCatching { syncCoordinator.requestSync(SyncReason.FCM_HINT) }
                val freshBody = buildFreshFinanceBody(body, type)
                showNotification(title, freshBody, navRoute)
            }
        } else {
            showNotification(title, body, navRoute)
        }
    }

    // يحسب body الإشعار من السيرفر (commission_eligibility view) بعد المزامنة
    private suspend fun buildFreshFinanceBody(fallback: String, type: String?): String {
        val session = sessionReader.currentSession()
        val clientId = session.clientId ?: return fallback
        val userId = session.userId ?: return fallback
        return runCatching {
            val entries = commissionRepository.getEligibilities(clientId)
            val summary = calculator.summarize(entries)

            when (type) {
                "COMMISSION_WITHDRAWABLE", "WEEK_ENDING_SOON" ->
                    "لديك ${fmtAmount(summary.withdrawable)} جاهزة للسحب"
                "NEW_COMMISSION" ->
                    "عمولة جديدة — المعلقة: ${fmtAmount(summary.pending)}"
                "COMMISSION_PAID", "BALANCE_CREDITED" -> {
                    val balance = db.marketerBalanceDao().get(userId)?.balance ?: BigDecimal.ZERO
                    "رصيدك الحالي: ${fmtAmount(balance)}"
                }
                "WITHDRAWAL_APPROVED", "WITHDRAWAL_REJECTED", "WITHDRAWAL_COMPLETED" -> {
                    val balance = db.marketerBalanceDao().get(userId)?.balance ?: BigDecimal.ZERO
                    "تم تحديث طلب السحب — رصيدك: ${fmtAmount(balance)}"
                }
                else -> fallback
            }
        }.getOrElse { fallback }
    }

    private fun fmtAmount(amount: Money): String = fmtAmount(amount.amount)

    private fun fmtAmount(amount: BigDecimal): String =
        String.format(Locale.US, "%,d", amount.max(BigDecimal.ZERO).toLong())

    private fun showNotification(title: String, body: String, navRoute: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (navRoute != null) {
                putExtra(AutoDriveNotificationConstants.EXTRA_NAV_ROUTE, navRoute)
            }
        }
        val pi = PendingIntent.getActivity(
            this, 0, intent,
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
        val FINANCE_NOTIFICATION_TYPES = setOf(
            "COMMISSION_PAID", "COMMISSION_WITHDRAWABLE", "BALANCE_CREDITED",
            "WITHDRAWAL_APPROVED", "WITHDRAWAL_REJECTED", "WITHDRAWAL_COMPLETED",
            "NEW_COMMISSION", "WEEK_ENDING_SOON"
        )
    }
}
