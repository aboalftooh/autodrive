package com.autodrive.app

import com.autodrive.app.R

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.autodrive.app.core.observability.AppLogger
import com.autodrive.app.feature.auth.data.sms.SmsHashLogger
import com.autodrive.app.core.observability.FirebaseCrashlyticsReporter
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.autodrive.app.core.platform.notifications.PushTokenRepository
import com.autodrive.app.core.sync.domain.SyncCoordinator
import com.autodrive.app.core.sync.worker.PendingOperationsWorker
import com.autodrive.app.feature.chat.data.worker.RetryFailedMessagesWorker
import com.autodrive.app.core.platform.notifications.FcmTokenStore
import com.autodrive.app.core.session.domain.SessionReader
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AutoDriveApp : Application(), Configuration.Provider {

    @Inject lateinit var syncCoordinator: SyncCoordinator
    @Inject lateinit var pushTokenRepository: PushTokenRepository
    @Inject lateinit var sessionReader: SessionReader
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        AppLogger.install(FirebaseCrashlyticsReporter())
        validateRuntimeConfiguration()
        SmsHashLogger.log(this)
        createNotificationChannel()
        syncCoordinator.start()
        syncFcmTokenIfLoggedIn()
        RetryFailedMessagesWorker.schedule(this)
        PendingOperationsWorker.schedule(this)
    }

    private fun syncFcmTokenIfLoggedIn() {
        val session = sessionReader.currentSession()
        if (!session.isLoggedIn) return
        if (session.clientId.isNullOrBlank() || session.orgId.isNullOrBlank()) return
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            val token = task.result ?: return@addOnCompleteListener
            val candidate = FcmTokenStore.pending(this) ?: token
            // Reconcile with the server on every authenticated process start. Local "uploaded"
            // state is not proof the server still owns the token (RPC/server migrations may change).
            FcmTokenStore.setPending(this, candidate)
            appScope.launch {
                pushTokenRepository.upsertCurrentUserToken(candidate)
                    .onSuccess {
                        FcmTokenStore.setLastUploaded(this@AutoDriveApp, candidate)
                        FcmTokenStore.setPending(this@AutoDriveApp, null)
                    }
                    .onFailure {
                        AppLogger.w("FcmTokenStartup", "تعذّر مزامنة توكن FCM: ${it.message}")
                    }
            }
        }
    }


    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notif_channel_desc)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun validateRuntimeConfiguration() {
        val missing = buildList {
            if (BuildConfig.SUPABASE_URL.isBlank()) add("SUPABASE_URL")
            if (BuildConfig.SUPABASE_ANON_KEY.isBlank()) add("SUPABASE_ANON_KEY")
            if (BuildConfig.ADMIN_WHATSAPP.isBlank()) add("ADMIN_WHATSAPP")
        }
        if (missing.isNotEmpty()) {
            AppLogger.e(
                tag = "RuntimeConfig",
                msg = "required_configuration_missing",
                fields = mapOf("missing_keys" to missing.joinToString(",")),
            )
        }
    }

    companion object {
        const val CHANNEL_ID = "autodrive_general"
    }
}
