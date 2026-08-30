package com.autodrive.app.feature.auth.presentation.join

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.designsystem.components.containers.AutoDriveCard
import java.text.SimpleDateFormat
import java.util.*
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.components.actions.AutoDriveIconButton

/**
 * شاشة 2 — انتظار التفعيل
 */
@Composable
fun WaitingScreen(
    onCodeClick: () -> Unit,
    onBack: () -> Unit
) {
    // أنيميشن النبض
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.95f,
        targetValue  = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val today = remember {
        SimpleDateFormat("EEEE، dd/MM/yyyy", Locale("ar")).format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AutoDriveSurface.Canvas)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        // ─── زر الرجوع ───────────────────────────
        Row(modifier = Modifier.fillMaxWidth()) {
            AutoDriveIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "رجوع",
                onClick = onBack,
            )
        }

        Spacer(Modifier.height(40.dp))

        // ─── أيقونة النبض ─────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp)
        ) {
            // حلقة خارجية نابضة
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .background(AutoDriveFinance.Pending.copy(alpha = 0.08f), CircleShape)
            )
            // حلقة داخلية
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
            text = "طلبك قيد المراجعة",
            style = MaterialTheme.typography.headlineLarge,
            color = AutoDriveText.Primary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "سيصلك كود التفعيل عبر واتساب",
            style = MaterialTheme.typography.bodyMedium,
            color = AutoDriveText.Secondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))

        // ─── بطاقة الحالة ─────────────────────────
        AutoDriveCard(modifier = Modifier.fillMaxWidth()) {
            StatusRow(label = "حالة الطلب", value = "قيد المراجعة", valueColor = AutoDriveFinance.Pending)
            Divider(color = AutoDriveBorderColor.Default, modifier = Modifier.padding(vertical = 12.dp))
            StatusRow(label = "تاريخ الإرسال", value = today)
            Divider(color = AutoDriveBorderColor.Default, modifier = Modifier.padding(vertical = 12.dp))
            StatusRow(label = "وقت الرد المتوقع", value = "خلال 24 ساعة")
        }

        Spacer(Modifier.weight(1f))

        // ─── زر إدخال الكود ──────────────────────
        AutoDrivePrimaryButton(
            text = "أدخل كود الانضمام ←",
            modifier = Modifier.fillMaxWidth(),
            onClick = onCodeClick
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StatusRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = AutoDriveText.Primary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AutoDriveText.Secondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}
