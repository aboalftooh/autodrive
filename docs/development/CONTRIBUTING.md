---
status: ACTIVE
scope: repository contribution, verification evidence, and documentation-impact rules
owner: AutoDrive Engineering
last_verified_against: AutoDrive-v75
last_verified_date: 2026-08-22
supersedes: NONE
---

# Contributing

## Source of truth and scope

Start from the declared source-of-truth package/session. Do not silently merge historical evidence into current runtime truth. Keep the requested session scope: documentation-only sessions must not mutate production behavior, SQL, Gradle topology, or historical evidence unless explicitly authorized.

## Documentation impact

Every future session must include:

```text
Documentation impact: NONE | REQUIRED
Canonical docs affected: <paths> | NONE
Drift check: PASS | BLOCKED | NOT_RUN
```

Use [`DOCUMENTATION_STANDARD.md`](DOCUMENTATION_STANDARD.md) to decide impact. If impact is required, update the canonical owner in the same session by default.

## Evidence rules

Report verification states literally:

- `PASS`: the named check actually ran and passed.
- `BLOCKED`: execution was attempted but an external/environment prerequisite prevented completion.
- `NOT_RUN`: the check was not executed.
- `NOT_APPLICABLE`: the check does not apply to the scoped change.
- `UNVERIFIED`: a claimed external/runtime state lacks direct evidence.

Never convert static PASS into Android runtime, instrumentation, or live-server PASS.

## Required documentation gate

Before completing a change that can affect canonical documentation, run:

```bash
bash scripts/run-documentation-gate.sh
```

The gate checks Room/documentation version alignment, Gradle modules, production server-operation coverage, canonical registry integrity, local links, stale authority names, metadata, ADR integrity, critical KDoc targets, operations-document navigation, and the session documentation-impact policy. It also runs negative mutation tests proving fail-closed behavior.

## When an ADR is required

Create or supersede an ADR when changing a durable architectural invariant with cross-module or data-correctness impact, such as local source-of-truth ownership, Realtime authority, durable mutation/retry strategy, or canonical synchronization/recovery strategy. Do not create ADRs for routine implementation details or to copy old session reports.

See [`ADR_INDEX.md`](../architecture/adr/ADR_INDEX.md).

## When KDoc is required

Add KDoc when misuse can violate data correctness, convergence, tenant/session ownership, retry/idempotency, concurrency, or cross-module contract expectations. Do not perform broad comment campaigns.

See [`KDOC_STANDARD.md`](KDOC_STANDARD.md).

## Archive immutability

Historical reports and execution evidence are immutable by default. Index or supersede their authority; do not rewrite their old claims.

## Session report expectations

A completed implementation report should identify input/source hash, changed/added/removed files, the documentation-impact decision, gate result, build/test/runtime states, and any blockers or deferred follow-up. Claims must be tied to executed evidence.
