package com.autodrive.app.core.designsystem.patterns.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.autodrive.app.core.designsystem.components.AutoDriveAccent
import com.autodrive.app.core.designsystem.components.containers.AutoDriveHighlightCard
import com.autodrive.app.core.designsystem.components.data.AutoDriveStatSize
import com.autodrive.app.core.designsystem.components.data.AutoDriveStatValue
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.theme.AutoDriveTheme

@Composable
fun DashboardHero(
    modifier: Modifier = Modifier,
    accent: AutoDriveAccent = AutoDriveAccent.Primary,
    label: String? = null,
    heroContent: @Composable () -> Unit,
    supportingContent: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    AutoDriveHighlightCard(accent, modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.LG)) {
            if (label != null) Text(label, style = MaterialTheme.typography.labelMedium, color = AutoDriveText.Secondary)
            heroContent()
            if (supportingContent != null) supportingContent()
            if (action != null) action()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090C)
@Composable
private fun DashboardHeroPreview() = AutoDriveTheme {
    DashboardHero(label = "الرصيد القابل للسحب", heroContent = { AutoDriveStatValue("1,240,000", size = AutoDriveStatSize.Hero, accent = AutoDriveAccent.Active) })
}
