package com.autodrive.app.feature.auth.presentation.join

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText

@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(AutoDriveSurface.Canvas).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("أهلاً بك في بنزين", style = MaterialTheme.typography.headlineLarge, color = AutoDriveText.Primary)
        Spacer(Modifier.height(12.dp))
        Text("تم تفعيل حسابك بنجاح. يمكنك الآن البدء.", style = MaterialTheme.typography.bodyMedium, color = AutoDriveText.Secondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(40.dp))
        AutoDrivePrimaryButton(text = "ابدأ الآن ←", onClick = onContinue, modifier = Modifier.fillMaxWidth())
    }
}
