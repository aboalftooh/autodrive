package com.autodrive.app.architecture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationP1RegressionTest {
    private val root = File(System.getProperty("user.dir"))

    @Test
    fun notificationP1ContractsRemainClosed() {
        val service = root.resolve("app/src/main/kotlin/com/autodrive/app/coordinator/notifications/AutoDriveFirebaseMessagingService.kt").readText()
        val pushRepo = root.resolve("core/platform/src/main/kotlin/com/autodrive/app/core/platform/notifications/PushTokenRepository.kt").readText()
        val authRepo = root.resolve("feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/data/AuthRepositoryImpl.kt").readText()

        assertTrue(service.contains("val requestCode = notificationKey.hashCode()"))
        assertFalse(service.contains("FINANCE_NOTIFICATION_TYPES"))
        assertTrue(service.contains("server payload is authoritative", ignoreCase = true))

        assertTrue(pushRepo.contains("autodrive_revoke_push_token_command_v2"))
        assertTrue(pushRepo.contains("FirebaseMessaging.getInstance().deleteToken()"))
        assertTrue(authRepo.contains("pushTokens.deleteCurrentUserToken(pushToken)"))
        assertTrue(authRepo.contains("FcmTokenStore.setLastUploaded(appContext, null)"))
    }
}
