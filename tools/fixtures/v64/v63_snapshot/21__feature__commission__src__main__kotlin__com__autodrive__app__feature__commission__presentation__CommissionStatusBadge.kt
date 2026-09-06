package com.autodrive.app.feature.commission.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.autodrive.app.feature.commission.domain.model.CommissionStatus
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance

@Composable
fun StatusBadge(status: CommissionStatus) {
    val (label, color) = when (status) {
        CommissionStatus.WITHDRAWABLE -> "قابل للسحب" to AutoDriveFinance.Withdrawable
        CommissionStatus.PENDING -> "معلّق" to AutoDriveFinance.Pending
        CommissionStatus.PAID -> "مصروف" to AutoDriveFinance.Paid
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.wrapContentSize(),
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
