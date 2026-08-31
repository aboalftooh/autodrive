package com.autodrive.app.feature.auth.presentation.join

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WaitingViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `pending request remains waiting without sending OTP`() = runTest(dispatcher) {
        val repository = FakeAuthRepository().apply {
            statusResult = Result.Success(JoinRequestStatus("req-1", "PENDING"))
        }
        val vm = WaitingViewModel(repository, sessionReader("req-1"))

        advanceUntilIdle()

        assertTrue(vm.state.value is WaitingState.Pending)
        assertTrue(repository.sentApprovedOtp.isEmpty())
    }

    @Test
    fun `approved request sends scoped OTP and becomes ready`() = runTest(dispatcher) {
        val repository = FakeAuthRepository().apply {
            statusResult = Result.Success(JoinRequestStatus("req-1", "APPROVED"))
            sendApprovedResult = Result.Success(Unit)
        }
        val vm = WaitingViewModel(repository, sessionReader("req-1"))

        advanceUntilIdle()

        assertEquals(listOf("249912345678" to "req-1"), repository.sentApprovedOtp)
        val state = vm.state.value as WaitingState.OtpReady
        assertEquals("249912345678", state.phone)
        assertEquals("req-1", state.requestId)
    }

    @Test
    fun `rejected request never sends OTP`() = runTest(dispatcher) {
        val repository = FakeAuthRepository().apply {
            statusResult = Result.Success(
                JoinRequestStatus("req-1", "REJECTED", rejectionReason = "رفض الإدارة")
            )
        }
        val vm = WaitingViewModel(repository, sessionReader("req-1"))

        advanceUntilIdle()

        val state = vm.state.value as WaitingState.Rejected
        assertEquals("رفض الإدارة", state.message)
        assertTrue(repository.sentApprovedOtp.isEmpty())
    }

    @Test
    fun `missing persisted request fails closed`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val vm = WaitingViewModel(repository, FixedSessionReader(CurrentSession(phone = "249912345678")))

        advanceUntilIdle()

        assertTrue(vm.state.value is WaitingState.Error)
        assertEquals(0, repository.statusCalls)
    }

    @Test
    fun `status network error remains retryable error`() = runTest(dispatcher) {
        val repository = FakeAuthRepository().apply {
            statusResult = Result.Error("network")
        }
        val vm = WaitingViewModel(repository, sessionReader("req-1"))

        advanceUntilIdle()

        assertEquals("network", (vm.state.value as WaitingState.Error).message)
    }

    private fun sessionReader(requestId: String) = FixedSessionReader(
        CurrentSession(phone = "249912345678", pendingJoinRequestId = requestId)
    )

    private class FixedSessionReader(private val session: CurrentSession) : SessionReader {
        override fun currentSession(): CurrentSession = session
    }

    private class FakeAuthRepository : AuthRepository {
        var statusResult: Result<JoinRequestStatus> = Result.Success(JoinRequestStatus("req", "PENDING"))
        var sendApprovedResult: Result<Unit> = Result.Success(Unit)
        var statusCalls = 0
        val sentApprovedOtp = mutableListOf<Pair<String, String>>()

        override suspend fun enterPhone(phone: String): PhoneEntryResult = PhoneEntryResult.NewRequest
        override suspend fun sendPhoneOtp(phone: String): Result<String?> = Result.Success(null)
        override suspend fun verifyPhoneOtp(phone: String, otp: String): Result<Unit> = Result.Success(Unit)
        override suspend fun submitJoinRequest(phone: String, fullName: String, accountType: String): Result<String> = Result.Success("request")
        override suspend fun getJoinRequestStatus(requestId: String): Result<JoinRequestStatus> {
            statusCalls++
            return statusResult
        }
        override suspend fun sendApprovedPhoneOtp(phone: String, requestId: String): Result<Unit> {
            sentApprovedOtp += phone to requestId
            return sendApprovedResult
        }
        override suspend fun verifyApprovedPhoneOtp(phone: String, otp: String, requestId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun restoreSession(): Boolean = false
        override suspend fun signOut() = Unit
        override fun isLoggedIn(): Boolean = false
        override fun isRegistrationComplete(): Boolean = false
        override fun getCurrentUserId(): String = ""
    }
}
