package com.autodrive.app.feature.balance.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.autodrive.app.core.common.format.FormatUtils
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveSecondaryButton
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveBottomSheet
import com.autodrive.app.core.designsystem.components.inputs.AutoDriveNumericField
import com.autodrive.app.core.designsystem.components.inputs.AutoDriveTextField
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.model.money.Money
import com.autodrive.app.feature.balance.domain.model.WithdrawalStatus

@Composable
internal fun WithdrawalSheet(
    state: BalanceUiState,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val reservedSum = state.withdrawalRequests
        .filter { it.status == WithdrawalStatus.PENDING || it.status == WithdrawalStatus.APPROVED }
        .let { Money.sum(it.map { request -> request.amount }) }
    val rawAvailable = (state.balance?.balance ?: Money.ZERO) - reservedSum
    val maxBalance = if (rawAvailable.isNegative()) Money.ZERO else rawAvailable
    val noteLimit = 200

    AutoDriveBottomSheet(
        onDismissRequest = onDismiss,
        title = "طلب سحب رصيد",
    ) {
        Text(
            "الرصيد المتاح: ${FormatUtils.formatSar(maxBalance)}",
            style = MaterialTheme.typography.bodyMedium,
            color = AutoDriveText.Secondary,
        )
        if (reservedSum.isPositive()) {
            Text(
                "محجوز بطلبات سحب سابقة: ${FormatUtils.formatSar(reservedSum)}",
                style = MaterialTheme.typography.bodySmall,
                color = AutoDriveText.Secondary,
            )
        }
        AutoDriveNumericField(
            value = state.withdrawalAmount,
            onValueChange = onAmountChange,
            label = "المبلغ المطلوب سحبه",
            placeholder = "0.00",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        AutoDriveTextField(
            value = state.withdrawalNote,
            onValueChange = { if (it.length <= noteLimit) onNoteChange(it) },
            label = "ملاحظة (اختياري)",
            supportingText = "${state.withdrawalNote.length} / $noteLimit",
            errorText = state.submitError,
            singleLine = false,
            maxLines = 3,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = AutoDriveSpace.LG),
            horizontalArrangement = Arrangement.spacedBy(AutoDriveSpace.SM),
        ) {
            AutoDriveSecondaryButton("إلغاء", onDismiss, Modifier.weight(1f), enabled = !state.isSubmitting)
            AutoDrivePrimaryButton(
                text = "موافق",
                onClick = onSubmit,
                modifier = Modifier.weight(1f),
                enabled = state.withdrawalAmount.isNotBlank(),
                loading = state.isSubmitting,
            )
        }
    }
}
