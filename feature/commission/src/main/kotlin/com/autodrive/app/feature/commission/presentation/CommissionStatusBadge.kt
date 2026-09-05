package com.autodrive.app.feature.commission.presentation

import androidx.compose.runtime.Composable
import com.autodrive.app.core.designsystem.components.AutoDriveStatusTone
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveStatusChip
import com.autodrive.app.feature.commission.domain.model.CommissionStatus

@Composable
fun StatusBadge(status: CommissionStatus) {
    val (label, tone) = when (status) {
        CommissionStatus.WITHDRAWABLE -> "قابل للسحب" to AutoDriveStatusTone.Success
        CommissionStatus.PENDING -> "معلّق" to AutoDriveStatusTone.Warning
        CommissionStatus.PAID -> "مصروف" to AutoDriveStatusTone.Neutral
    }
    AutoDriveStatusChip(text = label, tone = tone)
}
