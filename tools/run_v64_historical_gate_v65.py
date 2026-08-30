#!/usr/bin/env python3
from __future__ import annotations
import argparse, json, shutil, subprocess, sys, tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

def main(argv=None):
    ap=argparse.ArgumentParser(); ap.add_argument('--root',type=Path,default=ROOT); ap.add_argument('--output',type=Path); ap.add_argument('--gate',choices=['v64','v63','v62','fixtures63','stack'],default='v64'); a=ap.parse_args(argv)
    root=a.root.resolve(); manifest=json.loads((root/'tools/fixtures/v65/v64_snapshot/manifest.json').read_text())
    with tempfile.TemporaryDirectory(prefix='v64-shadow-') as td:
        shadow=Path(td)/'root'
        shutil.copytree(root,shadow,ignore=shutil.ignore_patterns('.gradle','build','.verification-v65','*.zip'))
        snap=root/'tools/fixtures/v65/v64_snapshot'
        for rec in manifest['files']:
            dst=shadow/rec['path']; dst.parent.mkdir(parents=True,exist_ok=True); shutil.copy2(snap/rec['snapshot'],dst)
        shutil.copy2(snap/'designsystem-ratchet-state-v64.json',shadow/'core/designsystem/verification/designsystem-ratchet-state.json')
        if a.gate=='stack':
            cmds=[
                [sys.executable,str(shadow/'tools/verify_designsystem_v64_adoption.py'),'--root',str(shadow),'--output',str(shadow/'v64-shadow-report.json')],
                [sys.executable,str(shadow/'tools/run_v63_historical_gate_v64.py'),'--gate','stack'],
            ]
            detail=[]; ok=True; cp=None
            for cmd in cmds:
                cp=subprocess.run(cmd,cwd=shadow,text=True,capture_output=True)
                if cp.returncode:
                    ok=False; detail=[cp.stdout,cp.stderr]; break
            historical='62-64'
        elif a.gate=='v64':
            out=shadow/'v64-shadow-report.json'
            cmd=[sys.executable,str(shadow/'tools/verify_designsystem_v64_adoption.py'),'--root',str(shadow),'--output',str(out)]
            cp=subprocess.run(cmd,text=True,capture_output=True)
            report=json.loads(out.read_text()) if out.exists() else {'verdict':'TOOL_ERROR','errors':[cp.stderr or cp.stdout]}
            ok=cp.returncode==0 and report.get('verdict')=='PASS'
            detail=report.get('errors',[])
            historical=64
        else:
            cmd=[sys.executable,str(shadow/'tools/run_v63_historical_gate_v64.py')]
            if a.gate=='v62': cmd += ['--gate','v62']
            elif a.gate=='fixtures63': cmd += ['--gate','fixtures63']
            cp=subprocess.run(cmd,cwd=shadow,text=True,capture_output=True)
            ok=cp.returncode==0
            detail=[] if ok else [cp.stdout,cp.stderr]
            historical=62 if a.gate=='v62' else 63
        payload={'schemaVersion':65,'session':65,'historicalSession':historical,'requestedGate':a.gate,'shadowVerifierReturnCode':cp.returncode,'shadowVerifierErrors':detail,'verdict':'PASS' if ok else 'FAIL_HISTORICAL_GATE'}
    if a.output:
        outp=a.output if a.output.is_absolute() else root/a.output; outp.parent.mkdir(parents=True,exist_ok=True); outp.write_text(json.dumps(payload,ensure_ascii=False,indent=2)+'\n')
    print(f"HISTORICAL GATE {a.gate.upper()} VIA V64 SHADOW V65: {payload['verdict']}")
    if payload['verdict']!='PASS':
        print(' - returnCode=',payload['shadowVerifierReturnCode']); [print(' -',x) for x in payload.get('shadowVerifierErrors',[])[:20]]
    return 0 if payload['verdict']=='PASS' else 1
if __name__=='__main__': raise SystemExit(main())
