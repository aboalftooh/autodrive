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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBrand
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
import com.autodrive.app.feature.commission.domain.model.CommissionStatus
import com.autodrive.app.feature.commission.domain.usecase.GetCommissionListSummaryUseCase
import com.autodrive.app.feature.commission.domain.usecase.GetCommissionPageUseCase
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

private const val PAGE_SIZE = 10

data class AllCommissionsUiState(
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
class AllCommissionsViewModel @Inject constructor(
    private val getCommissionPage: GetCommissionPageUseCase,
    private val getCommissionListSummary: GetCommissionListSummaryUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AllCommissionsUiState())
    val state: StateFlow<AllCommissionsUiState> = _state.asStateFlow()

    init {
        loadInitial()
    }

    fun retry() = loadInitial()

    fun loadMore() {
        val current = _state.value
        if (current.loading || current.loadingMore || !current.hasMore || current.nextCursor == null) return

        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true, error = null) }
            runCatching { getCommissionPage(current.nextCursor, PAGE_SIZE) }
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
        if (_state.value.loading && _state.value.entries.isNotEmpty()) return
        viewModelScope.launch {
            _state.value = AllCommissionsUiState(loading = true)
            runCatching {
                coroutineScope {
                    val summaryDeferred = async { getCommissionListSummary() }
                    val pageDeferred = async { getCommissionPage(null, PAGE_SIZE) }
                    summaryDeferred.await() to pageDeferred.await()
                }
            }.onSuccess { (summary, page) ->
                _state.value = AllCommissionsUiState(
                    totalCount = summary.totalCount,
                    totalAmount = summary.totalAmount,
                    entries = page.entries,
                    nextCursor = page.nextCursor,
                    hasMore = page.hasMore,
                    loading = false,
                )
            }.onFailure { error ->
                _state.value = AllCommissionsUiState(
                    loading = false,
                    error = error.message ?: "تعذّر تحميل العمولات",
                )
            }
        }
    }
}

@Composable
fun AllCommissionsScreen(
    onBack: () -> Unit,
    onOpenInvoice: (String) -> Unit,
    viewModel: AllCommissionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = AutoDriveSurface.Canvas,
            topBar = {
                AutoDriveBackHeader(
                    title = "جميع العمولات",
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
                    state.loading -> InitialLoading()
                    state.error != null && state.entries.isEmpty() -> InitialError(
                        message = state.error.orEmpty(),
                        onRetry = viewModel::retry,
                    )
                    else -> AllCommissionsContent(
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
private fun AllCommissionsContent(
    state: AllCommissionsUiState,
    onOpenInvoice: (String) -> Unit,
    onLoadMore: () -> Unit,
) {
    val groups = remember(state.entries) { groupByMonth(state.entries) }

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
        verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.LG),
    ) {
        item {
            CommissionsSummaryCard(
                totalCount = state.totalCount,
                totalAmount = state.totalAmount,
            )
        }

        if (groups.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "لا توجد عمولات بعد",
                        color = AutoDriveText.Secondary,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        } else {
            items(groups, key = { it.key }) { group ->
                CommissionMonthSection(
                    group = group,
                    onOpenInvoice = onOpenInvoice,
                )
            }
        }

        if (state.hasMore) {
            item {
                LoadMoreButton(
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
private fun CommissionsSummaryCard(
    totalCount: Long,
    totalAmount: Money,
) {
    val shape = RoundedCornerShape(AutoDriveRadius.XL)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = AutoDriveSurface.Base,
        border = BorderStroke(1.dp, AutoDriveBrand.Primary.copy(alpha = 0.9f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AutoDriveSpace.XL, vertical = AutoDriveSpace.LG),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryMetric(
                modifier = Modifier.weight(1f),
                label = "إجمالي عدد العمولات",
                value = "${formatCount(totalCount)} عمولة",
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(58.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AutoDriveBorderColor.Default,
                ) {}
            }

            SummaryMetric(
                modifier = Modifier.weight(1f),
                label = "إجمالي قيمتها",
                value = FormatUtils.formatSarNoLabel(totalAmount),
            )

            Spacer(Modifier.width(AutoDriveSpace.SM))
            Icon(
                imageVector = Icons.Rounded.ChevronLeft,
                contentDescription = null,
                tint = AutoDriveBrand.Primary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun SummaryMetric(
    modifier: Modifier,
    label: String,
    value: String,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.XS),
    ) {
        Text(
            text = label,
            color = AutoDriveText.Secondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = value,
            color = AutoDriveText.Primary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun CommissionMonthSection(
    group: CommissionMonthGroup,
    onOpenInvoice: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM),
    ) {
        Text(
            text = group.label,
            modifier = Modifier.fillMaxWidth(),
            color = AutoDriveText.Primary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Right,
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AutoDriveRadius.LG),
            color = AutoDriveSurface.Raised,
            border = BorderStroke(1.dp, AutoDriveBorderColor.Default),
        ) {
            Column {
                group.entries.forEachIndexed { index, entry ->
                    CommissionRow(
                        entry = entry,
                        onClick = { onOpenInvoice(entry.invoiceId) },
                    )
                    if (index != group.entries.lastIndex) {
                        HorizontalDivider(color = AutoDriveBorderColor.Default)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommissionRow(
    entry: CommissionEntry,
    onClick: () -> Unit,
) {
    val statusVisual = entry.status.visual()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AutoDriveSpace.MD, vertical = AutoDriveSpace.MD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM),
    ) {
        Text(
            text = formatFullDate(entry.createdAt),
            modifier = Modifier.weight(1.10f),
            color = AutoDriveText.Primary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Column(
            modifier = Modifier.weight(0.90f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "INV-${entry.invoiceNumber}",
                color = AutoDriveText.Primary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
            Text(
                text = "رقم الفاتورة",
                color = AutoDriveText.Secondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }

        CommissionStatusChip(
            label = statusVisual.label,
            color = statusVisual.color,
            modifier = Modifier.weight(0.78f),
        )

        Text(
            text = FormatUtils.formatSarNoLabel(entry.amount),
            modifier = Modifier.weight(0.84f),
            color = statusVisual.color,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )

        Icon(
            imageVector = Icons.Rounded.ChevronLeft,
            contentDescription = "فتح الفاتورة",
            tint = AutoDriveText.Primary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun CommissionStatusChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, color.copy(alpha = 0.9f)),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun LoadMoreButton(
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
                    color = AutoDriveBrand.Primary,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    tint = AutoDriveBrand.Primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(AutoDriveSpace.SM))
                Text(
                    text = "تحميل المزيد",
                    color = AutoDriveBrand.Primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun InitialLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = AutoDriveBrand.Primary)
    }
}

@Composable
private fun InitialError(
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
                color = AutoDriveBrand.Primary,
            )
        }
    }
}

private data class CommissionStatusVisual(
    val label: String,
    val color: Color,
)

private fun CommissionStatus.visual(): CommissionStatusVisual = when (this) {
    CommissionStatus.WITHDRAWABLE -> CommissionStatusVisual("متاحة", AutoDriveFinance.Withdrawable)
    CommissionStatus.PENDING -> CommissionStatusVisual("معلقة", AutoDriveFinance.Pending)
    CommissionStatus.PAID -> CommissionStatusVisual("مصروفة", AutoDriveStatus.Error)
}

private data class CommissionMonthGroup(
    val key: String,
    val label: String,
    val entries: List<CommissionEntry>,
)

private fun groupByMonth(entries: List<CommissionEntry>): List<CommissionMonthGroup> =
    entries
        .groupBy { entry ->
            val date = parseDate(entry.createdAt)
            if (date == null) "unknown" else "${date.year}-${date.monthValue.toString().padStart(2, '0')}"
        }
        .map { (key, rows) ->
            val date = rows.firstOrNull()?.createdAt?.let(::parseDate)
            CommissionMonthGroup(
                key = key,
                label = if (date == null) "" else "${ARABIC_MONTHS[date.monthValue - 1]} ${date.year}",
                entries = rows,
            )
        }

private fun formatFullDate(value: String): String {
    val date = parseDate(value) ?: return value.substringBefore('T')
    return "${date.dayOfMonth.toString().padStart(2, '0')} ${ARABIC_MONTHS[date.monthValue - 1]} ${date.year}"
}

private fun parseDate(value: String): java.time.LocalDate? = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
}.recoverCatching {
    ZonedDateTime.parse(value).withZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
}.recoverCatching {
    Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDate()
}.getOrNull()

private fun formatCount(value: Long): String = NumberFormat.getIntegerInstance(Locale.US).format(value)

private val ARABIC_MONTHS = listOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر",
)
