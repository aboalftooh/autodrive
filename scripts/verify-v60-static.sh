#!/usr/bin/env bash
set -u
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
TMP="$(mktemp -d "${TMPDIR:-/tmp}/autodrive-v60-gate.XXXXXX")"
trap 'rm -rf "$TMP"' EXIT
META="$TMP/meta.tsv"
: > "$META"

run_gate() {
  local id="$1" tool="$2"; shift 2
  local out="$TMP/${id}.out"
  "$@" >"$out" 2>&1
  local ec=$?
  printf '%s\t%s\t%s\t%s\n' "$id" "$tool" "$ec" "$out" >> "$META"
  cat "$out"
  return 0
}

# Mandatory order from SESSION_60_FINAL.md §68.
run_gate v07 tools/verify_designsystem_v07.py python3 tools/verify_designsystem_v07.py
run_gate v08 tools/verify_designsystem_v08.py python3 tools/verify_designsystem_v08.py
run_gate exceptions tools/validate_designsystem_exceptions.py python3 tools/validate_designsystem_exceptions.py
run_gate ratchet tools/verify_designsystem_ratchet.py python3 tools/verify_designsystem_ratchet.py --output "$TMP/ratchet.json"
run_gate fixtures tools/test_designsystem_verification_v60.py python3 tools/test_designsystem_verification_v60.py --output "$TMP/fixtures.json"
run_gate v58 scripts/verify-v58-static.sh bash scripts/verify-v58-static.sh

python3 - "$META" "$TMP" "$ROOT" <<'PY'
from pathlib import Path
import hashlib, json, sys
meta=Path(sys.argv[1]); tmp=Path(sys.argv[2]); root=Path(sys.argv[3])
commands={
 'v07':'python3 tools/verify_designsystem_v07.py',
 'v08':'python3 tools/verify_designsystem_v08.py',
 'exceptions':'python3 tools/validate_designsystem_exceptions.py',
 'ratchet':'python3 tools/verify_designsystem_ratchet.py',
 'fixtures':'python3 tools/test_designsystem_verification_v60.py',
 'v58':'bash scripts/verify-v58-static.sh',
}
def sha(p:Path)->str: return hashlib.sha256(p.read_bytes()).hexdigest()
children=[]; exits=[]
for line in meta.read_text().splitlines():
    gid,tool,ec_s,out_s=line.split('\t')
    ec=int(ec_s); exits.append(ec); out=Path(out_s)
    children.append({
      'id':gid,'tool':tool,'command':commands[gid],
      'sha256_of_tool':sha(root/tool),
      'exit_code':ec,'stdout_sha256':sha(out),
      'status':'PASS' if ec==0 else ('TOOL_ERROR' if ec==2 else 'FAIL'),
      'error_kind':None if ec==0 else ('TOOL_ERROR' if ec==2 else 'POLICY_FAILURE'),
    })
ratchet=json.loads((tmp/'ratchet.json').read_text()) if (tmp/'ratchet.json').is_file() else None
fixtures=json.loads((tmp/'fixtures.json').read_text()) if (tmp/'fixtures.json').is_file() else None
try: exceptions=json.loads((root/'core/designsystem/verification/designsystem-exceptions.json').read_text())
except Exception: exceptions=None
if any(x==2 for x in exits): final_exit=2; verdict='TOOL_ERROR'
elif any(x!=0 for x in exits): final_exit=1; verdict='FAIL'
else: final_exit=0; verdict='PASS'
payload={
 'schemaVersion':60,
 'session':60,
 'inputSourceSha256':'c499ce72edde8572dff1eed43a4ab1aaec7a50c8e5dd8fcc52b111390e8fbf26',
 'authorityHashes':{
   'baseline':sha(root/'DESIGN_SYSTEM_BASELINE_v59.json'),
   'coverage':sha(root/'DESIGN_SYSTEM_UI_COVERAGE_v59.csv'),
   'contractRegistry':sha(root/'DESIGN_SYSTEM_V1_CONTRACT_REGISTRY_v59.md'),
   'exceptionLedger':sha(root/'core/designsystem/verification/designsystem-exceptions.json'),
 },
 'subgates':children,
 'ratchetSummary':None if ratchet is None else {
   'verdict':ratchet.get('verdict'),
   'confirmedTotal':sum(r['current_total'] for r in ratchet.get('rules',[])),
   'newViolations':len(ratchet.get('newViolations',[])),
   'resolvedViolations':len(ratchet.get('resolvedViolations',[])),
   'knownCandidates':len(ratchet.get('candidates',[]))-len(ratchet.get('newCandidates',[])),
   'newCandidates':len(ratchet.get('newCandidates',[])),
   'touchedFiles':len(ratchet.get('touchedFiles',[])),
 },
 'fixtureSummary':None if fixtures is None else {
   'verdict':fixtures.get('verdict'),'fixtureCount':fixtures.get('fixtureCount'),
   'coreRuleOutcomes':fixtures.get('coreRuleOutcomes'),'candidateGuardOutcomes':fixtures.get('candidateGuardOutcomes'),
   'exceptionOutcomes':fixtures.get('exceptionOutcomes'),'toolErrorOutcomes':fixtures.get('toolErrorOutcomes'),
 },
 'exceptionSummary':{'activeCount':len(exceptions) if isinstance(exceptions,list) else None},
 'productionMutationSummary':None if ratchet is None else ratchet.get('productionMutation'),
 'uiHarnessStatus':'SOURCE_PRESENT_RUNTIME_PENDING',
 'finalVerdict':verdict,
 'exitCode':final_exit,
}
(root/'DESIGN_SYSTEM_VERIFICATION_v60.json').write_text(json.dumps(payload,ensure_ascii=False,indent=2)+'\n')
print(f"VERIFY_V60_STATIC: {verdict}")
print(f" - child exits: {','.join(str(x) for x in exits)}")
print(f" - report: DESIGN_SYSTEM_VERIFICATION_v60.json")
(root/'.v60-parent-exit').write_text(str(final_exit))
PY
exit "$(cat .v60-parent-exit)"
