# AutoDrive v42 — Static Verification

## Source of truth
- `AutoDrive-v41.zip`
- Session scope: v42 from `AutoDrive_PreLaunch_Sync_Integration_Plan_v41-v45.md`

## Implemented
- Added `BillingTargetedRefresher` for authoritative tenant-scoped PostgREST reads.
- Invoice realtime INSERT/UPDATE/DELETE now signals a targeted invoice refresh instead of applying payload financial state directly.
- Payment realtime INSERT/UPDATE/DELETE now signals a targeted payment refresh using `payments.client_id` ownership.
- Payment arrival no longer depends on the invoice already existing in Room.
- Targeted payment refresh attempts invoice reconciliation first; a temporarily missing invoice does not permanently discard a tenant-owned payment.
- Targeted refresh failure falls back to coordinator full sync via `REALTIME_HINT`.
- Added short per-record burst coalescing to avoid duplicate realtime refresh storms.
- Full sync PAYMENTS is now tenant-scoped directly by `client_id`, independent of invoice phase completion/order.
- Duplicate remote payment rows are deduplicated by id before Room upsert.
- No financial calculation or commission semantics changed.

## Static verification
- Realtime billing subscriptions remain scoped by `client_id`.
- Billing realtime payloads are identifiers/signals for invoice/payment state; authoritative state is pulled from PostgREST before Room mutation.
- Room remains the UI data source.
- Existing APP_START, NETWORK_RESTORED, USER_REFRESH, WorkManager and FCM paths were not removed.
- No Supabase migration, RLS, publication, RPC, Edge Function, or server-side file changed.
- No service-role/admin secret added.

## Deferred / server dependencies
- Server-side payment/RPC idempotency and commission single-credit remain SERVER_PLAN items for v44/server plan.
- Deletion convergence/tombstone semantics remain v43/server-plan scope.
- Runtime, RLS and E2E tests remain DEFERRED_TEST per plan.

## Files changed
- `core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/BillingTargetedRefresher.kt` (new)
- `core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/SyncManager.kt`
- `core/sync/src/main/kotlin/com/autodrive/app/core/sync/domain/SyncCoordinator.kt`
- `feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/data/realtime/BillingRealtimeParticipant.kt`
- `verification-v42.md`

## Result
Static v42 scope complete. `AutoDrive-v42.zip` is the sole source of truth for v43.
