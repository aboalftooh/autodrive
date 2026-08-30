package com.autodrive.app.feature.balance.presentation

import java.math.BigDecimal
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.core.sync.domain.SyncCoordinator
import com.autodrive.app.core.sync.domain.SyncReason
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.feature.balance.domain.model.WithdrawalStatus
import com.autodrive.app.feature.balance.domain.model.WithdrawalSubmitResult
import com.autodrive.app.core.model.money.Money
import com.autodrive.app.feature.balance.domain.usecase.ObserveBalanceUseCase
import com.autodrive.app.feature.balance.domain.usecase.ObserveTransactionsUseCase
import com.autodrive.app.feature.balance.domain.usecase.ObserveWithdrawalRequestsUseCase
import com.autodrive.app.feature.balance.domain.usecase.CancelAllPendingWithdrawalsUseCase
import com.autodrive.app.feature.balance.domain.usecase.RequestWithdrawalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BalanceViewModel @Inject constructor(
    private val observeBalance: ObserveBalanceUseCase,
    private val observeTransactions: ObserveTransactionsUseCase,
    private val observeWithdrawalRequests: ObserveWithdrawalRequestsUseCase,
    private val requestWithdrawal: RequestWithdrawalUseCase,
    private val cancelAllPendingWithdrawals: CancelAllPendingWithdrawalsUseCase,
    private val syncCoordinator: SyncCoordinator
) : ViewModel() {

    private val _state = MutableStateFlow(BalanceUiState())
    val state: StateFlow<BalanceUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                observeBalance(),
                observeTransactions(),
                observeWithdrawalRequests()
            ) { balance, transactions, withdrawals ->
                Triple(balance, transactions, withdrawals)
            }.collect { (balance, transactions, withdrawals) ->
                // دمج حركات الرصيد مع طلبات السحب المعلّقة في سجل موحَّد مرتَّب بالتاريخ
                val history = buildList {
                    transactions.forEach { add(BalanceHistoryItem.Transaction(it)) }
                    withdrawals
                        .filter { it.status == WithdrawalStatus.PENDING }
                        .forEach { add(BalanceHistoryItem.PendingWithdrawal(it)) }
                }.sortedByDescending { it.sortKey }

                _state.update { s ->
                    s.copy(
                        isLoading          = false,
                        balance            = balance,
                        transactions       = transactions,
                        withdrawalRequests = withdrawals,
                        historyItems       = history
                    )
                }
            }
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            syncCoordinator.requestSync(SyncReason.USER_REFRESH)
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun dismissSubmitBanners() { _state.update { it.copy(submitSuccess = false, submitPendingLocal = false) } }

    // يُستدعى عند ضغط زر "طلب سحب": يُحدِّث الرصيد من الخادم أولاً ثم يفتح النافذة
    fun openWithdrawSheet() {
        if (_state.value.isRefreshingBalance) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshingBalance = true, submitError = null) }
            syncCoordinator.requestSync(SyncReason.USER_REFRESH)
            _state.update { it.copy(isRefreshingBalance = false, showWithdrawSheet = true,
                submitSuccess = false, submitPendingLocal = false) }
        }
    }

    fun onWithdrawSheetOpen()  { _state.update { it.copy(showWithdrawSheet = true, submitError = null, submitSuccess = false, submitPendingLocal = false) } }
    fun onWithdrawSheetClose() { _state.update { it.copy(showWithdrawSheet = false, withdrawalAmount = "", withdrawalNote = "") } }
    fun onAmountChange(v: String) { _state.update { it.copy(withdrawalAmount = v.filter { c -> c.isDigit() || c == '.' }) } }
    fun onNoteChange(v: String)   { _state.update { it.copy(withdrawalNote = v) } }

    fun onCancelDialogShow()    { _state.update { it.copy(showCancelConfirmDialog = true) } }
    fun onCancelDialogDismiss() { _state.update { it.copy(showCancelConfirmDialog = false) } }

    fun cancelAllPending() {
        viewModelScope.launch {
            _state.update { it.copy(isCancelling = true, showCancelConfirmDialog = false) }
            when (val r = cancelAllPendingWithdrawals()) {
                is Result.Success -> { load() }
                is Result.Error   -> _state.update { it.copy(submitError = r.message) }
                else -> {}
            }
            _state.update { it.copy(isCancelling = false) }
        }
    }

    fun submitWithdrawal() {
        if (_state.value.isSubmitting) return
        val amountBD  = _state.value.withdrawalAmount.toBigDecimalOrNull() ?: return
        val balanceMoney = _state.value.balance?.balance ?: Money.ZERO
        // يجب أن يطابق حساب السيرفر تماماً: المحجوز = PENDING + APPROVED
        // (request_withdrawal يحجز كليهما — الخصم الفعلي يقع عند COMPLETED فقط).
        // خصم PENDING وحدها كان يُظهر رصيداً متاحاً أكبر من الحقيقي فيُفاجأ المستخدم
        // برفض السيرفر INSUFFICIENT_BALANCE.
        val holdsBD   = _state.value.withdrawalRequests
            .filter { it.status == WithdrawalStatus.PENDING || it.status == WithdrawalStatus.APPROVED }
            .fold(Money.ZERO) { acc, w -> acc + w.amount }
        val available = (balanceMoney - holdsBD).let { if (it.isNegative()) Money.ZERO else it }
        val amount = Money.of(amountBD)
        if (amountBD <= BigDecimal.ZERO) { _state.update { it.copy(submitError = "أدخل مبلغاً صحيحاً") }; return }
        if (amount > available) { _state.update { it.copy(submitError = "المبلغ أكبر من الرصيد المتاح (${available.toPlainString()})") }; return }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, submitError = null) }
            when (val r = requestWithdrawal(amount, _state.value.withdrawalNote)) {
                is Result.Success -> {
                    when (r.data) {
                        is WithdrawalSubmitResult.Submitted -> {
                            _state.update {
                                it.copy(
                                    isSubmitting     = false,
                                    submitSuccess     = true,
                                    submitPendingLocal = false,
                                    showWithdrawSheet = false,
                                    withdrawalAmount  = "",
                                    withdrawalNote    = ""
                                )
                            }
                        }
                        WithdrawalSubmitResult.PendingLocal -> {
                            // الطلب لم يصل للسيرفر — لا "تم"
                            _state.update {
                                it.copy(
                                    isSubmitting      = false,
                                    submitSuccess      = false,
                                    submitPendingLocal = true,
                                    showWithdrawSheet  = false,
                                    withdrawalAmount   = "",
                                    withdrawalNote     = ""
                                )
                            }
                        }
                    }
                    load()
                }
                is Result.Error -> _state.update { it.copy(isSubmitting = false, submitError = r.message) }
                else -> {}
            }
        }
    }
}
