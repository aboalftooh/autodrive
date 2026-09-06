# AutoDrive v44 — Static Verification

Source of truth: `AutoDrive-v43.zip`
Scope: v44 Outbox, idempotency, and financial command boundaries only.

## Implemented / verified
- Withdrawal creates one `clientRequestId` and reuses it as the Outbox `idempotencyKey` and RPC `p_client_request_id`.
- Retry does not generate a new financial command identity.
- `OutboxSynchronizer` reconciles `withdrawal_requests` by `client_request_id` before retrying an ambiguous RPC result.
- Ambiguous network/timeout/unknown/5xx-style outcomes now preserve the local command for reconciliation instead of deleting it as a final failure.
- Known business/auth rejection codes remain definitive failures and do not create duplicate retries.
- Dead-letter processing preserves the original PendingOperation row, payload, and idempotency key; only successful operations are deleted.
- `WithdrawalRequestDto` now explicitly models `client_request_id` for the server contract.
- Financial submission remains through `request_withdrawal` RPC; no privileged direct financial table write was introduced.
- Read refresh/Room state remains separate from command submission/confirmation.

## Server dependencies
- SERVER_PLAN: server must enforce payout/withdrawal idempotency for `p_client_request_id` / `client_request_id`.
- SERVER_PLAN: commission single-credit semantics remain server-owned.
- SERVER_PLAN: payment idempotency contract and RPC grants/guards remain server-owned.

## Deferred
- DEFERRED_TEST: runtime, RLS, E2E, and live Supabase verification were not run per plan.

## Files changed in v44
- `feature/balance/src/main/kotlin/com/autodrive/app/feature/balance/data/BalanceRepositoryImpl.kt`
- `core/network/src/main/kotlin/com/autodrive/app/core/network/dto/Dtos.kt`
- `verification-v44.md`

No Supabase migration, RLS, publication, function, or service-role change was made.
