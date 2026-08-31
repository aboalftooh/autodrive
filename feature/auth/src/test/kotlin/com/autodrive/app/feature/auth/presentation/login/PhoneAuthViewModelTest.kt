package com.autodrive.app.feature.auth.presentation.login

import com.autodrive.app.core.common.result.Result
import com.autodrive.app.feature.auth.domain.model.CodeVerificationResult
import com.autodrive.app.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhoneAuthViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAuthRepository
    private lateinit var viewModel: PhoneAuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAuthRepository()
        viewModel = PhoneAuthViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `invalid phone is rejected without network call`() = runTest(dispatcher) {
        viewModel.sendOtp("12345")

        assertTrue(viewModel.state.value is PhoneAuthState.Error)
        assertTrue(repository.sentPhones.isEmpty())
    }

    @Test
    fun `local Sudan phone is normalized before OTP send`() = runTest(dispatcher) {
        repository.sendResult = Result.Success(null)

        viewModel.sendOtp("0912345678")
        advanceUntilIdle()

        assertEquals(listOf("249912345678"), repository.sentPhones)
        val state = viewModel.state.value as PhoneAuthState.OtpSent
        assertEquals("249912345678", state.phone)
    }

    @Test
    fun `Arabic phone digits are normalized before OTP send`() = runTest(dispatcher) {
        viewModel.sendOtp("٠٩١٢٣٤٥٦٧٨")
        advanceUntilIdle()

        assertEquals(listOf("249912345678"), repository.sentPhones)
        assertTrue(viewModel.state.value is PhoneAuthState.OtpSent)
    }

    @Test
    fun `development OTP is sanitized and capped at six digits`() = runTest(dispatcher) {
        viewModel.initOtp("0912345678", "١٢٣٤٥٦٧٨")

        assertEquals("249912345678", viewModel.otpState.value.phoneNumber)
        assertEquals("123456", viewModel.otpState.value.otp)
    }

    @Test
    fun `OTP input accepts Arabic and Persian digits`() = runTest(dispatcher) {
        viewModel.initOtp("0912345678")
        viewModel.onOtpChanged("١۲3٤۵6")

        assertEquals("123456", viewModel.otpState.value.otp)
    }

    @Test
    fun `short OTP is rejected without verification call`() = runTest(dispatcher) {
        viewModel.initOtp("0912345678")
        viewModel.onOtpChanged("12345")
        viewModel.verifyOtp()

        assertEquals("رمز التحقق غير مكتمل", viewModel.otpState.value.errorMessage)
        assertTrue(repository.verifiedOtps.isEmpty())
    }

    @Test
    fun `valid OTP verification marks state verified`() = runTest(dispatcher) {
        repository.verifyResult = Result.Success(Unit)
        viewModel.initOtp("0912345678")
        viewModel.onOtpChanged("123456")

        viewModel.verifyOtp()
        advanceUntilIdle()

        assertEquals(listOf("249912345678" to "123456"), repository.verifiedOtps)
        assertTrue(viewModel.otpState.value.isVerified)
        assertFalse(viewModel.otpState.value.isLoading)
    }

    @Test
    fun `rejected OTP is cleared so stale code cannot remain`() = runTest(dispatcher) {
        repository.verifyResult = Result.Error("invalid otp")
        viewModel.initOtp("0912345678")
        viewModel.onOtpChanged("123456")

        viewModel.verifyOtp()
        advanceUntilIdle()

        assertEquals("", viewModel.otpState.value.otp)
        assertEquals("رمز التحقق غير صحيح أو منتهي الصلاحية", viewModel.otpState.value.errorMessage)
        assertFalse(viewModel.otpState.value.isVerified)
    }

    @Test
    fun `network verification failure uses retryable message and clears OTP`() = runTest(dispatcher) {
        repository.verifyResult = Result.Error("network timeout")
        viewModel.initOtp("0912345678")
        viewModel.onOtpChanged("123456")

        viewModel.verifyOtp()
        advanceUntilIdle()

        assertEquals("", viewModel.otpState.value.otp)
        assertEquals("حدث خطأ أثناء التحقق، حاول مرة أخرى", viewModel.otpState.value.errorMessage)
    }

    private class FakeAuthRepository : AuthRepository {
        var sendResult: Result<String?> = Result.Success(null)
        var verifyResult: Result<Unit> = Result.Success(Unit)
        val sentPhones = mutableListOf<String>()
        val verifiedOtps = mutableListOf<Pair<String, String>>()

        override suspend fun sendPhoneOtp(phone: String): Result<String?> {
            sentPhones += phone
            return sendResult
        }

        override suspend fun verifyPhoneOtp(phone: String, otp: String): Result<Unit> {
            verifiedOtps += phone to otp
            return verifyResult
        }

        override suspend fun verifyInviteCode(code: String): CodeVerificationResult =
            CodeVerificationResult.Invalid

        override suspend fun restoreSession(): Boolean = false
        override suspend fun signOut() = Unit
        override fun isLoggedIn(): Boolean = false
        override fun isRegistrationComplete(): Boolean = false
        override fun getCurrentUserId(): String = ""
    }
}
