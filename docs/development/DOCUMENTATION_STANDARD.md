---
status: ACTIVE
scope: documentation lifecycle, authority, metadata, and drift-prevention rules
owner: AutoDrive Engineering
last_verified_against: AutoDrive-v75
last_verified_date: 2026-08-22
supersedes: NONE
---

# Documentation Standard

## Authority and lifecycle

Every maintained document has one lifecycle state:

- `DRAFT`: incomplete work; never current authority.
- `ACTIVE`: current authority for its registered concern.
- `SUPERSEDED`: replaced by a newer authority; preserved for traceability.
- `ARCHIVED`: historical evidence or reference material; not current authority.

The authority selector is [`docs/CANONICAL_DOCUMENT_MAP.md`](../CANONICAL_DOCUMENT_MAP.md). One concern has one `ACTIVE` owner. Historical names such as `*_CURRENT.md`, `active-*`, and `target-*` have no authority unless the canonical registry explicitly assigns it.

When prose conflicts with executable repository truth, prefer production code/schema/Gradle/current repository server source, then test-enforced invariants, then current canonical documentation, then historical evidence. Repository server source proves intended repository contracts, not live deployment.

## Required metadata

Every canonical `ACTIVE` document must begin with YAML metadata containing:

```yaml
status: ACTIVE
scope: <specific concern>
owner: AutoDrive Engineering
last_verified_against: <verified AutoDrive baseline>
last_verified_date: YYYY-MM-DD
supersedes: <path/list or NONE>
```

Do not invent personal owners. `last_verified_against` is evidence, not a release label: raise it only after the relevant verification passes.

## Event-driven update triggers

Documentation freshness is event-driven. Any change to the following requires an explicit documentation-impact decision and re-verification of the affected canonical documents:

- Gradle module graph or project dependency direction;
- Room version, schema/entity inventory, or migration chain;
- synchronization protocol, cursor, Outbox/Inbox, bootstrap, reconciliation, or Realtime authority;
- RPC/Edge Function identifiers or server contract;
- authentication/session ownership and tenant/scope rules;
- cross-module public contracts;
- build, test, release, or service-integration workflow;
- architectural invariants that would change an ADR decision.

A calendar-only review interval is not a substitute for this trigger.

## Documentation impact contract

Every new `SESSION_*_FINAL.md` must state exactly:

```text
Documentation impact:
- NONE
```

or:

```text
Documentation impact:
- REQUIRED

Canonical docs affected:
- <paths>

Drift check:
- PASS | BLOCKED | NOT_RUN
```

If documentation impact is `REQUIRED`, update the affected canonical docs in the same session by default. A deferral must record the reason, affected docs, risk, follow-up owner/next session, and drift-check state; the session must not claim completion without making that deferral explicit.

## Historical evidence immutability

Execution contracts, old verification reports, build/fix reports, design-system reports, prior sync verification, and `docs/refactor` authority-like material are historical evidence. Future work may classify, index, or supersede their authority, but must not rewrite old PASS/BLOCKED claims to fit current truth.

## Verification gate

Run from repository root:

```bash
bash scripts/run-documentation-gate.sh
```

Mandatory drift checks are fail-closed. A warning followed by exit code `0` is not an acceptable failure mode.

## Canonical update procedure

1. Change executable truth first when the product/session requires it.
2. Decide `Documentation impact`.
3. Update only the canonical owner(s) for affected concerns.
4. Add an ADR only when the architectural decision rule applies.
5. Add KDoc only when the contract rule applies.
6. Run the documentation gate.
7. Record build/test/runtime evidence separately; never promote `NOT_RUN`, `BLOCKED`, or static verification to runtime `PASS`.
