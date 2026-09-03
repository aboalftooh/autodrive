package com.autodrive.app.feature.auth.presentation.login

import com.autodrive.app.core.common.result.Result
import com.autodrive.app.feature.auth.domain.model.JoinCodeVerificationResult
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
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhoneAuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAuthRepository
    private lateinit var viewModel: PhoneAuthViewModel
    @Before fun setUp(){ Dispatchers.setMain(dispatcher); repository=FakeAuthRepository(); viewModel=PhoneAuthViewModel(repository) }
    @After fun tearDown(){ Dispatchers.resetMain() }

    @Test fun `existing phone gets OTP`() = runTest(dispatcher) {
        repository.entryResult=PhoneEntryResult.LoginOtp
        viewModel.sendOtp("0912345678"); advanceUntilIdle()
        assertEquals(listOf("249912345678"),repository.sentPhones)
        assertTrue(viewModel.state.value is PhoneAuthState.OtpSent)
    }
    @Test fun `new phone requires join code and sends no OTP`() = runTest(dispatcher) {
        repository.entryResult=PhoneEntryResult.JoinCodeRequired
        viewModel.sendOtp("٠٩١٢٣٤٥٦٧٨"); advanceUntilIdle()
        val state=viewModel.state.value as PhoneAuthState.JoinCodeRequired
        assertEquals("249912345678",state.phone)
        assertTrue(repository.sentPhones.isEmpty())
    }
    @Test fun `six digit OTP uses one verifier for both flows`() = runTest(dispatcher) {
        viewModel.initOtp("0912345678"); viewModel.onOtpChanged("١٢٣٤٥٦"); viewModel.verifyOtp(); advanceUntilIdle()
        assertEquals(listOf("249912345678" to "123456"),repository.verifiedOtps)
        assertTrue(viewModel.otpState.value.isVerified)
    }
    @Test fun `short OTP fails locally`() = runTest(dispatcher) {
        viewModel.initOtp("0912345678"); viewModel.onOtpChanged("12345"); viewModel.verifyOtp()
        assertTrue(repository.verifiedOtps.isEmpty()); assertEquals("رمز التحقق غير مكتمل",viewModel.otpState.value.errorMessage)
    }
    private class FakeAuthRepository:AuthRepository{
        var entryResult:PhoneEntryResult=PhoneEntryResult.LoginOtp
        val sentPhones=mutableListOf<String>(); val verifiedOtps=mutableListOf<Pair<String,String>>()
        override suspend fun enterPhone(phone:String)=entryResult
        override suspend fun verifyJoinCode(phone:String,code:String)=JoinCodeVerificationResult.Error("unused")
        override suspend fun sendPhoneOtp(phone:String):Result<String?>{sentPhones+=phone;return Result.Success(null)}
        override suspend fun verifyPhoneOtp(phone:String,otp:String):Result<Unit>{verifiedOtps+=phone to otp;return Result.Success(Unit)}
        override suspend fun restoreSession()=false; override suspend fun signOut()=Unit
        override fun isLoggedIn()=false; override fun isRegistrationComplete()=false; override fun getCurrentUserId()=""
    }
}
