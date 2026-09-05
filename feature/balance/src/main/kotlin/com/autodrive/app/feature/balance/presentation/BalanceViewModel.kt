package com.autodrive.app.feature.balance.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.core.model.money.Money
import com.autodrive.app.core.sync.domain.SyncCoordinator
import com.autodrive.app.core.sync.domain.SyncReason
import com.autodrive.app.feature.balance.domain.WithdrawalDecision
import com.autodrive.app.feature.balance.domain.WithdrawalPolicy
import com.autodrive.app.feature.balance.domain.model.WithdrawalSubmitResult
import com.autodrive.app.feature.balance.domain.usecase.GetBalanceActivityPageUseCase
import com.autodrive.app.feature.balance.domain.usecase.ObserveBalanceUseCase
import com.autodrive.app.feature.balance.domain.usecase.ObserveTransactionsUseCase
import com.autodrive.app.feature.balance.domain.usecase.ObserveWithdrawalRequestsUseCase
import com.autodrive.app.feature.balance.domain.usecase.RequestWithdrawalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val BALANCE_ACTIVITY_PAGE_SIZE = 10

@HiltViewModel
class BalanceViewModel @Inject constructor(
    private val observeBalance: ObserveBalanceUseCase,
    private val observeTransactions: ObserveTransactionsUseCase,
    private val observeWithdrawalRequests: ObserveWithdrawalRequestsUseCase,
    private val getBalanceActivityPage: GetBalanceActivityPageUseCase,
    private val requestWithdrawal: RequestWithdrawalUseCase,
    private val withdrawalPolicy: WithdrawalPolicy,
    private val syncCoordinator: SyncCoordinator,
) : ViewModel() {

    private val _state = MutableStateFlow(BalanceUiState())
    val state: StateFlow<BalanceUiState> = _state.asStateFlow()

    init {
        observeWalletState()
        observeBookedMovementChanges()
        load()
    }

    private fun observeWalletState() {
        viewModelScope.launch {
            combine(
                observeBalance(),
                observeWithdrawalRequests(),
            ) { balance, withdrawals -> balance to withdrawals }
                .collect { (balance, withdrawals) ->
                    _state.update { current ->
                        current.copy(
                            isLoading = false,
                            balance = balance,
                            withdrawalRequests = withdrawals,
                            activeWithdrawal = withdrawalPolicy.activeRequest(withdrawals),
                        )
                    }
                }
        }
    }

    /**
     * Room is used only as a realtime/sync change signal here. The visible statement itself is
     * always re-read from the server-paged wallet journal; local rows never decide financial truth.
     */
    private fun observeBookedMovementChanges() {
        viewModelScope.launch {
            observeTransactions()
                .map { rows -> rows.firstOrNull()?.id to rows.size }
                .distinctUntilChanged()
                .drop(1)
                .collect { loadActivitiesInitial() }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.balance == null) }
            syncCoordinator.requestSync(SyncReason.USER_REFRESH)
            loadActivitiesInitial()
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun retryActivities() = loadActivitiesInitial()

    fun loadMoreActivities() {
        val current = _state.value
        if (
            current.isLoadingActivities ||
            current.isLoadingMoreActivities ||
            !current.hasMoreActivities ||
            current.nextActivityCursor == null
        ) return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingMoreActivities = true, activityError = null) }
            runCatching {
                getBalanceActivityPage(current.nextActivityCursor, BALANCE_ACTIVITY_PAGE_SIZE)
            }.onSuccess { page ->
                _state.update {
                    it.copy(
                        transactions = it.transactions + page.entries,
                        nextActivityCursor = page.nextCursor,
                        hasMoreActivities = page.hasMore,
                        isLoadingMoreActivities = false,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoadingMoreActivities = false,
                        activityError = error.message ?: "تعذّر تحميل المزيد",
                    )
                }
            }
        }
    }

    private fun loadActivitiesInitial() {
        if (_state.value.isLoadingActivities && _state.value.transactions.isNotEmpty()) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoadingActivities = true,
                    activityError = null,
                )
            }
            runCatching { getBalanceActivityPage(null, BALANCE_ACTIVITY_PAGE_SIZE) }
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            transactions = page.entries,
                            nextActivityCursor = page.nextCursor,
                            hasMoreActivities = page.hasMore,
                            isLoadingActivities = false,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoadingActivities = false,
                            activityError = error.message ?: "تعذّر تحميل سجل الحركات",
                        )
                    }
                }
        }
    }

    fun dismissSubmitBanners() {
        _state.update { it.copy(submitSuccess = false, submitPendingLocal = false) }
    }

    fun openWithdrawSheet() {
        if (_state.value.isRefreshingBalance || _state.value.activeWithdrawal != null) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshingBalance = true, submitError = null) }
            syncCoordinator.requestSync(SyncReason.USER_REFRESH)
            val hasActive = _state.value.activeWithdrawal != null
            _state.update {
                it.copy(
                    isRefreshingBalance = false,
                    showWithdrawSheet = !hasActive,
                    submitError = if (hasActive) "يوجد طلب سحب قيد المعالجة بالفعل" else null,
                    submitSuccess = false,
                    submitPendingLocal = false,
                )
            }
        }
    }

    fun onWithdrawSheetClose() {
        _state.update {
            it.copy(
                showWithdrawSheet = false,
                withdrawalAmount = "",
                withdrawalNote = "",
            )
        }
    }

    fun onAmountChange(value: String) {
        _state.update { state ->
            state.copy(withdrawalAmount = value.filter { it.isDigit() || it == '.' })
        }
    }

    fun onNoteChange(value: String) {
        _state.update { it.copy(withdrawalNote = value) }
    }

    fun submitWithdrawal() {
        if (_state.value.isSubmitting) return
        val amountDecimal = _state.value.withdrawalAmount.toBigDecimalOrNull() ?: return
        val amount = Money.of(amountDecimal)
        val balance = _state.value.balance?.balance ?: Money.ZERO

        when (val decision = withdrawalPolicy.evaluate(amount, balance, _state.value.withdrawalRequests)) {
            WithdrawalDecision.ActiveRequestExists -> {
                _state.update { it.copy(submitError = "يوجد طلب سحب قيد المعالجة بالفعل") }
                return
            }
            WithdrawalDecision.InvalidAmount -> {
                _state.update { it.copy(submitError = "أدخل مبلغاً صحيحاً") }
                return
            }
            is WithdrawalDecision.InsufficientBalance -> {
                _state.update {
                    it.copy(submitError = "المبلغ أكبر من الرصيد المتاح (${decision.available.toPlainString()})")
                }
                return
            }
            WithdrawalDecision.Allowed -> Unit
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, submitError = null) }
            when (val result = requestWithdrawal(amount, _state.value.withdrawalNote)) {
                is Result.Success -> {
                    when (result.data) {
                        is WithdrawalSubmitResult.Submitted -> {
                            _state.update {
                                it.copy(
                                    isSubmitting = false,
                                    submitSuccess = true,
                                    submitPendingLocal = false,
                                    showWithdrawSheet = false,
                                    withdrawalAmount = "",
                                    withdrawalNote = "",
                                )
                            }
                        }
                        WithdrawalSubmitResult.PendingLocal -> {
                            _state.update {
                                it.copy(
                                    isSubmitting = false,
                                    submitSuccess = false,
                                    submitPendingLocal = true,
                                    showWithdrawSheet = false,
                                    withdrawalAmount = "",
                                    withdrawalNote = "",
                                )
                            }
                        }
                    }
                    load()
                }
                is Result.Error -> _state.update {
                    it.copy(isSubmitting = false, submitError = result.message)
                }
                else -> Unit
            }
        }
    }
}
