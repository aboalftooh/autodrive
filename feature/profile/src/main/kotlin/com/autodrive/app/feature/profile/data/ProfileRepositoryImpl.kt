package com.autodrive.app.feature.profile.data

import androidx.room.withTransaction
import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.database.entities.PendingOperationEntity
import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.core.network.dto.AutoDriveUserUpdateDto
import com.autodrive.app.core.network.dto.RedeemInviteCodeParams
import com.autodrive.app.core.model.account.AccountType
import com.autodrive.app.core.model.account.AutoDriveUser
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.feature.profile.domain.repository.ProfileRepository
import com.autodrive.app.core.platform.notifications.FcmTokenUploader
import com.autodrive.app.core.platform.notifications.PushTokenRepository
import com.autodrive.app.core.session.domain.RegistrationState
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.session.domain.SessionWriter
import com.autodrive.app.core.sync.data.SyncScope
import com.autodrive.app.core.sync.outbox.OUTBOX_CONTRACT_VERSION
import com.autodrive.app.core.sync.outbox.OutboxEntityType
import com.autodrive.app.core.sync.outbox.OutboxOperationType
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val supabase: AutoDriveSupabase,
    private val sessionReader: SessionReader,
    private val sessionWriter: SessionWriter,
    private val db: AutoDriveDatabase,
    private val pushTokens: PushTokenRepository,
    @ApplicationContext private val appContext: Context,
) : ProfileRepository {
    private val opJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = true
    }

    override fun observeUser(userId: String): Flow<AutoDriveUser?> {
        if (userId.isBlank()) return flowOf(null)
        return db.autoDriveUserDao().observe(userId)
            .map { it?.toDomain() }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun saveUser(user: AutoDriveUser): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val inviteCode = sessionReader.currentSession().pendingInviteCode
                ?: return@runCatching Result.Error("كود الدعوة غير موجود — أعد إدخال الكود")

            // ذرّي: يُعلّم الكود used ويُنشئ autodrive_users + marketer_balance في معاملة واحدة
            supabase.client.postgrest.rpc(
                "redeem_invite_code",
                RedeemInviteCodeParams(
                    code         = inviteCode,
                    fullName     = user.fullName,
                    phone        = user.phone,
                    accountType  = user.accountType.name,
                    bankName     = user.bankName,
                    bankAccount  = user.bankAccount,
                    workshopName = user.workshopName,
                    specialty    = user.specialty,
                    workersCount = user.workersCount,
                    address      = user.address
                )
            )
            supabase.client.postgrest["autodrive_users"].update(
                buildJsonObject { put("onboarding_completed", true) }
            ) {
                filter { eq("user_id", user.userId) }
            }

            markRegistrationComplete(user)
            FcmTokenUploader.trigger(appContext, pushTokens)
            Result.Success(Unit)
        }.fold(
            onSuccess = { it },
            onFailure = { e ->
                // السجل موجود بالفعل (client_id_unique) — تسجيل سابق ناجح جزئياً
                // نعتبره نجاحاً: ندير prefs ونترك SyncManager يجلب البيانات الحقيقية
                if (e.message?.contains("autodrive_users_client_id_unique") == true) {
                    markRegistrationComplete(user)
                    FcmTokenUploader.trigger(appContext, pushTokens)
                    return@withContext Result.Success(Unit)
                }
                val msg = when {
                    e.message?.contains("CODE_ALREADY_USED") == true -> "الكود مستخدم بالفعل — تواصل مع الإدارة للحصول على كود جديد"
                    e.message?.contains("CODE_NOT_FOUND") == true    -> "الكود غير صحيح"
                    e.message?.contains("CODE_EXPIRED") == true      -> "انتهت صلاحية الكود"
                    else -> e.message ?: "خطأ في حفظ البيانات"
                }
                Result.Error(msg, e)
            }
        )
    }

    override suspend fun updateUser(user: AutoDriveUser): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val scope = SyncScope.from(sessionReader.currentSession())
                ?: return@runCatching Result.Error("جلسة منتهية. أعد تسجيل الدخول.")
            if (user.userId != scope.userId) {
                return@runCatching Result.Error("جلسة المستخدم غير متطابقة")
            }

            val dto = AutoDriveUserUpdateDto(
                fullName     = user.fullName.ifBlank { null },
                phone        = user.phone.ifBlank { null },
                bankName     = user.bankName,
                bankAccount  = user.bankAccount,
                workshopName = user.workshopName,
                specialty    = user.specialty,
                workersCount = user.workersCount,
                address      = user.address
            )
            val operationId = "profile_${UUID.randomUUID()}"
            val mutationId = UUID.randomUUID().toString()

            db.withTransaction {
                check(SyncScope.from(sessionReader.currentSession()) == scope) { "STALE_LOCAL_MUTATION_SCOPE" }
                val current = db.autoDriveUserDao().get(scope.userId)
                    ?: error("PROFILE_TARGET_NOT_FOUND")
                check(current.clientId == scope.clientId && current.orgId == scope.orgId) {
                    "PROFILE_SCOPE_MISMATCH"
                }
                db.autoDriveUserDao().upsert(
                    current.copy(
                        fullName     = user.fullName,
                        phone        = user.phone,
                        bankName     = user.bankName,
                        bankAccount  = user.bankAccount,
                        workshopName = user.workshopName,
                        specialty    = user.specialty,
                        workersCount = user.workersCount,
                        address      = user.address,
                        syncStatus   = "PENDING"
                    )
                )
                db.pendingOperationDao().insert(
                    PendingOperationEntity(
                        id = operationId,
                        mutationId = mutationId,
                        userId = scope.userId,
                        clientId = scope.clientId,
                        orgId = scope.orgId,
                        entityType = OutboxEntityType.PROFILE,
                        entityId = scope.userId,
                        operation = OutboxOperationType.UPDATE_PROFILE,
                        payload = opJson.encodeToString(dto),
                        contractVersion = OUTBOX_CONTRACT_VERSION,
                    )
                )
            }

            // Preferences are a UI convenience, not the durability authority.
            sessionWriter.updateSession { current ->
                if (SyncScope.from(current) == scope) {
                    current.copy(userName = user.fullName, phone = user.phone)
                } else {
                    current
                }
            }
            Result.Success(Unit)
        }.getOrElse { Result.Error(it.message ?: "خطأ في التحديث", it) }
    }

    private fun markRegistrationComplete(user: AutoDriveUser) {
        sessionWriter.updateSession { current ->
            current.copy(
                userName = user.fullName,
                accountType = user.accountType.name,
                phone = user.phone,
                isLoggedIn = true,
                registrationState = RegistrationState.COMPLETE,
                pendingInviteCode = null
            )
        }
    }

    // ─── Mapper ────────────────────────────────────────────────

    private fun com.autodrive.app.core.database.entities.AutoDriveUserEntity.toDomain() = AutoDriveUser(
        id           = id,
        userId       = userId,
        clientId     = clientId,
        orgId        = orgId,
        accountType  = if (accountType == "WORKSHOP_OWNER") AccountType.WORKSHOP_OWNER else AccountType.MARKETER,
        fullName     = fullName,
        phone        = phone,
        bankName     = bankName,
        bankAccount  = bankAccount,
        workshopName = workshopName,
        specialty    = specialty,
        workersCount = workersCount,
        address      = address,
        createdAt    = createdAt
    )
}
