#!/usr/bin/env python3
from __future__ import annotations
import importlib.util, shutil, subprocess, sys, tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SNAP=ROOT/'tools/fixtures/v63/v62_snapshot_flat'
VERIFIER=ROOT/'tools/verify_designsystem_v62_reports.py'

def copy_file(src:Path,dst:Path):
    dst.parent.mkdir(parents=True,exist_ok=True); shutil.copy2(src,dst)

def main()->int:
    spec=importlib.util.spec_from_file_location('v62',VERIFIER); v=importlib.util.module_from_spec(spec); spec.loader.exec_module(v)
    with tempfile.TemporaryDirectory(prefix='autodrive-v62-historical-') as td:
        shadow=Path(td)
        # Current DS tree, then restore exactly the two files changed by v63.
        shutil.copytree(ROOT/'core/designsystem/src/main', shadow/'core/designsystem/src/main')
        copy_file(SNAP/'SettingsPatterns.kt.txt', shadow/v.SETTINGS if hasattr(v,'SETTINGS') else shadow/'core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/patterns/settings/SettingsPatterns.kt')
        copy_file(SNAP/'DataComponents.kt.txt', shadow/'core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/data/DataComponents.kt')
        # Minimal files consumed/protected by the immutable v62 verifier.
        needed=set(v.FILES.values())|set(v.PROTECTED.keys())|{'DESIGN_SYSTEM_BASELINE_v59.json','DESIGN_SYSTEM_UI_COVERAGE_v61.csv','DESIGN_SYSTEM_UI_COVERAGE_v62.csv'}
        for rel in sorted(needed): copy_file(ROOT/rel,shadow/rel)
        copy_file(SNAP/'designsystem-ratchet-state.json',shadow/'core/designsystem/verification/designsystem-ratchet-state.json')
        cp=subprocess.run([sys.executable,str(VERIFIER),'--root',str(shadow)],text=True,capture_output=True)
        sys.stdout.write(cp.stdout); sys.stderr.write(cp.stderr)
        return cp.returncode
if __name__=='__main__': raise SystemExit(main())
