package com.autodrive.app.core.sync.outbox

import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.core.network.dto.AutoDriveUserUpdateDto
import com.autodrive.app.core.network.dto.RequestWithdrawalParams
import com.autodrive.app.core.network.serialization.BigDecimalSerializer
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object ServerCommandType {
    const val UPDATE_PROFILE = "UPDATE_PROFILE"
    const val REQUEST_WITHDRAWAL = "REQUEST_WITHDRAWAL"
    const val SEND_CHAT_MESSAGE = "SEND_CHAT_MESSAGE"
    const val CREATE_CHAT_CONVERSATION = "CREATE_CHAT_CONVERSATION"
    const val MARK_CHAT_READ = "MARK_CHAT_READ"
    const val MARK_NOTIFICATION_READ = "MARK_NOTIFICATION_READ"
}

object ServerCommandResultStatus {
    const val APPLIED = "APPLIED"
    const val REJECTED = "REJECTED"
    const val CONFLICT = "CONFLICT"
}

const val COMMAND_RECEIPT_REVISION_KIND = "COMMAND_RECEIPT"

@Serializable
internal data class ServerCommandReceiptDto(
    @SerialName("mutation_id") val mutationId: String,
    @SerialName("command_type") val commandType: String,
    @SerialName("result_status") val resultStatus: String,
    @SerialName("server_entity_id") val serverEntityId: String? = null,
    @SerialName("server_revision") val serverRevision: Long,
    @SerialName("revision_kind") val revisionKind: String,
    val replayed: Boolean = false,
    @SerialName("error_code") val errorCode: String? = null,
    @SerialName("server_created_at_ms") val serverCreatedAt: Long? = null,
    @SerialName("result_count") val resultCount: Int? = null,
)

@Serializable
private data class UpdateProfileCommandParams(
    @SerialName("p_mutation_id") val mutationId: String,
    @SerialName("p_full_name") val fullName: String? = null,
    @SerialName("p_phone") val phone: String? = null,
    @SerialName("p_bank_name") val bankName: String? = null,
    @SerialName("p_bank_account") val bankAccount: String? = null,
    @SerialName("p_workshop_name") val workshopName: String? = null,
    @SerialName("p_specialty") val specialty: String? = null,
    @SerialName("p_workers_count") val workersCount: Int? = null,
    @SerialName("p_address") val address: String? = null,
)

@Serializable
private data class WithdrawalCommandParams(
    @SerialName("p_mutation_id") val mutationId: String,
    @SerialName("p_amount")
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    @SerialName("p_note") val note: String,
)

@Serializable
private data class ChatSendCommandParams(
    @SerialName("p_mutation_id") val mutationId: String,
    @SerialName("p_message_id") val messageId: String,
    @SerialName("p_conversation_id") val conversationId: String,
    @SerialName("p_type") val type: String,
    @SerialName("p_body") val body: String,
    @SerialName("p_media_url") val mediaUrl: String? = null,
    @SerialName("p_media_mime") val mediaMime: String? = null,
    @SerialName("p_media_duration_ms") val mediaDurationMs: Long? = null,
)

@Serializable
private data class ChatSendCommandV2Params(
    @SerialName("p_mutation_id") val mutationId: String,
    @SerialName("p_message_id") val messageId: String,
    @SerialName("p_conversation_id") val conversationId: String,
    @SerialName("p_type") val type: String,
    @SerialName("p_body") val body: String,
    @SerialName("p_media_url") val mediaUrl: String? = null,
    @SerialName("p_media_mime") val mediaMime: String? = null,
    @SerialName("p_media_duration_ms") val mediaDurationMs: Long? = null,
    @SerialName("p_media_object_path") val mediaObjectPath: String,
)


@Serializable
private data class CreateChatConversationCommandParams(
    @SerialName("p_mutation_id") val mutationId: String,
    @SerialName("p_local_conversation_id") val localConversationId: String,
    @SerialName("p_subject") val subject: String,
)

@Serializable
private data class ChatReadCommandParams(
    @SerialName("p_mutation_id") val mutationId: String,
    @SerialName("p_conversation_id") val conversationId: String,
)

@Serializable
private data class NotificationReadCommandParams(
    @SerialName("p_mutation_id") val mutationId: String,
    @SerialName("p_notification_id") val notificationId: String,
)

/**
 * Delivers supported durable Outbox mutations through server idempotent-command RPCs.
 *
 * Callers must preserve the stable `mutationId` when retrying an ambiguous outcome. Transport,
 * timeout, IO, and PostgREST ambiguity is surfaced as [AmbiguousCommandOutcomeException] rather
 * than inferred success/failure, allowing the Outbox to replay the same mutation identity. Server
 * receipts are the command boundary; canonical shared state still converges through synchronization.
 */
@Singleton
class IdempotentServerCommandGateway @Inject constructor(
    private val supabase: AutoDriveSupabase,
) {
    suspend fun updateProfile(mutationId: String, dto: AutoDriveUserUpdateDto): OutboxDeliveryReceipt = execute(
        rpcName = "autodrive_update_profile_command_v1",
        params = UpdateProfileCommandParams(
            mutationId = mutationId,
            fullName = dto.fullName,
            phone = dto.phone,
            bankName = dto.bankName,
            bankAccount = dto.bankAccount,
            workshopName = dto.workshopName,
            specialty = dto.specialty,
            workersCount = dto.workersCount,
            address = dto.address,
        ),
    )

    suspend fun requestWithdrawal(mutationId: String, params: RequestWithdrawalParams): OutboxDeliveryReceipt = execute(
        rpcName = "autodrive_request_withdrawal_command_v1",
        params = WithdrawalCommandParams(mutationId, params.amount, params.note),
    )

    suspend fun sendChatMessage(mutationId: String, payload: ChatSendOutboxPayload): OutboxDeliveryReceipt =
        if (payload.mediaObjectPath != null) {
            execute(
                rpcName = "autodrive_send_chat_message_command_v2",
                params = ChatSendCommandV2Params(
                    mutationId = mutationId,
                    messageId = payload.id,
                    conversationId = payload.conversationId,
                    type = payload.type,
                    body = payload.body,
                    mediaUrl = payload.mediaUrl,
                    mediaMime = payload.mediaMime,
                    mediaDurationMs = payload.mediaDurationMs,
                    mediaObjectPath = payload.mediaObjectPath,
                ),
            )
        } else {
            execute(
                rpcName = "autodrive_send_chat_message_command_v1",
                params = ChatSendCommandParams(
                    mutationId = mutationId,
                    messageId = payload.id,
                    conversationId = payload.conversationId,
                    type = payload.type,
                    body = payload.body,
                    mediaUrl = payload.mediaUrl,
                    mediaMime = payload.mediaMime,
                    mediaDurationMs = payload.mediaDurationMs,
                ),
            )
        }


    suspend fun createChatConversation(
        mutationId: String,
        payload: CreateChatConversationOutboxPayload,
    ): OutboxDeliveryReceipt = execute(
        rpcName = "autodrive_create_chat_conversation_command_v1",
        params = CreateChatConversationCommandParams(
            mutationId = mutationId,
            localConversationId = payload.localConversationId,
            subject = payload.subject,
        ),
    )

    suspend fun markChatRead(mutationId: String, conversationId: String): OutboxDeliveryReceipt = execute(
        rpcName = "autodrive_mark_chat_read_command_v1",
        params = ChatReadCommandParams(mutationId, conversationId),
    )

    suspend fun markNotificationRead(mutationId: String, notificationId: String): OutboxDeliveryReceipt = execute(
        rpcName = "autodrive_mark_notification_read_command_v1",
        params = NotificationReadCommandParams(mutationId, notificationId),
    )

    private suspend inline fun <reified T : Any> execute(
        rpcName: String,
        params: T,
    ): OutboxDeliveryReceipt {
        val dto = try {
            supabase.client.postgrest.rpc(rpcName, params).decodeAs<ServerCommandReceiptDto>()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (timeout: HttpRequestTimeoutException) {
            throw AmbiguousCommandOutcomeException("TRANSPORT_TIMEOUT_AMBIGUOUS", timeout)
        } catch (http: HttpRequestException) {
            throw AmbiguousCommandOutcomeException("HTTP_TRANSPORT_AMBIGUOUS", http)
        } catch (io: IOException) {
            throw AmbiguousCommandOutcomeException("TRANSPORT_IO_AMBIGUOUS", io)
        } catch (rest: RestException) {
            // supabase-kt RestException in this baseline does not expose the HTTP status at this
            // call site. It is therefore safer to preserve/replay the mutation than infer from text.
            throw AmbiguousCommandOutcomeException("POSTGREST_AMBIGUOUS", rest)
        }
        return OutboxDeliveryReceipt(
            mutationId = dto.mutationId,
            commandType = dto.commandType,
            resultStatus = dto.resultStatus,
            serverEntityId = dto.serverEntityId,
            serverRevision = dto.serverRevision,
            revisionKind = dto.revisionKind,
            replayed = dto.replayed,
            errorCode = dto.errorCode,
            serverCreatedAt = dto.serverCreatedAt,
            resultCount = dto.resultCount,
        )
    }
}
