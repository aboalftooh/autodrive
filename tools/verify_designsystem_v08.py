#!/usr/bin/env python3
"""Session-60 compatibility bridge for the historical v08 screen-migration gate.

Hard behavior/shell invariants remain enforced. Known v59 Design System debt is
acknowledged by immutable baseline IDs and delegated to the ratchet rather than
being misreported as a fresh v08 migration failure.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
BASELINE = ROOT / "DESIGN_SYSTEM_BASELINE_v59.json"
STATE = ROOT / "core/designsystem/verification/designsystem-ratchet-state.json"
BASELINE_SHA = "906c12831752daa2f7fce190a3aab37a75817c415b055c09ab367d8207248abc"
HISTORICAL_HOME_DEBT_IDS = {
    "DS59-HOME-001", "DS59-HOME-002", "DS59-HOME-003", "DS59-HOME-004",
    "DS59-HOME-005", "DS59-HOME-006", "DS59-HOME-007", "DS59-HOME-008",
}
REPORTS_DEBT_IDS = {"DS59-REPORTS-001", "DS59-REPORTS-002", "DS59-REPORTS-003"}
SETTINGS_DEBT_IDS = {"DS59-SETTINGS-001"}
OPEN_POST_V61_DEBT_IDS = REPORTS_DEBT_IDS | SETTINGS_DEBT_IDS
DEBT_IDS = HISTORICAL_HOME_DEBT_IDS | OPEN_POST_V61_DEBT_IDS


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run(root: Path) -> tuple[int, dict[str, Any]]:
    errors: list[str] = []
    checks: list[dict[str, Any]] = []

    def check(ok: bool, cid: str, message: str) -> None:
        checks.append({"id": cid, "status": "PASS" if ok else "FAIL", "message": message})
        if not ok: errors.append(f"{cid}: {message}")

    def text(rel: str) -> str:
        p = root / rel
        if not p.is_file():
            check(False, "V08-FILE", f"missing file: {rel}")
            return ""
        return p.read_text(encoding="utf-8")

    baseline_path = root / "DESIGN_SYSTEM_BASELINE_v59.json"
    state_path = root / "core/designsystem/verification/designsystem-ratchet-state.json"
    if not baseline_path.is_file() or not state_path.is_file():
        raise FileNotFoundError("baseline/state missing")
    if sha(baseline_path) != BASELINE_SHA:
        errors.append("V08-AUTHORITY: v59 baseline SHA drift")
    baseline = json.loads(baseline_path.read_text(encoding="utf-8"))
    state = json.loads(state_path.read_text(encoding="utf-8"))
    baseline_ids = {f.get("finding_id") for f in baseline.get("findings", []) if f.get("classification") == "CONFIRMED_VIOLATION"}
    state_ids = {f.get("findingId") for f in state.get("confirmedFindings", [])}
    accepted_version = state.get("acceptedVersion")
    check(DEBT_IDS <= baseline_ids, "V08-DEBT-BASELINE", "all historical Home/Reports/Settings debt IDs resolve in immutable v59 baseline")
    if accepted_version in {"v64", "v65", "v66"}:
        check(not state_ids, "V08-V64-CONFIRMED", f"accepted {accepted_version} open confirmed findings = 0")
        check(len(state.get("acceptedCandidates", [])) == 6, "V08-V64-CANDIDATES", f"accepted {accepted_version} candidates = exact historical 6")
        check(not (HISTORICAL_HOME_DEBT_IDS & state_ids), "V08-HOME-RESOLVED", f"Home debt remains resolved at {accepted_version}")
        check(not (REPORTS_DEBT_IDS & state_ids), "V08-REPORTS-RESOLVED", f"Reports debt remains resolved at {accepted_version}")
        check(not (SETTINGS_DEBT_IDS & state_ids), "V08-SETTINGS-RESOLVED", f"Settings debt remains resolved at {accepted_version}")
    elif accepted_version == "v63":
        check(not (HISTORICAL_HOME_DEBT_IDS & state_ids), "V08-HOME-RESOLVED", "resolved Home debt is absent from accepted v63 open state")
        check(not (REPORTS_DEBT_IDS & state_ids), "V08-REPORTS-RESOLVED", "resolved Reports debt is absent from accepted v63 open state")
        check(not (SETTINGS_DEBT_IDS & state_ids), "V08-SETTINGS-RESOLVED", "resolved Settings debt is absent from accepted v63 open state")
        check(len(state.get("confirmedFindings", [])) == 46, "V08-OPEN-COUNT", "accepted v63 open confirmed findings = 46")
    elif accepted_version == "v62":
        check(not (HISTORICAL_HOME_DEBT_IDS & state_ids), "V08-HOME-RESOLVED", "resolved Home debt is absent from accepted v62 open state")
        check(not (REPORTS_DEBT_IDS & state_ids), "V08-REPORTS-RESOLVED", "resolved Reports debt is absent from accepted v62 open state")
        check(SETTINGS_DEBT_IDS <= state_ids, "V08-OPEN-RATCHET", "Settings debt remains represented")
        check(len(state.get("confirmedFindings", [])) == 47, "V08-OPEN-COUNT", "accepted v62 open confirmed findings = 47")
    elif accepted_version == "v61":
        check(not (HISTORICAL_HOME_DEBT_IDS & state_ids), "V08-HOME-RESOLVED", "resolved Home debt is absent from accepted v61 open state")
        check(OPEN_POST_V61_DEBT_IDS <= state_ids, "V08-OPEN-RATCHET", "Reports/Settings open debt remains represented")
        check(len(state.get("confirmedFindings", [])) == 57, "V08-OPEN-COUNT", "accepted v61 open confirmed findings = 57")
    else:
        check(DEBT_IDS <= state_ids, "V08-DEBT-RATCHET", "pre-accept v59-backed state still represents historical debt")
        check(len(state.get("confirmedFindings", [])) == 77, "V08-PRE-ACCEPT-COUNT", "pre-accept v59-backed state remains 77")
    check(sum(1 for f in baseline.get("findings", []) if f.get("classification") == "CONFIRMED_VIOLATION") == 77, "V08-DEBT-COUNT", "v59 historical debt remains 77 confirmed findings")

    screens = {
        "home": "app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeScreen.kt",
        "conversations": "app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt",
        "reports": "app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt",
        "balance": "feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt",
        "settings": "feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt",
        "new_chat": "feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/NewChatDialog.kt",
    }
    source = {name: text(rel) for name, rel in screens.items()}
    withdrawal = text("feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/WithdrawalSheet.kt")
    home_support = text("app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeSupportCards.kt")
    home_hero = text("app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeHeroComponents.kt")

    # HOME_V1 is a hard post-v61 source contract.
    check(re.search(r"(?<![A-Za-z0-9_])ScreenHeader\s*\(", source["home"]) is not None, "V08-HOME-HEADER", "Home uses ScreenHeader")
    check(re.search(r"(?<![A-Za-z0-9_])DashboardHero\s*\(", home_hero) is not None, "V08-HOME-HERO", "Home hero uses DashboardHero")
    check(re.search(r"(?<![A-Za-z0-9_])AutoDriveInstrumentNumber\s*\(", home_hero) is not None, "V08-HOME-INSTRUMENT", "Home uses AutoDriveInstrumentNumber")
    check("AutoDriveContentWidth.Dashboard" in source["home"], "V08-HOME-WIDTH", "Home applies Dashboard width contract")
    check("HomeHeader(" not in source["home"], "V08-HOME-NO-LOCAL-HEADER", "local HomeHeader responsibility removed")

    # Root shell invariants.
    for name in ("home", "conversations", "reports", "settings"):
        s = source[name]
        check("AutoDriveBottomNavigation(" in s, f"V08-SHELL-{name}-NAV", f"{name}: V1 bottom navigation present")
        check("AutoDriveFab(" in s, f"V08-SHELL-{name}-FAB", f"{name}: center FAB present")
        check("unreadMessages: Int" in s, f"V08-SHELL-{name}-UNREAD", f"{name}: unread presentation value accepted")
    for name in ("conversations", "reports", "settings"):
        check("ScreenHeader(" in source[name], f"V08-HEADER-{name}", f"{name}: ScreenHeader present")
    for name in ("balance", "new_chat"):
        check("AutoDriveBottomNavigation(" not in source[name], f"V08-CHILD-{name}", f"{name}: root bottom navigation hidden")
    for name, selected in (("home","home"),("conversations","messages"),("reports","reports"),("settings","settings")):
        check(f'selectedItemId = "{selected}"' in source[name], f"V08-SELECT-{name}", f"{name}: selected root item retained")

    # Screen-specific V1 contracts that are not part of the recorded v59 debt.
    check("AutoDriveSearchField(" in source["conversations"], "V08-CONV-SEARCH", "Conversations search field retained")
    check("ConversationItem(" in source["conversations"], "V08-CONV-ITEM", "Conversations item pattern retained")
    check("AutoDriveSnackbarContent(" in source["conversations"], "V08-CONV-SNACK", "Conversations governed error visual retained")
    check("DashboardHero(" in source["balance"], "V08-BAL-HERO", "Balance hero retained")
    check("PendingRequestCard(" in source["balance"], "V08-BAL-PENDING", "Balance pending card retained")
    check("TransactionPatternRow(" in source["balance"], "V08-BAL-TXN", "Balance transaction row retained")
    check("AutoDriveBottomSheet(" in withdrawal, "V08-BAL-SHEET", "Withdrawal V1 bottom sheet retained")
    check("SettingsGroup(" in source["settings"], "V08-SET-GROUP", "Settings group retained")
    for sym in ("AutoDriveTextField(", "AutoDriveNumericField(", "AutoDriveSelectionField(", "AutoDrivePrimaryButton("):
        check(sym in source["settings"], f"V08-SET-{sym.split('(')[0]}", f"Settings edit form retains {sym[:-1]}")
    check("AutoDriveMetricCard(" in source["reports"], "V08-REPORT-METRIC", "Reports passive metric card retained")
    check("AutoDriveDialog(" in source["new_chat"], "V08-CHAT-DIALOG", "New Chat V1 dialog retained")
    check("MediaActionGroup(" in source["new_chat"], "V08-CHAT-MEDIA", "New Chat media group retained")
    for sym in ("AutoDriveTextField(", "AutoDrivePrimaryButton(", "AutoDriveSecondaryButton("):
        check(sym in source["new_chat"], f"V08-CHAT-{sym.split('(')[0]}", f"New Chat retains {sym[:-1]}")
    check('text = "نافذة بنزين"' in home_support, "V08-HOME-IDENTITY", "Home insight identity retained")

    # Semantic unread wiring: named-argument syntax is accepted; no brittle one-line call string.
    nav = text("app/src/main/kotlin/com/autodrive/app/navigation/AppNavigation.kt")
    nav_vm = text("app/src/main/kotlin/com/autodrive/app/navigation/AppNavigationViewModel.kt")
    graphs = text("app/src/main/kotlin/com/autodrive/app/navigation/NavigationGraphs.kt")
    observer = text("feature/notifications/src/main/kotlin/com/autodrive/app/feature/notifications/data/UnreadMessagesObserver.kt")
    check("UnreadMessagesObserver" in nav_vm and "observeUnreadMessages" in nav_vm, "V08-UNREAD-COORD", "app shell unread coordinator retained")
    check("collectAsState(initial = 0)" in nav, "V08-UNREAD-COLLECT", "unread flow collected with initial zero")
    check("mainGraph(" in nav and re.search(r"unreadMessages\s*=\s*unreadMessages", nav) is not None, "V08-UNREAD-PASS", "unread count passed to mainGraph, including named arguments")
    check("internal fun NavGraphBuilder.mainGraph(" in graphs and "unreadMessages: Int" in graphs, "V08-UNREAD-RECEIVE", "mainGraph receives unreadMessages: Int")
    check(len(re.findall(r"unreadMessages\s*=\s*unreadMessages", graphs)) >= 4, "V08-UNREAD-FORWARD", "unread count forwarded to all four root destinations")
    check("ChatRepository" in observer and "SessionReader" in observer, "V08-UNREAD-OWNER", "unread source remains feature-owned")
    ds_src = "\n".join(p.read_text(encoding="utf-8") for p in (root/"core/designsystem/src/main/kotlin").rglob("*.kt"))
    check("UnreadMessagesObserver" not in ds_src and "ChatRepository" not in ds_src, "V08-UNREAD-DS", "Design System remains presentation-only")

    # Legacy call sites remain forbidden.
    prod_roots = [root/"app/src/main/kotlin", root/"feature"]
    prod = "\n".join(p.read_text(encoding="utf-8") for r in prod_roots for p in r.rglob("*.kt"))
    for legacy in ("AutoDriveBottomBar(", "BottomNavItem.", "SevenSegmentNumber("):
        check(legacy not in prod, f"V08-LEGACY-{legacy[:8]}", f"legacy production call absent: {legacy}")

    # Strict raw-style checks still apply to migrated surfaces without recorded v59 debt.
    strict = [(screens[n], source[n]) for n in ("conversations","balance","settings","new_chat")] + [("WithdrawalSheet.kt", withdrawal)]
    for rel, s in strict:
        for bad in (r"RoundedCornerShape\(", r"\bColor\(", r"\d+(?:\.\d+)?\.dp\b", r"\d+(?:\.\d+)?\.sp\b"):
            check(re.search(bad, s) is None, f"V08-RAW-{hashlib.sha1((rel+bad).encode()).hexdigest()[:10]}", f"no raw foundation styling in non-debt migrated surface: {rel} / {bad}")
        check("core.designsystem.components.*" not in s, f"V08-WILDCARD-C-{hashlib.sha1(rel.encode()).hexdigest()[:8]}", f"no legacy DS component wildcard: {rel}")
        check("core.designsystem.theme.*" not in s, f"V08-WILDCARD-T-{hashlib.sha1(rel.encode()).hexdigest()[:8]}", f"no legacy theme wildcard: {rel}")

    # Critical interactions; current syntax may be callable references or lambda calls.
    required = {
        "home": ["refresh", "onPumpTapped", "onPumpAnimationComplete", "refreshDynamoMessage"],
        "conversations": ["onSearchQuery", "refresh", "clearError", "openOrCreateConversation"],
        "settings": ["startEditing", "cancelEditing", "saveAccount", "savePayout", "saveWorkshop", "setWeeklyTarget", "signOut"],
        "balance": ["openWithdrawSheet", "onAmountChange", "onNoteChange", "submitWithdrawal", "cancelAllPending"],
    }
    for name, methods in required.items():
        for method in methods:
            check(re.search(rf"viewModel(?:::|\.){re.escape(method)}\b", source[name]) is not None, f"V08-INTERACT-{name}-{method}", f"{name}: current interaction retained: {method}")
    for needle in ("viewModel.createAndSend", "ActivityResultContracts.TakePicture", "ActivityResultContracts.GetContent", "ActivityResultContracts.RequestPermission"):
        check(needle in source["new_chat"], f"V08-CHAT-ACTION-{hashlib.sha1(needle.encode()).hexdigest()[:8]}", f"New Chat action retained: {needle}")
    for needle in ("onNavigateBalance", "onNavigateInvoiceList", "onNavigateWinWeeks", "onNavigateWeeklyCommissions", "onNavigateCompetitionHistory"):
        check(needle in source["reports"], f"V08-REPORT-NAV-{needle}", f"Reports navigation retained: {needle}")

    # Rough structure sanity for relevant Kotlin source.
    def strip_kotlin(s: str) -> str:
        s = re.sub(r'/\*.*?\*/', '', s, flags=re.S); s = re.sub(r'//.*', '', s)
        s = re.sub(r'""".*?"""', '""', s, flags=re.S); s = re.sub(r'"(?:\\.|[^"\\])*"', '""', s)
        s = re.sub(r"'(?:\\.|[^'\\])'", "''", s); return s
    structural = list(screens.values()) + [
        "feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/WithdrawalSheet.kt",
        "app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeSupportCards.kt",
        "app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeHeroComponents.kt",
        "app/src/main/kotlin/com/autodrive/app/navigation/AppNavigation.kt",
        "app/src/main/kotlin/com/autodrive/app/navigation/AppNavigationViewModel.kt",
        "app/src/main/kotlin/com/autodrive/app/navigation/NavigationGraphs.kt",
    ]
    for rel in structural:
        s = strip_kotlin(text(rel))
        for a,b,label in (("{","}","braces"),("(",")","parentheses"),("[","]","brackets")):
            depth=0; ok=True
            for ch in s:
                if ch==a: depth+=1
                elif ch==b:
                    depth-=1
                    if depth<0: ok=False; break
            check(ok and depth==0, f"V08-STRUCT-{hashlib.sha1((rel+label).encode()).hexdigest()[:10]}", f"balanced {label}: {rel}")

    payload = {
        "schemaVersion": 66 if accepted_version == "v66" else (65 if accepted_version == "v65" else (64 if accepted_version == "v64" else 61)),
        "tool": "verify_designsystem_v08",
        "mode": "post-v66 zero-drift bridge" if accepted_version == "v66" else ("post-v65 static-accessibility bridge" if accepted_version == "v65" else ("post-v64 component-adoption bridge" if accepted_version == "v64" else "post-v63 historical/open debt bridge")),
        "acceptedVersion": accepted_version,
        "hardInvariantChecks": checks,
        "hardInvariantFailures": errors,
        "baselineDebt": {
            "historicalConfirmed": 77,
            "historicalHomeIds": sorted(HISTORICAL_HOME_DEBT_IDS),
            "currentAcceptedOpenConfirmed": len(state.get("confirmedFindings", [])),
            "openPostV61Ids": sorted(OPEN_POST_V61_DEBT_IDS),
            "reportsResolvedAtV62Ids": sorted(REPORTS_DEBT_IDS),
            "settingsOpenIds": sorted(SETTINGS_DEBT_IDS if accepted_version not in {"v63", "v64", "v65", "v66"} else set()),
            "componentAdoptionV64": "COMPLETE" if accepted_version in {"v64", "v65", "v66"} else "PENDING",
            "accessibilityV65": "COMPLETE" if accepted_version == "v66" else ("STATIC_REPAIRED_RUNTIME_BLOCKED" if accepted_version == "v65" else "PENDING"),
            "runtimeAccessibilityVerified": True if accepted_version == "v66" else (False if accepted_version == "v65" else None),
            "delegatedTo": "tools/verify_designsystem_ratchet.py",
        },
        "homeMigration": "HOME_V1",
        "staleSyntaxCorrections": ["unread named-argument call accepted", "startEditing lambda-call syntax accepted", "saveProfile replaced by current saveAccount/savePayout/saveWorkshop contract"],
        "verdict": "PASS" if not errors else "FAIL_V08_COMPATIBILITY_BRIDGE",
    }
    return (0 if not errors else 1), payload


def main(argv: list[str] | None = None) -> int:
    parser=argparse.ArgumentParser(); parser.add_argument("--root",type=Path,default=ROOT); parser.add_argument("--json",action="store_true",dest="json_mode")
    args=parser.parse_args(argv)
    try:
        code,payload=run(args.root.resolve())
    except (OSError, json.JSONDecodeError, UnicodeError, ValueError) as exc:
        code=2; payload={"schemaVersion":61,"tool":"verify_designsystem_v08","errors":[f"{type(exc).__name__}: {exc}"],"verdict":"TOOL_ERROR"}
    if args.json_mode: print(json.dumps(payload,ensure_ascii=False,sort_keys=True))
    else:
        print(f"V08 SCREEN MIGRATION STATIC VERIFICATION: {payload['verdict']}")
        if code==0:
            print(" - hard invariants: PASS")
            print(" - historical v59 baseline: 77 confirmed findings")
            print(f" - current accepted open confirmed: {payload.get('baselineDebt', {}).get('currentAcceptedOpenConfirmed')}")
            if payload.get("acceptedVersion") == "v65": print(" - Home/Reports/Settings migrated; Component adoption v64 complete; static accessibility v65 repaired; runtime BLOCKED; v66 pending")
            elif payload.get("acceptedVersion") == "v64": print(" - Home/Reports/Settings migrated; Component adoption v64 complete; v65 accessibility audit pending")
            elif payload.get("acceptedVersion") == "v63": print(" - Home migrated under HOME_V1; Reports migrated under REPORTS_V1; Settings migrated under SETTINGS_V1")
            else: print(" - Home migrated under HOME_V1; Reports migrated under REPORTS_V1; Settings remains delegated")
        else:
            for e in payload.get("hardInvariantFailures",payload.get("errors",[])): print(" -",e)
    return code

if __name__ == "__main__":
    try: raise SystemExit(main())
    except KeyboardInterrupt: raise
    except Exception as exc:
        print(f"V08 SCREEN MIGRATION STATIC VERIFICATION: TOOL_ERROR\n - {type(exc).__name__}: {exc}")
        raise SystemExit(2)
