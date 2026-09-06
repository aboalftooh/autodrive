#!/usr/bin/env python3
from __future__ import annotations
import argparse, hashlib, json, re, sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
HOME_FILES = [
    "app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeScreen.kt",
    "app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeHeroComponents.kt",
    "app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeSupportCards.kt",
]
PROTECTED = {
    "app/src/main/kotlin/com/autodrive/app/feature/home/presentation/RealtimeStatusBar.kt": "a397ff55159c1d86041ded0a1f3513bde47cb1ab9e5b7b5ae84c48bbe8420778",
    "app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeViewModel.kt": "0f5f70ce541b544be3352943773ebddc3677f12c576fd6b57793f7b2d314f002",
    "app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeUiState.kt": "3b9e199a44d3c213fb782b80a459df308d9c985f9d8a723f2f08e33469466ef1",
    "app/src/main/kotlin/com/autodrive/app/feature/home/presentation/DynamoStateUiMapper.kt": "39fb7f73c3ea163c9874375a0186a88a11ccf0c2c7aa45df880178f4409200d7",
}
RESOLVED_IDS = sorted([
    "DS59-COLOR-HOMEHEROCOMPONENTS", "DS59-COLOR-HOMESCREEN", "DS59-COLOR-HOMESUPPORTCARDS",
    "DS59-HOME-001", "DS59-HOME-002", "DS59-HOME-003", "DS59-HOME-004", "DS59-HOME-005", "DS59-HOME-006", "DS59-HOME-007", "DS59-HOME-008",
    "DS59-SHAPE-HOMEHEROCOMPONENTS", "DS59-SHAPE-HOMESCREEN", "DS59-SHAPE-HOMESUPPORTCARDS",
    "DS59-SPACE-HOMEHEROCOMPONENTS", "DS59-SPACE-HOMESCREEN", "DS59-SPACE-HOMESUPPORTCARDS",
    "DS59-TYPE-HOMEHEROCOMPONENTS", "DS59-TYPE-HOMESCREEN", "DS59-TYPE-HOMESUPPORTCARDS",
])

def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()

def run(root: Path) -> tuple[int, dict[str, Any]]:
    checks: list[dict[str, str]] = []
    errors: list[str] = []
    def check(cid: str, ok: bool, message: str) -> None:
        checks.append({"id": cid, "status": "PASS" if ok else "FAIL", "message": message})
        if not ok: errors.append(f"{cid}: {message}")
    src = {}
    for rel in HOME_FILES:
        p = root / rel
        if not p.is_file():
            check("HOME-FILE", False, f"missing {rel}")
            src[rel] = ""
        else: src[rel] = p.read_text(encoding="utf-8")
    screen, hero, support = (src[x] for x in HOME_FILES)
    combined = "\n".join(src.values())

    check("HOME-CONTRACT-HEADER", re.search(r"(?<![A-Za-z0-9_])ScreenHeader\s*\(", screen) is not None, "ScreenHeader owns Home header")
    check("HOME-CONTRACT-NO-LOCAL-HEADER", re.search(r"\bfun\s+HomeHeader\s*\(", screen) is None and "HomeHeader(" not in screen, "local HomeHeader responsibility absent")
    check("HOME-CONTRACT-HERO", re.search(r"(?<![A-Za-z0-9_])DashboardHero\s*\(", hero) is not None, "DashboardHero owns pump hero container")
    check("HOME-CONTRACT-INSTRUMENT", re.search(r"(?<![A-Za-z0-9_])AutoDriveInstrumentNumber\s*\(", hero) is not None and "LargeLedNumber" not in hero and "LargeLedDigit" not in hero and "LARGE_LED_SEGMENTS" not in hero, "official instrument number replaces local LED renderer")
    check("HOME-CONTRACT-WIDTH", "AutoDriveContentWidth.Dashboard" in screen and ".widthIn(" in screen, "Dashboard max-width applied functionally")

    check("HOME-STYLE-COLOR", re.search(r"(?<![A-Za-z0-9_.])Color\s*\(", combined) is None, "no raw Color ownership")
    check("HOME-STYLE-SP", re.search(r"\b\d+(?:\.\d+)?\.sp\b", combined) is None, "no direct raw typography")
    check("HOME-STYLE-SHAPE", "RoundedCornerShape(" not in combined, "no repeated raw shape ownership")
    check("HOME-STYLE-SPACING", re.search(r"\b\d+(?:\.\d+)?\.dp\b", combined) is None, "Home shared spacing is token-governed")
    check("HOME-STYLE-BORDER", re.search(r"\.border\s*\([^)]*\d+(?:\.\d+)?\.dp", combined) is None, "no raw repeated border decision")

    behavior = {
        "HOME-BEHAVIOR-RESUME": "refreshDynamoMessage()" in screen and "Lifecycle.Event.ON_RESUME" in screen,
        "HOME-BEHAVIOR-REFRESH": "onRefresh = viewModel::refresh" in screen,
        "HOME-BEHAVIOR-PUMP-START": "onPump = viewModel::onPumpTapped" in screen,
        "HOME-BEHAVIOR-PUMP-COMPLETE": "onPumpAnimationComplete = viewModel::onPumpAnimationComplete" in screen,
        "HOME-BEHAVIOR-PUMP-EARLY": "state.syncedTotal <= state.displayedTotal" in hero,
        "HOME-BEHAVIOR-PUMP-DURATION": "coerceIn(800L, 2500L)" in hero,
        "HOME-BEHAVIOR-PUMP-SOUND": "BenzineSound.playPumpFill" in hero and "BenzineSound.playTankFull" in hero,
        "HOME-BEHAVIOR-COUNTDOWN": "state.nextFriday9AmMs - System.currentTimeMillis()" in hero and "delay(1000)" in hero,
        "HOME-BEHAVIOR-DISABLED": "competitionAvailability != CompetitionAvailability.DISABLED" in screen,
        "HOME-BEHAVIOR-LOCKED": "CompetitionAvailability.LOCKED" in screen and '"قريباً"' in screen,
        "HOME-BEHAVIOR-ACTIVE": '"تحقق من مركزك هذا الأسبوع"' in screen and "onClick = onNavigateCompetition" in screen,
        "HOME-BEHAVIOR-RECENT": '"messages" -> onNavigateRecent()' in screen,
        "HOME-BEHAVIOR-LOG": '"reports" -> onNavigateLog(null)' in screen,
        "HOME-BEHAVIOR-PROFILE": '"settings" -> onNavigateProfile()' in screen,
        "HOME-BEHAVIOR-NOTIFICATIONS": "onClick = onNavigateNotifications" in screen,
        "HOME-BEHAVIOR-FAB": "onClick = onAddClick" in screen and 'contentDescription = "محادثة جديدة"' in screen,
        "HOME-BEHAVIOR-INSIGHT": 'text = "نافذة بنزين"' in support and '"جاري تحميل النصائح..."' in support,
    }
    for cid, ok in behavior.items(): check(cid, ok, "behavior contract retained")

    for rel, expected in PROTECTED.items():
        p = root / rel
        check("HOME-PROTECTED-" + Path(rel).stem.upper(), p.is_file() and sha(p) == expected, f"protected hash unchanged: {rel}")

    payload = {
        "schemaVersion": 61,
        "session": 61,
        "tool": "verify_designsystem_v61_home",
        "sourceHashes": {rel: sha(root/rel) for rel in HOME_FILES if (root/rel).is_file()},
        "contractChecks": [x for x in checks if x["id"].startswith("HOME-CONTRACT")],
        "behaviorChecks": [x for x in checks if x["id"].startswith("HOME-BEHAVIOR")],
        "styleChecks": [x for x in checks if x["id"].startswith("HOME-STYLE")],
        "protectedChecks": [x for x in checks if x["id"].startswith("HOME-PROTECTED")],
        "resolvedBaselineIds": RESOLVED_IDS,
        "remainingHomeFindings": [] if not errors else errors,
        "errors": errors,
        "verdict": "PASS" if not errors else "FAIL_HOME_V61_CONTRACT",
    }
    return (0 if not errors else 1), payload

def main(argv=None) -> int:
    ap=argparse.ArgumentParser(); ap.add_argument("--root",type=Path,default=ROOT); ap.add_argument("--output",type=Path); ap.add_argument("--json",action="store_true")
    args=ap.parse_args(argv)
    try: code,payload=run(args.root.resolve())
    except (OSError,UnicodeError,ValueError) as exc:
        code=2; payload={"schemaVersion":61,"session":61,"tool":"verify_designsystem_v61_home","errors":[f"{type(exc).__name__}: {exc}"],"verdict":"TOOL_ERROR"}
    if args.output:
        out=args.output if args.output.is_absolute() else args.root.resolve()/args.output; out.write_text(json.dumps(payload,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    if args.json: print(json.dumps(payload,ensure_ascii=False,sort_keys=True))
    else:
        print(f"V61 HOME STATIC VERIFICATION: {payload['verdict']}")
        print(f" - resolved baseline IDs: {len(payload.get('resolvedBaselineIds',[]))}")
        for e in payload.get('errors',[]): print(' -',e)
    return code
if __name__ == '__main__':
    try: raise SystemExit(main())
    except KeyboardInterrupt: raise
    except Exception as exc:
        print(f"V61 HOME STATIC VERIFICATION: TOOL_ERROR\n - {type(exc).__name__}: {exc}")
        raise SystemExit(2)
