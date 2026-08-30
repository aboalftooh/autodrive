#!/usr/bin/env bash
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
mkdir -p .verification-v63
run_child() {
  local name="$1"; shift
  "$@" > ".verification-v63/${name}.log" 2>&1
  local ec=$?
  cat ".verification-v63/${name}.log"
  return $ec
}
exits=()
run_child v07 python3 tools/verify_designsystem_v07.py; exits+=($?)
run_child v08 python3 tools/verify_designsystem_v08.py; exits+=($?)
run_child exceptions python3 tools/validate_designsystem_exceptions.py; exits+=($?)
run_child home python3 tools/verify_designsystem_v61_home.py --output .verification-v63/home.json; exits+=($?)
run_child reports-v62-historical python3 tools/run_v62_historical_gate_v63.py; exits+=($?)
run_child settings python3 tools/verify_designsystem_v63_settings.py --output .verification-v63/settings.json; exits+=($?)
run_child ratchet python3 tools/verify_designsystem_ratchet.py --session 63 --output .verification-v63/ratchet.json; exits+=($?)
run_child fixtures60 python3 tools/test_designsystem_verification_v60.py --output .verification-v63/fixtures60.json; exits+=($?)
run_child fixtures61 python3 tools/test_designsystem_verification_v61.py --output .verification-v63/fixtures61.json; exits+=($?)
run_child fixtures62 python3 tools/test_designsystem_verification_v62.py --output .verification-v63/fixtures62.json; exits+=($?)
run_child fixtures63 python3 tools/test_designsystem_verification_v63.py --output .verification-v63/fixtures63.json; exits+=($?)
run_child v58 bash scripts/verify-v58-static.sh; exits+=($?)
result=0
for e in "${exits[@]}"; do
  if [[ "$e" -eq 2 ]]; then result=2; break; fi
  if [[ "$e" -ne 0 ]]; then result=1; fi
done
if [[ "$result" -eq 0 ]]; then
  echo "VERIFY_V63_STATIC: PASS"
else
  echo "VERIFY_V63_STATIC: FAIL exit=$result child_exits=${exits[*]}"
fi
exit "$result"
