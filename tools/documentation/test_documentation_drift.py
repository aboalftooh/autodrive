#!/usr/bin/env python3
"""Negative mutation suite: prove D1-D9 fail closed without mutating the working tree."""
from __future__ import annotations

import argparse
import shutil
import tempfile
import sys
sys.dont_write_bytecode = True
from pathlib import Path

from documentation_drift import run_checks


def mutate_text(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise RuntimeError(f"mutation marker missing in {path}: {old!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=".")
    args = ap.parse_args()
    source = Path(args.root).resolve()

    base = run_checks(source)
    if not all(r.passed for r in base):
        print("BASELINE_FAIL: negative suite requires a clean base gate")
        for r in base:
            if not r.passed:
                print(f"  {r.code}: {'; '.join(r.details)}")
        return 2

    cases = []
    with tempfile.TemporaryDirectory(prefix="autodrive-doc-negative-") as td:
        temp_root = Path(td) / "repo"
        shutil.copytree(source, temp_root, symlinks=True)

        def run_case(name: str, expected: str, mutate, restore) -> None:
            mutate()
            results = run_checks(temp_root)
            target = next((r for r in results if r.code == expected), None)
            ok = target is not None and not target.passed and not all(r.passed for r in results)
            cases.append((name, expected, ok, [] if target is None else target.details))
            restore()
            restored = run_checks(temp_root)
            if not all(r.passed for r in restored):
                raise RuntimeError(f"restore after {name} did not recover clean gate")

        # N1
        p = temp_root / "docs/data/DATABASE.md"; original = p.read_text()
        run_case("N1 change documented Room version", "D1",
                 lambda: mutate_text(p, "AUTODRIVE_DATABASE_VERSION = 19", "AUTODRIVE_DATABASE_VERSION = 999"),
                 lambda: p.write_text(original))

        # N2
        p = temp_root / "settings.gradle.kts"; original = p.read_text()
        run_case("N2 inject undocumented Gradle module", "D2",
                 lambda: p.write_text(original + '\ninclude(":feature:documentation-negative")\n'),
                 lambda: p.write_text(original))

        # N3
        p = temp_root / "core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/UnifiedSyncProtocol.kt"; original = p.read_text()
        run_case("N3 inject undocumented RPC identifier", "D3",
                 lambda: p.write_text(original + '\nprivate val documentationNegativeRpc = client.rpc("autodrive_documentation_negative_rpc_v1")\n'),
                 lambda: p.write_text(original))

        # N4
        p = temp_root / "docs/operations/RELEASE.md"; original = p.read_bytes()
        run_case("N4 delete ACTIVE canonical path", "D4",
                 lambda: p.unlink(),
                 lambda: p.write_bytes(original))

        # N5
        p = temp_root / "README.md"; original = p.read_text()
        run_case("N5 break local Markdown link", "D5",
                 lambda: p.write_text(original + '\n[negative broken link](docs/does-not-exist.md)\n'),
                 lambda: p.write_text(original))

        # N6
        p = temp_root / "docs/CANONICAL_DOCUMENT_MAP.md"; original = p.read_text()
        bad_row = "| Negative stale authority | `BUILD_REPORT_CURRENT.md` | ACTIVE | Historical report | AutoDrive-v75 | NONE | negative mutation |\n"
        run_case("N6 promote stale CURRENT report authority", "D6",
                 lambda: p.write_text(original + "\n" + bad_row),
                 lambda: p.write_text(original))

        # N7
        p = temp_root / "docs/development/CONTRIBUTING.md"; original = p.read_text()
        run_case("N7 remove canonical metadata", "D7",
                 lambda: mutate_text(p, "owner: AutoDrive Engineering\n", ""),
                 lambda: p.write_text(original))

        # N8
        p = temp_root / "docs/architecture/adr/ADR_INDEX.md"; original = p.read_text()
        run_case("N8 break ADR index", "D8",
                 lambda: mutate_text(p, "ADR-0001-room-local-ui-source-of-truth.md", "ADR-0001-missing.md"),
                 lambda: p.write_text(original))

        # N9
        p = temp_root / "core/session/src/main/kotlin/com/autodrive/app/core/session/domain/SessionReader.kt"; original = p.read_text()
        run_case("N9 remove required KDoc target", "D9",
                 lambda: mutate_text(p, "/**\n * Read boundary", "/*\n * Read boundary"),
                 lambda: p.write_text(original))

    passed = sum(1 for _, _, ok, _ in cases if ok)
    for name, expected, ok, details in cases:
        print(f"{'PASS' if ok else 'FAIL'} {name} -> {expected}")
        if not ok:
            for d in details: print(f"  {d}")
    print(f"NEGATIVE_MUTATION_TESTS={passed}/{len(cases)}")
    return 0 if passed == 9 and len(cases) == 9 else 1

if __name__ == "__main__":
    raise SystemExit(main())
