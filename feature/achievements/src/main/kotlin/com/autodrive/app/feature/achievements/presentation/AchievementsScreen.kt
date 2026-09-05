package com.autodrive.app.feature.achievements.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autodrive.app.core.common.format.FormatUtils
import com.autodrive.app.core.designsystem.components.actions.AutoDriveFab
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveTextButton
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveBottomSheet
import com.autodrive.app.core.designsystem.components.inputs.AutoDriveNumericField
import com.autodrive.app.core.designsystem.components.navigation.AutoDriveBottomNavigation
import com.autodrive.app.core.designsystem.components.navigation.AutoDriveNavigationItem
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBrand
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.radius.AutoDriveRadius
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveContentWidth
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.theme.AutoDriveTheme
import com.autodrive.app.core.model.money.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AchievementsScreen(
    onNavigateHome: () -> Unit,
    onNavigateRecent: () -> Unit,
    onNavigateProfile: () -> Unit,
    onAddClick: () -> Unit,
    unreadMessages: Int = 0,
    onOpenAllCommissions: () -> Unit,
    onOpenBalance: () -> Unit,
    onOpenPendingCommissions: () -> Unit,
    viewModel: AchievementsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        state.targetEditorInitialValue
            ?.takeIf { state.showTargetEditor }
            ?.let { target ->
                WeeklyTargetEditorSheet(
                    initialTarget = target,
                    isSaving = state.isSavingTarget,
                    error = state.targetEditorError,
                    onDismiss = viewModel::dismissTargetEditor,
                    onSave = viewModel::saveWeeklyTarget,
                )
            }

        AchievementsContent(
            state = state,
            onNavigateHome = onNavigateHome,
            onNavigateRecent = onNavigateRecent,
            onNavigateProfile = onNavigateProfile,
            onAddClick = onAddClick,
            unreadMessages = unreadMessages,
            onOpenAllCommissions = onOpenAllCommissions,
            onOpenBalance = onOpenBalance,
            onOpenPendingCommissions = onOpenPendingCommissions,
            onEditTarget = { viewModel.openTargetEditor() },
            onUseSuggestedTarget = viewModel::openTargetEditor,
            onSnoozeTargetSuggestion = viewModel::snoozeTargetSuggestion,
            onRefreshPerformance = viewModel::refreshPerformance,
        )
    }
}

@Composable
internal fun AchievementsContent(
    state: AchievementsUiState,
    onNavigateHome: () -> Unit,
    onNavigateRecent: () -> Unit,
    onNavigateProfile: () -> Unit,
    onAddClick: () -> Unit,
    unreadMessages: Int,
    onOpenAllCommissions: () -> Unit,
    onOpenBalance: () -> Unit,
    onOpenPendingCommissions: () -> Unit,
    onEditTarget: () -> Unit,
    onUseSuggestedTarget: (Money) -> Unit,
    onSnoozeTargetSuggestion: () -> Unit,
    onRefreshPerformance: () -> Unit,
) {
    Scaffold(
        containerColor = AutoDriveSurface.Canvas,
        bottomBar = {
            AutoDriveBottomNavigation(
                items = achievementsRootItems(unreadMessages),
                selectedItemId = "achievements",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = AutoDriveContentWidth.Dashboard),
                contentPadding = PaddingValues(
                    start = AutoDriveSpace.LG,
                    end = AutoDriveSpace.LG,
                    top = AutoDriveSpace.X3L,
                    bottom = AutoDriveSpace.X3L,
                ),
                verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.LG),
            ) {
                item {
                    Text(
                        text = "إنجازاتي",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = AutoDriveText.Primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                    )
                }

                item {
                    LifetimeCommissionCard(
                        amount = state.lifetimeCommission,
                        amountVerified = state.hasVerifiedCommissions,
                        joinedAtLabel = state.joinedAtLabel,
                        onClick = onOpenAllCommissions,
                    )
                }

                item {
                    AvailableBalanceCard(
                        balance = state.availableBalance,
                        balanceVerified = state.hasVerifiedBalance,
                        hasActiveWithdrawal = state.hasActiveWithdrawal,
                        onClick = onOpenBalance,
                    )
                }

                item {
                    PendingCommissionCard(
                        amount = state.pendingCommission,
                        amountVerified = state.hasVerifiedCommissions,
                        onClick = onOpenPendingCommissions,
                    )
                }

                item {
                    WeeklyPerformanceCard(
                        state = state.weeklyPerformance,
                        onEditTarget = onEditTarget,
                        onUseSuggestedTarget = onUseSuggestedTarget,
                        onSnoozeTargetSuggestion = onSnoozeTargetSuggestion,
                        onRefresh = onRefreshPerformance,
                    )
                }
            }
        }
    }
}

private fun achievementsRootItems(unreadMessages: Int) = listOf(
    AutoDriveNavigationItem("home", "الرئيسية", Icons.Rounded.Home),
    AutoDriveNavigationItem("messages", "المحادثات", Icons.Rounded.Message, unreadMessages),
    AutoDriveNavigationItem("achievements", "إنجازاتي", Icons.Rounded.EmojiEvents),
    AutoDriveNavigationItem("settings", "الإعدادات", Icons.Rounded.Settings),
)

@Composable
private fun LifetimeCommissionCard(
    amount: Money,
    amountVerified: Boolean,
    joinedAtLabel: String,
    onClick: () -> Unit,
) {
    val title = if (joinedAtLabel.isNotBlank()) {
        "إجمالي عمولاتك منذ $joinedAtLabel"
    } else {
        "إجمالي عمولاتك منذ انضمامك"
    }

    FinanceSummaryCard(
        accent = AutoDriveBrand.Primary,
        borderColor = AutoDriveBrand.Primary.copy(alpha = 0.86f),
        background = Brush.linearGradient(
            colors = listOf(
                AutoDriveBrand.Primary.copy(alpha = 0.14f),
                AutoDriveSurface.Base.copy(alpha = 0.96f),
            ),
        ),
        icon = Icons.Rounded.EmojiEvents,
        iconDescription = "إجمالي العمولات",
        title = title,
        amount = amount,
        amountVerified = amountVerified,
        subtitle = "إجمالي ما حققته خلال هذه الفترة",
        minHeight = 174.dp,
        onClick = onClick,
    )
}

@Composable
private fun AvailableBalanceCard(
    balance: Money,
    balanceVerified: Boolean,
    hasActiveWithdrawal: Boolean,
    onClick: () -> Unit,
) {
    FinanceSummaryCard(
        accent = AutoDriveFinance.Withdrawable,
        borderColor = AutoDriveBorderColor.Default,
        background = Brush.linearGradient(
            listOf(AutoDriveSurface.Raised, AutoDriveSurface.Base),
        ),
        icon = Icons.Rounded.AccountBalanceWallet,
        iconDescription = "الرصيد والسحب",
        title = "متاح للسحب الآن",
        amount = balance,
        amountVerified = balanceVerified,
        subtitle = "يفتح الرصيد والحركات والسحب",
        minHeight = 190.dp,
        bottomContent = if (hasActiveWithdrawal) {
            { ProcessingWithdrawalChip() }
        } else null,
        onClick = onClick,
    )
}

@Composable
private fun PendingCommissionCard(
    amount: Money,
    amountVerified: Boolean,
    onClick: () -> Unit,
) {
    FinanceSummaryCard(
        accent = AutoDriveFinance.Pending,
        borderColor = AutoDriveBorderColor.Default,
        background = Brush.linearGradient(
            listOf(AutoDriveSurface.Raised, AutoDriveSurface.Base),
        ),
        icon = Icons.Rounded.Schedule,
        iconDescription = "عمولات غير جاهزة",
        title = "عمولات غير جاهزة",
        amount = amount,
        amountVerified = amountVerified,
        subtitle = "تنتظر شروط الاستحقاق\nيعرض التفاصيل والأسباب لكل عمولة",
        minHeight = 168.dp,
        onClick = onClick,
    )
}

@Composable
private fun WeeklyPerformanceCard(
    state: WeeklyPerformanceUiState,
    onEditTarget: () -> Unit,
    onUseSuggestedTarget: (Money) -> Unit,
    onSnoozeTargetSuggestion: () -> Unit,
    onRefresh: () -> Unit,
) {
    val shape = RoundedCornerShape(AutoDriveRadius.XL)
    val progress = state.progressPercent
        .divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
        .toFloat()
        .coerceIn(0f, 1f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = AutoDriveSurface.Raised,
        border = BorderStroke(1.dp, AutoDriveBorderColor.Default),
    ) {
        Column(
            modifier = Modifier.padding(AutoDriveSpace.XL),
            verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.LG),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.XS)) {
                Text(
                    text = "أداؤك هذا الأسبوع",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AutoDriveText.Primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = weekRangeLabel(state.weekStartMs, state.weekEndMs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AutoDriveText.Secondary,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM)) {
                MoneyInline(
                    amount = state.currentAmount,
                    accent = AutoDriveBrand.Primary,
                    fontSize = 42,
                )
                Text(
                    text = commissionCountLabel(state.currentCount),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AutoDriveText.Secondary,
                )
                PerformanceComparison(state)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AutoDriveBorderColor.Default),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "هدفك الأسبوعي",
                        style = MaterialTheme.typography.labelLarge,
                        color = AutoDriveText.Secondary,
                    )
                    Text(
                        text = "${FormatUtils.formatSar(state.weeklyTarget)} ج.س",
                        style = MaterialTheme.typography.titleLarge,
                        color = AutoDriveText.Primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(onClick = onEditTarget) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "تعديل الهدف الأسبوعي",
                        tint = AutoDriveBrand.Primary,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(99.dp)),
                    color = AutoDriveBrand.Primary,
                    trackColor = AutoDriveSurface.Base,
                )
                Text(
                    text = "${formatPercent(state.progressPercent)} من هدفك",
                    style = MaterialTheme.typography.labelLarge,
                    color = AutoDriveBrand.Primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            GoalMessage(state)

            if (state.targetSuggestionVisible && state.suggestedTarget != null) {
                TargetSuggestion(
                    suggestedTarget = state.suggestedTarget,
                    onUseSuggestedTarget = onUseSuggestedTarget,
                    onSnooze = onSnoozeTargetSuggestion,
                )
            }

            if (state.loadError) {
                Surface(
                    shape = RoundedCornerShape(AutoDriveRadius.MD),
                    color = AutoDriveFinance.Pending.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, AutoDriveFinance.Pending.copy(alpha = 0.20f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AutoDriveSpace.MD),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "تعذّر تحديث المقارنة، والأرقام الحالية من آخر بيانات محفوظة.",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = AutoDriveText.Secondary,
                        )
                        IconButton(onClick = onRefresh) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "إعادة المحاولة",
                                tint = AutoDriveBrand.Primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceComparison(state: WeeklyPerformanceUiState) {
    val (icon, color, text) = when (state.trend) {
        WeeklyPerformanceTrend.UP -> Triple(
            Icons.Rounded.TrendingUp,
            AutoDriveFinance.Withdrawable,
            "أعلى بـ${formatPercentAbsolute(state.changePercent)} من نفس الفترة الأسبوع الماضي",
        )
        WeeklyPerformanceTrend.DOWN -> Triple(
            Icons.Rounded.TrendingDown,
            MaterialTheme.colorScheme.error,
            "أقل بـ${formatPercentAbsolute(state.changePercent)} من نفس الفترة الأسبوع الماضي",
        )
        WeeklyPerformanceTrend.FLAT -> Triple(
            Icons.Rounded.TrendingUp,
            AutoDriveText.Secondary,
            "بنفس مستوى الفترة المقابلة من الأسبوع الماضي",
        )
        WeeklyPerformanceTrend.UP_NO_BASELINE -> Triple(
            Icons.Rounded.TrendingUp,
            AutoDriveFinance.Withdrawable,
            "بداية أفضل من نفس الفترة الأسبوع الماضي",
        )
        WeeklyPerformanceTrend.NO_BASELINE -> Triple(
            Icons.Rounded.TrendingUp,
            AutoDriveText.Secondary,
            "ستظهر المقارنة عندما تتوفر بيانات كافية",
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = if (state.trend == WeeklyPerformanceTrend.UP) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun GoalMessage(state: WeeklyPerformanceUiState) {
    val achieved = state.targetAchieved
    val accent = if (achieved) AutoDriveFinance.Withdrawable else AutoDriveBrand.Primary
    val icon = if (achieved) Icons.Rounded.CheckCircle else Icons.Rounded.Flag
    val title = when {
        state.targetAchievedEarly -> "حققت هدف الأسبوع مبكرًا"
        state.targetAchieved -> "حققت هدفك الأسبوعي"
        else -> "باقي ${FormatUtils.formatSar(state.remainingToTarget)} ج.س لتحقيق هدفك"
    }
    val subtitle = when {
        state.targetAchievedEarly -> "استمر على نفس المستوى حتى نهاية الأسبوع."
        state.targetAchieved -> "أكملت هدفك، وأي عمولة جديدة ترفع إنجاز هذا الأسبوع."
        state.daysRemaining <= 1 -> "باقي اليوم • تحتاج نحو ${FormatUtils.formatSar(state.requiredDailyAverage)} ج.س للوصول للهدف"
        else -> "متبقي ${state.daysRemaining} أيام • تحتاج نحو ${FormatUtils.formatSar(state.requiredDailyAverage)} ج.س يوميًا"
    }

    Surface(
        shape = RoundedCornerShape(AutoDriveRadius.LG),
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.20f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AutoDriveSpace.LG),
            horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(26.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.XS),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = AutoDriveText.Primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AutoDriveText.Secondary,
                )
            }
        }
    }
}

@Composable
private fun TargetSuggestion(
    suggestedTarget: Money,
    onUseSuggestedTarget: (Money) -> Unit,
    onSnooze: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(AutoDriveRadius.LG),
        color = AutoDriveFinance.Withdrawable.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, AutoDriveFinance.Withdrawable.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AutoDriveSpace.LG),
            verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM),
        ) {
            Text(
                text = "مستواك ارتفع",
                style = MaterialTheme.typography.titleMedium,
                color = AutoDriveText.Primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "تجاوزت هدفك بشكل واضح في أسبوعين من آخر 3 أسابيع. قد يكون الوقت مناسبًا لرفعه.",
                style = MaterialTheme.typography.bodyMedium,
                color = AutoDriveText.Secondary,
            )
            Text(
                text = "هدف مقترح: ${FormatUtils.formatSar(suggestedTarget)} ج.س",
                style = MaterialTheme.typography.bodyLarge,
                color = AutoDriveFinance.Withdrawable,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AutoDrivePrimaryButton(
                    text = "تعديل الهدف",
                    onClick = { onUseSuggestedTarget(suggestedTarget) },
                )
                AutoDriveTextButton(
                    text = "ليس الآن",
                    onClick = onSnooze,
                )
            }
        }
    }
}

@Composable
private fun WeeklyTargetEditorSheet(
    initialTarget: Money,
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (Money) -> Unit,
) {
    var rawValue by rememberSaveable(initialTarget.toPlainString()) {
        mutableStateOf(initialTarget.amount.setScale(0, RoundingMode.DOWN).toPlainString())
    }
    val parsed = runCatching { Money.of(rawValue.ifBlank { "0" }) }.getOrNull()
    val valid = parsed != null && parsed >= Money.of(100_000L) && parsed <= Money.of(5_000_000L)

    AutoDriveBottomSheet(
        onDismissRequest = onDismiss,
        title = "تعديل الهدف الأسبوعي",
    ) {
        AutoDriveNumericField(
            value = rawValue,
            onValueChange = { value ->
                rawValue = value.filter(Char::isDigit).take(10)
            },
            label = "الهدف الأسبوعي",
            enabled = !isSaving,
        )
        Text(
            text = "هدف شخصي لتحفيزك وقياس تقدمك. لا يؤثر على ترتيب المسابقة.",
            style = MaterialTheme.typography.bodySmall,
            color = AutoDriveText.Secondary,
        )
        Text(
            text = "الحد المتاح من 100,000 إلى 5,000,000 ج.س",
            style = MaterialTheme.typography.bodySmall,
            color = AutoDriveText.Secondary,
        )
        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        AutoDrivePrimaryButton(
            text = "حفظ الهدف",
            onClick = { parsed?.let(onSave) },
            enabled = valid && !isSaving,
            loading = isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FinanceSummaryCard(
    accent: Color,
    borderColor: Color,
    background: Brush,
    icon: ImageVector,
    iconDescription: String,
    title: String,
    amount: Money,
    amountVerified: Boolean,
    subtitle: String,
    minHeight: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    bottomContent: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(AutoDriveRadius.XL)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(shape)
            .background(background)
            .border(BorderStroke(1.dp, borderColor), shape)
            .clickable(onClick = onClick)
            .padding(AutoDriveSpace.XL),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM),
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleLarge,
                        color = AutoDriveText.Primary,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Right,
                    )
                    MoneyValue(
                        amount = amount,
                        verified = amountVerified,
                        accent = accent,
                    )
                    Text(
                        text = subtitle,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = AutoDriveText.Secondary,
                        textAlign = TextAlign.Right,
                    )
                }

                Spacer(Modifier.width(AutoDriveSpace.LG))

                IconBubble(
                    icon = icon,
                    contentDescription = iconDescription,
                    accent = accent,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (bottomContent != null) bottomContent() else Spacer(Modifier.width(1.dp))
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = "فتح",
                    tint = accent,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
private fun MoneyValue(
    amount: Money,
    verified: Boolean,
    accent: Color,
) {
    val formatted = if (verified) FormatUtils.formatSar(amount) else "—"
    val amountSize = when {
        formatted.length >= 10 -> 38.sp
        formatted.length >= 8 -> 44.sp
        else -> 50.sp
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Start,
    ) {
        Text(
            text = formatted,
            color = accent,
            fontSize = amountSize,
            lineHeight = 56.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Spacer(Modifier.width(AutoDriveSpace.SM))
        Text(
            text = "ج.س",
            color = accent,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 7.dp),
        )
    }
}

@Composable
private fun MoneyInline(
    amount: Money,
    accent: Color,
    fontSize: Int,
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM),
    ) {
        Text(
            text = FormatUtils.formatSar(amount),
            color = accent,
            fontSize = fontSize.sp,
            lineHeight = (fontSize + 8).sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "ج.س",
            color = accent,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 5.dp),
        )
    }
}

@Composable
private fun IconBubble(
    icon: ImageVector,
    contentDescription: String,
    accent: Color,
) {
    Surface(
        modifier = Modifier.size(64.dp),
        shape = CircleShape,
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.10f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = accent,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

@Composable
private fun ProcessingWithdrawalChip() {
    Surface(
        shape = RoundedCornerShape(AutoDriveRadius.MD),
        color = AutoDriveFinance.Pending.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, AutoDriveFinance.Pending.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AutoDriveSpace.MD,
                vertical = AutoDriveSpace.SM,
            ),
            horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = AutoDriveFinance.Pending,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "سحب قيد المعالجة",
                color = AutoDriveFinance.Pending,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun weekRangeLabel(startMs: Long, endMs: Long): String {
    if (startMs <= 0L || endMs <= 0L) return "الأسبوع الحالي"
    val zone = ZoneOffset.ofHours(3)
    val locale = Locale.forLanguageTag("ar-SD")
    val dayMonth = DateTimeFormatter.ofPattern("d MMMM", locale).withZone(zone)
    val year = DateTimeFormatter.ofPattern("yyyy", locale).withZone(zone)
    val start = Instant.ofEpochMilli(startMs)
    val end = Instant.ofEpochMilli(endMs)
    return "${dayMonth.format(start)} – ${dayMonth.format(end)} ${year.format(end)}"
}

private fun commissionCountLabel(count: Long): String = when (count) {
    0L -> "لا توجد عمولات حتى الآن"
    1L -> "عمولة واحدة هذا الأسبوع"
    2L -> "عمولتان هذا الأسبوع"
    in 3L..10L -> "$count عمولات هذا الأسبوع"
    else -> "$count عمولة هذا الأسبوع"
}

private fun formatPercent(value: BigDecimal): String =
    "${value.setScale(0, RoundingMode.HALF_UP).toPlainString()}%"

private fun formatPercentAbsolute(value: BigDecimal?): String =
    value?.abs()?.setScale(0, RoundingMode.HALF_UP)?.toPlainString()?.plus("%") ?: "—"

@Preview(showBackground = true, backgroundColor = 0xFF08090C, widthDp = 412)
@Composable
private fun AchievementsPreview() {
    AutoDriveTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AchievementsContent(
                state = AchievementsUiState(
                    isLoading = false,
                    hasVerifiedCommissions = true,
                    hasVerifiedBalance = true,
                    lifetimeCommission = Money.of("128450"),
                    availableBalance = Money.of("18900"),
                    pendingCommission = Money.of("7350"),
                    joinedAtLabel = "أبريل 2025",
                    hasActiveWithdrawal = true,
                    weeklyPerformance = WeeklyPerformanceUiState(
                        hasServerSnapshot = true,
                        weekStartMs = 1788492000000L,
                        weekEndMs = 1789096800000L,
                        currentAmount = Money.of("576000"),
                        currentCount = 8,
                        previousSamePeriodAmount = Money.of("488000"),
                        previousSamePeriodCount = 6,
                        changePercent = BigDecimal("18.0"),
                        trend = WeeklyPerformanceTrend.UP,
                        weeklyTarget = Money.of("800000"),
                        progressPercent = BigDecimal("72.0"),
                        remainingToTarget = Money.of("224000"),
                        daysRemaining = 3,
                        requiredDailyAverage = Money.of("74667"),
                    ),
                ),
                onNavigateHome = {},
                onNavigateRecent = {},
                onNavigateProfile = {},
                onAddClick = {},
                unreadMessages = 0,
                onOpenAllCommissions = {},
                onOpenBalance = {},
                onOpenPendingCommissions = {},
                onEditTarget = {},
                onUseSuggestedTarget = {},
                onSnoozeTargetSuggestion = {},
                onRefreshPerformance = {},
            )
        }
    }
}
