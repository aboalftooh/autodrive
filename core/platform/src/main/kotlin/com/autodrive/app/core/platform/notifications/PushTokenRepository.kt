package com.autodrive.app.core.platform.notifications

import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.core.session.domain.SessionReader
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Singleton
class PushTokenRepository @Inject constructor(
    private val supabase: AutoDriveSupabase,
    private val sessionReader: SessionReader,
) {

    suspend fun upsertCurrentUserToken(token: String): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Local identity checks prevent lifecycle work from crossing an account switch. The RPC
            // still derives user/client/org from auth.uid(); none of these values is sent as authority.
            val session = sessionReader.currentSession()
            val uid = supabase.client.auth.currentSessionOrNull()?.user?.id
                ?: session.userId
                ?: error("لا يوجد مستخدم مسجّل")
            require(session.userId == null || session.userId == uid) { "PUSH_TOKEN_SESSION_MISMATCH" }
            requireNotNull(session.clientId) { "client_id مفقود — أكمل التسجيل أولاً" }
            requireNotNull(session.orgId) { "org_id مفقود — أكمل التسجيل أولاً" }

            val mutationId = UUID.randomUUID().toString()
            val receipt = supabase.client.postgrest.rpc(
                "autodrive_register_push_token_command_v1",
                RegisterPushTokenParams(mutationId, token, "android"),
            ).decodeAs<PushCommandReceipt>()
            receipt.requireApplied(mutationId, "REGISTER_PUSH_TOKEN")
            Unit
        }
    }

    suspend fun deleteCurrentUserToken(): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = supabase.client.auth.currentSessionOrNull()?.user?.id
                ?: sessionReader.currentSession().userId
                ?: return@runCatching
            val session = sessionReader.currentSession()
            require(session.userId == null || session.userId == uid) { "PUSH_TOKEN_SESSION_MISMATCH" }

            val mutationId = UUID.randomUUID().toString()
            val receipt = supabase.client.postgrest.rpc(
                "autodrive_revoke_push_token_command_v1",
                RevokePushTokenParams(mutationId),
            ).decodeAs<PushCommandReceipt>()
            receipt.requireApplied(mutationId, "REVOKE_PUSH_TOKEN")
        }
    }

    private fun PushCommandReceipt.requireApplied(mutationId: String, commandType: String) {
        check(this.mutationId == mutationId) { "INVALID_SERVER_RECEIPT_MUTATION" }
        check(this.commandType == commandType) { "INVALID_SERVER_RECEIPT_COMMAND" }
        check(revisionKind == "COMMAND_RECEIPT") { "INVALID_SERVER_RECEIPT_REVISION_KIND" }
        when (resultStatus) {
            "APPLIED" -> check(serverRevision > 0L) { "INVALID_SERVER_RECEIPT_REVISION" }
            "REJECTED" -> error(errorCode ?: "SERVER_COMMAND_REJECTED")
            "CONFLICT" -> error(errorCode ?: "SERVER_COMMAND_CONFLICT")
            else -> error("UNSUPPORTED_SERVER_RECEIPT")
        }
    }
}

@Serializable
private data class RegisterPushTokenParams(
    @SerialName("p_mutation_id") val mutationId: String,
    @SerialName("p_token") val token: String,
    @SerialName("p_platform") val platform: String,
)

@Serializable
private data class RevokePushTokenParams(
    @SerialName("p_mutation_id") val mutationId: String,
)

@Serializable
private data class PushCommandReceipt(
    @SerialName("mutation_id") val mutationId: String,
    @SerialName("command_type") val commandType: String,
    @SerialName("result_status") val resultStatus: String,
    @SerialName("server_revision") val serverRevision: Long,
    @SerialName("revision_kind") val revisionKind: String,
    val replayed: Boolean = false,
    @SerialName("error_code") val errorCode: String? = null,
)
