package com.autodrive.app.feature.auth.presentation.splash

import com.autodrive.app.core.common.result.Result
import com.autodrive.app.core.session.domain.CurrentSession
import com.autodrive.app.core.session.domain.RegistrationState
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.feature.auth.domain.model.CodeVerificationResult
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
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `missing Supabase session routes to phone input`() = runTest(dispatcher) {
        val repository = FakeAuthRepository(hasSession = false)
        val vm = SplashViewModel(
            restoreSession = RestoreSessionUseCase(repository),
            sessionReader = FixedSessionReader(CurrentSession()),
        )

        advanceUntilIdle()

        assertEquals(SplashDestination.PHONE_INPUT, vm.startDest.value)
    }

    @Test
    fun `restored complete session routes directly home`() = runTest(dispatcher) {
        val repository = FakeAuthRepository(hasSession = true)
        val vm = SplashViewModel(
            restoreSession = RestoreSessionUseCase(repository),
            sessionReader = FixedSessionReader(
                CurrentSession(
                    isLoggedIn = true,
                    registrationState = RegistrationState.COMPLETE,
                    userId = "user-1",
                    clientId = "client-1",
                    orgId = "org-1",
                    phone = "249912345678",
                )
            ),
        )

        advanceUntilIdle()

        assertEquals(SplashDestination.HOME, vm.startDest.value)
    }

    @Test
    fun `restored incomplete session routes to registration`() = runTest(dispatcher) {
        val repository = FakeAuthRepository(hasSession = true)
        val vm = SplashViewModel(
            restoreSession = RestoreSessionUseCase(repository),
            sessionReader = FixedSessionReader(
                CurrentSession(
                    isLoggedIn = true,
                    registrationState = RegistrationState.INCOMPLETE,
                    userId = "user-1",
                    phone = "249912345678",
                )
            ),
        )

        advanceUntilIdle()

        assertEquals(SplashDestination.REGISTRATION, vm.startDest.value)
    }

    private class FixedSessionReader(
        private val session: CurrentSession,
    ) : SessionReader {
        override fun currentSession(): CurrentSession = session
    }

    private class FakeAuthRepository(
        private val hasSession: Boolean,
    ) : AuthRepository {
        override suspend fun sendPhoneOtp(phone: String): Result<String?> = Result.Success(null)
        override suspend fun verifyPhoneOtp(phone: String, otp: String): Result<Unit> = Result.Success(Unit)
        override suspend fun verifyInviteCode(code: String): CodeVerificationResult = CodeVerificationResult.Invalid
        override suspend fun restoreSession(): Boolean = hasSession
        override suspend fun signOut() = Unit
        override fun isLoggedIn(): Boolean = hasSession
        override fun isRegistrationComplete(): Boolean = false
        override fun getCurrentUserId(): String = ""
    }
}
