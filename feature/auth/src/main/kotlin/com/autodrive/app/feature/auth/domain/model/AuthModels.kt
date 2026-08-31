package com.autodrive.app.feature.auth.domain.model

sealed class PhoneEntryResult {
    data object LoginOtp : PhoneEntryResult()
    data object NewRequest : PhoneEntryResult()
    data class WaitApproval(val requestId: String) : PhoneEntryResult()
    data class ApprovedOtp(val requestId: String) : PhoneEntryResult()
    data object AccountSelectionRequired : PhoneEntryResult()
    data class Error(val message: String) : PhoneEntryResult()
}

data class JoinRequestStatus(
    val requestId: String,
    val status: String,
    val accountType: String? = null,
    val organizationName: String? = null,
    val rejectionReason: String? = null,
) {
    val isPending: Boolean get() = status == "PENDING"
    val isApproved: Boolean get() = status == "APPROVED" || status == "OTP_PENDING"
    val isRejected: Boolean get() = status == "REJECTED" || status == "EXPIRED" || status == "CANCELLED"
}
