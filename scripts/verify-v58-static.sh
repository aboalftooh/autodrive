#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PASS=0
FAIL=0
check() {
  local label="$1"; shift
  if "$@"; then
    PASS=$((PASS+1)); printf 'PASS  %s\n' "$label"
  else
    FAIL=$((FAIL+1)); printf 'FAIL  %s\n' "$label"
  fi
}
contains() { grep -Fq -- "$2" "$1"; }
not_contains() { ! grep -Fq -- "$2" "$1"; }

COMP="app/src/main/kotlin/com/autodrive/app/feature/competition/data/WeeklyCompetitionRepositoryImpl.kt"
HOME="app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeScreen.kt"
COMP_SCREEN="app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionScreen.kt"
ACH_VM="feature/achievements/src/main/kotlin/com/autodrive/app/feature/achievements/presentation/AchievementsViewModel.kt"
ACH_UI="feature/achievements/src/main/kotlin/com/autodrive/app/feature/achievements/presentation/AchievementsScreen.kt"
PROFILE_VM="feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileViewModel.kt"
PROFILE_UI="feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt"
PROFILE_STATE="feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileUiState.kt"
PROFILE_REPO="feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/data/ProfileRepositoryImpl.kt"
NAV="app/src/main/kotlin/com/autodrive/app/navigation/NavigationGraphs.kt"
DEST="app/src/main/kotlin/com/autodrive/app/navigation/AppDestinations.kt"

# Competition closure
for token in 'startPolling' 'fetchLeaderboardDirectly' 'currentFriday9AM' 'postgrest["invoices"]'; do
  check "competition forbids $token" not_contains "$COMP" "$token"
done
check 'competition rank from get_weekly_competition RPC' contains "$COMP" 'rpc("get_weekly_competition")'
check 'competition history from RPC' contains "$COMP" 'rpc("get_my_competition_history"'
check 'competition wins from RPC' contains "$COMP" 'rpc("get_my_win_weeks")'
check 'DISABLED hidden from Home' contains "$HOME" 'competitionAvailability != CompetitionAvailability.DISABLED'
check 'LOCKED teaser retained' contains "$HOME" 'CompetitionAvailability.LOCKED'
check 'ACTIVE full competition branch retained' contains "$COMP_SCREEN" 'CompetitionAvailability.ACTIVE ->'

# Achievements closure / legacy reports removal
check 'achievements uses weekly performance API' contains "$ACH_VM" 'WeeklyPerformanceApi'
check 'achievements owns root bottom-nav selection' contains "$ACH_UI" 'selectedItemId = "achievements"'
check 'achievements root item is present' contains "$ACH_UI" 'AutoDriveNavigationItem("achievements", "إنجازاتي"'
check 'legacy reports feature removed' test ! -e 'app/src/main/kotlin/com/autodrive/app/feature/reports'
check 'legacy ActivityLog route removed' not_contains "$DEST" 'ActivityLog'
check 'achievements route retained' contains "$DEST" '"achievements"'

# Settings closure
check 'settings uses section editing state' contains "$PROFILE_STATE" 'editingSection: ProfileEditSection?'
check 'settings has no global isEditing state' not_contains "$PROFILE_STATE" 'isEditing'
check 'payout bank can clear to null' contains "$PROFILE_VM" 'bankName.trim().ifBlank { null }'
check 'payout IBAN can clear to null' contains "$PROFILE_VM" 'bankAccount.trim().ifBlank { null }'
check 'workshop ownership guarded' contains "$PROFILE_VM" 'current.accountType != AccountType.WORKSHOP_OWNER'
check 'weekly target remains local preference' contains "$PROFILE_VM" 'dashboardPreferences.weeklyTarget = clamped'
check 'logout remains SignOutAction' contains "$PROFILE_VM" 'SignOutAction'
check 'IBAN field is text ASCII' contains "$PROFILE_UI" 'KeyboardType.Ascii'
check 'IBAN is not numeric field' not_contains "$PROFILE_UI" 'AutoDriveNumericField(bankAccount'
check 'profile optimistic row is pending' contains "$PROFILE_REPO" 'syncStatus   = "PENDING"'
check 'profile update preserves Outbox' contains "$PROFILE_REPO" 'PendingOperationEntity('
check 'profile Outbox is idempotent' contains "$PROFILE_REPO" 'profile:${user.userId}'
check 'profile remote success clears stale Outbox op' contains "$PROFILE_REPO" 'deleteByIdempotencyKey(profileIdempotencyKey)'

# Architecture scans implemented in Python for path-aware checks.
python3 - <<'PY'
from pathlib import Path
import re, sys
root=Path('.')
problems=[]
files=[p for p in root.glob('**/src/main/kotlin/**/*.kt') if '/build/' not in p.as_posix()]
for p in files:
    s=p.as_posix()
    text=p.read_text(errors='ignore')
    if '/domain/' in s or '/presentation/' in s:
        for token in ('com.autodrive.app.core.database','AutoDriveDatabase','io.github.jan.supabase','androidx.work','com.google.firebase'):
            if token in text:
                problems.append(f'infrastructure:{p}:{token}')
    m=re.search(r'/feature/([^/]+)/', '/'+s)
    owner=m.group(1) if m else None
    if owner:
        for i,line in enumerate(text.splitlines(),1):
            m2=re.match(r'import com\.autodrive\.app\.feature\.([^.]+)\.data(?:\.|$)', line)
            if m2 and m2.group(1) != owner:
                problems.append(f'cross-data:{p}:{i}:{line}')
    for i,raw in enumerate(text.splitlines(),1):
        line=raw.strip()
        if line.startswith('//') or line.startswith('*') or line.startswith('/*'):
            continue
        lower=line.lower()
        if any(token in lower for token in ('service_role','supabase_service','jwt_secret')):
            problems.append(f'service-role:{p}:{i}:{line}')
if problems:
    print('\n'.join(problems))
    sys.exit(1)
PY
ARCH=$?
if [[ $ARCH -eq 0 ]]; then PASS=$((PASS+1)); echo 'PASS  architecture ownership/infrastructure/service-role scan'; else FAIL=$((FAIL+1)); echo 'FAIL  architecture ownership/infrastructure/service-role scan'; fi

# Navigation retention
for route in 'home' 'profile' 'balance' 'achievements' 'weekly_competition' 'win_weeks' 'weekly_commissions' 'competition_history' 'about_app' 'privacy_policy' 'faq'; do
  check "route retained: $route" contains "$DEST" "\"$route\""
done
for screen in Home Profile Balance Achievements WeeklyCompetition InvoiceList WinWeeks WeeklyCommissions CompetitionHistory AboutApp PrivacyPolicy Faq; do
  check "navigation destination retained: $screen" contains "$NAV" "Screen.$screen.route"
done

# v57 build contract lock
check 'AGP unchanged' contains 'gradle/libs.versions.toml' 'agp = "8.5.2"'
check 'Kotlin unchanged' contains 'gradle/libs.versions.toml' 'kotlin = "2.0.21"'
check 'Gradle wrapper unchanged' contains 'gradle/wrapper/gradle-wrapper.properties' 'gradle-8.7-bin.zip'
check 'compileSdk unchanged' contains 'app/build.gradle.kts' 'compileSdk = 35'
check 'minSdk unchanged' contains 'app/build.gradle.kts' 'minSdk = 26'
check 'targetSdk unchanged' contains 'app/build.gradle.kts' 'targetSdk = 35'

printf '\nSTATIC_V58: %d passed, %d failed\n' "$PASS" "$FAIL"
[[ "$FAIL" -eq 0 ]]
