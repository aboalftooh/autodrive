#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
OUT=".verification-v65/parent"
mkdir -p "$OUT"

python3 tools/verify_designsystem_v07.py
python3 tools/verify_designsystem_v08.py
python3 tools/validate_designsystem_exceptions.py
python3 tools/verify_designsystem_v61_home.py
# Historical stack executes run_v62_historical_gate_v63.py, run_v63_historical_gate_v64.py,
# test_designsystem_verification_v63.py and immutable v64 verification from the exact v64 shadow.
python3 tools/run_v64_historical_gate_v65.py --gate stack --output "$OUT/historical-stack.json"
python3 tools/verify_designsystem_v65_accessibility.py --output "$OUT/v65-a.json"
python3 tools/verify_designsystem_v65_accessibility.py --output "$OUT/v65-b.json"
python3 tools/verify_designsystem_ratchet.py --session 65 --output "$OUT/ratchet.json"
python3 tools/test_designsystem_verification_v60.py
python3 tools/test_designsystem_verification_v61.py
python3 tools/test_designsystem_verification_v62.py
python3 tools/test_designsystem_verification_v64.py
python3 tools/test_designsystem_verification_v65.py
bash scripts/verify-v58-static.sh
python3 - <<'PY'
import csv, hashlib, json
from pathlib import Path
root=Path('.')
def norm(p): return json.dumps(json.load(open(p,encoding='utf-8')),ensure_ascii=False,sort_keys=True,separators=(',',':')).encode()
a=norm('.verification-v65/parent/v65-a.json'); b=norm('.verification-v65/parent/v65-b.json')
assert a==b, 'FAIL_STATIC_DETERMINISM: v65 normalized JSON differs'
rows=list(csv.DictReader(open('DESIGN_SYSTEM_UI_COVERAGE_v65.csv',encoding='utf-8',newline='')))
assert len(rows)==56, 'FAIL_COVERAGE_COUNT'
counts={}
for r in rows: counts[r['v65_a11y_classification']]=counts.get(r['v65_a11y_classification'],0)+1
assert counts.get('RECLASSIFIED_V65')==4, 'FAIL_A11Y_SCOPE_RECONCILIATION'
assert counts.get('V65_NOT_APPLICABLE_VERIFIED')==3, 'FAIL_A11Y_SCOPE_RECONCILIATION'
state=json.load(open('core/designsystem/verification/designsystem-ratchet-state.json',encoding='utf-8'))
assert state.get('acceptedVersion') in {'v64','v65'}
assert len(state.get('confirmedFindings',[]))==0
assert len(state.get('acceptedCandidates',[]))==6
print('V65 COVERAGE/DETERMINISM: PASS')
PY

echo "VERIFY_V65_STATIC: PASS"
