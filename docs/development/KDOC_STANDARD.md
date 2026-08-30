---
status: ACTIVE
scope: KDoc policy for high-risk cross-module and correctness-critical contracts
owner: AutoDrive Engineering
last_verified_against: AutoDrive-v75
last_verified_date: 2026-08-22
supersedes: NONE
---

# KDoc Standard

KDoc exists to protect contracts whose misuse can corrupt data, break synchronization convergence, violate tenant/session ownership, duplicate non-idempotent effects, lose retry safety, violate concurrency expectations, or misuse a cross-module interface.

## What useful KDoc should state

Where applicable, document:

- purpose and authority/source-of-truth;
- invariants and scope/tenant assumptions;
- preconditions and postconditions;
- threading/concurrency or serialization expectations;
- retry/idempotency semantics;
- failure and cancellation semantics.

KDoc must describe implemented behavior. If accurate documentation would require changing runtime code, do not change code in a documentation-only session; record the discrepancy instead.

## What not to document

Do not chase percentage coverage. Avoid KDoc on trivial getters/setters, obvious private helpers, simple Compose wrappers, self-explanatory DTO fields, or comments that repeat names/types.

## Session 75 critical target policy

The deterministic target manifest is [`tools/documentation/critical_kdoc_targets.json`](../../tools/documentation/critical_kdoc_targets.json). The documentation gate verifies those selected symbols directly. The goal is useful KDoc on every selected high-risk contract, not 100% repository KDoc coverage.

Changes to the manifest require the same review as changes to a correctness-critical public/cross-module contract.
