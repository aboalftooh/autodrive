package com.autodrive.app.core.designsystem.components.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Message
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.autodrive.app.core.designsystem.components.AutoDriveAccent
import com.autodrive.app.core.designsystem.components.AutoDriveStatusTone
import com.autodrive.app.core.designsystem.components.actions.*
import com.autodrive.app.core.designsystem.components.containers.*
import com.autodrive.app.core.designsystem.components.data.*
import com.autodrive.app.core.designsystem.components.feedback.*
import com.autodrive.app.core.designsystem.components.inputs.*
import com.autodrive.app.core.designsystem.components.navigation.*
import com.autodrive.app.core.designsystem.theme.AutoDriveTheme

@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun PrimaryButtonPreview() = AutoDriveTheme { AutoDrivePrimaryButton("حفظ", {}) }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun SecondaryButtonPreview() = AutoDriveTheme { AutoDriveSecondaryButton("إلغاء", {}) }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun TextButtonPreview() = AutoDriveTheme { AutoDriveTextButton("المزيد", {}) }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun IconButtonPreview() = AutoDriveTheme { AutoDriveIconButton(Icons.Rounded.Info, "معلومات", {}) }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun FabPreview() = AutoDriveTheme { AutoDriveFab({}, contentDescription = "إضافة") }

@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun TextFieldPreview() = AutoDriveTheme { AutoDriveTextField("محمد", {}, "الاسم") }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun SearchFieldPreview() = AutoDriveTheme { AutoDriveSearchField("ورشة", {}, "ابحث") }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun NumericFieldPreview() = AutoDriveTheme { AutoDriveNumericField("250000", {}, "المبلغ") }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun SelectionFieldPreview() = AutoDriveTheme {
    val option = AutoDriveSelectionOption("general", "عام")
    AutoDriveSelectionField(option, listOf(option, AutoDriveSelectionOption("electric", "كهرباء")), "النوع", {})
}

@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun CardPreview() = AutoDriveTheme { AutoDriveCard { androidx.compose.material3.Text("بطاقة") } }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun MetricCardPreview() = AutoDriveTheme { AutoDriveMetricCard("الطلبات", "24") }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun HighlightCardPreview() = AutoDriveTheme { AutoDriveHighlightCard(AutoDriveAccent.Primary) { androidx.compose.material3.Text("أبرز نتيجة") } }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun AlertCardPreview() = AutoDriveTheme { AutoDriveAlertCard("تنبيه", "معلومة مهمة", AutoDriveStatusTone.Warning) }

@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun BottomNavigationPreview() = AutoDriveTheme {
    AutoDriveBottomNavigation(
        listOf(AutoDriveNavigationItem("home", "الرئيسية", Icons.Rounded.Home), AutoDriveNavigationItem("messages", "الرسائل", Icons.Rounded.Message, 2)),
        "home",
        {},
    )
}
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun TopHeaderPreview() = AutoDriveTheme { AutoDriveTopHeader("الرئيسية", subtitle = "مرحبًا") }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun BackHeaderPreview() = AutoDriveTheme { AutoDriveBackHeader("التفاصيل", {}) }

@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun BadgePreview() = AutoDriveTheme { AutoDriveBadge(count = 12) }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun StatusChipPreview() = AutoDriveTheme { AutoDriveStatusChip("مكتمل", AutoDriveStatusTone.Success) }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun SnackbarPreview() = AutoDriveTheme { AutoDriveSnackbarContent("تم الحفظ") }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun DialogPreview() = AutoDriveTheme { AutoDriveDialog("تأكيد", {}, body = "هل تريد المتابعة؟") }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun BottomSheetPreview() = AutoDriveTheme { AutoDriveBottomSheet({}, "خيارات") { androidx.compose.material3.Text("محتوى") } }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun LoadingStatePreview() = AutoDriveTheme { AutoDriveLoadingState(label = "جارٍ التحميل") }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun EmptyStatePreview() = AutoDriveTheme { AutoDriveEmptyState("لا توجد بيانات", "ستظهر هنا لاحقًا") }

@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun AvatarPreview() = AutoDriveTheme { AutoDriveAvatar("محمد") }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun ListRowPreview() = AutoDriveTheme { AutoDriveListRow("محمد", supportingText = "آخر ظهور اليوم") }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun SectionHeaderPreview() = AutoDriveTheme { AutoDriveSectionHeader("الحساب") }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun DividerPreview() = AutoDriveTheme { AutoDriveDivider() }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun StatValuePreview() = AutoDriveTheme { AutoDriveStatValue("1,240,000", size = AutoDriveStatSize.Large) }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun StatusIndicatorPreview() = AutoDriveTheme { AutoDriveStatusIndicator(AutoDriveStatusTone.Info, "معلومة") }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun StepIndicatorPreview() = AutoDriveTheme { AutoDriveStepIndicator(1, 3) }
@Preview(locale = "ar", showBackground = true, backgroundColor = 0xFF08090C) @Composable private fun InstrumentNumberPreview() = AutoDriveTheme { AutoDriveInstrumentNumber("128.4", AutoDriveInstrumentTone.Active) }
