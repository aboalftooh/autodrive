---
status: ACTIVE
scope: durable mutation delivery and inbound replay/idempotency strategy
owner: AutoDrive Engineering
last_verified_against: AutoDrive-v75
last_verified_date: 2026-08-22
supersedes: NONE
---

# ADR-0003 — Durable Scoped Outbox/Inbox and Idempotent Commands

## Context

Mobile writes can encounter process death, loss of connectivity, and ambiguous transport outcomes. Inbound events can also be retried. Assuming a timeout means a mutation failed can duplicate effects; applying the same inbound event repeatedly can corrupt local state.

## Decision

Supported outbound mutations use durable exact-scope Outbox state and stable mutation identity. Idempotent command RPCs return receipts; ambiguous transport errors preserve retry/replay instead of inferring failure. Canonical inbound events use a durable Inbox identity/apply ledger and are committed with cursor progression according to the synchronization contract.

## Alternatives considered

- Fire-and-forget remote mutations.
- Generate a new mutation identity on every retry.
- Apply inbound events without durable replay identity.

These options lose retry safety or permit duplicate effects.

## Consequences

Mutation identity and tenant/session scope are correctness data. Retry code must preserve stable identity where the Outbox contract requires it. Inbox identity conflicts fail rather than silently merging different server events under one event id.

## Status

ACTIVE

## Supersedes

NONE

## Superseded by

NONE

## Verified baseline

AutoDrive-v75; unchanged production behavior from v74, including `IdempotentServerCommandGateway`, Outbox, and unified Inbox/change-feed paths.
