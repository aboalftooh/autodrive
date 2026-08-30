#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
python3 "$ROOT/scripts/verify-v67-model.py" > "$ROOT/.verification-v67/model.json"
python3 "$ROOT/scripts/verify-v67-static.py" > "$ROOT/AUTODRIVE_SYNC_VERIFICATION_v67.json"
