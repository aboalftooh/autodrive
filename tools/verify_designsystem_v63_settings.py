#!/usr/bin/env python3
from __future__ import annotations
import argparse, csv, hashlib, json, re
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
TARGET_ID = "DS59-SETTINGS-001"
SETTINGS = "core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/settings/SettingsPatterns.kt"
DATA = "core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/data/DataComponents.kt"
PROFILE = "feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt"
ALLOW_PROD = {SETTINGS, DATA}
PRE_HASHES = {
    SETTINGS: "09175af564c30693a1f78b1f2025b80e7a068a62d7181487469a1b6aae9c4eb5",
    DATA: "102703f69a4b7bac0cc08a7561630f325bfee0171e15166f8d4487577537db99",
}
PROTECTED = {
    "DESIGN_SYSTEM_BASELINE_v59.md":"f6b59f0e09c58262a19438462da309a6739d9abe0e861c715fb21f8971a49438",
    "DESIGN_SYSTEM_BASELINE_v59.json":"906c12831752daa2f7fce190a3aab37a75817c415b055c09ab367d8207248abc",
    "DESIGN_SYSTEM_UI_COVERAGE_v59.csv":"191d4497d0433ba078fb7b71bee080763cd013cc64f34ab3220477649d568dea",
    "DESIGN_SYSTEM_V1_CONTRACT_REGISTRY_v59.md":"ed758157e064bb1f8fc61d13e2efdd4b09938ec54640f49d603e0df18a215e0d",
    "DESIGN_SYSTEM_UI_COVERAGE_v62.csv":"a65b960345c697cdecf5cca9e9b5e0b1b5405f4fc313a9561fca15c77a56afd8",
    "DESIGN_SYSTEM_VERIFICATION_v62.json":"0ddb3c906091c9b609fdbb47edc6d36fa375e8ee75d49dd1737ea3f5c52e5753",
    "DESIGN_SYSTEM_VERIFICATION_v62.md":"00cc515935fbdd95c36a998ca4c7ef56e8de610fa839e7fed26e11c3c8290ad8",
    "AutoDrive-v62-report.md":"0989a0742739860ce6e1b23745bf0559046f107c96f463433e359adc7954a98e",
    "tools/verify_designsystem_v62_reports.py":"8168ab0fe413a99cfa3b9147dec29125eaa60c05679887ab0b2f578f74e39524",
    "tools/test_designsystem_verification_v62.py":"94bca31f42e17ed796ec816aea71b8e54046cf2f516d0bd3323be197b1fd2372",
    "scripts/verify-v62-static.sh":"249afe3cbd307a8af45987d45f4c7023bcb1513e9992f44e88bfc7f124bf32ec",
    PROFILE:"ca0b1e33ad406452d5f4511df85e244125bf19113f953fa50261cbf1a27fb198",
    "feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileViewModel.kt":"12fad5fe0cf7b764b6c8c40a5d5b87fbe3cc8b68cbbd4726d8895bd81594c2e5",
    "feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileUiState.kt":"ca9dcaf869186a31a877ac4881e1b985f734684c97b79819a3be4c03606b4edb",
    "feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/data/ProfileRepositoryImpl.kt":"762fe6ab43fa59df544fcc9acc476cdb33bb3f22f0271aa18e98fe9f01f8ed97",
    "app/src/main/kotlin/com/autodrive/app/navigation/NavigationGraphs.kt":"950c3dcfd884fe10481d0bb7fe5179bc85b8f20253d998e8a670d678661074cc",
    "core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/ComponentSemantics.kt":"233531d130c0dcf50d8f2ed7def6f2b1a7cdc0c341827840e7a2a6239aa69dbc",
    "core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/foundation/color/ColorTokens.kt":"a70932b660ab2c97b04ccd26dc6ef55b4107b960fff827f0b51fa4a1ba1cf4d4",
    "core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/preview/V1PatternPreviews.kt":"988bd65202e4c52b78a89b4b2568e322fdf9d3ad442e1ee196346c2d7169075c",
    "docs/design-system/04_PATTERNS.md":"1569313eeda3a9c874f7a3da6fb9874bc51a5b97490c07ed06457b944f4cca6c",
    "docs/design-system/03_COMPONENT_SPEC.md":"3da0eeadedc3ff71b728a75431bc730692e1dbe650cb2e21afa8da8c29f81459",
    "docs/design-system/02_FOUNDATIONS.md":"23b275a7aff3a96757e5a7db9bccd493ee8e7e9ad396eda3250d7fdde7a2017c",
}
PROTECTED_PROD_DIGEST = "6c7ad8efdd6326726b6766287f7e77b2c11eb0cbdb784f3cb730fdb821e75831"
PROTECTED_DS_DIGEST = "4a7a96909146a115664a0e68323a2634c8941987e6a7403434123076a093bfd5"


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def digest_rows(rows: list[str]) -> str:
    return hashlib.sha256("\n".join(rows).encode()).hexdigest()


def production_files(root: Path) -> list[Path]:
    return sorted(p for p in root.rglob("*.kt") if "/src/main/kotlin/" in "/" + p.relative_to(root).as_posix() and "/build/" not in "/" + p.relative_to(root).as_posix())


def protected_prod_digest(root: Path) -> tuple[int,str]:
    ps=[p for p in production_files(root) if p.relative_to(root).as_posix() not in ALLOW_PROD]
    return len(ps), digest_rows([f"{p.relative_to(root).as_posix()}\t{sha(p)}" for p in ps])


def protected_ds_digest(root: Path) -> tuple[int,str]:
    base=root/"core/designsystem/src/main"
    ps=sorted(p for p in base.rglob("*") if p.is_file() and p.relative_to(root).as_posix() not in ALLOW_PROD)
    return len(ps), digest_rows([f"{p.relative_to(base).as_posix()}\t{sha(p)}" for p in ps])


def source_contract_errors(settings: str, data: str) -> list[str]:
    e=[]
    # Typed API and backward compatible default.
    if not re.search(r"titleTone\s*:\s*AutoDriveStatusTone\?\s*=\s*null", data): e.append("API_TYPED_TITLE_TONE")
    if re.search(r"titleColor\s*:\s*Color", data): e.append("RAW_COLOR_ESCAPE")
    # Functional resolution: disabled must win before tone.
    if not re.search(r"val\s+titleColor\s*=\s*if\s*\(\s*!enabled\s*\)\s*AutoDriveText\.Disabled\s*else\s*titleTone\?\.color\(\)\s*\?:\s*AutoDriveText\.Primary", data): e.append("DISABLED_PRECEDENCE")
    if not re.search(r"Text\(\s*title\s*,[^\n]*color\s*=\s*titleColor", data): e.append("TITLE_TONE_UNUSED")
    if "androidx.compose.ui.semantics.disabled" not in data or not re.search(r"if\s*\(\s*!enabled\s*\)\s*Modifier\.semantics\s*\{\s*disabled\(\)\s*\}", data, re.S): e.append("DISABLED_SEMANTICS")
    if "if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier" not in data: e.append("CLICK_GUARD")
    # Settings row must forward semantic Error tone, not a raw/local color.
    if not re.search(r"titleTone\s*=\s*if\s*\(\s*variant\s*==\s*SettingsRowVariant\.Destructive\s*\)\s*AutoDriveStatusTone\.Error\s*else\s*null", settings): e.append("DESTRUCTIVE_FORWARD")
    if re.search(r"val\s+titleColor\b", settings): e.append("STALE_TITLE_COLOR")
    if re.search(r"(?<!AutoDrive)\bRow\s*\(", settings): e.append("LOCAL_ROW_CLONE")
    if "Icons.AutoMirrored.Rounded.KeyboardArrowLeft" not in settings: e.append("RTL_ICON")
    if not re.search(r"tint\s*=\s*if\s*\(enabled\)\s*AutoDriveText\.Secondary\s*else\s*AutoDriveText\.Disabled", settings): e.append("DISABLED_TRAILING_ICON")
    if not re.search(r"Text\(value,\s*style\s*=\s*MaterialTheme\.typography\.bodyMedium,\s*color\s*=\s*if\s*\(enabled\)\s*AutoDriveText\.Secondary\s*else\s*AutoDriveText\.Disabled\)", settings): e.append("DISABLED_VALUE")
    if "SettingsRowVariant.Status -> if (value != null) AutoDriveStatusChip(value, statusTone)" not in settings: e.append("STATUS_CHIP")
    if "if (index != items.lastIndex) AutoDriveDivider()" not in settings: e.append("DIVIDER_RULE")
    if "SettingsRowVariant.Toggle" in settings or "Switch(" in settings: e.append("TOGGLE_SCOPE")
    if "onItemClick: (String) -> Unit" not in settings or "onClick = { onItemClick(item.id) }" not in settings: e.append("CALLBACK_OWNERSHIP")
    return sorted(set(e))


def state_contract_errors(state: dict[str,Any]) -> list[str]:
    e=[]; av=state.get("acceptedVersion")
    ids={x.get("findingId") for x in state.get("confirmedFindings",[])}
    counts=Counter(x.get("rule_id") for x in state.get("confirmedFindings",[]))
    if av=="v62":
        if len(ids)!=47 or TARGET_ID not in ids: e.append("RATCHET_PRE")
    elif av=="v63":
        if len(ids)!=46 or TARGET_ID in ids: e.append("RATCHET_POST")
        hist=state.get("history",[])
        if not hist or hist[-1].get("acceptedVersion")!="v63" or hist[-1].get("resolvedFindingIds")!=[TARGET_ID]: e.append("RATCHET_HISTORY")
    else: e.append("RATCHET_VERSION")
    if counts.get("DS-A11Y-001",0)!=3: e.append("A11Y_DEBT_MUTATION")
    if counts.get("DS-MATERIAL-001",0)!=43: e.append("MATERIAL_DEBT_MUTATION")
    expected_contract=1 if av=="v62" else 0
    if counts.get("DS-CONTRACT-001",0)!=expected_contract: e.append("CONTRACT_TOTAL")
    if len(state.get("acceptedCandidates",[]))!=18: e.append("CANDIDATE_WASHING")
    return sorted(set(e))


def profile_behavior_errors(profile: str) -> list[str]:
    required=[
        '"تسجيل الخروج"',
        'viewModel::requestSignOut','showSignOutConfirmDialog','viewModel::dismissSignOutDialog','viewModel::signOut',
        'AutoDriveDialogTone.Destructive','AutoDriveTextButtonTone.Destructive',
        'user.accountType == AccountType.WORKSHOP_OWNER','startEditing(ProfileEditSection.WEEKLY_TARGET)',
        'selectedItemId = "settings"','onNavigateAbout()','onNavigatePrivacy()','onNavigateFaq()',
    ]
    return [f"PROFILE_BEHAVIOR:{x}" for x in required if x not in profile]


def run(root: Path) -> tuple[int,dict[str,Any]]:
    errors=[]; checks=[]
    def ck(cid: str, ok: bool, msg: str):
        checks.append({"id":cid,"status":"PASS" if ok else "FAIL","message":msg})
        if not ok: errors.append(f"{cid}: {msg}")
    settings=(root/SETTINGS).read_text(encoding="utf-8"); data=(root/DATA).read_text(encoding="utf-8"); profile=(root/PROFILE).read_text(encoding="utf-8")
    for x in source_contract_errors(settings,data): ck("SETTINGS-CONTRACT",False,x)
    if not source_contract_errors(settings,data): ck("SETTINGS-CONTRACT",True,"typed title tone, disabled precedence/semantics, trailing state and no duplicate/raw-color workaround")
    for x in profile_behavior_errors(profile): ck("PROFILE-BEHAVIOR",False,x)
    if not profile_behavior_errors(profile): ck("PROFILE-BEHAVIOR",True,"SETTINGS_V1 business/navigation/sign-out behavior retained")
    # immutable authorities/evidence/protected production
    for rel,expected in PROTECTED.items():
        p=root/rel; ck("PROTECTED-HASH", p.is_file() and sha(p)==expected, rel)
    pc,pd=protected_prod_digest(root); ck("PROTECTED-PRODUCTION",pc==249 and pd==PROTECTED_PROD_DIGEST,f"249 protected production Kotlin digest={pd}")
    dc,dd=protected_ds_digest(root); ck("PROTECTED-DS",dc==30 and dd==PROTECTED_DS_DIGEST,f"30 protected DS-main digest={dd}")
    # v62 coverage authority and exact two v63 rows.
    with (root/"DESIGN_SYSTEM_UI_COVERAGE_v62.csv").open(newline="",encoding="utf-8") as f: rows=list(csv.DictReader(f))
    target=[r for r in rows if r.get("target_session")=="v63"]
    ck("COVERAGE-V62",len(target)==2 and {r.get("full_relative_path") for r in target}=={SETTINGS,PROFILE},"exact two v63 rows from immutable v62 coverage")
    baseline=json.loads((root/"DESIGN_SYSTEM_BASELINE_v59.json").read_text(encoding="utf-8"))
    target_ids={x.get("finding_id") for x in baseline.get("findings",[]) if x.get("repair_session")=="v63" and x.get("classification")=="CONFIRMED_VIOLATION"}
    ck("TARGET-ID",target_ids=={TARGET_ID},"exact v63 target finding set")
    state=json.loads((root/"core/designsystem/verification/designsystem-ratchet-state.json").read_text(encoding="utf-8"))
    for x in state_contract_errors(state): ck("STATE",False,x)
    if not state_contract_errors(state): ck("STATE",True,f"acceptedVersion={state.get('acceptedVersion')} totals/candidates/debt boundaries valid")
    # mutation identity pre/post acceptance.
    accepted={x["path"]:x["sha256"] for x in state.get("productionFiles",[]) if isinstance(x,dict) and "path" in x}
    current={p.relative_to(root).as_posix():sha(p) for p in production_files(root)}
    changed=sorted(p for p in set(accepted)&set(current) if accepted[p]!=current[p])
    added=sorted(set(current)-set(accepted)); removed=sorted(set(accepted)-set(current))
    if state.get("acceptedVersion")=="v62": ck("MUTATION-SCOPE",set(changed)==ALLOW_PROD and not added and not removed,f"pre-accept changed production={changed}")
    else: ck("MUTATION-SCOPE",not changed and not added and not removed,"post-accept production equals v63 state")
    cov63=root/"DESIGN_SYSTEM_UI_COVERAGE_v63.csv"
    if cov63.is_file():
        with cov63.open(newline="",encoding="utf-8") as f: vr=list(csv.DictReader(f))
        t=[r for r in vr if r.get("target_session")=="v63"]
        ck("COVERAGE-V63",len(t)==2 and all(r.get("v63_open_confirmed")=="0" and r.get("v63_new_candidates")=="0" and r.get("v63_status") in {"MIGRATED_V63","VERIFIED_CLEAN_CARRY_FORWARD"} for r in t),"Settings rows closed 2/2")
    payload={
        "schemaVersion":63,"session":63,"tool":"verify_designsystem_v63_settings","targetResolvedIds":[TARGET_ID],
        "settingsRows":2,"apiGap":"AutoDriveListRow lacked typed title tone input","apiExtension":"titleTone: AutoDriveStatusTone? = null",
        "changedProductionFiles":changed,"protectedProductionDigest":pd,"protectedDsDigest":dd,
        "contractChecks":checks,"interactionChecks":[x for x in checks if x["id"] in {"SETTINGS-CONTRACT","PROFILE-BEHAVIOR"}],
        "disabledChecks":[x for x in checks if x["id"]=="SETTINGS-CONTRACT"],"destructiveChecks":[x for x in checks if x["id"]=="SETTINGS-CONTRACT"],
        "errors":errors,"verdict":"PASS" if not errors else "FAIL_SETTINGS_V63_CONTRACT",
    }
    return (0 if not errors else 1),payload


def main(argv=None)->int:
    ap=argparse.ArgumentParser(); ap.add_argument("--root",type=Path,default=ROOT); ap.add_argument("--output",type=Path); ap.add_argument("--json",action="store_true"); a=ap.parse_args(argv)
    try: code,p=run(a.root.resolve())
    except (OSError,UnicodeError,ValueError,json.JSONDecodeError,csv.Error) as exc: code=2; p={"schemaVersion":63,"session":63,"tool":"verify_designsystem_v63_settings","errors":[f"{type(exc).__name__}: {exc}"],"verdict":"TOOL_ERROR"}
    if a.output:
        out=a.output if a.output.is_absolute() else a.root.resolve()/a.output; out.parent.mkdir(parents=True,exist_ok=True); out.write_text(json.dumps(p,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    if a.json: print(json.dumps(p,ensure_ascii=False,sort_keys=True))
    else:
        print(f"V63 SETTINGS STATIC VERIFICATION: {p['verdict']}")
        if code==0: print(" - exact target IDs: 1\n - Settings rows: 2\n - protected hashes/digests: PASS\n - typed titleTone + disabled semantics: PASS")
        else:
            for x in p.get("errors",[]): print(" -",x)
    return code
if __name__=="__main__": raise SystemExit(main())
