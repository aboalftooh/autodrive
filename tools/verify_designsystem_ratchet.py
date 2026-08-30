#!/usr/bin/env python3
"""AutoDrive Design System monotonic ratchet verifier (Session 60).

The v59 baseline is immutable authority. This tool does not regenerate it. It
uses stable finding identities, exact tracked-file hashes, conservative
scanners, candidate classification, and strict 0/1/2 exit semantics.
"""
from __future__ import annotations
import argparse
import csv
import hashlib
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
BASELINE_SHA = "906c12831752daa2f7fce190a3aab37a75817c415b055c09ab367d8207248abc"
BASELINE_MD_SHA = "f6b59f0e09c58262a19438462da309a6739d9abe0e861c715fb21f8971a49438"
COVERAGE_SHA = "191d4497d0433ba078fb7b71bee080763cd013cc64f34ab3220477649d568dea"
CONTRACT_SHA = "ed758157e064bb1f8fc61d13e2efdd4b09938ec54640f49d603e0df18a215e0d"
EXCEPTIONS_V59_SHA = "37517e5f3dc66819f61f5a7bb8ace1921282415f10551d2defa5c3eb0985b570"
EXPECTED_PRODUCTION_MANIFEST = "3b684bd91c390eeade95b5594d8924156465c88be2510caa843540c37c877751"
EXPECTED_UI_MANIFEST = "4e3a94666454e9308e445d1de5fd3d88aed7dbb9646121ee3ae023f004f58883"
RULE_IDS = [
    "DS-A11Y-001", "DS-A11Y-002", "DS-A11Y-003", "DS-BORDER-001",
    "DS-COLOR-001", "DS-CONTRACT-001", "DS-CONTRAST-001", "DS-DUP-001",
    "DS-ELEVATION-001", "DS-EXCEPTION-001", "DS-MATERIAL-001", "DS-SHAPE-001",
    "DS-SPACE-001", "DS-TYPE-001",
]
ALLOWED_CANDIDATE_CLASSIFICATIONS = {
    "CONFIRMED_VIOLATION", "ALLOWED_LOCAL_VALUE", "NO_GLOBAL_EQUIVALENT_PROVEN",
    "DOMAIN_PALETTE_CANDIDATE", "RUNTIME_VERIFICATION_REQUIRED", "APPROVED_EXCEPTION",
    "FALSE_POSITIVE", "UNCLASSIFIED",
}
MAPPING_POLICIES = {"ENFORCE_WHEN_SEMANTIC_MATCH", "NO_GLOBAL_REPLACEMENT_PROVEN", "NO_GLOBAL_REPLACEMENT"}
EXCLUDED_PARTS = {"build", ".gradle", "test", "androidTest"}


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    return sha256_bytes(path.read_bytes())


def norm_anchor(value: str) -> str:
    return re.sub(r"\s+", " ", value.strip()).lower()


def stable_fingerprint(rule_id: str, relpath: str, symbol: str, anchor: str) -> str:
    raw = "|".join((rule_id, relpath, symbol, norm_anchor(anchor)))
    return sha256_bytes(raw.encode("utf-8"))


def is_production_kotlin(path: Path, root: Path) -> bool:
    try:
        rel = path.relative_to(root).as_posix()
    except ValueError:
        return False
    return rel.endswith(".kt") and "/src/main/kotlin/" in f"/{rel}" and not any(f"/{x}/" in f"/{rel}/" for x in ("build", ".gradle"))


def production_files(root: Path) -> list[Path]:
    return sorted(p for p in root.rglob("*.kt") if is_production_kotlin(p, root))


def production_digest(records: list[dict[str, str]]) -> str:
    data = "\n".join(f"{r['path']}\t{r['sha256']}" for r in sorted(records, key=lambda x: x["path"]))
    return sha256_bytes(data.encode("utf-8"))


def _line_symbol(source: str, pos: int) -> tuple[int, str]:
    line = source.count("\n", 0, pos) + 1
    prefix = source[:pos]
    funcs = re.findall(r"\bfun\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(", prefix)
    return line, funcs[-1] if funcs else "<file>"


def _finding(rule: str, relpath: str, source: str, pos: int, anchor: str, kind: str = "CONFIRMED_VIOLATION") -> dict[str, Any]:
    line, symbol = _line_symbol(source, pos)
    return {
        "rule_id": rule,
        "relative_path": relpath,
        "symbol": symbol,
        "line": line,
        "semantic_anchor": norm_anchor(anchor),
        "fingerprint": stable_fingerprint(rule, relpath, symbol, anchor),
        "classification": kind,
    }


def scan_text(source: str, relpath: str, rule_id: str, mapping: dict[str, Any] | None = None) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    """Conservative fixture/touched-file scanner. Returns (confirmed, candidates)."""
    findings: list[dict[str, Any]] = []
    candidates: list[dict[str, Any]] = []
    ds_owned = relpath.startswith("core/designsystem/")

    if rule_id == "DS-COLOR-001":
        if not ds_owned:
            for m in re.finditer(r"(?<![A-Za-z0-9_.])Color\s*\(", source):
                findings.append(_finding(rule_id, relpath, source, m.start(), "raw Color() outside design system"))
            for m in re.finditer(r"\b(?:0x)?FF[0-9A-Fa-f]{6}\b", source):
                findings.append(_finding(rule_id, relpath, source, m.start(), "raw color literal outside design system"))

    elif rule_id == "DS-TYPE-001":
        if not ds_owned:
            for m in re.finditer(r"\b\d+(?:\.\d+)?\.sp\b", source):
                candidates.append(_finding(rule_id, relpath, source, m.start(), f"direct sp value {m.group(0)}", "UNCLASSIFIED"))
            # Repeated same raw typography value is an enforceable shared decision.
            vals = defaultdict(list)
            for m in re.finditer(r"\b\d+(?:\.\d+)?\.sp\b", source): vals[m.group(0)].append(m)
            for value, matches in sorted(vals.items()):
                if len(matches) >= 2:
                    findings.append(_finding(rule_id, relpath, source, matches[0].start(), f"repeated shared typography value {value}"))

    elif rule_id == "DS-SPACE-001":
        if not ds_owned:
            vals = defaultdict(list)
            for m in re.finditer(r"\b\d+(?:\.\d+)?\.dp\b", source): vals[m.group(0)].append(m)
            for value, matches in sorted(vals.items()):
                if len(matches) >= 3:
                    findings.append(_finding(rule_id, relpath, source, matches[0].start(), f"repeated shared spacing value {value}"))
                elif len(matches) == 2:
                    candidates.append(_finding(rule_id, relpath, source, matches[0].start(), f"repeated local spacing candidate {value}", "UNCLASSIFIED"))
                # one-off dp is explicitly allowed, not a candidate.

    elif rule_id == "DS-SHAPE-001":
        if not ds_owned:
            matches = list(re.finditer(r"RoundedCornerShape\s*\(([^)]*)\)", source))
            groups = defaultdict(list)
            for m in matches: groups[norm_anchor(m.group(1))].append(m)
            for value, items in sorted(groups.items()):
                if len(items) >= 2:
                    findings.append(_finding(rule_id, relpath, source, items[0].start(), f"repeated shared RoundedCornerShape({value})"))
                elif len(items) == 1:
                    candidates.append(_finding(rule_id, relpath, source, items[0].start(), f"local shape candidate RoundedCornerShape({value})", "UNCLASSIFIED"))

    elif rule_id == "DS-BORDER-001":
        if not ds_owned:
            matches = list(re.finditer(r"(?:BorderStroke\s*\([^)]*\)|\.border\s*\([^)]*\))", source))
            raw_matches = [
                m for m in matches
                if not (
                    "AutoDriveBorder." in m.group(0)
                    and ("AutoDriveBorderColor." in m.group(0) or "AutoDriveBrand." in m.group(0) or "AutoDriveStatus." in m.group(0))
                )
            ]
            if len(raw_matches) >= 2:
                findings.append(_finding(rule_id, relpath, source, raw_matches[0].start(), "repeated shared raw border decision"))

    elif rule_id == "DS-ELEVATION-001":
        if not ds_owned:
            matches = list(re.finditer(r"(?:\.shadow\s*\([^)]*\)|shadowElevation\s*=\s*[^,\n)]+)", source))
            if len(matches) >= 2:
                findings.append(_finding(rule_id, relpath, source, matches[0].start(), "repeated shared raw elevation decision"))

    elif rule_id == "DS-MATERIAL-001":
        mappings = (mapping or {}).get("mappings", [])
        by_primitive = {x.get("primitive"): x for x in mappings if isinstance(x, dict)}
        # Imports alone are never findings. Call expressions are evaluated by mapping policy.
        for primitive, rec in sorted(by_primitive.items(), key=lambda x: x[0] or ""):
            if not primitive or ds_owned:
                continue
            for m in re.finditer(rf"(?<![A-Za-z0-9_]){re.escape(primitive)}\s*\(", source):
                policy = rec.get("policy")
                if policy == "ENFORCE_WHEN_SEMANTIC_MATCH":
                    findings.append(_finding(rule_id, relpath, source, m.start(), f"confirmed mapped primitive call {primitive}"))
                elif policy == "NO_GLOBAL_REPLACEMENT_PROVEN":
                    c = _finding(rule_id, relpath, source, m.start(), f"unresolved primitive candidate {primitive}", "NO_GLOBAL_EQUIVALENT_PROVEN")
                    candidates.append(c)
                # NO_GLOBAL_REPLACEMENT is intentionally not a finding/candidate.

    elif rule_id == "DS-DUP-001":
        # Names alone are insufficient. Fixture marker represents evidence that all five
        # semantic/state/slot/interaction/visual contracts were proven equivalent.
        for m in re.finditer(r"DS_DUPLICATE_EQUIVALENCE_PROVEN\s*:\s*([A-Za-z0-9_.-]+)", source):
            findings.append(_finding(rule_id, relpath, source, m.start(), f"proven duplicate {m.group(1)}"))

    elif rule_id == "DS-A11Y-001":
        # Confirm only a clear interactive IconButton whose descendant Icon explicitly has null description.
        for m in re.finditer(r"IconButton\s*\([^)]*\)\s*\{(?P<body>.{0,600}?)\}", source, flags=re.S):
            body = m.group("body")
            if re.search(r"Icon\s*\([^)]*contentDescription\s*=\s*null", body) or re.search(r"Icon\s*\([^,]+,\s*null\b", body):
                findings.append(_finding(rule_id, relpath, source, m.start(), "interactive icon has null content description"))

    elif rule_id == "DS-A11Y-002":
        # Static size does not prove effective Material hitbox; classify for runtime verification.
        for m in re.finditer(r"\.size\s*\(\s*(?:3[0-9]|4[0-7])(?:\.\d+)?\.dp\s*\)", source):
            candidates.append(_finding(rule_id, relpath, source, m.start(), "sub-48 visual size requires runtime hitbox verification", "RUNTIME_VERIFICATION_REQUIRED"))

    elif rule_id == "DS-A11Y-003":
        for m in re.finditer(r"DS_COLOR_ONLY_STATE_CANDIDATE", source):
            candidates.append(_finding(rule_id, relpath, source, m.start(), "color-only state requires review", "UNCLASSIFIED"))

    elif rule_id == "DS-CONTRAST-001":
        for m in re.finditer(r"DS_CONTRAST_RUNTIME_REQUIRED", source):
            candidates.append(_finding(rule_id, relpath, source, m.start(), "contrast requires runtime/quantitative verification", "RUNTIME_VERIFICATION_REQUIRED"))

    return sorted(findings, key=lambda x: (x["rule_id"], x["relative_path"], x["symbol"], x["fingerprint"])), sorted(candidates, key=lambda x: (x["rule_id"], x["relative_path"], x["symbol"], x["fingerprint"]))


def _load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def validate_mapping_registry(mapping: Any) -> list[str]:
    errors: list[str] = []
    if not isinstance(mapping, dict) or mapping.get("schemaVersion") != 60 or not isinstance(mapping.get("mappings"), list):
        return ["primitive mapping root/schema invalid"]
    seen: set[str] = set()
    confirmed = candidate = 0
    for i, rec in enumerate(mapping["mappings"]):
        p = f"mapping[{i}]"
        if not isinstance(rec, dict): errors.append(f"{p}: not object"); continue
        mid = rec.get("mapping_id")
        if not isinstance(mid, str) or not mid: errors.append(f"{p}: mapping_id missing")
        elif mid in seen: errors.append(f"{p}: duplicate mapping_id {mid}")
        else: seen.add(mid)
        if not isinstance(rec.get("primitive"), str) or not rec.get("primitive"): errors.append(f"{p}: primitive missing")
        if rec.get("rule_id") != "DS-MATERIAL-001": errors.append(f"{p}: rule_id must be DS-MATERIAL-001")
        policy = rec.get("policy")
        if policy not in MAPPING_POLICIES: errors.append(f"{p}: invalid policy {policy!r}")
        if policy == "ENFORCE_WHEN_SEMANTIC_MATCH" and not rec.get("official_replacement"):
            errors.append(f"{p}: enforced mapping requires official_replacement")
        for key in ("semantic_requirements", "slot_requirements", "state_requirements", "interaction_requirements", "allowed_cases"):
            if not isinstance(rec.get(key), list) or not rec.get(key): errors.append(f"{p}: {key} must be nonempty list")
        for key in ("baseline_confirmed_count", "baseline_candidate_count"):
            value = rec.get(key)
            if not isinstance(value, int) or isinstance(value, bool) or value < 0: errors.append(f"{p}: {key} must be nonnegative int")
        confirmed += rec.get("baseline_confirmed_count", 0) if isinstance(rec.get("baseline_confirmed_count"), int) else 0
        candidate += rec.get("baseline_candidate_count", 0) if isinstance(rec.get("baseline_candidate_count"), int) else 0
    if confirmed != 50: errors.append(f"confirmed mapping reconciliation expected 50, got {confirmed}")
    if candidate != 12: errors.append(f"candidate mapping reconciliation expected 12, got {candidate}")
    return sorted(errors)


def _baseline_finding_persists(rec: dict[str, Any], source: str, mapping: dict[str, Any]) -> bool:
    """Conservative persistence test used only after a tracked file changes."""
    rule = rec["rule_id"]
    anchor = rec.get("semantic_anchor", "")
    symbol = rec.get("symbol", "")
    if rule == "DS-MATERIAL-001":
        # semantic anchor from v59 is "primitive bypass" and baseline finding's symbol is screen,
        # so locate original primitive from current line evidence by the finding ID is unavailable here.
        # A changed file with any enforced mapped primitive remains debt; if none remain, this finding may resolve.
        f, _ = scan_text(source, rec["relative_path"], rule, mapping)
        return bool(f)
    if rule == "DS-COLOR-001": return bool(re.search(r"(?<![A-Za-z0-9_.])Color\s*\(", source))
    if rule == "DS-TYPE-001": return bool(re.search(r"\b\d+(?:\.\d+)?\.sp\b", source))
    if rule == "DS-SPACE-001": return bool(re.search(r"\b\d+(?:\.\d+)?\.dp\b", source))
    if rule == "DS-SHAPE-001": return "RoundedCornerShape(" in source or "RoundedCornerShape (" in source
    if rule == "DS-A11Y-001":
        f, _ = scan_text(source, rec["relative_path"], rule, mapping); return bool(f)
    if rule == "DS-CONTRACT-001":
        tests = {
            "pump hero not hosted by dashboardhero": "DashboardHero(" not in source,
            "technical number not using autodriveinstrumentnumber": "AutoDriveInstrumentNumber(" not in source,
            "local header instead of screenheader contract": "ScreenHeader(" not in source,
            "responsive dashboard width contract absent": "AutoDriveContentWidth.Dashboard" not in source,
            "reportstattile adoption missing in root reports composition": "ReportStatTile(" not in source,
            "dashboard max-width contract absent": "AutoDriveContentWidth.Dashboard" not in source,
            "narrow two-column fallback contract absent": "ReportTwoColumn" not in source,
        }
        if anchor in tests: return tests[anchor]
        if "raw/shared foundation styling umbrella drift" in anchor:
            return bool(re.search(r"\bColor\s*\(|\b\d+(?:\.\d+)?\.(?:dp|sp)\b|RoundedCornerShape\s*\(", source))
        if "local branded palette/style island" in anchor:
            return bool(re.search(r"\bColor\s*\(|\b\d+(?:\.\d+)?\.(?:dp|sp)\b", source))
        if "supporting-line contract drift" in anchor:
            return "HomeHeader(" in source and "ScreenHeader(" not in source
        if "destructive titlecolor computed but not applied" in anchor:
            # v63 closes this historical gap only when SettingsRow forwards the governed typed Error tone.
            return not bool(re.search(r"titleTone\s*=\s*if\s*\(\s*variant\s*==\s*SettingsRowVariant\.Destructive\s*\)\s*AutoDriveStatusTone\.Error\s*else\s*null", source))
        return symbol in source if symbol and symbol != "<file>" else True
    return True


def _candidate_persists(rec: dict[str, Any], source: str) -> bool:
    anchor = rec.get("semantic_anchor", "")
    if "circularprogressindicator" in anchor: return "CircularProgressIndicator(" in source
    if "topappbar" in anchor: return "TopAppBar(" in source
    if "explicit visual size" in anchor:
        return bool(re.search(r"\.size\s*\(\s*(?:3[0-9]|4[0-7])(?:\.\d+)?\.dp\s*\)", source))
    if "local header duplicate candidate" in anchor: return "HomeHeader" in source
    return True


def _candidate_semantic_key(rec: dict[str, Any]) -> tuple[str, str, str]:
    """Match known candidates by semantic primitive, not incidental symbol/line movement."""
    rule = str(rec.get("rule_id", ""))
    rel = str(rec.get("relative_path", ""))
    anchor = str(rec.get("semantic_anchor", "")).lower()
    for primitive in ("circularprogressindicator", "topappbar"):
        if primitive in anchor:
            return (rule, rel, primitive)
    return (rule, rel, str(rec.get("fingerprint", "")))


def _exception_matches(exc: dict[str, Any], finding: dict[str, Any]) -> bool:
    return exc.get("rule_id") == finding.get("rule_id") and exc.get("file") == finding.get("relative_path") and exc.get("symbol") == finding.get("symbol")


def run(root: Path, baseline_path: Path, coverage_path: Path, state_path: Path, exceptions_path: Path, mapping_path: Path, session: int = 60) -> tuple[int, dict[str, Any]]:
    # Input/runtime errors are handled as exit 2, distinct from policy failures.
    for p in (baseline_path, coverage_path, state_path, exceptions_path, mapping_path):
        if not p.is_file(): raise FileNotFoundError(str(p))
    baseline = _load_json(baseline_path)
    state = _load_json(state_path)
    exceptions = _load_json(exceptions_path)
    mapping = _load_json(mapping_path)

    errors: list[str] = []
    policy_failures: list[str] = []
    authority_hashes = {
        "baseline": sha256_file(baseline_path),
        "coverage": sha256_file(coverage_path),
        "state": sha256_file(state_path),
        "exceptions": sha256_file(exceptions_path),
        "mapping": sha256_file(mapping_path),
    }
    if authority_hashes["baseline"] != BASELINE_SHA: policy_failures.append("BLOCKED_BASELINE_AUTHORITY_DRIFT: baseline JSON SHA")
    if authority_hashes["coverage"] != COVERAGE_SHA: policy_failures.append("BLOCKED_BASELINE_AUTHORITY_DRIFT: coverage CSV SHA")
    for rel, expected in (("DESIGN_SYSTEM_BASELINE_v59.md", BASELINE_MD_SHA), ("DESIGN_SYSTEM_V1_CONTRACT_REGISTRY_v59.md", CONTRACT_SHA)):
        p = root / rel
        if not p.is_file() or sha256_file(p) != expected: policy_failures.append(f"BLOCKED_BASELINE_AUTHORITY_DRIFT: {rel}")

    if baseline.get("schemaVersion") != 59: policy_failures.append("FAIL_RATCHET_RECONCILIATION: baseline schema != 59")
    accepted_version = state.get("acceptedVersion") if isinstance(state, dict) else None
    state_schema = state.get("schemaVersion") if isinstance(state, dict) else None
    allowed_identity = ((state_schema == 60 and accepted_version == "v59") or (state_schema >= 61 and accepted_version == "v61") or (state_schema >= 62 and accepted_version == "v62") or (state_schema >= 63 and accepted_version == "v63") or (state_schema >= 64 and accepted_version == "v64") or (state_schema >= 65 and accepted_version == "v65"))
    if not allowed_identity:
        policy_failures.append("FAIL_RATCHET_RECONCILIATION: ratchet state identity")
    if session == 60 and accepted_version != "v59":
        policy_failures.append("FAIL_RATCHET_RECONCILIATION: session 60 requires v59-backed state")
    if state.get("authority", {}).get("sha256") != BASELINE_SHA: policy_failures.append("FAIL_RATCHET_RECONCILIATION: state authority SHA")
    if state.get("productionManifestSha256") != EXPECTED_PRODUCTION_MANIFEST: policy_failures.append("FAIL_RATCHET_RECONCILIATION: production manifest authority")
    if state.get("uiManifestSha256") != EXPECTED_UI_MANIFEST: policy_failures.append("FAIL_RATCHET_RECONCILIATION: UI manifest authority")

    baseline_confirmed = [f for f in baseline.get("findings", []) if f.get("classification") == "CONFIRMED_VIOLATION"]
    baseline_candidates = [f for f in baseline.get("findings", []) if f.get("classification") != "CONFIRMED_VIOLATION"]
    if len(baseline_confirmed) != 77: policy_failures.append(f"FAIL_RATCHET_RECONCILIATION: confirmed baseline expected 77 got {len(baseline_confirmed)}")
    if len(baseline_candidates) != 19: policy_failures.append(f"FAIL_RATCHET_RECONCILIATION: candidate baseline expected 19 got {len(baseline_candidates)}")
    expected_state_confirmed = 0 if accepted_version in {"v64", "v65"} else (46 if accepted_version == "v63" else (47 if accepted_version == "v62" else (57 if accepted_version == "v61" else 77)))
    expected_state_candidates = 6 if accepted_version in {"v64", "v65"} else (18 if accepted_version in {"v61", "v62", "v63"} else 19)
    if len(state.get("confirmedFindings", [])) != expected_state_confirmed:
        policy_failures.append(f"FAIL_RATCHET_RECONCILIATION: state confirmed findings != {expected_state_confirmed}")
    if len(state.get("acceptedCandidates", [])) != expected_state_candidates:
        policy_failures.append(f"FAIL_RATCHET_RECONCILIATION: state candidates != {expected_state_candidates}")
    if sorted(x.get("rule_id") for x in state.get("rules", [])) != RULE_IDS: policy_failures.append("FAIL_RATCHET_RECONCILIATION: state rule set")
    if any(x.get("accepted_classification") not in ALLOWED_CANDIDATE_CLASSIFICATIONS or x.get("accepted_classification") == "UNCLASSIFIED" for x in state.get("acceptedCandidates", [])):
        policy_failures.append("FAIL_RATCHET_RECONCILIATION: known candidate classification invalid/unclassified")

    mapping_errors = validate_mapping_registry(mapping)
    policy_failures.extend(f"FAIL_PRIMITIVE_MAPPING_RECONCILIATION: {x}" for x in mapping_errors)

    try:
        from validate_designsystem_exceptions import validate_document
        exception_errors = validate_document(exceptions, root=root)
    except Exception as exc:
        raise RuntimeError(f"exception validator import/use failed: {exc}") from exc
    if exception_errors:
        policy_failures.extend(f"FAIL_EXCEPTION_LEDGER: {x}" for x in exception_errors)
    if session == 60 and isinstance(exceptions, list) and len(exceptions) != 0:
        policy_failures.append("FAIL_EXCEPTION_ABUSE: v60 expected zero production exceptions")

    # Exact per-file identity is stronger than relying only on a count digest.
    accepted_prod = {x["path"]: x["sha256"] for x in state.get("productionFiles", [])}
    current_prod_paths = production_files(root)
    current_prod = {p.relative_to(root).as_posix(): sha256_file(p) for p in current_prod_paths}
    added = sorted(set(current_prod) - set(accepted_prod))
    removed = sorted(set(accepted_prod) - set(current_prod))
    changed = sorted(p for p in set(current_prod) & set(accepted_prod) if current_prod[p] != accepted_prod[p])
    touched = sorted(set(added + removed + changed))
    current_records = [{"path": p, "sha256": current_prod[p]} for p in sorted(current_prod)]
    current_digest = production_digest(current_records)
    if len(current_prod) != 251 and session in {60, 64, 65, 66}: policy_failures.append(f"FAIL_SCOPE_VIOLATION: production Kotlin count {len(current_prod)} != 251")
    if session == 60 and touched:
        policy_failures.append(f"FAIL_SCOPE_VIOLATION: production Kotlin modified {len(touched)}")

    # UI coverage denominator and per-file identity.
    with coverage_path.open(newline="", encoding="utf-8") as fh:
        coverage_rows = list(csv.DictReader(fh))
    if len(coverage_rows) != 56 or len({r["full_relative_path"] for r in coverage_rows}) != 56:
        policy_failures.append("FAIL_RATCHET_RECONCILIATION: UI coverage denominator != 56")
    ui_changed = []
    for row in coverage_rows:
        p = root / row["full_relative_path"]
        if not p.is_file() or sha256_file(p) != row["sha256_v58"]:
            ui_changed.append(row["full_relative_path"])
    if session == 60 and ui_changed: policy_failures.append(f"FAIL_SCOPE_VIOLATION: production UI files modified {len(ui_changed)}")

    # Persist baseline identities exactly for unchanged files; conservatively re-evaluate only touched files.
    current_findings: list[dict[str, Any]] = []
    resolved: list[dict[str, Any]] = []
    for rec in state.get("confirmedFindings", []):
        rel = rec["relative_path"]
        if rel not in changed and rel not in removed:
            current_findings.append(dict(rec))
        elif rel in removed:
            resolved.append(dict(rec))
        else:
            source = (root / rel).read_text(encoding="utf-8")
            if _baseline_finding_persists(rec, source, mapping): current_findings.append(dict(rec))
            else: resolved.append(dict(rec))

    known_candidates: list[dict[str, Any]] = []
    resolved_candidates: list[dict[str, Any]] = []
    for rec in state.get("acceptedCandidates", []):
        rel = rec["relative_path"]
        if rel not in changed and rel not in removed:
            known_candidates.append(dict(rec))
        elif rel in removed:
            resolved_candidates.append(dict(rec))
        else:
            source = (root / rel).read_text(encoding="utf-8")
            if _candidate_persists(rec, source): known_candidates.append(dict(rec))
            else: resolved_candidates.append(dict(rec))

    # Scan only touched/new production source. Untouched legacy debt is ratcheted, not re-labeled.
    detected_new: list[dict[str, Any]] = []
    detected_candidates: list[dict[str, Any]] = []
    for rel in sorted(set(added + changed)):
        if rel not in current_prod: continue
        source = (root / rel).read_text(encoding="utf-8")
        for rid in RULE_IDS:
            f, c = scan_text(source, rel, rid, mapping)
            detected_new.extend(f); detected_candidates.extend(c)
    # Session 64 touched-scope reconciliation: compare scanner signals against the
    # immutable v63 snapshots. Line shifts/comments and pre-existing local dp/sp/shape
    # decisions are not new debt; genuinely new semantic scanner signals remain fatal.
    if session == 64 and accepted_version == "v63":
        snapshot_dir = root / "tools/fixtures/v64/v63_snapshot"
        manifest_path = snapshot_dir / "manifest.json"
        if not manifest_path.is_file():
            policy_failures.append("FAIL_RATCHET_RECONCILIATION: v64 snapshot manifest missing")
        else:
            try:
                manifest = _load_json(manifest_path)
                snap_by_rel = {x["path"]: x for x in manifest.get("files", []) if isinstance(x, dict) and x.get("path", "").endswith(".kt")}
                pre_confirmed = Counter()
                pre_candidates = Counter()
                def sig_key(x: dict[str, Any]) -> tuple[str, str, str]:
                    rule = str(x.get("rule_id", ""))
                    rel = str(x.get("relative_path", ""))
                    anchor = norm_anchor(str(x.get("semantic_anchor", "")))
                    if rule == "DS-SPACE-001":
                        m = re.search(r"\b\d+(?:\.\d+)?\.dp\b", anchor)
                        if m: anchor = "spacing:" + m.group(0)
                    elif rule == "DS-TYPE-001":
                        m = re.search(r"\b\d+(?:\.\d+)?\.sp\b", anchor)
                        if m: anchor = "type:" + m.group(0)
                    return (rule, rel, anchor)
                for rel in sorted(set(added + changed)):
                    rec = snap_by_rel.get(rel)
                    if not rec:
                        continue
                    sp = snapshot_dir / rec["snapshot"]
                    if not sp.is_file() or sha256_file(sp) != rec.get("sha256"):
                        policy_failures.append(f"FAIL_RATCHET_RECONCILIATION: snapshot drift {rel}")
                        continue
                    src0 = sp.read_text(encoding="utf-8")
                    for rid in RULE_IDS:
                        f0, c0 = scan_text(src0, rel, rid, mapping)
                        pre_confirmed.update(sig_key(x) for x in f0)
                        pre_candidates.update(sig_key(x) for x in c0)
                def excess(records: list[dict[str, Any]], baseline_counts: Counter) -> list[dict[str, Any]]:
                    used = Counter()
                    out = []
                    for x in sorted(records, key=lambda r:(r["rule_id"],r["relative_path"],r.get("symbol",""),r["fingerprint"])):
                        k = sig_key(x); used[k] += 1
                        if used[k] > baseline_counts[k]: out.append(x)
                    return out
                detected_new = excess(detected_new, pre_confirmed)
                detected_candidates = excess(detected_candidates, pre_candidates + pre_confirmed)
            except (OSError, json.JSONDecodeError, KeyError, TypeError, ValueError) as exc:
                policy_failures.append(f"FAIL_RATCHET_RECONCILIATION: v64 snapshot error {type(exc).__name__}: {exc}")

    # Session 65 accessibility reconciliation. v65 deliberately changes semantics in
    # a frozen 14-file allowlist. Old style signals in those files are compared to the
    # immutable v64 snapshots so pre-existing debt cannot be relabeled as introduced.
    # Accessibility/contrast closure itself is delegated to the source-aware v65 gate.
    if session in {65, 66}:
        allowlist_path = root / "V65_MUTATION_ALLOWLIST.json"
        snapshot_dir = root / "tools/fixtures/v65/v64_snapshot"
        manifest_path = snapshot_dir / "manifest.json"
        if not allowlist_path.is_file() or not manifest_path.is_file():
            policy_failures.append("FAIL_RATCHET_RECONCILIATION: v65 allowlist/snapshot missing")
        else:
            try:
                allow_doc = _load_json(allowlist_path)
                manifest = _load_json(manifest_path)
                allowed_v65 = {x["path"] for x in allow_doc.get("files", [])}
                if len(allowed_v65) != 14:
                    policy_failures.append(f"FAIL_SCOPE_VIOLATION: v65 allowlist count {len(allowed_v65)} != 14")
                if accepted_version == "v64" and set(touched) != allowed_v65:
                    policy_failures.append("FAIL_SCOPE_VIOLATION: v65 preaccept touched set != frozen allowlist")
                if accepted_version == "v65" and touched:
                    policy_failures.append("FAIL_SCOPE_VIOLATION: v65 postaccept production drift")
                snap_by_rel = {x["path"]: x for x in manifest.get("files", []) if isinstance(x, dict)}
                pre_confirmed = Counter(); pre_candidates = Counter()
                def sig65(x: dict[str, Any]) -> tuple[str, str, str]:
                    rule = str(x.get("rule_id", "")); rel = str(x.get("relative_path", ""))
                    anchor = norm_anchor(str(x.get("semantic_anchor", "")))
                    if rule == "DS-SPACE-001":
                        m = re.search(r"\b\d+(?:\.\d+)?\.dp\b", anchor)
                        if m: anchor = "spacing:" + m.group(0)
                    elif rule == "DS-TYPE-001":
                        m = re.search(r"\b\d+(?:\.\d+)?\.sp\b", anchor)
                        if m: anchor = "type:" + m.group(0)
                    return (rule, rel, anchor)
                non_a11y_rules = [r for r in RULE_IDS if r not in {"DS-A11Y-001","DS-A11Y-002","DS-A11Y-003","DS-CONTRAST-001"}]
                for rel in sorted(allowed_v65):
                    rec = snap_by_rel.get(rel)
                    if not rec:
                        policy_failures.append(f"FAIL_RATCHET_RECONCILIATION: missing v64 snapshot {rel}"); continue
                    sp = snapshot_dir / rec["snapshot"]
                    expected = rec.get("preSha256") or rec.get("sha256")
                    if not sp.is_file() or sha256_file(sp) != expected:
                        policy_failures.append(f"FAIL_RATCHET_RECONCILIATION: v64 snapshot drift {rel}"); continue
                    src0 = sp.read_text(encoding="utf-8")
                    for rid in non_a11y_rules:
                        f0,c0 = scan_text(src0, rel, rid, mapping)
                        pre_confirmed.update(sig65(x) for x in f0); pre_candidates.update(sig65(x) for x in c0)
                cur_non_a11y = [x for x in detected_new if x.get("rule_id") in non_a11y_rules]
                cur_non_a11y_c = [x for x in detected_candidates if x.get("rule_id") in non_a11y_rules]
                def excess65(records: list[dict[str, Any]], base: Counter) -> list[dict[str, Any]]:
                    used=Counter(); out=[]
                    for x in sorted(records,key=lambda r:(r["rule_id"],r["relative_path"],r.get("symbol",""),r["fingerprint"])):
                        k=sig65(x); used[k]+=1
                        if used[k] > base[k]: out.append(x)
                    return out
                detected_new = excess65(cur_non_a11y, pre_confirmed)
                detected_candidates = excess65(cur_non_a11y_c, pre_candidates + pre_confirmed)
                from verify_designsystem_v65_accessibility import run as run_v65_accessibility
                v65_code, v65_payload = run_v65_accessibility(root)
                if v65_code != 0:
                    policy_failures.append("FAIL_RATCHET_RECONCILIATION: v65 accessibility verifier failed")
            except (OSError, json.JSONDecodeError, KeyError, TypeError, ValueError, ImportError) as exc:
                policy_failures.append(f"FAIL_RATCHET_RECONCILIATION: v65 snapshot error {type(exc).__name__}: {exc}")

    # De-duplicate scanner records and avoid classifying a known fingerprint as new.
    known_fps = {x["fingerprint"] for x in state.get("confirmedFindings", [])}
    known_candidate_fps = {x["fingerprint"] for x in state.get("acceptedCandidates", [])}
    known_candidate_keys = {_candidate_semantic_key(x) for x in state.get("acceptedCandidates", [])}
    new_violations = [x for x in detected_new if x["fingerprint"] not in known_fps]
    new_candidates = [
        x for x in detected_candidates
        if x["fingerprint"] not in known_candidate_fps
        and _candidate_semantic_key(x) not in known_candidate_keys
    ]
    new_violations = sorted({x["fingerprint"]: x for x in new_violations}.values(), key=lambda x:(x["rule_id"],x["relative_path"],x["symbol"],x["fingerprint"]))
    new_candidates = sorted({x["fingerprint"]: x for x in new_candidates}.values(), key=lambda x:(x["rule_id"],x["relative_path"],x["symbol"],x["fingerprint"]))

    # Approved exact exceptions only apply after validation; they never erase the underlying finding.
    exceptions_applied: list[dict[str, Any]] = []
    if isinstance(exceptions, list) and not exception_errors:
        for exc in exceptions:
            for finding in current_findings + new_violations:
                if _exception_matches(exc, finding):
                    exceptions_applied.append({"exception_id": exc["id"], "finding_fingerprint": finding["fingerprint"], "rule_id": finding["rule_id"], "relative_path": finding["relative_path"], "symbol": finding["symbol"]})
    excepted_fps = {x["finding_fingerprint"] for x in exceptions_applied}

    if new_violations:
        policy_failures.append(f"FAIL_NEW_VIOLATION: {len(new_violations)} new confirmed findings")
    unclassified_new = [x for x in new_candidates if x.get("classification") == "UNCLASSIFIED"]
    if unclassified_new:
        policy_failures.append(f"FAIL_RATCHET_REGRESSION: {len(unclassified_new)} unclassified new candidates")

    touched_scope = [x for x in current_findings + new_violations if x.get("relative_path") in touched and x.get("fingerprint") not in excepted_fps]
    if touched_scope:
        policy_failures.append(f"FAIL_TOUCHED_SCOPE_DEBT: {len(touched_scope)} unapproved findings remain in touched production scope")

    current_by_rule = Counter(x["rule_id"] for x in current_findings + new_violations)
    resolved_by_rule = Counter(x["rule_id"] for x in resolved)
    new_by_rule = Counter(x["rule_id"] for x in new_violations)
    exc_by_rule = Counter(x["rule_id"] for x in exceptions_applied)
    cand_by_rule = Counter(x["rule_id"] for x in new_candidates if x.get("classification") == "UNCLASSIFIED")
    prev = {x["rule_id"]: x["previous_accepted_total"] for x in state.get("rules", [])}
    rules_out = []
    for rid in RULE_IDS:
        current = current_by_rule[rid]
        previous = prev.get(rid, 0)
        status = "PASS" if current <= previous and new_by_rule[rid] == 0 and cand_by_rule[rid] == 0 else "FAIL"
        if current > previous: policy_failures.append(f"FAIL_RATCHET_REGRESSION: {rid} total {current}>{previous}")
        rules_out.append({
            "rule_id": rid,
            "baseline_count": next((r["counts"]["baseline_count"] for r in baseline.get("rules", []) if r.get("rule_id") == rid), 0),
            "previous_accepted_total": previous,
            "current_total": current,
            "new_violation_count": new_by_rule[rid],
            "resolved_count": resolved_by_rule[rid],
            "approved_exception_count": exc_by_rule[rid],
            "unclassified_candidate_count": cand_by_rule[rid],
            "status": status,
        })

    confirmed_total = sum(current_by_rule.values())
    previous_total = sum(prev.values())
    if session == 60 and (confirmed_total != 77 or previous_total != 77 or len(new_violations) != 0 or len(resolved) != 0):
        policy_failures.append(f"FAIL_RATCHET_RECONCILIATION: expected 77/77 new=0 resolved=0 got {confirmed_total}/{previous_total} new={len(new_violations)} resolved={len(resolved)}")
    if session == 60 and (len(known_candidates) != 19 or new_candidates):
        policy_failures.append(f"FAIL_RATCHET_RECONCILIATION: expected known candidates=19 new=0 got {len(known_candidates)}/{len(new_candidates)}")
    if session == 65:
        expected_ids = sorted([
            "DS59-MATC-FC49FB269C","DS59-MATC-EA81ACB322","DS59-MATC-B28DBA9BD1",
            "DS59-MATC-9B9E4A62B6","DS59-MATC-353C4BA208","DS59-MATC-D44DE38681",
        ])
        if confirmed_total != 0 or resolved:
            policy_failures.append("FAIL_RATCHET_TOTAL: v65 confirmed/resolved mismatch")
        if len(known_candidates) != 6 or resolved_candidates:
            policy_failures.append("FAIL_RATCHET_TOTAL: v65 candidate count/resolution mismatch")
        if sorted(x.get("findingId") for x in known_candidates) != expected_ids:
            policy_failures.append("FAIL_CROSS_SESSION_CANDIDATE_WASHING: v65 historical six mismatch")
        if new_violations: policy_failures.append("FAIL_NEW_VIOLATION: v65 expected zero new violations")
        if new_candidates: policy_failures.append("FAIL_NEW_CANDIDATE: v65 expected zero new candidates")

    if session == 66:
        expected_ids = sorted([
            "DS59-MATC-FC49FB269C","DS59-MATC-EA81ACB322","DS59-MATC-B28DBA9BD1",
            "DS59-MATC-9B9E4A62B6","DS59-MATC-353C4BA208","DS59-MATC-D44DE38681",
        ])
        if confirmed_total != 0 or resolved:
            policy_failures.append("FAIL_RATCHET_TOTAL: v66 preaccept confirmed/resolved mismatch")
        if accepted_version == "v65":
            if sorted(x.get("findingId") for x in known_candidates) != expected_ids:
                policy_failures.append("FAIL_CROSS_SESSION_CANDIDATE_WASHING: v66 preaccept must retain exact six in accepted state")
            resolution_path = root / "DESIGN_SYSTEM_V66_MATERIAL_RESOLUTION.json"
            verification_path = root / "DESIGN_SYSTEM_VERIFICATION_v66.json"
            if not resolution_path.is_file() or not verification_path.is_file():
                policy_failures.append("FAIL_EVIDENCE_INTEGRITY: v66 resolution/verification missing")
            else:
                resolution = _load_json(resolution_path); verification = _load_json(verification_path)
                if sorted(resolution.get("resolvedCandidateIds", [])) != expected_ids or resolution.get("acceptedCandidatesAfterAnalysis") != 0:
                    policy_failures.append("FAIL_UNRESOLVED_CANDIDATE: v66 exact-six analysis resolution missing")
                if verification.get("runtimeFinalVerified") is not False or verification.get("fullV66Completion") is not False:
                    policy_failures.append("FAIL_RUNTIME_REQUIRED: blocked preaccept must remain runtime false")
            if touched:
                policy_failures.append("FAIL_SCOPE_VIOLATION: v66 zero-op preaccept has production drift")
        elif accepted_version == "v66":
            if known_candidates:
                policy_failures.append("FAIL_RATCHET_TOTAL: v66 postaccept candidates must be zero")
        else:
            policy_failures.append(f"FAIL_RATCHET_TOTAL: v66 unsupported acceptedVersion={accepted_version}")
        if new_violations: policy_failures.append("FAIL_NEW_VIOLATION: v66 expected zero new violations")
        if new_candidates: policy_failures.append("FAIL_NEW_CANDIDATE: v66 expected zero new candidates")

    if session == 64:
        if accepted_version == "v63":
            if confirmed_total != 0 or len(resolved) != 46:
                policy_failures.append(f"FAIL_RATCHET_TOTAL: v64 preaccept confirmed={confirmed_total} resolved={len(resolved)} expected 0/46")
            if len(known_candidates) != 6 or len(resolved_candidates) != 12:
                policy_failures.append(f"FAIL_RATCHET_TOTAL: v64 preaccept candidates={len(known_candidates)} resolvedCandidates={len(resolved_candidates)} expected 6/12")
            if len(touched) != 23:
                policy_failures.append(f"FAIL_SCOPE_VIOLATION: v64 touched production files {len(touched)} != 23")
        elif accepted_version == "v64":
            if confirmed_total != 0 or resolved:
                policy_failures.append("FAIL_RATCHET_TOTAL: v64 postaccept confirmed/resolved mismatch")
            if len(known_candidates) != 6 or resolved_candidates:
                policy_failures.append("FAIL_RATCHET_TOTAL: v64 postaccept candidates mismatch")
            if touched:
                policy_failures.append("FAIL_SCOPE_VIOLATION: v64 postaccept production drift")
        if new_violations:
            policy_failures.append("FAIL_NEW_VIOLATION: v64 expected zero new violations")
        if new_candidates:
            policy_failures.append("FAIL_NEW_CANDIDATE: v64 expected zero new candidates")

    # Deduplicate policy messages deterministically.
    policy_failures = sorted(set(policy_failures))
    verdict = "PASS" if not policy_failures else policy_failures[0].split(":",1)[0]
    payload = {
        "schemaVersion": 66 if session >= 66 else 65 if session >= 65 else 64 if session >= 64 else 63 if session >= 63 else 62 if session >= 62 else 61 if session >= 61 else 60,
        "acceptedVersionBeforeRun": accepted_version,
        "sourceIdentity": {
            "productionKotlinCount": len(current_prod),
            "declaredV59ProductionManifestSha256": EXPECTED_PRODUCTION_MANIFEST,
            "v60PerFileProductionDigest": current_digest,
            "expectedV60PerFileProductionDigest": state.get("v60PerFileProductionDigest"),
            "coverageEligibleCount": len(coverage_rows),
            "declaredV59UiManifestSha256": EXPECTED_UI_MANIFEST,
        },
        "authorityHashes": authority_hashes,
        "rules": rules_out,
        "findings": sorted(current_findings + new_violations, key=lambda x:(x["rule_id"],x["relative_path"],x["symbol"],x["fingerprint"])),
        "candidates": sorted(known_candidates + new_candidates, key=lambda x:(x["rule_id"],x["relative_path"],x["symbol"],x["fingerprint"])),
        "newViolations": new_violations,
        "resolvedViolations": sorted(resolved, key=lambda x:(x["rule_id"],x["relative_path"],x["symbol"],x["fingerprint"])),
        "touchedFiles": touched,
        "touchedScopeViolations": sorted(touched_scope, key=lambda x:(x["rule_id"],x["relative_path"],x["symbol"],x["fingerprint"])),
        "newCandidates": new_candidates,
        "resolvedCandidates": sorted(resolved_candidates, key=lambda x:(x["rule_id"],x["relative_path"],x["symbol"],x["fingerprint"])),
        "exceptionsApplied": sorted(exceptions_applied, key=lambda x:(x["exception_id"],x["finding_fingerprint"])),
        "productionMutation": {"added": added, "removed": removed, "changed": changed, "uiChanged": sorted(ui_changed)},
        "mappingReconciliation": {"confirmed": sum(x.get("baseline_confirmed_count",0) for x in mapping.get("mappings",[])), "candidates": sum(x.get("baseline_candidate_count",0) for x in mapping.get("mappings",[])), "errors": mapping_errors},
        "errors": errors,
        "policyFailures": policy_failures,
        "verdict": verdict,
    }
    return (0 if not policy_failures else 1), payload


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--baseline", type=Path)
    parser.add_argument("--coverage", type=Path)
    parser.add_argument("--state", type=Path)
    parser.add_argument("--exceptions", type=Path)
    parser.add_argument("--mapping", type=Path)
    parser.add_argument("--scope", type=str, default=None, help="reserved explicit future scan scope; v60 still enforces full identity")
    parser.add_argument("--session", type=int, default=60)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--json", action="store_true", dest="json_mode")
    args = parser.parse_args(argv)
    root = args.root.resolve()
    def resolve(value: Path | None, default: str) -> Path:
        if value is None: return root / default
        return value if value.is_absolute() else root / value
    try:
        code, payload = run(
            root,
            resolve(args.baseline, "DESIGN_SYSTEM_BASELINE_v59.json"),
            resolve(args.coverage, "DESIGN_SYSTEM_UI_COVERAGE_v59.csv"),
            resolve(args.state, "core/designsystem/verification/designsystem-ratchet-state.json"),
            resolve(args.exceptions, "core/designsystem/verification/designsystem-exceptions.json"),
            resolve(args.mapping, "core/designsystem/verification/primitive-mapping.json"),
            args.session,
        )
    except (OSError, json.JSONDecodeError, UnicodeError, ValueError, RuntimeError) as exc:
        payload = {"schemaVersion":66 if args.session >= 66 else 65 if args.session >= 65 else 64 if args.session >= 64 else 63 if args.session >= 63 else 62 if args.session >= 62 else 61 if args.session >= 61 else 60,"errors":[f"{type(exc).__name__}: {exc}"],"verdict":"TOOL_ERROR"}
        code = 2
    if args.output:
        out = args.output if args.output.is_absolute() else root / args.output
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=False)+"\n", encoding="utf-8")
    if args.json_mode:
        print(json.dumps(payload, ensure_ascii=False, sort_keys=True))
    else:
        print(f"DESIGN SYSTEM RATCHET S{args.session}: {payload.get('verdict')}")
        if code != 2:
            print(f" - confirmed: {sum(r['current_total'] for r in payload['rules'])}/77")
            print(f" - new violations: {len(payload['newViolations'])}")
            print(f" - resolved: {len(payload['resolvedViolations'])}")
            print(f" - known candidates: {len(payload['candidates']) - len(payload['newCandidates'])}")
            print(f" - new candidates: {len(payload['newCandidates'])}")
            print(f" - touched production files: {len(payload['touchedFiles'])}")
            print(f" - primitive mapping: {payload['mappingReconciliation']['confirmed']} confirmed + {payload['mappingReconciliation']['candidates']} unresolved")
            for failure in payload["policyFailures"]: print(" -", failure)
        else:
            for err in payload.get("errors", []): print(" -", err)
    return code


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        raise
    except Exception as exc:
        print(f"V60 DESIGN SYSTEM RATCHET: TOOL_ERROR\n - {type(exc).__name__}: {exc}")
        raise SystemExit(2)
