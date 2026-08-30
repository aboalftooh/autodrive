package com.autodrive.app.feature.balance.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.autodrive.app.core.common.format.FormatUtils
import com.autodrive.app.core.designsystem.components.AutoDriveAccent
import com.autodrive.app.core.designsystem.components.AutoDriveStatusTone
import com.autodrive.app.core.designsystem.components.actions.AutoDriveSecondaryButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveTextButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveTextButtonTone
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveEmptyState
import com.autodrive.app.core.designsystem.components.data.AutoDriveSectionHeader
import com.autodrive.app.core.designsystem.components.data.AutoDriveStatSize
import com.autodrive.app.core.designsystem.components.data.AutoDriveStatValue
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveDialog
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveDialogTone
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveSnackbarContent
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveContentWidth
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.patterns.dashboard.DashboardHero
import com.autodrive.app.core.designsystem.patterns.finance.PendingRequestCard
import com.autodrive.app.core.designsystem.patterns.finance.TransactionRow as TransactionPatternRow
import com.autodrive.app.core.designsystem.patterns.header.ScreenHeader
import com.autodrive.app.core.designsystem.patterns.state.ErrorScreen
import com.autodrive.app.core.designsystem.patterns.state.LoadingScreen
import com.autodrive.app.core.model.money.Money
import com.autodrive.app.feature.balance.domain.model.WithdrawalStatus

@Composable
fun BalanceScreen(
    onBack: () -> Unit,
    onOpenReport: () -> Unit = {},
    viewModel: BalanceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.submitSuccess, state.submitPendingLocal) {
        when {
            state.submitSuccess -> {
                kotlinx.coroutines.delay(3000)
                viewModel.dismissSubmitBanners()
            }
            state.submitPendingLocal -> {
                kotlinx.coroutines.delay(5000)
                viewModel.dismissSubmitBanners()
            }
        }
    }

    if (state.showCancelConfirmDialog) {
        AutoDriveDialog(
            title = "إلغاء الطلبات المعلقة",
            body = "سيتم حذف جميع طلبات السحب قيد المعالجة. هل أنت متأكد؟",
            tone = AutoDriveDialogTone.Destructive,
            onDismissRequest = viewModel::onCancelDialogDismiss,
            actions = {
                AutoDriveTextButton("تراجع", viewModel::onCancelDialogDismiss)
                AutoDriveTextButton("نعم، احذف", viewModel::cancelAllPending, tone = AutoDriveTextButtonTone.Destructive, loading = state.isCancelling)
            },
        )
    }

    if (state.showWithdrawSheet) {
        WithdrawalSheet(
            state = state,
            onAmountChange = viewModel::onAmountChange,
            onNoteChange = viewModel::onNoteChange,
            onSubmit = viewModel::submitWithdrawal,
            onDismiss = viewModel::onWithdrawSheetClose,
        )
    }

    Scaffold(
        containerColor = AutoDriveSurface.Canvas,
        topBar = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                ScreenHeader(
                    title = "رصيدي",
                    modifier = Modifier.widthIn(max = AutoDriveContentWidth.Dashboard).fillMaxWidth(),
                    onBack = onBack,

                )
            }
        },
        snackbarHost = {
            when {
                state.submitSuccess -> AutoDriveSnackbarContent("تم تقديم الطلب بنجاح", Modifier.padding(AutoDriveSpace.LG))
                state.submitPendingLocal -> AutoDriveSnackbarContent("في الانتظار — سيُرسل تلقائياً عند عودة الاتصال", Modifier.padding(AutoDriveSpace.LG))
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val contentModifier = Modifier.widthIn(max = AutoDriveContentWidth.Dashboard).fillMaxSize()
            when {
                state.isLoading -> LoadingScreen(
                    label = "جاري تحميل الرصيد",
                    modifier = contentModifier,
                )
                state.errorMessage != null && state.balance == null -> ErrorScreen(
                    title = "تعذر تحميل الرصيد",
                    body = state.errorMessage.orEmpty(),
                    retryLabel = "إعادة المحاولة",
                    onRetry = viewModel::load,
                    modifier = contentModifier,
                )
                else -> BalanceContent(
                    state = state,
                    onOpenReport = onOpenReport,
                    onWithdraw = viewModel::openWithdrawSheet,
                    onCancelPending = viewModel::onCancelDialogShow,
                    modifier = contentModifier,
                )
            }
        }
    }
}

@Composable
private fun BalanceContent(
    state: BalanceUiState,
    onOpenReport: () -> Unit,
    onWithdraw: () -> Unit,
    onCancelPending: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pendingRequests = state.withdrawalRequests.filter { it.status == WithdrawalStatus.PENDING }
    val pendingSum = Money.sum(pendingRequests.map { it.amount })

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = AutoDriveSpace.LG,
            end = AutoDriveSpace.LG,
            top = AutoDriveSpace.XL,
            bottom = AutoDriveSpace.X3L,
        ),
        verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.LG),
    ) {
        item {
            DashboardHero(
                accent = AutoDriveAccent.Active,
                label = "الرصيد الحالي",
                heroContent = {
                    AutoDriveStatValue(
                        value = FormatUtils.formatSar(state.balance?.balance ?: Money.ZERO),
                        size = AutoDriveStatSize.Large,
                        accent = AutoDriveAccent.Active,
                    )
                },
                supportingContent = {
                    if (pendingSum.isPositive()) {
                        AutoDriveStatValue(
                            value = FormatUtils.formatSar(pendingSum),
                            size = AutoDriveStatSize.Small,
                            accent = AutoDriveAccent.Secondary,
                        )
                    }
                },
            )
        }
        item {
            AutoDriveSecondaryButton(
                text = "طلب سحب",
                onClick = onWithdraw,
                icon = Icons.Rounded.ArrowUpward,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            AutoDriveSecondaryButton(
                text = "تقرير عمولاتي",
                onClick = onOpenReport,
                icon = Icons.Rounded.Description,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (pendingRequests.isNotEmpty()) {
            item {
                AutoDriveSectionHeader(
                    title = "طلبات السحب المعلقة",
                    subtitle = "${pendingRequests.size} طلب — ${FormatUtils.formatSar(pendingSum)}",
                    action = {
                        AutoDriveTextButton(
                            text = "إلغاء الكل",
                            onClick = onCancelPending,
                            enabled = !state.isCancelling,
                            loading = state.isCancelling,
                            tone = AutoDriveTextButtonTone.Destructive,
                        )
                    },
                )
            }
            items(pendingRequests, key = { "pending_${it.id}" }) { request ->
                PendingRequestCard(
                    title = "طلب سحب",
                    amount = FormatUtils.formatSar(request.amount),
                    metadata = FormatUtils.formatDate(request.createdAt),
                    statusLabel = if (request.status == WithdrawalStatus.APPROVED) "معتمد" else "قيد المعالجة",
                )
            }
        }

        item { AutoDriveSectionHeader("حركات الرصيد") }
        if (state.historyItems.isEmpty()) {
            item {
                AutoDriveEmptyState(
                    title = "لا توجد حركات بعد",
                    body = "ستظهر هنا حركات الرصيد وطلبات السحب",
                    icon = Icons.Rounded.Payments,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            items(state.historyItems, key = { item ->
                when (item) {
                    is BalanceHistoryItem.Transaction -> "tx_${item.tx.id}"
                    is BalanceHistoryItem.PendingWithdrawal -> "wr_${item.req.id}"
                }
            }) { item ->
                when (item) {
                    is BalanceHistoryItem.Transaction -> {
                        val tx = item.tx
                        val isCredit = tx.type == "CREDIT"
                        TransactionPatternRow(
                            title = tx.description.ifBlank { if (isCredit) "إيداع" else "خصم" },
                            amount = "${if (isCredit) "+" else "-"} ${FormatUtils.formatSar(tx.amount)}",
                            metadata = FormatUtils.formatDate(tx.createdAt),
                            tone = if (isCredit) AutoDriveStatusTone.Success else AutoDriveStatusTone.Error,
                            statusLabel = if (isCredit) "إضافة" else "خصم",
                        )
                    }
                    is BalanceHistoryItem.PendingWithdrawal -> {
                        val req = item.req
                        TransactionPatternRow(
                            title = "طلب سحب",
                            amount = "- ${FormatUtils.formatSar(req.amount)}",
                            metadata = FormatUtils.formatDate(req.createdAt),
                            tone = AutoDriveStatusTone.Warning,
                            statusLabel = if (req.status == WithdrawalStatus.APPROVED) "معتمد" else "قيد المعالجة",
                        )
                    }
                }
            }
        }
    }
}
