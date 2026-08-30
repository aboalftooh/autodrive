package com.autodrive.app.feature.auth.presentation.login

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.autodrive.app.feature.auth.BuildConfig
import com.autodrive.app.feature.auth.data.sms.SmsOtpAutofillCoordinator
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.components.actions.AutoDriveIconButton


@Composable
fun OtpInputScreen(
    phoneNumber: String,
    devOtp: String? = null,
    onVerified: () -> Unit,
    onBack: () -> Unit,
    viewModel: PhoneAuthViewModel = hiltViewModel()
) {
    val state by viewModel.otpState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(phoneNumber) {
        // A new OTP challenge must always start empty. Never preload clipboard content,
        // because it may contain the previous (already consumed) verification code.
        viewModel.initOtp(phoneNumber, devOtp.takeIf { BuildConfig.DEBUG })
        focusRequester.requestFocus()
    }

    SmsAutofillEffect(onOtpReceived = viewModel::onOtpChanged)

    LaunchedEffect(state.otp, state.isLoading, state.errorMessage) {
        if (state.otp.length == 6 && !state.isLoading && state.errorMessage == null) {
            viewModel.verifyOtp()
        }
    }

    LaunchedEffect(state.isVerified) {
        if (state.isVerified) onVerified()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(AutoDriveSurface.Canvas, AutoDriveSurface.Raised.copy(alpha = 0.68f), AutoDriveSurface.Canvas)
                )
            )
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        AutoDriveIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "رجوع",
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart),
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(76.dp))
            LockBadge()
            Spacer(Modifier.height(34.dp))

            Text(
                text = "أدخل رمز التحقق",
                style = MaterialTheme.typography.headlineLarge,
                color = AutoDriveText.Primary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "أرسلنا رمزاً إلى",
                style = MaterialTheme.typography.bodyMedium,
                color = AutoDriveText.Secondary,
                textAlign = TextAlign.Center
            )
            Text(
                text = state.phoneNumber.ifBlank { phoneNumber },
                style = MaterialTheme.typography.titleMedium,
                color = AutoDriveFinance.Pending,
                textAlign = TextAlign.Center
            )

            if (BuildConfig.DEBUG && !devOtp.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "وضع تطوير: $devOtp",
                    style = MaterialTheme.typography.labelMedium,
                    color = AutoDriveFinance.Pending
                )
            }

            Spacer(Modifier.height(40.dp))

            BasicTextField(
                value = state.otp,
                onValueChange = viewModel::onOtpChanged,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                textStyle = TextStyle(color = AutoDriveText.Primary, fontSize = 1.sp),
                decorationBox = {
                    OtpBoxes(value = state.otp, isError = state.errorMessage != null)
                }
            )

            Spacer(Modifier.height(16.dp))

            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            state.infoMessage?.let { message ->
                Text(
                    text = message,
                    color = AutoDriveFinance.Pending,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.weight(1f))
            HelpCard()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SmsAutofillEffect(onOtpReceived: (String) -> Unit) {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onOtpReceived)
    val receivedOtp by SmsOtpAutofillCoordinator.otp.collectAsState()
    val consentIntent by SmsOtpAutofillCoordinator.consentIntent.collectAsState()

    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            SmsOtpAutofillCoordinator.acceptConsentResult(result.data)
        }
    }

    LaunchedEffect(Unit) {
        // Idempotent fallback for process recreation/deep links. It never clears an OTP already
        // captured by the process-scoped receiver before this screen became visible.
        SmsOtpAutofillCoordinator.ensureListening(context)
    }

    LaunchedEffect(receivedOtp) {
        receivedOtp?.let { otp ->
            callback.value(otp)
            SmsOtpAutofillCoordinator.consumeOtp(otp)
        }
    }

    LaunchedEffect(consentIntent) {
        consentIntent?.let { intent ->
            SmsOtpAutofillCoordinator.consumeConsentIntent(intent)
            consentLauncher.launch(intent)
        }
    }
}

@Composable
private fun OtpBoxes(value: String, isError: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        repeat(6) { index ->
            val char = value.getOrNull(index)?.toString().orEmpty()
            val active = index == value.length.coerceAtMost(5)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(62.dp)
                    .border(
                        width = 1.2.dp,
                        color = when {
                            isError -> MaterialTheme.colorScheme.error
                            active -> AutoDriveFinance.Pending
                            char.isNotEmpty() -> AutoDriveFinance.Withdrawable.copy(alpha = 0.82f)
                            else -> AutoDriveBorderColor.Default
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(AutoDriveSurface.Canvas.copy(alpha = 0.74f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = char.ifBlank { if (active) "|" else "" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (char.isNotEmpty()) AutoDriveText.Primary else AutoDriveFinance.Pending,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LockBadge() {
    Box(
        modifier = Modifier
            .size(126.dp)
            .border(1.dp, AutoDriveFinance.Pending, CircleShape)
            .background(AutoDriveFinance.Pending.copy(alpha = 0.06f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = AutoDriveFinance.Pending,
            modifier = Modifier.size(62.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = AutoDriveSurface.Canvas,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}

@Composable
private fun HelpCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AutoDriveBorderColor.Default, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = AutoDriveSurface.Canvas.copy(alpha = 0.62f)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Rounded.VerifiedUser,
                contentDescription = null,
                tint = AutoDriveFinance.Pending,
                modifier = Modifier.size(38.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("رمز التحقق خاص بك", color = AutoDriveText.Primary, style = MaterialTheme.typography.bodyMedium)
                Text("لا تشاركه مع أي شخص", color = AutoDriveText.Secondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
