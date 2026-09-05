---
status: ACTIVE
scope: current v73 synchronization architecture
owner: AutoDrive Engineering
last_verified_against: AutoDrive-v73
last_verified_date: 2026-08-22
supersedes: earlier sync verification/design descriptions as current authority
---

# Sync Architecture

## Canonical contract constants

From `UnifiedSyncProtocol.kt`:

```text
STREAM = autodrive-global-change-v1
CONTRACT_VERSION = 2
PAGE_SIZE = 200
MAX_PAGES_PER_CYCLE = 50
BOOTSTRAP_PAGE_SIZE = 500
RECONCILIATION_CONTRACT_VERSION = 1
```

## Authority model

```text
local mutation → Room transaction + scoped Outbox
                         ↓
                  idempotent command RPC
                         ↓
              server receipt / shared state
                         ↓
server revision change feed → scoped Inbox → Room
                         ↓
                  UI reads Room
```

Realtime is a **hint/acceleration channel**, not a correctness authority.

## Coordinator and phase order

`DefaultSyncCoordinator` coalesces requests into a generation-draining single active run. `SyncManager` currently performs:

1. `AUTH`: await Supabase auth initialization/session.
2. `PENDING_OPERATIONS`: recover expired Outbox leases and flush durable mutations.
3. canonical pull under the `PROFILE` phase label: ensure a canonical cursor (bootstrap when missing), then consume unified changes; expired cursor triggers bootstrap + resumed delta pull.
4. `DELETIONS`: phase compatibility marker; canonical `DELETE` operations are already represented in the unified change stream.
5. `RECONCILE`: anti-entropy when the canonical delta run is at head; a reconciliation demand can force rebootstrap.
6. coordinator `REALTIME`: restart feature Realtime participants after engine work.

The phase name `PROFILE` is a legacy enum label around the broader canonical change-feed pull; it must not be interpreted as profile-only synchronization.

## Durable Outbox

`pending_operations` stores exact `(user, client, org)` scope, mutation id, entity identity, operation, payload, contract version, retry/lease state and optional dependency. `OutboxSynchronizer` + `PendingOperationProcessor` deliver supported writes. `IdempotentServerCommandGateway` maps operations to command RPCs and validates receipt identity/status/revision.

Transport timeout/HTTP/IO/PostgREST ambiguity is treated as an ambiguous command outcome, preserving the stable mutation identity for safe retry/replay rather than assuming failure.

## Durable Inbox + revision cursor

`UnifiedChangeSynchronizer` reads an exact-scope revision cursor, fetches `autodrive_sync_changes_v1`, validates contract/scope/monotonic revision/entity/operation/group boundaries, then applies contiguous transaction groups inside Room transactions. Inbox event identity and entity application are committed before/with cursor advancement.

Cursor authority is **server revision**, not `System.currentTimeMillis()` ordering. Local wall-clock timestamps are used for local bookkeeping only.

## Change feed

Client RPC: `autodrive_sync_changes_v1(p_after_revision, p_page_limit)`.

Each event includes event id, monotonic revision, entity type/id, `UPSERT` or `DELETE`, transaction group, contract version and exact server-derived scope. Page data includes head/minimum available/next revision and `has_more`.

The repository SQL in `20260822074200_autodrive_unified_change_feed_v1.sql` implements the intended stream/trigger/RPC side. Session 74 does not claim that migration is deployed to a live target.

## Safe bootstrap

`SafeBootstrapSynchronizer` is used when the canonical cursor is missing/incompatible and on cursor expiry/rebootstrap demand. Flow:

1. begin snapshot → receive bootstrap id + baseline revision;
2. persist exact-scope bootstrap state;
3. page snapshot into durable Room staging (`BOOTSTRAP_PAGE_SIZE=500`);
4. transactionally install staged rows, remove stale local rows against the complete staged inventory, set cursor to baseline revision, initialize reconciliation state, clear staging/state;
5. resume delta feed.

A bootstrap expiration clears staged state and is surfaced explicitly.

## Anti-entropy

`AntiEntropyReconciler` compares a server manifest at a known revision with local canonical projection digests. It defers when pending local mutations or cursor/head conditions make comparison unsafe. Mismatched partitions can be fetched/repaired; conditions requiring stronger recovery cause rebootstrap.

`RECONCILIATION_CONTRACT_VERSION = 1`.

## Deletion

For the current canonical stream, deletion is an explicit `DELETE` event. Older `DeletionSynchronizer`/tombstone extension code remains in the module but is not the primary path invoked by `SyncManager` v73. Absence from bounded/direct reads is not deletion authority.

## Chat recovery

`ChatRecoverySynchronizer` uses `autodrive_chat_recovery_page_v1` with a server sequence cursor and exact scope validation. It persists chat recovery checkpoints and merges remote messages/conversation state into Room. This is a domain recovery path complementary to the global canonical stream.

## Realtime

`RealtimeManager` supervises four participants (billing, balance, chat, notifications) with reconnect backoff/health aggregation. `RealtimeHintDispatcher` requests a coordinator sync; its source comment states Realtime is a wake-up signal only. Realtime outages can degrade freshness but are not designed to destroy convergence authority.

## Exact scope and logout

`SyncScope.from(CurrentSession)` requires non-blank user/client/org. Scope is rechecked before sensitive Room commits. During logout, the departing scope is blocked/quiesced, session is cleared, local account data is cleared under the lifecycle mutex, and the logout barrier is released after best-effort remote sign-out.

## Observability

`sync_observability_state` and `SyncObservabilityStore` record run/cursor/head/bootstrap/reconciliation/outbox/realtime/hint diagnostics. They are **diagnostic only** and must never decide data authority.

## Current server RPCs used by canonical sync

- `autodrive_sync_changes_v1`
- `autodrive_sync_bootstrap_begin_v1`
- `autodrive_sync_bootstrap_page_v1`
- `autodrive_sync_manifest_v1`
- `autodrive_sync_partition_v1`

See [RPC Catalog](../api/RPC_CATALOG.md) for every production server operation, including non-sync calls.
