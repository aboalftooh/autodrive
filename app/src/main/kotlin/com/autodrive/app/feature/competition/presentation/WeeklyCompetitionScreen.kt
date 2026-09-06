package com.autodrive.app.feature.competition.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import com.autodrive.app.core.designsystem.components.data.AutoDriveDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import com.autodrive.app.core.common.format.FormatUtils
import com.autodrive.app.core.designsystem.components.AutoDriveAccent
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveSecondaryButton
import com.autodrive.app.core.designsystem.components.containers.AutoDriveCard
import com.autodrive.app.core.designsystem.components.containers.AutoDriveHighlightCard
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveStatus
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.patterns.header.ScreenHeader
import com.autodrive.app.core.designsystem.patterns.state.ErrorScreen
import com.autodrive.app.core.designsystem.patterns.state.LoadingScreen
import com.autodrive.app.feature.competition.domain.model.CompetitionAvailability
import com.autodrive.app.feature.competition.domain.model.LeaderboardEntry
import com.autodrive.app.feature.competition.domain.model.WeeklyCompetitionData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyCompetitionScreen(
    availability: CompetitionAvailability,
    onBack: () -> Unit,
    onNavigateCompetitionHistory: () -> Unit = {},
    onNavigateWinWeeks: () -> Unit = {},
    viewModel: WeeklyCompetitionViewModel = hiltViewModel(),
) {
    Scaffold(
        containerColor = AutoDriveSurface.Canvas,
        topBar = { ScreenHeader(title = "المسابقة الأسبوعية", onBack = onBack) },
    ) { padding ->
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            when (availability) {
                CompetitionAvailability.DISABLED -> DisabledCompetitionContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )

                CompetitionAvailability.LOCKED -> LockedCompetitionContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )

                CompetitionAvailability.ACTIVE -> {
                    LaunchedEffect(Unit) { viewModel.onActiveEntry() }
                    val state by viewModel.uiState.collectAsState()
                    ActiveCompetitionContent(
                        state = state,
                        onRefresh = viewModel::refresh,
                        onNavigateCompetitionHistory = onNavigateCompetitionHistory,
                        onNavigateWinWeeks = onNavigateWinWeeks,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    )
                }
            }
        }
    }
}

@Composable
private fun DisabledCompetitionContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = AutoDriveSpace.LG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "المسابقة غير متاحة حالياً",
            style = MaterialTheme.typography.titleLarge,
            color = AutoDriveText.Primary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LockedCompetitionContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = AutoDriveSpace.LG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AutoDriveHighlightCard(
            accent = AutoDriveAccent.Insight,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("المسابقة الأسبوعية", style = MaterialTheme.typography.titleLarge, color = AutoDriveText.Primary)
            Text("قريباً", style = MaterialTheme.typography.titleMedium, color = AutoDriveText.Primary)
            Text("نجهز منافسة عادلة وممتعة.", style = MaterialTheme.typography.bodyMedium, color = AutoDriveText.Secondary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveCompetitionContent(
    state: WeeklyCompetitionUiState,
    onRefresh: () -> Unit,
    onNavigateCompetitionHistory: () -> Unit,
    onNavigateWinWeeks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val data = state.data
    when {
        data == null && state.isLoading -> LoadingScreen(modifier = modifier, label = "جاري تحميل المسابقة")
        data == null && state.errorMessage != null -> ErrorScreen(
            title = "تعذر تحميل المسابقة",
            body = state.errorMessage,
            retryLabel = "إعادة المحاولة",
            onRetry = onRefresh,
            modifier = modifier,
        )
        data == null -> LoadingScreen(modifier = modifier, label = "جاري تحميل المسابقة")
        else -> PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier,
        ) {
            ActiveCompetitionList(
                data = data,
                warning = state.errorMessage,
                onNavigateCompetitionHistory = onNavigateCompetitionHistory,
                onNavigateWinWeeks = onNavigateWinWeeks,
            )
        }
    }
}

@Composable
private fun ActiveCompetitionList(
    data: WeeklyCompetitionData,
    warning: String?,
    onNavigateCompetitionHistory: () -> Unit,
    onNavigateWinWeeks: () -> Unit,
) {
    val ordered = data.entries.sortedBy { it.rank }
    val meIndex = ordered.indexOfFirst { it.isMe }
    val me = ordered.getOrNull(meIndex)
    val previous = if (meIndex > 0) ordered[meIndex - 1] else null
    val difference = if (me != null && previous != null) previous.totalAmount - me.totalAmount else null
    val topFive = ordered.take(5)
    val meOutsideTopFive = me?.takeIf { it.rank > 5 }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(AutoDriveSpace.LG),
        verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD),
    ) {
        item {
            PersonalHero(
                me = me,
                difference = difference?.takeIf { it.isPositive() },
            )
        }

        if (warning != null) {
            item {
                Text(
                    text = warning,
                    style = MaterialTheme.typography.bodySmall,
                    color = AutoDriveStatus.Warning,
                )
            }
        }

        if (data.isFromCache) {
            item {
                Text(
                    text = "آخر ترتيب محفوظ",
                    style = MaterialTheme.typography.labelMedium,
                    color = AutoDriveText.Secondary,
                )
            }
        }

        item {
            Text(
                text = "أفضل 5",
                style = MaterialTheme.typography.titleMedium,
                color = AutoDriveText.Primary,
                fontWeight = FontWeight.Bold,
            )
        }

        items(topFive, key = { "top-${it.rank}" }) { entry ->
            LeaderboardRow(entry)
        }

        if (meOutsideTopFive != null) {
            item { AutoDriveDivider() }
            item { LeaderboardRow(meOutsideTopFive) }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM)) {
                AutoDrivePrimaryButton(
                    text = "سجل مشاركاتي",
                    onClick = onNavigateCompetitionHistory,
                    modifier = Modifier.fillMaxWidth(),
                )
                AutoDriveSecondaryButton(
                    text = "أسابيع الفوز",
                    onClick = onNavigateWinWeeks,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PersonalHero(
    me: LeaderboardEntry?,
    difference: com.autodrive.app.core.model.money.Money?,
) {
    AutoDriveHighlightCard(
        accent = AutoDriveAccent.Active,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (me == null) {
            Text("لم تدخل المنافسة بعد", style = MaterialTheme.typography.titleLarge, color = AutoDriveText.Primary)
            Text(
                "ابدأ بمشتريات مؤهلة هذا الأسبوع للظهور في الترتيب.",
                style = MaterialTheme.typography.bodyMedium,
                color = AutoDriveText.Secondary,
            )
        } else {
            Text("مركزك هذا الأسبوع", style = MaterialTheme.typography.labelLarge, color = AutoDriveText.Secondary)
            Text("#${me.rank}", style = MaterialTheme.typography.displaySmall, color = AutoDriveText.Primary)
            Text(
                "مشترياتك المؤهلة: ${FormatUtils.formatSar(me.totalAmount)}",
                style = MaterialTheme.typography.bodyLarge,
                color = AutoDriveText.Primary,
            )
            if (difference != null) {
                Text(
                    "يفصلك ${FormatUtils.formatSar(difference)} عن المركز السابق",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AutoDriveText.Secondary,
                )
            }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry) {
    AutoDriveCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM),
            ) {
                Text(
                    text = "#${entry.rank}",
                    style = MaterialTheme.typography.titleMedium,
                    color = AutoDriveText.Primary,
                    fontWeight = FontWeight.Bold,
                )
                if (entry.isMe) {
                    Text("أنت", style = MaterialTheme.typography.labelMedium, color = AutoDriveText.Secondary)
                }
            }
            Text(
                text = FormatUtils.formatSar(entry.totalAmount),
                style = MaterialTheme.typography.bodyLarge,
                color = AutoDriveText.Primary,
            )
        }
    }
}
