package com.autodrive.app.feature.auth.presentation.join

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.autodrive.app.core.designsystem.components.actions.AutoDriveIconButton
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.designsystem.components.containers.AutoDriveCard
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WaitingScreen(
    onOtpReady: (phone: String, requestId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: WaitingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        val ready = state as? WaitingState.OtpReady ?: return@LaunchedEffect
        onOtpReady(ready.phone, ready.requestId)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            viewModel.refresh()
        }
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val today = remember {
        SimpleDateFormat("EEEE، dd/MM/yyyy", Locale("ar")).format(Date())
    }

    val headline = when (state) {
        is WaitingState.OtpReady -> "تمت الموافقة"
        is WaitingState.Rejected -> "تعذر تفعيل الطلب"
        else -> "طلبك قيد المراجعة"
    }
    val details = when (val current = state) {
        is WaitingState.Error -> current.message
        is WaitingState.Rejected -> current.message
        is WaitingState.OtpReady -> "جارٍ فتح شاشة رمز التحقق"
        is WaitingState.Loading -> "جارٍ تحديث حالة الطلب"
        is WaitingState.Pending -> "سيظهر رمز التحقق فور موافقة الإدارة"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AutoDriveSurface.Canvas)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            AutoDriveIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "رجوع",
                onClick = onBack,
            )
        }

        Spacer(Modifier.height(40.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .background(AutoDriveFinance.Pending.copy(alpha = 0.08f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(AutoDriveFinance.Pending.copy(alpha = 0.15f), CircleShape)
            )
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = AutoDriveFinance.Pending,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(Modifier.height(32.dp))
        Text(
            text = headline,
            style = MaterialTheme.typography.headlineLarge,
            color = AutoDriveText.Primary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = details,
            style = MaterialTheme.typography.bodyMedium,
            color = if (state is WaitingState.Error || state is WaitingState.Rejected) {
                MaterialTheme.colorScheme.error
            } else AutoDriveText.Secondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))
        AutoDriveCard(modifier = Modifier.fillMaxWidth()) {
            Text("حالة الطلب: ${statusLabel(state)}", color = AutoDriveText.Primary)
            Spacer(Modifier.height(12.dp))
            Text("تاريخ المتابعة: $today", color = AutoDriveText.Secondary)
        }

        Spacer(Modifier.weight(1f))
        if (state !is WaitingState.OtpReady) {
            AutoDrivePrimaryButton(
                text = "تحديث الحالة",
                modifier = Modifier.fillMaxWidth(),
                onClick = viewModel::refresh,
                loading = state is WaitingState.Loading,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun statusLabel(state: WaitingState): String = when (state) {
    is WaitingState.Loading -> "جارٍ التحديث"
    is WaitingState.Pending -> "قيد المراجعة"
    is WaitingState.OtpReady -> "تمت الموافقة"
    is WaitingState.Rejected -> "مرفوض/منتهي"
    is WaitingState.Error -> "تعذر التحديث"
}
