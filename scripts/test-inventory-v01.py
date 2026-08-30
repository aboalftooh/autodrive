#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TEST_RE = re.compile(r"@Test\b")

@dataclass(frozen=True)
class Entry:
    path: Path
    count: int
    category: str


def classify(path: Path, text: str) -> str:
    normalized = path.as_posix()
    if "/src/androidTest/" in normalized:
        return "instrumentation"
    if "/src/test/" in normalized and "/architecture/" in normalized:
        return "architecture"
    if "/src/test/" in normalized:
        gradle_markers = (
            "import android.",
            "import androidx.",
            "import io.mockk.",
        )
        if any(marker in text for marker in gradle_markers):
            return "gradle_bound_unit"
        return "pure_unit"
    return "unclassified"


def main() -> int:
    entries: list[Entry] = []
    for module in ("app", "core", "feature"):
        for path in sorted((ROOT / module).rglob("*.kt")):
            text = path.read_text(encoding="utf-8", errors="replace")
            count = len(TEST_RE.findall(text))
            if count:
                entries.append(Entry(path.relative_to(ROOT), count, classify(path, text)))

    totals: dict[str, int] = {}
    files: dict[str, int] = {}
    for entry in entries:
        totals[entry.category] = totals.get(entry.category, 0) + entry.count
        files[entry.category] = files.get(entry.category, 0) + 1

    expected = {
        "pure_unit": 48,
        "gradle_bound_unit": 10,
        "architecture": 81,
        "instrumentation": 3,
    }

    print("v01 test inventory")
    print("category | files | tests")
    print("--- | ---: | ---:")
    for category in ("pure_unit", "gradle_bound_unit", "architecture", "instrumentation"):
        print(f"{category} | {files.get(category, 0)} | {totals.get(category, 0)}")
    print(f"TOTAL | {len(entries)} | {sum(totals.values())}")

    unknown = [entry for entry in entries if entry.category == "unclassified"]
    if unknown:
        for entry in unknown:
            print(f"ERROR: unclassified test file: {entry.path}", file=sys.stderr)
        return 1

    mismatches = [
        f"{key}: expected {value}, found {totals.get(key, 0)}"
        for key, value in expected.items()
        if totals.get(key, 0) != value
    ]
    if sum(totals.values()) != 142:
        mismatches.append(f"total: expected 142, found {sum(totals.values())}")

    if mismatches:
        for mismatch in mismatches:
            print(f"ERROR: {mismatch}", file=sys.stderr)
        return 1

    print("inventory gate: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
