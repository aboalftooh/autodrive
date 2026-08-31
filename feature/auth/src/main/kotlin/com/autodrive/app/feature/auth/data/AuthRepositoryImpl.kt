package com.autodrive.app.feature.auth.data

import android.content.Context
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.core.network.dto.AutoDriveUserDto
import com.autodrive.app.core.network.dto.LinkPhoneUserParams
import com.autodrive.app.core.network.dto.SendPhoneOtpResponse
import com.autodrive.app.core.network.dto.VerifyCodeRpcParams
import com.autodrive.app.core.network.dto.VerifyCodeRpcResult
import com.autodrive.app.core.network.dto.VerifyPhoneOtpResponse
import com.autodrive.app.core.platform.notifications.FcmTokenUploader
import com.autodrive.app.core.platform.notifications.PushTokenRepository
import com.autodrive.app.core.session.domain.RegistrationState
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.session.domain.SessionWriter
import com.autodrive.app.core.sync.data.SyncManager
import com.autodrive.app.core.sync.data.SyncScope
import com.autodrive.app.core.sync.domain.RealtimeController
import com.autodrive.app.feature.auth.BuildConfig
import com.autodrive.app.feature.auth.data.sms.SmsOtpAutofillCoordinator
import com.autodrive.app.feature.auth.data.sms.SmsRetrieverAppHash
import com.autodrive.app.feature.auth.domain.model.CodeVerificationResult
import com.autodrive.app.feature.auth.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
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
    @ApplicationContext private val appContext: Context,
) : AuthRepository {
    private val db get() = supabase.client.postgrest

    override suspend fun sendPhoneOtp(phone: String): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            supabase.client.auth.awaitInitialization()
            SmsOtpAutofillCoordinator.startChallenge(appContext)
            val appHash = SmsRetrieverAppHash.current(appContext)

            val response = supabase.client.functions.invoke(
                function = "send-phone-otp",
                body = buildJsonObject {
                    put("phone", phone.trim())
                    appHash?.let { put("app_hash", it) }
                }
            )
            if (!response.status.isSuccess()) {
                val errBody = runCatching { response.body<Map<String, String>>() }.getOrNull()
                val msg = errBody?.get("error") ?: "فشل إرسال رمز التحقق"
                return@runCatching Result.Error(msg)
            }
            val devOtp = if (BuildConfig.DEBUG) {
                runCatching { response.body<SendPhoneOtpResponse>().devOtp }.getOrNull()
            } else null
            Result.Success(devOtp)
        }.getOrElse { e ->
            Result.Error(parseEdgeFunctionError(e, "فشل إرسال رمز التحقق — حاول مجدداً"), e)
        }
    }

    override suspend fun verifyPhoneOtp(phone: String, otp: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = supabase.client.functions.invoke(
                function = "verify-phone-otp",
                body = buildJsonObject {
                    put("phone", phone.trim())
                    put("otp", otp.trim())
                }
            )
            if (!response.status.isSuccess()) {
                val errBody = runCatching { response.body<Map<String, String>>() }.getOrNull()
                val msg = errBody?.get("error") ?: "رمز التحقق غير صحيح"
                return@runCatching Result.Error(msg)
            }
            val body = response.body<VerifyPhoneOtpResponse>()
            supabase.client.auth.importSession(
                UserSession(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken,
                    expiresIn = body.expiresIn,
                    tokenType = body.tokenType
                )
            )
            val session = supabase.client.auth.currentSessionOrNull()
                ?: return@runCatching Result.Error("لم تُنشأ الجلسة — أعد المحاولة")
            val resolvedUserId = resolveUserId(session)
            sessionWriter.updateSession { current ->
                current.copy(userId = resolvedUserId, phone = phone.trim())
            }
            runCatching {
                supabase.client.postgrest.rpc(
                    "link_phone_user_by_phone",
                    buildJsonObject { put("p_phone", phone.trim()) }
                )
            }
            refreshRegistrationStateFromSupabase()
            Result.Success(Unit)
        }.getOrElse { e ->
            Result.Error(parseEdgeFunctionError(e, "فشل التحقق — حاول مجدداً"), e)
        }
    }

    override suspend fun verifyInviteCode(code: String): CodeVerificationResult = withContext(Dispatchers.IO) {
        runCatching { supabase.client.auth.awaitInitialization() }

        val freshSession = supabase.client.auth.currentSessionOrNull()
        if (freshSession == null) {
            return@withContext CodeVerificationResult.Error("يجب تسجيل الدخول بالهاتف أولاً")
        }

        runCatching {
            val rpcResult = supabase.client.postgrest.rpc(
                "verify_invite_code_v2",
                VerifyCodeRpcParams(code)
            ).decodeList<VerifyCodeRpcResult>().firstOrNull()
                ?: return@runCatching CodeVerificationResult.Error("استجابة فارغة من الخادم")

            when {
                !rpcResult.isValid && rpcResult.reason == "NOT_FOUND" -> CodeVerificationResult.Invalid
                !rpcResult.isValid && rpcResult.reason == "ALREADY_USED" -> CodeVerificationResult.AlreadyUsed
                !rpcResult.isValid && rpcResult.reason == "EXPIRED" -> CodeVerificationResult.Expired
                !rpcResult.isValid && rpcResult.reason == "NOT_A_MARKETER_CODE" -> CodeVerificationResult.Invalid
                !rpcResult.isValid -> CodeVerificationResult.Error(rpcResult.reason)
                else -> {
                    val clientId = rpcResult.clientId
                        ?: return@runCatching CodeVerificationResult.Error("بيانات الكود ناقصة")
                    val orgId = rpcResult.orgId
                        ?: return@runCatching CodeVerificationResult.Error("بيانات الكود ناقصة")

                    val resolvedUserId = resolveUserId(freshSession)
                    sessionWriter.updateSession { current ->
                        current.copy(
                            userId = resolvedUserId,
                            clientId = clientId,
                            orgId = orgId,
                            pendingInviteCode = code
                        )
                    }

                    val existingUserResult = runCatching {
                        db["autodrive_users"]
                            .select(Columns.ALL) { filter { eq("client_id", clientId) } }
                            .decodeList<AutoDriveUserDto>()
                            .firstOrNull()
                    }
                    if (existingUserResult.isFailure) {
                        return@runCatching CodeVerificationResult.Error(
                            "تعذّر التحقق من حالة الحساب — تحقق من الإنترنت وحاول مجدداً"
                        )
                    }
                    val existingUser = existingUserResult.getOrNull()

                    if (existingUser != null) {
                        val linkResult = runCatching {
                            supabase.client.postgrest.rpc(
                                "link_phone_user",
                                LinkPhoneUserParams(code)
                            )
                        }
                        if (linkResult.isFailure) {
                            return@runCatching CodeVerificationResult.Error(
                                "تعذّر ربط الحساب: ${linkResult.exceptionOrNull()?.message ?: "خطأ غير معروف"}"
                            )
                        }
                        sessionWriter.updateSession { current ->
                            current.copy(
                                userName = existingUser.fullName,
                                accountType = existingUser.accountType,
                                phone = existingUser.phone,
                                registrationState = RegistrationState.COMPLETE,
                                isLoggedIn = true,
                                pendingInviteCode = null
                            )
                        }
                        FcmTokenUploader.trigger(appContext, pushTokens)
                    }

                    CodeVerificationResult.Success(
                        clientId = clientId,
                        orgId = orgId,
                        isExistingUser = existingUser != null
                    )
                }
            }
        }.getOrElse { e ->
            CodeVerificationResult.Error(e.message ?: "خطأ غير متوقع")
        }
    }

    override suspend fun restoreSession(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            supabase.client.auth.awaitInitialization()
            val session = supabase.client.auth.currentSessionOrNull()
            val user = session?.user
            if (user != null && user.email.isNullOrBlank() && user.phone.isNullOrBlank()) {
                return@runCatching false
            }
            if (session != null && sessionReader.currentSession().userId.isNullOrBlank()) {
                val resolvedUserId = resolveUserId(session)
                sessionWriter.updateSession { current -> current.copy(userId = resolvedUserId) }
            }
            if (session == null) return@runCatching false

            // A transient profile lookup failure must never downgrade a previously complete session.
            // refreshRegistrationStateFromSupabase mutates local state only after a successful lookup.
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
            if (scopeToLogout != null) {
                syncManager.quiesceAndClearForLogout(scopeToLogout)
            }
            runCatching { supabase.client.auth.signOut() }
        } finally {
            if (scopeToLogout != null) syncManager.releaseLogoutBarrier(scopeToLogout)
        }
        Unit
    }

    suspend fun syncPushToken(token: String): kotlin.Result<Unit> =
        pushTokens.upsertCurrentUserToken(token)

    override fun isLoggedIn() = sessionReader.currentSession().isLoggedIn
    override fun isRegistrationComplete() = sessionReader.currentSession().isRegistrationComplete

    override fun getCurrentUserId(): String =
        supabase.client.auth.currentSessionOrNull()?.user?.id
            ?: sessionReader.currentSession().userId?.takeIf { it.isNotBlank() }
            ?: ""

    private fun parseEdgeFunctionError(e: Throwable, fallback: String): String {
        val raw = e.message ?: return fallback
        return when {
            raw.contains("429") -> "انتظر دقيقة واحدة قبل إعادة الإرسال"
            raw.contains("timeout", ignoreCase = true) -> "تعذّر الاتصال — تحقق من الإنترنت"
            else -> {
                val jsonPart = raw.substringBefore("\nURL:").trim()
                if (jsonPart.startsWith("{")) {
                    runCatching {
                        Json { ignoreUnknownKeys = true }
                            .decodeFromString<Map<String, String>>(jsonPart)["error"]
                    }.getOrNull()?.takeIf { it.isNotBlank() } ?: fallback
                } else fallback
            }
        }
    }

    /**
     * Returns true only when the server lookup completed successfully.
     * On transport/server failure it leaves the encrypted cached registration state untouched.
     */
    private suspend fun refreshRegistrationStateFromSupabase(): Boolean {
        val uid = getCurrentUserId()
        if (uid.isBlank()) return false

        val linkedResult = runCatching {
            db["autodrive_users"]
                .select(Columns.ALL) {
                    filter { eq("user_id", uid) }
                    limit(1)
                }
                .decodeList<AutoDriveUserDto>()
                .firstOrNull()
        }
        if (linkedResult.isFailure) return false

        val linked = linkedResult.getOrNull()
        if (linked == null) {
            sessionWriter.updateSession { current ->
                current.copy(isLoggedIn = true, registrationState = RegistrationState.INCOMPLETE)
            }
            return true
        }

        sessionWriter.updateSession { current ->
            current.copy(
                userId = uid,
                clientId = linked.clientId,
                orgId = linked.orgId,
                userName = linked.fullName,
                accountType = linked.accountType,
                phone = linked.phone,
                isLoggedIn = true,
                registrationState = if (linked.onboardingCompleted) {
                    RegistrationState.COMPLETE
                } else {
                    RegistrationState.INCOMPLETE
                }
            )
        }
        FcmTokenUploader.trigger(appContext, pushTokens)
        return true
    }

    private suspend fun resolveUserId(session: UserSession): String? =
        session.user?.id
            ?: runCatching { supabase.client.auth.retrieveUser(session.accessToken).id }.getOrNull()
}
