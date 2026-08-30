---
status: ACTIVE
scope: project entry point and current documentation gateway
owner: AutoDrive Engineering
last_verified_against: AutoDrive-v75
last_verified_date: 2026-08-22
supersedes: NONE
---

# AutoDrive

AutoDrive is a modular Android application for marketer/workshop workflows backed by Room locally and Supabase remotely. The Session 75 source package preserves v74 production behavior while adding documentation sustainability controls.

## Current architecture

- Android modular monolith: `:app` + 9 `:core:*` modules + 6 `:feature:*` modules = **16 Gradle modules**.
- `:app` is the composition/navigation layer and still hosts Home, Reports, Competition, and Information code not extracted into standalone Gradle feature modules.
- Room database version is **19** with explicit migrations; synchronized UI state reads from Room.
- Canonical inbound synchronization is server-revision based with durable Inbox/cursor, safe bootstrap, and anti-entropy recovery.
- Realtime is a wake-up/acceleration signal, not canonical state authority.
- Exact sync scope is `(userId, clientId, orgId)`.

## Server boundary

The Android client uses Supabase Auth/RPC/Realtime/Storage/Edge Functions. Repository SQL/functions define the intended repository server contract; their presence is not proof of live deployment.

See [Server contract](docs/api/SERVER_CONTRACT.md), [RPC catalog](docs/api/RPC_CATALOG.md), and [Authentication contract](docs/api/AUTH_CONTRACT.md).

## Build, test, and release

Use the current operational documents rather than historical `BUILD_REPORT*` files:

- [Build and test](docs/operations/BUILD_AND_TEST.md)
- [Release](docs/operations/RELEASE.md)
- [Troubleshooting](docs/operations/TROUBLESHOOTING.md)

Session 75 adds a fail-closed, network-independent documentation gate:

```bash
bash scripts/run-documentation-gate.sh
```

A documentation/static PASS is not Android build, instrumentation, or live-server PASS.

## Development governance

- [Contributing](docs/development/CONTRIBUTING.md)
- [Documentation standard](docs/development/DOCUMENTATION_STANDARD.md)
- [KDoc standard](docs/development/KDOC_STANDARD.md)
- [Architecture decisions](docs/architecture/adr/ADR_INDEX.md)

Every future session must make an explicit documentation-impact decision and run/report the drift gate when relevant.

## Documentation

Start with the [Documentation Index](docs/INDEX.md). Current authority is selected only by the [Canonical Document Map](docs/CANONICAL_DOCUMENT_MAP.md).

Historical evidence remains available through the [Archive Index](docs/archive/INDEX.md) and is not rewritten to match current truth.
