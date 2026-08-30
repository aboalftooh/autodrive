#!/usr/bin/env python3
"""Aggregate Session 75 static verifier."""
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import sys
sys.dont_write_bytecode = True
from pathlib import Path

ROOT_DEFAULT = Path(__file__).resolve().parents[1]


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def normalized_semantic_sha(path: Path, strip_comments) -> str:
    text = path.read_text(encoding="utf-8")
    normalized = " ".join(strip_comments(text).split())
    return hashlib.sha256(normalized.encode("utf-8")).hexdigest()


def verify_hash_map(root: Path, mapping: dict[str, str]) -> tuple[list[str], list[str]]:
    missing, changed = [], []
    for rel, expected in mapping.items():
        p = root / rel
        if not p.is_file():
            missing.append(rel)
        elif sha256(p) != expected:
            changed.append(rel)
    return missing, changed


def current_gradle_paths(root: Path) -> set[str]:
    out = set()
    for p in root.rglob("*"):
        if not p.is_file():
            continue
        s = p.relative_to(root).as_posix()
        if p.name in {"settings.gradle.kts", "settings.gradle", "gradle.properties"} or p.suffix == ".gradle" or s.endswith(".gradle.kts") or s in {
            "gradle/libs.versions.toml",
            "gradle/wrapper/gradle-wrapper.properties",
            "gradle/wrapper/gradle-wrapper.jar",
        }:
            out.add(s)
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=str(ROOT_DEFAULT))
    ap.add_argument("--json-output")
    args = ap.parse_args()
    root = Path(args.root).resolve()

    sys.path.insert(0, str(root / "tools/documentation"))
    from documentation_drift import run_checks, strip_kotlin_comments  # type: ignore

    manifest_path = root / "tools/documentation/v75_baseline_integrity.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    expected_source_sha = "2629e33dca8f7f634ae9cf3b102443ca8ac8c2174f1a9bb2fa2f8bd4f326f321"

    result: dict[str, object] = {
        "session": 75,
        "source_zip": manifest.get("source_zip"),
        "source_zip_sha256": manifest.get("source_zip_sha256"),
        "inputSourceIntegrity": manifest.get("source_zip_sha256") == expected_source_sha,
    }

    drift = run_checks(root)
    result["documentationDriftChecks"] = {r.code: r.passed for r in drift}
    result["documentationDriftGate"] = all(r.passed for r in drift)

    negative = subprocess.run(
        [sys.executable, "tools/documentation/test_documentation_drift.py", "--root", "."],
        cwd=root,
        text=True,
        capture_output=True,
    )
    result["negativeMutationExitCode"] = negative.returncode
    result["negativeMutationTests"] = "9/9" if negative.returncode == 0 and "NEGATIVE_MUTATION_TESTS=9/9" in negative.stdout else "FAIL"

    allowed = set(manifest["allowed_kdoc_paths"])
    semantic_failures = []
    byte_changed_allowed = []
    for rel in sorted(allowed):
        p = root / rel
        if not p.is_file():
            semantic_failures.append(rel + ":MISSING")
            continue
        if normalized_semantic_sha(p, strip_kotlin_comments) != manifest["allowed_kotlin_semantic_sha256"][rel]:
            semantic_failures.append(rel)
        if sha256(p) != manifest["allowed_kotlin_original_sha256"][rel]:
            byte_changed_allowed.append(rel)
    result["allowedKotlinChangedFiles"] = byte_changed_allowed
    result["allowedKotlinChangedCount"] = len(byte_changed_allowed)
    result["commentStrippedSemanticTextEqual"] = not semantic_failures
    result["semanticKotlinFailures"] = semantic_failures

    uk_missing, uk_changed = verify_hash_map(root, manifest["unauthorized_production_kotlin_sha256"])
    current_prod = {
        p.relative_to(root).as_posix()
        for p in root.rglob("*.kt")
        if "/src/main/" in p.as_posix()
    }
    baseline_prod = set(manifest["unauthorized_production_kotlin_sha256"]) | allowed
    prod_set_drift = sorted(current_prod ^ baseline_prod)
    result["unauthorizedSourceDrift"] = len(uk_missing) + len(uk_changed) + len(prod_set_drift)
    result["unauthorizedSourceMissing"] = uk_missing
    result["unauthorizedSourceChanged"] = uk_changed
    result["productionSourceSetDrift"] = prod_set_drift

    sql_missing, sql_changed = verify_hash_map(root, manifest["sql_sha256"])
    current_sql = {p.relative_to(root).as_posix() for p in root.rglob("*.sql")}
    sql_set_drift = sorted(current_sql ^ set(manifest["sql_sha256"]))
    result["sqlMutation"] = len(sql_missing) + len(sql_changed) + len(sql_set_drift)
    result["sqlChanged"] = sql_changed + sql_set_drift

    gradle_missing, gradle_changed = verify_hash_map(root, manifest["gradle_settings_sha256"])
    gradle_set_drift = sorted(current_gradle_paths(root) ^ set(manifest["gradle_settings_sha256"]))
    result["gradleMutation"] = len(gradle_missing) + len(gradle_changed) + len(gradle_set_drift)
    result["gradleChanged"] = gradle_changed + gradle_set_drift

    hist_missing, hist_changed = verify_hash_map(root, manifest["historical_evidence_sha256"])
    result["historicalEvidenceMutation"] = len(hist_missing) + len(hist_changed)
    result["historicalEvidenceMissing"] = hist_missing
    result["historicalEvidenceChanged"] = hist_changed

    result["productionBehaviorMutation"] = 0 if not semantic_failures and result["unauthorizedSourceDrift"] == 0 else 1

    passed = bool(
        result["inputSourceIntegrity"]
        and result["documentationDriftGate"]
        and result["negativeMutationTests"] == "9/9"
        and result["commentStrippedSemanticTextEqual"]
        and result["unauthorizedSourceDrift"] == 0
        and result["sqlMutation"] == 0
        and result["gradleMutation"] == 0
        and result["historicalEvidenceMutation"] == 0
        and result["productionBehaviorMutation"] == 0
    )
    result["passed"] = passed
    result["verdict"] = "PASS_AUTODRIVE_DOCUMENTATION_SUSTAINABILITY_V75" if passed else "FAIL_AUTODRIVE_DOCUMENTATION_SUSTAINABILITY_V75"

    rendered = json.dumps(result, indent=2, sort_keys=True)
    if args.json_output:
        (root / args.json_output).write_text(rendered + "\n", encoding="utf-8")
    print(rendered)
    return 0 if passed else 1

if __name__ == "__main__":
    raise SystemExit(main())
