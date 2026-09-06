package com.autodrive.app.feature.commission.presentation

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.autodrive.app.core.common.format.FormatUtils
import com.autodrive.app.core.designsystem.components.navigation.AutoDriveBackHeader
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveStatus
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.radius.AutoDriveRadius
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveContentWidth
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.model.money.Money
import com.autodrive.app.feature.commission.domain.model.CommissionEntry
import com.autodrive.app.feature.commission.domain.model.CommissionPageCursor
import com.autodrive.app.feature.commission.domain.usecase.GetPendingCommissionListSummaryUseCase
import com.autodrive.app.feature.commission.domain.usecase.GetPendingCommissionPageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PENDING_PAGE_SIZE = 10

internal data class PendingCommissionsUiState(
    val totalCount: Long = 0L,
    val totalAmount: Money = Money.ZERO,
    val entries: List<CommissionEntry> = emptyList(),
    val nextCursor: CommissionPageCursor? = null,
    val hasMore: Boolean = false,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class PendingCommissionsViewModel @Inject constructor(
    private val getPendingCommissionPage: GetPendingCommissionPageUseCase,
    private val getPendingCommissionListSummary: GetPendingCommissionListSummaryUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PendingCommissionsUiState())
    val state: StateFlow<PendingCommissionsUiState> = _state.asStateFlow()

    init {
        loadInitial()
    }

    fun retry() = loadInitial()

    fun loadMore() {
        val current = _state.value
        if (current.loading || current.loadingMore || !current.hasMore || current.nextCursor == null) return

        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true, error = null) }
            runCatching { getPendingCommissionPage(current.nextCursor, PENDING_PAGE_SIZE) }
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            entries = it.entries + page.entries,
                            nextCursor = page.nextCursor,
                            hasMore = page.hasMore,
                            loadingMore = false,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            loadingMore = false,
                            error = error.message ?: "تعذّر تحميل المزيد",
                        )
                    }
                }
        }
    }

    private fun loadInitial() {
        viewModelScope.launch {
            _state.value = PendingCommissionsUiState(loading = true)
            runCatching {
                coroutineScope {
                    val summaryDeferred = async { getPendingCommissionListSummary() }
                    val pageDeferred = async { getPendingCommissionPage(null, PENDING_PAGE_SIZE) }
                    summaryDeferred.await() to pageDeferred.await()
                }
            }.onSuccess { (summary, page) ->
                _state.value = PendingCommissionsUiState(
                    totalCount = summary.totalCount,
                    totalAmount = summary.totalAmount,
                    entries = page.entries,
                    nextCursor = page.nextCursor,
                    hasMore = page.hasMore,
                    loading = false,
                )
            }.onFailure { error ->
                _state.value = PendingCommissionsUiState(
                    loading = false,
                    error = error.message ?: "تعذّر تحميل العمولات المعلقة",
                )
            }
        }
    }
}

@Composable
fun PendingCommissionsScreen(
    onBack: () -> Unit,
    onOpenInvoice: (String) -> Unit,
    viewModel: PendingCommissionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = AutoDriveSurface.Canvas,
            topBar = {
                AutoDriveBackHeader(
                    title = "العمولات المعلقة",
                    onBack = onBack,
                )
            },
        ) { scaffoldPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                when {
                    state.loading -> PendingInitialLoading()
                    state.error != null && state.entries.isEmpty() -> PendingInitialError(
                        message = state.error,
                        onRetry = viewModel::retry,
                    )
                    else -> PendingCommissionsContent(
                        state = state,
                        onOpenInvoice = onOpenInvoice,
                        onLoadMore = viewModel::loadMore,
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingCommissionsContent(
    state: PendingCommissionsUiState,
    onOpenInvoice: (String) -> Unit,
    onLoadMore: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = AutoDriveContentWidth.Dashboard),
        contentPadding = PaddingValues(
            start = AutoDriveSpace.LG,
            end = AutoDriveSpace.LG,
            top = AutoDriveSpace.MD,
            bottom = AutoDriveSpace.X3L,
        ),
        verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD),
    ) {
        item {
            PendingSummaryCard(
                totalCount = state.totalCount,
                totalAmount = state.totalAmount,
            )
        }

        if (state.entries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "لا توجد عمولات معلقة",
                        color = AutoDriveText.Secondary,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        } else {
            items(state.entries, key = { it.invoiceId }) { entry ->
                PendingCommissionCard(
                    entry = entry,
                    onClick = { onOpenInvoice(entry.invoiceId) },
                )
            }
        }

        if (state.hasMore) {
            item {
                PendingLoadMoreButton(
                    loading = state.loadingMore,
                    onClick = onLoadMore,
                )
            }
        }

        if (state.error != null && state.entries.isNotEmpty()) {
            item {
                Text(
                    text = state.error,
                    modifier = Modifier.fillMaxWidth(),
                    color = AutoDriveStatus.Error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PendingSummaryCard(
    totalCount: Long,
    totalAmount: Money,
) {
    val pendingColor = AutoDriveFinance.Pending
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AutoDriveRadius.XL),
        color = AutoDriveSurface.Base,
        border = BorderStroke(1.dp, pendingColor.copy(alpha = 0.9f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AutoDriveSpace.XL, vertical = AutoDriveSpace.LG),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD),
        ) {
            Surface(
                shape = RoundedCornerShape(AutoDriveRadius.LG),
                color = pendingColor.copy(alpha = 0.12f),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = pendingColor,
                    modifier = Modifier.padding(AutoDriveSpace.MD).size(30.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.XS),
            ) {
                Text(
                    text = "إجمالي العمولات المعلقة",
                    color = AutoDriveText.Secondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = FormatUtils.formatSarNoLabel(totalAmount),
                    color = pendingColor,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = "${formatPendingCount(totalCount)} عمولة",
                    color = AutoDriveText.Secondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun PendingCommissionCard(
    entry: CommissionEntry,
    onClick: () -> Unit,
) {
    val pendingColor = AutoDriveFinance.Pending
    val reason = pendingReason(entry)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(AutoDriveRadius.LG),
        color = AutoDriveSurface.Raised,
        border = BorderStroke(1.dp, AutoDriveBorderColor.Default),
    ) {
        Column(
            modifier = Modifier.padding(AutoDriveSpace.MD),
            verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = FormatUtils.formatSarNoLabel(entry.pendingAmount),
                        color = pendingColor,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        text = "INV-${entry.invoiceNumber}  ·  ${formatPendingDate(entry.createdAt)}",
                        color = AutoDriveText.Secondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = pendingColor.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, pendingColor.copy(alpha = 0.85f)),
                ) {
                    Text(
                        text = "معلقة",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = pendingColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = "فتح الفاتورة",
                    tint = AutoDriveText.Primary,
                    modifier = Modifier.size(22.dp),
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AutoDriveRadius.MD),
                color = pendingColor.copy(alpha = 0.08f),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = AutoDriveSpace.MD, vertical = AutoDriveSpace.SM),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = reason.title,
                        color = AutoDriveText.Primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    reason.detail?.let { detail ->
                        Text(
                            text = detail,
                            color = AutoDriveText.Secondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

private data class PendingReason(
    val title: String,
    val detail: String? = null,
)

private fun pendingReason(entry: CommissionEntry): PendingReason = when (entry.reasonCode) {
    "CREDIT_NOT_FULLY_PAID" -> PendingReason(
        title = "الفاتورة الآجلة لم تُسدّد بالكامل",
        detail = entry.creditRemainingAmount.takeIf { it > Money.ZERO }?.let {
            "متبقي من الفاتورة: ${FormatUtils.formatSarNoLabel(it)}"
        },
    )
    "WEEKLY_CYCLE" -> PendingReason("بانتظار إغلاق دورة العمولة")
    "INVOICE_NOT_CLOSED" -> PendingReason("بانتظار إغلاق الفاتورة")
    "AWAITING_PROMOTION" -> PendingReason("بانتظار تحديث الاستحقاق")
    else -> PendingReason(entry.reasonMessage?.takeIf { it.isNotBlank() } ?: "بانتظار اكتمال شروط الاستحقاق")
}

@Composable
private fun PendingLoadMoreButton(
    loading: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !loading, role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(AutoDriveRadius.LG),
        color = AutoDriveSurface.Raised,
        border = BorderStroke(1.dp, AutoDriveBorderColor.Default),
    ) {
        Row(
            modifier = Modifier.padding(vertical = AutoDriveSpace.MD),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = AutoDriveFinance.Pending,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    tint = AutoDriveFinance.Pending,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(AutoDriveSpace.SM))
                Text(
                    text = "تحميل المزيد",
                    color = AutoDriveFinance.Pending,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun PendingInitialLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = AutoDriveFinance.Pending)
    }
}

@Composable
private fun PendingInitialError(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AutoDriveSpace.XL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD),
    ) {
        Text(
            text = message,
            color = AutoDriveStatus.Error,
            textAlign = TextAlign.Center,
        )
        Surface(
            modifier = Modifier.clickable(role = Role.Button, onClick = onRetry),
            shape = RoundedCornerShape(AutoDriveRadius.MD),
            color = AutoDriveSurface.Raised,
            border = BorderStroke(1.dp, AutoDriveBorderColor.Default),
        ) {
            Text(
                text = "إعادة المحاولة",
                modifier = Modifier.padding(horizontal = AutoDriveSpace.LG, vertical = AutoDriveSpace.SM),
                color = AutoDriveFinance.Pending,
            )
        }
    }
}

private fun formatPendingDate(value: String): String {
    val date = parsePendingDate(value) ?: return value.substringBefore('T')
    return "${date.dayOfMonth.toString().padStart(2, '0')} ${PENDING_ARABIC_MONTHS[date.monthValue - 1]} ${date.year}"
}

private fun parsePendingDate(value: String): java.time.LocalDate? = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
}.recoverCatching {
    ZonedDateTime.parse(value).withZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
}.recoverCatching {
    Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDate()
}.getOrNull()

private fun formatPendingCount(value: Long): String = NumberFormat.getIntegerInstance(Locale.US).format(value)

private val PENDING_ARABIC_MONTHS = listOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر",
)
