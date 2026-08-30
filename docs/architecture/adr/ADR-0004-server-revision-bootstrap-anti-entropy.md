---
status: ACTIVE
scope: canonical remote convergence and recovery protocol
owner: AutoDrive Engineering
last_verified_against: AutoDrive-v75
last_verified_date: 2026-08-22
supersedes: NONE
---

# ADR-0004 — Server Revision Change Feed with Bootstrap and Anti-Entropy

## Context

Device clocks and bounded reads are unsafe global ordering/deletion authorities. A client also needs a recovery path when no usable cursor exists or retained change history is no longer available.

## Decision

Canonical inbound synchronization uses a server-revision ordered change feed with an exact-scope durable cursor. Missing/incompatible/expired cursor state uses a staged safe bootstrap at a server baseline revision, followed by delta resume. Periodic/forced anti-entropy compares canonical digests at a known revision, performs targeted repair where safe, and can demand rebootstrap when consistency cannot be proven.

## Alternatives considered

- Device-time `updated_at` cursors.
- Treat absence from bounded reads as deletion.
- Full replacement sync on every run without durable cursor/recovery state.

These alternatives risk missed changes, false deletions, or unnecessary destructive work.

## Consequences

Protocol version, revision monotonicity, transaction-group boundaries, exact scope, bootstrap identity, and reconciliation revision are correctness invariants. Repository SQL represents the intended server contract; live deployment still requires separate verification.

## Status

ACTIVE

## Supersedes

NONE

## Superseded by

NONE

## Verified baseline

AutoDrive-v75; unchanged v74 production implementation in unified change feed, safe bootstrap, and anti-entropy reconciliation.
