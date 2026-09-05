package com.autodrive.app.feature.reports.presentation.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.feature.commission.domain.CommissionCalculator
import com.autodrive.app.feature.commission.domain.model.Invoice
import com.autodrive.app.feature.commission.domain.usecase.ObserveInvoicesUseCase
import com.autodrive.app.core.common.format.FormatUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import com.autodrive.app.core.designsystem.components.actions.AutoDriveTextButton
import com.autodrive.app.core.designsystem.foundation.border.AutoDriveBorder
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBrand
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.radius.AutoDriveRadius
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.patterns.header.ScreenHeader

// ── ViewModel ──────────────────────────────────────
@HiltViewModel
class InvoiceListViewModel @Inject constructor(
    observeInvoices: ObserveInvoicesUseCase,
    private val calculator: CommissionCalculator,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val weekMode: String = savedStateHandle["weekMode"] ?: "all"
    private val weekOffset = MutableStateFlow(if (weekMode == "previous") 1 else 0)

    @Suppress("DEPRECATION")
    val invoices = combine(observeInvoices(), weekOffset) { list, offset ->
            val (start, end) = weekRange(offset)
            list.filter { inv ->
                calculator.parseIsoMs(inv.createdAt).let { it in start until end }
            }.sortedByDescending { it.createdAt }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @Suppress("DEPRECATION")
    val weekRangeLabel = weekOffset
        .map { offset -> weekRange(offset).let { (start, end) -> buildWeekLabel(start, end) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun showOlderWeek() {
        weekOffset.update { it + 1 }
    }

    @Suppress("DEPRECATION")
    private fun weekRange(offset: Int): Pair<Long, Long> {
        val currentStart = calculator.fallbackLastFriday9AM()
        val start = currentStart - offset * 7L * 24 * 3_600_000L
        val end = start + 7L * 24 * 3_600_000L
        return start to end
    }

    private fun buildWeekLabel(startMs: Long, endMs: Long): String {
        val fmt = SimpleDateFormat("d/M/yyyy", Locale("ar"))
        return "من ${fmt.format(Date(startMs))} إلى ${fmt.format(Date(endMs))}"
    }
}

// ── Screen ─────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceListScreen(
    weekMode: String = "all",
    onBack: () -> Unit,
    onNavigateInvoiceDetail: (String) -> Unit,
    viewModel: InvoiceListViewModel = hiltViewModel()
) {
    val invoices by viewModel.invoices.collectAsState()
    val weekRangeLabel by viewModel.weekRangeLabel.collectAsState()
    val headerContext: (@Composable () -> Unit)? = if (weekRangeLabel.isNotBlank()) {
        {
            Text(
                text = weekRangeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = AutoDriveText.Secondary,
            )
        }
    } else {
        null
    }

    Scaffold(
        containerColor = AutoDriveSurface.Canvas,
        topBar = {
            ScreenHeader(
                title = "فواتير هذا الأسبوع",
                onBack = onBack,
                context = headerContext,
            )
        }
    ) { padding ->
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(padding),
                contentPadding      = PaddingValues(horizontal = AutoDriveSpace.LG, vertical = AutoDriveSpace.MD),
                verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM)
            ) {
                if (invoices.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text      = "لا توجد فواتير في هذا الأسبوع",
                                color     = AutoDriveText.Disabled,
                                style     = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(invoices, key = { it.id }) { inv ->
                        InvoiceListRow(invoice = inv) { onNavigateInvoiceDetail(inv.id) }
                    }
                }
                item {
                    AutoDriveTextButton(
                        text = "الأسبوع الأقدم",
                        onClick = { viewModel.showOlderWeek() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
fun InvoiceListRow(invoice: Invoice, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AutoDriveSurface.Raised, AutoDriveRadius.LargeShape)
            .border(AutoDriveBorder.Thin, AutoDriveFinance.Pending.copy(alpha = 0.25f), AutoDriveRadius.LargeShape)
            .clickable(onClick = onClick)
            .padding(horizontal = AutoDriveSpace.LG, vertical = AutoDriveSpace.MD),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text       = "فاتورة #${invoice.invoiceNumber}",
                style      = MaterialTheme.typography.labelLarge,
                color      = AutoDriveText.Primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text  = FormatUtils.formatDate(invoice.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = AutoDriveText.Secondary
            )
        }
        Text(
            text       = FormatUtils.formatSar(invoice.totalAmount),
            style      = MaterialTheme.typography.titleMedium,
            color      = AutoDriveBrand.Primary,
            fontWeight = FontWeight.Bold
        )
    }
}
