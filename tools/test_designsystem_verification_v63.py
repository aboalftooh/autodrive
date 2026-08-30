#!/usr/bin/env python3
from __future__ import annotations
import argparse, copy, importlib.util, json, tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SPEC=importlib.util.spec_from_file_location("v63",ROOT/"tools/verify_designsystem_v63_settings.py"); V=importlib.util.module_from_spec(SPEC); SPEC.loader.exec_module(V)

def main(argv=None):
    ap=argparse.ArgumentParser(); ap.add_argument("--root",type=Path,default=ROOT); ap.add_argument("--output",type=Path); a=ap.parse_args(argv); root=a.root.resolve()
    settings=(root/V.SETTINGS).read_text(); data=(root/V.DATA).read_text()
    # Build canonical post-state fixture snippets independently of production mutation.
    good_data=data.replace("    supportingText: String? = null,", "    supportingText: String? = null,\n    titleTone: AutoDriveStatusTone? = null,")
    good_data=good_data.replace("import androidx.compose.ui.semantics.contentDescription", "import androidx.compose.ui.semantics.contentDescription\nimport androidx.compose.ui.semantics.disabled")
    good_data=good_data.replace("    val contentColor = if (enabled) AutoDriveText.Primary else AutoDriveText.Disabled", "    val titleColor = if (!enabled) AutoDriveText.Disabled else titleTone?.color() ?: AutoDriveText.Primary")
    good_data=good_data.replace("            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)", "            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)\n            .then(if (!enabled) Modifier.semantics { disabled() } else Modifier)")
    good_data=good_data.replace("Text(title, style = MaterialTheme.typography.titleMedium, color = contentColor)", "Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor)")
    good_settings=settings.replace("import com.autodrive.app.core.designsystem.foundation.color.AutoDriveStatus\n", "")
    good_settings=good_settings.replace("    val titleColor = if (variant == SettingsRowVariant.Destructive) AutoDriveStatus.Error else if (enabled) AutoDriveText.Primary else AutoDriveText.Disabled\n", "")
    good_settings=good_settings.replace("        supportingText = null,", "        supportingText = null,\n        titleTone = if (variant == SettingsRowVariant.Destructive) AutoDriveStatusTone.Error else null,")
    good_settings=good_settings.replace("tint = AutoDriveText.Secondary", "tint = if (enabled) AutoDriveText.Secondary else AutoDriveText.Disabled")
    good_settings=good_settings.replace("color = AutoDriveText.Secondary)", "color = if (enabled) AutoDriveText.Secondary else AutoDriveText.Disabled)")
    cases=[]
    def expect(name,s,d,needle=None):
        errs=V.source_contract_errors(s,d); ok=(not errs if needle is None else needle in errs); cases.append({"name":name,"status":"PASS" if ok else "FAIL","errors":errs,"expected":needle or "clean"})
    expect("positive typed titleTone default null",good_settings,good_data)
    expect("negative remove titleTone forwarding",good_settings.replace("titleTone = if (variant == SettingsRowVariant.Destructive) AutoDriveStatusTone.Error else null,",""),good_data,"DESTRUCTIVE_FORWARD")
    bad_unused=good_settings.replace("        titleTone = if (variant == SettingsRowVariant.Destructive) AutoDriveStatusTone.Error else null,\n", "").replace("    AutoDriveListRow(\n", "    val titleColor = if (variant == SettingsRowVariant.Destructive) AutoDriveText.Disabled else AutoDriveText.Primary\n    AutoDriveListRow(\n", 1)
    expect("negative computed color unused",bad_unused,good_data,"DESTRUCTIVE_FORWARD")
    expect("negative raw Color parameter",good_settings,good_data.replace("titleTone: AutoDriveStatusTone? = null,","titleTone: AutoDriveStatusTone? = null,\n    titleColor: Color = Color.Unspecified,"),"RAW_COLOR_ESCAPE")
    expect("negative Error wins over disabled",good_settings,good_data.replace("if (!enabled) AutoDriveText.Disabled else titleTone?.color() ?: AutoDriveText.Primary","if (titleTone != null) titleTone.color() else if (!enabled) AutoDriveText.Disabled else AutoDriveText.Primary"),"DISABLED_PRECEDENCE")
    expect("negative local Row clone",good_settings.replace("AutoDriveListRow(","Row("),good_data,"LOCAL_ROW_CLONE")
    expect("negative remove disabled semantics",good_settings,good_data.replace("            .then(if (!enabled) Modifier.semantics { disabled() } else Modifier)\n",""),"DISABLED_SEMANTICS")
    expect("negative callback when disabled",good_settings,good_data.replace("if (onClick != null && enabled)","if (onClick != null)"),"CLICK_GUARD")
    expect("negative non mirrored icon",good_settings.replace("Icons.AutoMirrored.Rounded.KeyboardArrowLeft","Icons.Rounded.KeyboardArrowLeft"),good_data,"RTL_ICON")
    expect("negative divider after final row",good_settings.replace("if (index != items.lastIndex) AutoDriveDivider()","AutoDriveDivider()"),good_data,"DIVIDER_RULE")
    expect("negative remove status chip",good_settings.replace("SettingsRowVariant.Status -> if (value != null) AutoDriveStatusChip(value, statusTone)","SettingsRowVariant.Status -> Unit"),good_data,"STATUS_CHIP")
    # state-policy fixtures cover protected cross-session debt and candidates.
    state=json.loads((root/"core/designsystem/verification/designsystem-ratchet-state.json").read_text())
    def statecase(name,mut,needle):
        x=copy.deepcopy(state); mut(x); errs=V.state_contract_errors(x); ok=needle in errs; cases.append({"name":name,"status":"PASS" if ok else "FAIL","errors":errs,"expected":needle})
    statecase("negative resolve v64 material finding",lambda x:x["confirmedFindings"].pop(next(i for i,r in enumerate(x["confirmedFindings"]) if r["rule_id"]=="DS-MATERIAL-001")),"MATERIAL_DEBT_MUTATION")
    statecase("negative remove v65 a11y finding",lambda x:x["confirmedFindings"].pop(next(i for i,r in enumerate(x["confirmedFindings"]) if r["rule_id"]=="DS-A11Y-001")),"A11Y_DEBT_MUTATION")
    statecase("negative candidate washing",lambda x:x["acceptedCandidates"].pop(),"CANDIDATE_WASHING")
    # Protected hashes are tested with isolated temporary files, never production mutation.
    for name,rel in [("negative modify ProfileScreen",V.PROFILE),("negative modify ProfileViewModel","feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileViewModel.kt"),("negative modify ProfileUiState","feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileUiState.kt"),("negative modify ProfileRepository","feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/data/ProfileRepositoryImpl.kt"),("negative modify NavigationGraphs","app/src/main/kotlin/com/autodrive/app/navigation/NavigationGraphs.kt")]:
        expected=V.PROTECTED[rel]
        with tempfile.TemporaryDirectory() as td:
            p=Path(td)/"x"; p.write_bytes((root/rel).read_bytes()+b"\nmutation")
            ok=V.sha(p)!=expected
        cases.append({"name":name,"status":"PASS" if ok else "FAIL","errors":[] if ok else ["hash did not change"],"expected":"protected hash mismatch"})
    failed=[x for x in cases if x["status"]!="PASS"]
    payload={"schemaVersion":63,"tool":"test_designsystem_verification_v63","explicitOutcomes":len(cases),"cases":cases,"verdict":"PASS" if not failed else "FAIL","failed":[x["name"] for x in failed]}
    if a.output:
        out=a.output if a.output.is_absolute() else root/a.output; out.parent.mkdir(parents=True,exist_ok=True); out.write_text(json.dumps(payload,ensure_ascii=False,indent=2)+"\n")
    print(f"V63 FIXTURES: {payload['verdict']} ({len(cases)-len(failed)}/{len(cases)})")
    for x in failed: print(" -",x["name"],x["errors"])
    return 0 if not failed else 1
if __name__=="__main__": raise SystemExit(main())
