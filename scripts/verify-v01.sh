#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
echo "v01 scope: static verification only; Gradle build and lint are intentionally excluded."
exec "$ROOT/scripts/verify-v01-static.sh"
