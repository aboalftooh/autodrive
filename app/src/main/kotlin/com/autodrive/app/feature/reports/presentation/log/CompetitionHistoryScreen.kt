package com.autodrive.app.feature.reports.presentation.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.autodrive.app.core.common.format.FormatUtils
import com.autodrive.app.core.designsystem.components.actions.AutoDriveTextButton
import com.autodrive.app.core.designsystem.foundation.border.AutoDriveBorder
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.radius.AutoDriveRadius
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.patterns.header.ScreenHeader
import com.autodrive.app.core.designsystem.patterns.state.ErrorScreen
import com.autodrive.app.feature.competition.domain.model.WeeklyRankingRow
import com.autodrive.app.feature.competition.domain.repository.WeeklyCompetitionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val COMP_PAGE = 10

@HiltViewModel
class CompetitionHistoryViewModel @Inject constructor(
    private val repository: WeeklyCompetitionRepository
) : ViewModel() {

    private val _rankings = MutableStateFlow<List<WeeklyRankingRow>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _hasMore = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private var _offset = 0

    val rankings = _rankings.asStateFlow()
    val isLoading = _isLoading.asStateFlow()
    val hasMore = _hasMore.asStateFlow()
    val errorMessage = _errorMessage.asStateFlow()

    init {
        loadMore()
    }

    fun loadMore() {
        if (_isLoading.value || !_hasMore.value) return
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            runCatching { repository.getCompetitionHistory(COMP_PAGE, _offset) }
                .onSuccess { rows ->
                    _rankings.update { it + rows }
                    _offset += rows.size
                    _hasMore.value = rows.size >= COMP_PAGE
                }
                .onFailure {
                    _errorMessage.value = "تعذر تحميل سجل المشاركات"
                }
            _isLoading.value = false
        }
    }
}

@Composable
fun CompetitionHistoryScreen(
    onBack: () -> Unit,
    viewModel: CompetitionHistoryViewModel = hiltViewModel()
) {
    val rankings by viewModel.rankings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        containerColor = AutoDriveSurface.Canvas,
        topBar = {
            ScreenHeader(
                title = "سجل مشاركاتي",
                onBack = onBack,
            )
        }
    ) { padding ->
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            when {
                rankings.isEmpty() && isLoading -> Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = AutoDriveFinance.Pending)
                }

                rankings.isEmpty() && errorMessage != null -> ErrorScreen(
                    title = "تعذر تحميل سجل المشاركات",
                    body = "تحقق من الاتصال وحاول مجدداً",
                    retryLabel = "إعادة المحاولة",
                    onRetry = viewModel::loadMore,
                    modifier = Modifier.fillMaxSize().padding(padding),
                )

                rankings.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "لا توجد مشاركات سابقة\nفي المسابقة الأسبوعية",
                        color = AutoDriveText.Disabled,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = AutoDriveSpace.LG, vertical = AutoDriveSpace.MD),
                    verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM)
                ) {
                    items(rankings) { row ->
                        CompetitionWeekRow(row)
                    }
                    if (hasMore || isLoading || errorMessage != null) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(AutoDriveSpace.SM),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                when {
                                    isLoading -> CircularProgressIndicator(
                                        color = AutoDriveFinance.Pending,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                    )

                                    errorMessage != null -> AutoDriveTextButton(
                                        text = "إعادة المحاولة",
                                        onClick = viewModel::loadMore,
                                    )

                                    hasMore -> AutoDriveTextButton(
                                        text = "الأقدم",
                                        onClick = viewModel::loadMore,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompetitionWeekRow(row: WeeklyRankingRow) {
    val isChampion = row.myRank == 1
    val rankColor = if (isChampion) AutoDriveFinance.Pending else AutoDriveText.Primary
    val borderColor = if (isChampion) {
        AutoDriveFinance.Pending.copy(alpha = 0.45f)
    } else {
        AutoDriveFinance.Pending.copy(alpha = 0.15f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AutoDriveSurface.Raised, AutoDriveRadius.LargeShape)
            .border(AutoDriveBorder.Thin, borderColor, AutoDriveRadius.LargeShape)
            .padding(horizontal = AutoDriveSpace.LG, vertical = AutoDriveSpace.MD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "الأسبوع من ${row.weekStartLabel} إلى ${row.weekEndLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = AutoDriveText.Secondary,
            )
            Text(
                text = "مشترياتك: ${FormatUtils.formatSar(row.myTotal)}",
                style = MaterialTheme.typography.bodyMedium,
                color = AutoDriveText.Primary,
            )
        }

        Spacer(Modifier.width(AutoDriveSpace.SM))

        if (row.myRank == null) {
            Text(
                text = "لم تشارك",
                style = MaterialTheme.typography.labelMedium,
                color = AutoDriveText.Secondary,
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.XS)
            ) {
                if (isChampion) Text("👑", style = MaterialTheme.typography.labelLarge)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "#${row.myRank}",
                        style = MaterialTheme.typography.titleMedium,
                        color = rankColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "ترتيبك",
                        style = MaterialTheme.typography.labelSmall,
                        color = AutoDriveText.Secondary
                    )
                }
            }
        }
    }
}
