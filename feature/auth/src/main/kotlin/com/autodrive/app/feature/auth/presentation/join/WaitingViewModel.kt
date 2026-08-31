package com.autodrive.app.feature.auth.presentation.join

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.feature.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class WaitingState {
    data object Loading : WaitingState()
    data object Pending : WaitingState()
    data class OtpReady(val phone: String, val requestId: String) : WaitingState()
    data class Rejected(val message: String) : WaitingState()
    data class Error(val message: String) : WaitingState()
}

@HiltViewModel
class WaitingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionReader: SessionReader,
) : ViewModel() {
    private val _state = MutableStateFlow<WaitingState>(WaitingState.Loading)
    val state: StateFlow<WaitingState> = _state

    init {
        refresh()
    }

    fun refresh() {
        if (_state.value is WaitingState.OtpReady) return
        val session = sessionReader.currentSession()
        val requestId = session.pendingJoinRequestId
        val phone = session.phone.orEmpty()
        if (requestId.isNullOrBlank() || phone.isBlank()) {
            _state.value = WaitingState.Error("بيانات طلب الانضمام مفقودة — أعد إدخال رقم الهاتف")
            return
        }

        viewModelScope.launch {
            if (_state.value !is WaitingState.Pending) _state.value = WaitingState.Loading
            when (val result = authRepository.getJoinRequestStatus(requestId)) {
                is Result.Success -> {
                    val status = result.data
                    when {
                        status.isPending -> _state.value = WaitingState.Pending
                        status.isApproved -> {
                            when (val sent = authRepository.sendApprovedPhoneOtp(phone, requestId)) {
                                is Result.Success -> _state.value = WaitingState.OtpReady(phone, requestId)
                                is Result.Error -> _state.value = WaitingState.Error(sent.message)
                                is Result.Loading -> _state.value = WaitingState.Loading
                            }
                        }
                        status.isRejected -> _state.value = WaitingState.Rejected(
                            status.rejectionReason ?: "تم رفض أو انتهاء طلب الانضمام"
                        )
                        else -> _state.value = WaitingState.Error("حالة الطلب غير معروفة: ${status.status}")
                    }
                }
                is Result.Error -> _state.value = WaitingState.Error(result.message)
                is Result.Loading -> _state.value = WaitingState.Loading
            }
        }
    }
}
