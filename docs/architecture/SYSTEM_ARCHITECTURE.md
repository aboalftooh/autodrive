---
status: ACTIVE
scope: current system architecture derived from v73 production code
owner: AutoDrive Engineering
last_verified_against: AutoDrive-v73
last_verified_date: 2026-08-22
supersedes: docs/refactor/active-architecture-v14.md; docs/refactor/target-architecture-v14.md
---

# System Architecture

## System context

AutoDrive is an Android modular monolith. The client stores synchronized operational state in Room and integrates with Supabase Auth, PostgREST/RPC, Realtime, Storage, and Edge Functions using an anon-key client. Server-side authorization/tenant enforcement is therefore expected to be provided by authenticated identity and RLS/RPC guards.

## Composition and logical layers

```text
Android UI / ViewModels
        ↓
feature/app repositories + domain contracts
        ↓
Room local state / scoped durable mutations
        ↓
core:sync coordinator and protocol boundaries
        ↓
core:network Supabase client
        ↓
Supabase Auth / RPC / tables / Realtime / Functions / Storage
```

`:app` owns the application object, activity, navigation, app-level DI, Firebase messaging service, and still contains Home/Reports/Competition/Information feature code. Extracted feature modules own Auth, Chat, Notifications, Commission, Balance, and Profile.

See [Module Boundaries](MODULE_BOUNDARIES.md) for the exact 16-module graph.

## Dependency direction

Current Gradle topology keeps `core:*` free of dependencies on feature/app modules. Features depend on reusable core modules; two current cross-feature edges exist (`notifications→chat`, `profile→balance`). `:app` aggregates all modules.

## DI / Hilt boundaries

- `AppModule` creates the singleton Room database and registers `AutoDriveDatabase.ALL_MIGRATIONS`.
- Feature/core Hilt modules bind repository and sync interfaces.
- `RealtimeModule` contributes four `RealtimeParticipant` implementations: billing, balance, chat, notifications.
- `SyncEngineModule`/sync DI binds the coordinator/engine stack; production fault injection is a no-op binding while diagnostics remain separate from correctness authority.

## Data ownership

- `core:database` owns Room entities/DAOs/migrations.
- Room is the local/UI read source for synchronized domains and durable sync control state.
- `core:session` owns `CurrentSession` and encrypted local persistence via `EncryptedSharedPreferences`.
- `core:network` owns the Supabase client and shared network DTOs.
- Feature repositories may expose Room-backed flows and create scoped durable mutations.

## Synchronization ownership

`DefaultSyncCoordinator` serializes/coalesces sync requests. `SyncManager` executes auth → Outbox delivery → canonical pull/bootstrap → reconciliation. Realtime is restarted after the engine run and acts as a hint/acceleration channel; it is not a correctness authority.

Canonical inbound synchronization is server-revision based. The client persists exact-scope cursor, Inbox events, bootstrap state/staging, reconciliation state, and observability state in Room. A missing/expired canonical cursor triggers safe bootstrap; anti-entropy can repair drift or force rebootstrap.

See [Sync Architecture](../data/SYNC_ARCHITECTURE.md).

## Session/auth boundaries

`CurrentSession` carries login/registration state plus user/client/org identity. `SyncScope.from` requires non-blank `userId`, `clientId`, and `orgId`; sync data/state are keyed to that exact scope. Auth uses Supabase session import/restore; sign-out stops Realtime, blocks/quiesces the departing sync scope, clears local account data, then signs out remotely on a best-effort basis.

## Realtime relationship

Four feature participants can receive domain-specific Realtime signals. `RealtimeHintDispatcher` explicitly documents Realtime as “wake-up signal only”; the coordinator owns authoritative pull/apply correctness. Participant failures are supervised/retried and aggregated for health/diagnostics.

## Background/worker responsibilities

`PendingOperationsWorker` supports durable pending-operation delivery; chat also owns media/retry workers. Background execution does not replace the canonical cursor/inbox/reconciliation model.

## Failure boundaries

- Missing exact sync scope or missing Supabase auth session causes sync skip/failure rather than cross-account fallback.
- Outbox transport ambiguity is preserved for replay via stable mutation IDs/server receipts.
- Inbound pages validate scope, contract version, monotonic revision, group boundaries, entity support, and operation type.
- Bootstrap is staged and installed transactionally with the baseline revision.
- Diagnostics/observability are append/update state for troubleshooting and do not authorize data correctness decisions.

## Offline behavior

Room remains readable without Realtime/network. Durable Outbox rows preserve supported local write intent; later connectivity lets the coordinator retry delivery and pull canonical revisions. Not every legacy/direct server feature is guaranteed offline-capable; the statement applies to paths explicitly implemented through Room/sync durability.

## Architectural invariants verified in v73

- 16 Gradle modules.
- Room version 19; no destructive migration fallback configured.
- `Room.databaseBuilder(...).addMigrations(*AutoDriveDatabase.ALL_MIGRATIONS)`.
- Exact sync scope is user/client/org.
- Canonical stream uses revision cursor, not device time.
- Realtime is not the sole convergence path.
- Repository server source proves intended contracts only; deployed server state remains unverified in Session 74.
