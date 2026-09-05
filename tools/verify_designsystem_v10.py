#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
DS = ROOT / 'core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem'
errors = []

def read(path):
    path = ROOT / path if isinstance(path, str) else path
    return path.read_text(encoding='utf-8') if path.exists() else ''

def check(condition, message):
    if not condition:
        errors.append(message)

final_doc = read('docs/design-system/DESIGN_SYSTEM_V1.md')
check('**STATUS:** APPROVED' in final_doc, 'DESIGN_SYSTEM_V1.md is not approved')
for required in [
    'Adding a new Component', 'Modifying an existing Component', 'New-screen review gate',
    'Does an existing Component already solve it?', 'Does an existing Pattern already solve the composition?',
    'Would a semantic Variant solve the remaining difference?', 'Is a new Component genuinely required?',
    '# Decisions', '# Forbidden', '# Deferred', '# Open Issues', '# Next Session Input',
]:
    check(required in final_doc, f'missing governance contract: {required}')

canonical = [
    'components/actions/ActionComponents.kt',
    'components/inputs/InputComponents.kt',
    'components/containers/ContainerComponents.kt',
    'components/navigation/NavigationComponents.kt',
    'components/feedback/FeedbackComponents.kt',
    'components/data/DataComponents.kt',
    'theme/Theme.kt',
]
for rel in canonical:
    check((DS / rel).is_file(), f'missing canonical DS file: {rel}')

legacy_files = [
    'components/SharedComponents.kt',
    'components/BottomNavigationComponents.kt',
    'components/SevenSegment.kt',
    'theme/Typography.kt',
]
for rel in legacy_files:
    check(not (DS / rel).exists(), f'legacy compatibility file restored: {rel}')

prod_roots = [ROOT/'app/src/main/kotlin', ROOT/'feature', DS]
prod_sources = []
for base in prod_roots:
    if base.exists():
        prod_sources.extend(base.rglob('*.kt'))
combined = '\n'.join(read(p) for p in prod_sources)

legacy_symbols = [
    'BgDeep', 'BgSurface1', 'BgSurface2', 'BgSurface3', 'BorderColor',
    'GreenWithdraw', 'GoldPending', 'GrayPaid', 'WhatsAppGreen',
    'TextPrimary', 'TextSecondary', 'TextDisabled', 'AccentBlue', 'OrangeAccent',
    'AutoDriveButton', 'AutoDriveBottomBar', 'BottomNavItem', 'SevenSegmentNumber',
    'SpecialtyPicker', 'UserAvatar',
]
for symbol in legacy_symbols:
    check(re.search(rf'\b{re.escape(symbol)}\b', combined) is None, f'legacy symbol remains in production: {symbol}')

check(re.search(r'^import com\.autodrive\.app\.core\.designsystem\.(?:theme|components)\.\*$', combined, re.M) is None,
      'wildcard root Design System import remains')

# Canonical public component functions use AutoDrive prefix.
for p in (DS/'components').rglob('*.kt'):
    if '/preview/' in str(p).replace('\\','/'):
        continue
    src = read(p)
    for match in re.finditer(r'(?m)^(?:public\s+)?fun\s+(?:<[^>]+>\s*)?([A-Z][A-Za-z0-9_]*)\s*\(', src):
        name = match.group(1)
        check(name.startswith('AutoDrive'), f'public component lacks AutoDrive prefix: {p.name}:{name}')

# Theme must consume foundations directly and no longer define compatibility aliases.
theme = read(DS/'theme/Theme.kt')
check('foundation.typography.AutoDriveTypography' in theme, 'Theme does not import canonical foundation typography directly')
for alias in ['BgDeep', 'TextPrimary', 'AccentBlue', 'OrangeAccent']:
    check(re.search(rf'\bval\s+{alias}\b', theme) is None, f'legacy theme alias restored: {alias}')

# Six migrated V1 surfaces keep their existing raw-style gate.
strict_files = {
    'Home': 'app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeScreen.kt',
    'Conversations': 'app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt',
    'Reports': 'app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt',
    'Balance': 'feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt',
    'Settings': 'feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt',
    'NewChat': 'feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/NewChatDialog.kt',
}
for label, rel in strict_files.items():
    src = read(rel)
    for pattern in [r'RoundedCornerShape\(', r'\bColor\(', r'\d+(?:\.\d+)?\.dp\b', r'\d+(?:\.\d+)?\.sp\b']:
        check(re.search(pattern, src) is None, f'{label} reintroduced raw styling: {pattern}')

# Deleted compatibility names are also guarded by architecture test.
arch = read('app/src/test/kotlin/com/autodrive/app/architecture/ResponsibilitySplitArchitectureTest.kt')
for rel in legacy_files:
    check(rel in arch, f'architecture test does not guard deleted file: {rel}')

if errors:
    print('V10 CONSOLIDATION & GOVERNANCE VERIFICATION: FAIL')
    for error in errors:
        print(' -', error)
    sys.exit(1)

print('V10 CONSOLIDATION & GOVERNANCE VERIFICATION: PASS')
print(' - final V1 governance document: approved')
print(' - legacy compatibility files: absent')
print(' - legacy theme/component symbols: absent from production')
print(' - canonical responsibility packages: present')
print(' - component naming and migrated-screen style gates: governed')
print(' - runtime-only visual QA remains explicitly deferred')
