package com.autodrive.app.feature.balance.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.autodrive.app.core.common.format.FormatUtils
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveSecondaryButton
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveSnackbarContent
import com.autodrive.app.core.designsystem.components.navigation.AutoDriveBackHeader
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBrand
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveStatus
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.radius.AutoDriveRadius
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveContentWidth
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.feature.balance.domain.model.BalanceTransaction
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

@Composable
fun BalanceScreen(
    onBack: () -> Unit,
    onOpenInvoice: (String) -> Unit = {},
    viewModel: BalanceViewModel = hiltViewModel(),
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

    if (state.showWithdrawSheet) {
        WithdrawalSheet(
            state = state,
            onAmountChange = viewModel::onAmountChange,
            onNoteChange = viewModel::onNoteChange,
            onSubmit = viewModel::submitWithdrawal,
            onDismiss = viewModel::onWithdrawSheetClose,
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = AutoDriveSurface.Canvas,
            topBar = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    AutoDriveBackHeader(
                        title = "الرصيد والسحب",
                        onBack = onBack,
                        modifier = Modifier
                            .widthIn(max = AutoDriveContentWidth.Dashboard)
                            .fillMaxWidth(),
                        titleContent = {
                            Column {
                                Text(
                                    text = "الرصيد والسحب",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = AutoDriveText.Primary,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "الرصيد والحركات وطلبات السحب",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AutoDriveText.Secondary,
                                )
                            }
                        },
                    )
                }
            },
            snackbarHost = {
                when {
                    state.submitSuccess -> AutoDriveSnackbarContent(
                        "تم تقديم طلب السحب بنجاح",
                        Modifier.padding(AutoDriveSpace.LG),
                    )
                    state.submitPendingLocal -> AutoDriveSnackbarContent(
                        "في الانتظار — سيُرسل تلقائياً عند عودة الاتصال",
                        Modifier.padding(AutoDriveSpace.LG),
                    )
                }
            },
        ) { scaffoldPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .widthIn(max = AutoDriveContentWidth.Dashboard),
                contentPadding = PaddingValues(
                    start = AutoDriveSpace.LG,
                    end = AutoDriveSpace.LG,
                    top = AutoDriveSpace.MD,
                    bottom = AutoDriveSpace.X3L,
                ),
                verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.LG),
            ) {
                item {
                    BalanceHeroCard(
                        state = state,
                        onWithdraw = viewModel::openWithdrawSheet,
                    )
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            text = "سجل الحركات",
                            style = MaterialTheme.typography.titleLarge,
                            color = AutoDriveText.Primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "الحركات التي غيّرت الرصيد فعلياً",
                            style = MaterialTheme.typography.bodySmall,
                            color = AutoDriveText.Secondary,
                        )
                    }
                }

                when {
                    state.isLoadingActivities && state.transactions.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = AutoDriveBrand.Primary)
                            }
                        }
                    }
                    state.activityError != null && state.transactions.isEmpty() -> {
                        item {
                            ActivityErrorCard(
                                message = state.activityError.orEmpty(),
                                onRetry = viewModel::retryActivities,
                            )
                        }
                    }
                    state.transactions.isEmpty() -> {
                        item {
                            EmptyActivityCard()
                        }
                    }
                    else -> {
                        item {
                            ActivityListCard(
                                transactions = state.transactions,
                                onOpenInvoice = onOpenInvoice,
                            )
                        }
                    }
                }

                if (state.hasMoreActivities) {
                    item {
                        AutoDriveSecondaryButton(
                            text = "تحميل المزيد",
                            onClick = viewModel::loadMoreActivities,
                            modifier = Modifier.fillMaxWidth(),
                            loading = state.isLoadingMoreActivities,
                            icon = Icons.Rounded.Download,
                        )
                    }
                }

                if (state.activityError != null && state.transactions.isNotEmpty()) {
                    item {
                        Text(
                            text = state.activityError.orEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            color = AutoDriveStatus.Error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceHeroCard(
    state: BalanceUiState,
    onWithdraw: () -> Unit,
) {
    val balance = state.balance?.balance ?: com.autodrive.app.core.model.money.Money.ZERO
    val activeWithdrawal = state.activeWithdrawal
    val shape = RoundedCornerShape(AutoDriveRadius.XL)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = AutoDriveSurface.Base,
        border = BorderStroke(1.dp, AutoDriveFinance.Withdrawable.copy(alpha = 0.82f)),
    ) {
        Column(
            modifier = Modifier.padding(AutoDriveSpace.XL),
            verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.LG),
            ) {
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(AutoDriveRadius.LG),
                    color = AutoDriveFinance.Withdrawable.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, AutoDriveFinance.Withdrawable.copy(alpha = 0.5f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = AutoDriveFinance.Withdrawable,
                            modifier = Modifier.size(38.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "المتاح للسحب الآن",
                        style = MaterialTheme.typography.titleMedium,
                        color = AutoDriveText.Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = FormatUtils.formatSarNoLabel(balance),
                        style = MaterialTheme.typography.displaySmall,
                        color = AutoDriveFinance.Withdrawable,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "الرصيد القابل لطلب السحب حالياً",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AutoDriveText.Secondary,
                    )
                }
            }

            if (activeWithdrawal != null) {
                Surface(
                    shape = AutoDriveRadius.PillShape,
                    color = AutoDriveFinance.Pending.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, AutoDriveFinance.Pending.copy(alpha = 0.55f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = AutoDriveSpace.MD, vertical = AutoDriveSpace.SM),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = AutoDriveFinance.Pending,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "سحب قيد المعالجة",
                            style = MaterialTheme.typography.labelLarge,
                            color = AutoDriveFinance.Pending,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            AutoDrivePrimaryButton(
                text = "سحب الرصيد",
                onClick = onWithdraw,
                modifier = Modifier.fillMaxWidth(),
                enabled = activeWithdrawal == null && balance.isPositive(),
                loading = state.isRefreshingBalance,
                icon = Icons.Rounded.Payments,
            )

            if (activeWithdrawal != null) {
                Text(
                    text = "طلبك بمبلغ ${FormatUtils.formatSarNoLabel(activeWithdrawal.amount)} قيد المعالجة، سيتاح طلب سحب جديد بعد اكتماله",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = AutoDriveText.Secondary,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

@Composable
private fun ActivityListCard(
    transactions: List<BalanceTransaction>,
    onOpenInvoice: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AutoDriveRadius.LG),
        color = AutoDriveSurface.Base,
        border = BorderStroke(1.dp, AutoDriveBorderColor.Default),
    ) {
        Column {
            transactions.forEachIndexed { index, transaction ->
                BalanceActivityRow(
                    transaction = transaction,
                    onOpenInvoice = onOpenInvoice,
                )
                if (index != transactions.lastIndex) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp),
                        color = AutoDriveBorderColor.Default,
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun BalanceActivityRow(
    transaction: BalanceTransaction,
    onOpenInvoice: (String) -> Unit,
) {
    val isCredit = transaction.type == "CREDIT"
    val color = if (isCredit) AutoDriveFinance.Withdrawable else AutoDriveStatus.Error
    val invoiceId = transaction.referenceId.takeIf {
        isCredit && transaction.referenceType == "COMMISSION_EARNED" && !it.isNullOrBlank()
    }
    val title = when {
        isCredit && transaction.referenceType == "COMMISSION_EARNED" -> "عمولة داخلة"
        isCredit -> "إضافة للرصيد"
        transaction.referenceType in setOf("WITHDRAWAL", "COMMISSION_PAYOUT") -> "مبلغ مسحوب"
        transaction.referenceType == "COMMISSION_CLAWBACK" -> "تسوية عمولة"
        transaction.referenceType == "BALANCE_RECONCILIATION" -> "تسوية رصيد"
        else -> "خصم من الرصيد"
    }
    val subtitle = when {
        isCredit && transaction.invoiceNumber != null -> "فاتورة INV-${transaction.invoiceNumber}"
        transaction.referenceType in setOf("WITHDRAWAL", "COMMISSION_PAYOUT") -> "تفاصيل السحب"
        transaction.description.isNotBlank() -> transaction.description
        isCredit -> "حركة إضافة مؤكدة"
        else -> "حركة خصم مؤكدة"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (invoiceId != null) {
                    Modifier.clickable(role = Role.Button) { onOpenInvoice(invoiceId) }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = AutoDriveSpace.MD, vertical = AutoDriveSpace.MD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD),
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(AutoDriveRadius.MD),
            color = color.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isCredit) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1.1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AutoDriveText.Secondary,
                maxLines = 1,
            )
        }

        Text(
            text = formatActivityDate(transaction.createdAt),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = AutoDriveText.Secondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )

        Text(
            text = FormatUtils.formatSarNoLabel(transaction.amount),
            modifier = Modifier.weight(0.9f),
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 1,
        )

        if (invoiceId != null) {
            Icon(
                imageVector = Icons.Rounded.ChevronLeft,
                contentDescription = "فتح الفاتورة",
                tint = AutoDriveText.Secondary,
                modifier = Modifier.size(22.dp),
            )
        } else {
            Spacer(Modifier.width(22.dp))
        }
    }
}

@Composable
private fun EmptyActivityCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AutoDriveRadius.LG),
        color = AutoDriveSurface.Base,
        border = BorderStroke(1.dp, AutoDriveBorderColor.Default),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 40.dp, horizontal = AutoDriveSpace.XL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM),
        ) {
            Text(
                text = "لا توجد حركات بعد",
                style = MaterialTheme.typography.titleMedium,
                color = AutoDriveText.Primary,
            )
            Text(
                text = "ستظهر هنا فقط الحركات التي زادت الرصيد أو أنقصته",
                style = MaterialTheme.typography.bodySmall,
                color = AutoDriveText.Secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ActivityErrorCard(
    message: String,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AutoDriveRadius.LG),
        color = AutoDriveSurface.Base,
        border = BorderStroke(1.dp, AutoDriveStatus.Error.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.padding(AutoDriveSpace.XL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD),
        ) {
            Text(
                text = message,
                color = AutoDriveStatus.Error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            AutoDriveSecondaryButton(
                text = "إعادة المحاولة",
                onClick = onRetry,
            )
        }
    }
}

private val activityMonths = listOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر",
)

private fun formatActivityDate(value: String): String {
    val date = runCatching { OffsetDateTime.parse(value).toLocalDate() }
        .recoverCatching { Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDate() }
        .getOrNull() ?: return value.take(10)
    return "%02d %s %d".format(date.dayOfMonth, activityMonths[date.monthValue - 1], date.year)
}
