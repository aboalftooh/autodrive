package com.autodrive.app.feature.auth.presentation.login

import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.autodrive.app.feature.auth.BuildConfig
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText

private fun readClipboardText(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return clipboard.primaryClip
        ?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.coerceToText(context)
        ?.toString()
}

@Composable
fun OtpInputScreen(
    phoneNumber: String,
    devOtp: String? = null,
    onVerified: () -> Unit,
    onBack: () -> Unit,
    viewModel: PhoneAuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.otpState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(phoneNumber) {
        viewModel.initOtp(phoneNumber, devOtp.takeIf { BuildConfig.DEBUG })
        viewModel.tryAutofillFromClipboard(readClipboardText(context))
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
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "رجوع", tint = AutoDriveFinance.Pending)
        }

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

private val OTP_REGEX = Regex("""(?<!\d)\d{6}(?!\d)""")

@Composable
private fun SmsAutofillEffect(onOtpReceived: (String) -> Unit) {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onOtpReceived)

    DisposableEffect(context) {
        SmsRetriever.getClient(context).startSmsRetriever()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action != SmsRetriever.SMS_RETRIEVED_ACTION) return
                @Suppress("DEPRECATION")
                val status = intent.getParcelableExtra(SmsRetriever.EXTRA_STATUS) as? Status
                if (status?.statusCode != CommonStatusCodes.SUCCESS) return
                val body = intent.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE).orEmpty()
                OTP_REGEX.find(body)?.value?.let { callback.value(it) }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
            ContextCompat.RECEIVER_EXPORTED,
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
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
