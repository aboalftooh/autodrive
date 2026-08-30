---
status: ACTIVE
scope: realtime authority relative to canonical synchronization
owner: AutoDrive Engineering
last_verified_against: AutoDrive-v75
last_verified_date: 2026-08-22
supersedes: NONE
---

# ADR-0002 — Realtime Is a Hint, Not Canonical State Authority

## Context

Realtime delivery can accelerate freshness but can disconnect, retry, arrive late, or miss periods while the application is offline. Treating it as the only convergence path would make correctness depend on subscription continuity.

## Decision

Realtime acts as a wake-up/acceleration signal. The synchronization coordinator owns authoritative pull/apply convergence through the durable server-revision protocol and Room. Realtime participant failure may degrade freshness but must not become the sole correctness boundary.

## Alternatives considered

- Apply Realtime payloads directly as the only current state.
- Require continuous subscription availability before considering data valid.

Both make convergence fragile under disconnect/offline conditions.

## Consequences

Realtime code may request synchronization/restart participants, while canonical cursor/Inbox/bootstrap/reconciliation rules remain authoritative. Monitoring must distinguish Realtime health from data-convergence correctness.

## Status

ACTIVE

## Supersedes

NONE

## Superseded by

NONE

## Verified baseline

AutoDrive-v75; verified against unchanged `core:sync` production behavior and current canonical sync documentation.
