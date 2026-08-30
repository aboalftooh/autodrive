package com.autodrive.app.feature.reports.presentation.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.feature.commission.domain.CommissionCalculator
import com.autodrive.app.feature.commission.domain.model.CommissionEntry
import com.autodrive.app.feature.commission.domain.model.Invoice
import com.autodrive.app.core.model.money.Money
import com.autodrive.app.feature.commission.domain.usecase.ObserveCommissionsUseCase
import com.autodrive.app.feature.commission.domain.usecase.ObserveInvoicesUseCase
import com.autodrive.app.core.common.format.FormatUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import com.autodrive.app.core.designsystem.components.actions.AutoDriveTextButton
import com.autodrive.app.core.designsystem.foundation.border.AutoDriveBorder
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.radius.AutoDriveRadius
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.patterns.header.ScreenHeader

private const val PAGE = 10

data class WeeklyPurchaseRow(
    val weekLabel: String,
    val totalPurchases: Money,
    val totalCommissions: Money
)

// ── ViewModel ──────────────────────────────────────
@HiltViewModel
class WeeklyCommissionsViewModel @Inject constructor(
    observeCommissions: ObserveCommissionsUseCase,
    observeInvoices: ObserveInvoicesUseCase,
    private val calculator: CommissionCalculator
) : ViewModel() {

    val rows = combine(observeCommissions(), observeInvoices()) { commResult, invoices ->
        @Suppress("UNCHECKED_CAST")
        val entries = (commResult as Pair<*, *>).second as List<*>
        buildWeeklyRows(entries.filterIsInstance<CommissionEntry>(), invoices)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @Suppress("DEPRECATION")
    private fun buildWeeklyRows(
        entries: List<CommissionEntry>,
        invoices: List<Invoice>
    ): List<WeeklyPurchaseRow> {
        if (entries.isEmpty() && invoices.isEmpty()) return emptyList()

        val allMs    = entries.map { calculator.parseIsoMs(it.createdAt) } +
                       invoices.map { calculator.parseIsoMs(it.createdAt) }
        val earliest = allMs.minOrNull() ?: return emptyList()

        val weekMs   = 7L * 24 * 3_600_000L
        val labelFmt = SimpleDateFormat("d/M", Locale("ar"))
        var weekEnd  = calculator.fallbackLastFriday9AM() + weekMs
        val result   = mutableListOf<WeeklyPurchaseRow>()

        while (weekEnd > earliest) {
            val weekStart = weekEnd - weekMs

            val purchases   = invoices
                .filter { calculator.parseIsoMs(it.createdAt) in weekStart until weekEnd }
                .let { Money.sum(it.map { invoice -> invoice.totalAmount }) }
            val commissions = entries
                .filter { calculator.parseIsoMs(it.createdAt) in weekStart until weekEnd }
                .let { Money.sum(it.map { entry -> entry.amount }) }

            if (purchases.isPositive() || commissions.isPositive()) {
                result.add(WeeklyPurchaseRow(
                    weekLabel        = "${labelFmt.format(weekStart)} ← ${labelFmt.format(weekEnd)}",
                    totalPurchases   = purchases,
                    totalCommissions = commissions
                ))
            }
            weekEnd = weekStart
        }
        return result
    }
}

// ── State for visible count ─────────────────────────
// ── Screen ─────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyCommissionsScreen(
    onBack: () -> Unit,
    viewModel: WeeklyCommissionsViewModel = hiltViewModel()
) {
    val allRows by viewModel.rows.collectAsState()
    var visibleCount by remember { mutableIntStateOf(PAGE) }

    Scaffold(
        containerColor = AutoDriveSurface.Canvas,
        topBar = {
            ScreenHeader(
                title = "العمولات الأسبوعية",
                onBack = onBack,
            )
        }
    ) { padding ->
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            if (allRows.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text      = "لا توجد بيانات",
                        color     = AutoDriveText.Disabled,
                        style     = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize().padding(padding),
                    contentPadding      = PaddingValues(horizontal = AutoDriveSpace.LG, vertical = AutoDriveSpace.MD),
                    verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM)
                ) {
                    items(allRows.take(visibleCount)) { row ->
                        WeeklyCommRow(row)
                    }
                    if (visibleCount < allRows.size) {
                        item {
                            AutoDriveTextButton(
                                text = "الأقدم",
                                onClick = { visibleCount += PAGE },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyCommRow(row: WeeklyPurchaseRow) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AutoDriveSurface.Raised, AutoDriveRadius.LargeShape)
            .border(AutoDriveBorder.Thin, AutoDriveFinance.Pending.copy(alpha = 0.22f), AutoDriveRadius.LargeShape)
            .padding(horizontal = AutoDriveSpace.LG, vertical = AutoDriveSpace.MD),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text  = "الأسبوع ${row.weekLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = AutoDriveText.Secondary
        )
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.Optical)) {
                Text(
                    text  = "مشترياتك",
                    style = MaterialTheme.typography.labelSmall,
                    color = AutoDriveText.Secondary
                )
                Text(
                    text       = FormatUtils.formatSar(row.totalPurchases),
                    style      = MaterialTheme.typography.titleMedium,
                    color      = AutoDriveFinance.Withdrawable,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.Optical)
            ) {
                Text(
                    text  = "عمولتك",
                    style = MaterialTheme.typography.labelSmall,
                    color = AutoDriveText.Secondary
                )
                Text(
                    text       = FormatUtils.formatSar(row.totalCommissions),
                    style      = MaterialTheme.typography.titleMedium,
                    color      = AutoDriveFinance.Pending,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
