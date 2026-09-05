#!/usr/bin/env python3
from __future__ import annotations
import argparse, csv, hashlib, json, re
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
TARGET_IDS = {
    'DS59-REPORTS-001','DS59-REPORTS-002','DS59-REPORTS-003',
    'DS59-MAT-6FEF7AF6CB','DS59-MAT-D6C993673C','DS59-MAT-FEEC3D97F3',
    'DS59-MAT-D9E88F5DA5','DS59-MAT-83500757C5','DS59-MAT-40A566C71D','DS59-MAT-A0FB9B1576',
}
REPORT_CANDIDATES = {
    'DS59-MATC-EA81ACB322','DS59-MATC-B28DBA9BD1','DS59-MATC-9B9E4A62B6',
    'DS59-MATC-353C4BA208','DS59-MATC-D44DE38681',
}
FILES = {
    'activity':'app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt',
    'competition':'app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/CompetitionHistoryScreen.kt',
    'detail':'app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/InvoiceDetailScreen.kt',
    'list':'app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/InvoiceListScreen.kt',
    'weekly':'app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/WeeklyCommissionsScreen.kt',
}
PROTECTED = {
    'DESIGN_SYSTEM_BASELINE_v59.md':'f6b59f0e09c58262a19438462da309a6739d9abe0e861c715fb21f8971a49438',
    'DESIGN_SYSTEM_BASELINE_v59.json':'906c12831752daa2f7fce190a3aab37a75817c415b055c09ab367d8207248abc',
    'DESIGN_SYSTEM_UI_COVERAGE_v59.csv':'191d4497d0433ba078fb7b71bee080763cd013cc64f34ab3220477649d568dea',
    'DESIGN_SYSTEM_V1_CONTRACT_REGISTRY_v59.md':'ed758157e064bb1f8fc61d13e2efdd4b09938ec54640f49d603e0df18a215e0d',
    'DESIGN_SYSTEM_UI_COVERAGE_v61.csv':'fc735fe34752028aae42c0ed87d47c1a958da8cb5dbb3185c585f72ce7bc4763',
    'DESIGN_SYSTEM_VERIFICATION_v61.json':'85d4912c0cc699546c001da6a95c3047c844e10879f5faecc6da859fc09de7e4',
    'DESIGN_SYSTEM_VERIFICATION_v61.md':'0510c749f2a2c7f39b08d5118a329962a857628a57538ea45e4291cdb0f45b47',
    'AutoDrive-v61-report.md':'8c13ca6880f5032f577089ddb9bb5bca4f166ec2bdf592bcf1e7d4ad76913827',
    'app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/WinWeeksScreen.kt':'be26af99b1a84fef2f4f227ca7f11447ba9103f259aebdeb83879a8922e0fb90',
    'app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ReportsViewModel.kt':'83bde6ed98963f36bc328eed5c16bd02b48ce0507fa3a2499bbfe2adac77d34a',
    'app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ReportsUiState.kt':'9ff6d6ce4a90be272c292b2b6a8c8efb6b66fecb31deeb8ae8d2701f3a2e7594',
    'app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/InvoiceDetailViewModel.kt':'eebf0f80dc00551b07ad87ffed4e77cbbe3aae4b85dae34aedb5718392bd186f',
    'app/src/main/kotlin/com/autodrive/app/navigation/NavigationGraphs.kt':'950c3dcfd884fe10481d0bb7fe5179bc85b8f20253d998e8a670d678661074cc',
    'app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityScreen.kt':'61958995f7f3a34aa253e1410de7aec42e1f2fa6312fe77e70770f51bd39365f',
    'app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityUiState.kt':'7b2d3307cf7db850db12f0c13458d83ae29226f2786fee7322e1239756e0ac39',
    'app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/recent/RecentActivityViewModel.kt':'b1e270c103842a41ee2bf9608bf5d86a305aa8f9d9632a43e5596552b2be8031',
    'feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionReportScreen.kt':'667226b3d182a8f043073e523262bc32e55f001ed3d54fa2e9f28e580d5bc539',
    'app/src/main/kotlin/com/autodrive/app/feature/reports/data/InvoiceDetailRepositoryImpl.kt':'cb8e27ccfe4f78e650104677f0b38be1b4443d928d61c398645a0925badd75e7',
    'app/src/main/kotlin/com/autodrive/app/feature/reports/domain/repository/InvoiceDetailRepository.kt':'daf78e61dcd075de789b7f5937eb91d7c7e2c90f70fed7548c2f3bb9850c9740',
    'app/src/main/kotlin/com/autodrive/app/feature/reports/domain/usecase/GetInvoiceDetailsUseCase.kt':'e120c4fd70e55b198b494da70f31825f1d1c432b375e43245d33ed50df31071d',
}
REGIONS = [
    ('competition-vm', FILES['competition'], 'class CompetitionHistoryViewModel', '@Composable\nfun CompetitionHistoryScreen', 'f2014f00582746c61f59ebb5abd2948c6547c3acc7c0a3294c4e4b52a5d5d519'),
    ('invoice-list-vm', FILES['list'], 'class InvoiceListViewModel', '// ── Screen', 'ca3e2c1b7fdb29e96af837f05f6afb6b40789d4cb84797e50f1dc7a68475518d'),
    ('weekly-vm', FILES['weekly'], 'class WeeklyCommissionsViewModel', '// ── State for visible count', 'cc4245f4ef14f9fd3a2c1348a2047394ea7d72bab81a1172fb0eb16f2acea4d8'),
]
DS_MAIN_DIGEST='521965fba8f524e55e5d58d91224d39545a128c143fa3942baf3d5bef4c99634'

def sha(p: Path) -> str: return hashlib.sha256(p.read_bytes()).hexdigest()
def tree_digest(root: Path) -> str:
    base=root/'core/designsystem/src/main'; rows=[]
    for p in sorted(base.rglob('*')):
        if p.is_file(): rows.append(f"{p.relative_to(base).as_posix()}\t{sha(p)}")
    return hashlib.sha256('\n'.join(rows).encode()).hexdigest()
def function_region(src: str, start: str, end: str) -> str:
    return src[src.index(start):src.index(end)]
def source_contract_errors(src: dict[str,str]) -> list[str]:
    e=[]; a=src['activity']; c=src['competition']; d=src['detail']; l=src['list']; w=src['weekly']
    reports=function_region(a,'private fun ReportsContent','private fun CurrentWeekHero')
    financial=function_region(a,'private fun FinancialStatus','private fun ReportDetails')
    pair=function_region(a,'private fun ResponsiveReportPair','private fun TrendCard')
    details=function_region(a,'private fun ReportDetails','private fun HistoricalAchievement')
    if 'FinancialStatus(' not in reports or 'ReportStatTile(' not in financial: e.append('REPORTS-001 ReportStatTile not functionally reachable from ReportsContent')
    if 'widthIn(max = AutoDriveContentWidth.Dashboard)' not in reports: e.append('REPORTS-002 Dashboard width not functionally applied to reports body')
    if '840.dp' in a: e.append('REPORTS-002 raw 840.dp forbidden')
    if not all(x in pair for x in ('BoxWithConstraints','maxWidth >= AutoDriveContentWidth.ReportTwoColumn','Row(','Column(')): e.append('REPORTS-003 responsive token branch incomplete')
    if '360.dp' in a: e.append('REPORTS-003 raw 360.dp forbidden')
    order=[reports.find(x) for x in ('CurrentWeekHero','PreviousWeekComparison','FinancialStatus','ReportDetails','HistoricalAchievement')]
    if any(x<0 for x in order) or order != sorted(order): e.append('REPORTS section order changed')
    if 'competitionAvailability == CompetitionAvailability.ACTIVE' not in details: e.append('competition is not ACTIVE-only')
    if 'onNavigateInvoiceList("current")' not in details: e.append('current invoice route changed')
    if 'onClick = onNavigateBalance' not in financial: e.append('balance navigation changed')
    pending=financial[financial.find('label = "العمولات المعلقة"'):]
    if 'onClick' in pending.split(')',1)[0]: e.append('pending metric became clickable')
    hero=function_region(a,'private fun CurrentWeekHero','private fun HeroSupportingMetric')
    for x in ('state.currentWeekPurchases','state.currentWeekCommissions','state.currentWeekInvoiceCount','state.currentWeekLabel'):
        if x not in hero: e.append(f'current-week hero lost {x}')
    if 'lifetimeCommissions' in hero: e.append('current-week hero uses lifetime values')
    for x in ('TrendDirection.UP -> "أعلى','TrendDirection.DOWN -> "أقل','TrendDirection.FLAT -> "بدون تغيير"','TrendDirection.NEW -> "نشاط جديد"'):
        if x not in a: e.append(f'trend semantic lost: {x}')
    # Exact primitive replacements.
    checks=[(c,'TextButton(',2,'AutoDriveTextButton('),(d,'IconButton(',1,'AutoDriveIconButton('),(d,'FloatingActionButton(',1,'AutoDriveFab('),(d,'HorizontalDivider(',1,'AutoDriveDivider('),(l,'TextButton(',1,'AutoDriveTextButton('),(w,'TextButton(',1,'AutoDriveTextButton(')]
    for text,raw,needed,repl in checks:
        if re.search(rf'(?<![A-Za-z0-9_]){re.escape(raw)}',text): e.append(f'raw primitive remains: {raw}')
        if text.count(repl) < needed: e.append(f'official replacement missing: {repl}')
    if c.count('CircularProgressIndicator(')!=2: e.append('CompetitionHistory CPI candidates changed')
    if d.count('TopAppBar(')!=1 or d.count('CircularProgressIndicator(')!=1: e.append('InvoiceDetail accepted candidates changed')
    for needle in ('COMP_PAGE = 10','_offset','_hasMore','viewModel::loadMore','row.myRank == null','"لم تشارك"'):
        if needle not in c: e.append(f'CompetitionHistory invariant missing: {needle}')
    for needle in ('InvoicePdfGenerator.generateAndPrint','context = context','entry = entry.toPdfEntry()','items = items.map { it.toPdfItem() }','invoiceStatus = inv.status.name','contentDescription = "رجوع"','contentDescription = "طباعة"'):
        if needle not in d: e.append(f'InvoiceDetail invariant missing: {needle}')
    for needle in ('showOlderWeek()','onNavigateInvoiceDetail(inv.id)','weekMode','offset 0'):
        if needle=='offset 0':
            if 'else 0' not in l: e.append('InvoiceList default offset changed')
        elif needle not in l: e.append(f'InvoiceList invariant missing: {needle}')
    for needle in ('private const val PAGE = 10','visibleCount by remember { mutableIntStateOf(PAGE) }','visibleCount += PAGE','allRows.take(visibleCount)'):
        if needle not in w: e.append(f'WeeklyCommissions invariant missing: {needle}')
    return e

def run(root: Path) -> tuple[int,dict[str,Any]]:
    errors=[]; checks=[]
    def ck(cid, ok, msg):
        checks.append({'id':cid,'status':'PASS' if ok else 'FAIL','message':msg})
        if not ok: errors.append(f'{cid}: {msg}')
    src={k:(root/v).read_text(encoding='utf-8') for k,v in FILES.items()}
    for msg in source_contract_errors(src): ck('REPORTS-CONTRACT',False,msg)
    if not any(x['id']=='REPORTS-CONTRACT' for x in checks): ck('REPORTS-CONTRACT',True,'Reports source contract satisfied')
    # Baseline target identity and coverage authority.
    baseline=json.loads((root/'DESIGN_SYSTEM_BASELINE_v59.json').read_text())
    base_ids={x.get('finding_id') for x in baseline.get('findings',[]) if x.get('classification')=='CONFIRMED_VIOLATION'}
    ck('REPORTS-TARGET-IDS', TARGET_IDS <= base_ids and len(TARGET_IDS)==10, 'exact 10 v62 target IDs exist in immutable baseline')
    with (root/'DESIGN_SYSTEM_UI_COVERAGE_v61.csv').open(newline='',encoding='utf-8') as f: rows=list(csv.DictReader(f))
    targets=[r for r in rows if r.get('target_session')=='v62']
    ck('REPORTS-COVERAGE-V61',len(targets)==6,'v61 coverage contains exactly six v62 rows')
    # Candidate policy and state policy works pre/post acceptance.
    state=json.loads((root/'core/designsystem/verification/designsystem-ratchet-state.json').read_text())
    cand_ids={x.get('findingId') for x in state.get('acceptedCandidates',[])}
    ck('REPORTS-CANDIDATES',REPORT_CANDIDATES <= cand_ids and len(state.get('acceptedCandidates',[]))==18,'five Reports candidates retained; accepted candidate total=18')
    if state.get('acceptedVersion')=='v61': ck('REPORTS-RATCHET-PRE',len(state.get('confirmedFindings',[]))==57,'pre-accept state remains v61/57')
    elif state.get('acceptedVersion')=='v62':
        ids={x.get('findingId') for x in state.get('confirmedFindings',[])}
        ck('REPORTS-RATCHET-POST',len(ids)==47 and not (TARGET_IDS & ids) and 'DS59-SETTINGS-001' in ids,'post-accept state is v62/47 with exact target set resolved')
    else: ck('REPORTS-RATCHET-STATE',False,f"unexpected acceptedVersion={state.get('acceptedVersion')}")
    # Protected immutable files.
    for rel,expected in PROTECTED.items():
        p=root/rel; ck('PROTECTED-HASH',p.is_file() and sha(p)==expected,rel)
    ck('PROTECTED-DS-MAIN',tree_digest(root)==DS_MAIN_DIGEST,'core/designsystem/src/main unchanged')
    for name,rel,start,end,expected in REGIONS:
        s=(root/rel).read_text(); region=function_region(s,start,end); ck('PROTECTED-REGION',hashlib.sha256(region.encode()).hexdigest()==expected,name)
    # v62 coverage, when present, must close 6/6.
    cov=root/'DESIGN_SYSTEM_UI_COVERAGE_v62.csv'
    if cov.is_file():
        with cov.open(newline='',encoding='utf-8') as f: vr=list(csv.DictReader(f))
        t=[r for r in vr if r.get('target_session')=='v62']
        ck('REPORTS-COVERAGE-V62',len(t)==6 and all(r.get('v62_open_confirmed')=='0' and r.get('v62_status') in ('MIGRATED_V62','VERIFIED_CLEAN_CARRY_FORWARD') for r in t),'v62 Reports rows closed 6/6')
    payload={'schemaVersion':62,'session':62,'tool':'verify_designsystem_v62_reports','targetResolvedIds':sorted(TARGET_IDS),'contractChecks':checks,'errors':errors,'verdict':'PASS' if not errors else 'FAIL_REPORTS_V62_CONTRACT'}
    return (0 if not errors else 1),payload

def main(argv=None):
    ap=argparse.ArgumentParser(); ap.add_argument('--root',type=Path,default=ROOT); ap.add_argument('--output',type=Path); ap.add_argument('--json',action='store_true'); a=ap.parse_args(argv)
    try: code,p=run(a.root.resolve())
    except (OSError,UnicodeError,ValueError,json.JSONDecodeError) as exc: code=2; p={'schemaVersion':62,'session':62,'tool':'verify_designsystem_v62_reports','errors':[f'{type(exc).__name__}: {exc}'],'verdict':'TOOL_ERROR'}
    if a.output:
        out=a.output if a.output.is_absolute() else a.root.resolve()/a.output; out.parent.mkdir(parents=True,exist_ok=True); out.write_text(json.dumps(p,ensure_ascii=False,indent=2)+'\n')
    if a.json: print(json.dumps(p,ensure_ascii=False,sort_keys=True))
    else:
        print(f"V62 REPORTS STATIC VERIFICATION: {p['verdict']}")
        if code==0: print(' - exact target IDs: 10\n - Reports rows: 6\n - protected hashes/regions: PASS')
        else:
            for x in p.get('errors',[]): print(' -',x)
    return code
if __name__=='__main__': raise SystemExit(main())
