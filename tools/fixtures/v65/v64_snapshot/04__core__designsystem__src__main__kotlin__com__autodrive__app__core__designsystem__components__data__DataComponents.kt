package com.autodrive.app.core.designsystem.components.data

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.autodrive.app.core.designsystem.components.AutoDriveAccent
import com.autodrive.app.core.designsystem.components.AutoDriveStatusTone
import com.autodrive.app.core.designsystem.components.color
import com.autodrive.app.core.designsystem.foundation.border.AutoDriveBorder
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBrand
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveInstrument
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveOpacity
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.icon.AutoDriveIconSize
import com.autodrive.app.core.designsystem.foundation.motion.AutoDriveMotion
import com.autodrive.app.core.designsystem.foundation.radius.AutoDriveRadius
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.foundation.typography.AutoDriveStatXL
import com.autodrive.app.core.designsystem.theme.AutoDriveTheme

enum class AutoDriveAvatarSize(val dp: Dp) { Small(32.dp), Default(40.dp), Large(48.dp), Hero(64.dp) }
enum class AutoDriveStatSize { Small, Medium, Large, Hero }
enum class AutoDriveInstrumentTone { Secondary, Active, Full, Good, Caution, Low, Empty }

@Composable
fun AutoDriveAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: AutoDriveAvatarSize = AutoDriveAvatarSize.Default,
    accent: AutoDriveAccent = AutoDriveAccent.Active,
    imageContent: (@Composable () -> Unit)? = null,
) {
    val color = accent.color()
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = AutoDriveOpacity.Subtle))
            .border(AutoDriveBorder.Thin, color.copy(alpha = AutoDriveOpacity.Muted), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (imageContent != null) imageContent()
        else Text(
            text = name.trim().firstOrNull()?.toString() ?: "؟",
            color = color,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun AutoDriveListRow(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    titleTone: AutoDriveStatusTone? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val titleColor = if (!enabled) AutoDriveText.Disabled else titleTone?.color() ?: AutoDriveText.Primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(if (selected) AutoDriveBrand.Active.copy(alpha = AutoDriveOpacity.Tint) else Color.Transparent)
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .then(if (!enabled) Modifier.semantics { disabled() } else Modifier)
            .padding(horizontal = AutoDriveSpace.LG, vertical = AutoDriveSpace.MD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD),
    ) {
        if (leading != null) Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { leading() }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor)
            if (supportingText != null) Text(supportingText, style = MaterialTheme.typography.bodySmall, color = if (enabled) AutoDriveText.Secondary else AutoDriveText.Disabled)
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun AutoDriveSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = AutoDriveText.Primary)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AutoDriveText.Secondary)
        }
        if (action != null) action()
    }
}

@Composable
fun AutoDriveDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(AutoDriveBorder.Thin).background(AutoDriveBorderColor.Default))
}

@Composable
fun AutoDriveStatValue(
    value: String,
    modifier: Modifier = Modifier,
    size: AutoDriveStatSize = AutoDriveStatSize.Medium,
    accent: AutoDriveAccent? = null,
    unit: String? = null,
) {
    val style = when (size) {
        AutoDriveStatSize.Small -> MaterialTheme.typography.displaySmall
        AutoDriveStatSize.Medium -> MaterialTheme.typography.displayMedium
        AutoDriveStatSize.Large -> MaterialTheme.typography.displayLarge
        AutoDriveStatSize.Hero -> AutoDriveStatXL
    }
    val color = accent?.color() ?: AutoDriveText.Primary
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(value, style = style, color = color)
        }
        if (unit != null) Text(unit, style = MaterialTheme.typography.bodyMedium, color = AutoDriveText.Secondary)
    }
}

@Composable
fun AutoDriveStatusIndicator(
    tone: AutoDriveStatusTone,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.semantics { this.contentDescription = contentDescription }.size(8.dp).clip(CircleShape).background(tone.color()),
    )
}

@Composable
fun AutoDriveStepIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM)) {
        repeat(totalSteps.coerceAtLeast(0)) { index ->
            val active = index == currentStep
            val completed = index < currentStep
            val width by animateDpAsState(if (active) 32.dp else 8.dp, AutoDriveMotion.emphasized(), label = "step_width")
            Box(
                Modifier
                    .width(width)
                    .height(8.dp)
                    .clip(AutoDriveRadius.PillShape)
                    .background(
                        when {
                            active -> AutoDriveBrand.Active
                            completed -> AutoDriveBrand.Active.copy(alpha = AutoDriveOpacity.Medium)
                            else -> AutoDriveBorderColor.Default
                        }
                    )
            )
        }
    }
}

private val SEGMENTS = arrayOf(
    booleanArrayOf(true, true, true, true, true, true, false),
    booleanArrayOf(false, true, true, false, false, false, false),
    booleanArrayOf(true, true, false, true, true, false, true),
    booleanArrayOf(true, true, true, true, false, false, true),
    booleanArrayOf(false, true, true, false, false, true, true),
    booleanArrayOf(true, false, true, true, false, true, true),
    booleanArrayOf(true, false, true, true, true, true, true),
    booleanArrayOf(true, true, true, false, false, false, false),
    booleanArrayOf(true, true, true, true, true, true, true),
    booleanArrayOf(true, true, true, true, false, true, true),
)

@Composable
private fun SegmentDigit(digit: Int, color: Color, width: Dp, height: Dp) {
    val segments = if (digit in 0..9) SEGMENTS[digit] else BooleanArray(7)
    val inactive = color.copy(alpha = AutoDriveOpacity.Ghost)
    Canvas(Modifier.size(width, height)) {
        val thickness = size.width * 0.17f
        val w = size.width - thickness
        val h = size.height - thickness
        val ox = thickness / 2f
        val oy = thickness / 2f
        val gap = thickness * 0.14f
        fun horizontal(y: Float, on: Boolean) = drawLine(if (on) color else inactive, Offset(ox + gap, oy + y), Offset(ox + w - gap, oy + y), thickness, StrokeCap.Round)
        fun vertical(x: Float, y1: Float, y2: Float, on: Boolean) = drawLine(if (on) color else inactive, Offset(ox + x, oy + y1 + gap), Offset(ox + x, oy + y2 - gap), thickness, StrokeCap.Round)
        horizontal(0f, segments[0]); horizontal(h / 2f, segments[6]); horizontal(h, segments[3])
        vertical(0f, 0f, h / 2f, segments[5]); vertical(0f, h / 2f, h, segments[4])
        vertical(w, 0f, h / 2f, segments[1]); vertical(w, h / 2f, h, segments[2])
    }
}

@Composable
fun AutoDriveInstrumentNumber(
    text: String,
    tone: AutoDriveInstrumentTone,
    modifier: Modifier = Modifier,
) {
    val color = when (tone) {
        AutoDriveInstrumentTone.Secondary -> AutoDriveBrand.Secondary
        AutoDriveInstrumentTone.Active -> AutoDriveBrand.Active
        AutoDriveInstrumentTone.Full -> AutoDriveInstrument.Full
        AutoDriveInstrumentTone.Good -> AutoDriveInstrument.Good
        AutoDriveInstrumentTone.Caution -> AutoDriveInstrument.Caution
        AutoDriveInstrumentTone.Low -> AutoDriveInstrument.Low
        AutoDriveInstrumentTone.Empty -> AutoDriveInstrument.Empty
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            text.forEach { ch ->
                when {
                    ch.isDigit() -> SegmentDigit(ch.digitToInt(), color, 20.dp, 36.dp)
                    ch == '.' || ch == ',' -> Canvas(Modifier.size(8.dp, 36.dp)) {
                        drawCircle(color, radius = size.width * 0.25f, center = Offset(size.width / 2f, size.height - size.width * 0.35f))
                    }
                    ch == ' ' -> Spacer(Modifier.width(AutoDriveSpace.SM))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090C)
@Composable
private fun DataComponentsPreview() = AutoDriveTheme {
    Column(verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.LG)) {
        AutoDriveAvatar("محمد")
        AutoDriveStatValue("1,240,000", size = AutoDriveStatSize.Large, accent = AutoDriveAccent.Secondary, unit = "ج.س")
        AutoDriveInstrumentNumber("128.4", AutoDriveInstrumentTone.Active)
        AutoDriveStepIndicator(1, 3)
    }
}
