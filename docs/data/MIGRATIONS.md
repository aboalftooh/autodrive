---
status: ACTIVE
scope: current explicit Room migration chain
owner: AutoDrive Engineering
last_verified_against: AutoDrive-v73
last_verified_date: 2026-08-22
supersedes: docs/refactor/database-migrations.md
---

# Migrations

## Current state

- Current Room version: **19**.
- Oldest explicit supported migration baseline in `ALL_MIGRATIONS`: **4**.
- Destructive fallback: **not configured**.
- Runtime registration: `Room.databaseBuilder(...).addMigrations(*AutoDriveDatabase.ALL_MIGRATIONS)`.

## Explicit chain

| From | To | Code-derived purpose |
|---:|---:|---|
| 4 | 5 | Add `conversations.subject`. |
| 5 | 6 | Add chat media URL/MIME/duration columns. |
| 6 | 7 | Create `dynamo_content`. |
| 7 | 8 | Add `chat_messages.local_path`. |
| 8 | 9 | Create `weekly_leaderboard_cache`. |
| 9 | 10 | Add `notifications.nav_route`. |
| 10 | 11 | Rebuild legacy pending operations into retry/idempotency-aware Outbox shape and indexes. |
| 11 | 12 | Rebuild money-bearing tables from SQLite `REAL` to decimal text representation. |
| 12 | 13 | Add DAO-driven indexes; avoids imposing foreign keys that conflict with offline/out-of-order arrival. |
| 13 | 14 | Create exact-scope `sync_cursors`. |
| 14 | 15 | Rebuild Outbox as exact-scope durable mutations with mutation identity, lease/retry state and safety checks for legacy ownership. |
| 15 | 16 | Create durable exact-scope `sync_inbox`. |
| 16 | 17 | Add Outbox dependency, chat media object path, chat recovery checkpoints and durable chat media transfer queue. |
| 17 | 18 | Add `payments.client_id`; create safe bootstrap staging/state and reconciliation state. |
| 18 | 19 | Add exact-scope diagnostic-only `sync_observability_state`. |

## Policy derived from code/comments

1. Historical migrations are append-only implementation history; do not rewrite old steps to resemble the latest schema.
2. Any future Room version bump requires a connected migration object and addition to `ALL_MIGRATIONS` before the version constant is raised.
3. Keep destructive fallback absent unless a separately approved data-loss policy explicitly changes that architecture.
4. Preserve schema exports under `core/database/schemas/...` and update migration verification evidence when runtime instrumentation is actually executed.
5. A static/schema check is not a substitute for Android migration instrumentation.

The old `docs/refactor/database-migrations.md` claimed current version 10 and is therefore `SUPERSEDED` in Session 74.
