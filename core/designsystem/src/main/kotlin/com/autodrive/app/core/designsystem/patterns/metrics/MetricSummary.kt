package com.autodrive.app.core.designsystem.patterns.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.autodrive.app.core.designsystem.components.AutoDriveAccent
import com.autodrive.app.core.designsystem.components.containers.AutoDriveMetricCard
import com.autodrive.app.core.designsystem.components.data.AutoDriveSectionHeader
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.theme.AutoDriveTheme

data class MetricSummaryItem(
    val id: String,
    val label: String,
    val value: String,
    val supportingText: String? = null,
    val accent: AutoDriveAccent? = null,
)

@Composable
fun MetricSummary(
    items: List<MetricSummaryItem>,
    modifier: Modifier = Modifier,
    title: String? = null,
    onItemClick: ((String) -> Unit)? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD)) {
        if (title != null) AutoDriveSectionHeader(title)
        items.chunked(2).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.MD)) {
                rowItems.forEach { item ->
                    AutoDriveMetricCard(
                        label = item.label,
                        value = item.value,
                        supportingText = item.supportingText,
                        accent = item.accent,
                        modifier = Modifier.weight(1f),
                        onClick = onItemClick?.let { callback -> { callback(item.id) } },
                    )
                }
                if (rowItems.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090C)
@Composable
private fun MetricSummaryPreview() = AutoDriveTheme {
    MetricSummary(listOf(MetricSummaryItem("a", "الطلبات", "24"), MetricSummaryItem("b", "المكتمل", "18", accent = AutoDriveAccent.Active)))
}
