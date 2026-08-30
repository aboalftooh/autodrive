package com.autodrive.app.feature.auth.presentation.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveTextButton
import com.autodrive.app.core.designsystem.components.AutoDriveAccent
import com.autodrive.app.core.designsystem.components.data.AutoDriveAvatar
import com.autodrive.app.core.designsystem.components.data.AutoDriveAvatarSize
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText

/**
 * شاشة انتهاء الجلسة — تظهر عندما تنتهي جلسة Supabase لمستخدم مسجّل سابقاً.
 * الحل: تسجيل الدخول مجدداً برقم الهاتف.
 */
@Composable
fun SessionExpiredScreen(
    userName: String,
    onLogin: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier            = Modifier.fillMaxSize().background(AutoDriveSurface.Canvas).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⏰", fontSize = 64.sp)
        Spacer(Modifier.height(24.dp))

        Text(
            text      = "مرحباً مجدداً،",
            style     = MaterialTheme.typography.headlineMedium,
            color     = AutoDriveText.Secondary,
            textAlign = TextAlign.Center
        )

        if (userName.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            AutoDriveAvatar(name = userName, size = AutoDriveAvatarSize.Hero, accent = AutoDriveAccent.Secondary)
            Spacer(Modifier.height(8.dp))
            Text(
                text      = userName,
                style     = MaterialTheme.typography.headlineSmall,
                color     = AutoDriveFinance.Pending,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(32.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AutoDriveSurface.Raised
        ) {
            Column(
                modifier            = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text  = "انتهت جلسة الدخول",
                    style = MaterialTheme.typography.titleMedium,
                    color = AutoDriveText.Primary
                )
                Text(
                    text      = "سجّل دخولك مجدداً برقم هاتفك وستعود مباشرةً للتطبيق.",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = AutoDriveText.Secondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        AutoDrivePrimaryButton(
            text    = "تسجيل الدخول",
            modifier = Modifier.fillMaxWidth(),
            onClick = onLogin,
        )

        Spacer(Modifier.height(12.dp))

        AutoDriveTextButton(
            text    = "إعادة المحاولة",
            onClick = onRetry
        )
    }
}
