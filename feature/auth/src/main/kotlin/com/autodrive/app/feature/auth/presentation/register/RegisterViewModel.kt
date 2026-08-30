package com.autodrive.app.feature.auth.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.core.sync.domain.SyncCoordinator
import com.autodrive.app.core.sync.domain.SyncReason
import com.autodrive.app.core.model.account.AccountType
import com.autodrive.app.core.model.account.AutoDriveUser
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.core.common.registration.RegistrationProfileWriter
import com.autodrive.app.core.session.domain.SessionReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registrationProfileWriter: RegistrationProfileWriter,
    private val syncCoordinator: SyncCoordinator,
    private val sessionReader: SessionReader,
    private val draftStore: RegistrationDraftStore
) : ViewModel() {

    private val _saved = MutableStateFlow<Result<Unit>?>(null)
    val saved: StateFlow<Result<Unit>?> = _saved

    var accountType: String = draftStore.accountType
    var fullName  = draftStore.fullName
    var phone     = draftStore.phone
    var bankName  = draftStore.bankName
    var bankAccount = draftStore.bankAccount
    var workshopName = draftStore.workshopName
    var specialty    = draftStore.specialty
    var workersCount = draftStore.workersCount
    var address      = draftStore.address

    val verifiedPhone: String
        get() = sessionReader.currentSession().phone.orEmpty()

    fun saveDraft() {
        draftStore.accountType = accountType
        draftStore.fullName = fullName.trim()
        draftStore.phone = phone.trim()
        draftStore.bankName = bankName.trim()
        draftStore.bankAccount = bankAccount.trim()
        draftStore.workshopName = workshopName.trim()
        draftStore.specialty = specialty.trim()
        draftStore.workersCount = workersCount.trim()
        draftStore.address = address.trim()
    }

    fun saveMarketer() {
        viewModelScope.launch {
            _saved.value = Result.Loading
            val session = sessionReader.currentSession()
            val uid = session.userId ?: run { _saved.value = Result.Error("بيانات الجلسة مفقودة — أعد إدخال كود الدعوة"); return@launch }
            val clientId = session.clientId ?: run { _saved.value = Result.Error("معرف العميل مفقود — أعد إدخال كود الدعوة"); return@launch }
            val orgId = session.orgId ?: run { _saved.value = Result.Error("معرف المنظمة مفقود — أعد إدخال كود الدعوة"); return@launch }
            val user = AutoDriveUser(
                id          = "",
                userId      = uid,
                clientId    = clientId,
                orgId       = orgId,
                accountType = AccountType.MARKETER,
                fullName    = fullName,
                phone       = phone,
                bankName    = bankName.ifBlank { null },
                bankAccount = bankAccount.ifBlank { null }
            )
            val result = registrationProfileWriter.saveRegisteredUser(user)
            _saved.value = result
            if (result is Result.Success) syncCoordinator.requestSync(SyncReason.LOGIN_SUCCESS)
        }
    }

    fun saveWorkshopOwner() {
        viewModelScope.launch {
            _saved.value = Result.Loading
            val session = sessionReader.currentSession()
            val uid = session.userId ?: run { _saved.value = Result.Error("بيانات الجلسة مفقودة — أعد إدخال كود الدعوة"); return@launch }
            val clientId = session.clientId ?: run { _saved.value = Result.Error("معرف العميل مفقود — أعد إدخال كود الدعوة"); return@launch }
            val orgId = session.orgId ?: run { _saved.value = Result.Error("معرف المنظمة مفقود — أعد إدخال كود الدعوة"); return@launch }
            val user = AutoDriveUser(
                id           = "",
                userId       = uid,
                clientId     = clientId,
                orgId        = orgId,
                accountType  = AccountType.WORKSHOP_OWNER,
                fullName     = fullName,
                phone        = phone,
                bankName     = bankName.ifBlank { null },
                bankAccount  = bankAccount.ifBlank { null },
                workshopName = workshopName.ifBlank { null },
                specialty    = specialty.ifBlank { null },
                workersCount = workersCount.toIntOrNull(),
                address      = address.ifBlank { null }
            )
            val result = registrationProfileWriter.saveRegisteredUser(user)
            _saved.value = result
            if (result is Result.Success) syncCoordinator.requestSync(SyncReason.LOGIN_SUCCESS)
        }
    }
}
