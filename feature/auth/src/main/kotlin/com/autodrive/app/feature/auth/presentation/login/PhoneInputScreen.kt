package com.autodrive.app.feature.auth.presentation.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveTextButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveTextButtonTone
import com.autodrive.app.core.designsystem.components.inputs.AutoDriveTextField
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.feature.auth.R
import com.autodrive.app.feature.auth.domain.validation.SudanPhoneNumber

@Composable
fun PhoneInputScreen(
    onBack: () -> Unit,
    onOtpSent: (phone: String, devOtp: String?) -> Unit,
    onJoinCodeRequired: (phone: String) -> Unit,
    viewModel: PhoneAuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var phone by remember { mutableStateOf("") }
    var showTermsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        when (val current = state) {
            is PhoneAuthState.OtpSent -> onOtpSent(current.phone, current.devOtp)
            is PhoneAuthState.JoinCodeRequired -> onJoinCodeRequired(current.phone)
            else -> Unit
        }
    }

    val isValidPhone = SudanPhoneNumber.normalize(phone) != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AutoDriveSurface.Canvas,
                        AutoDriveSurface.Raised.copy(alpha = 0.72f),
                        AutoDriveSurface.Canvas,
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(18.dp))
        LoginHero()
        Spacer(Modifier.height(42.dp))

        Text(
            text = "أدخل رقم هاتفك",
            style = MaterialTheme.typography.headlineLarge,
            color = AutoDriveText.Primary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "سنحدد حالة حسابك ثم نرسل رمز التحقق عند توفر الدخول",
            style = MaterialTheme.typography.bodyMedium,
            color = AutoDriveText.Secondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(36.dp))

        AutoDriveTextField(
            value = phone,
            onValueChange = { input ->
                val filtered = input.filter { it.isDigit() || it == '+' || it == '-' || it == ' ' }
                if (filtered.length <= 20) phone = filtered
            },
            label = null,
            leadingIcon = Icons.Rounded.PhoneAndroid,
            placeholder = "09 123 456 789",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            isError = state is PhoneAuthState.Error,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(10.dp))
        if (state is PhoneAuthState.Error) {
            Text(
                text = (state as PhoneAuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(22.dp))
        AutoDrivePrimaryButton(
            text = "متابعة",
            modifier = Modifier.fillMaxWidth(),
            onClick = { viewModel.sendOtp(phone) },
            enabled = isValidPhone && state !is PhoneAuthState.Loading,
            loading = state is PhoneAuthState.Loading,
        )
        Spacer(Modifier.height(14.dp))
        AutoDriveTextButton(
            text = "بالمتابعة أنت توافق على سياسة الاستخدام",
            onClick = { showTermsSheet = true },
            tone = AutoDriveTextButtonTone.Primary,
        )

        Spacer(Modifier.height(92.dp))
        SecureHint()
        Spacer(Modifier.height(20.dp))
    }

    if (showTermsSheet) {
        TermsScreen(
            onAccepted = { showTermsSheet = false },
            onBack = { showTermsSheet = false }
        )
    }
}

@Composable
private fun LoginHero() {
    Image(
        painter = painterResource(R.drawable.login_hero),
        contentDescription = null,
        modifier = Modifier.fillMaxWidth(),
        contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
    )
}

@Composable
private fun SecureHint() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) { }
}
