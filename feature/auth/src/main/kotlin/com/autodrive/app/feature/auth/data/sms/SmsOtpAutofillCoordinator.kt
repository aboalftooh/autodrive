package com.autodrive.app.feature.auth.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-scoped SMS OTP bridge.
 *
 * It starts listening before the network request sends the OTP, so a fast SMS cannot arrive in
 * the navigation gap between the phone screen and the OTP screen. The latest OTP/consent intent
 * is replayed to the OTP screen when it becomes visible.
 */
object SmsOtpAutofillCoordinator {
    private val otpRegex = Regex("""(?<!\d)\d{6}(?!\d)""")
    private val lock = Any()

    private var receiverRegistered = false

    private val _otp = MutableStateFlow<String?>(null)
    val otp: StateFlow<String?> = _otp.asStateFlow()

    private val _consentIntent = MutableStateFlow<Intent?>(null)
    val consentIntent: StateFlow<Intent?> = _consentIntent.asStateFlow()

    fun startChallenge(context: Context) {
        val appContext = context.applicationContext
        ensureReceiver(appContext)
        _otp.value = null
        _consentIntent.value = null
        startGoogleListeners(appContext)
    }

    /** Ensures fallback listeners exist without clearing an OTP already captured before navigation. */
    fun ensureListening(context: Context) {
        val appContext = context.applicationContext
        ensureReceiver(appContext)
        startGoogleListeners(appContext)
    }

    fun acceptConsentResult(data: Intent?) {
        val body = data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE).orEmpty()
        publishMessage(body)
    }

    fun consumeOtp(value: String) {
        if (_otp.value == value) _otp.value = null
    }

    fun consumeConsentIntent(value: Intent) {
        if (_consentIntent.value === value) _consentIntent.value = null
    }

    private fun startGoogleListeners(context: Context) {
        val client = SmsRetriever.getClient(context)
        client.startSmsRetriever()
        client.startSmsUserConsent(null)
    }

    private fun ensureReceiver(context: Context) {
        synchronized(lock) {
            if (receiverRegistered) return
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (intent.action != SmsRetriever.SMS_RETRIEVED_ACTION) return
                    @Suppress("DEPRECATION")
                    val status = intent.getParcelableExtra(SmsRetriever.EXTRA_STATUS) as? Status
                    if (status?.statusCode != CommonStatusCodes.SUCCESS) return

                    val message = intent.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE).orEmpty()
                    if (publishMessage(message)) return

                    @Suppress("DEPRECATION")
                    val consent = intent.getParcelableExtra(
                        SmsRetriever.EXTRA_CONSENT_INTENT,
                    ) as? Intent
                    if (consent != null) _consentIntent.value = consent
                }
            }
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
                ContextCompat.RECEIVER_EXPORTED,
            )
            receiverRegistered = true
        }
    }

    private fun publishMessage(message: String): Boolean {
        val code = otpRegex.find(message)?.value ?: return false
        _otp.value = code
        return true
    }
}
