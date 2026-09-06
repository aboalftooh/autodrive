# AutoDrive v41 — Static Verification

## Source of truth
- `autodrive-v40.zip`
- Plan: `AutoDrive_PreLaunch_Sync_Integration_Plan_v41-v45.md`
- Scope executed: **v41 only**

## Implemented
1. `payments` Realtime subscription is now tenant-scoped with `client_id = session.clientId`.
2. `PaymentDto` now models the server `client_id` field.
3. Payment Realtime ownership checks use the server payload `client_id` instead of requiring a locally-present Invoice first.
4. `RealtimeManager` now supervises participants independently. A failed/unpublished optional Realtime participant no longer cancels healthy participants.
5. Failed participants retry independently with bounded exponential backoff.
6. Realtime remains non-critical to convergence: the existing APP_START, network recovery, user refresh, WorkManager, and FCM sync paths were preserved.
7. No server publication/RLS/Function/migration change was made.

## Burst behavior
- Realtime participants do not trigger broad `requestSync()` calls per event, so event bursts cannot fan out into unbounded full refreshes through this layer.
- Existing coordinator sync remains single-flight.

## Static evidence
- `DefaultSyncCoordinator.kt`: APP_START and NETWORK_RESTORED preserved.
- Home/balance/profile refresh paths still issue USER_REFRESH.
- `AutoDriveFirebaseMessagingService.kt`: FCM_HINT preserved.
- `PendingOperationsWorker.kt`: periodic WorkManager fallback preserved.
- `BillingRealtimeParticipant.kt`: invoices, commission_payments, and payments all use `client_id` subscription filters.

## Modified files
- `core/network/src/main/kotlin/com/autodrive/app/core/network/dto/Dtos.kt`
- `core/sync/src/main/kotlin/com/autodrive/app/core/sync/realtime/RealtimeManager.kt`
- `feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/data/realtime/BillingRealtimeParticipant.kt`
- `verification-v41.md`

## Server-dependent blockers / follow-up
- Publication membership remains a server responsibility.
- If a table is not published, its participant can remain unavailable while normal sync convergence continues.
- v42 should harden Billing ordering/races further using targeted pull and reconciliation; this session intentionally does not implement v42.

## Verification mode
Static inspection only. Runtime, RLS and E2E tests were intentionally not executed per plan.
