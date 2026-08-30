package com.autodrive.app.feature.commission.presentation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.feature.commission.domain.CommissionCalculator
import com.autodrive.app.feature.commission.domain.model.CommissionEntry
import com.autodrive.app.feature.commission.domain.model.CommissionSummary
import com.autodrive.app.core.model.money.Money
import com.autodrive.app.feature.commission.domain.usecase.ObserveCommissionsUseCase
import com.autodrive.app.core.common.format.FormatUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText

data class CommissionReportState(
    val summary: CommissionSummary? = null,
    val entries: List<CommissionEntry> = emptyList()
)

@HiltViewModel
class CommissionReportViewModel @Inject constructor(
    observeCommissions: ObserveCommissionsUseCase,
    private val calculator: CommissionCalculator
) : ViewModel() {

    val nextFriday9AmMs: Long = calculator.fallbackNextFriday9AM()

    val state = observeCommissions()
        .map { (summary, entries) ->
            CommissionReportState(
                summary = summary,
                entries = entries.sortedByDescending { calculator.parseIsoMs(it.createdAt) }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CommissionReportState())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommissionReportScreen(
    onBack: () -> Unit,
    viewModel: CommissionReportViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = AutoDriveSurface.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("تقرير عمولاتي", style = MaterialTheme.typography.titleLarge, color = AutoDriveText.Primary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "رجوع", tint = AutoDriveText.Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AutoDriveSurface.Canvas)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.summary?.let { summary ->
                item { SummaryCard(summary) }
            }
            item {
                Text("الفواتير (${state.entries.size})", color = AutoDriveText.Primary,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (state.entries.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد عمولات بعد", color = AutoDriveText.Secondary, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                items(state.entries, key = { it.invoiceId }) { entry ->
                    CommissionEntryRow(entry = entry, nextFriday9AmMs = viewModel.nextFriday9AmMs)
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: CommissionSummary) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AutoDriveSurface.Raised,
        modifier = Modifier.fillMaxWidth().border(1.dp, AutoDriveBorderColor.Default, RoundedCornerShape(18.dp))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricCell("قابل للسحب", summary.withdrawable, AutoDriveFinance.Withdrawable)
                MetricCell("معلّقة", summary.pending, AutoDriveFinance.Pending)
                MetricCell("مصروفة", summary.paid, AutoDriveText.Secondary)
            }
        }
    }
}

@Composable
private fun MetricCell(label: String, amount: Money, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AutoDriveText.Secondary)
        Text(FormatUtils.formatSar(amount), style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
    }
}
