#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
VM="$ROOT/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileViewModel.kt"
STATE="$ROOT/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileUiState.kt"
REPO="$ROOT/src/main/kotlin/com/autodrive/app/feature/profile/data/ProfileRepositoryImpl.kt"

python3 - "$VM" "$STATE" "$REPO" <<'PY'
from pathlib import Path
import re, sys
vm, state, repo = map(lambda p: Path(p).read_text(), sys.argv[1:])

def section(text, start, end):
    a = text.index(start)
    b = text.index(end, a)
    return text[a:b]

def require(cond, message):
    if not cond:
        raise SystemExit(f"FAIL: {message}")
    print(f"PASS: {message}")

account = section(vm, "fun saveAccount", "fun savePayout")
payout = section(vm, "fun savePayout", "fun saveWorkshop")
workshop = section(vm, "fun saveWorkshop", "private fun validationError")
target = section(vm, "fun setWeeklyTarget", "fun startEditing")

require("enum class ProfileEditSection" in state and "editingSection: ProfileEditSection? = null" in state, "section editing state replaces global boolean")
require("fullName = normalizedName" in account and "phone = normalizedPhone" in account, "saveAccount updates account fields")
require(all(x not in account for x in ("bankName =", "bankAccount =", "workshopName =", "specialty =", "workersCount =", "address =")), "saveAccount preserves payout/workshop fields")
require("normalizedName.isBlank()" in account and "normalizedPhone.isBlank()" in account, "blank fullName/phone are rejected")
require("bankName.trim().ifBlank { null }" in payout, "blank bankName clears to null")
require("bankAccount.trim().ifBlank { null }" in payout, "blank bankAccount clears to null")
require("Regex(" not in payout and "toInt" not in payout and "toLong" not in payout, "payout account accepts non-numeric text")
require("normalizedWorkers.isBlank() -> null" in workshop, "blank workersCount clears to null")
require("normalizedWorkers.toIntOrNull()" in workshop and "عدد العمال غير صالح" in workshop, "invalid workersCount is rejected")
require("current.accountType != AccountType.WORKSHOP_OWNER" in workshop, "marketer cannot save workshop fields")
require("dashboardPreferences.weeklyTarget = clamped" in target and "competition" not in target.lower(), "weekly target remains local and competition-independent")
require('syncStatus   = "PENDING"' in repo or 'syncStatus = "PENDING"' in repo, "optimistic Room row remains PENDING")
require('idempotencyKey = profileIdempotencyKey' in repo and '"profile:${user.userId}"' in repo, "profile Outbox idempotency key is preserved")
require('operation = "UPDATE_PROFILE"' in repo, "profile Outbox operation is preserved")
require("explicitUpdatePayload" in repo and "JsonNull" in repo, "cleared nullable fields are explicit nulls in direct PATCH")
require("encodeDefaults = true" in repo and "explicitNulls = true" in repo, "Outbox payload records explicit nullable fields")
PY
