package com.autodrive.app.feature.auth.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.core.common.registration.RegistrationProfileWriter
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.core.model.account.AccountType
import com.autodrive.app.core.model.account.AutoDriveUser
import com.autodrive.app.core.session.domain.SessionReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RegistrationActionState {
    data object Idle : RegistrationActionState()
    data object Loading : RegistrationActionState()
    data object Completed : RegistrationActionState()
    data class Error(val message: String) : RegistrationActionState()
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val sessionReader: SessionReader,
    private val draftStore: RegistrationDraftStore,
    private val registrationProfileWriter: RegistrationProfileWriter,
) : ViewModel() {

    private val _action = MutableStateFlow<RegistrationActionState>(RegistrationActionState.Idle)
    val action: StateFlow<RegistrationActionState> = _action

    var accountType: String = sessionReader.currentSession().accountType
        ?.takeIf { it.isNotBlank() } ?: draftStore.accountType
    var fullName: String = draftStore.fullName.ifBlank {
        val session = sessionReader.currentSession()
        session.userName.orEmpty().takeUnless { it == session.phone }.orEmpty()
    }
    var bankName = draftStore.bankName
    var bankAccount = draftStore.bankAccount
    var workshopName = draftStore.workshopName
    var specialty = draftStore.specialty
    var workersCount = draftStore.workersCount
    var address = draftStore.address

    val registrationPhone: String
        get() = sessionReader.currentSession().phone.orEmpty().ifBlank { draftStore.phone }

    val isPhoneVerified: Boolean
        get() = sessionReader.currentSession().isLoggedIn

    fun saveDraft() {
        draftStore.accountType = accountType
        draftStore.fullName = fullName.trim()
        draftStore.phone = registrationPhone.trim()
        draftStore.bankName = bankName.trim()
        draftStore.bankAccount = bankAccount.trim()
        draftStore.workshopName = workshopName.trim()
        draftStore.specialty = specialty.trim()
        draftStore.workersCount = workersCount.trim()
        draftStore.address = address.trim()
    }

    fun submitOrComplete() {
        if (_action.value is RegistrationActionState.Loading) return
        saveDraft()
        viewModelScope.launch {
            _action.value = RegistrationActionState.Loading
            val session = sessionReader.currentSession()
            if (!session.isLoggedIn) {
                _action.value = RegistrationActionState.Error("يجب التحقق من رقم الهاتف أولاً")
                return@launch
            }

            val uid = session.userId ?: run {
                _action.value = RegistrationActionState.Error("بيانات الجلسة مفقودة")
                return@launch
            }
            val clientId = session.clientId ?: run {
                _action.value = RegistrationActionState.Error("معرف العميل مفقود")
                return@launch
            }
            val orgId = session.orgId ?: run {
                _action.value = RegistrationActionState.Error("معرف المؤسسة مفقود")
                return@launch
            }
            val phone = session.phone?.takeIf { it.isNotBlank() } ?: run {
                _action.value = RegistrationActionState.Error("رقم الهاتف الموثق مفقود")
                return@launch
            }
            val serverAccountType = session.accountType ?: accountType
            val user = AutoDriveUser(
                id = "",
                userId = uid,
                clientId = clientId,
                orgId = orgId,
                accountType = if (serverAccountType == "WORKSHOP_OWNER") AccountType.WORKSHOP_OWNER else AccountType.MARKETER,
                fullName = fullName.trim(),
                phone = phone,
                bankName = bankName.trim().ifBlank { null },
                bankAccount = bankAccount.trim().ifBlank { null },
                workshopName = workshopName.trim().ifBlank { null },
                specialty = specialty.trim().ifBlank { null },
                workersCount = workersCount.trim().toIntOrNull(),
                address = address.trim().ifBlank { null },
            )
            _action.value = when (val result = registrationProfileWriter.saveRegisteredUser(user)) {
                is Result.Success -> {
                    draftStore.clear()
                    RegistrationActionState.Completed
                }
                is Result.Error -> RegistrationActionState.Error(result.message)
                is Result.Loading -> RegistrationActionState.Loading
            }
        }
    }

    fun resetAction() {
        _action.value = RegistrationActionState.Idle
    }
}
