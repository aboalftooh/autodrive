#!/usr/bin/env python3
"""Shared project layout helpers for the multi-module AutoDrive tree."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def declared_modules() -> dict[str, Path]:
    settings = (ROOT / "settings.gradle.kts").read_text(encoding="utf-8")
    names = ["app"]
    for match in re.finditer(r'include\((.*?)\)', settings, flags=re.S):
        names.extend(re.findall(r'"(:[^"]+)"', match.group(1)))
    result: dict[str, Path] = {}
    for name in names:
        key = name.lstrip(":").replace(":", "/")
        result[key] = ROOT / key
    return result


MODULES = declared_modules()
MAIN_SOURCE_ROOTS = [path / "src/main/kotlin" for path in MODULES.values() if (path / "src/main/kotlin").is_dir()]
TEST_SOURCE_ROOTS = [
    source
    for path in MODULES.values()
    for source in (path / "src/test/kotlin", path / "src/androidTest/kotlin")
    if source.is_dir()
]
ALL_SOURCE_ROOTS = MAIN_SOURCE_ROOTS + TEST_SOURCE_ROOTS


def module_path(name: str) -> Path:
    return MODULES[name]


def source_file(module: str, package_relative_path: str, source_set: str = "main") -> Path:
    return module_path(module) / f"src/{source_set}/kotlin/com/autodrive/app" / package_relative_path
