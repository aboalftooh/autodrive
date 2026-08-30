#!/usr/bin/env python3
from __future__ import annotations
import hashlib,json,subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; SNAP=ROOT/'tools/fixtures/v66/v65_snapshot'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 m=json.loads((SNAP/'manifest.json').read_text()); bad=[]
 for x in m['files']:
  live=ROOT/x['path']; snap=SNAP/x['path']
  if not live.is_file() or sha(live)!=x['sha256'] or sha(snap)!=x['sha256']:bad.append(x['path'])
 if bad:
  print('V65 HISTORICAL GATE V66: FAIL');[print(' - drift',x) for x in bad];return 1
 cmds=[
  [sys.executable,'tools/verify_designsystem_v65_accessibility.py'],
  [sys.executable,'tools/test_designsystem_verification_v65.py'],
  [sys.executable,'tools/verify_designsystem_ratchet.py','--session','65'],
 ]
 logs=[]
 for cmd in cmds:
  p=subprocess.run(cmd,cwd=ROOT,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True);logs.append(p.stdout)
  if p.returncode:
   print('V65 HISTORICAL GATE V66: FAIL');print(p.stdout[-4000:]);return 1
 out=ROOT/'.verification-v66/v65-historical.log';out.parent.mkdir(exist_ok=True);out.write_text('\n'.join(logs))
 print(f'V65 HISTORICAL GATE V66: PASS ({len(m["files"])} immutable evidence files + v65 verifier/fixtures/ratchet)');return 0
if __name__=='__main__':raise SystemExit(main())
