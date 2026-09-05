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
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
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
        subtitle = "إجمالي ما حققته منذ انضمامك",
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
            {
                ProcessingWithdrawalChip()
            }
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
        iconDescription = "عمولات معلقة",
        title = "عمولات معلقة",
        amount = amount,
        amountVerified = amountVerified,
        subtitle = "تنتظر شروط الاستحقاق\nيعرض التفاصيل والأسباب لكل عمولة",
        minHeight = 168.dp,
        onClick = onClick,
    )
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

            if (bottomContent != null) {
                Spacer(Modifier.padding(top = AutoDriveSpace.XS))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    bottomContent()
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = "فتح",
                        tint = accent,
                        modifier = Modifier.size(28.dp),
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
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
                ),
                onNavigateHome = {},
                onNavigateRecent = {},
                onNavigateProfile = {},
                onAddClick = {},
                unreadMessages = 0,
                onOpenAllCommissions = {},
                onOpenBalance = {},
                onOpenPendingCommissions = {},
            )
        }
    }
}
