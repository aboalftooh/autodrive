package com.autodrive.app.feature.auth.presentation.join

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveTextButton
import com.autodrive.app.core.designsystem.components.inputs.AutoDriveTextField
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText

@Composable
fun CodeInputScreen(
    onOtpReady: (phone: String, devOtp: String?) -> Unit,
    onBack: () -> Unit,
    viewModel: CodeInputViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var code by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is JoinCodeState.OtpReady) {
            val ready = state as JoinCodeState.OtpReady
            onOtpReady(ready.phone, ready.devOtp)
            viewModel.reset()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(AutoDriveSurface.Canvas).padding(24.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        AutoDriveTextButton(text = "رجوع", onClick = onBack)
        Spacer(Modifier.height(28.dp))
        Text("كود الانضمام", style = MaterialTheme.typography.headlineLarge, color = AutoDriveText.Primary)
        Spacer(Modifier.height(8.dp))
        Text("أدخل الكود المكون من 8 أرقام والصادر من Verto", style = MaterialTheme.typography.bodyMedium, color = AutoDriveText.Secondary)
        Spacer(Modifier.height(24.dp))
        AutoDriveTextField(
            value = code,
            onValueChange = { code = viewModel.normalizeDigits(it) },
            label = "كود الانضمام",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        if (state is JoinCodeState.Error) {
            Spacer(Modifier.height(12.dp))
            Text((state as JoinCodeState.Error).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))
        AutoDrivePrimaryButton(
            text = "متابعة",
            modifier = Modifier.fillMaxWidth(),
            onClick = { viewModel.submit(code) },
            enabled = code.length == 8 && state !is JoinCodeState.Loading,
            loading = state is JoinCodeState.Loading,
        )
    }
}
