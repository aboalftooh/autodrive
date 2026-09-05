package com.autodrive.app.core.designsystem.patterns.reports

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.autodrive.app.core.designsystem.components.AutoDriveAccent
import com.autodrive.app.core.designsystem.components.containers.AutoDriveMetricCard
import com.autodrive.app.core.designsystem.theme.AutoDriveTheme

@Composable
fun ReportStatTile(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    accent: AutoDriveAccent? = null,
    icon: ImageVector? = null,
) {
    AutoDriveMetricCard(label, value, modifier, supportingText, accent, icon, onClick)
}

@Preview(showBackground = true, backgroundColor = 0xFF08090C)
@Composable
private fun ReportStatTilePreview() = AutoDriveTheme { ReportStatTile("إجمالي الأسبوع", "1.2M", {}) }
