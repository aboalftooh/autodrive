package com.autodrive.app.feature.auth.presentation.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText

// سطر العنوان الرئيسي: "اتفاقية استخدام تطبيق بنزين"
private fun String.isMainTitle() =
    trim().startsWith("اتفاقية استخدام")

// سطر آخر تحديث: "آخر تحديث..."
private fun String.isLastUpdate() =
    trim().startsWith("آخر تحديث")

// عنوان قسم: يبدأ برقم عربي أو لاتيني ثم نقطة
private val SECTION_REGEX = Regex("""^[\d١-٩][\d٠-٩]*\.""")
private fun String.isSectionHeader() = SECTION_REGEX.containsMatchIn(trim())

@Composable
fun TermsScreen(onAccepted: () -> Unit, onBack: () -> Unit = {}) {
    BackHandler(onBack = onBack)

    val context     = LocalContext.current
    val scrollState = rememberScrollState()
    val reachedBottom by remember {
        derivedStateOf { scrollState.value >= scrollState.maxValue - 50 }
    }
    val lines = remember {
        runCatching {
            context.assets.open("rs.txt").bufferedReader().use { it.readLines() }
        }.getOrDefault(listOf("تعذّر تحميل سياسة الاستخدام."))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AutoDriveSurface.Canvas)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text  = "اتفاقية الاستخدام",
            style = MaterialTheme.typography.headlineLarge,
            color = AutoDriveText.Primary
        )
        Text(
            text  = "يرجى قراءة الاتفاقية كاملاً قبل المتابعة",
            style = MaterialTheme.typography.bodyMedium,
            color = AutoDriveText.Secondary
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AutoDriveSurface.Raised)
                .border(1.dp, AutoDriveBorderColor.Default, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier            = Modifier.verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                lines.forEach { line ->
                    when {
                        line.isMainTitle() -> Text(
                            text       = line,
                            style      = MaterialTheme.typography.titleMedium,
                            color      = AutoDriveFinance.Withdrawable,
                            fontWeight = FontWeight.Bold
                        )
                        line.isLastUpdate() -> Text(
                            text  = line,
                            style = MaterialTheme.typography.bodySmall,
                            color = AutoDriveFinance.Withdrawable.copy(alpha = 0.75f)
                        )
                        line.isSectionHeader() -> {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text       = line,
                                style      = MaterialTheme.typography.bodyMedium,
                                color      = AutoDriveFinance.Pending,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        line.isBlank() -> Spacer(Modifier.height(4.dp))
                        else -> Text(
                            text  = line,
                            style = MaterialTheme.typography.bodySmall,
                            color = AutoDriveText.Secondary,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }

        if (!reachedBottom) {
            Text(
                text     = "↓ اسحب للأسفل لقراءة الاتفاقية كاملاً",
                style    = MaterialTheme.typography.labelSmall,
                color    = AutoDriveFinance.Pending,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        AutoDrivePrimaryButton(
            text    = "أوافق على الشروط والاستمرار",
            modifier = Modifier.fillMaxWidth(),
            onClick  = onAccepted,
            enabled  = reachedBottom,
        )

        TextButton(
            onClick  = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text  = "إغلاق والرجوع",
                color = AutoDriveText.Secondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
