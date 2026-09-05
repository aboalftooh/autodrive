package com.autodrive.app.core.designsystem.components.inputs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.autodrive.app.core.designsystem.foundation.border.AutoDriveBorder
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBrand
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveStatus
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.foundation.icon.AutoDriveIconSize
import com.autodrive.app.core.designsystem.foundation.radius.AutoDriveRadius
import com.autodrive.app.core.designsystem.theme.AutoDriveTheme

enum class AutoDriveTextFieldLayout { Standard, CompactMultiline }

@Composable
fun AutoDriveTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    supportingText: String? = null,
    errorText: String? = null,
    isError: Boolean = errorText != null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 5,
    leadingIcon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    layout: AutoDriveTextFieldLayout = AutoDriveTextFieldLayout.Standard,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let { text -> ({ Text(text, style = MaterialTheme.typography.bodySmall) }) },
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        supportingText = when {
            errorText != null -> ({ Text(errorText) })
            supportingText != null -> ({ Text(supportingText) })
            else -> null
        },
        isError = isError,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        maxLines = maxLines,
        leadingIcon = leadingIcon?.let { icon -> ({ Icon(icon, null, Modifier.size(AutoDriveIconSize.SM)) }) },
        trailingIcon = trailingContent,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = AutoDriveRadius.MediumShape,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AutoDriveBrand.Primary,
            unfocusedBorderColor = AutoDriveBorderColor.Default,
            errorBorderColor = AutoDriveStatus.Error.copy(alpha = 0.72f),
            focusedTextColor = AutoDriveText.Primary,
            unfocusedTextColor = AutoDriveText.Primary,
            disabledTextColor = AutoDriveText.Disabled,
            cursorColor = AutoDriveBrand.Primary,
            errorCursorColor = AutoDriveStatus.Error,
            focusedContainerColor = AutoDriveSurface.Raised,
            unfocusedContainerColor = AutoDriveSurface.Raised,
            disabledContainerColor = AutoDriveSurface.Raised,
            focusedLabelColor = AutoDriveText.Secondary,
            unfocusedLabelColor = AutoDriveText.Secondary,
            focusedPlaceholderColor = AutoDriveText.Disabled,
            unfocusedPlaceholderColor = AutoDriveText.Disabled,
            focusedSupportingTextColor = AutoDriveText.Secondary,
            unfocusedSupportingTextColor = AutoDriveText.Secondary,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = when {
                singleLine -> 56.dp
                layout == AutoDriveTextFieldLayout.CompactMultiline -> 56.dp
                else -> 112.dp
            }),
    )
}

@Composable
fun AutoDriveSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    searching: Boolean = false,
    onClear: () -> Unit = { onValueChange("") },
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = { Icon(Icons.Rounded.Search, null, Modifier.size(AutoDriveIconSize.MD)) },
        trailingIcon = {
            when {
                searching -> CircularProgressIndicator(
                    modifier = Modifier.size(AutoDriveIconSize.SM),
                    strokeWidth = AutoDriveBorder.Strong,
                    color = AutoDriveBrand.Primary,
                )
                value.isNotBlank() -> IconButton(onClick = onClear, modifier = Modifier.size(AutoDriveIconSize.TouchTarget)) {
                    Icon(Icons.Rounded.Close, contentDescription = "مسح", modifier = Modifier.size(AutoDriveIconSize.SM))
                }
            }
        },
        shape = AutoDriveRadius.MediumShape,
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AutoDriveBrand.Primary,
            unfocusedBorderColor = AutoDriveBorderColor.Default,
            focusedContainerColor = AutoDriveSurface.Raised,
            unfocusedContainerColor = AutoDriveSurface.Raised,
            focusedTextColor = AutoDriveText.Primary,
            unfocusedTextColor = AutoDriveText.Primary,
            disabledTextColor = AutoDriveText.Disabled,
            cursorColor = AutoDriveBrand.Primary,
            focusedLeadingIconColor = AutoDriveText.Secondary,
            unfocusedLeadingIconColor = AutoDriveText.Secondary,
            focusedTrailingIconColor = AutoDriveText.Secondary,
            unfocusedTrailingIconColor = AutoDriveText.Secondary,
            focusedPlaceholderColor = AutoDriveText.Disabled,
            unfocusedPlaceholderColor = AutoDriveText.Disabled,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun AutoDriveNumericField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    supportingText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        AutoDriveTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = modifier,
            placeholder = placeholder,
            supportingText = supportingText,
            errorText = errorText,
            enabled = enabled,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
        )
    }
}

data class AutoDriveSelectionOption(val id: String, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoDriveSelectionField(
    selected: AutoDriveSelectionOption?,
    options: List<AutoDriveSelectionOption>,
    label: String,
    onSelected: (AutoDriveSelectionOption) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "",
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected?.label.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = AutoDriveRadius.MediumShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AutoDriveBrand.Primary,
                unfocusedBorderColor = AutoDriveBorderColor.Default,
                focusedContainerColor = AutoDriveSurface.Raised,
                unfocusedContainerColor = AutoDriveSurface.Raised,
                focusedTextColor = AutoDriveText.Primary,
                unfocusedTextColor = AutoDriveText.Primary,
                focusedLabelColor = AutoDriveText.Secondary,
                unfocusedLabelColor = AutoDriveText.Secondary,
            ),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = AutoDriveSurface.Raised) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option.label,
                            color = if (option.id == selected?.id) AutoDriveBrand.Active else AutoDriveText.Primary,
                        )
                    },
                    onClick = { onSelected(option); expanded = false },
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090C)
@Composable
private fun InputComponentsPreview() = AutoDriveTheme {
    AutoDriveTextField(value = "ورشة النيل", onValueChange = {}, label = "اسم الورشة")
}
