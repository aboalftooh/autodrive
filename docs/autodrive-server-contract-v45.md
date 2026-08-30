# AutoDrive Server Contract — v45

Source of truth: `AutoDrive-v44.zip`  
Scope: consolidation of sync/integration behavior introduced or hardened in v41–v44. This document records the Android-side contract; it does not change Supabase.

## Architectural invariant

`Supabase = shared source of truth` → `Realtime = change signal/acceleration` → `targeted or recovery pull` → `Room = UI read source`.

Realtime delivery is never required for convergence. APP_START sync, network-recovery sync, manual refresh, WorkManager fallback and FCM hints remain independent recovery paths.

## Read contract

| Table | Android use | Tenant/owner field | Pull semantics |
|---|---|---|---|
| `autodrive_users` | profile | `user_id` | owner-scoped row |
| `invoices` | commission/billing | `client_id` | tenant-scoped; billing subset uses `category=SALE`, `commission>0` |
| `payments` | billing | `client_id` | tenant-scoped; independent of local Invoice arrival |
| `commission_payments` | commission state | `client_id` | tenant-scoped |
| `marketer_balance` | balance | `client_id` | tenant-scoped single row |
| `balance_transactions` | recent balance history | `client_id` | bounded recent window; absence is not deletion |
| `withdrawal_requests` | withdrawals/reconciliation | `client_id`; command lookup also `client_request_id` | bounded recent window plus idempotency reconciliation |
| `notifications` | notifications | `user_id` | bounded recent window; absence is not deletion |
| `conversations` | chat | `client_id` | tenant-scoped |
| `internal_messages` | admin chat | `client_id` | bounded history window; absence is not deletion |

Other feature reads outside the v41–v44 sync surfaces remain unchanged by v45.

## Write contract

| Target | Operation | Boundary |
|---|---|---|
| `notifications` | mark `is_read=true` | authenticated owner update, `id + user_id` scoped |
| `autodrive_users` | profile update | authenticated owner update, `user_id` scoped; Outbox-supported |
| `internal_messages` | marketer chat message insert | authenticated application write; existing feature contract unchanged |
| `push_tokens` | upsert/delete device token | authenticated `user_id` ownership |
| withdrawal | **RPC only** via `request_withdrawal` | no privileged direct financial table write |

No service-role key or privileged bypass is part of the Android contract.

## RPC contract

| RPC | Purpose | Idempotency / ownership |
|---|---|---|
| `request_withdrawal` | submit withdrawal | `client_request_id` is the stable command identity across every retry |
| `cancel_pending_withdrawals` | cancel pending withdrawals | server must enforce authenticated ownership |
| `touch_last_seen` | presence | authenticated session |
| `get_current_week_number` | competition read | read contract |
| `get_my_competition_history` | competition read | authenticated caller |
| `get_weekly_competition` | competition read | authenticated caller |
| `get_my_win_weeks` | competition read | authenticated caller |

### Withdrawal state boundary

`command submitted` ≠ `command confirmed remotely` ≠ `read refresh`.

A timeout/ambiguous response is not proof that `request_withdrawal` failed. Before retrying, Android reconciles `withdrawal_requests.client_request_id` using the same original key. Dead-letter state preserves the original operation identity and payload.

## Realtime subscriptions

| Stream/table | Subscription scope | Android behavior | Fallback |
|---|---|---|---|
| billing / `invoices` | `client_id = session.clientId` | signal → targeted invoice pull → Room | broader sync on targeted failure + normal convergence paths |
| billing / `payments` | `client_id = session.clientId` | signal → targeted payment/invoice pull → Room | broader sync on targeted failure + normal convergence paths |
| billing / `commission_payments` | `client_id = session.clientId` | tenant-validated local reconciliation | normal sync |
| balance / `marketer_balance` | `client_id = session.clientId` | tenant-validated reconciliation | normal sync |
| balance / `balance_transactions` | `client_id = session.clientId` | tenant-validated reconciliation | normal sync |
| balance / `withdrawal_requests` | `client_id = session.clientId` | tenant-validated reconciliation | normal sync/outbox reconciliation |
| chat / `internal_messages` | `client_id = session.clientId` | tenant-validated chat acceleration | normal sync/feature refresh |
| notifications / `notifications` | `user_id = session.userId` | owner-validated notification acceleration | normal sync/FCM hint |

Optional/unpublished Realtime streams must not block application sync or other participants. Participant failures are isolated. Billing event bursts are coalesced before targeted refreshes.

## Billing ordering contract

Payment ownership is determined by `payments.client_id`, not by whether its Invoice already exists in Room. Realtime financial payloads are not authoritative financial state. The preferred path is:

`event → tenant validation → targeted PostgREST pull → Room upsert`

If targeted reconciliation fails, Android requests a broader sync. Repeated events are deduplicated/coalesced and Room upserts remain idempotent by row identity.

## Deletion semantics

Android distinguishes:

- remote upsert
- unchanged
- explicit deletion/tombstone
- absent/unknown

Absence from a bounded/paginated/RLS-filtered query is **never** a deletion signal. The v43 `DeletionFeed`/`DeletionBatch` extension point expects a future server deletion contract. Checkpoints may advance only after successful application of the corresponding batch.

### Required server contract — `SERVER_PLAN`

Server must provide one authoritative deletion mechanism for synchronized entities: `deleted_at`, `sync_tombstones`, or a changelog/revision feed. It must expose stable row identity, tenant/owner identity, deletion/revision ordering, and a checkpoint/cursor that can safely resume after partial failure.

## Remaining integration ownership

### SERVER_PLAN
- Publish the required Realtime tables/streams; Android must still work when a stream is absent.
- Enforce RLS/tenant binding for every table and RPC.
- Preserve/validate `payments.client_id` and its server constraints/indexing as planned.
- Enforce withdrawal/payout idempotency for `client_request_id`.
- Define payment idempotency contract.
- Keep commission single-credit semantics server-owned.
- Harden RPC grants/guards.
- Provide tombstone/deletion/changelog semantics.
- Resolve server migration/index/default/FK drift identified by the server plan.

### VERTO_PLAN
- Any Verto-side producer changes required to emit/maintain the shared invoice/payment/commission contract.
- Any Verto integration behavior needed for the shared Supabase records; no Verto code is changed here.

### DEFERRED_TEST
- Live Supabase publication verification.
- Runtime Realtime reconnect/failure behavior.
- RLS/tenant-isolation tests.
- E2E Invoice-before-Payment and Payment-before-Invoice ordering tests.
- Timeout-after-commit withdrawal/idempotency test.
- Offline recovery and deletion-feed convergence tests once the server deletion contract exists.
- WorkManager/network-recovery/FCM convergence runtime tests.

## Weekly competition feature gate — v49

`public.autodrive_feature_flags` is the server-controlled rollout source for Android feature availability. The `weekly_competition` row is seeded as `DISABLED`. Its only valid states are `DISABLED`, `LOCKED`, and `ACTIVE`.

Android access is read-only for both `anon` and `authenticated`: the migration revokes table privileges and grants `SELECT` only, with a select-only RLS policy. Android has no insert/update/delete policy or grant for this table. Administrative rollout is performed server-side; changing the row state does not require a new APK.

Android observes the existing `DataStore<Preferences>` cache. Missing cache, a missing server row, an unknown/invalid state, or parse failure resolves safely to `DISABLED`. A network failure never clears or replaces a valid cached `LOCKED` or `ACTIVE` state. The cached `updated_at` is copied only from a real server timestamp; Android does not invent one.

The feature flag controls availability/rollout only. It has no role in winner selection, rank calculation, competition eligibility, commission calculation, or any financial rule. Existing competition RPC names and contracts are unchanged.

## v45 conclusion

Room remains the UI source. Realtime remains optional acceleration. Outbox retains financial command identity across retry/reconciliation. No Android-side server privilege or publication mutation is introduced.

## Weekly competition rollout closure — v52

The rollout contract is closed for the first implementation phase with exactly three server states:

| Server state | Home | Reports competition | Competition route | Leaderboard RPC |
|---|---|---|---|---|
| `DISABLED` | hidden | hidden | unavailable | no |
| `LOCKED` | teaser: `قريباً` | hidden | locked | no |
| `ACTIVE` | visible | visible | active | yes |

`public.autodrive_feature_flags` remains the rollout source of truth. Android has read-only access and cannot change the row. The application refreshes the flag and consumes the cached state; therefore server transitions `ACTIVE → DISABLED` and `DISABLED → LOCKED → ACTIVE` require no APK change.

Safe default remains `DISABLED` when there is no cache, the server row is missing, parsing fails, or the server state is unknown. Network failure does not overwrite a valid cached state.

The rollout flag controls visibility/availability only. It does not participate in winner selection, rank calculation, tie handling, competition totals, or any financial calculation. Those remain owned by the existing server RPC contracts.
