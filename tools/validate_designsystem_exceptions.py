#!/usr/bin/env python3
"""Validate the AutoDrive Design System exception ledger (Session 60)."""
from __future__ import annotations
import argparse
import json
import re
import sys
from pathlib import Path
from typing import Callable, Any

ROOT = Path(__file__).resolve().parents[1]
LEDGER_DEFAULT = ROOT / "core/designsystem/verification/designsystem-exceptions.json"
ALLOWED_RULES = {
    "DS-A11Y-001", "DS-A11Y-002", "DS-A11Y-003", "DS-BORDER-001",
    "DS-COLOR-001", "DS-CONTRACT-001", "DS-CONTRAST-001", "DS-DUP-001",
    "DS-ELEVATION-001", "DS-EXCEPTION-001", "DS-MATERIAL-001", "DS-SHAPE-001",
    "DS-SPACE-001", "DS-TYPE-001",
}
GENERIC_REASONS = {"temporary", "temp", "todo", "general", "exception", "waiver", "later"}
VERSION_RE = re.compile(r"^v(\d+)$")
WILDCARD_RE = re.compile(r"[*?\[\]]")


def validate_document(
    records: Any,
    *,
    root: Path = ROOT,
    current_version: int = 60,
    file_exists: Callable[[Path], bool] | None = None,
) -> list[str]:
    """Return policy errors. Malformed JSON itself is handled by the CLI as a tool error."""
    errors: list[str] = []
    exists = file_exists or (lambda p: p.is_file())
    if not isinstance(records, list):
        return ["ledger root must be a JSON array"]
    seen: set[str] = set()
    for index, rec in enumerate(records):
        prefix = f"record[{index}]"
        if not isinstance(rec, dict):
            errors.append(f"{prefix}: must be an object")
            continue
        eid = rec.get("id")
        if not isinstance(eid, str) or not eid.strip():
            errors.append(f"{prefix}: id must be nonblank")
        elif eid in seen:
            errors.append(f"{prefix}: duplicate id: {eid}")
        else:
            seen.add(eid)

        rule = rec.get("rule_id")
        if rule not in ALLOWED_RULES:
            errors.append(f"{prefix}: unknown rule_id: {rule!r}")
        if isinstance(rule, str) and WILDCARD_RE.search(rule):
            errors.append(f"{prefix}: wildcard rule_id is forbidden")

        rel = rec.get("file")
        if not isinstance(rel, str) or not rel.strip():
            errors.append(f"{prefix}: file must be an exact nonblank path")
        else:
            if WILDCARD_RE.search(rel):
                errors.append(f"{prefix}: wildcard file path is forbidden")
            p = Path(rel)
            if p.is_absolute() or ".." in p.parts:
                errors.append(f"{prefix}: file must be a project-relative exact path")
            elif not WILDCARD_RE.search(rel) and not exists(root / p):
                errors.append(f"{prefix}: file does not exist: {rel}")

        symbol = rec.get("symbol")
        if not isinstance(symbol, str) or not symbol.strip():
            errors.append(f"{prefix}: symbol must be nonblank")

        line_hint = rec.get("line_hint")
        if not isinstance(line_hint, int) or isinstance(line_hint, bool) or line_hint <= 0:
            errors.append(f"{prefix}: line_hint must be a positive integer")

        reason = rec.get("reason")
        if not isinstance(reason, str) or not reason.strip():
            errors.append(f"{prefix}: reason must be specific and nonblank")
        else:
            normalized = reason.strip().lower().rstrip(".")
            if normalized in GENERIC_REASONS or len(reason.strip()) < 12:
                errors.append(f"{prefix}: reason is too general")

        owner = rec.get("owner")
        if not isinstance(owner, str) or not owner.strip():
            errors.append(f"{prefix}: owner must be nonblank")

        approved = rec.get("approved_in")
        am = VERSION_RE.fullmatch(approved) if isinstance(approved, str) else None
        if not am:
            errors.append(f"{prefix}: approved_in must be v<integer>")

        expires = rec.get("expires_in")
        em = VERSION_RE.fullmatch(expires) if isinstance(expires, str) else None
        if not em:
            errors.append(f"{prefix}: expires_in must be v<integer>")
        elif int(em.group(1)) <= current_version:
            errors.append(f"{prefix}: exception expired in {expires}")

        plan = rec.get("replacement_plan")
        if not isinstance(plan, str) or not plan.strip():
            errors.append(f"{prefix}: replacement_plan must be nonblank")

    return sorted(errors)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--ledger", type=Path, default=None)
    parser.add_argument("--json", action="store_true", dest="json_mode")
    args = parser.parse_args(argv)
    root = args.root.resolve()
    ledger = args.ledger or (root / "core/designsystem/verification/designsystem-exceptions.json")
    try:
        if not ledger.is_file():
            raise FileNotFoundError(str(ledger))
        data = json.loads(ledger.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        payload = {"schemaVersion": 60, "tool": "exception-validator", "errors": [str(exc)], "verdict": "TOOL_ERROR"}
        print(json.dumps(payload, ensure_ascii=False, sort_keys=True) if args.json_mode else f"EXCEPTION VALIDATOR: TOOL_ERROR\n - {exc}")
        return 2

    errors = validate_document(data, root=root)
    verdict = "PASS" if not errors else "FAIL_EXCEPTION_LEDGER"
    payload = {
        "schemaVersion": 60,
        "tool": "exception-validator",
        "activeExceptionCount": len(data) if isinstance(data, list) else 0,
        "errors": errors,
        "verdict": verdict,
    }
    if args.json_mode:
        print(json.dumps(payload, ensure_ascii=False, sort_keys=True))
    else:
        print(f"EXCEPTION VALIDATOR: {verdict}")
        print(f" - active exceptions: {payload['activeExceptionCount']}")
        for err in errors:
            print(" -", err)
    return 0 if not errors else 1


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        raise
    except Exception as exc:  # tool failures must never masquerade as policy failures
        print(f"EXCEPTION VALIDATOR: TOOL_ERROR\n - {type(exc).__name__}: {exc}")
        raise SystemExit(2)
