#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SCREEN="$ROOT/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt"
VM="$ROOT/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileViewModel.kt"
INPUTS="$ROOT/../../core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/inputs/InputComponents.kt"

python3 - "$SCREEN" "$VM" "$INPUTS" <<'PY'
from pathlib import Path
import sys
screen, vm, inputs = [Path(p).read_text() for p in sys.argv[1:]]

def section(text, start, end):
    a = text.index(start)
    b = text.index(end, a)
    return text[a:b]

def require(cond, message):
    if not cond:
        raise SystemExit(f"FAIL: {message}")
    print(f"PASS: {message}")

header = section(screen, 'ScreenHeader(title = "الإعدادات")', 'val user = state.user')
account = section(screen, 'private fun AccountEditSheet(', 'private fun PayoutEditSheet(')
payout = section(screen, 'private fun PayoutEditSheet(', 'private fun WorkshopEditSheet(')
workshop = section(screen, 'private fun WorkshopEditSheet(', 'private fun WeeklyTargetSheet(')
target = section(screen, 'private fun WeeklyTargetSheet(', 'private fun SaveError(')

require('trailing =' not in header and 'Icons.Rounded.Edit' not in screen, 'no global edit button in header')
require('Icons.Rounded.ExitToApp' not in screen, 'no logout button in header')
require(all(x in account for x in ('"الاسم الكامل"', '"الهاتف"', 'onSave(fullName, phone)')), 'account editor contains account fields only')
require(all(x not in account for x in ('"اسم البنك"', '"رقم الحساب / IBAN"', '"اسم الورشة"', '"عدد العمال"')), 'account editor excludes payout/workshop fields')
require(all(x in payout for x in ('"اسم البنك"', '"رقم الحساب / IBAN"', 'onSave(bankName, bankAccount)')), 'payout editor contains payout fields only')
require(all(x not in payout for x in ('"الاسم الكامل"', '"الهاتف"', '"اسم الورشة"', '"عدد العمال"')), 'payout editor excludes account/workshop fields')
require('if (user.accountType == AccountType.WORKSHOP_OWNER)' in screen and 'if (user.workshopName != null)' not in screen, 'workshop section is based on ownership, not workshopName presence')
require('user.accountType == AccountType.WORKSHOP_OWNER' in screen, 'workshop is hidden for marketer accounts')
require('label = "رقم الحساب / IBAN"' in payout and 'KeyboardType.Ascii' in payout and 'AutoDriveTextField(' in payout, 'IBAN accepts letters through a text/ascii field')
require('bankName.trim().ifBlank { null }' in vm and 'bankAccount.trim().ifBlank { null }' in vm, 'blank payout remains saveable as explicit clearing')
require('SettingsRowVariant.Destructive' in screen and 'viewModel::requestSignOut' in screen and 'showSignOutConfirmDialog' in screen, 'logout row keeps confirmation flow')
require('هدف شخصي لعرض تقدمك في الشاشة الرئيسية، ولا يؤثر على ترتيب المسابقة.' in target, 'weekly target copy states no competition effect')
require(all(x in screen for x in ('onNavigateAbout()', 'onNavigatePrivacy()', 'onNavigateFaq()')), 'About/Privacy/FAQ routes are preserved')
require(all(x in screen for x in ('"home" -> onNavigateHome()', '"messages" -> onNavigateRecent()', '"reports" -> onNavigateLog()', 'selectedItemId = "settings"')), 'bottom navigation behavior is preserved')
require('AutoDriveBottomSheet(' in account and 'AutoDriveBottomSheet(' in payout and 'AutoDriveBottomSheet(' in workshop and 'AutoDriveBottomSheet(' in target, 'all four editors use AutoDriveBottomSheet')
require('rememberSaveable' in account and 'rememberSaveable' in payout and 'rememberSaveable' in workshop, 'editor drafts survive recomposition')
require('if (!state.isSaving) viewModel.cancelEditing()' in screen, 'saveable sheets cannot dismiss while saving')
require('keyboardActions: KeyboardActions = KeyboardActions.Default' in inputs, 'numeric field supports IME actions required by workers count')
require('KeyboardType.Phone' in account and 'KeyboardType.Number' in workshop and 'ImeAction.Next' in screen and 'ImeAction.Done' in screen, 'keyboard and IME semantics are explicit')
require('EditProfileForm' not in screen and 'saveProfile(' not in screen, 'global profile form is removed from the screen')
PY
