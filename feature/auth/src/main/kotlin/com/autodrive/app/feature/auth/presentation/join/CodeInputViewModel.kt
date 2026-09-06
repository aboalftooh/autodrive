package com.autodrive.app.feature.auth.presentation.join

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.feature.auth.domain.model.JoinCodeVerificationResult
import com.autodrive.app.feature.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class JoinCodeState {
    data object Idle : JoinCodeState()
    data object Loading : JoinCodeState()
    data class OtpReady(val phone: String, val devOtp: String? = null) : JoinCodeState()
    data class Error(val message: String) : JoinCodeState()
}

@HiltViewModel
class CodeInputViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionReader: SessionReader,
) : ViewModel() {
    private val _state = MutableStateFlow<JoinCodeState>(JoinCodeState.Idle)
    val state: StateFlow<JoinCodeState> = _state

    val phone: String get() = sessionReader.currentSession().phone.orEmpty()

    fun submit(rawCode: String) {
        val code = normalizeDigits(rawCode)
        if (code.length != 8) {
            _state.value = JoinCodeState.Error("أدخل كود الانضمام المكون من 8 أرقام")
            return
        }
        val currentPhone = phone
        if (currentPhone.isBlank()) {
            _state.value = JoinCodeState.Error("رقم الهاتف مفقود — ارجع وأدخله مجدداً")
            return
        }
        viewModelScope.launch {
            _state.value = JoinCodeState.Loading
            when (val verified = authRepository.verifyJoinCode(currentPhone, code)) {
                is JoinCodeVerificationResult.Valid -> {
                    _state.value = when (val otp = authRepository.sendPhoneOtp(currentPhone)) {
                        is Result.Success -> JoinCodeState.OtpReady(currentPhone, otp.data)
                        is Result.Error -> JoinCodeState.Error(otp.message)
                        is Result.Loading -> JoinCodeState.Loading
                    }
                }
                is JoinCodeVerificationResult.Invalid -> _state.value = JoinCodeState.Error(mapReason(verified.reason))
                is JoinCodeVerificationResult.Error -> _state.value = JoinCodeState.Error(verified.message)
            }
        }
    }

    fun reset() { _state.value = JoinCodeState.Idle }

    fun normalizeDigits(value: String): String = value.mapNotNull { ch ->
        when (ch) {
            in '0'..'9' -> ch
            in '\u0660'..'\u0669' -> '0' + (ch.code - '\u0660'.code)
            in '\u06F0'..'\u06F9' -> '0' + (ch.code - '\u06F0'.code)
            else -> null
        }
    }.joinToString("").take(8)

    private fun mapReason(reason: String): String = when (reason.uppercase()) {
        "EXPIRED" -> "كود الانضمام منتهي الصلاحية"
        "ALREADY_USED" -> "كود الانضمام مستخدم مسبقاً"
        "CLIENT_ALREADY_LINKED" -> "هذا الحساب مرتبط مسبقاً بأوتودرايف"
        "PHONE_ALREADY_REGISTERED" -> "رقم الهاتف مسجل مسبقاً — ارجع وسجل الدخول"
        else -> "كود الانضمام غير صحيح"
    }
}
