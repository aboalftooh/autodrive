#!/usr/bin/env python3
from __future__ import annotations
import argparse,csv,hashlib,json,re,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
INPUT_SHA='746566c4d4bc61040dd134d395071bdfe97117aaa257bed9c19a94b2cd17c10e'
PROD_DIGEST='74f189d671b26860f0adb41bd800439d0718841404f175130a82e0bbf61989da'
UI_DIGEST='1098eae501d9270f12e28a1a992d194287422690e2670744130a9ae75b85fa85'
PREVIEW_DIGEST='89d648bed81115711d6fbac70812c650c83db4f021344a7d1cd198d8cb4729b0'
DS_DIGEST='e900f3942de51d3bfe10783dfe92ec605d6dc9756117b0f241ce867508266e65'
MAPPING_SHA='75eaa3e4c197df6e444778afde8d1714a42ac787355b57fbb7cc24afceb669b5'
EXCEPTIONS_SHA='37517e5f3dc66819f61f5a7bb8ace1921282415f10551d2defa5c3eb0985b570'
CANDS=sorted(['DS59-MATC-FC49FB269C','DS59-MATC-EA81ACB322','DS59-MATC-B28DBA9BD1','DS59-MATC-9B9E4A62B6','DS59-MATC-353C4BA208','DS59-MATC-D44DE38681'])
RULES=sorted(['DS-A11Y-001','DS-A11Y-002','DS-A11Y-003','DS-BORDER-001','DS-COLOR-001','DS-CONTRACT-001','DS-CONTRAST-001','DS-DUP-001','DS-ELEVATION-001','DS-EXCEPTION-001','DS-MATERIAL-001','DS-SHAPE-001','DS-SPACE-001','DS-TYPE-001'])
V65_AUTH={
'DESIGN_SYSTEM_UI_COVERAGE_v65.csv':'43c669115a232ec5a2ddb6192faaa4da008950020095d12c8577ee90c16f61c3',
'DESIGN_SYSTEM_VERIFICATION_v65.json':'d259350466dde906bb6972042d367cb62fc86d501b36987cd743e003c4620fb8',
'DESIGN_SYSTEM_VERIFICATION_v65.md':'53b147055b438e6c3541289205bc097738b92c5c14298e2be3d0bcf3de36ca6f',
'AutoDrive-v65-report.md':'684689aa31616850c26c42e9e2abca53abd0d602dd0e33560090ed7c7a29838e',
'ACCESSIBILITY_AUDIT_V65.md':'29bb4b1f572687d92cae424db06664e75a37d36277dced7903d3ff45577c040f',
'ACCESSIBILITY_CONTRAST_MATRIX_v65.csv':'a11edca40949c9a2103c74c6d85f880d6d9ee7a7b8050eecc8e2cc209c9bb9e7',
'ACCESSIBILITY_RUNTIME_MATRIX_v65.md':'2733d81066ab9eb5b4068621db5698cae3128a2c288ee9f7dad55b8afc54885f',
'DESIGN_SYSTEM_V65_ACCESSIBILITY_PRESTATE.json':'d432c61fdf8ac6d4e9b2e9bcb5c796908a255329cb05a99ef6bdd553b77df366',
'DESIGN_SYSTEM_V65_FINDINGS_LOCK.json':'31eaaa0831322bad438f80ae740876720fa108042fc50b25cf4542cee4ec29e5',
'V65_MUTATION_ALLOWLIST.json':'58c9f04484b9cd077d674bf8c2714f39e9f83e73f53b80d803449f28a78d5ca6',
'tools/verify_designsystem_v65_accessibility.py':'437f81092869f4289e681480161b7c76645a06b72b666f132196c16f2ce266b0',
'tools/test_designsystem_verification_v65.py':'beb5e17174b84c82bbe4e5935bc60dd1f36a51fa9a510bb209192ed24e691fcc',
'tools/run_v64_historical_gate_v65.py':'0050f52eb2320f2adc7741e3e7c954e8fbdc2d88ba996908940fef98511a4dac',
'scripts/verify-v65-static.sh':'6fd5196739296617d8c1955df6ea014d56d8731ecbb173ed7c167825c20b91c5'}
V59_AUTH={'DESIGN_SYSTEM_BASELINE_v59.md':'f6b59f0e09c58262a19438462da309a6739d9abe0e861c715fb21f8971a49438','DESIGN_SYSTEM_BASELINE_v59.json':'906c12831752daa2f7fce190a3aab37a75817c415b055c09ab367d8207248abc','DESIGN_SYSTEM_UI_COVERAGE_v59.csv':'191d4497d0433ba078fb7b71bee080763cd013cc64f34ab3220477649d568dea','DESIGN_SYSTEM_V1_CONTRACT_REGISTRY_v59.md':'ed758157e064bb1f8fc61d13e2efdd4b09938ec54640f49d603e0df18a215e0d'}
PREVIEWS=sorted(['core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/preview/V1ComponentPreviews.kt','core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/preview/V1PatternPreviews.kt'])
BUILD_NAMES={'build.gradle','build.gradle.kts','settings.gradle','settings.gradle.kts','gradle.properties','libs.versions.toml','gradle-wrapper.properties'}
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def dg(root,paths):return hashlib.sha256('\n'.join(f'{p.relative_to(root).as_posix()}\t{sha(p)}' for p in sorted(paths)).encode()).hexdigest()
def readj(p):return json.loads(p.read_text(encoding='utf-8'))
def run(root:Path):
  checks=[]; errors=[]
  def ck(cid,cond,msg):
    checks.append({'id':cid,'status':'PASS' if cond else 'FAIL','message':msg})
    if not cond: errors.append(f'{cid}: {msg}')
  for rel,exp in {**V59_AUTH,**V65_AUTH}.items(): ck('AUTH', (root/rel).is_file() and sha(root/rel)==exp, rel)
  ck('MAPPING',sha(root/'core/designsystem/verification/primitive-mapping.json')==MAPPING_SHA,'primitive mapping immutable')
  ck('EXCEPTIONS',sha(root/'core/designsystem/verification/designsystem-exceptions.json')==EXCEPTIONS_SHA and readj(root/'core/designsystem/verification/designsystem-exceptions.json')==[],'exception ledger immutable/empty')
  state=readj(root/'core/designsystem/verification/designsystem-ratchet-state.json')
  ck('RATCHET',state.get('acceptedVersion')=='v65' and state.get('schemaVersion')==65,'blocked/preaccept state remains v65/schema65')
  ck('RATCHET',len(state.get('confirmedFindings',[]))==0,'confirmed=0')
  ck('RATCHET',sorted(x['findingId'] for x in state.get('acceptedCandidates',[]))==CANDS,'exact historical six retained in accepted state')
  prod=sorted(p for p in root.rglob('*.kt') if '/src/main/kotlin/' in '/'+p.relative_to(root).as_posix() and '/build/' not in '/'+p.relative_to(root).as_posix())
  ck('PROD',len(prod)==251 and dg(root,prod)==PROD_DIGEST,'251 production Kotlin and accepted digest exact')
  accepted={x['path']:x['sha256'] for x in state.get('productionFiles',[])}
  changed=[p.relative_to(root).as_posix() for p in prod if accepted.get(p.relative_to(root).as_posix())!=sha(p)]
  ck('ZERO-OP',changed==[],'no production mutation from accepted v65')
  compose=[p for p in prod if '@Composable' in p.read_text(encoding='utf-8')]
  ck('COMPOSE',len(compose)==58,'58 Compose-source files')
  cov=list(csv.DictReader((root/'DESIGN_SYSTEM_UI_COVERAGE_v66.csv').open(encoding='utf-8',newline='')))
  ui=[root/r['full_relative_path'] for r in cov]
  ck('COVERAGE',len(cov)==56 and len({r['full_relative_path'] for r in cov})==56 and dg(root,ui)==UI_DIGEST,'56/56 runtime UI coverage exact')
  ck('PREVIEW',all((root/x).is_file() for x in PREVIEWS) and dg(root,[root/x for x in PREVIEWS])==PREVIEW_DIGEST,'exact 2 preview files/digest')
  inv=readj(root/'DESIGN_SYSTEM_V66_SOURCE_INVENTORY.json'); classes={x['path']:x['runtimeReachabilityClass'] for x in inv['files']}
  ck('INVENTORY',len(inv['files'])==251 and inv['unclassifiedCount']==0,'251/251 classified')
  ck('INVENTORY',sum(v=='RUNTIME_UI' for v in classes.values())==56 and sum(v=='PREVIEW_ONLY' for v in classes.values())==2,'56 runtime + 2 preview')
  ck('PREVIEW',sorted(k for k,v in classes.items() if v=='PREVIEW_ONLY')==PREVIEWS,'preview classification exact')
  pre=readj(root/'DESIGN_SYSTEM_V66_PRESTATE.json')
  ck('INPUT',pre.get('inputZipSha256')==INPUT_SHA and pre.get('archiveEntries')==751,'input identity exact')
  ck('PRESTATE',len(pre.get('rows',[]))==56 and pre.get('productionDigest')==PROD_DIGEST and pre.get('dsMainDigest')==DS_DIGEST,'prestate coverage/source digest exact')
  build=sorted(p for p in root.rglob('*') if p.is_file() and p.name in BUILD_NAMES and '/build/' not in '/'+p.relative_to(root).as_posix() and '/.gradle/' not in '/'+p.relative_to(root).as_posix())
  ck('BUILD-CONFIG',len(build)==20 and dg(root,build)==pre.get('buildConfigurationDigest'),'20 build config files unchanged from frozen prestate')
  lock=readj(root/'DESIGN_SYSTEM_V66_FINDINGS_LOCK.json'); allow=readj(root/'V66_MUTATION_ALLOWLIST.json')
  ck('FINDINGS',lock.get('confirmedStaticFindings')==0 and lock.get('candidateNeedsFinalClassification')==0 and len(lock.get('runtimeRequiredIds',[]))==45,'0 static / 45 runtime obligations')
  ck('ALLOWLIST',allow.get('files')==[] and allow.get('productionMutationPolicy')=='ZERO_OP','zero-op mutation allowlist')
  mat=readj(root/'DESIGN_SYSTEM_V66_MATERIAL_RESOLUTION.json')
  ck('MATERIAL',sorted(mat.get('resolvedCandidateIds',[]))==CANDS and mat.get('acceptedCandidatesAfterAnalysis')==0 and mat.get('newCandidates')==0,'exact six resolved; zero new')
  ck('MATERIAL',all(x.get('status')=='VERIFIED_ALLOWED_PRIMITIVE' and x.get('mappingId') in {'MAT-TOPAPPBAR','MAT-CPI'} and x.get('sourceEvidence',{}).get('sha256')==sha(root/x['path']) for x in mat.get('records',[])),'six semantic evidence records source-bound')
  scanner=readj(root/'DESIGN_SYSTEM_V66_SCANNER_COVERAGE.json')['rules']; by={x['ruleId']:x for x in scanner}
  ck('SCANNER',sorted(by)==RULES and all(by[r]['filesScanned']>0 and by[r]['symbolsScanned']>0 and by[r]['confirmedFindings']==0 and by[r]['candidateFindings']==0 for r in RULES),'14/14 source-aware scanners complete')
  base=readj(root/'DESIGN_SYSTEM_BASELINE_v66.json')
  ck('ZERO-DRIFT',base.get('projectUnapprovedViolations')==0 and all(x['v66_unapproved']==0 for x in base.get('rules',[])),'all rules zero unapproved')
  ck('STATIC-VERDICT',base.get('finalVerdict')=='STATIC_ZERO_DRIFT_COMPLETE / FINAL_RUNTIME_BLOCKED' and base.get('runtimeFinalVerified') is False,'truthful static blocked verdict')
  runtime=readj(root/'V66_RUNTIME_TASK_MANIFEST.json')
  ck('RUNTIME-TRUTH',runtime.get('bootstrapStatus')=='BLOCKED_UNKNOWN_HOST' and runtime.get('taskDiscoveryStatus')=='NOT_RUN','runtime bootstrap blocked, no task washing')
  ck('REGISTRY',sum(1 for l in (root/'DESIGN_SYSTEM_V1_CONTRACT_REGISTRY_v66.md').read_text(encoding='utf-8').splitlines() if l.startswith('## DS-'))==48,'48 V1 contracts reconciled')
  payload={'schemaVersion':66,'session':66,'checks':sorted(checks,key=lambda x:(x['id'],x['message'])),'errors':sorted(errors),'productionKotlinCount':len(prod),'composeSourceCount':len(compose),'runtimeUiCount':len(cov),'previewOnlyCount':2,'changedProductionFiles':changed,'resolvedCandidateIds':CANDS,'runtimeRequiredCount':len(lock.get('runtimeRequiredIds',[])),'projectUnapprovedViolations':0 if not errors else None,'runtimeFinalVerified':False,'fullV66Completion':False,'acceptedVersion':state.get('acceptedVersion'),'verdict':'PASS_STATIC' if not errors else 'FAIL_V66_STATIC'}
  return (0 if not errors else 1),payload
def main(argv=None):
  ap=argparse.ArgumentParser();ap.add_argument('--root',type=Path,default=ROOT);ap.add_argument('--output',type=Path);ap.add_argument('--json',action='store_true');a=ap.parse_args(argv)
  try: code,p=run(a.root.resolve())
  except (OSError,ValueError,KeyError,json.JSONDecodeError,UnicodeError) as e: code=2;p={'schemaVersion':66,'session':66,'errors':[f'{type(e).__name__}: {e}'],'verdict':'TOOL_ERROR'}
  if a.output:
    out=a.output if a.output.is_absolute() else a.root.resolve()/a.output;out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(p,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
  if a.json:print(json.dumps(p,ensure_ascii=False,sort_keys=True))
  else:
    print('V66 FINAL STATIC:',p['verdict']);
    if code==0:print(' - 251 source / 58 Compose / 56 runtime UI / 2 preview\n - 14 rules: zero unapproved\n - Material: 6/6 resolved in analysis layer\n - production mutation: 0\n - Runtime: BLOCKED; Ratchet remains v65')
    else:
      for e in p.get('errors',[]):print(' -',e)
  return code
if __name__=='__main__':raise SystemExit(main())
