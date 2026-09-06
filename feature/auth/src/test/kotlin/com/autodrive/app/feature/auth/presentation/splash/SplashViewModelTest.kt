package com.autodrive.app.feature.auth.presentation.splash

import com.autodrive.app.core.common.result.Result
import com.autodrive.app.core.session.domain.*
import com.autodrive.app.feature.auth.domain.model.JoinCodeVerificationResult
import com.autodrive.app.feature.auth.domain.model.PhoneEntryResult
import com.autodrive.app.feature.auth.domain.repository.AuthRepository
import com.autodrive.app.feature.auth.domain.usecase.RestoreSessionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {
 private val dispatcher=StandardTestDispatcher(); @Before fun setup()=Dispatchers.setMain(dispatcher); @After fun close()=Dispatchers.resetMain()
 @Test fun `no auth session always starts at phone even with cached invite code`()=runTest(dispatcher){val vm=create(false,CurrentSession(phone="249912345678",pendingInviteCode="12345678"));advanceUntilIdle();assertEquals(SplashDestination.PHONE_INPUT,vm.startDest.value)}
 @Test fun `complete session opens home`()=runTest(dispatcher){val vm=create(true,CurrentSession(isLoggedIn=true,registrationState=RegistrationState.COMPLETE));advanceUntilIdle();assertEquals(SplashDestination.HOME,vm.startDest.value)}
 @Test fun `incomplete authenticated session resumes profile completion`()=runTest(dispatcher){val vm=create(true,CurrentSession(isLoggedIn=true,registrationState=RegistrationState.INCOMPLETE));advanceUntilIdle();assertEquals(SplashDestination.REGISTRATION,vm.startDest.value)}
 private fun create(has:Boolean,s:CurrentSession)=SplashViewModel(RestoreSessionUseCase(FakeRepo(has)),object:SessionReader{override fun currentSession()=s})
 private class FakeRepo(private val has:Boolean):AuthRepository{override suspend fun enterPhone(phone:String)=PhoneEntryResult.JoinCodeRequired;override suspend fun verifyJoinCode(phone:String,code:String)=JoinCodeVerificationResult.Error("unused");override suspend fun sendPhoneOtp(phone:String):Result<String?> = Result.Success(null);override suspend fun verifyPhoneOtp(phone:String,otp:String):Result<Unit> = Result.Success(Unit);override suspend fun restoreSession()=has;override suspend fun signOut()=Unit;override fun isLoggedIn()=has;override fun isRegistrationComplete()=false;override fun getCurrentUserId()=""}
}
