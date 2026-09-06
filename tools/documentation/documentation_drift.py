#!/usr/bin/env python3
"""Fail-closed AutoDrive documentation drift checker (Session 75)."""
from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Iterable

VALID_ADR_STATUS = {"DRAFT", "ACTIVE", "SUPERSEDED", "ARCHIVED"}
REQUIRED_METADATA = {
    "status",
    "scope",
    "owner",
    "last_verified_against",
    "last_verified_date",
    "supersedes",
}

@dataclass
class CheckResult:
    code: str
    name: str
    passed: bool
    details: list[str]


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def _front_matter(path: Path) -> dict[str, str]:
    text = _read(path)
    if not text.startswith("---\n"):
        return {}
    end = text.find("\n---\n", 4)
    if end < 0:
        return {}
    out: dict[str, str] = {}
    for line in text[4:end].splitlines():
        if ":" not in line:
            continue
        k, v = line.split(":", 1)
        out[k.strip()] = v.strip()
    return out


def _canonical_rows(root: Path) -> list[dict[str, str]]:
    path = root / "docs/CANONICAL_DOCUMENT_MAP.md"
    text = _read(path)
    rows = []
    for line in text.splitlines():
        if not line.startswith("|") or "`" not in line:
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) < 7 or cells[0] in {"Concern", "---"}:
            continue
        m = re.fullmatch(r"`([^`]+)`", cells[1])
        if not m:
            continue
        rows.append({
            "concern": cells[0],
            "path": m.group(1),
            "status": cells[2],
            "authority": cells[3],
            "verified": cells[4],
            "supersedes": cells[5],
            "notes": cells[6],
        })
    return rows


def strip_kotlin_comments(text: str) -> str:
    """Remove Kotlin line/block comments while preserving strings/chars and newlines."""
    out: list[str] = []
    i, n, state, depth = 0, len(text), "code", 0
    while i < n:
        if state == "code":
            if text.startswith('"""', i):
                out.append('"""'); i += 3; state = "triple"; continue
            c = text[i]
            if c == '"':
                out.append(c); i += 1; state = "string"; continue
            if c == "'":
                out.append(c); i += 1; state = "char"; continue
            if text.startswith("//", i):
                out.append(" "); i += 2; state = "line"; continue
            if text.startswith("/*", i):
                out.append(" "); i += 2; state = "block"; depth = 1; continue
            out.append(c); i += 1
        elif state == "line":
            if text[i] == "\n":
                out.append("\n"); i += 1; state = "code"
            else:
                i += 1
        elif state == "block":
            if text.startswith("/*", i):
                depth += 1; i += 2
            elif text.startswith("*/", i):
                depth -= 1; i += 2
                if depth == 0:
                    state = "code"
            else:
                if text[i] == "\n":
                    out.append("\n")
                i += 1
        elif state == "string":
            c = text[i]; out.append(c); i += 1
            if c == "\\" and i < n:
                out.append(text[i]); i += 1
            elif c == '"':
                state = "code"
        elif state == "char":
            c = text[i]; out.append(c); i += 1
            if c == "\\" and i < n:
                out.append(text[i]); i += 1
            elif c == "'":
                state = "code"
        else:  # triple string
            if text.startswith('"""', i):
                out.append('"""'); i += 3; state = "code"
            else:
                out.append(text[i]); i += 1
    return "".join(out)


def _production_kotlin(root: Path) -> Iterable[Path]:
    for p in root.rglob("*.kt"):
        if "/src/main/" in p.as_posix():
            yield p


def _extract_server_operations(root: Path) -> set[str]:
    ops: set[str] = set()
    for p in _production_kotlin(root):
        text = strip_kotlin_comments(_read(p))
        patterns = [
            r"\.rpc\s*\(\s*\"([^\"]+)\"",
            r"rpcName\s*=\s*\"([^\"]+)\"",
            r"functions\.invoke\s*\(\s*function\s*=\s*\"([^\"]+)\"",
        ]
        for pat in patterns:
            ops.update(re.findall(pat, text, flags=re.S))
    return ops


def _catalog_operations(root: Path) -> set[str]:
    text = _read(root / "docs/api/RPC_CATALOG.md")
    names: set[str] = set()
    for line in text.splitlines():
        if not line.startswith("|") or line.startswith("|---"):
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) >= 2 and cells[0] not in {"Canonical name", ""}:
            names.add(cells[0])
    return names


def check_d1(root: Path) -> CheckResult:
    code_text = _read(root / "core/database/src/main/kotlin/com/autodrive/app/core/database/AutoDriveDatabase.kt")
    doc_text = _read(root / "docs/data/DATABASE.md")
    cm = re.search(r"AUTODRIVE_DATABASE_VERSION\s*=\s*(\d+)", code_text)
    dm = re.search(r"AUTODRIVE_DATABASE_VERSION\s*=\s*(\d+)", doc_text)
    details = []
    if not cm or not dm:
        details.append("Unable to parse Room version from code/docs")
        return CheckResult("D1", "Room version drift", False, details)
    cv, dv = int(cm.group(1)), int(dm.group(1))
    ok = cv == dv
    details.append(f"Room version code={cv} docs={dv}")
    return CheckResult("D1", "Room version drift", ok, details)


def check_d2(root: Path) -> CheckResult:
    settings = _read(root / "settings.gradle.kts")
    code_modules = set(re.findall(r'include\s*\((.*?)\)', settings, flags=re.S))
    modules: set[str] = set()
    for group in code_modules:
        modules.update(re.findall(r'"(:[^\"]+)"', group))
    doc = _read(root / "docs/architecture/MODULE_BOUNDARIES.md")
    documented = set(re.findall(r"^\| `(:[^`]+)` \|", doc, flags=re.M))
    missing = sorted(modules - documented)
    stale = sorted(documented - modules)
    ok = not missing and not stale and len(modules) == len(documented)
    details = [f"settings modules={len(modules)} documented modules={len(documented)}"]
    if missing: details.append("missing documented: " + ", ".join(missing))
    if stale: details.append("stale documented: " + ", ".join(stale))
    return CheckResult("D2", "Gradle module drift", ok, details)


def check_d3(root: Path) -> CheckResult:
    production = _extract_server_operations(root)
    catalog = _catalog_operations(root)
    missing = sorted(production - catalog)
    stale = sorted(catalog - production)
    ok = not missing and not stale
    details = [f"production operations={len(production)} catalog operations={len(catalog)}"]
    if missing: details.append("undocumented production operations: " + ", ".join(missing))
    if stale: details.append("stale active catalog operations: " + ", ".join(stale))
    return CheckResult("D3", "RPC / server operation drift", ok, details)


def check_d4(root: Path) -> CheckResult:
    rows = _canonical_rows(root)
    active = [r for r in rows if r["status"] == "ACTIVE"]
    details = [f"ACTIVE concerns={len(active)} registry rows={len(rows)}"]
    errors: list[str] = []
    concerns = [r["concern"] for r in active]
    paths = [r["path"] for r in active]
    for value in sorted({x for x in concerns if concerns.count(x) > 1}):
        errors.append(f"duplicate ACTIVE concern: {value}")
    for value in sorted({x for x in paths if paths.count(x) > 1}):
        errors.append(f"duplicate ACTIVE path: {value}")
    for row in active:
        if not (root / row["path"]).is_file():
            errors.append(f"ACTIVE path missing: {row['path']}")
    # A reserved path cannot simultaneously appear as active authority elsewhere.
    reserved_paths = {r["path"] for r in rows if r["status"].startswith("RESERVED")}
    overlap = reserved_paths & set(paths)
    for p in sorted(overlap):
        errors.append(f"path both RESERVED and ACTIVE: {p}")
    details.extend(errors)
    return CheckResult("D4", "Canonical registry integrity", not errors and bool(active), details)


def _link_scan_files(root: Path) -> set[Path]:
    files: set[Path] = {root / "README.md", root / "docs/INDEX.md", root / "docs/architecture/adr/ADR_INDEX.md"}
    for r in _canonical_rows(root):
        if r["status"] == "ACTIVE":
            files.add(root / r["path"])
    for p in (root / "docs/archive").rglob("INDEX.md"):
        files.add(p)
    for p in (root / "docs/architecture/adr").glob("ADR-*.md"):
        files.add(p)
    return {p for p in files if p.is_file()}


def _resolve_link(source: Path, target: str, root: Path) -> tuple[bool, str]:
    target = target.strip()
    if not target or target.startswith("#"):
        return True, ""
    if re.match(r"^[a-zA-Z][a-zA-Z0-9+.-]*:", target):
        return True, ""
    raw = target.split("#", 1)[0].split("?", 1)[0]
    if not raw:
        return True, ""
    # absolute repository-style links are resolved from repository root; normal links from source dir.
    resolved = (root / raw.lstrip("/")) if raw.startswith("/") else (source.parent / raw)
    resolved = resolved.resolve()
    try:
        resolved.relative_to(root.resolve())
    except ValueError:
        return False, f"escapes repository: {target}"
    return resolved.exists(), target


def check_d5(root: Path) -> CheckResult:
    broken: list[str] = []
    link_re = re.compile(r"(?<!!)\[[^\]]*\]\(([^)]+)\)")
    for p in sorted(_link_scan_files(root)):
        text = _read(p)
        for target in link_re.findall(text):
            # optional markdown title: keep first token unless angle-wrapped
            target = target.strip()
            if target.startswith("<") and ">" in target:
                target = target[1:target.index(">")]
            elif " \"" in target:
                target = target.split(" \"", 1)[0]
            ok, why = _resolve_link(p, target, root)
            if not ok:
                broken.append(f"{p.relative_to(root)} -> {why}")
    details = [f"scanned files={len(_link_scan_files(root))} broken local links={len(broken)}"] + broken[:30]
    return CheckResult("D5", "Broken local links", not broken, details)


def check_d6(root: Path) -> CheckResult:
    bad = []
    for r in _canonical_rows(root):
        if r["status"] != "ACTIVE":
            continue
        p = Path(r["path"])
        name = p.name
        parts = p.parts
        if name.endswith("_CURRENT.md") or any(part.startswith("active-") or part.startswith("target-") for part in parts):
            bad.append(r["path"])
    details = [f"stale-current ACTIVE authorities={len(bad)}"] + bad
    return CheckResult("D6", "Stale-current naming authority", not bad, details)


def check_d7(root: Path) -> CheckResult:
    errors: list[str] = []
    active = [r for r in _canonical_rows(root) if r["status"] == "ACTIVE"]
    for r in active:
        p = root / r["path"]
        if not p.is_file():
            errors.append(f"missing before metadata check: {r['path']}")
            continue
        md = _front_matter(p)
        missing = sorted(REQUIRED_METADATA - md.keys())
        if missing:
            errors.append(f"{r['path']}: missing metadata {','.join(missing)}")
            continue
        if md.get("status") != r["status"]:
            errors.append(f"{r['path']}: metadata status={md.get('status')} registry={r['status']}")
        if md.get("owner") != "AutoDrive Engineering":
            errors.append(f"{r['path']}: owner must be AutoDrive Engineering")
        if not re.fullmatch(r"\d{4}-\d{2}-\d{2}", md.get("last_verified_date", "")):
            errors.append(f"{r['path']}: invalid last_verified_date")
        for k in REQUIRED_METADATA:
            if not md.get(k, "").strip():
                errors.append(f"{r['path']}: blank metadata {k}")
    details = [f"ACTIVE metadata documents={len(active)} errors={len(errors)}"] + errors
    return CheckResult("D7", "Canonical metadata enforcement", not errors and bool(active), details)


def check_d8(root: Path) -> CheckResult:
    index = root / "docs/architecture/adr/ADR_INDEX.md"
    errors: list[str] = []
    if not index.is_file():
        return CheckResult("D8", "ADR integrity", False, ["ADR_INDEX.md missing"])
    text = _read(index)
    entries = re.findall(r"\| \[ADR-(\d{4})\]\(([^)]+)\) \| ([A-Z_]+) \|", text)
    nums = [n for n, _, _ in entries]
    for n in sorted({x for x in nums if nums.count(x) > 1}):
        errors.append(f"duplicate ADR number: {n}")
    for n, target, status in entries:
        if status not in VALID_ADR_STATUS:
            errors.append(f"ADR-{n}: invalid index status {status}")
        p = index.parent / target
        if not p.is_file():
            errors.append(f"ADR-{n}: missing target {target}")
            continue
        md = _front_matter(p)
        if md.get("status") != status:
            errors.append(f"ADR-{n}: metadata status={md.get('status')} index={status}")
        body = _read(p)
        for heading in ["Context", "Decision", "Alternatives considered", "Consequences", "Status", "Supersedes", "Superseded by", "Verified baseline"]:
            if f"## {heading}" not in body:
                errors.append(f"ADR-{n}: missing section {heading}")
    required = {"0001", "0002", "0003", "0004"}
    if not required.issubset(set(nums)):
        errors.append("required initial ADRs missing: " + ",".join(sorted(required - set(nums))))
    details = [f"ADR entries={len(entries)} errors={len(errors)}"] + errors
    return CheckResult("D8", "ADR integrity", not errors and bool(entries), details)


def _symbol_has_kdoc(text: str, symbol: str) -> bool:
    # A KDoc block must directly precede optional annotations then the declaration.
    sym = re.escape(symbol)
    pattern = re.compile(
        r"/\*\*(?:(?!\*/).)*\*/\s*"
        r"(?:@[A-Za-z_][A-Za-z0-9_.]*(?:\([^\n]*?\))?\s*)*"
        r"(?:(?:data|sealed|enum|value)\s+)?(?:class|interface|object)\s+" + sym + r"\b",
        re.S,
    )
    return bool(pattern.search(text))


def check_d9(root: Path) -> CheckResult:
    manifest = root / "tools/documentation/critical_kdoc_targets.json"
    errors: list[str] = []
    try:
        data = json.loads(_read(manifest))
    except Exception as exc:
        return CheckResult("D9", "Critical KDoc contracts", False, [f"manifest unreadable: {exc}"])
    targets = data.get("targets", [])
    seen = set()
    for item in targets:
        path = item.get("path", "")
        symbol = item.get("symbol", "")
        key = (path, symbol)
        if key in seen:
            errors.append(f"duplicate target: {path}:{symbol}")
            continue
        seen.add(key)
        p = root / path
        if not p.is_file():
            errors.append(f"missing target file: {path}")
            continue
        if not _symbol_has_kdoc(_read(p), symbol):
            errors.append(f"missing KDoc: {path}:{symbol}")
    details = [f"critical targets={len(targets)} errors={len(errors)}"] + errors
    return CheckResult("D9", "Critical KDoc contracts", not errors and bool(targets), details)


def check_d10(root: Path) -> CheckResult:
    required = [
        "docs/operations/BUILD_AND_TEST.md",
        "docs/operations/RELEASE.md",
        "docs/operations/TROUBLESHOOTING.md",
    ]
    errors = [f"missing {p}" for p in required if not (root / p).is_file()]
    idx = _read(root / "docs/INDEX.md") if (root / "docs/INDEX.md").is_file() else ""
    for p in required:
        rel = p.removeprefix("docs/")
        if f"]({rel})" not in idx:
            errors.append(f"docs/INDEX.md does not link {rel}")
    details = [f"operations docs required={len(required)} errors={len(errors)}"] + errors
    return CheckResult("D10", "Operations documentation presence", not errors, details)


def check_d11(root: Path) -> CheckResult:
    errors: list[str] = []
    for rel in ["docs/development/CONTRIBUTING.md", "docs/development/DOCUMENTATION_STANDARD.md"]:
        p = root / rel
        if not p.is_file():
            errors.append(f"missing {rel}")
            continue
        text = _read(p)
        for phrase in ["Documentation impact", "Canonical docs affected", "Drift check"]:
            if phrase not in text:
                errors.append(f"{rel}: missing policy phrase '{phrase}'")
    details = [f"session-policy errors={len(errors)}"] + errors
    return CheckResult("D11", "Documentation session impact policy", not errors, details)


def run_checks(root: Path) -> list[CheckResult]:
    root = root.resolve()
    checks = [check_d1, check_d2, check_d3, check_d4, check_d5, check_d6, check_d7, check_d8, check_d9, check_d10, check_d11]
    results: list[CheckResult] = []
    for fn in checks:
        try:
            results.append(fn(root))
        except Exception as exc:
            results.append(CheckResult(fn.__name__.replace("check_", "").upper(), fn.__name__, False, [f"checker exception: {type(exc).__name__}: {exc}"]))
    return results


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=".")
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()
    results = run_checks(Path(args.root))
    passed = all(r.passed for r in results)
    if args.json:
        print(json.dumps({"passed": passed, "checks": [asdict(r) for r in results]}, indent=2, sort_keys=True))
    else:
        for r in results:
            print(f"{r.code} {'PASS' if r.passed else 'FAIL'} — {r.name}")
            for d in r.details:
                print(f"  {d}")
        print(f"DOCUMENTATION_DRIFT_GATE={'PASS' if passed else 'FAIL'}")
    return 0 if passed else 1

if __name__ == "__main__":
    raise SystemExit(main())
