package com.autodrive.app.feature.balance.presentation

import com.autodrive.app.feature.balance.domain.model.BalanceTransaction
import com.autodrive.app.feature.balance.domain.model.MarketerBalance
import com.autodrive.app.feature.balance.domain.model.WithdrawalRequest
import com.autodrive.app.feature.balance.domain.model.WithdrawalStatus

// عنصر موحَّد في سجل الحركات: إما حركة رصيد حقيقية أو طلب سحب
sealed class BalanceHistoryItem(open val sortKey: String) {
    data class Transaction(
        val tx: BalanceTransaction
    ) : BalanceHistoryItem(tx.createdAt)

    data class PendingWithdrawal(
        val req: WithdrawalRequest
    ) : BalanceHistoryItem(req.createdAt)
}

data class BalanceUiState(
    val isLoading: Boolean = true,
    val balance: MarketerBalance? = null,
    val transactions: List<BalanceTransaction> = emptyList(),
    val withdrawalRequests: List<WithdrawalRequest> = emptyList(),
    val historyItems: List<BalanceHistoryItem> = emptyList(),
    val errorMessage: String? = null,
    val showWithdrawSheet: Boolean = false,
    // true = نحدّث الرصيد من الخادم قبل فتح نافذة السحب
    val isRefreshingBalance: Boolean = false,
    val withdrawalAmount: String = "",
    val withdrawalNote: String = "",
    val isSubmitting: Boolean = false,
    // true = RPC وصل للسيرفر وعاد بنجاح
    val submitSuccess: Boolean = false,
    // true = فشل شبكي — الطلب محفوظ محلياً وسيُرسَل عند عودة الشبكة
    val submitPendingLocal: Boolean = false,
    val submitError: String? = null,
    val showCancelConfirmDialog: Boolean = false,
    val isCancelling: Boolean = false
)
