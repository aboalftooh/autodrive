package com.autodrive.app.feature.auth.presentation.join

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.core.common.registration.RegistrationProfileWriter
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.core.model.account.AccountType
import com.autodrive.app.core.model.account.AutoDriveUser
import com.autodrive.app.core.platform.notifications.FcmTokenUploader
import com.autodrive.app.core.platform.notifications.PushTokenRepository
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.sync.domain.SyncCoordinator
import com.autodrive.app.core.sync.domain.SyncReason
import com.autodrive.app.feature.auth.domain.model.CodeVerificationResult
import com.autodrive.app.feature.auth.domain.usecase.VerifyInviteCodeUseCase
import com.autodrive.app.feature.auth.presentation.register.RegistrationDraftStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CodeInputViewModel @Inject constructor(
    private val verifyInviteCode: VerifyInviteCodeUseCase,
    private val draftStore: RegistrationDraftStore,
    private val registrationProfileWriter: RegistrationProfileWriter,
    private val sessionReader: SessionReader,
    private val syncCoordinator: SyncCoordinator,
    private val pushTokenRepository: PushTokenRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<CodeState>(CodeState.Idle)
    val state: StateFlow<CodeState> = _state

    fun requestMessage(): String = draftStore.whatsappMessage()

    fun verify(code: String) {
        if (!code.matches(Regex("^[0-9]{8}$"))) return
        viewModelScope.launch {
            _state.value = CodeState.Loading
            _state.value = when (val result = verifyInviteCode(code)) {
                is CodeVerificationResult.Success -> {
                    if (result.isExistingUser) {
                        CodeState.Success(true)
                    } else {
                        when (val save = saveDraftProfile()) {
                            is Result.Success -> {
                                draftStore.clear()
                                syncCoordinator.requestSync(SyncReason.LOGIN_SUCCESS)
                                FcmTokenUploader.trigger(appContext, pushTokenRepository)
                                CodeState.Success(false)
                            }
                            is Result.Error -> CodeState.Error(save.message)
                            else -> CodeState.Error("تعذّر إكمال التسجيل")
                        }
                    }
                }
                is CodeVerificationResult.Invalid -> CodeState.Error("الكود غير صحيح")
                is CodeVerificationResult.Expired -> CodeState.Error("انتهت صلاحية الكود")
                is CodeVerificationResult.AlreadyUsed -> CodeState.Error("الكود مستخدم بالفعل")
                is CodeVerificationResult.Error -> CodeState.Error(result.message)
            }
        }
    }

    private suspend fun saveDraftProfile(): Result<Unit> {
        val session = sessionReader.currentSession()
        val uid = session.userId ?: return Result.Error("بيانات الجلسة مفقودة")
        val clientId = session.clientId ?: return Result.Error("معرف العميل مفقود")
        val orgId = session.orgId ?: return Result.Error("معرف المنظمة مفقود")
        val verifiedPhone = session.phone?.trim()?.takeIf { it.isNotBlank() }
            ?: return Result.Error("رقم الهاتف الموثق مفقود — أعد التحقق بالهاتف")

        val user = AutoDriveUser(
            id = "",
            userId = uid,
            clientId = clientId,
            orgId = orgId,
            accountType = if (draftStore.accountType == "WORKSHOP_OWNER") AccountType.WORKSHOP_OWNER else AccountType.MARKETER,
            fullName = draftStore.fullName,
            phone = verifiedPhone,
            bankName = draftStore.bankName.ifBlank { null },
            bankAccount = draftStore.bankAccount.ifBlank { null },
            workshopName = draftStore.workshopName.ifBlank { null },
            specialty = draftStore.specialty.ifBlank { null },
            workersCount = draftStore.workersCount.toIntOrNull(),
            address = draftStore.address.ifBlank { null },
        )
        return registrationProfileWriter.saveRegisteredUser(user)
    }
}

sealed class CodeState {
    data object Idle : CodeState()
    data object Loading : CodeState()
    data class Success(val isExistingUser: Boolean = false) : CodeState()
    data class Error(val message: String) : CodeState()
}
