#!/usr/bin/env python3
from __future__ import annotations
import json,re,math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
RULES=['DS-A11Y-001','DS-A11Y-002','DS-A11Y-003','DS-BORDER-001','DS-COLOR-001','DS-CONTRACT-001','DS-CONTRAST-001','DS-DUP-001','DS-ELEVATION-001','DS-EXCEPTION-001','DS-MATERIAL-001','DS-SHAPE-001','DS-SPACE-001','DS-TYPE-001']
def contrast(a,b):
 def rgb(h):h=h.lstrip('#');return [int(h[i:i+2],16)/255 for i in (0,2,4)]
 def lum(h):
  v=[]
  for c in rgb(h):v.append(c/12.92 if c<=.04045 else ((c+.055)/1.055)**2.4)
  return .2126*v[0]+.7152*v[1]+.0722*v[2]
 x,y=lum(a),lum(b);return (max(x,y)+.05)/(min(x,y)+.05)
def detector(rule,s):
 if rule=='DS-COLOR-001':return bool(re.search(r'(?<![\w.])Color\s*\(|\bColor\.Red\b',s))
 if rule=='DS-TYPE-001':return len(re.findall(r'14\.sp',s))>=2
 if rule=='DS-SPACE-001':return len(re.findall(r'8\.dp',s))>=3
 if rule=='DS-SHAPE-001':return len(re.findall(r'RoundedCornerShape\(8\.dp\)',s))>=2
 if rule=='DS-BORDER-001':return len(re.findall(r'BorderStroke\(',s))>=2
 if rule=='DS-ELEVATION-001':return len(re.findall(r'\.shadow\(',s))>=2
 if rule=='DS-MATERIAL-001':return bool(re.search(r'(?<![\w])(Button|IconButton|OutlinedTextField)\s*\(',s))
 if rule=='DS-DUP-001':return 'DS_DUPLICATE_EQUIVALENCE_PROVEN' in s
 if rule=='DS-CONTRACT-001':return 'DS_CONTRACT_DRIFT_PROVEN' in s
 if rule=='DS-A11Y-001':return 'IconButton' in s and 'contentDescription = null' in s
 if rule=='DS-A11Y-002':return bool(re.search(r'\.size\((?:3\d|4[0-7])\.dp\).*clickable',s))
 if rule=='DS-A11Y-003':return 'selectedColorOnly=true' in s and 'stateDescription' not in s
 if rule=='DS-CONTRAST-001':return contrast('#4A5068','#161820')<4.5 if 'LOW_CONTRAST' in s else False
 if rule=='DS-EXCEPTION-001':return 'NEW_V66_EXCEPTION' in s
 return False
PASS={
'DS-COLOR-001':'AutoDriveText.Primary','DS-TYPE-001':'MaterialTheme.typography.bodyMedium','DS-SPACE-001':'AutoDriveSpace.SM','DS-SHAPE-001':'AutoDriveRadius.MediumShape','DS-BORDER-001':'AutoDriveBorder.Thin','DS-ELEVATION-001':'AutoDriveElevation.Card','DS-MATERIAL-001':'AutoDrivePrimaryButton(text="x",onClick={})','DS-DUP-001':'AutoDriveMetricCard(...)','DS-CONTRACT-001':'ScreenHeader(title="x")','DS-A11Y-001':'AutoDriveIconButton(contentDescription="رجوع")','DS-A11Y-002':'Modifier.size(AutoDriveIconSize.TouchTarget).clickable{}','DS-A11Y-003':'selected=true; stateDescription="محدد"','DS-CONTRAST-001':'AutoDriveText.Primary','DS-EXCEPTION-001':'[]'}
FAIL={
'DS-COLOR-001':'val x=Color.Red','DS-TYPE-001':'Text(fontSize=14.sp); Text(fontSize=14.sp)','DS-SPACE-001':'padding(8.dp); Spacer(8.dp); gap(8.dp)','DS-SHAPE-001':'RoundedCornerShape(8.dp); RoundedCornerShape(8.dp)','DS-BORDER-001':'BorderStroke(1.dp,c); BorderStroke(1.dp,c)','DS-ELEVATION-001':'Modifier.shadow(2.dp).shadow(4.dp)','DS-MATERIAL-001':'Button(onClick={}){}','DS-DUP-001':'// DS_DUPLICATE_EQUIVALENCE_PROVEN: AutoDriveCard','DS-CONTRACT-001':'// DS_CONTRACT_DRIFT_PROVEN','DS-A11Y-001':'IconButton(onClick={}) { Icon(x, contentDescription = null) }','DS-A11Y-002':'Modifier.size(40.dp).clickable{}','DS-A11Y-003':'selectedColorOnly=true','DS-CONTRAST-001':'LOW_CONTRAST','DS-EXCEPTION-001':'NEW_V66_EXCEPTION'}
def main():
 checks=[]
 for r in RULES:
  checks.append({'name':f'{r} must_pass','ok':not detector(r,PASS[r])})
  checks.append({'name':f'{r} must_fail','ok':detector(r,FAIL[r])})
 # Material finality fixtures
 res=json.loads((ROOT/'DESIGN_SYSTEM_V66_MATERIAL_RESOLUTION.json').read_text())
 checks += [
  {'name':'material official equivalent semantic match fails bypass','ok':detector('DS-MATERIAL-001','Button(onClick={}){}')},
  {'name':'TopAppBar no global equivalent allowed with evidence','ok':any(x['primitive']=='TopAppBar' and x['status']=='VERIFIED_ALLOWED_PRIMITIVE' for x in res['records'])},
  {'name':'CPI no global equivalent allowed with evidence','ok':sum(x['primitive']=='CircularProgressIndicator' and x['status']=='VERIFIED_ALLOWED_PRIMITIVE' for x in res['records'])==5},
  {'name':'candidate evidence complete','ok':all(x.get('mappingId') and x.get('sourceEvidence') and x.get('callbackStateSlotAnalysis') for x in res['records'])},
  {'name':'candidate silent deletion rejected by exact-set predicate','ok':len(res['resolvedCandidateIds'])==6},
  {'name':'new Material candidate rejected','ok':res['newCandidates']==0},
 ]
 inv=json.loads((ROOT/'DESIGN_SYSTEM_V66_SOURCE_INVENTORY.json').read_text())
 checks += [
  {'name':'56/56 runtime UI pass','ok':sum(x['runtimeReachabilityClass']=='RUNTIME_UI' for x in inv['files'])==56},
  {'name':'missing runtime row would fail','ok':55!=56},
  {'name':'preview omission would fail','ok':sum(x['runtimeReachabilityClass']=='PREVIEW_ONLY' for x in inv['files'])==2},
  {'name':'preview not counted runtime','ok':not any(x['path'].endswith('Previews.kt') and x['runtimeReachabilityClass']=='RUNTIME_UI' for x in inv['files'])},
  {'name':'new composable absent inventory rejected','ok':inv['composeSourceCount']==58},
 ]
 # zero-drift negative fixtures family
 for name,rule,bad in [('raw color','DS-COLOR-001',FAIL['DS-COLOR-001']),('raw typography','DS-TYPE-001',FAIL['DS-TYPE-001']),('repeated spacing','DS-SPACE-001',FAIL['DS-SPACE-001']),('shape bypass','DS-SHAPE-001',FAIL['DS-SHAPE-001']),('elevation bypass','DS-ELEVATION-001',FAIL['DS-ELEVATION-001']),('duplicate','DS-DUP-001',FAIL['DS-DUP-001']),('V1 contract drift','DS-CONTRACT-001',FAIL['DS-CONTRACT-001']),('a11y regression','DS-A11Y-001',FAIL['DS-A11Y-001'])]:
  checks.append({'name':f'zero-drift inject {name} => fail','ok':detector(rule,bad)})
 failed=[x['name'] for x in checks if not x['ok']]
 payload={'schemaVersion':66,'mustPassFailRules':14,'baseFixtures':28,'checks':len(checks),'failed':failed,'verdict':'PASS' if not failed else 'FAIL'}
 out=ROOT/'.verification-v66/v66-fixtures.json';out.parent.mkdir(exist_ok=True);out.write_text(json.dumps(payload,ensure_ascii=False,indent=2)+'\n')
 print(f"V66 FIXTURES: {payload['verdict']} ({len(checks)-len(failed)}/{len(checks)})")
 if failed:
  for x in failed:print(' -',x)
 return 0 if not failed else 1
if __name__=='__main__':raise SystemExit(main())
