package com.autodrive.app.feature.auth.data

import android.content.Context
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.core.network.dto.AutoDriveUserDto
import com.autodrive.app.core.network.dto.SendPhoneOtpResponse
import com.autodrive.app.core.network.dto.VerifyPhoneOtpResponse
import com.autodrive.app.core.platform.notifications.FcmTokenStore
import com.autodrive.app.core.platform.notifications.FcmTokenUploader
import com.autodrive.app.core.platform.notifications.PushTokenRepository
import com.autodrive.app.core.session.domain.RegistrationState
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.session.domain.SessionWriter
import com.autodrive.app.core.sync.data.SyncManager
import com.autodrive.app.core.sync.data.SyncScope
import com.autodrive.app.core.sync.domain.RealtimeController
import com.autodrive.app.feature.auth.BuildConfig
import com.autodrive.app.feature.auth.data.registration.InstallationIdProvider
import com.autodrive.app.feature.auth.data.registration.JoinCodeVerificationDto
import com.autodrive.app.feature.auth.data.registration.PhoneEntryDto
import com.autodrive.app.feature.auth.data.registration.RegistrationEnvelope
import com.autodrive.app.feature.auth.data.sms.SmsOtpAutofillCoordinator
import com.autodrive.app.feature.auth.data.sms.SmsRetrieverAppHash
import com.autodrive.app.feature.auth.domain.model.JoinCodeVerificationResult
import com.autodrive.app.feature.auth.domain.model.PhoneEntryResult
import com.autodrive.app.feature.auth.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.client.call.body
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.runCatching

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabase: AutoDriveSupabase,
    private val sessionReader: SessionReader,
    private val sessionWriter: SessionWriter,
    private val syncManager: SyncManager,
    private val realtimeController: RealtimeController,
    private val pushTokens: PushTokenRepository,
    private val installationId: InstallationIdProvider,
    @ApplicationContext private val appContext: Context,
) : AuthRepository {
    private val db get() = supabase.client.postgrest

    override suspend fun enterPhone(phone: String): PhoneEntryResult = withContext(Dispatchers.IO) {
        runCatching {
            val normalized = phone.trim()
            val response = supabase.client.functions.invoke(
                function = "autodrive-registration",
                body = buildJsonObject {
                    put("action", "phone_entry")
                    put("phone", normalized)
                    put("device_id", installationId.get())
                    put("platform", "android")
                    currentPushToken()?.let { put("push_token", it) }
                }
            )
            if (!response.status.isSuccess()) {
                val message = runCatching { response.body<Map<String, String>>()["error"] }.getOrNull()
                    ?: "تعذّر التحقق من حالة الحساب"
                return@runCatching PhoneEntryResult.Error(message)
            }
            val data = response.body<RegistrationEnvelope<PhoneEntryDto>>().data
                ?: return@runCatching PhoneEntryResult.Error("استجابة غير مكتملة من الخادم")

            sessionWriter.updateSession { current ->
                current.copy(phone = normalized, pendingInviteCode = null)
            }

            when (data.nextAction) {
                "LOGIN_OTP" -> PhoneEntryResult.LoginOtp
                "JOIN_CODE_REQUIRED" -> PhoneEntryResult.JoinCodeRequired
                "ACCOUNT_SELECTION_REQUIRED" -> PhoneEntryResult.AccountSelectionRequired
                else -> PhoneEntryResult.Error("حالة حساب غير معروفة")
            }
        }.getOrElse { e -> PhoneEntryResult.Error(parseEdgeFunctionError(e, "تعذّر الاتصال — حاول مجدداً")) }
    }

    override suspend fun verifyJoinCode(phone: String, code: String): JoinCodeVerificationResult = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedPhone = phone.trim()
            val normalizedCode = code.trim()
            val response = supabase.client.functions.invoke(
                function = "autodrive-registration",
                body = buildJsonObject {
                    put("action", "verify_join_code")
                    put("phone", normalizedPhone)
                    put("code", normalizedCode)
                    put("device_id", installationId.get())
                }
            )
            if (!response.status.isSuccess()) {
                val message = runCatching { response.body<Map<String, String>>()["error"] }.getOrNull()
                    ?: "تعذّر التحقق من كود الانضمام"
                return@runCatching JoinCodeVerificationResult.Error(message)
            }
            val data = response.body<RegistrationEnvelope<JoinCodeVerificationDto>>().data
                ?: return@runCatching JoinCodeVerificationResult.Error("استجابة غير مكتملة من الخادم")
            if (!data.isValid) return@runCatching JoinCodeVerificationResult.Invalid(data.reason)

            val clientId = data.clientId ?: return@runCatching JoinCodeVerificationResult.Error("معرف العميل مفقود")
            val orgId = data.orgId ?: return@runCatching JoinCodeVerificationResult.Error("معرف المؤسسة مفقود")
            val accountType = data.accountType ?: return@runCatching JoinCodeVerificationResult.Error("نوع الحساب مفقود")
            sessionWriter.updateSession { current ->
                current.copy(
                    phone = normalizedPhone,
                    clientId = clientId,
                    orgId = orgId,
                    accountType = accountType,
                    pendingInviteCode = normalizedCode,
                )
            }
            JoinCodeVerificationResult.Valid(clientId, orgId, accountType)
        }.getOrElse { e -> JoinCodeVerificationResult.Error(parseEdgeFunctionError(e, "تعذّر التحقق من كود الانضمام")) }
    }

    override suspend fun sendPhoneOtp(phone: String): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            supabase.client.auth.awaitInitialization()
            SmsOtpAutofillCoordinator.startChallenge(appContext)
            val appHash = SmsRetrieverAppHash.current(appContext)
            val inviteCode = sessionReader.currentSession().pendingInviteCode?.takeIf { it.isNotBlank() }
            val response = supabase.client.functions.invoke(
                function = "send-phone-otp",
                body = buildJsonObject {
                    put("phone", phone.trim())
                    inviteCode?.let { put("invite_code", it) }
                    appHash?.let { put("app_hash", it) }
                }
            )
            if (!response.status.isSuccess()) {
                val errBody = runCatching { response.body<Map<String, String>>() }.getOrNull()
                return@runCatching Result.Error(errBody?.get("error") ?: "فشل إرسال رمز التحقق")
            }
            val devOtp = if (BuildConfig.DEBUG) {
                runCatching { response.body<SendPhoneOtpResponse>().devOtp }.getOrNull()
            } else null
            Result.Success(devOtp)
        }.getOrElse { e -> Result.Error(parseEdgeFunctionError(e, "فشل إرسال رمز التحقق — حاول مجدداً"), e) }
    }

    override suspend fun verifyPhoneOtp(phone: String, otp: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val inviteCode = sessionReader.currentSession().pendingInviteCode?.takeIf { it.isNotBlank() }
            val response = supabase.client.functions.invoke(
                function = "verify-phone-otp",
                body = buildJsonObject {
                    put("phone", phone.trim())
                    put("otp", otp.trim())
                    inviteCode?.let { put("invite_code", it) }
                }
            )
            if (!response.status.isSuccess()) {
                val errBody = runCatching { response.body<Map<String, String>>() }.getOrNull()
                return@runCatching Result.Error(errBody?.get("error") ?: "رمز التحقق غير صحيح")
            }
            val body = response.body<VerifyPhoneOtpResponse>()
            importSession(body.accessToken, body.refreshToken, body.expiresIn, body.tokenType)
            val authSession = supabase.client.auth.currentSessionOrNull()
                ?: return@runCatching Result.Error("لم تُنشأ الجلسة — أعد المحاولة")
            val resolvedUserId = resolveUserId(authSession)
            sessionWriter.updateSession { current ->
                current.copy(userId = resolvedUserId, phone = phone.trim(), pendingInviteCode = null)
            }
            if (!refreshRegistrationStateFromSupabase()) {
                return@runCatching Result.Error("تعذّر تحميل بيانات الحساب")
            }
            Result.Success(Unit)
        }.getOrElse { e -> Result.Error(parseEdgeFunctionError(e, "فشل التحقق — حاول مجدداً"), e) }
    }

    override suspend fun restoreSession(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            supabase.client.auth.awaitInitialization()
            val session = supabase.client.auth.currentSessionOrNull()
            val user = session?.user
            if (user != null && user.email.isNullOrBlank() && user.phone.isNullOrBlank()) return@runCatching false
            if (session != null && sessionReader.currentSession().userId.isNullOrBlank()) {
                sessionWriter.updateSession { current -> current.copy(userId = resolveUserId(session)) }
            }
            if (session == null) return@runCatching false
            refreshRegistrationStateFromSupabase()
            true
        }.getOrDefault(false)
    }

    override suspend fun signOut() = withContext(Dispatchers.IO) {
        val scopeToLogout = SyncScope.from(sessionReader.currentSession())
        runCatching { pushTokens.deleteCurrentUserToken() }
        realtimeController.stop()
        try {
            if (scopeToLogout != null) syncManager.beginLogout(scopeToLogout)
            sessionWriter.clearSession()
            if (scopeToLogout != null) syncManager.quiesceAndClearForLogout(scopeToLogout)
            runCatching { supabase.client.auth.signOut() }
        } finally {
            if (scopeToLogout != null) syncManager.releaseLogoutBarrier(scopeToLogout)
        }
        Unit
    }

    suspend fun syncPushToken(token: String): kotlin.Result<Unit> = pushTokens.upsertCurrentUserToken(token)
    override fun isLoggedIn() = sessionReader.currentSession().isLoggedIn
    override fun isRegistrationComplete() = sessionReader.currentSession().isRegistrationComplete
    override fun getCurrentUserId(): String =
        supabase.client.auth.currentSessionOrNull()?.user?.id
            ?: sessionReader.currentSession().userId?.takeIf { it.isNotBlank() }
            ?: ""

    private fun currentPushToken(): String? = FcmTokenStore.pending(appContext) ?: FcmTokenStore.lastUploaded(appContext)

    private suspend fun importSession(accessToken: String, refreshToken: String, expiresIn: Long, tokenType: String) {
        supabase.client.auth.importSession(UserSession(accessToken = accessToken, refreshToken = refreshToken, expiresIn = expiresIn, tokenType = tokenType))
    }

    private fun parseEdgeFunctionError(e: Throwable, fallback: String): String {
        val raw = e.message ?: return fallback
        return when {
            raw.contains("429") -> "انتظر دقيقة واحدة قبل إعادة المحاولة"
            raw.contains("timeout", ignoreCase = true) || raw.contains("network", ignoreCase = true) -> "تعذّر الاتصال — تحقق من الإنترنت"
            else -> {
                val jsonPart = raw.substringBefore("\nURL:").trim()
                if (jsonPart.startsWith("{")) {
                    runCatching { Json { ignoreUnknownKeys = true }.decodeFromString<Map<String, String>>(jsonPart)["error"] }
                        .getOrNull()?.takeIf { it.isNotBlank() } ?: fallback
                } else fallback
            }
        }
    }

    private suspend fun refreshRegistrationStateFromSupabase(): Boolean {
        val uid = getCurrentUserId()
        if (uid.isBlank()) return false
        val linkedResult = runCatching {
            db["autodrive_users"].select(Columns.ALL) {
                filter { eq("user_id", uid) }
                limit(1)
            }.decodeList<AutoDriveUserDto>().firstOrNull()
        }
        if (linkedResult.isFailure) return false
        val linked = linkedResult.getOrNull() ?: return false
        sessionWriter.updateSession { current ->
            current.copy(
                userId = uid,
                clientId = linked.clientId,
                orgId = linked.orgId,
                userName = linked.fullName,
                accountType = linked.accountType,
                phone = linked.phone,
                isLoggedIn = true,
                registrationState = if (linked.onboardingCompleted) RegistrationState.COMPLETE else RegistrationState.INCOMPLETE,
                pendingInviteCode = null,
            )
        }
        FcmTokenUploader.trigger(appContext, pushTokens)
        return true
    }

    private suspend fun resolveUserId(session: UserSession): String? =
        session.user?.id ?: runCatching { supabase.client.auth.retrieveUser(session.accessToken).id }.getOrNull()
}
