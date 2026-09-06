package com.autodrive.app

import androidx.compose.runtime.Composable
import com.autodrive.app.core.designsystem.components.actions.AutoDrivePrimaryButton
import com.autodrive.app.core.designsystem.components.actions.AutoDriveTextButton
import com.autodrive.app.core.designsystem.components.feedback.AutoDriveDialog

@Composable
fun PermissionsDeniedDialog(onContinue: () -> Unit, onGrant: () -> Unit) {
    AutoDriveDialog(
        title = "التطبيق لا يعمل بكامل الميزات",
        body = "بعض الأذونات غير ممنوحة، مما قد يحدّ من وظائف التطبيق.",
        onDismissRequest = onContinue,
        actions = {
            AutoDriveTextButton("استمر", onContinue)
            AutoDrivePrimaryButton("امنح الأذونات", onGrant)
        },
    )
}
