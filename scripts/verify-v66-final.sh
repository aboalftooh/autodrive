#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
bash scripts/verify-v66-static.sh
python - <<'PY'
import json,sys
v=json.load(open('DESIGN_SYSTEM_VERIFICATION_v66.json'))
if not v.get('runtimeFinalVerified') or not v.get('fullV66Completion') or v.get('finalVerdict')!='DESIGN_SYSTEM_V66_ZERO_DRIFT_COMPLETE':
    print('VERIFY_V66_FINAL: FAIL_RUNTIME_REQUIRED')
    sys.exit(1)
print('VERIFY_V66_FINAL: PASS')
PY
