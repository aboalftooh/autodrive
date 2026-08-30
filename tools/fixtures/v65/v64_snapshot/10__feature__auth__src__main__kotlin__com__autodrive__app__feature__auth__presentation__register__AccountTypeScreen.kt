package com.autodrive.app.feature.auth.presentation.register

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.autodrive.app.core.model.account.AccountType
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.designsystem.components.data.AutoDriveStepIndicator
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText

/**
 * شاشة 4 — نوع الحساب
 */
@Composable
fun AccountTypeScreen(onNext: (String) -> Unit) {
    var selected by remember { mutableStateOf<AccountType?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AutoDriveSurface.Canvas)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        AutoDriveStepIndicator(currentStep = 0, totalSteps = 3)

        Spacer(Modifier.height(40.dp))

        Text(
            "ما طبيعة عملك؟",
            style = MaterialTheme.typography.headlineLarge,
            color = AutoDriveText.Primary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "اختر النوع المناسب لحسابك في AutoDrive",
            style = MaterialTheme.typography.bodyMedium,
            color = AutoDriveText.Secondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(48.dp))

        // ─── بطاقة المسوّق ─────────────────────
        AccountTypeCard(
            emoji = "📣",
            title = "مسوّق",
            subtitle = "تجلب عملاء وتحصل على عمولة",
            selected = selected == AccountType.MARKETER,
            onClick = { selected = AccountType.MARKETER }
        )

        Spacer(Modifier.height(16.dp))

        // ─── بطاقة صاحب الورشة ─────────────────
        AccountTypeCard(
            emoji = "🔧",
            title = "صاحب ورشة",
            subtitle = "ترسل سيارات وتكسب عمولة",
            selected = selected == AccountType.WORKSHOP_OWNER,
            onClick = { selected = AccountType.WORKSHOP_OWNER }
        )

        Spacer(Modifier.weight(1f))

        AutoDrivePrimaryButton(
            text = "التالي ←",
            modifier = Modifier.fillMaxWidth(),
            onClick = { selected?.let { onNext(it.name) } },
            enabled = selected != null
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AccountTypeCard(
    emoji: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) AutoDriveFinance.Withdrawable else AutoDriveBorderColor.Default
    val bgColor     = if (selected) AutoDriveFinance.Withdrawable.copy(alpha = 0.07f) else AutoDriveSurface.Raised

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(emoji, style = MaterialTheme.typography.displaySmall)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = AutoDriveText.Primary)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = AutoDriveText.Secondary)
            }
        }
    }
}
