#!/usr/bin/env python3
from __future__ import annotations
import argparse, json, sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

def load_sources(root):
    sys.path.insert(0,str(root/'tools'))
    from verify_designsystem_v62_reports import FILES, source_contract_errors
    return FILES, source_contract_errors, {k:(root/v).read_text() for k,v in FILES.items()}

def main(argv=None):
    ap=argparse.ArgumentParser(); ap.add_argument('--root',type=Path,default=ROOT); ap.add_argument('--output',type=Path); a=ap.parse_args(argv); root=a.root.resolve()
    try: FILES,check,base=load_sources(root)
    except Exception as exc: print('V62 FIXTURES: TOOL_ERROR',exc); return 2
    cases=[]
    def case(name, mutate, expected_fail=True):
        s=dict(base); mutate(s); errs=check(s); passed=bool(errs)==expected_fail; cases.append({'case':name,'detected':bool(errs),'errors':errs,'status':'PASS' if passed else 'FAIL'})
    # positive
    errs=check(base); cases.append({'case':'positive_current_tree','detected':bool(errs),'errors':errs,'status':'PASS' if not errs else 'FAIL'})
    case('remove_reportstattile',lambda s:s.__setitem__('activity',s['activity'].replace('ReportStatTile(','AutoDriveMetricCard(',1)))
    case('raw_840',lambda s:s.__setitem__('activity',s['activity'].replace('AutoDriveContentWidth.Dashboard','840.dp',1)))
    case('raw_360',lambda s:s.__setitem__('activity',s['activity'].replace('AutoDriveContentWidth.ReportTwoColumn','360.dp',1)))
    case('always_two_column',lambda s:s.__setitem__('activity',s['activity'].replace('if (maxWidth >= AutoDriveContentWidth.ReportTwoColumn) {','if (true) {',1)))
    case('competition_textbutton',lambda s:s.__setitem__('competition',s['competition'].replace('AutoDriveTextButton(','TextButton(',1)))
    case('detail_iconbutton',lambda s:s.__setitem__('detail',s['detail'].replace('AutoDriveIconButton(','IconButton(',1)))
    case('detail_fab',lambda s:s.__setitem__('detail',s['detail'].replace('AutoDriveFab(','FloatingActionButton(',1)))
    case('detail_divider',lambda s:s.__setitem__('detail',s['detail'].replace('AutoDriveDivider()','HorizontalDivider()',1)))
    case('list_textbutton',lambda s:s.__setitem__('list',s['list'].replace('AutoDriveTextButton(','TextButton(',1)))
    case('weekly_textbutton',lambda s:s.__setitem__('weekly',s['weekly'].replace('AutoDriveTextButton(','TextButton(',1)))
    case('competition_disabled',lambda s:s.__setitem__('activity',s['activity'].rsplit('competitionAvailability == CompetitionAvailability.ACTIVE',1)[0]+'competitionAvailability == CompetitionAvailability.DISABLED'+s['activity'].rsplit('competitionAvailability == CompetitionAvailability.ACTIVE',1)[1]))
    case('current_route_changed',lambda s:s.__setitem__('activity',s['activity'].replace('onNavigateInvoiceList("current")','onNavigateInvoiceList("all")',1)))
    failures=[x for x in cases if x['status']!='PASS']; payload={'schemaVersion':62,'tool':'test_designsystem_verification_v62','results':cases,'failures':failures,'verdict':'PASS' if not failures else 'FAIL_FIXTURE_CONTRACT'}
    if a.output:
        out=a.output if a.output.is_absolute() else root/a.output; out.parent.mkdir(parents=True,exist_ok=True); out.write_text(json.dumps(payload,ensure_ascii=False,indent=2)+'\n')
    print(f"V62 FIXTURES: {payload['verdict']} ({len(cases)-len(failures)}/{len(cases)})")
    for f in failures: print(' -',f)
    return 0 if not failures else 1
if __name__=='__main__': raise SystemExit(main())
