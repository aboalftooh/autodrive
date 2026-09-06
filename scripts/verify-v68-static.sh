#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

python3 scripts/verify-v68-model.py > /tmp/autodrive-v68-model.json

# Production-only forbidden authority patterns.
if grep -R -n --include='*.kt' -E 'pendingOperationDao\(\)\.deleteAll\(|findActiveByIdempotencyKey|deleteByIdempotencyKey|next_retry_at = :leaseUntil' core feature app/src/main; then
  echo 'FAIL: forbidden v67 Outbox authority remains' >&2
  exit 21
fi
if grep -R -n --include='*.kt' -E 'notificationDao\(\)\.getUnsynced\([^)]*\)\.forEach' core/sync feature; then
  echo 'FAIL: standalone notification sender remains' >&2
  exit 22
fi
if grep -R -n --include='*.kt' 'fallbackToDestructiveMigration' core/database; then
  echo 'FAIL: destructive Room migration fallback present' >&2
  exit 23
fi

# Session 68 must not smuggle UI or server migrations into correctness work.
# These are inventory checks; baseline diff is performed by the outer verification step.
python3 - <<'PY'
from pathlib import Path
root=Path('.')
required=[
 'core/database/src/main/kotlin/com/autodrive/app/core/database/dao/PendingOperationDao.kt',
 'core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/OutboxSynchronizer.kt',
 'feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/data/ProfileRepositoryImpl.kt',
 'feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/data/BalanceRepositoryImpl.kt',
 'feature/chat/src/main/kotlin/com/autodrive/app/feature/chat/data/ChatRepositoryImpl.kt',
 'feature/notifications/src/main/kotlin/com/autodrive/app/feature/notifications/data/NotificationRepositoryImpl.kt',
]
missing=[p for p in required if not (root/p).is_file()]
if missing: raise SystemExit('missing required sources: '+','.join(missing))
PY
cat /tmp/autodrive-v68-model.json
