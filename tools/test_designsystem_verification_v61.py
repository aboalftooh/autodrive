#!/usr/bin/env python3
from __future__ import annotations
import argparse, json, sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

def main(argv=None):
    ap=argparse.ArgumentParser(); ap.add_argument('--root',type=Path,default=ROOT); ap.add_argument('--output',type=Path)
    args=ap.parse_args(argv); root=args.root.resolve(); sys.path.insert(0,str(root/'tools'))
    try:
        from verify_designsystem_ratchet import scan_text
        mapping=json.loads((root/'core/designsystem/verification/primitive-mapping.json').read_text())
    except Exception as exc:
        print('V61 FIXTURES: TOOL_ERROR',exc); return 2
    cases=[
        ('one_off_dp','DS-SPACE-001','val x = 13.dp',0),
        ('repeated_raw_dp','DS-SPACE-001','val a=16.dp\nval b=16.dp\nval c=16.dp',1),
        ('token_spacing','DS-SPACE-001','val a=AutoDriveSpace.LG\nval b=AutoDriveSpace.LG\nval c=AutoDriveSpace.LG',0),
        ('governed_shape','DS-SHAPE-001','val a=AutoDriveRadius.MediumShape\nval b=AutoDriveRadius.MediumShape',0),
        ('repeated_raw_shape','DS-SHAPE-001','val a=RoundedCornerShape(12.dp)\nval b=RoundedCornerShape(12.dp)',1),
        ('governed_border','DS-BORDER-001','val a=BorderStroke(AutoDriveBorder.Thin, AutoDriveBorderColor.Default)\nval b=BorderStroke(AutoDriveBorder.Thin, AutoDriveBorderColor.Default)',0),
        ('repeated_raw_border','DS-BORDER-001','val a=BorderStroke(1.dp, Color.Red)\nval b=BorderStroke(1.dp, Color.Red)',1),
        ('theme_typography','DS-TYPE-001','val a=MaterialTheme.typography.bodyMedium',0),
        ('repeated_raw_sp','DS-TYPE-001','val a=14.sp\nval b=14.sp',1),
        ('raw_color','DS-COLOR-001','val a=Color(0xFF123456)',1),
    ]
    results=[]
    for name,rule,source,expected in cases:
        findings,candidates=scan_text(source,f'tools/designsystem-fixtures/v61/{name}.kt',rule,mapping)
        actual=1 if findings else 0
        results.append({'id':name,'rule':rule,'expected':expected,'actual':actual,'findings':len(findings),'candidates':len(candidates),'status':'PASS' if actual==expected else 'FAIL'})
    failures=[x for x in results if x['status']!='PASS']
    payload={'schemaVersion':61,'tool':'test_designsystem_verification_v61','results':results,'failures':failures,'verdict':'PASS' if not failures else 'FAIL_FIXTURE_CONTRACT'}
    if args.output:
        out=args.output if args.output.is_absolute() else root/args.output; out.write_text(json.dumps(payload,ensure_ascii=False,indent=2)+'\n')
    print(f"V61 FIXTURES: {payload['verdict']} ({len(results)-len(failures)}/{len(results)})")
    for f in failures: print(' -',f)
    return 0 if not failures else 1
if __name__=='__main__': raise SystemExit(main())
