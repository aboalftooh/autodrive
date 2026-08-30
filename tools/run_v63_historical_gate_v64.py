#!/usr/bin/env python3
from __future__ import annotations
import argparse, json, shutil, subprocess, sys, tempfile
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
SNAP = ROOT / 'tools/fixtures/v64/v63_snapshot'

def main(argv=None) -> int:
    ap=argparse.ArgumentParser(); ap.add_argument('--gate',choices=['v63','v62','fixtures63','stack'],default='v63'); a=ap.parse_args(argv)
    manifest = json.loads((SNAP/'manifest.json').read_text(encoding='utf-8'))
    with tempfile.TemporaryDirectory(prefix='autodrive-v63-historical-') as td:
        shadow = Path(td)
        shutil.copytree(ROOT, shadow, dirs_exist_ok=True, ignore=shutil.ignore_patterns('build','.gradle','.verification-v61','.verification-v62','.verification-v63','.verification-v64'))
        for rec in manifest['files']:
            dst = shadow / rec['path']; dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(SNAP / rec['snapshot'], dst)
        if a.gate=='stack':
            cmds=[
                [sys.executable, str(shadow/'tools/verify_designsystem_v63_settings.py'), '--root', str(shadow)],
                [sys.executable, str(shadow/'tools/run_v62_historical_gate_v63.py')],
                [sys.executable, str(shadow/'tools/test_designsystem_verification_v63.py'), '--root', str(shadow)],
            ]
            rc=0
            for cmd in cmds:
                cp=subprocess.run(cmd,cwd=shadow,text=True,capture_output=True)
                sys.stdout.write(cp.stdout); sys.stderr.write(cp.stderr)
                if cp.returncode: rc=cp.returncode; break
            return rc
        if a.gate=='v62': cmd=[sys.executable, str(shadow/'tools/run_v62_historical_gate_v63.py')]
        elif a.gate=='fixtures63': cmd=[sys.executable, str(shadow/'tools/test_designsystem_verification_v63.py'), '--root', str(shadow)]
        else: cmd=[sys.executable, str(shadow/'tools/verify_designsystem_v63_settings.py'), '--root', str(shadow)]
        cp = subprocess.run(cmd, cwd=shadow, text=True, capture_output=True)
        sys.stdout.write(cp.stdout); sys.stderr.write(cp.stderr)
        return cp.returncode
if __name__ == '__main__': raise SystemExit(main())
