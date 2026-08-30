#!/usr/bin/env bash
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
mkdir -p .verification-v62
run_child() {
  local name="$1"; shift
  "$@" > ".verification-v62/${name}.log" 2>&1
  local ec=$?
  cat ".verification-v62/${name}.log"
  return $ec
}
exits=()
run_child v07 python3 tools/verify_designsystem_v07.py; exits+=($?)
run_child v08 python3 tools/verify_designsystem_v08.py; exits+=($?)
run_child exceptions python3 tools/validate_designsystem_exceptions.py; exits+=($?)
run_child home python3 tools/verify_designsystem_v61_home.py --output .verification-v62/home.json; exits+=($?)
run_child reports python3 tools/verify_designsystem_v62_reports.py --output .verification-v62/reports.json; exits+=($?)
run_child ratchet python3 tools/verify_designsystem_ratchet.py --session 62 --output .verification-v62/ratchet.json; exits+=($?)
run_child fixtures60 python3 tools/test_designsystem_verification_v60.py --output .verification-v62/fixtures60.json; exits+=($?)
run_child fixtures61 python3 tools/test_designsystem_verification_v61.py --output .verification-v62/fixtures61.json; exits+=($?)
run_child fixtures62 python3 tools/test_designsystem_verification_v62.py --output .verification-v62/fixtures62.json; exits+=($?)
run_child v58 bash scripts/verify-v58-static.sh; exits+=($?)
result=0
for e in "${exits[@]}"; do
  if [[ "$e" -eq 2 ]]; then result=2; break; fi
  if [[ "$e" -ne 0 ]]; then result=1; fi
done
if [[ "$result" -eq 0 ]]; then
  echo "VERIFY_V62_STATIC: PASS"
else
  echo "VERIFY_V62_STATIC: FAIL exit=$result child_exits=${exits[*]}"
fi
exit "$result"
