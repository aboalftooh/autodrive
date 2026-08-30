#!/usr/bin/env python3
"""Failure-injection and false-positive fixture harness for Session 60."""
from __future__ import annotations
import argparse
import hashlib
import json
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
FIXTURES = ROOT / "tools/designsystem-fixtures"


def main(argv: list[str] | None = None) -> int:
    parser=argparse.ArgumentParser(); parser.add_argument("--root",type=Path,default=ROOT); parser.add_argument("--output",type=Path); parser.add_argument("--json",action="store_true",dest="json_mode")
    args=parser.parse_args(argv); root=args.root.resolve()
    sys.path.insert(0,str(root/"tools"))
    try:
        from verify_designsystem_ratchet import scan_text
        from validate_designsystem_exceptions import validate_document
        manifest=json.loads((root/"tools/designsystem-fixtures/fixture-manifest.json").read_text(encoding="utf-8"))
        mapping=json.loads((root/"core/designsystem/verification/primitive-mapping.json").read_text(encoding="utf-8"))
    except (OSError,json.JSONDecodeError,ImportError) as exc:
        print(f"V60 VERIFICATION FIXTURES: TOOL_ERROR\n - {type(exc).__name__}: {exc}")
        return 2

    results: list[dict[str,Any]]=[]
    def add(fid:str,rule:str,expected:int,actual:int,findings:list[dict[str,Any]]|None=None,detail:str=""):
        findings=findings or []
        results.append({
            "fixture_id":fid,"rule_id":rule,"expected_exit":expected,"actual_exit":actual,
            "finding_count":len(findings),"finding_fingerprints":sorted(f.get("fingerprint","") for f in findings),
            "status":"PASS" if expected==actual else "FAIL","detail":detail,
        })

    # 18 core outcomes + 5 candidate/false-positive guards.
    for rec in sorted(manifest.get("fixtures",[]),key=lambda x:x["fixture_id"]):
        path=root/rec["path"]
        try: source=path.read_text(encoding="utf-8")
        except OSError as exc:
            add(rec["fixture_id"],rec["rule_id"],rec["expected_exit"],2,detail=str(exc)); continue
        findings,candidates=scan_text(source,rec["path"],rec["rule_id"],mapping)
        actual=1 if findings else 0
        detail=f"confirmed={len(findings)} candidates={len(candidates)}"
        guard=rec.get("guard_expectation")
        if guard=="CANDIDATE_ONLY" and not (not findings and candidates): actual=1; detail += " guard-mismatch"
        if guard=="NO_FINDING" and findings: actual=1; detail += " guard-mismatch"
        add(rec["fixture_id"],rec["rule_id"],rec["expected_exit"],actual,findings,detail)

    # Per enforceable rule: required fixture input removed => tool/input error 2.
    enforceable=sorted({r["rule_id"] for r in manifest.get("fixtures",[]) if r["fixture_id"].endswith(":must_fail")})
    with tempfile.TemporaryDirectory(prefix="autodrive-v60-fixture-") as td:
        t=Path(td)
        for rule in enforceable:
            missing=t/f"{rule}.kt"
            try:
                missing.read_text(encoding="utf-8"); actual=0
            except OSError:
                actual=2
            add(f"{rule}:missing_required_input",rule,2,actual,detail="required fixture input deliberately absent")

    # Five exception-ledger fixtures (fixture ledgers never touch production ledger).
    exception_cases=[("valid",0),("expired",1),("wildcard",1),("blank_reason",1),("unknown_rule",1)]
    for name,expected in exception_cases:
        p=root/f"tools/designsystem-fixtures/exceptions/{name}.json"
        try:
            data=json.loads(p.read_text(encoding="utf-8")); errs=validate_document(data,root=root,current_version=60); actual=0 if not errs else 1
        except (OSError,json.JSONDecodeError):
            errs=["fixture parse/input error"]; actual=2
        add(f"EXCEPTION:{name}","DS-EXCEPTION-001",expected,actual,detail="; ".join(errs))

    # Three explicit ratchet tool-error injections: missing baseline, malformed JSON, missing mapping.
    verifier=root/"tools/verify_designsystem_ratchet.py"
    def subprocess_exit(extra:list[str]) -> int:
        cp=subprocess.run([sys.executable,str(verifier),"--root",str(root),*extra],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
        return cp.returncode
    add("TOOLERR:missing_baseline","TOOL",2,subprocess_exit(["--baseline","tools/designsystem-fixtures/does-not-exist.json"]))
    with tempfile.TemporaryDirectory(prefix="autodrive-v60-json-") as td:
        bad=Path(td)/"bad.json"; bad.write_text("{ malformed",encoding="utf-8")
        add("TOOLERR:malformed_json","TOOL",2,subprocess_exit(["--mapping",str(bad)]))
    add("TOOLERR:missing_mapping","TOOL",2,subprocess_exit(["--mapping","tools/designsystem-fixtures/no-mapping.json"]))

    results=sorted(results,key=lambda x:x["fixture_id"])
    failures=[r for r in results if r["status"]!="PASS"]
    payload={
        "schemaVersion":60,"tool":"test_designsystem_verification_v60",
        "fixtureCount":len(results),
        "coreRuleOutcomes":sum(1 for r in results if r["fixture_id"].endswith(":must_pass") or r["fixture_id"].endswith(":must_fail")),
        "candidateGuardOutcomes":sum(1 for r in results if r["fixture_id"].startswith("GUARD-")),
        "exceptionOutcomes":sum(1 for r in results if r["fixture_id"].startswith("EXCEPTION:")),
        "toolErrorOutcomes":sum(1 for r in results if r["expected_exit"]==2),
        "results":results,"failures":failures,"verdict":"PASS" if not failures else "FAIL_FIXTURE_CONTRACT",
    }
    if args.output:
        out=args.output if args.output.is_absolute() else root/args.output; out.parent.mkdir(parents=True,exist_ok=True); out.write_text(json.dumps(payload,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    if args.json_mode: print(json.dumps(payload,ensure_ascii=False,sort_keys=True))
    else:
        print(f"V60 VERIFICATION FIXTURES: {payload['verdict']}")
        print(f" - explicit outcomes: {payload['fixtureCount']}")
        print(f" - core pass/fail outcomes: {payload['coreRuleOutcomes']}")
        print(f" - false-positive guards: {payload['candidateGuardOutcomes']}")
        print(f" - exception outcomes: {payload['exceptionOutcomes']}")
        print(f" - expected tool-error outcomes: {payload['toolErrorOutcomes']}")
        for f in failures: print(" - mismatch",f["fixture_id"],f["expected_exit"],f["actual_exit"],f["detail"])
    return 0 if not failures else 1

if __name__=="__main__":
    try: raise SystemExit(main())
    except KeyboardInterrupt: raise
    except Exception as exc:
        print(f"V60 VERIFICATION FIXTURES: TOOL_ERROR\n - {type(exc).__name__}: {exc}")
        raise SystemExit(2)
