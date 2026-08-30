package com.autodrive.app.feature.reports.presentation.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import com.autodrive.app.core.common.format.FormatUtils
import com.autodrive.app.core.designsystem.components.AutoDriveAccent
import com.autodrive.app.core.designsystem.components.actions.AutoDriveFab
import com.autodrive.app.core.designsystem.components.containers.AutoDriveCard
import com.autodrive.app.core.designsystem.components.containers.AutoDriveMetricCard
import com.autodrive.app.core.designsystem.components.data.AutoDriveDivider
import com.autodrive.app.core.designsystem.components.data.AutoDriveListRow
import com.autodrive.app.core.designsystem.components.data.AutoDriveSectionHeader
import com.autodrive.app.core.designsystem.components.data.AutoDriveStatSize
import com.autodrive.app.core.designsystem.components.data.AutoDriveStatValue
import com.autodrive.app.core.designsystem.components.navigation.AutoDriveBottomNavigation
import com.autodrive.app.core.designsystem.components.navigation.AutoDriveNavigationItem
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveStatus
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveContentWidth
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.patterns.dashboard.DashboardHero
import com.autodrive.app.core.designsystem.patterns.header.ScreenHeader
import com.autodrive.app.core.designsystem.patterns.reports.ReportStatTile
import com.autodrive.app.core.designsystem.patterns.state.ErrorScreen
import com.autodrive.app.core.designsystem.patterns.state.LoadingScreen
import com.autodrive.app.feature.competition.domain.model.CompetitionAvailability

@Composable
fun ActivityLogScreen(
    onNavigateHome: () -> Unit,
    onNavigateRecent: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateBalance: () -> Unit = {},
    onNavigateInvoiceDetail: (invoiceId: String) -> Unit = {},
    onNavigateInvoiceList: (weekMode: String) -> Unit = {},
    onNavigateWinWeeks: () -> Unit = {},
    onNavigateWeeklyCommissions: () -> Unit = {},
    onNavigateCompetitionHistory: () -> Unit = {},
    onAddClick: () -> Unit = {},
    unreadMessages: Int = 0,
    competitionAvailability: CompetitionAvailability = CompetitionAvailability.DISABLED,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(competitionAvailability) {
        viewModel.setCompetitionActive(competitionAvailability == CompetitionAvailability.ACTIVE)
    }

    Scaffold(
        containerColor = AutoDriveSurface.Canvas,
        bottomBar = {
            AutoDriveBottomNavigation(
                items = reportRootItems(unreadMessages),
                selectedItemId = "reports",
                onItemClick = { item ->
                    when (item.id) {
                        "home" -> onNavigateHome()
                        "messages" -> onNavigateRecent()
                        "settings" -> onNavigateProfile()
                    }
                },
                centerAction = {
                    AutoDriveFab(
                        onClick = onAddClick,
                        contentDescription = "محادثة جديدة",
                        icon = Icons.Rounded.Add,
                    )
                },
            )
        },
    ) { scaffoldPadding ->
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            when (state.loadState) {
                ReportsLoadState.LOADING -> ReportsLoading(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding),
                )

                ReportsLoadState.ERROR -> ReportsError(
                    message = state.errorMessage,
                    onRetry = viewModel::retryReports,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding),
                )

                ReportsLoadState.CONTENT -> ReportsContent(
                    state = state,
                    competitionAvailability = competitionAvailability,
                    onNavigateBalance = onNavigateBalance,
                    onNavigateInvoiceList = onNavigateInvoiceList,
                    onNavigateWeeklyCommissions = onNavigateWeeklyCommissions,
                    onNavigateCompetitionHistory = onNavigateCompetitionHistory,
                    onNavigateWinWeeks = onNavigateWinWeeks,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding),
                )
            }
        }
    }
}

@Composable
private fun ReportsLoading(modifier: Modifier = Modifier) {
    Column(modifier) {
        ScreenHeader("تقاريري")
        LoadingScreen(
            label = "جاري تحميل التقارير",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@Composable
private fun ReportsError(
    message: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        ScreenHeader("تقاريري")
        ErrorScreen(
            title = "تعذر تحميل التقارير",
            body = message ?: "تحقق من الاتصال وحاول مجدداً",
            retryLabel = "إعادة المحاولة",
            onRetry = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@Composable
private fun ReportsContent(
    state: ReportsUiState,
    competitionAvailability: CompetitionAvailability,
    onNavigateBalance: () -> Unit,
    onNavigateInvoiceList: (weekMode: String) -> Unit,
    onNavigateWeeklyCommissions: () -> Unit,
    onNavigateCompetitionHistory: () -> Unit,
    onNavigateWinWeeks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        ScreenHeader("تقاريري")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = AutoDriveContentWidth.Dashboard),
                contentPadding = PaddingValues(
                    top = AutoDriveSpace.SM,
                    start = AutoDriveSpace.LG,
                    end = AutoDriveSpace.LG,
                    bottom = AutoDriveSpace.XL,
                ),
                verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.XL),
            ) {
                item { CurrentWeekHero(state) }

                item { PreviousWeekComparison(state) }

                item {
                    FinancialStatus(
                        balance = FormatUtils.formatSar(state.balance),
                        pending = FormatUtils.formatSar(state.pending),
                        onNavigateBalance = onNavigateBalance,
                    )
                }

                item {
                    ReportDetails(
                        competitionAvailability = competitionAvailability,
                        onNavigateInvoiceList = onNavigateInvoiceList,
                        onNavigateWeeklyCommissions = onNavigateWeeklyCommissions,
                        onNavigateCompetitionHistory = onNavigateCompetitionHistory,
                        onNavigateWinWeeks = onNavigateWinWeeks,
                    )
                }

                item { HistoricalAchievement(state) }
            }
        }
    }
}

@Composable
private fun CurrentWeekHero(state: ReportsUiState) {
    DashboardHero(
        label = "هذا الأسبوع",
        accent = AutoDriveAccent.Primary,
        heroContent = {
            Column(verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM)) {
                Text(
                    text = "مشترياتك",
                    style = MaterialTheme.typography.labelLarge,
                    color = AutoDriveText.Secondary,
                )
                AutoDriveStatValue(
                    value = FormatUtils.formatSar(state.currentWeekPurchases),
                    size = AutoDriveStatSize.Large,
                    accent = AutoDriveAccent.Primary,
                )
            }
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD)) {
                ResponsiveReportPair(
                    first = { metricModifier ->
                        HeroSupportingMetric(
                            label = "عمولتك",
                            value = FormatUtils.formatSar(state.currentWeekCommissions),
                            modifier = metricModifier,
                        )
                    },
                    second = { metricModifier ->
                        HeroSupportingMetric(
                            label = "الفواتير",
                            value = state.currentWeekInvoiceCount.toString(),
                            modifier = metricModifier,
                        )
                    },
                )
                if (state.currentWeekLabel.isNotBlank()) {
                    Text(
                        text = state.currentWeekLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = AutoDriveText.Secondary,
                    )
                }
            }
        },
    )
}

@Composable
private fun HeroSupportingMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.XS),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = AutoDriveText.Secondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = AutoDriveText.Primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PreviousWeekComparison(state: ReportsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD)) {
        AutoDriveSectionHeader("مقارنة بالأسبوع السابق")
        ResponsiveReportPair(
            first = { metricModifier ->
                TrendCard(
                    label = "المشتريات",
                    currentValue = FormatUtils.formatSar(state.currentWeekPurchases),
                    trend = state.purchaseTrend,
                    modifier = metricModifier,
                )
            },
            second = { metricModifier ->
                TrendCard(
                    label = "العمولات",
                    currentValue = FormatUtils.formatSar(state.currentWeekCommissions),
                    trend = state.commissionTrend,
                    modifier = metricModifier,
                )
            },
        )
    }
}

@Composable
private fun ResponsiveReportPair(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= AutoDriveContentWidth.ReportTwoColumn) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD),
            ) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD),
            ) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun TrendCard(
    label: String,
    currentValue: String,
    trend: TrendComparison,
    modifier: Modifier = Modifier,
) {
    AutoDriveCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = AutoDriveText.Secondary,
            )
            Text(
                text = currentValue,
                style = MaterialTheme.typography.titleLarge,
                color = AutoDriveText.Primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = trendLabel(trend),
                style = MaterialTheme.typography.bodyMedium,
                color = trendColor(trend.direction),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private fun trendLabel(trend: TrendComparison): String = when (trend.direction) {
    TrendDirection.UP -> "أعلى ${trend.percent ?: 0}%"
    TrendDirection.DOWN -> "أقل ${trend.percent ?: 0}%"
    TrendDirection.FLAT -> "بدون تغيير"
    TrendDirection.NEW -> "نشاط جديد"
}

private fun trendColor(direction: TrendDirection): Color = when (direction) {
    TrendDirection.UP -> AutoDriveStatus.Success
    TrendDirection.DOWN -> AutoDriveStatus.Warning
    TrendDirection.FLAT -> AutoDriveText.Secondary
    TrendDirection.NEW -> AutoDriveStatus.Info
}

@Composable
private fun FinancialStatus(
    balance: String,
    pending: String,
    onNavigateBalance: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD)) {
        AutoDriveSectionHeader("الحالة المالية")
        ResponsiveReportPair(
            first = { metricModifier ->
                ReportStatTile(
                    label = "الرصيد القابل للسحب",
                    value = balance,
                    supportingText = "عرض التفاصيل",
                    accent = AutoDriveAccent.Active,
                    icon = Icons.Outlined.AccountBalanceWallet,
                    onClick = onNavigateBalance,
                    modifier = metricModifier,
                )
            },
            second = { metricModifier ->
                AutoDriveMetricCard(
                    label = "العمولات المعلقة",
                    value = pending,
                    accent = AutoDriveAccent.Secondary,
                    icon = Icons.Outlined.HourglassEmpty,
                    modifier = metricModifier,
                )
            },
        )
    }
}

@Composable
private fun ReportDetails(
    competitionAvailability: CompetitionAvailability,
    onNavigateInvoiceList: (weekMode: String) -> Unit,
    onNavigateWeeklyCommissions: () -> Unit,
    onNavigateCompetitionHistory: () -> Unit,
    onNavigateWinWeeks: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD)) {
        AutoDriveSectionHeader("التفاصيل")
        AutoDriveCard {
            AutoDriveListRow(
                title = "فواتير هذا الأسبوع",
                supportingText = "عرض فواتير الفترة الحالية",
                leading = {
                    Icon(
                        imageVector = Icons.Outlined.ReceiptLong,
                        contentDescription = null,
                        tint = AutoDriveText.Primary,
                    )
                },
                onClick = { onNavigateInvoiceList("current") },
            )
            AutoDriveDivider()
            AutoDriveListRow(
                title = "العمولات الأسبوعية",
                supportingText = "مشترياتك وعمولتك أسبوعياً",
                leading = {
                    Icon(
                        imageVector = Icons.Outlined.Payments,
                        contentDescription = null,
                        tint = AutoDriveText.Primary,
                    )
                },
                onClick = onNavigateWeeklyCommissions,
            )

            if (competitionAvailability == CompetitionAvailability.ACTIVE) {
                AutoDriveDivider()
                AutoDriveListRow(
                    title = "المسابقة الأسبوعية",
                    supportingText = "سجل مشاركاتك الأسبوعية",
                    leading = {
                        Icon(
                            imageVector = Icons.Outlined.Timeline,
                            contentDescription = null,
                            tint = AutoDriveText.Primary,
                        )
                    },
                    onClick = onNavigateCompetitionHistory,
                )
                AutoDriveDivider()
                AutoDriveListRow(
                    title = "أسابيع الفوز",
                    supportingText = "الأسابيع التي حققت فيها الفوز",
                    leading = {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = null,
                            tint = AutoDriveText.Primary,
                        )
                    },
                    onClick = onNavigateWinWeeks,
                )
            }
        }
    }
}

@Composable
private fun HistoricalAchievement(state: ReportsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD)) {
        AutoDriveSectionHeader("منذ انضمامك")
        AutoDriveCard {
            Column(verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM)) {
                Text(
                    text = "إجمالي العمولات",
                    style = MaterialTheme.typography.labelLarge,
                    color = AutoDriveText.Secondary,
                )
                AutoDriveStatValue(
                    value = FormatUtils.formatSar(state.lifetimeCommissions),
                    size = AutoDriveStatSize.Medium,
                )
                if (state.joinDate.isNotBlank()) {
                    Text(
                        text = "منذ ${state.joinDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AutoDriveText.Secondary,
                    )
                }
            }
        }
    }
}

private fun reportRootItems(unreadMessages: Int) = listOf(
    AutoDriveNavigationItem("home", "الرئيسية", Icons.Rounded.Home),
    AutoDriveNavigationItem("messages", "الرسائل", Icons.Rounded.Message, unreadMessages),
    AutoDriveNavigationItem("reports", "التقارير", Icons.Rounded.BarChart),
    AutoDriveNavigationItem("settings", "الإعدادات", Icons.Rounded.Settings),
)
