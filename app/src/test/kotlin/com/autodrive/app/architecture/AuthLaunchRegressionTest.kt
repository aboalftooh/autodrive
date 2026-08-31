package com.autodrive.app.architecture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthLaunchRegressionTest {

    @Test
    fun `registration identity always comes from entered or OTP verified session phone`() {
        val registerVm = ProjectLayout.source(
            "feature/auth/presentation/register/RegisterViewModel.kt"
        ).readText()
        val registerUi = ProjectLayout.source(
            "feature/auth/presentation/register/RegisterScreens.kt"
        ).readText()
        val profileRepository = ProjectLayout.source(
            "feature/profile/data/ProfileRepositoryImpl.kt"
        ).readText()

        assertTrue(registerVm.contains("draftStore.phone = registrationPhone.trim()"))
        assertTrue(registerUi.contains("readOnly = true"))
        assertTrue(profileRepository.contains("val verifiedPhone = session.phone"))
        assertTrue(profileRepository.contains("phone = null"))
        assertTrue(profileRepository.contains("phone = current.phone"))
    }

    @Test
    fun `phone entry is server driven and device bound`() {
        val repository = ProjectLayout.source(
            "feature/auth/data/AuthRepositoryImpl.kt"
        ).readText()
        val phoneVm = ProjectLayout.source(
            "feature/auth/presentation/login/PhoneAuthViewModel.kt"
        ).readText()

        assertTrue(repository.contains("function = \"autodrive-registration\""))
        assertTrue(repository.contains("put(\"action\", \"phone_entry\")"))
        assertTrue(repository.contains("put(\"device_id\", installationId.get())"))
        assertTrue(phoneVm.contains("PhoneEntryResult.LoginOtp"))
        assertTrue(phoneVm.contains("PhoneEntryResult.NewRequest"))
        assertTrue(phoneVm.contains("PhoneEntryResult.WaitApproval"))
        assertTrue(phoneVm.contains("PhoneEntryResult.ApprovedOtp"))
        assertTrue(phoneVm.contains("PhoneEntryResult.AccountSelectionRequired"))
    }

    @Test
    fun `transient registration lookup failure never downgrades cached session`() {
        val repository = ProjectLayout.source(
            "feature/auth/data/AuthRepositoryImpl.kt"
        ).readText()
        val refresh = repository.substringAfter(
            "private suspend fun refreshRegistrationStateFromSupabase(): Boolean"
        )

        assertTrue(refresh.contains("val linkedResult = runCatching"))
        assertTrue(refresh.contains("if (linkedResult.isFailure) return false"))
        assertTrue(repository.contains("refreshRegistrationStateFromSupabase()\n            Result.Success(Unit)"))
    }

    @Test
    fun `anonymous Supabase session is rejected`() {
        val repository = ProjectLayout.source(
            "feature/auth/data/AuthRepositoryImpl.kt"
        ).readText()

        assertTrue(repository.contains("user.email.isNullOrBlank() && user.phone.isNullOrBlank()"))
        assertTrue(repository.contains("return@runCatching false"))
    }

    @Test
    fun `sign out revokes account state before releasing logout barrier`() {
        val repository = ProjectLayout.source(
            "feature/auth/data/AuthRepositoryImpl.kt"
        ).readText()
        val signOut = repository.substringAfter("override suspend fun signOut()")
            .substringBefore("suspend fun syncPushToken")

        val deleteToken = signOut.indexOf("pushTokens.deleteCurrentUserToken()")
        val stopRealtime = signOut.indexOf("realtimeController.stop()")
        val beginBarrier = signOut.indexOf("syncManager.beginLogout")
        val clearSession = signOut.indexOf("sessionWriter.clearSession()")
        val clearLocal = signOut.indexOf("syncManager.quiesceAndClearForLogout")
        val remoteSignOut = signOut.indexOf("supabase.client.auth.signOut()")
        val releaseBarrier = signOut.indexOf("syncManager.releaseLogoutBarrier")

        listOf(deleteToken, stopRealtime, beginBarrier, clearSession, clearLocal, remoteSignOut, releaseBarrier)
            .forEach { assertTrue(it >= 0) }

        assertTrue(deleteToken < clearSession)
        assertTrue(stopRealtime < clearSession)
        assertTrue(beginBarrier < clearSession)
        assertTrue(clearSession < clearLocal)
        assertTrue(clearLocal < releaseBarrier)
        assertTrue(remoteSignOut < releaseBarrier)
    }

    @Test
    fun `legacy invite and workshop onboarding routes are absent`() {
        val destinations = ProjectLayout.source("navigation/AppDestinations.kt").readText()
        val graphs = ProjectLayout.source("navigation/NavigationGraphs.kt").readText()
        val registrationScreens = ProjectLayout.source(
            "feature/auth/presentation/register/RegisterScreens.kt"
        ).readText()

        assertFalse(destinations.contains("CodeInput"))
        assertFalse(graphs.contains("CodeInput"))
        assertFalse(destinations.contains("WorkshopInfo"))
        assertFalse(graphs.contains("WorkshopInfo"))
        assertFalse(registrationScreens.contains("fun WorkshopInfoScreen"))
    }

    @Test
    fun `pending approval survives restart and returns to waiting`() {
        val splash = ProjectLayout.source(
            "feature/auth/presentation/splash/SplashViewModel.kt"
        ).readText()
        val preferences = ProjectLayout.source(
            "core/session/data/PreferencesManager.kt"
        ).readText()

        assertTrue(splash.contains("pendingJoinRequestId"))
        assertTrue(splash.contains("SplashDestination.WAITING"))
        assertTrue(preferences.contains("KEY_PENDING_JOIN_REQUEST_ID"))
    }

    @Test
    fun `OTP remains six digits and debug OTP cannot leak to release`() {
        val phoneVm = ProjectLayout.source(
            "feature/auth/presentation/login/PhoneAuthViewModel.kt"
        ).readText()
        val otpScreen = ProjectLayout.source(
            "feature/auth/presentation/login/OtpInputScreen.kt"
        ).readText()

        assertTrue(phoneVm.contains("take(6)"))
        assertTrue(otpScreen.contains("devOtp.takeIf { BuildConfig.DEBUG }"))
        assertTrue(otpScreen.contains("state.otp.length == 6"))
    }

    @Test
    fun `SMS listener starts before both OTP network requests`() {
        val repository = ProjectLayout.source(
            "feature/auth/data/AuthRepositoryImpl.kt"
        ).readText()
        val legacySend = repository.indexOf("function = \"send-phone-otp\"")
        val approvedSend = repository.indexOf("function = \"autodrive-send-otp\"")
        val firstListener = repository.indexOf("SmsOtpAutofillCoordinator.startChallenge")
        val secondListener = repository.indexOf(
            "SmsOtpAutofillCoordinator.startChallenge",
            firstListener + 1,
        )

        assertTrue(firstListener >= 0 && legacySend > firstListener)
        assertTrue(secondListener >= 0 && approvedSend > secondListener)
    }
}
