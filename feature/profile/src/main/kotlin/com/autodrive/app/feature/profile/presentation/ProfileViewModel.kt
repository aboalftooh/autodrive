package com.autodrive.app.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.core.common.session.SignOutAction
import com.autodrive.app.core.model.account.AccountType
import com.autodrive.app.core.model.account.AutoDriveUser
import com.autodrive.app.core.model.money.Money
import com.autodrive.app.core.network.WeeklyPerformanceApi
import com.autodrive.app.core.session.domain.CurrentSession
import com.autodrive.app.core.session.domain.DashboardPreferences
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.sync.domain.SyncCoordinator
import com.autodrive.app.core.sync.domain.SyncReason
import com.autodrive.app.feature.balance.domain.usecase.ObserveBalanceUseCase
import com.autodrive.app.feature.profile.domain.usecase.ObserveProfileUseCase
import com.autodrive.app.feature.profile.domain.usecase.UpdateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val observeProfile: ObserveProfileUseCase,
    private val updateProfile: UpdateProfileUseCase,
    private val signOut: SignOutAction,
    private val observeBalance: ObserveBalanceUseCase,
    private val syncCoordinator: SyncCoordinator,
    private val sessionReader: SessionReader,
    private val dashboardPreferences: DashboardPreferences,
    private val weeklyPerformanceApi: WeeklyPerformanceApi,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ProfileUiState(
            weeklyTarget = dashboardPreferences.weeklyTarget,
            user = buildUserFromSession(sessionReader.currentSession())
        )
    )
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()
    private var targetPersistJob: Job? = null

    init {
        viewModelScope.launch {
            observeProfile().collect { user ->
                if (user != null) _state.update { it.copy(user = user) }
            }
        }
        viewModelScope.launch {
            observeBalance().collect { balance ->
                _state.update { it.copy(balance = balance.balance, balanceLoaded = true) }
            }
        }
        viewModelScope.launch {
            runCatching { syncCoordinator.requestSync(SyncReason.USER_REFRESH) }
            val legacyTarget = dashboardPreferences.weeklyTarget
            runCatching { weeklyPerformanceApi.getSnapshot(legacyTarget.amount) }
                .onSuccess { snapshot ->
                    val serverTarget = Money.of(snapshot.weeklyTarget)
                    dashboardPreferences.weeklyTarget = serverTarget
                    _state.update { it.copy(weeklyTarget = serverTarget) }
                }
        }
    }

    private fun buildUserFromSession(session: CurrentSession): AutoDriveUser? {
        val userId = session.userId ?: return null
        val name = session.userName ?: return null
        return AutoDriveUser(
            id = "",
            userId = userId,
            clientId = session.clientId ?: "",
            orgId = session.orgId ?: "",
            accountType = if (session.accountType == "WORKSHOP_OWNER") AccountType.WORKSHOP_OWNER else AccountType.MARKETER,
            fullName = name,
            phone = session.phone ?: "",
            bankName = null,
            bankAccount = null
        )
    }

    fun setWeeklyTarget(value: Money) {
        val min = Money.of(100_000L)
        val max = Money.of(5_000_000L)
        val clamped = when {
            value < min -> min
            value > max -> max
            else -> value
        }
        _state.update { it.copy(weeklyTarget = clamped) }
        dashboardPreferences.weeklyTarget = clamped

        // The legacy sheet changes the value on every +/- tap. Debounce remote
        // persistence so rapid taps cannot race and leave an older value on the server.
        targetPersistJob?.cancel()
        targetPersistJob = viewModelScope.launch {
            delay(350)
            runCatching { weeklyPerformanceApi.setWeeklyTarget(clamped.amount) }
                .onSuccess { update ->
                    val serverTarget = Money.of(update.weeklyTarget)
                    dashboardPreferences.weeklyTarget = serverTarget
                    _state.update { it.copy(weeklyTarget = serverTarget) }
                }
                .onFailure {
                    _state.update {
                        it.copy(successMessage = "تم حفظ الهدف على الجهاز، وتعذرت مزامنته الآن")
                    }
                }
        }
    }

    fun startEditing(section: ProfileEditSection) = _state.update {
        it.copy(editingSection = section, saveError = null)
    }

    // Temporary bridge for the pre-v57 screen. v57 replaces the global editor UI.
    fun startEditing() = startEditing(ProfileEditSection.ACCOUNT)

    fun cancelEditing() = _state.update {
        it.copy(editingSection = null, saveError = null)
    }

    fun saveAccount(fullName: String, phone: String) {
        val current = _state.value.user ?: return
        val normalizedName = fullName.trim()
        val normalizedPhone = phone.trim()
        if (normalizedName.isBlank()) {
            validationError("الاسم الكامل مطلوب")
            return
        }
        if (normalizedPhone.isBlank()) {
            validationError("رقم الهاتف مطلوب")
            return
        }
        saveSection(
            current.copy(
                fullName = normalizedName,
                phone = normalizedPhone
            )
        )
    }

    fun savePayout(bankName: String, bankAccount: String) {
        val current = _state.value.user ?: return
        saveSection(
            current.copy(
                bankName = bankName.trim().ifBlank { null },
                bankAccount = bankAccount.trim().ifBlank { null }
            )
        )
    }

    fun saveWorkshop(
        workshopName: String,
        specialty: String,
        workersCount: String,
        address: String
    ) {
        val current = _state.value.user ?: return
        if (current.accountType != AccountType.WORKSHOP_OWNER) {
            validationError("بيانات الورشة متاحة لصاحب الورشة فقط")
            return
        }

        val normalizedWorkers = workersCount.trim()
        val parsedWorkers = when {
            normalizedWorkers.isBlank() -> null
            normalizedWorkers.toIntOrNull() != null -> normalizedWorkers.toInt()
            else -> {
                validationError("عدد العمال غير صالح")
                return
            }
        }

        saveSection(
            current.copy(
                workshopName = workshopName.trim().ifBlank { null },
                specialty = specialty.trim().ifBlank { null },
                workersCount = parsedWorkers,
                address = address.trim().ifBlank { null }
            )
        )
    }

    private fun validationError(message: String) {
        _state.update { it.copy(isSaving = false, saveError = message) }
    }

    private fun saveSection(updated: AutoDriveUser) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, saveError = null) }
            when (val result = updateProfile(updated)) {
                is Result.Success -> _state.update {
                    it.copy(
                        isSaving = false,
                        editingSection = null,
                        successMessage = "تم الحفظ بنجاح",
                        user = updated
                    )
                }
                is Result.Error -> _state.update {
                    it.copy(isSaving = false, saveError = result.message)
                }
                Result.Loading -> Unit
            }
        }
    }

    /**
     * Compatibility path for the v55 form only. It keeps v56 semantics (validation + nullable clearing)
     * until v57 replaces the global form with section editors.
     */
    fun saveProfile(
        fullName: String,
        phone: String,
        bankName: String,
        bankAccount: String,
        workshopName: String,
        specialty: String,
        workersCount: String,
        address: String
    ) {
        val current = _state.value.user ?: return
        val normalizedName = fullName.trim()
        val normalizedPhone = phone.trim()
        if (normalizedName.isBlank()) {
            validationError("الاسم الكامل مطلوب")
            return
        }
        if (normalizedPhone.isBlank()) {
            validationError("رقم الهاتف مطلوب")
            return
        }

        val normalizedWorkers = workersCount.trim()
        val parsedWorkers = when {
            normalizedWorkers.isBlank() -> null
            normalizedWorkers.toIntOrNull() != null -> normalizedWorkers.toInt()
            else -> {
                validationError("عدد العمال غير صالح")
                return
            }
        }

        val workshopFieldsAllowed = current.accountType == AccountType.WORKSHOP_OWNER
        saveSection(
            current.copy(
                fullName = normalizedName,
                phone = normalizedPhone,
                bankName = bankName.trim().ifBlank { null },
                bankAccount = bankAccount.trim().ifBlank { null },
                workshopName = if (workshopFieldsAllowed) workshopName.trim().ifBlank { null } else current.workshopName,
                specialty = if (workshopFieldsAllowed) specialty.trim().ifBlank { null } else current.specialty,
                workersCount = if (workshopFieldsAllowed) parsedWorkers else current.workersCount,
                address = if (workshopFieldsAllowed) address.trim().ifBlank { null } else current.address
            )
        )
    }

    fun clearSuccessMessage() = _state.update { it.copy(successMessage = null) }

    fun requestSignOut() = _state.update { it.copy(showSignOutConfirmDialog = true) }
    fun dismissSignOutDialog() = _state.update { it.copy(showSignOutConfirmDialog = false) }

    fun signOut() {
        viewModelScope.launch {
            _state.update { it.copy(showSignOutConfirmDialog = false) }
            signOut.invoke()
            _state.update { it.copy(signedOut = true) }
        }
    }
}
