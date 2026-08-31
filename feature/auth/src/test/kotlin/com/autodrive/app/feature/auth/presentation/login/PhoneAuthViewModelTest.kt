package com.autodrive.app.feature.auth.presentation.login

import com.autodrive.app.core.common.result.Result
import com.autodrive.app.core.session.domain.CurrentSession
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.feature.auth.domain.model.JoinRequestStatus
import com.autodrive.app.feature.auth.domain.model.PhoneEntryResult
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhoneAuthViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAuthRepository
    private lateinit var sessionReader: FakeSessionReader
    private lateinit var viewModel: PhoneAuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAuthRepository()
        sessionReader = FakeSessionReader()
        viewModel = PhoneAuthViewModel(repository, sessionReader)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `invalid phone is rejected before server entry`() = runTest(dispatcher) {
        viewModel.sendOtp("12345")

        assertTrue(viewModel.state.value is PhoneAuthState.Error)
        assertTrue(repository.enteredPhones.isEmpty())
    }

    @Test
    fun `active member phone is normalized then receives login OTP`() = runTest(dispatcher) {
        repository.entryResult = PhoneEntryResult.LoginOtp
        repository.sendResult = Result.Success(null)

        viewModel.sendOtp("0912345678")
        advanceUntilIdle()

        assertEquals(listOf("249912345678"), repository.enteredPhones)
        assertEquals(listOf("249912345678"), repository.sentPhones)
        val state = viewModel.state.value as PhoneAuthState.OtpSent
        assertEquals("249912345678", state.phone)
        assertNull(state.requestId)
    }

    @Test
    fun `Arabic phone digits are normalized before server entry`() = runTest(dispatcher) {
        repository.entryResult = PhoneEntryResult.NewRequest

        viewModel.sendOtp("٠٩١٢٣٤٥٦٧٨")
        advanceUntilIdle()

        assertEquals(listOf("249912345678"), repository.enteredPhones)
        assertTrue(viewModel.state.value is PhoneAuthState.RegistrationRequired)
        assertTrue(repository.sentPhones.isEmpty())
    }

    @Test
    fun `pending join request routes to waiting without sending OTP`() = runTest(dispatcher) {
        repository.entryResult = PhoneEntryResult.WaitApproval("req-1")

        viewModel.sendOtp("0912345678")
        advanceUntilIdle()

        val state = viewModel.state.value as PhoneAuthState.WaitingApproval
        assertEquals("req-1", state.requestId)
        assertTrue(repository.sentPhones.isEmpty())
        assertTrue(repository.approvedOtpRequests.isEmpty())
    }

    @Test
    fun `approved join request sends request scoped OTP`() = runTest(dispatcher) {
        repository.entryResult = PhoneEntryResult.ApprovedOtp("req-2")
        repository.approvedSendResult = Result.Success(Unit)

        viewModel.sendOtp("0912345678")
        advanceUntilIdle()

        assertEquals(listOf("249912345678" to "req-2"), repository.approvedOtpRequests)
        val state = viewModel.state.value as PhoneAuthState.OtpSent
        assertEquals("req-2", state.requestId)
    }

    @Test
    fun `account selection requirement fails closed`() = runTest(dispatcher) {
        repository.entryResult = PhoneEntryResult.AccountSelectionRequired

        viewModel.sendOtp("0912345678")
        advanceUntilIdle()

        assertTrue(viewModel.state.value is PhoneAuthState.Error)
        assertTrue(repository.sentPhones.isEmpty())
        assertTrue(repository.approvedOtpRequests.isEmpty())
    }

    @Test
    fun `development OTP is sanitized and capped at six digits`() = runTest(dispatcher) {
        viewModel.initOtp("0912345678", "١٢٣٤٥٦٧٨")

        assertEquals("249912345678", viewModel.otpState.value.phoneNumber)
        assertEquals("123456", viewModel.otpState.value.otp)
    }

    @Test
    fun `persisted join request is recovered by OTP screen`() = runTest(dispatcher) {
        sessionReader.session = CurrentSession(phone = "249912345678", pendingJoinRequestId = "req-persisted")

        viewModel.initOtp("0912345678")

        assertEquals("req-persisted", viewModel.otpState.value.requestId)
    }

    @Test
    fun `explicit request id wins over persisted request id`() = runTest(dispatcher) {
        sessionReader.session = CurrentSession(pendingJoinRequestId = "old")

        viewModel.initOtp("0912345678", requestId = "new")

        assertEquals("new", viewModel.otpState.value.requestId)
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
        assertTrue(repository.approvedVerifiedOtps.isEmpty())
    }

    @Test
    fun `active member OTP uses legacy member-only verifier`() = runTest(dispatcher) {
        repository.verifyResult = Result.Success(Unit)
        viewModel.initOtp("0912345678")
        viewModel.onOtpChanged("123456")

        viewModel.verifyOtp()
        advanceUntilIdle()

        assertEquals(listOf("249912345678" to "123456"), repository.verifiedOtps)
        assertTrue(repository.approvedVerifiedOtps.isEmpty())
        assertTrue(viewModel.otpState.value.isVerified)
        assertFalse(viewModel.otpState.value.isLoading)
    }

    @Test
    fun `approved request OTP uses request scoped verifier`() = runTest(dispatcher) {
        repository.approvedVerifyResult = Result.Success(Unit)
        viewModel.initOtp("0912345678", requestId = "req-3")
        viewModel.onOtpChanged("123456")

        viewModel.verifyOtp()
        advanceUntilIdle()

        assertEquals(listOf(Triple("249912345678", "123456", "req-3")), repository.approvedVerifiedOtps)
        assertTrue(repository.verifiedOtps.isEmpty())
        assertTrue(viewModel.otpState.value.isVerified)
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

    private class FakeSessionReader : SessionReader {
        var session: CurrentSession = CurrentSession()
        override fun currentSession(): CurrentSession = session
    }

    private class FakeAuthRepository : AuthRepository {
        var entryResult: PhoneEntryResult = PhoneEntryResult.LoginOtp
        var sendResult: Result<String?> = Result.Success(null)
        var verifyResult: Result<Unit> = Result.Success(Unit)
        var approvedSendResult: Result<Unit> = Result.Success(Unit)
        var approvedVerifyResult: Result<Unit> = Result.Success(Unit)
        var joinStatusResult: Result<JoinRequestStatus> = Result.Error("unused")
        var submitResult: Result<String> = Result.Success("req")

        val enteredPhones = mutableListOf<String>()
        val sentPhones = mutableListOf<String>()
        val verifiedOtps = mutableListOf<Pair<String, String>>()
        val approvedOtpRequests = mutableListOf<Pair<String, String>>()
        val approvedVerifiedOtps = mutableListOf<Triple<String, String, String>>()

        override suspend fun enterPhone(phone: String): PhoneEntryResult {
            enteredPhones += phone
            return entryResult
        }

        override suspend fun sendPhoneOtp(phone: String): Result<String?> {
            sentPhones += phone
            return sendResult
        }

        override suspend fun verifyPhoneOtp(phone: String, otp: String): Result<Unit> {
            verifiedOtps += phone to otp
            return verifyResult
        }

        override suspend fun submitJoinRequest(phone: String, fullName: String, accountType: String): Result<String> =
            submitResult

        override suspend fun getJoinRequestStatus(requestId: String): Result<JoinRequestStatus> = joinStatusResult

        override suspend fun sendApprovedPhoneOtp(phone: String, requestId: String): Result<Unit> {
            approvedOtpRequests += phone to requestId
            return approvedSendResult
        }

        override suspend fun verifyApprovedPhoneOtp(phone: String, otp: String, requestId: String): Result<Unit> {
            approvedVerifiedOtps += Triple(phone, otp, requestId)
            return approvedVerifyResult
        }

        override suspend fun restoreSession(): Boolean = false
        override suspend fun signOut() = Unit
        override fun isLoggedIn(): Boolean = false
        override fun isRegistrationComplete(): Boolean = false
        override fun getCurrentUserId(): String = ""
    }
}
