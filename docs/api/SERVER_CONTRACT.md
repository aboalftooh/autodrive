---
status: ACTIVE
scope: current Android-to-server repository contract
owner: AutoDrive Engineering
last_verified_against: AutoDrive-v73
last_verified_date: 2026-08-22
supersedes: docs/autodrive-server-contract-v45.md
---

# Server Contract

## Contract boundary

This document describes **v73 Android callers plus server SQL/Edge Function source present in this repository**. It distinguishes intended repository contract from deployed runtime state.

`SERVER_RUNTIME = NOT_RUN` for Session 74. A SQL migration/function existing in the repository does **not** prove it is deployed on a live Supabase project.

## Authentication and tenant authority

- Android constructs `AutoDriveSupabase` with the anon key; service-role credentials are forbidden in the Android client.
- Auth uses Supabase sessions created/imported after phone OTP verification.
- Current v73 canonical sync RPCs intentionally do not serialize Android `SyncScope` as authority; SQL derives exact user/client/org from `auth.uid()`/server-side mapping.
- Command RPC SQL present in v73 revokes public/anon execution and grants authenticated execution for the defined command endpoints.
- Legacy/direct RPCs whose definitions are absent from the repository remain dependent on deployed server/RLS behavior and are marked source-absent/runtime-unverified in the RPC catalog.

## Read paths

Canonical cross-domain convergence uses the unified change feed/bootstrap/reconciliation RPCs. Additional feature/direct PostgREST reads still exist for:

`ai_insights`, `autodrive_feature_flags`, `autodrive_users`, `balance_transactions`, `commission_eligibility`, `commission_payments`, `conversations`, `dynamo_content`, `invoice_items`, `invoices`, `marketer_balance`, `notifications`, `payments`, `withdrawal_requests`.

Some of these are feature reads or legacy/targeted recovery paths; they are not substitutes for the global revision cursor when canonical convergence is required.

## Command/write paths

Durable v73 Outbox commands use server receipt semantics for profile update, withdrawal request, chat send/create/read, and notification read. Push-token register/revoke and cancel-pending-withdrawals also use receipt-style command RPCs directly from their repositories.

Receipt contract includes stable `mutation_id`, command type, result status (`APPLIED`/`REJECTED`/`CONFLICT`), server revision, revision kind `COMMAND_RECEIPT`, replay indicator and optional error/result fields.

Ambiguous transport outcomes are retry-safe only where the same stable mutation id is replayed and the server receipt/idempotency contract is honored.

## Change-feed / bootstrap / reconciliation

Repository SQL `20260822074200_autodrive_unified_change_feed_v1.sql` defines intended authenticated RPCs:

- `autodrive_sync_changes_v1`
- `autodrive_sync_bootstrap_begin_v1`
- `autodrive_sync_bootstrap_page_v1`
- `autodrive_sync_manifest_v1`
- `autodrive_sync_partition_v1`

Android validates protocol versions and exact returned scope. Revisions are server ordered. Cursor expiry is a first-class condition that triggers safe bootstrap.

## Delete semantics

Canonical change events carry explicit operation `DELETE`; absence from bounded/paginated/RLS reads is not deletion. Bootstrap stale-row removal is permitted only after a complete staged snapshot is installed transactionally.

## Realtime

Realtime is an acceleration/hint channel. Participant events may trigger targeted refresh or a canonical sync request; authoritative correctness remains the Room-applied pull/recovery path.

## Error categories

Android distinguishes at least:

- authentication/session unavailable;
- protocol/status/version/scope/revision/group validation failures;
- cursor/bootstrap expiry;
- command `REJECTED`/`CONFLICT` receipts;
- ambiguous transport outcomes requiring stable-id replay;
- stale local/account scope changes;
- Realtime degradation isolated from canonical pull correctness.

Exact server error codes vary by RPC/function and are documented where known in [RPC Catalog](RPC_CATALOG.md).

## Retry safety

- Idempotent command RPCs: retry with the same mutation id after ambiguous delivery.
- Canonical change pages: safe to refetch from the persisted revision; Inbox/event identity + transactional cursor advancement prevent duplicate effect.
- Bootstrap: durable staged state supports restart until expiration; expiration restarts from a new snapshot.
- Read-only RPCs: repeatable at the application level, subject to server-side consistency/authorization.
- OTP send/verify are **not general idempotent commands**; server functions implement rate limits and one-time OTP consumption semantics.

## Runtime-unverified assumptions

Session 74 does not prove:

- repository migrations are deployed to the currently intended project;
- live RLS matches repository expectations for source-absent legacy RPCs/tables;
- all production RPC grants exist live;
- multi-device convergence or cross-account isolation passed live end-to-end tests.

Those remain `UNVERIFIED/NOT_RUN`, not silently promoted from static source evidence.
