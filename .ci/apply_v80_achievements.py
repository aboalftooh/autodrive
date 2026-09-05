from pathlib import Path
import re

ROOT = Path('.')

def patch(path, pairs):
    p = ROOT / path
    s = p.read_text(encoding='utf-8')
    for old, new in pairs:
        if old not in s:
            raise SystemExit(f'missing patch target in {path}: {old[:120]!r}')
        s = s.replace(old, new, 1)
    p.write_text(s, encoding='utf-8')

patch('settings.gradle.kts', [
    ('include(":feature:commission", ":feature:balance", ":feature:profile")', 'include(":feature:commission", ":feature:balance", ":feature:profile", ":feature:achievements")'),
])

patch('app/build.gradle.kts', [
    ('    implementation(project(":feature:profile"))\n', '    implementation(project(":feature:profile"))\n    implementation(project(":feature:achievements"))\n'),
])

patch('app/src/main/kotlin/com/autodrive/app/navigation/AppDestinations.kt', [
    ('    data object ActivityLog     : Screen("activity_log?filter={filter}") {\n        fun createRoute(filter: String? = null) =\n            if (filter != null) "activity_log?filter=$filter" else "activity_log"\n    }\n', '    data object Achievements    : Screen("achievements")\n'),
])

patch('app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeScreen.kt', [
    ('import androidx.compose.material.icons.outlined.BarChart\n', ''),
    ('import androidx.compose.material.icons.rounded.Add\n', 'import androidx.compose.material.icons.rounded.Add\nimport androidx.compose.material.icons.rounded.EmojiEvents\n'),
    ('    onNavigateLog: (String?) -> Unit,\n', '    onNavigateAchievements: () -> Unit,\n'),
    ('                        "reports" -> onNavigateLog(null)\n', '                        "achievements" -> onNavigateAchievements()\n'),
    ('    AutoDriveNavigationItem("reports", "التقارير", Icons.Outlined.BarChart),\n', '    AutoDriveNavigationItem("achievements", "إنجازاتي", Icons.Rounded.EmojiEvents),\n'),
])

patch('app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt', [
    ('import androidx.compose.material.icons.rounded.BarChart\n', 'import androidx.compose.material.icons.rounded.EmojiEvents\n'),
    ('    onNavigateLog: () -> Unit,\n', '    onNavigateAchievements: () -> Unit,\n'),
    ('                        "reports" -> onNavigateLog()\n', '                        "achievements" -> onNavigateAchievements()\n'),
    ('    AutoDriveNavigationItem("reports", "التقارير", Icons.Rounded.BarChart),\n', '    AutoDriveNavigationItem("achievements", "إنجازاتي", Icons.Rounded.EmojiEvents),\n'),
])

profile = ROOT / 'feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt'
s = profile.read_text(encoding='utf-8')
for old, new in [
    ('import androidx.compose.material.icons.rounded.BarChart\n', 'import androidx.compose.material.icons.rounded.EmojiEvents\n'),
    ('    onNavigateLog: () -> Unit,\n', '    onNavigateAchievements: () -> Unit,\n'),
    ('                        "reports" -> onNavigateLog()\n', '                        "achievements" -> onNavigateAchievements()\n'),
    ('AutoDriveNavigationItem("reports", "التقارير", Icons.Rounded.BarChart)', 'AutoDriveNavigationItem("achievements", "إنجازاتي", Icons.Rounded.EmojiEvents)'),
]:
    if old not in s:
        raise SystemExit(f'missing profile patch target: {old!r}')
    s = s.replace(old, new, 1)
profile.write_text(s, encoding='utf-8')

nav_path = ROOT / 'app/src/main/kotlin/com/autodrive/app/navigation/NavigationGraphs.kt'
nav = nav_path.read_text(encoding='utf-8')
nav = nav.replace('import com.autodrive.app.feature.balance.presentation.BalanceScreen\n', 'import com.autodrive.app.feature.balance.presentation.BalanceScreen\nimport com.autodrive.app.feature.achievements.presentation.AchievementsScreen\n', 1)
nav = nav.replace('import com.autodrive.app.feature.reports.presentation.log.ActivityLogScreen\n', '', 1)
nav = nav.replace('        onNavigateLog = { filter -> navController.navigate(Screen.ActivityLog.createRoute(filter)) },\n', '        onNavigateAchievements = { navController.navigate(Screen.Achievements.route) },\n', 1)
nav = nav.replace('        onNavigateLog = { navController.navigate(Screen.ActivityLog.createRoute()) },\n', '        onNavigateAchievements = { navController.navigate(Screen.Achievements.route) },\n', 1)
nav = nav.replace('        onNavigateLog = { navController.navigate(Screen.ActivityLog.createRoute()) },\n', '        onNavigateAchievements = { navController.navigate(Screen.Achievements.route) },\n', 1)
pattern = re.compile(r'\ncomposable\(\n    route = Screen\.ActivityLog\.route,.*?\n\}\n\n(?=composable\(\n    route = Screen\.InvoiceList\.route,)', re.S)
nav, count = pattern.subn('\n', nav, count=1)
if count != 1:
    raise SystemExit('ActivityLog composable block not found')
ach = '''composable(Screen.Achievements.route) {\n    AchievementsScreen(\n        onNavigateHome = { navController.navigate(Screen.Home.route) },\n        onNavigateRecent = { navController.navigate(Screen.RecentActivity.createRoute()) },\n        onNavigateProfile = { navController.navigate(Screen.Profile.route) },\n        onAddClick = onOpenNewChat,\n        unreadMessages = unreadMessages,\n        onOpenAllCommissions = { navController.navigate(Screen.CommissionReport.route) },\n        onOpenBalance = { navController.navigate(Screen.Balance.route) },\n        onOpenPendingCommissions = { navController.navigate(Screen.CommissionReport.route) },\n    )\n}\n\n'''
anchor = 'composable(\n    route = Screen.InvoiceList.route,'
if anchor not in nav:
    raise SystemExit('InvoiceList anchor missing')
nav = nav.replace(anchor, ach + anchor, 1)
nav_path.write_text(nav, encoding='utf-8')

old_screen = ROOT / 'app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt'
if old_screen.exists():
    old_screen.unlink()

checks = {
    'settings.gradle.kts': ':feature:achievements',
    'app/src/main/kotlin/com/autodrive/app/navigation/AppDestinations.kt': 'data object Achievements',
    'app/src/main/kotlin/com/autodrive/app/navigation/NavigationGraphs.kt': 'composable(Screen.Achievements.route)',
}
for path, token in checks.items():
    if token not in (ROOT / path).read_text(encoding='utf-8'):
        raise SystemExit(f'check failed: {path} -> {token}')
for path in [
    'app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeScreen.kt',
    'app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt',
    'feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt',
]:
    text = (ROOT / path).read_text(encoding='utf-8')
    assert 'AutoDriveNavigationItem("achievements", "إنجازاتي"' in text
    assert '"reports"' not in text
assert 'Screen.ActivityLog' not in nav_path.read_text(encoding='utf-8')
assert not old_screen.exists()
print('V80 achievements navigation migration: PASS')
