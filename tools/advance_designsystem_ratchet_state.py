#!/usr/bin/env python3
from __future__ import annotations
import argparse, hashlib, json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
HOME_IDS=sorted([
"DS59-COLOR-HOMEHEROCOMPONENTS","DS59-COLOR-HOMESCREEN","DS59-COLOR-HOMESUPPORTCARDS","DS59-HOME-001","DS59-HOME-002","DS59-HOME-003","DS59-HOME-004","DS59-HOME-005","DS59-HOME-006","DS59-HOME-007","DS59-HOME-008","DS59-SHAPE-HOMEHEROCOMPONENTS","DS59-SHAPE-HOMESCREEN","DS59-SHAPE-HOMESUPPORTCARDS","DS59-SPACE-HOMEHEROCOMPONENTS","DS59-SPACE-HOMESCREEN","DS59-SPACE-HOMESUPPORTCARDS","DS59-TYPE-HOMEHEROCOMPONENTS","DS59-TYPE-HOMESCREEN","DS59-TYPE-HOMESUPPORTCARDS"])
SETTINGS_IDS=["DS59-SETTINGS-001"]
REPORTS_IDS=sorted([
"DS59-REPORTS-001","DS59-REPORTS-002","DS59-REPORTS-003","DS59-MAT-6FEF7AF6CB","DS59-MAT-D6C993673C","DS59-MAT-FEEC3D97F3","DS59-MAT-D9E88F5DA5","DS59-MAT-83500757C5","DS59-MAT-40A566C71D","DS59-MAT-A0FB9B1576"])
ALLOWED_HOME={
"app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeScreen.kt",
"app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeHeroComponents.kt",
"app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeSupportCards.kt"}
ALLOWED_SETTINGS={
"core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/settings/SettingsPatterns.kt",
"core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/data/DataComponents.kt"}

V64_RESOLVED_IDS=sorted([
"DS59-A11Y-9AEB8C866B","DS59-A11Y-CDDE3825F9","DS59-A11Y-C2A837EC4E",
"DS59-MAT-78B406A1AA","DS59-MAT-6CD5AA263C","DS59-MAT-7C671B14F4","DS59-MAT-9B3C851CB9","DS59-MAT-6799D3BC58","DS59-MAT-02296B2D92","DS59-MAT-6197FF510A","DS59-MAT-26D2D5FFC4","DS59-MAT-D071B2A0EA","DS59-MAT-544BAE9C14","DS59-MAT-79C15801ED","DS59-MAT-D686E776D8","DS59-MAT-027E5AFDF5","DS59-MAT-EB816DB417","DS59-MAT-AD270FAD58","DS59-MAT-CF13DDC2AA","DS59-MAT-9EB48F8635","DS59-MAT-139CBEC2E6","DS59-MAT-D32F041B86","DS59-MAT-A340CA44C4","DS59-MAT-507CF8E50C","DS59-MAT-DCB7ADBC74","DS59-MAT-1F136AECB1","DS59-MAT-4D44160C64","DS59-MAT-64F7319A28","DS59-MAT-7A5ED65DD6","DS59-MAT-AB97443291","DS59-MAT-29FBED754E","DS59-MAT-A62108624A","DS59-MAT-74539FBD18","DS59-MAT-BE9B4C80B7","DS59-MAT-39AA2FA238","DS59-MAT-B4AC09E4D7","DS59-MAT-843B8B1A4A","DS59-MAT-498FA71016","DS59-MAT-FB32BA80CD","DS59-MAT-226B9C8A53","DS59-MAT-F3AAC88E93","DS59-MAT-7864E52047","DS59-MAT-5556A29491","DS59-MAT-0668DF89DF","DS59-MAT-14A3E53094","DS59-MAT-3457B1E784"
])
V64_RESOLVED_CANDIDATES=sorted([
"DS59-A11Y2C-9DB9049F24","DS59-A11Y2C-5E868149DD","DS59-A11Y2C-C00B365524","DS59-A11Y2C-467153365F","DS59-A11Y2C-CEF649D3E7","DS59-A11Y2C-C2A837EC4E",
"DS59-MATC-AFC7933F73","DS59-MATC-67E12A5D39","DS59-MATC-61EC00345B","DS59-MATC-A04F621C83","DS59-MATC-C90A33FCBB","DS59-MATC-8E52AED6C1"
])
V64_REMAINING_CANDIDATES=sorted([
"DS59-MATC-FC49FB269C","DS59-MATC-EA81ACB322","DS59-MATC-B28DBA9BD1","DS59-MATC-9B9E4A62B6","DS59-MATC-353C4BA208","DS59-MATC-D44DE38681"
])
ALLOWED_V64={
"app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionScreen.kt",
"app/src/main/kotlin/com/autodrive/app/feature/info/presentation/AboutAppScreen.kt",
"app/src/main/kotlin/com/autodrive/app/feature/info/presentation/FaqScreen.kt",
"app/src/main/kotlin/com/autodrive/app/feature/info/presentation/PrivacyPolicyScreen.kt",
"core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/actions/ActionComponents.kt",
"core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/feedback/FeedbackComponents.kt",
"core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/inputs/InputComponents.kt",
"core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/navigation/NavigationComponents.kt",
"feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/CodeInputScreen.kt",
"feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/WaitingScreen.kt",
"feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/OtpInputScreen.kt",
"feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/PhoneInputScreen.kt",
"feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/TermsScreen.kt",
"feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/register/RegisterScreens.kt",
"feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceComponents.kt",
"feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatComposer.kt",
"feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatImageViewer.kt",
"feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatMessageComponents.kt",
"feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatScreen.kt",
"feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionEntryComponents.kt",
"feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionReportScreen.kt",
"feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionStatusBadge.kt",
"feature/notifications/src/main/kotlin/com/autodrive/app/feature/notifications/presentation/NotificationsScreen.kt"
}
ALLOWED_REPORTS={
"app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt",
"app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/CompetitionHistoryScreen.kt",
"app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/InvoiceDetailScreen.kt",
"app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/InvoiceListScreen.kt",
"app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/WeeklyCommissionsScreen.kt"}
def sha(p:Path): return hashlib.sha256(p.read_bytes()).hexdigest()
def prod_files(root:Path): return sorted(p for p in root.rglob('*.kt') if '/src/main/kotlin/' in '/'+p.relative_to(root).as_posix() and '/build/' not in '/'+p.relative_to(root).as_posix())
def digest(records): return hashlib.sha256('\n'.join(f"{x['path']}\t{x['sha256']}" for x in records).encode()).hexdigest()
def main(argv=None):
    ap=argparse.ArgumentParser(); ap.add_argument('--root',type=Path,default=ROOT); ap.add_argument('--report',type=Path,required=True); ap.add_argument('--state',type=Path); ap.add_argument('--output',type=Path)
    a=ap.parse_args(argv); root=a.root.resolve(); report=a.report if a.report.is_absolute() else root/a.report; statep=a.state if a.state and a.state.is_absolute() else root/(a.state or Path('core/designsystem/verification/designsystem-ratchet-state.json'))
    try: r=json.loads(report.read_text()); state=json.loads(statep.read_text())
    except Exception as e: print('RATCHET ADVANCE: TOOL_ERROR',e); return 2
    before=state.get('acceptedVersion'); resolved=sorted(x.get('findingId') for x in r.get('resolvedViolations',[])); failures=[]
    if before=='v59': target='v61'; exact=HOME_IDS; allowed=ALLOWED_HOME; expected_before=77; expected_after=57; expected_candidates=18; previous='v60'
    elif before=='v61': target='v62'; exact=REPORTS_IDS; allowed=ALLOWED_REPORTS; expected_before=57; expected_after=47; expected_candidates=18; previous='v61'
    elif before=='v62': target='v63'; exact=SETTINGS_IDS; allowed=ALLOWED_SETTINGS; expected_before=47; expected_after=46; expected_candidates=18; previous='v62'
    elif before=='v63': target='v64'; exact=V64_RESOLVED_IDS; allowed=ALLOWED_V64; expected_before=46; expected_after=0; expected_candidates=6; previous='v63'
    elif before=='v64':
        target='v65'; exact=[]; expected_before=0; expected_after=0; expected_candidates=6; previous='v64'
        allowdoc=json.loads((root/'V65_MUTATION_ALLOWLIST.json').read_text(encoding='utf-8'))
        allowed={x['path'] for x in allowdoc.get('files',[])}
    elif before=='v65':
        target='v66'; exact=[]; expected_before=0; expected_after=0; expected_candidates=6; previous='v65'; allowed=set()
        v66=json.loads((root/'DESIGN_SYSTEM_VERIFICATION_v66.json').read_text(encoding='utf-8'))
        resolution=json.loads((root/'DESIGN_SYSTEM_V66_MATERIAL_RESOLUTION.json').read_text(encoding='utf-8'))
        if not v66.get('runtimeFinalVerified') or not v66.get('fullV66Completion') or v66.get('finalVerdict')!='DESIGN_SYSTEM_V66_ZERO_DRIFT_COMPLETE':
            print('RATCHET ADVANCE: FAIL\n - v66 runtime final prerequisite not met'); return 1
        if sorted(resolution.get('resolvedCandidateIds',[]))!=V64_REMAINING_CANDIDATES or resolution.get('acceptedCandidatesAfterAnalysis')!=0:
            print('RATCHET ADVANCE: FAIL\n - v66 candidate finality not exact'); return 1
    else: print(f'RATCHET ADVANCE: FAIL\n - unsupported acceptedVersion={before}'); return 1
    if r.get('verdict')!='PASS': failures.append('pre-accept report not PASS')
    if resolved!=exact: failures.append(f'resolved IDs are not exact {target} set')
    if len(r.get('findings',[]))!=expected_after: failures.append(f'current confirmed != {expected_after}')
    if r.get('newViolations'): failures.append('new violations != 0')
    if r.get('touchedScopeViolations'): failures.append('touched scope debt != 0')
    if r.get('newCandidates'): failures.append('new candidates != 0')
    if len(r.get('candidates',[]))!=expected_candidates: failures.append(f'accepted candidates != {expected_candidates}')
    if target=='v64':
        resolved_candidates=sorted(x.get('findingId') for x in r.get('resolvedCandidates',[]))
        remaining_candidates=sorted(x.get('findingId') for x in r.get('candidates',[]))
        if resolved_candidates!=V64_RESOLVED_CANDIDATES: failures.append('resolved candidate IDs are not exact v64 set')
        if remaining_candidates!=V64_REMAINING_CANDIDATES: failures.append('remaining candidate IDs are not exact historical 6')
    if target=='v65':
        remaining_candidates=sorted(x.get('findingId') for x in r.get('candidates',[]))
        if remaining_candidates!=V64_REMAINING_CANDIDATES: failures.append('remaining candidate IDs are not exact historical 6')
        if r.get('resolvedCandidates'): failures.append('v65 must not resolve historical candidates')
    if target=='v66':
        remaining_candidates=sorted(x.get('findingId') for x in r.get('candidates',[]))
        if remaining_candidates!=V64_REMAINING_CANDIDATES: failures.append('v66 preaccept accepted-state candidates must still be exact historical 6')
    if set(r.get('touchedFiles',[]))!=allowed: failures.append('touched production scope mismatch')
    if len(state.get('confirmedFindings',[]))!=expected_before: failures.append(f'state confirmed != {expected_before}')
    if target in {'v62','v63','v64','v65','v66'}:
        by={x['rule_id']:x for x in r.get('rules',[])}
        expected_contract=0 if target in {'v63','v64','v65','v66'} else 1
        expected_material=0 if target in {'v64','v65','v66'} else 43
        expected_a11y=0 if target in {'v64','v65','v66'} else 3
        if by.get('DS-CONTRACT-001',{}).get('current_total')!=expected_contract: failures.append(f'DS-CONTRACT-001 != {expected_contract}')
        if by.get('DS-MATERIAL-001',{}).get('current_total')!=expected_material: failures.append(f'DS-MATERIAL-001 != {expected_material}')
        if by.get('DS-A11Y-001',{}).get('current_total')!=expected_a11y: failures.append(f'DS-A11Y-001 != {expected_a11y}')
    if failures:
        print('RATCHET ADVANCE: FAIL'); [print(' -',x) for x in failures]; return 1
    records=[{'path':p.relative_to(root).as_posix(),'sha256':sha(p)} for p in prod_files(root)]
    if len(records)!=251: print('RATCHET ADVANCE: FAIL\n - production Kotlin count != 251'); return 1
    old_sha=sha(statep); old_digest=state.get('acceptedProductionDigestSha256') or state.get('v60PerFileProductionDigest')
    state['schemaVersion']=66 if target=='v66' else (65 if target=='v65' else (64 if target=='v64' else (63 if target=='v63' else (62 if target=='v62' else 61)))); state['previousAcceptedVersion']=previous; state['acceptedVersion']=target
    state['acceptedProductionDigestSha256']=digest(records); state['acceptedSourceSha256']=digest(records)
    if target=='v62': state['v62PerFileProductionDigest']=digest(records)
    if target=='v63': state['v63PerFileProductionDigest']=digest(records)
    if target=='v64': state['v64PerFileProductionDigest']=digest(records)
    if target=='v65': state['v65PerFileProductionDigest']=digest(records)
    if target=='v66': state['v66PerFileProductionDigest']=digest(records)
    state['rules']=[{'rule_id':x['rule_id'],'previous_accepted_total':x['current_total']} for x in sorted(r['rules'],key=lambda x:x['rule_id'])]
    state['confirmedFindings']=sorted(r['findings'],key=lambda x:(x['rule_id'],x['relative_path'],x.get('symbol',''),x['fingerprint']))
    state['acceptedCandidates']=[] if target=='v66' else sorted(r['candidates'],key=lambda x:(x['rule_id'],x['relative_path'],x.get('symbol',''),x['fingerprint']))
    state['productionFiles']=records
    history_entry={'acceptedVersion':target,'previousAcceptedVersion':before,'resolvedFindingIds':exact,'preAcceptReportSha256':sha(report),'productionDigestSha256':digest(records)}
    if target=='v64': history_entry['resolvedCandidateIds']=V64_RESOLVED_CANDIDATES
    if target=='v65':
        lock=json.loads((root/'DESIGN_SYSTEM_V65_FINDINGS_LOCK.json').read_text(encoding='utf-8'))
        history_entry.update({'discoveredAccessibilityFindings':len(lock.get('findings',[])),'resolvedAccessibilityFindings':len(lock.get('findings',[])),'runtimeCandidates':0,'contrastPairsChecked':3,'scopeRows':53,'coverageReclassifications':4})
    if target=='v66':
        history_entry.update({'freshAudit':True,'sourceInventory':251,'composeSourceFiles':58,'runtimeUiRows':56,'previewOnlyRows':2,'projectUnapprovedViolations':0,'resolvedHistoricalCandidates':6,'acceptedCandidatesAfter':0,'runtimeAccessibilityVerified':True,'resolvedCandidateIds':V64_REMAINING_CANDIDATES})
    state['history']=state.get('history',[])+[history_entry]
    statep.write_text(json.dumps(state,ensure_ascii=False,indent=2)+'\n',encoding='utf-8'); new_sha=sha(statep)
    payload={'schemaVersion':66 if target=='v66' else (65 if target=='v65' else (64 if target=='v64' else (63 if target=='v63' else (62 if target=='v62' else 61)))),'oldAcceptedVersion':before,'newAcceptedVersion':target,'oldTotals':expected_before,'newTotals':expected_after,'resolvedIds':exact,'candidateChanges':{'old':18 if target=='v64' else expected_candidates,'new':0 if target=='v66' else expected_candidates},'productionDigestOld':old_digest,'productionDigestNew':digest(records),'stateShaOld':old_sha,'stateShaNew':new_sha,'verdict':'PASS'}
    if a.output:
        out=a.output if a.output.is_absolute() else root/a.output; out.parent.mkdir(parents=True,exist_ok=True); out.write_text(json.dumps(payload,ensure_ascii=False,indent=2)+'\n')
    print(f'RATCHET ADVANCE: PASS\n - acceptedVersion={target}\n - confirmed={expected_after}\n - candidates={expected_candidates}')
    return 0
if __name__=='__main__': raise SystemExit(main())
