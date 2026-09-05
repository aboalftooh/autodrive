---
status: ACTIVE
scope: current Room schema and local data authority
owner: AutoDrive Engineering
last_verified_against: AutoDrive-v73
last_verified_date: 2026-08-22
supersedes: docs/refactor/database-migrations.md and older Room descriptions
---

# Database

## Current Room contract

Source: `core/database/src/main/kotlin/com/autodrive/app/core/database/AutoDriveDatabase.kt` plus exported schema `core/database/schemas/com.autodrive.app.core.database.AutoDriveDatabase/19.json`.

- `AUTODRIVE_DATABASE_VERSION = 19`
- `exportSchema = true`
- `fallbackToDestructiveMigration` is absent from the database setup.
- `AppModule` builds Room with `.addMigrations(*AutoDriveDatabase.ALL_MIGRATIONS)`.
- Exported schema 19 contains **21 entities**, **29 indexes**, and **0 declared foreign keys**.

## Entity inventory

| Room table | Primary key | Exported indexes |
|---|---|---:|
| `invoices` | `id` | 1 |
| `payments` | `id` | 2 |
| `commission_payments` | `id` | 1 |
| `marketer_balance` | `id` | 1 |
| `balance_transactions` | `id` | 2 |
| `withdrawal_requests` | `id` | 3 |
| `notifications` | `id` | 2 |
| `autodrive_users` | `id` | 1 |
| `pending_operations` | `id` | 3 |
| `conversations` | `id` | 3 |
| `chat_messages` | `id` | 3 |
| `dynamo_content` | `id` | 1 |
| `weekly_leaderboard_cache` | `id` | 1 |
| `sync_cursors` | `user_id, client_id, org_id, stream` | 0 |
| `sync_inbox` | `user_id, client_id, org_id, stream, event_id` | 1 |
| `chat_recovery_checkpoints` | `user_id, client_id, org_id, conversation_id` | 0 |
| `chat_media_transfers` | `transfer_id` | 2 |
| `sync_bootstrap_state` | `user_id, client_id, org_id, stream` | 1 |
| `sync_bootstrap_staging` | `user_id, client_id, org_id, bootstrap_id, entity_type, entity_id` | 1 |
| `sync_reconciliation_state` | `user_id, client_id, org_id, stream` | 0 |
| `sync_observability_state` | `user_id, client_id, org_id, stream` | 0 |

## DAO responsibility map

- invoices/payments/commission payments: `InvoiceDao`, `PaymentDao`, `CommissionPaymentDao`
- balance/withdrawals: `MarketerBalanceDao`, `BalanceTransactionDao`, `WithdrawalRequestDao`
- user/profile: `AutoDriveUserDao`
- notifications: `NotificationDao`
- chat: `ConversationDao`, `ChatMessageDao`, `ChatRecoveryCheckpointDao`, `ChatMediaTransferDao`
- dynamic content/competition cache: `DynamoContentDao`, `WeeklyLeaderboardDao`
- durable outbound sync: `PendingOperationDao`
- canonical cursor/inbound ledger: `SyncCursorDao`, `SyncInboxDao`
- bootstrap/reconciliation: `SyncBootstrapDao` plus `SyncReconciliationStateEntity`
- diagnostics: `SyncObservabilityDao`

## Relations inferable from code/schema

- `payments.invoice_id` references invoice identity semantically; no SQLite foreign key is declared.
- `chat_messages.conversation_id` references conversation identity semantically; no SQLite foreign key is declared.
- scoped sync tables use `(user_id, client_id, org_id, ...)` compound identity to prevent account/tenant state mixing.
- Outbox rows carry scope, entity type/id, operation, mutation id, retry/lease state, and optional dependency.

No additional ER relationships are asserted here without code/schema evidence.

## Local source-of-truth policy

For synchronized UI flows, Room is the local read boundary. The remote server remains shared authority for cross-device state, but Realtime payload/delivery is not treated as the sole source of truth. Canonical remote changes are pulled/applied into Room before UI convergence.

## Sync durability/control state

- `pending_operations`: durable scoped outbound intent.
- `sync_cursors`: revision cursor per exact scope/stream.
- `sync_inbox`: durable inbound event identity/apply ledger.
- `sync_bootstrap_state` + `sync_bootstrap_staging`: restartable safe bootstrap.
- `sync_reconciliation_state`: anti-entropy scheduling/result state.
- `sync_observability_state`: diagnostic-only health/run state added at 18→19.
- chat recovery/media tables preserve chat recovery progress and durable media work.

## Deletion semantics

Canonical unified change events distinguish `UPSERT` and `DELETE`; deletion is explicit. Absence from a bounded/paginated/RLS-filtered read is not documented as deletion. Bootstrap stale-row removal is performed against a complete staged snapshot, not a bounded-query absence assumption.

## Schema exports

Current repository exports are under `core/database/schemas/com.autodrive.app.core.database.AutoDriveDatabase/`; versions 13 and 19 are present in v73. Schema export existence does not itself prove every historical Android instrumentation migration test has run.
