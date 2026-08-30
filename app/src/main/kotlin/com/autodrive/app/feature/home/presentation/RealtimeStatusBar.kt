package com.autodrive.app.feature.home.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.autodrive.app.core.sync.domain.RealtimeConnectionObserver
import com.autodrive.app.core.sync.domain.RealtimeConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance

@HiltViewModel
class RealtimeStatusViewModel @Inject constructor(
    connectionObserver: RealtimeConnectionObserver
) : ViewModel() {
    val connectionState: StateFlow<RealtimeConnectionState> = connectionObserver.connectionState
}

@Composable
fun RealtimeStatusBar(
    modifier: Modifier = Modifier,
    viewModel: RealtimeStatusViewModel = hiltViewModel()
) {
    val state by viewModel.connectionState.collectAsState()

    val (label, color) = when (state) {
        RealtimeConnectionState.CONNECTED    -> "متصل"           to AutoDriveFinance.Withdrawable
        RealtimeConnectionState.CONNECTING   -> "جاري الاتصال..." to AutoDriveFinance.Pending
        RealtimeConnectionState.DISCONNECTED -> "غير متصل"        to MaterialTheme.colorScheme.error
    }

    AnimatedVisibility(
        visible = state != RealtimeConnectionState.CONNECTED,
        enter   = expandVertically(),
        exit    = shrinkVertically()
    ) {
        Row(
            modifier              = modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text       = label,
                style      = MaterialTheme.typography.labelSmall,
                color      = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
