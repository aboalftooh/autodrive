package com.autodrive.app.feature.auth.presentation.join

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.platform.share.WhatsAppHelper
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText

// ─── Screen ────────────────────────────────────
@Composable
fun CodeInputScreen(
    onVerified: (isExistingUser: Boolean) -> Unit,
    onBack: () -> Unit,
    viewModel: CodeInputViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val state by viewModel.state.collectAsState()
    var code by remember { mutableStateOf("") }
    var requestOpened by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is CodeState.Success) onVerified((state as CodeState.Success).isExistingUser)
    }

    LaunchedEffect(Unit) {
        if (!requestOpened) {
            requestOpened = true
            WhatsAppHelper.requestInviteCode(context, viewModel.requestMessage())
        }
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
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, "رجوع", tint = AutoDriveText.Secondary)
            }
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text = "أدخل كود الانضمام",
            style = MaterialTheme.typography.headlineLarge,
            color = AutoDriveText.Primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "أدخل كود الانضمام المكون من 8 أرقام",
            style = MaterialTheme.typography.bodyMedium,
            color = AutoDriveText.Secondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(48.dp))

        // ─── حقل إدخال الكود ─────────────────────
        OutlinedTextField(
            value = code,
            onValueChange = { input ->
                if (input.length <= 8 && input.all { it in '0'..'9' }) code = input
            },
            placeholder = {
                Text("12345678", color = AutoDriveText.Secondary)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            isError = state is CodeState.Error,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AutoDriveFinance.Withdrawable,
                unfocusedBorderColor = AutoDriveBorderColor.Default,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedTextColor = AutoDriveText.Primary,
                unfocusedTextColor = AutoDriveText.Primary,
                cursorColor = AutoDriveFinance.Withdrawable,
                focusedContainerColor = AutoDriveSurface.Raised,
                unfocusedContainerColor = AutoDriveSurface.Raised
            ),
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                textAlign = TextAlign.Center,
                letterSpacing = 8.sp
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // ─── زر لصق ─────────────────────────────
        TextButton(
            onClick = {
                val clip = clipboardManager.getText()?.text?.trim()?.uppercase() ?: return@TextButton
                if (clip.matches(Regex("^[0-9]{8}$"))) code = clip
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(
                Icons.Rounded.ContentPaste,
                contentDescription = null,
                tint = AutoDriveFinance.Withdrawable,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("لصق الكود", color = AutoDriveFinance.Withdrawable, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(8.dp))

        // ─── رسالة الخطأ ─────────────────────────
        if (state is CodeState.Error) {
            Text(
                text = (state as CodeState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "لم تستلم الكود؟ تواصل مع الإدارة عبر واتساب",
            style = MaterialTheme.typography.bodySmall,
            color = AutoDriveText.Secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(4.dp)
                .clickable { WhatsAppHelper.requestInviteCode(context, viewModel.requestMessage()) }
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "اطلب كود انضمام",
            style = MaterialTheme.typography.bodyMedium,
            color = AutoDriveFinance.Withdrawable,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(4.dp)
                .clickable { WhatsAppHelper.requestInviteCode(context, viewModel.requestMessage()) }
        )

        Spacer(Modifier.weight(1f))

        AutoDrivePrimaryButton(
            text = "تحقق من الكود ←",
            modifier = Modifier.fillMaxWidth(),
            onClick = { viewModel.verify(code) },
            enabled = code.matches(Regex("^[0-9]{8}$")),
            loading = state is CodeState.Loading
        )
        Spacer(Modifier.height(16.dp))
    }
}
