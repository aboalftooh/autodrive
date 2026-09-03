package com.autodrive.app.feature.auth.presentation.join

import com.autodrive.app.core.common.result.Result
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
class CodeInputViewModelTest {
 private val dispatcher=StandardTestDispatcher(); private lateinit var repo:FakeRepo; private lateinit var vm:CodeInputViewModel
 @Before fun setup(){Dispatchers.setMain(dispatcher);repo=FakeRepo();vm=CodeInputViewModel(repo,object:SessionReader{override fun currentSession()=CurrentSession(phone="249912345678")})}
 @After fun close(){Dispatchers.resetMain()}
 @Test fun `requires exactly eight digits`()=runTest(dispatcher){vm.submit("123");assertTrue(vm.state.value is JoinCodeState.Error);assertTrue(repo.codes.isEmpty())}
 @Test fun `valid code is verified before OTP is sent`()=runTest(dispatcher){repo.codeResult=JoinCodeVerificationResult.Valid("c","o","MARKETER");vm.submit("١٢٣٤٥٦٧٨");advanceUntilIdle();assertEquals(listOf("249912345678" to "12345678"),repo.codes);assertEquals(listOf("249912345678"),repo.sent);assertTrue(vm.state.value is JoinCodeState.OtpReady)}
 private class FakeRepo:AuthRepository{var codeResult:JoinCodeVerificationResult=JoinCodeVerificationResult.Invalid("NOT_FOUND");val codes=mutableListOf<Pair<String,String>>();val sent=mutableListOf<String>();override suspend fun enterPhone(phone:String)=PhoneEntryResult.JoinCodeRequired;override suspend fun verifyJoinCode(phone:String,code:String):JoinCodeVerificationResult{codes+=phone to code;return codeResult};override suspend fun sendPhoneOtp(phone:String):Result<String?>{sent+=phone;return Result.Success(null)};override suspend fun verifyPhoneOtp(phone:String,otp:String):Result<Unit> = Result.Success(Unit);override suspend fun restoreSession()=false;override suspend fun signOut()=Unit;override fun isLoggedIn()=false;override fun isRegistrationComplete()=false;override fun getCurrentUserId()=""}
}
