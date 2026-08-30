#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
mkdir -p .verification-v66
run(){ local name="$1"; shift; echo "== $name =="; "$@" > ".verification-v66/${name}.log" 2>&1; tail -3 ".verification-v66/${name}.log"; }
run v58 bash scripts/verify-v58-static.sh
run fixtures60 python tools/test_designsystem_verification_v60.py
run fixtures61 python tools/test_designsystem_verification_v61.py
run fixtures62 python tools/test_designsystem_verification_v62.py
run fixtures64 python tools/test_designsystem_verification_v64.py
run fixtures65 python tools/test_designsystem_verification_v65.py
run fixtures66 python tools/test_designsystem_verification_v66.py
run home61 python tools/verify_designsystem_v61_home.py
run historical62to64 python tools/run_v64_historical_gate_v65.py --gate stack
run hist65 python tools/run_v65_historical_gate_v66.py
run v07 python tools/verify_designsystem_v07.py
run v08 python tools/verify_designsystem_v08.py
run exceptions python tools/validate_designsystem_exceptions.py
run v66 python tools/verify_designsystem_v66_final.py --output .verification-v66/v66-final-static.json
run ratchet66 python tools/verify_designsystem_ratchet.py --session 66 --output .verification-v66/ratchet-v66-preaccept.json
python tools/verify_designsystem_v66_final.py --json > .verification-v66/determinism-1.json
python tools/verify_designsystem_v66_final.py --json > .verification-v66/determinism-2.json
cmp -s .verification-v66/determinism-1.json .verification-v66/determinism-2.json || { echo 'V66 STATIC DETERMINISM: FAIL'; exit 1; }
echo 'V66 STATIC DETERMINISM: PASS'
echo 'VERIFY_V66_STATIC: PASS'
