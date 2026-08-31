package com.autodrive.app.feature.auth.presentation.splash

import com.autodrive.app.core.common.result.Result
import com.autodrive.app.core.session.domain.CurrentSession
import com.autodrive.app.core.session.domain.RegistrationState
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.feature.auth.domain.model.JoinRequestStatus
import com.autodrive.app.feature.auth.domain.model.PhoneEntryResult
import com.autodrive.app.feature.auth.domain.repository.AuthRepository
import com.autodrive.app.feature.auth.domain.usecase.RestoreSessionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `missing session and join request routes to phone input`() = runTest(dispatcher) {
        val vm = createVm(false, CurrentSession())
        advanceUntilIdle()
        assertEquals(SplashDestination.PHONE_INPUT, vm.startDest.value)
    }

    @Test
    fun `pending join request survives process restart and routes to waiting`() = runTest(dispatcher) {
        val vm = createVm(
            false,
            CurrentSession(phone = "249912345678", pendingJoinRequestId = "request-1")
        )
        advanceUntilIdle()
        assertEquals(SplashDestination.WAITING, vm.startDest.value)
    }

    @Test
    fun `request id without phone does not enter waiting`() = runTest(dispatcher) {
        val vm = createVm(false, CurrentSession(pendingJoinRequestId = "request-1"))
        advanceUntilIdle()
        assertEquals(SplashDestination.PHONE_INPUT, vm.startDest.value)
    }

    @Test
    fun `restored complete session routes directly home`() = runTest(dispatcher) {
        val vm = createVm(
            true,
            CurrentSession(
                isLoggedIn = true,
                registrationState = RegistrationState.COMPLETE,
                userId = "user-1",
                clientId = "client-1",
                orgId = "org-1",
                phone = "249912345678",
            )
        )
        advanceUntilIdle()
        assertEquals(SplashDestination.HOME, vm.startDest.value)
    }

    @Test
    fun `restored incomplete session routes to onboarding`() = runTest(dispatcher) {
        val vm = createVm(
            true,
            CurrentSession(
                isLoggedIn = true,
                registrationState = RegistrationState.INCOMPLETE,
                userId = "user-1",
                clientId = "client-1",
                orgId = "org-1",
                accountType = "MARKETER",
                phone = "249912345678",
            )
        )
        advanceUntilIdle()
        assertEquals(SplashDestination.REGISTRATION, vm.startDest.value)
    }

    private fun createVm(hasSession: Boolean, session: CurrentSession) = SplashViewModel(
        restoreSession = RestoreSessionUseCase(FakeAuthRepository(hasSession)),
        sessionReader = FixedSessionReader(session),
    )

    private class FixedSessionReader(private val session: CurrentSession) : SessionReader {
        override fun currentSession(): CurrentSession = session
    }

    private class FakeAuthRepository(private val hasSession: Boolean) : AuthRepository {
        override suspend fun enterPhone(phone: String): PhoneEntryResult = PhoneEntryResult.NewRequest
        override suspend fun sendPhoneOtp(phone: String): Result<String?> = Result.Success(null)
        override suspend fun verifyPhoneOtp(phone: String, otp: String): Result<Unit> = Result.Success(Unit)
        override suspend fun submitJoinRequest(phone: String, fullName: String, accountType: String): Result<String> = Result.Success("request")
        override suspend fun getJoinRequestStatus(requestId: String): Result<JoinRequestStatus> = Result.Error("unused")
        override suspend fun sendApprovedPhoneOtp(phone: String, requestId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun verifyApprovedPhoneOtp(phone: String, otp: String, requestId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun restoreSession(): Boolean = hasSession
        override suspend fun signOut() = Unit
        override fun isLoggedIn(): Boolean = hasSession
        override fun isRegistrationComplete(): Boolean = false
        override fun getCurrentUserId(): String = ""
    }
}
