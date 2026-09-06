#!/usr/bin/env python3
from pathlib import Path
import re, sys

ROOT = Path(__file__).resolve().parents[1]
DS = ROOT / 'core/designsystem'
SRC = DS / 'src/main/kotlin/com/autodrive/app/core/designsystem'
RES = DS / 'src/main/res'

errors=[]
notes=[]

def check(cond, msg):
    if not cond: errors.append(msg)

def text(path):
    return path.read_text(encoding='utf-8') if path.exists() else ''

# 1) Locked architecture / foundations
foundation_files = [
    'foundation/color/ColorTokens.kt', 'foundation/spacing/SpacingTokens.kt',
    'foundation/radius/RadiusTokens.kt', 'foundation/border/BorderTokens.kt',
    'foundation/icon/IconTokens.kt', 'foundation/motion/MotionTokens.kt',
    'foundation/typography/TypographyTokens.kt', 'theme/Theme.kt',
]
for rel in foundation_files:
    check((SRC/rel).is_file(), f'missing foundation/theme file: {rel}')

foundation_symbols = [
    'AutoDriveSurface','AutoDriveBrand','AutoDriveStatus','AutoDriveFinance','AutoDriveText',
    'AutoDriveBorderColor','AutoDriveInstrument','AutoDriveOpacity','AutoDriveSpace','AutoDriveRadius',
    'AutoDriveBorder','AutoDriveIconSize','AutoDriveMotion','AutoDriveTypography','AutoDriveStatXL'
]
all_foundation='\n'.join(text(p) for p in (SRC/'foundation').rglob('*.kt')) + '\n' + text(SRC/'theme/Theme.kt')
for sym in foundation_symbols:
    check(sym in all_foundation, f'missing foundation symbol: {sym}')

# 2) V1 component implementation and preview coverage
components = [
    'AutoDrivePrimaryButton','AutoDriveSecondaryButton','AutoDriveTextButton','AutoDriveIconButton','AutoDriveFab',
    'AutoDriveTextField','AutoDriveSearchField','AutoDriveNumericField','AutoDriveSelectionField',
    'AutoDriveCard','AutoDriveMetricCard','AutoDriveHighlightCard','AutoDriveAlertCard',
    'AutoDriveBottomNavigation','AutoDriveTopHeader','AutoDriveBackHeader',
    'AutoDriveBadge','AutoDriveStatusChip','AutoDriveSnackbarContent','AutoDriveDialog','AutoDriveBottomSheet','AutoDriveLoadingState','AutoDriveEmptyState',
    'AutoDriveAvatar','AutoDriveListRow','AutoDriveSectionHeader','AutoDriveDivider','AutoDriveStatValue','AutoDriveStatusIndicator','AutoDriveStepIndicator','AutoDriveInstrumentNumber',
]
component_dirs=['components/actions','components/inputs','components/containers','components/navigation','components/feedback','components/data']
component_source='\n'.join(text(p) for d in component_dirs for p in (SRC/d).glob('*.kt'))
for name in components:
    check(re.search(rf'\bfun\s+{re.escape(name)}\s*\(', component_source) is not None, f'missing V1 component: {name}')

component_preview = text(SRC/'components/preview/V1ComponentPreviews.kt')
check(component_preview.count('@Preview') >= len(components), f'component preview count {component_preview.count("@Preview")} < {len(components)}')
for name in components:
    check(name in component_preview, f'component has no dedicated preview reference: {name}')
check(component_preview.count('locale = "ar"') >= len(components), 'all component previews must explicitly exercise Arabic RTL locale')

# 3) V1 pattern implementation and preview coverage
patterns = [
    'ScreenHeader','DashboardHero','MetricSummary','ConversationItem','TransactionRow','PendingRequestCard',
    'SettingsGroup','SettingsRow','ReportStatTile','MediaActionGroup','SearchResultsList','EmptyScreen','ErrorScreen','LoadingScreen'
]
pattern_source='\n'.join(text(p) for p in (SRC/'patterns').rglob('*.kt') if 'preview' not in p.parts)
for name in patterns:
    check(re.search(rf'\bfun(?:\s*<[^>]+>)?\s+{re.escape(name)}\s*\(', pattern_source) is not None, f'missing V1 pattern: {name}')
pattern_preview = text(SRC/'patterns/preview/V1PatternPreviews.kt')
check(pattern_preview.count('@Preview') >= len(patterns), f'pattern preview count {pattern_preview.count("@Preview")} < {len(patterns)}')
for name in patterns:
    check(name in pattern_preview, f'pattern has no dedicated preview reference: {name}')
check(pattern_preview.count('locale = "ar"') >= len(patterns), 'all pattern previews must explicitly exercise Arabic RTL locale')

# 4) DS must remain presentation-only
forbidden_patterns = {
    'ViewModel/lifecycle': r'androidx\.lifecycle|import\s+.*\bViewModel\b',
    'Hilt/injection': r'androidx\.hilt|dagger\.hilt|javax\.inject|hiltViewModel\s*\(',
    'feature dependency': r'import\s+com\.autodrive\.app\.feature\.',
    'data/session/network dependency': r'import\s+com\.autodrive\.app\.core\.(?:database|network|session)\.',
    'repository ownership': r'\bRepository\b',
    'navigation controller': r'\bNavController\b',
}
ds_code='\n'.join(text(p) for p in SRC.rglob('*.kt'))
for label, pattern in forbidden_patterns.items():
    check(re.search(pattern, ds_code) is None, f'forbidden DS dependency/state ownership: {label}')

build = text(DS/'build.gradle.kts')
for token in ['libs.plugins.hilt','libs.plugins.ksp','lifecycle','viewmodel','hilt.android','hilt.compiler','hilt.navigation']:
    check(token not in build.lower(), f'forbidden DS Gradle dependency/plugin: {token}')

# 5) State/resource ownership corrections locked by Session 06
check(not (SRC/'components/BottomNavBadge.kt').exists(), 'BottomNavBadge.kt still owned by DS')
check(not (SRC/'components/BottomNavigationComponents.kt').exists(), 'legacy bottom navigation compatibility file still exists')
check(not (SRC/'components/SharedComponents.kt').exists(), 'legacy shared components compatibility file still exists')
check(not (SRC/'components/SevenSegment.kt').exists(), 'legacy generic seven-segment component still exists')
observer = ROOT/'feature/notifications/src/main/kotlin/com/autodrive/app/feature/notifications/data/UnreadMessagesObserver.kt'
check(observer.exists(), 'feature-owned UnreadMessagesObserver missing')

platform_build = text(ROOT/'core/platform/build.gradle.kts')
check('api(project(":core:designsystem"))' not in platform_build, 'core:platform still re-exports core:designsystem')

# DS should retain fonts but not feature/app-owned resources identified in Session 06.
if RES.exists():
    resource_names=[p.name for p in RES.rglob('*') if p.is_file()]
    for bad in ['login_hero.png','whatsapp.png','logo_benzin.png','file_paths.xml','ic_launcher.xml','ic_launcher_foreground.xml','ic_launcher_background.xml']:
        check(bad not in resource_names, f'feature/app-owned resource still in DS: {bad}')
    check(not any(name.startswith('am_dynamo_') for name in resource_names), 'Home Dynamo resources still in DS')
    check(any(p.suffix.lower() in {'.ttf','.otf'} for p in RES.rglob('*') if p.is_file()), 'DS font resources missing')

# 6) Generic instrument primitive must be public, low-level digit primitive private.
data_file=text(SRC/'components/data/DataComponents.kt')
check(re.search(r'\bfun\s+AutoDriveInstrumentNumber\s*\(', data_file) is not None, 'generic instrument number missing')
check(re.search(r'private\s+fun\s+SegmentDigit\s*\(', data_file) is not None, 'low-level segment primitive not private')

# 7) Simple structural sanity: braces/parens balanced after stripping comments/strings roughly.
def strip_kotlin(s: str) -> str:
    s=re.sub(r'/\*.*?\*/', '', s, flags=re.S)
    s=re.sub(r'//.*', '', s)
    s=re.sub(r'""".*?"""', '""', s, flags=re.S)
    s=re.sub(r'"(?:\\.|[^"\\])*"', '""', s)
    s=re.sub(r"'(?:\\.|[^'\\])'", "''", s)
    return s
for p in SRC.rglob('*.kt'):
    s=strip_kotlin(text(p))
    for a,b,label in [('{','}','braces'),('(',')','parentheses'),('[',']','brackets')]:
        depth=0
        ok=True
        for ch in s:
            if ch==a: depth+=1
            elif ch==b:
                depth-=1
                if depth<0: ok=False; break
        check(ok and depth==0, f'unbalanced {label}: {p.relative_to(ROOT)}')

if errors:
    print('V07 DESIGN SYSTEM STATIC VERIFICATION: FAIL')
    for e in errors: print(' -',e)
    sys.exit(1)
print('V07 DESIGN SYSTEM STATIC VERIFICATION: PASS')
print(f' - foundations/theme files: {len(foundation_files)}')
print(f' - V1 components: {len(components)}')
print(f' - component previews: {component_preview.count("@Preview")}')
print(f' - V1 patterns: {len(patterns)}')
print(f' - RTL pattern previews: {pattern_preview.count("@Preview")}')
print(' - forbidden DS dependencies/state ownership: none')
print(' - Session 06 ownership boundaries: enforced')
