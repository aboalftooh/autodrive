#!/usr/bin/env python3
from pathlib import Path
import re, sys

ROOT = Path(__file__).resolve().parents[1]
errors=[]

def read(rel):
    p=ROOT/rel
    return p.read_text(encoding='utf-8') if p.exists() else ''

def check(cond, msg):
    if not cond: errors.append(msg)

state08=read('docs/design-system/08_MIGRATION_STATE.md')
qa=read('docs/design-system/09_VISUAL_QA.md')
spacing=read('core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/foundation/spacing/SpacingTokens.kt')
nav=read('core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/navigation/NavigationComponents.kt')
header=read('core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/header/ScreenHeader.kt')
home=read('app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeScreen.kt')
conv=read('app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt')
reports=read('app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt')
balance=read('feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt')
profile=read('feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt')
newchat=read('feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/NewChatDialog.kt')
withdrawal=read('feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/WithdrawalSheet.kt')

check('**STATUS:** APPROVED' in state08, 'Session 08 is not approved')
check((ROOT/'home.png').is_file(), 'home.png visual source is missing')
check('**STATUS:** APPROVED' in qa, '09_VISUAL_QA.md is not approved')

for symbol in ['AutoDriveContentWidth', 'Readable = 600.dp', 'Dashboard = 840.dp', 'ReportTwoColumn = 360.dp']:
    check(symbol in spacing, f'missing responsive QA token: {symbol}')

check('titleContent:' in nav and 'titleContent:' in header, 'header rich-title slot missing')
check('titleContent = {' in home and 'AutoDriveBrand.Primary' in home, 'Home first-name brand emphasis missing')
check('AutoDriveContentWidth.Dashboard' in home, 'Home dashboard width constraint missing')
check('AutoDriveContentWidth.Readable' in conv, 'Conversations readable width constraint missing')
check('AutoDriveContentWidth.Readable' in profile, 'Settings readable width constraint missing')
check('AutoDriveContentWidth.Dashboard' in reports, 'Reports dashboard width constraint missing')
check('AutoDriveContentWidth.Dashboard' in balance, 'Balance dashboard width constraint missing')
check('ReportMetricPair' in reports and 'AutoDriveContentWidth.ReportTwoColumn' in reports, 'Reports narrow-width fallback missing')

for token in ['AutoDriveDialog(', 'AutoDriveTextField(', 'MediaActionGroup(']:
    check(token in newchat, f'New Chat governed presentation missing: {token}')
check('WithdrawalSheet(' in balance and 'AutoDriveDialog(' in balance, 'Balance sheet/dialog behavior missing')
check('AutoDriveBottomSheet(' in withdrawal, 'Withdrawal governed Bottom Sheet missing')

# Session 09 must not introduce raw screen styling after the v08 migration gate.
strict={
    'Home':home,'Conversations':conv,'Reports':reports,'Balance':balance,'Settings':profile,'NewChat':newchat
}
for name,src in strict.items():
    for pat in [r'RoundedCornerShape\(', r'\bColor\(', r'\d+(?:\.\d+)?\.dp\b', r'\d+(?:\.\d+)?\.sp\b']:
        check(re.search(pat, src) is None, f'{name} reintroduced raw styling: {pat}')

if errors:
    print('V09 VISUAL QA STATIC VERIFICATION: FAIL')
    for e in errors: print(' -', e)
    sys.exit(1)

print('V09 VISUAL QA STATIC VERIFICATION: PASS')
print(' - Home greeting emphasis: governed')
print(' - readable/dashboard width constraints: governed')
print(' - report narrow-width fallback: governed')
print(' - six-screen raw styling regression: none')
print(' - New Chat / Balance modal contracts: preserved')
print(' - runtime-only visual checks: explicitly deferred in 09_VISUAL_QA.md')
