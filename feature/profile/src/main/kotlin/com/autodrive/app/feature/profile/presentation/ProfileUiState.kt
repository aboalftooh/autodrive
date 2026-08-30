package com.autodrive.app.feature.profile.presentation

import com.autodrive.app.core.model.account.AutoDriveUser
import com.autodrive.app.core.model.money.Money

enum class ProfileEditSection {
    ACCOUNT,
    PAYOUT,
    WORKSHOP,
    WEEKLY_TARGET
}

data class ProfileUiState(
    val user: AutoDriveUser? = null,
    val editingSection: ProfileEditSection? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val signedOut: Boolean = false,
    val weeklyTarget: Money = Money.of(500_000L),
    val successMessage: String? = null,
    val balance: Money = Money.ZERO,
    val balanceLoaded: Boolean = false,
    val showSignOutConfirmDialog: Boolean = false
)
