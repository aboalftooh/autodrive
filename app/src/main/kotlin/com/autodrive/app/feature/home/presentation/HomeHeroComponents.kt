package com.autodrive.app.feature.home.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import com.autodrive.app.core.designsystem.components.AutoDriveAccent
import com.autodrive.app.core.designsystem.components.actions.AutoDriveIconButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveIconButtonTone
import com.autodrive.app.core.designsystem.components.data.AutoDriveInstrumentNumber
import com.autodrive.app.core.designsystem.components.data.AutoDriveInstrumentTone
import com.autodrive.app.core.designsystem.foundation.border.AutoDriveBorder
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveInstrument
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.icon.AutoDriveIconSize
import com.autodrive.app.core.designsystem.foundation.radius.AutoDriveRadius
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.patterns.dashboard.DashboardHero
import com.autodrive.app.feature.home.presentation.audio.BenzineSound
import java.util.Locale
import kotlin.math.ceil

@Composable
fun PumpHeroCard(
    state: HomeUiState,
    onPump: () -> Unit,
    onPumpAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatable = remember(state.displayedTotal) {
        Animatable(state.displayedTotal.toDisplayDouble().toFloat())
    }

    LaunchedEffect(state.isPumping) {
        if (!state.isPumping) return@LaunchedEffect
        if (state.syncedTotal <= state.displayedTotal) {
            onPumpAnimationComplete()
            return@LaunchedEffect
        }
        val diff = state.syncedTotal - state.displayedTotal
        val duration = (800L + (diff.toDisplayDouble() / 1000.0 * 8.0).toLong())
            .coerceIn(800L, 2500L)
        BenzineSound.playPumpFill(duration.toInt())
        animatable.animateTo(
            targetValue = state.syncedTotal.toDisplayDouble().toFloat(),
            animationSpec = tween(duration.toInt(), easing = FastOutSlowInEasing),
        )
        if (state.syncedTotal >= state.weeklyTarget) BenzineSound.playTankFull()
        onPumpAnimationComplete()
    }

    var remaining by remember { mutableStateOf(state.nextFriday9AmMs - System.currentTimeMillis()) }
    LaunchedEffect(state.nextFriday9AmMs) {
        while (remaining > 0) {
            kotlinx.coroutines.delay(1000)
            remaining = state.nextFriday9AmMs - System.currentTimeMillis()
        }
    }

    val displayAmount = animatable.value.toDouble()
    val targetAmount = state.weeklyTarget.toDisplayDouble()
    val fillPercent = if (targetAmount > 0.0) {
        (displayAmount / targetAmount).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    DashboardHero(
        modifier = modifier.fillMaxWidth(),
        accent = AutoDriveAccent.Secondary,
        label = "إجمالي الأسبوع",
        heroContent = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = AutoDriveFinance.Pending,
                        modifier = Modifier.size(AutoDriveIconSize.LG),
                        strokeWidth = AutoDriveBorder.Strong,
                    )
                } else {
                    AutoDriveInstrumentNumber(
                        text = formatLedNumber(displayAmount),
                        tone = instrumentTone(fillPercent),
                    )
                }
            }
            FuelGaugeBar(
                fillPercent = fillPercent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AutoDriveSpace.MD),
            )
        },
        supportingContent = {
            CountdownStatsPanel(
                remaining = remaining,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        action = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                PumpActionButton(
                    onClick = onPump,
                    enabled = !state.isPumping,
                )
            }
        },
    )
}

private fun instrumentTone(fillPercent: Float): AutoDriveInstrumentTone = when {
    fillPercent >= 1f -> AutoDriveInstrumentTone.Full
    fillPercent >= 0.75f -> AutoDriveInstrumentTone.Good
    fillPercent >= 0.5f -> AutoDriveInstrumentTone.Caution
    fillPercent > 0f -> AutoDriveInstrumentTone.Low
    else -> AutoDriveInstrumentTone.Empty
}

@Composable
private fun CountdownStatsPanel(remaining: Long, modifier: Modifier = Modifier) {
    val active = remaining > 0
    val days = if (active) remaining / 86_400_000L else 0L
    val hours = if (active) (remaining % 86_400_000L) / 3_600_000L else 0L
    val minutes = if (active) (remaining % 3_600_000L) / 60_000L else 0L
    val seconds = if (active) (remaining % 60_000L) / 1_000L else 0L

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CountdownStat(seconds, "ثوان", Icons.Rounded.Autorenew)
        CountdownDivider()
        CountdownStat(minutes, "دقائق", Icons.Rounded.Timer)
        CountdownDivider()
        CountdownStat(hours, "ساعات", Icons.Rounded.Schedule)
        CountdownDivider()
        CountdownStat(days, "أيام", Icons.Rounded.CalendarMonth)
    }
}

@Composable
private fun RowScope.CountdownStat(
    value: Long,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.Optical),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AutoDriveText.Secondary,
            modifier = Modifier.size(AutoDriveIconSize.SM),
        )
        Text(
            text = String.format(Locale.US, "%02d", value.coerceIn(0, 99)),
            color = AutoDriveFinance.Withdrawable,
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
        )
        Text(
            text = label,
            color = AutoDriveText.Secondary,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun RowScope.CountdownDivider() {
    Box(
        Modifier
            .size(width = AutoDriveBorder.Thin, height = AutoDriveSpace.X4L)
            .background(AutoDriveBorderColor.Default),
    )
}

@Composable
fun FuelGaugeBar(fillPercent: Float, modifier: Modifier = Modifier) {
    val tones = listOf(
        AutoDriveInstrument.Full,
        AutoDriveInstrument.Good,
        AutoDriveInstrument.Caution,
        AutoDriveInstrument.Low,
        AutoDriveInstrument.Empty,
    )
    val activeSegments = ceil(fillPercent.coerceIn(0f, 1f) * tones.size).toInt()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.XS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "E",
            color = AutoDriveInstrument.Empty,
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
        )
        tones.asReversed().forEachIndexed { index, tone ->
            val active = index < activeSegments
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(AutoDriveSpace.LG)
                    .clip(AutoDriveRadius.SmallShape)
                    .background(if (active) tone else AutoDriveInstrument.Track),
            )
        }
        Text(
            text = "F",
            color = AutoDriveInstrument.Full,
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
        )
    }
}

@Composable
private fun PumpActionButton(onClick: () -> Unit, enabled: Boolean) {
    AutoDriveIconButton(
        icon = Icons.Rounded.LocalGasStation,
        contentDescription = "ضخ البنزين",
        onClick = onClick,
        enabled = enabled,
        tone = AutoDriveIconButtonTone.HighEmphasis,
    )
}

private fun formatLedNumber(amount: Double): String {
    val n = amount.toLong().coerceAtLeast(0L)
    return if (n < 1_000_000) {
        String.format(Locale.US, "%03d.%03d", n / 1000, n % 1000)
    } else {
        String.format(Locale.US, "%,d", n)
    }
}
