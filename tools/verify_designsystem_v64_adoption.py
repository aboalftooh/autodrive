#!/usr/bin/env python3
from __future__ import annotations
import argparse, csv, hashlib, json, re, sys
from collections import Counter
from pathlib import Path
from typing import Any
ROOT=Path(__file__).resolve().parents[1]
INPUT_SHA='b6c8f4c65c2a462cd2b5deedcc788a3478c50b3a097d413fa56e04a20b432c25'
V63_COVERAGE_SHA='f162bd735c921fb0d1bab233e158d056f78f39228e60957ce9644f5ce55ed1a4'
PROTECTED_PROD_DIGEST='164fb50c87aa7bad2d583fc5a4fa299f49b93e1a332b73f547cb17dc00b7a580'
PROTECTED_DS_DIGEST='41ff466d17f9395a5318a4ac4d332baaeba7c8f23cc28f859745e3662fd48b26'
MAPPING_SHA='75eaa3e4c197df6e444778afde8d1714a42ac787355b57fbb7cc24afceb669b5'
EXCEPTIONS_SHA='37517e5f3dc66819f61f5a7bb8ace1921282415f10551d2defa5c3eb0985b570'
PROTECTED={
'DESIGN_SYSTEM_BASELINE_v59.md':'f6b59f0e09c58262a19438462da309a6739d9abe0e861c715fb21f8971a49438',
'DESIGN_SYSTEM_BASELINE_v59.json':'906c12831752daa2f7fce190a3aab37a75817c415b055c09ab367d8207248abc',
'DESIGN_SYSTEM_UI_COVERAGE_v59.csv':'191d4497d0433ba078fb7b71bee080763cd013cc64f34ab3220477649d568dea',
'DESIGN_SYSTEM_V1_CONTRACT_REGISTRY_v59.md':'ed758157e064bb1f8fc61d13e2efdd4b09938ec54640f49d603e0df18a215e0d',
'DESIGN_SYSTEM_UI_COVERAGE_v63.csv':V63_COVERAGE_SHA,
'DESIGN_SYSTEM_VERIFICATION_v63.json':'7c1713dd79d5660841865af34e7103a5a1d8588afcadd90fd10cf9442513a56b',
'DESIGN_SYSTEM_VERIFICATION_v63.md':'9544bd76cb2f7e84116d4a5d631096617959d2e6b314e9c8c1e94dd514b7c4fd',
'AutoDrive-v63-report.md':'af68cd6ea4372fcd9000a608e8593b34d5f00d8603899666f0b22cdf4d38613f',
'tools/verify_designsystem_v63_settings.py':'8e1f4ec6e8f392be51d6737145b77740f491c2ec35b6c5d65b34a7e16dfacaea',
'tools/test_designsystem_verification_v63.py':'25a431209887f7e2d3bc2f3ffb3dc1885f9df53db6037e03a26751b2c63334d4',
'scripts/verify-v63-static.sh':'cc449607552a44c1ec3688d58074aa432d0f954c52916ab79064c14ec42d62fb',
}
ALLOW=[
'app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionScreen.kt','app/src/main/kotlin/com/autodrive/app/feature/info/presentation/AboutAppScreen.kt','app/src/main/kotlin/com/autodrive/app/feature/info/presentation/FaqScreen.kt','app/src/main/kotlin/com/autodrive/app/feature/info/presentation/PrivacyPolicyScreen.kt','core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/actions/ActionComponents.kt','core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/feedback/FeedbackComponents.kt','core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/inputs/InputComponents.kt','core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/navigation/NavigationComponents.kt','feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/CodeInputScreen.kt','feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/WaitingScreen.kt','feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/OtpInputScreen.kt','feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/PhoneInputScreen.kt','feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/TermsScreen.kt','feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/register/RegisterScreens.kt','feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceComponents.kt','feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatComposer.kt','feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatImageViewer.kt','feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatMessageComponents.kt','feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatScreen.kt','feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionEntryComponents.kt','feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionReportScreen.kt','feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionStatusBadge.kt','feature/notifications/src/main/kotlin/com/autodrive/app/feature/notifications/presentation/NotificationsScreen.kt']
PROVIDERS={x for x in ALLOW if x.startswith('core/designsystem/src/main/')}
CLEAN={
'app/src/main/kotlin/com/autodrive/app/PermissionsDeniedDialog.kt','app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt','app/src/main/kotlin/com/autodrive/app/navigation/AppNavigation.kt','feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/WelcomeScreen.kt','feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/LoginScreen.kt','feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/register/AccountTypeScreen.kt','feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/BalanceScreen.kt','feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/presentation/WithdrawalSheet.kt','feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/NewChatDialog.kt'}
MAPPED=['IconButton','TextButton','HorizontalDivider','OutlinedTextField','FloatingActionButton','Button','AlertDialog','ModalBottomSheet']
CANDIDATE=['TopAppBar','CircularProgressIndicator']

def sha(p:Path)->str:return hashlib.sha256(p.read_bytes()).hexdigest()
def digest(rows:list[str])->str:return hashlib.sha256('\n'.join(rows).encode()).hexdigest()
def prod(root:Path):return sorted(p for p in root.rglob('*.kt') if '/src/main/kotlin/' in '/'+p.relative_to(root).as_posix() and '/build/' not in '/'+p.relative_to(root).as_posix())
def balanced(text:str)->bool:
    text=re.sub(r'/\*.*?\*/','',text,flags=re.S); text=re.sub(r'//.*','',text); text=re.sub(r'""".*?"""','""',text,flags=re.S); text=re.sub(r'"(?:\\.|[^"\\])*"','""',text)
    for a,b in [('{','}'),('(',')'),('[',']')]:
        d=0
        for c in text:
            d += 1 if c==a else -1 if c==b else 0
            if d<0:return False
        if d:return False
    return True

def run(root:Path)->tuple[int,dict[str,Any]]:
    errors=[]; checks=[]
    def ck(cid,ok,msg):
        checks.append({'id':cid,'status':'PASS' if ok else 'FAIL','message':msg})
        if not ok: errors.append(f'{cid}: {msg}')
    for rel,exp in PROTECTED.items(): ck('AUTH', (root/rel).is_file() and sha(root/rel)==exp, rel)
    ck('MAPPING',sha(root/'core/designsystem/verification/primitive-mapping.json')==MAPPING_SHA,'primitive mapping immutable')
    ck('EXCEPTIONS',sha(root/'core/designsystem/verification/designsystem-exceptions.json')==EXCEPTIONS_SHA,'exception ledger immutable')
    pre=json.loads((root/'DESIGN_SYSTEM_V64_TOUCH_PRESTATE.json').read_text())
    ck('INPUT',pre.get('inputZipSha256')==INPUT_SHA and pre.get('acceptedVersion')=='v63','v64 prestate source identity')
    pre_by={x['path']:x for x in pre.get('files',[])}; ck('PRESTATE',set(pre_by)==set(ALLOW),'23 exact prestate files')
    snap=json.loads((root/'tools/fixtures/v64/v63_snapshot/manifest.json').read_text()); sm={x['path']:x for x in snap['files']}
    for rel in ALLOW:
        rec=pre_by.get(rel,{}); sr=sm.get(rel,{})
        ck('SNAPSHOT',bool(sr) and sha(root/'tools/fixtures/v64/v63_snapshot'/sr['snapshot'])==rec.get('preSha256'),'snapshot '+rel)
    rows=list(csv.DictReader((root/'DESIGN_SYSTEM_UI_COVERAGE_v63.csv').open(newline='',encoding='utf-8')))
    v64=[r for r in rows if r.get('target_session')=='v64']
    ck('COVERAGE',len(v64)==28,'target_session=v64 rows=28')
    ck('COVERAGE',sum(int(r.get('confirmed_material_bypasses') or 0) for r in v64)==43,'43 confirmed Material findings')
    row_by={r['full_relative_path']:r for r in rows}
    for rel in sorted(CLEAN):
        r=row_by[rel]; expected=r.get('v63_sha256') or r['sha256_v58']
        ck('CLEAN',sha(root/rel)==expected,'clean carry-forward '+rel)
    ps=prod(root); ck('PROD-COUNT',len(ps)==251,'production Kotlin count=251')
    protected=[p for p in ps if p.relative_to(root).as_posix() not in set(ALLOW)]
    ck('PROTECTED-PROD',len(protected)==228 and digest([f'{p.relative_to(root).as_posix()}\t{sha(p)}' for p in protected])==PROTECTED_PROD_DIGEST,'228 protected production digest')
    base=root/'core/designsystem/src/main'; ds=sorted(p for p in base.rglob('*.kt')); dsp=[p for p in ds if p.relative_to(root).as_posix() not in PROVIDERS]
    ck('PROTECTED-DS',len(dsp)==23 and digest([f'{p.relative_to(root).as_posix()}\t{sha(p)}' for p in dsp])==PROTECTED_DS_DIGEST,'23 protected DS digest')
    changed={rel for rel in ALLOW if sha(root/rel)!=pre_by[rel]['preSha256']}
    ck('MUTATION',changed==set(ALLOW),'exact 23 production files changed')
    # Provider gaps: only approved public extensions, and they are actively consumed.
    action=(root/next(x for x in PROVIDERS if x.endswith('ActionComponents.kt'))).read_text(); inp=(root/next(x for x in PROVIDERS if x.endswith('InputComponents.kt'))).read_text(); feed=(root/next(x for x in PROVIDERS if x.endswith('FeedbackComponents.kt'))).read_text(); nav=(root/next(x for x in PROVIDERS if x.endswith('NavigationComponents.kt'))).read_text()
    ck('API-A','enum class AutoDrivePrimaryButtonTone { Primary, Destructive }' in action and 'tone: AutoDrivePrimaryButtonTone = AutoDrivePrimaryButtonTone.Primary' in action and 'AutoDriveStatus.Error' in action,'destructive PrimaryButton tone')
    ck('API-B','enum class AutoDriveTextFieldLayout { Standard, CompactMultiline }' in inp and 'label: String? = null' in inp and 'isError: Boolean = errorText != null' in inp and 'layout: AutoDriveTextFieldLayout = AutoDriveTextFieldLayout.Standard' in inp,'TextField adoption gap')
    ck('API-C','skipPartiallyExpanded: Boolean = false' in feed and 'rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)' in feed,'BottomSheet partial state')
    ck('API-D','titleContent: (@Composable () -> Unit)? = null' in nav and 'if (titleContent != null) titleContent()' in nav,'BackHeader rich title')
    for name,text in [('Action',action),('Input',inp),('Feedback',feed),('Navigation',nav)]:
        ck('RAW-ESCAPE',not re.search(r'fun\s+AutoDrive(?:PrimaryButton|TextField|BottomSheet|BackHeader)\s*\([^)]*\bColor\s*[?,)]',text,re.S),name+' no raw Color public escape')
    consumers='\n'.join((root/x).read_text() for x in ALLOW if x not in PROVIDERS)
    for needle in ['AutoDrivePrimaryButtonTone.Destructive','AutoDriveTextFieldLayout.CompactMultiline','skipPartiallyExpanded = true','titleContent = {']:
        ck('API-USED',needle in consumers,'provider extension used: '+needle)
    # No mapped/candidate Material calls at the 19 migrated target files.
    target_mut=set(ALLOW)-PROVIDERS
    rawpat=re.compile(r'(?<![A-Za-z0-9_])(?:'+'|'.join(MAPPED+CANDIDATE)+r')\s*\(')
    for rel in sorted(target_mut): ck('MATERIAL-CLOSE',not rawpat.search((root/rel).read_text()),'mapped/candidate primitives absent '+rel)
    status=(root/'feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionStatusBadge.kt').read_text()
    ck('STATUS-ADAPTER','AutoDriveStatusChip' in status and 'CommissionStatus.WITHDRAWABLE' in status and 'Surface(' not in status,'CommissionStatus adapter delegates to DS')
    chatc=(root/'feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatComposer.kt').read_text(); chatm=(root/'feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatMessageComponents.kt').read_text(); chati=(root/'feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatImageViewer.kt').read_text()
    for desc in ['إلغاء التسجيل الصوتي','إرسال التسجيل الصوتي','إيقاف الرسالة الصوتية مؤقتًا','تشغيل الرسالة الصوتية']:
        ck('A11Y',desc in chatc+chatm,'meaningful description '+desc)
    ck('TOUCH',not re.search(r'\.size\(\s*(?:36|40|44)\.dp\s*\)',chatc+chatm+chati),'forced sub-48 caller sizes removed')
    # Core behavior/invariant anchors retained.
    invariants={
      'BALANCE':['Money.sum(requests.map { it.amount })','onCancelAll','isCancelling'],
      'COMPETITION':['CompetitionAvailability.DISABLED','CompetitionAvailability.LOCKED','CompetitionAvailability.ACTIVE'],
      'CHAT':['ActivityResultContracts.TakePicture','ActivityResultContracts.GetContent','ActivityResultContracts.RequestPermission','createAndStartRecorder','onSendVoice(voicePath)','onSendText(text.trim())','player.pause()','player.play()'],
      'COMMISSION':['InvoicePdfGenerator.generateAndShare','InvoicePdfGenerator.generateAndPrint','WhatsAppHelper.shareInvoice','CommissionStatus.WITHDRAWABLE','CommissionStatus.PENDING','CommissionStatus.PAID'],
      'NOTIFY':['viewModel.markAllAsRead()','snackbarHostState.showSnackbar','onNotificationClick(notification)']}
    blob={'BALANCE':(root/ALLOW[14]).read_text(),'COMPETITION':(root/ALLOW[0]).read_text(),'CHAT':chatc+chatm+(root/'feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/presentation/ChatScreen.kt').read_text(),'COMMISSION':(root/ALLOW[19]).read_text()+status,'NOTIFY':(root/ALLOW[22]).read_text()}
    for k,needles in invariants.items():
        for n in needles: ck('BEHAVIOR',n in blob[k],f'{k}: {n}')
    for rel in ALLOW:
        ck('STRUCTURE',balanced((root/rel).read_text()),'balanced Kotlin '+rel)
    state=json.loads((root/'core/designsystem/verification/designsystem-ratchet-state.json').read_text()); av=state.get('acceptedVersion')
    ck('STATE',av in {'v63','v64'},'acceptedVersion v63 preaccept or v64 postaccept')
    if av=='v63':
        ck('STATE',len(state.get('confirmedFindings',[]))==46 and len(state.get('acceptedCandidates',[]))==18,'v63 46/18 preaccept')
    elif av=='v64':
        ck('STATE',len(state.get('confirmedFindings',[]))==0 and len(state.get('acceptedCandidates',[]))==6,'v64 0/6 postaccept')
        hist=state.get('history',[]); ck('STATE',bool(hist) and hist[-1].get('acceptedVersion')=='v64' and len(hist[-1].get('resolvedFindingIds',[]))==46 and len(hist[-1].get('resolvedCandidateIds',[]))==12,'v64 exact history accounting')
    payload={'schemaVersion':64,'session':64,'inputSha256':INPUT_SHA,'acceptedVersion':av,'targetV64Rows':len(v64),'changedProductionFiles':sorted(changed),'protectedProductionDigest':PROTECTED_PROD_DIGEST,'protectedDsDigest':PROTECTED_DS_DIGEST,'checks':checks,'errors':errors,'verdict':'PASS' if not errors else 'FAIL_V64_ADOPTION'}
    return (0 if not errors else 1),payload

def main(argv=None):
    ap=argparse.ArgumentParser();ap.add_argument('--root',type=Path,default=ROOT);ap.add_argument('--output',type=Path);ap.add_argument('--json',action='store_true');a=ap.parse_args(argv)
    try: code,p=run(a.root.resolve())
    except (OSError,ValueError,KeyError,json.JSONDecodeError,UnicodeError) as e: code=2;p={'schemaVersion':64,'session':64,'errors':[f'{type(e).__name__}: {e}'],'verdict':'TOOL_ERROR'}
    if a.output:
        out=a.output if a.output.is_absolute() else a.root.resolve()/a.output;out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(p,ensure_ascii=False,indent=2)+'\n')
    if a.json:print(json.dumps(p,ensure_ascii=False,sort_keys=True))
    else:
        print('V64 COMPONENT ADOPTION STATIC VERIFICATION:',p['verdict'])
        if code==0: print(' - exact 23-file mutation scope\n - 43 Material + 3 forced A11Y closures source-verified\n - provider gaps exactly 4\n - protected digests PASS')
        else:
            for e in p.get('errors',[]):print(' -',e)
    return code
if __name__=='__main__': raise SystemExit(main())
