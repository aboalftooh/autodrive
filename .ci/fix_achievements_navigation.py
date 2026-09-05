from pathlib import Path

ROOT = Path('.')

def patch(path: str, replacements: list[tuple[str, str]]) -> None:
    p = ROOT / path
    s = p.read_text(encoding='utf-8')
    for old, new in replacements:
        if old not in s:
            raise SystemExit(f'Navigation patch target not found in {path}: {old[:100]!r}')
        s = s.replace(old, new, 1)
    p.write_text(s, encoding='utf-8')

patch('app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeScreen.kt', [
    ('import androidx.compose.material.icons.rounded.Add\n', 'import androidx.compose.material.icons.rounded.Add\nimport androidx.compose.material.icons.rounded.EmojiEvents\n'),
    ('    onNavigateProfile: () -> Unit,\n', '    onNavigateProfile: () -> Unit,\n    onNavigateAchievements: () -> Unit,\n'),
    ('                        "messages" -> onNavigateRecent()\n                        "settings" -> onNavigateProfile()\n', '                        "messages" -> onNavigateRecent()\n                        "achievements" -> onNavigateAchievements()\n                        "settings" -> onNavigateProfile()\n'),
    ('    AutoDriveNavigationItem("messages", "الرسائل", Icons.Outlined.Message, unreadMessages),\n    AutoDriveNavigationItem("settings", "الإعدادات", Icons.Outlined.Settings),\n', '    AutoDriveNavigationItem("messages", "الرسائل", Icons.Outlined.Message, unreadMessages),\n    AutoDriveNavigationItem("achievements", "إنجازاتي", Icons.Rounded.EmojiEvents),\n    AutoDriveNavigationItem("settings", "الإعدادات", Icons.Outlined.Settings),\n'),
])

patch('feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/recent/RecentActivityScreen.kt', [
    ('import androidx.compose.material.icons.rounded.Add\n', 'import androidx.compose.material.icons.rounded.Add\nimport androidx.compose.material.icons.rounded.EmojiEvents\n'),
    ('    onNavigateProfile: () -> Unit,\n', '    onNavigateProfile: () -> Unit,\n    onNavigateAchievements: () -> Unit,\n'),
    ('                        "home" -> onNavigateHome()\n                        "settings" -> onNavigateProfile()\n', '                        "home" -> onNavigateHome()\n                        "achievements" -> onNavigateAchievements()\n                        "settings" -> onNavigateProfile()\n'),
    ('    AutoDriveNavigationItem("messages", "المحادثات", Icons.Rounded.Message, unreadMessages),\n    AutoDriveNavigationItem("settings", "الإعدادات", Icons.Rounded.Settings),\n', '    AutoDriveNavigationItem("messages", "المحادثات", Icons.Rounded.Message, unreadMessages),\n    AutoDriveNavigationItem("achievements", "إنجازاتي", Icons.Rounded.EmojiEvents),\n    AutoDriveNavigationItem("settings", "الإعدادات", Icons.Rounded.Settings),\n'),
])

patch('feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt', [
    ('import androidx.compose.material.icons.rounded.Add\n', 'import androidx.compose.material.icons.rounded.Add\nimport androidx.compose.material.icons.rounded.EmojiEvents\n'),
    ('    onNavigateRecent: () -> Unit,\n', '    onNavigateRecent: () -> Unit,\n    onNavigateAchievements: () -> Unit,\n'),
    ('                        "home" -> onNavigateHome()\n                        "messages" -> onNavigateRecent()\n', '                        "home" -> onNavigateHome()\n                        "messages" -> onNavigateRecent()\n                        "achievements" -> onNavigateAchievements()\n'),
    ('    AutoDriveNavigationItem("messages", "المحادثات", Icons.Rounded.Message, unreadMessages),\n    AutoDriveNavigationItem("settings", "الإعدادات", Icons.Rounded.Settings),\n', '    AutoDriveNavigationItem("messages", "المحادثات", Icons.Rounded.Message, unreadMessages),\n    AutoDriveNavigationItem("achievements", "إنجازاتي", Icons.Rounded.EmojiEvents),\n    AutoDriveNavigationItem("settings", "الإعدادات", Icons.Rounded.Settings),\n'),
])

patch('feature/achievements/src/main/kotlin/com/autodrive/app/feature/achievements/presentation/AchievementsScreen.kt', [
    ('import androidx.compose.material.icons.rounded.AccountBalanceWallet\n', 'import androidx.compose.material.icons.rounded.AccountBalanceWallet\nimport androidx.compose.material.icons.rounded.Add\n'),
    ('import androidx.compose.material.icons.rounded.EmojiEvents\n', 'import androidx.compose.material.icons.rounded.EmojiEvents\nimport androidx.compose.material.icons.rounded.Home\nimport androidx.compose.material.icons.rounded.Message\nimport androidx.compose.material.icons.rounded.Settings\n'),
    ('import com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor\n', 'import com.autodrive.app.core.designsystem.components.actions.AutoDriveFab\nimport com.autodrive.app.core.designsystem.components.navigation.AutoDriveBottomNavigation\nimport com.autodrive.app.core.designsystem.components.navigation.AutoDriveNavigationItem\nimport com.autodrive.app.core.designsystem.foundation.color.AutoDriveBorderColor\n'),
    ('fun AchievementsScreen(\n    onOpenAllCommissions: () -> Unit,\n', 'fun AchievementsScreen(\n    onNavigateHome: () -> Unit,\n    onNavigateRecent: () -> Unit,\n    onNavigateProfile: () -> Unit,\n    onAddClick: () -> Unit,\n    unreadMessages: Int = 0,\n    onOpenAllCommissions: () -> Unit,\n'),
    ('            state = state,\n            onOpenAllCommissions = onOpenAllCommissions,\n', '            state = state,\n            onNavigateHome = onNavigateHome,\n            onNavigateRecent = onNavigateRecent,\n            onNavigateProfile = onNavigateProfile,\n            onAddClick = onAddClick,\n            unreadMessages = unreadMessages,\n            onOpenAllCommissions = onOpenAllCommissions,\n'),
    ('internal fun AchievementsContent(\n    state: AchievementsUiState,\n', 'internal fun AchievementsContent(\n    state: AchievementsUiState,\n    onNavigateHome: () -> Unit,\n    onNavigateRecent: () -> Unit,\n    onNavigateProfile: () -> Unit,\n    onAddClick: () -> Unit,\n    unreadMessages: Int,\n'),
    ('    Scaffold(containerColor = AutoDriveSurface.Canvas) { scaffoldPadding ->\n', '    Scaffold(\n        containerColor = AutoDriveSurface.Canvas,\n        bottomBar = {\n            AutoDriveBottomNavigation(\n                items = achievementsRootItems(unreadMessages),\n                selectedItemId = "achievements",\n                onItemClick = { item ->\n                    when (item.id) {\n                        "home" -> onNavigateHome()\n                        "messages" -> onNavigateRecent()\n                        "settings" -> onNavigateProfile()\n                    }\n                },\n                centerAction = {\n                    AutoDriveFab(\n                        onClick = onAddClick,\n                        contentDescription = "محادثة جديدة",\n                        icon = Icons.Rounded.Add,\n                    )\n                },\n            )\n        },\n    ) { scaffoldPadding ->\n'),
    ('@Composable\nprivate fun LifetimeCommissionCard(', 'private fun achievementsRootItems(unreadMessages: Int) = listOf(\n    AutoDriveNavigationItem("home", "الرئيسية", Icons.Rounded.Home),\n    AutoDriveNavigationItem("messages", "المحادثات", Icons.Rounded.Message, unreadMessages),\n    AutoDriveNavigationItem("achievements", "إنجازاتي", Icons.Rounded.EmojiEvents),\n    AutoDriveNavigationItem("settings", "الإعدادات", Icons.Rounded.Settings),\n)\n\n@Composable\nprivate fun LifetimeCommissionCard('),
    ('                onOpenAllCommissions = {},\n', '                onNavigateHome = {},\n                onNavigateRecent = {},\n                onNavigateProfile = {},\n                onAddClick = {},\n                unreadMessages = 0,\n                onOpenAllCommissions = {},\n'),
])

patch('app/src/main/kotlin/com/autodrive/app/navigation/NavigationGraphs.kt', [
    ('        onNavigateProfile = { navController.navigateMainTab(Screen.Profile.route) },\n        onNavigateNotifications', '        onNavigateProfile = { navController.navigateMainTab(Screen.Profile.route) },\n        onNavigateAchievements = { navController.navigateMainTab(Screen.Achievements.route) },\n        onNavigateNotifications'),
    ('    AchievementsScreen(\n        onOpenAllCommissions', '    AchievementsScreen(\n        onNavigateHome = { navController.navigateMainTab(Screen.Home.route) },\n        onNavigateRecent = { navController.navigateMainTab(Screen.RecentActivity.createRoute()) },\n        onNavigateProfile = { navController.navigateMainTab(Screen.Profile.route) },\n        onAddClick = onOpenNewChat,\n        unreadMessages = unreadMessages,\n        onOpenAllCommissions'),
    ('        onNavigateProfile = { navController.navigateMainTab(Screen.Profile.route) },\n        onAddClick = onOpenNewChat,\n        onOpenConversation', '        onNavigateProfile = { navController.navigateMainTab(Screen.Profile.route) },\n        onNavigateAchievements = { navController.navigateMainTab(Screen.Achievements.route) },\n        onAddClick = onOpenNewChat,\n        onOpenConversation'),
    ('        onNavigateRecent = { navController.navigateMainTab(Screen.RecentActivity.createRoute()) },\n        onSignedOut', '        onNavigateRecent = { navController.navigateMainTab(Screen.RecentActivity.createRoute()) },\n        onNavigateAchievements = { navController.navigateMainTab(Screen.Achievements.route) },\n        onSignedOut'),
])

nav = (ROOT / 'app/src/main/kotlin/com/autodrive/app/navigation/NavigationGraphs.kt').read_text(encoding='utf-8')
home = (ROOT / 'app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeScreen.kt').read_text(encoding='utf-8')
recent = (ROOT / 'feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/recent/RecentActivityScreen.kt').read_text(encoding='utf-8')
profile = (ROOT / 'feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt').read_text(encoding='utf-8')
ach = (ROOT / 'feature/achievements/src/main/kotlin/com/autodrive/app/feature/achievements/presentation/AchievementsScreen.kt').read_text(encoding='utf-8')

assert 'composable(Screen.Achievements.route)' in nav
assert nav.count('onNavigateAchievements = { navController.navigateMainTab(Screen.Achievements.route) }') == 3
for text in (home, recent, profile, ach):
    assert 'AutoDriveNavigationItem("achievements", "إنجازاتي"' in text
assert 'selectedItemId = "achievements"' in ach
print('Achievements root navigation: PASS')
