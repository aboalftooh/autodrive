package com.autodrive.app.feature.auth.presentation.register

import com.autodrive.app.core.common.registration.RegistrationProfileWriter
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.core.model.account.AutoDriveUser
import com.autodrive.app.core.session.domain.CurrentSession
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.feature.auth.domain.model.JoinCodeVerificationResult
import com.autodrive.app.feature.auth.domain.model.PhoneEntryResult
import com.autodrive.app.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {
 private val dispatcher=StandardTestDispatcher(); @Before fun setup()=Dispatchers.setMain(dispatcher); @After fun close()=Dispatchers.resetMain()
 @Test fun `unauthenticated profile completion fails closed`()=runTest(dispatcher){val w=Writer();val vm=vm(CurrentSession(isLoggedIn=false,phone="249912345678"),w);vm.submitOrComplete();advanceUntilIdle();assertTrue(vm.action.value is RegistrationActionState.Error);assertTrue(w.saved.isEmpty())}
 @Test fun `verified membership completes profile without join request`()=runTest(dispatcher){val w=Writer();val vm=vm(CurrentSession(isLoggedIn=true,userId="u",clientId="c",orgId="o",accountType="MARKETER",phone="249912345678"),w).apply{fullName="Ahmed";bankName="Bank";bankAccount="123"};vm.submitOrComplete();advanceUntilIdle();assertEquals(1,w.saved.size);assertEquals("249912345678",w.saved.single().phone);assertTrue(vm.action.value is RegistrationActionState.Completed)}
 private fun vm(s:CurrentSession,w:Writer)=RegisterViewModel(object:SessionReader{override fun currentSession()=s},RegistrationDraftStore(),w)
 private class Writer:RegistrationProfileWriter{val saved=mutableListOf<AutoDriveUser>();override suspend fun saveRegisteredUser(user:AutoDriveUser):Result<Unit>{saved+=user;return Result.Success(Unit)}}
 private class Repo:AuthRepository{override suspend fun enterPhone(phone:String)=PhoneEntryResult.JoinCodeRequired;override suspend fun verifyJoinCode(phone:String,code:String)=JoinCodeVerificationResult.Error("unused");override suspend fun sendPhoneOtp(phone:String):Result<String?> = Result.Success(null);override suspend fun verifyPhoneOtp(phone:String,otp:String):Result<Unit> = Result.Success(Unit);override suspend fun restoreSession()=false;override suspend fun signOut()=Unit;override fun isLoggedIn()=false;override fun isRegistrationComplete()=false;override fun getCurrentUserId()=""}
}
