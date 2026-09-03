package com.autodrive.app.feature.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.feature.auth.domain.model.PhoneEntryResult
import com.autodrive.app.feature.auth.domain.repository.AuthRepository
import com.autodrive.app.feature.auth.domain.validation.SudanPhoneNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PhoneAuthState {
    data object Idle : PhoneAuthState()
    data object Loading : PhoneAuthState()
    data class OtpSent(val phone: String, val devOtp: String? = null) : PhoneAuthState()
    data class JoinCodeRequired(val phone: String) : PhoneAuthState()
    data object Verified : PhoneAuthState()
    data class Error(val message: String) : PhoneAuthState()
}

data class OtpUiState(
    val phoneNumber: String = "",
    val otp: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isVerified: Boolean = false
)

@HiltViewModel
class PhoneAuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<PhoneAuthState>(PhoneAuthState.Idle)
    val state: StateFlow<PhoneAuthState> = _state
    private val _otpState = MutableStateFlow(OtpUiState())
    val otpState: StateFlow<OtpUiState> = _otpState

    fun sendOtp(phone: String) {
        val normalizedPhone = SudanPhoneNumber.normalize(phone)
        if (normalizedPhone == null) {
            _state.value = PhoneAuthState.Error("أدخل رقم سوداني صحيح")
            return
        }
        viewModelScope.launch {
            _state.value = PhoneAuthState.Loading
            _state.value = when (val entry = authRepository.enterPhone(normalizedPhone)) {
                PhoneEntryResult.LoginOtp -> sendLoginOtp(normalizedPhone)
                PhoneEntryResult.JoinCodeRequired -> PhoneAuthState.JoinCodeRequired(normalizedPhone)
                PhoneEntryResult.AccountSelectionRequired -> PhoneAuthState.Error("يوجد أكثر من حساب مرتبط بالرقم — تواصل مع الإدارة")
                is PhoneEntryResult.Error -> PhoneAuthState.Error(entry.message)
            }
        }
    }

    private suspend fun sendLoginOtp(phone: String): PhoneAuthState = when (val result = authRepository.sendPhoneOtp(phone)) {
        is Result.Success -> PhoneAuthState.OtpSent(phone, result.data)
        is Result.Error -> PhoneAuthState.Error(result.message)
        is Result.Loading -> PhoneAuthState.Loading
    }

    fun initOtp(phoneNumber: String, devOtp: String? = null) {
        val normalizedPhone = SudanPhoneNumber.normalize(phoneNumber) ?: phoneNumber
        _otpState.value = OtpUiState(phoneNumber = normalizedPhone, otp = devOtp?.let(::sanitizeOtpInput).orEmpty())
    }

    fun onOtpChanged(value: String) {
        _otpState.value = _otpState.value.copy(otp = sanitizeOtpInput(value), errorMessage = null, infoMessage = null, isVerified = false)
    }

    fun verifyOtp() {
        val current = _otpState.value
        if (current.isLoading) return
        if (current.otp.length != 6) {
            _otpState.value = current.copy(errorMessage = if (current.otp.isBlank()) "أدخل رمز التحقق" else "رمز التحقق غير مكتمل")
            return
        }
        val normalizedPhone = SudanPhoneNumber.normalize(current.phoneNumber)
        if (normalizedPhone == null) {
            _otpState.value = current.copy(errorMessage = "حدث خطأ أثناء التحقق، حاول مرة أخرى")
            return
        }
        viewModelScope.launch {
            _otpState.value = current.copy(isLoading = true, errorMessage = null, infoMessage = null, isVerified = false)
            _otpState.value = when (val result = authRepository.verifyPhoneOtp(normalizedPhone, current.otp)) {
                is Result.Success -> _otpState.value.copy(isLoading = false, isVerified = true)
                is Result.Error -> _otpState.value.copy(otp = "", isLoading = false, errorMessage = mapOtpVerificationError(result.message), isVerified = false)
                is Result.Loading -> _otpState.value.copy(isLoading = true)
            }
        }
    }

    fun resetToIdle() { _state.value = PhoneAuthState.Idle }
    private fun sanitizeOtpInput(value: String): String = value.mapNotNull { it.toEnglishDigitOrNull() }.joinToString("").take(6)
    private fun Char.toEnglishDigitOrNull(): Char? = when (this) {
        in '0'..'9' -> this
        in '\u0660'..'\u0669' -> '0' + (code - '\u0660'.code)
        in '\u06F0'..'\u06F9' -> '0' + (code - '\u06F0'.code)
        else -> null
    }
    private fun mapOtpVerificationError(message: String): String =
        if (message.contains("timeout", true) || message.contains("network", true) || message.contains("internet", true) || message.contains("الاتصال"))
            "حدث خطأ أثناء التحقق، حاول مرة أخرى" else "رمز التحقق غير صحيح أو منتهي الصلاحية"
}
