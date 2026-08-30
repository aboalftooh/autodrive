package com.autodrive.app.core.designsystem.patterns.header

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.autodrive.app.core.designsystem.components.navigation.AutoDriveBackHeader
import com.autodrive.app.core.designsystem.components.navigation.AutoDriveTopHeader
import com.autodrive.app.core.designsystem.foundation.spacing.AutoDriveSpace
import com.autodrive.app.core.designsystem.theme.AutoDriveTheme

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    context: (@Composable () -> Unit)? = null,
    titleContent: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        if (onBack == null) {
            AutoDriveTopHeader(
                title = title,
                subtitle = subtitle,
                actions = if (trailing != null) ({ trailing() }) else null,
                titleContent = titleContent,
            )
        }
        else AutoDriveBackHeader(title = title, onBack = onBack, trailingAction = trailing)
        if (context != null) {
            Column(Modifier.padding(horizontal = AutoDriveSpace.LG, vertical = AutoDriveSpace.SM)) { context() }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090C)
@Composable
private fun ScreenHeaderPreview() = AutoDriveTheme { ScreenHeader("المحادثات", subtitle = "آخر النشاط") }
