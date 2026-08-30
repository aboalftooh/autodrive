---
status: ACTIVE
scope: local/UI state authority for synchronized domains
owner: AutoDrive Engineering
last_verified_against: AutoDrive-v75
last_verified_date: 2026-08-22
supersedes: NONE
---

# ADR-0001 — Room as Local UI Source of Truth

## Context

AutoDrive supports offline-readable synchronized state, durable local control records, and multiple remote update mechanisms. UI correctness cannot depend on the timing or delivery reliability of a network subscription.

## Decision

For synchronized domains, Room is the durable local/UI read boundary. Shared cross-device authority is remote, but remote changes converge through synchronization into Room before they become canonical local UI state. Durable sync control state such as cursors, Inbox, bootstrap, reconciliation, and supported Outbox intent also persists locally.

## Alternatives considered

- Read directly from remote APIs/Realtime for UI state.
- Keep transient in-memory state as the primary UI authority.

Both weaken offline behavior and make correctness depend on network/event timing.

## Consequences

Repositories and sync code must preserve Room consistency and exact account/tenant scope. Remote freshness may lag while offline, but UI state remains durable. Any future move away from Room authority requires a superseding ADR and migration strategy.

## Status

ACTIVE

## Supersedes

NONE

## Superseded by

NONE

## Verified baseline

AutoDrive-v75; behavior inherited unchanged from the v74 production source and canonical database/sync documentation.
