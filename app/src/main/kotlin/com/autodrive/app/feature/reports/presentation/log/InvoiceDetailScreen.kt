package com.autodrive.app.feature.reports.presentation.log

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.autodrive.app.feature.commission.domain.model.CommissionEntry
import com.autodrive.app.feature.commission.domain.model.CommissionStatus
import com.autodrive.app.feature.commission.domain.model.Invoice
import com.autodrive.app.feature.commission.domain.model.InvoiceItem
import com.autodrive.app.feature.commission.domain.model.InvoiceStatus
import com.autodrive.app.core.common.format.FormatUtils
import com.autodrive.app.core.platform.export.InvoicePdfEntry
import com.autodrive.app.core.platform.export.InvoicePdfGenerator
import com.autodrive.app.core.platform.export.InvoicePdfItem
import com.autodrive.app.core.designsystem.components.actions.AutoDriveFab
import com.autodrive.app.core.designsystem.components.actions.AutoDriveIconButton
import com.autodrive.app.core.designsystem.components.data.AutoDriveDivider
import com.autodrive.app.core.designsystem.foundation.border.AutoDriveBorder
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBrand
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.radius.AutoDriveRadius
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    invoiceId: String,
    onBack: () -> Unit,
    viewModel: InvoiceDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val invoice by viewModel.invoice.collectAsState()
    val items   by viewModel.items.collectAsState()

    LaunchedEffect(invoiceId) { viewModel.load(invoiceId) }

    Scaffold(
        containerColor = AutoDriveSurface.Canvas,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text  = invoice?.let { "فاتورة #${it.invoiceNumber}" } ?: "تفاصيل الفاتورة",
                        style = MaterialTheme.typography.titleMedium,
                        color = AutoDriveText.Primary
                    )
                },
                navigationIcon = {
                    AutoDriveIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "رجوع",
                        onClick = onBack,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AutoDriveSurface.Base)
            )
        },
        floatingActionButton = {
            invoice?.let { inv ->
                AutoDriveFab(
                    onClick = {
                        val entry = inv.toCommissionEntry(viewModel.commissionStatus(inv))
                        InvoicePdfGenerator.generateAndPrint(
                            context = context,
                            entry = entry.toPdfEntry(),
                            items = items.map { it.toPdfItem() },
                            invoiceStatus = inv.status.name,
                        )
                    },
                    icon = Icons.Filled.Print,
                    contentDescription = "طباعة",
                )
            }
        }
    ) { padding ->
        when {
            viewModel.isLoading -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AutoDriveFinance.Withdrawable)
                }
            }
            invoice == null -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لم يتم العثور على الفاتورة", color = AutoDriveText.Secondary)
                }
            }
            else -> {
                val inv = invoice ?: return@Scaffold
                InvoiceDetailContent(
                    invoice  = inv,
                    items    = items,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }
}

// ─── InvoiceDetailContent ─────────────────────
@Composable
private fun InvoiceDetailContent(
    invoice:  Invoice,
    items:    List<InvoiceItem>,
    modifier: Modifier = Modifier
) {
    val isPaid      = invoice.status == InvoiceStatus.CLOSED_CASH ||
                      invoice.status == InvoiceStatus.CLOSED_CREDIT
    val statusColor = if (isPaid) AutoDriveFinance.Withdrawable else MaterialTheme.colorScheme.error
    val statusLabel = if (isPaid) "✅ مسددة" else "❌ غير مسددة"

    LazyColumn(
        modifier            = modifier,
        contentPadding      = PaddingValues(AutoDriveSpace.XL),
        verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.LG)
    ) {
        // ── رأس: التاريخ والحالة ──────────────────
        item {
            Surface(
                shape    = AutoDriveRadius.LargeShape,
                color    = statusColor.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(AutoDriveBorder.Accent, statusColor.copy(alpha = 0.4f), AutoDriveRadius.LargeShape)
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AutoDriveSpace.XL, vertical = AutoDriveSpace.LG),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.XS)) {
                        Text(
                            text  = FormatUtils.formatDate(invoice.createdAt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AutoDriveText.Secondary
                        )
                        Text(
                            text       = statusLabel,
                            style      = MaterialTheme.typography.titleSmall,
                            color      = statusColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text       = "فاتورة #${invoice.invoiceNumber}",
                        style      = MaterialTheme.typography.titleMedium,
                        color      = AutoDriveText.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ── البنود ───────────────────────────────
        if (items.isNotEmpty()) {
            item {
                Text(
                    text       = "البنود",
                    style      = MaterialTheme.typography.titleSmall,
                    color      = AutoDriveText.Secondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(horizontal = AutoDriveSpace.XS)
                )
            }
            items(items) { item ->
                InvoiceItemRow(item)
            }
        }

        // ── الإجماليات ───────────────────────────
        item {
            DetailSection(title = "الإجماليات") {
                DetailRow(
                    label      = "إجمالي الفاتورة",
                    value      = FormatUtils.formatSar(invoice.totalAmount),
                    valueColor = AutoDriveText.Primary,
                    bold       = true
                )
                DetailRow(
                    label      = "العمولة المكتسبة",
                    value      = FormatUtils.formatSar(invoice.commission),
                    valueColor = AutoDriveBrand.Primary,
                    bold       = true
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─── InvoiceItemRow ──────────────────────────
@Composable
private fun InvoiceItemRow(item: InvoiceItem) {
    Surface(
        shape    = AutoDriveRadius.MediumShape,
        color    = AutoDriveSurface.Raised,
        modifier = Modifier
            .fillMaxWidth()
            .border(AutoDriveBorder.Thin, AutoDriveBorderColor.Default, AutoDriveRadius.MediumShape)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = AutoDriveSpace.LG, vertical = AutoDriveSpace.MD),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.XS)
            ) {
                Text(
                    text       = item.itemName,
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = AutoDriveText.Primary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text  = "${item.quantity} × ${FormatUtils.formatSar(item.sellPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AutoDriveText.Secondary
                )
            }
            Text(
                text       = FormatUtils.formatSar(item.totalPrice),
                style      = MaterialTheme.typography.bodyMedium,
                color      = AutoDriveText.Primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─── DetailSection ───────────────────────────
@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape    = AutoDriveRadius.LargeShape,
        color    = AutoDriveSurface.Raised,
        modifier = Modifier
            .fillMaxWidth()
            .border(AutoDriveBorder.Thin, AutoDriveBorderColor.Default, AutoDriveRadius.LargeShape)
    ) {
        Column(modifier = Modifier.padding(AutoDriveSpace.LG)) {
            Text(
                text       = title,
                style      = MaterialTheme.typography.titleSmall,
                color      = AutoDriveText.Secondary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(AutoDriveSpace.MD))
            content()
        }
    }
}

// ─── DetailRow ───────────────────────────────
@Composable
private fun DetailRow(
    label:      String,
    value:      String,
    valueColor: Color = AutoDriveText.Primary,
    bold:       Boolean = false
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AutoDriveText.Secondary)
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodyMedium,
            color      = valueColor,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
    AutoDriveDivider()
}

// ─── Invoice → CommissionEntry (للطباعة) ────
private fun Invoice.toCommissionEntry(status: CommissionStatus) = CommissionEntry(
    invoiceId     = id,
    invoiceNumber = invoiceNumber,
    amount        = commission,
    status        = status,
    createdAt     = createdAt
)

private fun CommissionEntry.toPdfEntry(): InvoicePdfEntry = InvoicePdfEntry(
    invoiceNumber = invoiceNumber,
    createdAt = createdAt,
    amount = amount,
)

private fun InvoiceItem.toPdfItem(): InvoicePdfItem = InvoicePdfItem(
    itemName = itemName,
    quantity = quantity,
    sellPrice = sellPrice,
    totalPrice = totalPrice,
)
