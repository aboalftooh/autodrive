---
status: ACTIVE
scope: architecture decision record registry and governance
owner: AutoDrive Engineering
last_verified_against: AutoDrive-v75
last_verified_date: 2026-08-22
supersedes: NONE
---

# Architecture Decision Records

ADRs record durable architectural decisions, not every implementation change or historical session. New ADRs use the next numeric identifier. A superseded decision is preserved and points to its replacement.

| ADR | Status | Decision |
|---|---|---|
| [ADR-0001](ADR-0001-room-local-ui-source-of-truth.md) | ACTIVE | Room is the local/UI source of truth for synchronized state |
| [ADR-0002](ADR-0002-realtime-is-a-hint.md) | ACTIVE | Realtime is acceleration/hinting, not canonical state authority |
| [ADR-0003](ADR-0003-durable-outbox-inbox-idempotency.md) | ACTIVE | Durable scoped Outbox/Inbox plus idempotent command strategy |
| [ADR-0004](ADR-0004-server-revision-bootstrap-anti-entropy.md) | ACTIVE | Server revision change feed with bootstrap and anti-entropy recovery |

## ADR format

Each ADR must contain: Context, Decision, Alternatives considered, Consequences, Status, Supersedes, Superseded by, and Verified baseline.

Create or supersede an ADR when a durable invariant changes across modules/data correctness. Routine refactors, UI details, and historical `SESSION_*` execution evidence do not become ADRs automatically.
