#!/usr/bin/env bash
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
mkdir -p .verification-v64
run_child() {
  local name="$1"; shift
  "$@" > ".verification-v64/${name}.log" 2>&1
  local ec=$?
  cat ".verification-v64/${name}.log"
  return $ec
}
exits=()
run_child v07 python3 tools/verify_designsystem_v07.py; exits+=($?)
run_child v08 python3 tools/verify_designsystem_v08.py; exits+=($?)
run_child exceptions python3 tools/validate_designsystem_exceptions.py; exits+=($?)
run_child home python3 tools/verify_designsystem_v61_home.py --output .verification-v64/home.json; exits+=($?)
run_child reports-v62-historical python3 tools/run_v63_historical_gate_v64.py --gate v62; exits+=($?)
run_child settings-v63-historical python3 tools/run_v63_historical_gate_v64.py; exits+=($?)
run_child adoption-v64 python3 tools/verify_designsystem_v64_adoption.py --output .verification-v64/adoption.json; exits+=($?)
run_child ratchet python3 tools/verify_designsystem_ratchet.py --session 64 --output .verification-v64/ratchet.json; exits+=($?)
run_child fixtures60 python3 tools/test_designsystem_verification_v60.py --output .verification-v64/fixtures60.json; exits+=($?)
run_child fixtures61 python3 tools/test_designsystem_verification_v61.py --output .verification-v64/fixtures61.json; exits+=($?)
run_child fixtures62 python3 tools/test_designsystem_verification_v62.py --output .verification-v64/fixtures62.json; exits+=($?)
run_child fixtures63 python3 tools/run_v63_historical_gate_v64.py --gate fixtures63; exits+=($?)
run_child fixtures64 python3 tools/test_designsystem_verification_v64.py --output .verification-v64/fixtures64.json; exits+=($?)
run_child v58 bash scripts/verify-v58-static.sh; exits+=($?)
# Coverage/wave/evidence file checks are part of the v64 parent.
run_child evidence python3 - <<'PY'; exits+=($?)
import csv, json, hashlib
from pathlib import Path
root=Path('.')
required=['DESIGN_SYSTEM_V64_TOUCH_PRESTATE.json','DESIGN_SYSTEM_UI_COVERAGE_v64.csv','COMPONENT_ADOPTION_WAVES_v64.md']
missing=[x for x in required if not (root/x).is_file()]
if missing:
    print('V64 EVIDENCE: FAIL missing',missing); raise SystemExit(1)
rows=list(csv.DictReader((root/'DESIGN_SYSTEM_UI_COVERAGE_v64.csv').open(encoding='utf-8',newline='')))
v64=[r for r in rows if r['target_session']=='v64']
status={r['v64_status'] for r in v64}
if len(v64)!=28 or sum(r['v64_status']=='MIGRATED_V64' for r in v64)!=19 or sum(r['v64_status']=='VERIFIED_CLEAN_CARRY_FORWARD' for r in v64)!=9:
    print('V64 EVIDENCE: FAIL coverage states',len(v64)); raise SystemExit(1)
if sum(r['v64_status']=='COMPATIBLE_EXTENSION_V64' for r in rows)!=4:
    print('V64 EVIDENCE: FAIL provider states'); raise SystemExit(1)
print('V64 EVIDENCE: PASS (28 rows / 19 migrated / 9 clean / 4 providers)')
PY
result=0
for e in "${exits[@]}"; do
  if [[ "$e" -eq 2 ]]; then result=2; break; fi
  if [[ "$e" -ne 0 ]]; then result=1; fi
done
if [[ "$result" -eq 0 ]]; then
  echo "VERIFY_V64_STATIC: PASS"
else
  echo "VERIFY_V64_STATIC: FAIL exit=$result child_exits=${exits[*]}"
fi
exit "$result"
