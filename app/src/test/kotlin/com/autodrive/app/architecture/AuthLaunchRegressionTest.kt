package com.autodrive.app.architecture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthLaunchRegressionTest {

    @Test
    fun `registration persists only OTP verified phone`() {
        val registerVm = ProjectLayout.source(
            "feature/auth/presentation/register/RegisterViewModel.kt"
        ).readText()
        val registerUi = ProjectLayout.source(
            "feature/auth/presentation/register/RegisterScreens.kt"
        ).readText()
        val codeVm = ProjectLayout.source(
            "feature/auth/presentation/join/CodeInputViewModel.kt"
        ).readText()

        assertTrue(registerVm.contains("draftStore.phone = verifiedPhone.trim()"))
        assertTrue(registerUi.contains("val phone = viewModel.verifiedPhone"))
        assertTrue(registerUi.contains("readOnly = true"))
        assertTrue(codeVm.contains("phone = verifiedPhone"))
        assertFalse(codeVm.contains("draftStore.phone.ifBlank"))
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
        assertTrue(repository.contains("refreshRegistrationStateFromSupabase()\n            true"))
    }

    @Test
    fun `existing user lookup failure is not interpreted as new user`() {
        val repository = ProjectLayout.source(
            "feature/auth/data/AuthRepositoryImpl.kt"
        ).readText()

        assertTrue(repository.contains("val existingUserResult = runCatching"))
        assertTrue(repository.contains("if (existingUserResult.isFailure)"))
        assertTrue(repository.contains("تعذّر التحقق من حالة الحساب"))
    }

    @Test
    fun `legacy workshop onboarding route is fully removed`() {
        val destinations = ProjectLayout.source("navigation/AppDestinations.kt").readText()
        val graphs = ProjectLayout.source("navigation/NavigationGraphs.kt").readText()
        val registrationScreens = ProjectLayout.source(
            "feature/auth/presentation/register/RegisterScreens.kt"
        ).readText()

        assertFalse(destinations.contains("WorkshopInfo"))
        assertFalse(graphs.contains("WorkshopInfo"))
        assertFalse(registrationScreens.contains("fun WorkshopInfoScreen"))
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
    fun `SMS listener starts before OTP network request`() {
        val repository = ProjectLayout.source(
            "feature/auth/data/AuthRepositoryImpl.kt"
        ).readText()
        val listenerIndex = repository.indexOf("SmsOtpAutofillCoordinator.startChallenge")
        val requestIndex = repository.indexOf("function = \"send-phone-otp\"")

        assertTrue(listenerIndex >= 0)
        assertTrue(requestIndex > listenerIndex)
    }
}
