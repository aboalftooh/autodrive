package com.autodrive.app.feature.commission.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.autodrive.app.feature.commission.domain.model.CommissionEntry
import com.autodrive.app.feature.commission.domain.model.CommissionStatus
import com.autodrive.app.core.designsystem.components.actions.AutoDriveFab
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveTextButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveTextButtonTone
import com.autodrive.app.core.designsystem.components.data.AutoDriveDivider
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveBottomSheet
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveDialog
import com.autodrive.app.core.common.format.FormatUtils
import com.autodrive.app.core.platform.export.InvoicePdfEntry
import com.autodrive.app.core.platform.export.InvoicePdfGenerator
import com.autodrive.app.core.platform.share.WhatsAppHelper
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBrand
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText

// ═══════════════════════════════════════════════
// CommissionEntryRow — مُستخدَم في شاشات أخرى
// ═══════════════════════════════════════════════
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommissionEntryRow(
    entry: CommissionEntry,
    nextFriday9AmMs: Long = 0L,
    userName: String = ""
) {
    var showInvoiceSheet by remember { mutableStateOf(false) }
    var showInfoDialog   by remember { mutableStateOf(false) }

    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = AutoDriveSurface.Raised,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AutoDriveBorderColor.Default, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClickLabel = "عرض تفاصيل الفاتورة",
                onLongClickLabel = "عرض معلومات العمولة",
                onClick = { showInvoiceSheet = true },
                onLongClick = { showInfoDialog = true },
            )
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("فاتورة #${entry.invoiceNumber}", style = MaterialTheme.typography.titleMedium, color = AutoDriveFinance.Withdrawable)
                Text(FormatUtils.formatDate(entry.createdAt), style = MaterialTheme.typography.bodySmall, color = AutoDriveFinance.Withdrawable.copy(alpha = 0.65f))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(FormatUtils.formatSar(entry.amount), style = MaterialTheme.typography.titleMedium, color = AutoDriveFinance.Withdrawable, fontWeight = FontWeight.Bold)
                StatusBadge(entry.status)
            }
        }
    }

    if (showInvoiceSheet) {
        InvoiceDetailSheet(entry = entry, userName = userName, onDismiss = { showInvoiceSheet = false })
    }
    if (showInfoDialog) {
        EntryInfoDialog(entry = entry, nextFriday9AmMs = nextFriday9AmMs, onDismiss = { showInfoDialog = false })
    }
}

// ─── Invoice Detail Bottom Sheet ───────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailSheet(entry: CommissionEntry, userName: String, onDismiss: () -> Unit) {
    val context     = LocalContext.current
    val scrollState = rememberScrollState()

    AutoDriveBottomSheet(
        onDismissRequest = onDismiss,
        title = "تفاصيل الفاتورة",
        skipPartiallyExpanded = true,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AutoDriveDivider()
                InvoiceDetailRow("رقم الفاتورة",  "#${entry.invoiceNumber}")
                InvoiceDetailRow("قيمة العمولة",  FormatUtils.formatSar(entry.amount))
                InvoiceDetailRow("تاريخ الإنشاء", FormatUtils.formatDate(entry.createdAt))
                entry.paidAt?.let { InvoiceDetailRow("تاريخ الصرف", FormatUtils.formatDate(it)) }
                InvoiceDetailRow("الحالة", when (entry.status) {
                    CommissionStatus.WITHDRAWABLE -> "قابلة للسحب"
                    CommissionStatus.PENDING      -> "معلّقة"
                    CommissionStatus.PAID         -> "مصروفة"
                })
                Spacer(Modifier.height(4.dp))
                AutoDrivePrimaryButton(
                    text    = "مشاركة PDF",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        InvoicePdfGenerator.generateAndShare(context, entry.toPdfEntry(), userName)
                        onDismiss()
                    }
                )
                AutoDrivePrimaryButton(
                    text    = "واتساب",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val msg = "تفاصيل الفاتورة — $userName\n" +
                                  "رقم الفاتورة: #${entry.invoiceNumber}\n" +
                                  "قيمة العمولة: ${FormatUtils.formatSar(entry.amount)}\n" +
                                  "التاريخ: ${FormatUtils.formatDate(entry.createdAt)}"
                        WhatsAppHelper.shareInvoice(context, msg)
                        onDismiss()
                    }
                )
            }

            // FAB الطباعة
            AutoDriveFab(
                onClick = {
                    InvoicePdfGenerator.generateAndPrint(context, entry.toPdfEntry())
                    onDismiss()
                },
                icon = Icons.Default.Print,
                contentDescription = "طباعة",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun InvoiceDetailRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AutoDriveText.Secondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = AutoDriveText.Primary, fontWeight = FontWeight.Medium)
    }
}

// ─── Entry Info Dialog ─────────────────────────
@Composable
fun EntryInfoDialog(entry: CommissionEntry, nextFriday9AmMs: Long, onDismiss: () -> Unit) {
    val (title, message, showWithdraw) = when (entry.status) {
        CommissionStatus.PENDING      -> Triple(
            "عمولة معلّقة",
            "هذه العمولة في انتظار موعد الصرف الأسبوعي.\nسيكون بإمكانك سحبها يوم الجمعة القادمة الساعة 9:00 صباحاً.",
            false
        )
        CommissionStatus.WITHDRAWABLE -> Triple(
            "قابلة للسحب",
            "هذه العمولة جاهزة للسحب الآن.\nيمكنك طلب السحب في أي وقت.",
            true
        )
        CommissionStatus.PAID -> Triple(
            "عمولة مصروفة",
            "تم صرف هذه العمولة بتاريخ ${entry.paidAt?.let { FormatUtils.formatDate(it) } ?: "—"}.",
            false
        )
    }

    AutoDriveDialog(
        title = title,
        body = message,
        onDismissRequest = onDismiss,
        actions = {
            if (showWithdraw) {
                AutoDriveTextButton(text = "لاحقاً", onClick = onDismiss)
            }
            AutoDriveTextButton(
                text = if (showWithdraw) "سحب الآن" else "حسناً",
                onClick = onDismiss,
                tone = AutoDriveTextButtonTone.Primary,
            )
        },
    )
}


private fun CommissionEntry.toPdfEntry(): InvoicePdfEntry = InvoicePdfEntry(
    invoiceNumber = invoiceNumber,
    createdAt = createdAt,
    amount = amount,
)
