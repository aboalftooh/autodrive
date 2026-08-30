package com.autodrive.app.core.network.dto

import com.autodrive.app.core.network.serialization.BigDecimalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class AutoDriveUserDto(
    val id: String = "",
    @SerialName("user_id")       val userId: String = "",
    @SerialName("client_id")     val clientId: String = "",
    @SerialName("org_id")        val orgId: String = "",
    @SerialName("account_type")  val accountType: String = "",
    @SerialName("full_name")     val fullName: String = "",
    val phone: String = "",
    @SerialName("bank_name")     val bankName: String? = null,
    @SerialName("bank_account")  val bankAccount: String? = null,
    @SerialName("workshop_name") val workshopName: String? = null,
    val specialty: String? = null,
    @SerialName("workers_count") val workersCount: Int? = null,
    val address: String? = null,
    @SerialName("onboarding_completed") val onboardingCompleted: Boolean = true,
    @SerialName("created_at")    val createdAt: String = "",
    @SerialName("updated_at")    val updatedAt: String = ""
)

@Serializable
data class InviteCodeDto(
    val id: Int = 0,
    val code: String = "",
    @SerialName("marketer_client_id") val clientId: String = "",
    @SerialName("organization_id")    val orgId: String = "",
    @SerialName("expires_at")         val expiresAt: String? = null,
    val used: Boolean = false,
    @SerialName("used_at")            val usedAt: String? = null,
    @SerialName("created_at")         val createdAt: String = ""
)

@Serializable
data class InvoiceDto(
    val id: String = "",
    @SerialName("client_id")      val clientId: String = "",
    @Serializable(with = BigDecimalSerializer::class)
    val commission: BigDecimal = BigDecimal.ZERO,
    val status: String = "",
    val category: String = "",
    @SerialName("total_amount")
    @Serializable(with = BigDecimalSerializer::class)
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    @SerialName("invoice_number") val invoiceNumber: Int = 0,
    @SerialName("created_at")     val createdAt: String = ""
)

@Serializable
data class InvoiceItemDto(
    val id: String = "",
    @SerialName("invoice_id")   val invoiceId: String = "",
    @SerialName("item_name")    val itemName: String = "",
    @SerialName("item_type")    val itemType: String = "",
    val description: String? = null,
    val quantity: Int = 1,
    @SerialName("buy_price")
    @Serializable(with = BigDecimalSerializer::class)
    val buyPrice: BigDecimal = BigDecimal.ZERO,
    @SerialName("sell_price")
    @Serializable(with = BigDecimalSerializer::class)
    val sellPrice: BigDecimal = BigDecimal.ZERO,
    @SerialName("total_price")
    @Serializable(with = BigDecimalSerializer::class)
    val totalPrice: BigDecimal = BigDecimal.ZERO
)

@Serializable
data class PaymentDto(
    val id: String = "",
    @SerialName("client_id") val clientId: String = "",
    @SerialName("invoice_id") val invoiceId: String = "",
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal = BigDecimal.ZERO,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class CommissionPaymentDto(
    val id: String = "",
    @SerialName("client_id")   val clientId: String = "",
    // The production table uses total_amount / paid_at / transaction_ref.
    // Keep the domain names stable while decoding the actual server contract.
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("total_amount")
    val amount: BigDecimal = BigDecimal.ZERO,
    @SerialName("transaction_ref")
    val note: String? = null,
    @SerialName("invoice_ids") val invoiceIds: String = "",
    @SerialName("paid_at")     val createdAt: String = ""
)

@Serializable
data class AutoDriveUserInsertDto(
    @SerialName("user_id")       val userId: String,
    @SerialName("client_id")     val clientId: String,
    @SerialName("org_id")        val orgId: String,
    @SerialName("account_type")  val accountType: String,
    @SerialName("full_name")     val fullName: String,
    val phone: String,
    @SerialName("bank_name")     val bankName: String? = null,
    @SerialName("bank_account")  val bankAccount: String? = null,
    @SerialName("workshop_name") val workshopName: String? = null,
    val specialty: String? = null,
    @SerialName("workers_count") val workersCount: Int? = null,
    val address: String? = null
)

@Serializable
data class NotificationDto(
    val id: String = "",
    @SerialName("user_id")    val userId: String = "",
    @SerialName("client_id")  val clientId: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    @SerialName("is_read")    val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    // بند 9: حمولة إضافية؛ ADMIN_REMINDER يضع فيها nav_route
    val data: kotlinx.serialization.json.JsonObject? = null
)

// ── جدول: marketer_balance ──────────────────────────────────
// لا يحوي user_id في DB — يُفلتر بـ client_id
@Serializable
data class MarketerBalanceDto(
    val id: String = "",
    @SerialName("client_id")  val clientId: String = "",
    @SerialName("org_id")     val orgId: String = "",
    @Serializable(with = BigDecimalSerializer::class)
    val balance: BigDecimal = BigDecimal.ZERO,
    @SerialName("updated_at") val updatedAt: String = ""
)

// ── جدول: balance_transactions ──────────────────────────────
// لا يحوي user_id في DB — يُفلتر بـ client_id
@Serializable
data class BalanceTransactionDto(
    val id: String = "",
    @SerialName("client_id")      val clientId: String = "",
    @SerialName("org_id")         val orgId: String = "",
    val type: String = "",
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal = BigDecimal.ZERO,
    @SerialName("balance_before")
    @Serializable(with = BigDecimalSerializer::class)
    val balanceBefore: BigDecimal = BigDecimal.ZERO,
    @SerialName("balance_after")
    @Serializable(with = BigDecimalSerializer::class)
    val balanceAfter: BigDecimal = BigDecimal.ZERO,
    @SerialName("reference_type") val referenceType: String = "",
    @SerialName("reference_id")   val referenceId: String? = null,
    val note: String? = null,
    @SerialName("created_at")     val createdAt: String = ""
)

// ── جدول: withdrawal_requests ───────────────────────────────
// لا يحوي user_id في DB — يُفلتر بـ client_id
@Serializable
data class WithdrawalRequestDto(
    val id: String = "",
    @SerialName("client_id")       val clientId: String = "",
    @SerialName("org_id")          val orgId: String = "",
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal = BigDecimal.ZERO,
    val status: String = "",
    @SerialName("bank_name")       val bankName: String = "",
    @SerialName("bank_account")    val bankAccount: String = "",
    @SerialName("transaction_ref") val transactionRef: String? = null,
    val note: String? = null,
    @SerialName("admin_note")      val adminNote: String? = null,
    @SerialName("requested_at")    val requestedAt: String = "",
    @SerialName("processed_at")    val processedAt: String? = null,
    @SerialName("processed_by")    val processedBy: String? = null,
    @SerialName("client_request_id") val clientRequestId: String? = null,
)

@Serializable
data class MarkInviteUsedDto(
    val used: Boolean = true,
    @SerialName("used_at") val usedAt: String
)

// ── RPC: verify_invite_code_v2 ─────────────────────────────
@Serializable
data class VerifyCodeRpcParams(
    @SerialName("p_code") val code: String
)

@Serializable
data class VerifyCodeRpcResult(
    @SerialName("is_valid")    val isValid: Boolean,
    val reason: String,
    @SerialName("client_id")   val clientId: String?,
    @SerialName("org_id")      val orgId: String?,
    @SerialName("is_marketer") val isMarketer: Boolean,
)

// ── RPC: redeem_invite_code ────────────────────────────────
@Serializable
data class RedeemInviteCodeParams(
    @SerialName("p_code")          val code: String,
    @SerialName("p_full_name")     val fullName: String,
    @SerialName("p_phone")         val phone: String,
    @SerialName("p_account_type")  val accountType: String,
    @SerialName("p_bank_name")     val bankName: String?    = null,
    @SerialName("p_bank_account")  val bankAccount: String? = null,
    @SerialName("p_workshop_name") val workshopName: String? = null,
    @SerialName("p_specialty")     val specialty: String?   = null,
    @SerialName("p_workers_count") val workersCount: Int?   = null,
    @SerialName("p_address")       val address: String?     = null,
)

// ── RPC: link_phone_user (FIX-022) ────────────────────────
@Serializable
data class LinkPhoneUserParams(
    @SerialName("p_invite_code") val inviteCode: String
)

// ── RPC: request_withdrawal (FIX-008) ─────────────────────
@Serializable
data class RequestWithdrawalParams(
    @SerialName("p_amount")
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    @SerialName("p_note")              val note: String,
    @SerialName("p_client_request_id") val clientRequestId: String
)

// ── View: commission_eligibility (FIX-7) ──────────────────
@Serializable
data class EligibilityDto(
    @SerialName("invoice_id")      val invoiceId: String = "",
    @SerialName("client_id")       val clientId: String = "",
    @SerialName("org_id")          val orgId: String = "",
    @Serializable(with = BigDecimalSerializer::class)
    val commission: BigDecimal = BigDecimal.ZERO,
    @SerialName("invoice_number")  val invoiceNumber: Int = 0,
    @SerialName("total_amount")
    @Serializable(with = BigDecimalSerializer::class)
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    @SerialName("invoice_status")  val invoiceStatus: String = "",
    val category: String = "",
    @SerialName("created_at")      val createdAt: String = "",
    @SerialName("ledger_status")   val ledgerStatus: String? = null,
    @SerialName("paid_out_amount")
    @Serializable(with = BigDecimalSerializer::class)
    val paidOutAmount: BigDecimal? = null,
    @SerialName("payments_sum")
    @Serializable(with = BigDecimalSerializer::class)
    val paymentsSum: BigDecimal = BigDecimal.ZERO,
    @SerialName("week_start")      val weekStart: String = "",
    val eligibility: String = ""
)

// ── Edge Function: send-phone-otp / verify-phone-otp ──────
@Serializable
data class SendPhoneOtpRequest(
    val phone: String
)

@Serializable
data class SendPhoneOtpResponse(
    val success: Boolean = false,
    @SerialName("dev_otp") val devOtp: String? = null
)

@Serializable
data class VerifyPhoneOtpRequest(
    val phone: String,
    val otp: String
)

@Serializable
data class VerifyPhoneOtpResponse(
    @SerialName("access_token")  val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in")    val expiresIn: Long = 3600L,
    @SerialName("token_type")    val tokenType: String = "bearer",
    @SerialName("user_id")       val userId: String = ""
)

// ── Update DTO: تعديل الملف الشخصي ────────────────────────
@Serializable
data class AutoDriveUserUpdateDto(
    @SerialName("full_name")     val fullName: String? = null,
    val phone: String? = null,
    @SerialName("bank_name")     val bankName: String? = null,
    @SerialName("bank_account")  val bankAccount: String? = null,
    @SerialName("workshop_name") val workshopName: String? = null,
    val specialty: String? = null,
    @SerialName("workers_count") val workersCount: Int? = null,
    val address: String? = null
)
