#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
PYTHONDONTWRITEBYTECODE=1 python3 tools/documentation/documentation_drift.py --root .
PYTHONDONTWRITEBYTECODE=1 python3 tools/documentation/test_documentation_drift.py --root .
printf '%s\n' 'DOCUMENTATION_GATE=PASS'
