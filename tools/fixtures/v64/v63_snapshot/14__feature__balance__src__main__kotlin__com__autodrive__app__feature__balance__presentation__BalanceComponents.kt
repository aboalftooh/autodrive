package com.autodrive.app.feature.balance.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.autodrive.app.feature.balance.domain.model.BalanceTransaction
import com.autodrive.app.feature.balance.domain.model.WithdrawalRequest
import com.autodrive.app.feature.balance.domain.model.WithdrawalStatus
import com.autodrive.app.core.model.money.Money
import com.autodrive.app.feature.balance.presentation.BalanceHistoryItem
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.common.format.FormatUtils
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText

// PendingRequestsSection — طلبات معلقة + زر إلغاء الكل
// ═══════════════════════════════════════════════
@Composable
internal fun PendingRequestsSection(
    requests: List<WithdrawalRequest>,
    isCancelling: Boolean,
    onCancelAll: () -> Unit
) {
    val errorColor = MaterialTheme.colorScheme.error

    Surface(
        shape    = RoundedCornerShape(18.dp),
        color    = errorColor.copy(alpha = 0.06f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, errorColor.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "طلبات السحب المعلقة",
                        style      = MaterialTheme.typography.titleSmall,
                        color      = AutoDriveText.Primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${requests.size} طلب — ${FormatUtils.formatSar(Money.sum(requests.map { it.amount }))}",
                        style = MaterialTheme.typography.labelMedium,
                        color = AutoDriveText.Secondary
                    )
                }

                Button(
                    onClick  = onCancelAll,
                    enabled  = !isCancelling,
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = errorColor,
                        contentColor   = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    if (isCancelling) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(16.dp),
                            color       = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector        = Icons.Rounded.DeleteSweep,
                            contentDescription = null,
                            modifier           = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("إلغاء الكل", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            requests.forEach { req ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        FormatUtils.formatDate(req.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = AutoDriveText.Secondary
                    )
                    Text(
                        FormatUtils.formatSar(req.amount),
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = AutoDriveFinance.Pending,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
// BalanceHeroCard
// ═══════════════════════════════════════════════
@Composable
internal fun BalanceHeroCard(
    balance: Money,
    pendingWithdrawal: Money,
    updatedAt: String?
) {
    Surface(
        shape    = RoundedCornerShape(22.dp),
        color    = AutoDriveSurface.Raised,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .border(1.5.dp, AutoDriveFinance.Withdrawable.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier.fillMaxSize().padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text       = FormatUtils.formatSar(balance),
                    color      = AutoDriveFinance.Withdrawable,
                    fontSize   = 60.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign  = TextAlign.Center
                )

                if (pendingWithdrawal.isPositive()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AutoDriveFinance.Pending.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text     = "طلب سحب قيد المراجعة: ${FormatUtils.formatSar(pendingWithdrawal)}",
                            color    = AutoDriveFinance.Pending,
                            style    = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                val timeLabel = updatedAt?.let { FormatUtils.formatTime(it) }
                if (!timeLabel.isNullOrBlank()) {
                    Text(
                        text  = "آخر تحديث: $timeLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = AutoDriveText.Disabled
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
// TransactionRow — حركة رصيد واحدة
// ═══════════════════════════════════════════════
@Composable
internal fun TransactionRow(tx: BalanceTransaction) {
    val isCredit = tx.type == "CREDIT"
    val color    = if (isCredit) AutoDriveFinance.Withdrawable else MaterialTheme.colorScheme.error

    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = AutoDriveSurface.Raised,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AutoDriveBorderColor.Default, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector        = if (isCredit) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                    contentDescription = null,
                    tint               = color,
                    modifier           = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(tx.description.ifBlank { if (isCredit) "إيداع" else "خصم" },
                    style = MaterialTheme.typography.bodyMedium, color = AutoDriveText.Primary)
                Text(FormatUtils.formatDate(tx.createdAt),
                    style = MaterialTheme.typography.labelSmall, color = AutoDriveText.Secondary)
            }

            Text(
                text      = "${if (isCredit) "+" else "-"} ${FormatUtils.formatSar(tx.amount)}",
                style     = MaterialTheme.typography.titleSmall,
                color     = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ═══════════════════════════════════════════════
// PendingWithdrawalRow — طلب سحب قيد المعالجة في سجل الحركات
// ═══════════════════════════════════════════════
@Composable
internal fun PendingWithdrawalRow(req: WithdrawalRequest) {
    val color = AutoDriveFinance.Pending

    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = AutoDriveSurface.Raised,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector        = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint               = color,
                    modifier           = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    if (req.status == WithdrawalStatus.APPROVED) "طلب سحب — معتمد" else "طلب سحب — قيد المعالجة",
                    style = MaterialTheme.typography.bodyMedium, color = AutoDriveText.Primary
                )
                Text(FormatUtils.formatDate(req.createdAt),
                    style = MaterialTheme.typography.labelSmall, color = AutoDriveText.Secondary)
            }

            Text(
                text      = "- ${FormatUtils.formatSar(req.amount)}",
                style     = MaterialTheme.typography.titleSmall,
                color     = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ═══════════════════════════════════════════════
