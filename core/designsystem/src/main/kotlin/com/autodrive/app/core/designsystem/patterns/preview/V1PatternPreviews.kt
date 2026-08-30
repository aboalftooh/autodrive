package com.autodrive.app.core.designsystem.patterns.preview

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.autodrive.app.core.designsystem.components.AutoDriveAccent
import com.autodrive.app.core.designsystem.components.AutoDriveStatusTone
import com.autodrive.app.core.designsystem.components.data.AutoDriveStatSize
import com.autodrive.app.core.designsystem.components.data.AutoDriveStatValue
import com.autodrive.app.core.designsystem.patterns.conversation.ConversationItem
import com.autodrive.app.core.designsystem.patterns.dashboard.DashboardHero
import com.autodrive.app.core.designsystem.patterns.finance.PendingRequestCard
import com.autodrive.app.core.designsystem.patterns.finance.TransactionRow
import com.autodrive.app.core.designsystem.patterns.header.ScreenHeader
import com.autodrive.app.core.designsystem.patterns.media.MediaActionGroup
import com.autodrive.app.core.designsystem.patterns.media.MediaActionState
import com.autodrive.app.core.designsystem.patterns.metrics.MetricSummary
import com.autodrive.app.core.designsystem.patterns.metrics.MetricSummaryItem
import com.autodrive.app.core.designsystem.patterns.reports.ReportStatTile
import com.autodrive.app.core.designsystem.patterns.search.SearchResultsList
import com.autodrive.app.core.designsystem.patterns.search.SearchResultsState
import com.autodrive.app.core.designsystem.patterns.settings.SettingsGroup
import com.autodrive.app.core.designsystem.patterns.settings.SettingsGroupItem
import com.autodrive.app.core.designsystem.patterns.settings.SettingsRow
import com.autodrive.app.core.designsystem.patterns.settings.SettingsRowVariant
import com.autodrive.app.core.designsystem.patterns.state.EmptyScreen
import com.autodrive.app.core.designsystem.patterns.state.ErrorScreen
import com.autodrive.app.core.designsystem.patterns.state.LoadingScreen
import com.autodrive.app.core.designsystem.theme.AutoDriveTheme

private const val DARK = 0xFF08090C

@Preview(name = "Screen Header RTL", locale = "ar", showBackground = true, backgroundColor = DARK)
@Composable private fun ScreenHeaderV1Preview() = AutoDriveTheme { ScreenHeader("المحادثات", subtitle = "آخر النشاط") }

@Preview(name = "Dashboard Hero RTL", locale = "ar", showBackground = true, backgroundColor = DARK)
@Composable private fun DashboardHeroV1Preview() = AutoDriveTheme {
    DashboardHero(label = "الرصيد القابل للسحب", heroContent = { AutoDriveStatValue("1,240,000", size = AutoDriveStatSize.Hero, accent = AutoDriveAccent.Active) })
}

@Preview(name = "Metric Summary RTL", locale = "ar", showBackground = true, backgroundColor = DARK)
@Composable private fun MetricSummaryV1Preview() = AutoDriveTheme {
    MetricSummary(listOf(MetricSummaryItem("all", "الطلبات", "24"), MetricSummaryItem("done", "المكتمل", "18", accent = AutoDriveAccent.Active)))
}

@Preview(name = "Conversation Item RTL", locale = "ar", showBackground = true, backgroundColor = DARK)
@Composable private fun ConversationItemV1Preview() = AutoDriveTheme { ConversationItem("ورشة النيل", "تم استلام الطلب", "10:42", {}, unreadCount = 3) }

@Preview(name = "Transaction Row RTL", locale = "ar", showBackground = true, backgroundColor = DARK)
@Composable private fun TransactionRowV1Preview() = AutoDriveTheme { TransactionRow("عمولة أسبوعية", "250,000", "اليوم 10:42", AutoDriveStatusTone.Success, statusLabel = "مكتمل") }

@Preview(name = "Pending Request RTL", locale = "ar", showBackground = true, backgroundColor = DARK)
@Composable private fun PendingRequestCardV1Preview() = AutoDriveTheme {
    PendingRequestCard("طلب سحب", "250,000", "منذ ساعتين", "قيد المراجعة", primaryActionLabel = "عرض", onPrimaryAction = {})
}

@Preview(name = "Settings Group RTL", locale = "ar", showBackground = true, backgroundColor = DARK)
@Composable private fun SettingsGroupV1Preview() = AutoDriveTheme {
    SettingsGroup("الحساب", listOf(SettingsGroupItem("name", "الاسم", "محمد"), SettingsGroupItem("edit", "تعديل البيانات", variant = SettingsRowVariant.Navigation)))
}

@Preview(name = "Settings Row RTL", locale = "ar", showBackground = true, backgroundColor = DARK)
@Composable private fun SettingsRowV1Preview() = AutoDriveTheme { SettingsRow("تعديل البيانات", variant = SettingsRowVariant.Navigation, onClick = {}) }

@Preview(name = "Report Stat Tile RTL", locale = "ar", showBackground = true, backgroundColor = DARK)
@Composable private fun ReportStatTileV1Preview() = AutoDriveTheme { ReportStatTile("إجمالي الأسبوع", "1.2M", {}) }

@Preview(name = "Media Actions RTL", locale = "ar", showBackground = true, backgroundColor = DARK)
@Composable private fun MediaActionGroupV1Preview() = AutoDriveTheme {
    MediaActionGroup(MediaActionState.Recording, recordingTime = "00:14", onCamera = {}, onGallery = {}, onStartVoice = {}, onStopVoice = {}, onRemoveMedia = {})
}

@Preview(name = "Search Results RTL", locale = "ar", showBackground = true, backgroundColor = DARK)
@Composable private fun SearchResultsListV1Preview() = AutoDriveTheme {
    SearchResultsList(query = "", onQueryChange = {}, placeholder = "ابحث", state = SearchResultsState.Empty, items = emptyList<String>(), emptyTitle = "لا نتائج", emptyBody = "جرّب عبارة أخرى") { Text(it) }
}

@Preview(name = "Empty Screen RTL", locale = "ar", showBackground = true, backgroundColor = DARK)
@Composable private fun EmptyScreenV1Preview() = AutoDriveTheme { EmptyScreen("لا توجد بيانات", "ستظهر هنا عند توفرها") }

@Preview(name = "Error Screen RTL", locale = "ar", showBackground = true, backgroundColor = DARK)
@Composable private fun ErrorScreenV1Preview() = AutoDriveTheme { ErrorScreen("تعذر التحميل", "تحقق من الاتصال وحاول مجددًا", "إعادة المحاولة", {}) }

@Preview(name = "Loading Screen RTL", locale = "ar", showBackground = true, backgroundColor = DARK)
@Composable private fun LoadingScreenV1Preview() = AutoDriveTheme { LoadingScreen(label = "جارٍ التحميل") }
