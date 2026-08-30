# AutoDrive v43 — Static Verification

## Scope
Implemented only session v43 from `AutoDrive_PreLaunch_Sync_Integration_Plan_v41-v45.md`, starting from `AutoDrive-v42.zip`. No Supabase/RLS/Function/Publication changes and no runtime/RLS/E2E tests.

## Implemented
- Added explicit reconciliation semantics: `Upsert`, `Unchanged`, `ExplicitDeletion`, and `AbsentOrUnknown`.
- Added a deletion-feed extension point for a future server `deleted_at`, tombstone, or changelog contract.
- Added checkpoint semantics that only expose commit-after-successful-apply; current production sync had no cursor/checkpoint to advance prematurely.
- Kept bounded pulls (20/50/100) as recent windows and documented that absence is not authoritative deletion evidence.
- Added ID deduplication before Room application for invoices, payments, commission payments, balance transactions, withdrawals, notifications, conversations, and chat messages.
- No local rows are deleted merely because they are absent from a remote query.
- Existing phase isolation remains: a failed pull phase is recorded without turning absence/partial results into deletion.
- Existing WorkManager/network-recovery convergence paths were left intact.

## Server dependency
`SERVER_PLAN`: server must provide one authoritative deletion contract (`deleted_at`, `sync_tombstones`, or changelog/revision feed). Until then, explicit remote deletions cannot be reconciled reliably after missed DELETE events.

## Deferred
`DEFERRED_TEST`: runtime, RLS, E2E, offline/reconnect and deletion-feed integration tests remain deferred per plan.

## Files changed
- `core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/SyncManager.kt`
- `core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/RemoteSyncSemantics.kt`
- `verification-v43.md`

## Static verdict
PASS for v43 application-side scope. The remaining deletion guarantee is intentionally blocked on the documented server contract.
