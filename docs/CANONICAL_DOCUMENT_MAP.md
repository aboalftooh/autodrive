---
status: ACTIVE
scope: registry of documentation authority
owner: AutoDrive Engineering
last_verified_against: AutoDrive-v75
last_verified_date: 2026-08-22
supersedes: NONE
---

# Canonical Document Map

This registry is the authority selector. `ACTIVE` means current documentation authority. Exactly one path owns each concern.

| Concern | Path | Status | Authority | Verified against | Supersedes | Notes |
|---|---|---|---|---|---|---|
| Project entry point | `README.md` | ACTIVE | Current gateway | AutoDrive-v75 | NONE | Navigation and concise current truth |
| Documentation navigation | `docs/INDEX.md` | ACTIVE | Current navigation | AutoDrive-v75 | NONE | Task-oriented entry path |
| System architecture | `docs/architecture/SYSTEM_ARCHITECTURE.md` | ACTIVE | Production code + DI + Gradle | AutoDrive-v73 | `docs/refactor/active-architecture-v14.md`; `target-architecture-v14.md` | Production behavior unchanged through v75 |
| Module boundaries | `docs/architecture/MODULE_BOUNDARIES.md` | ACTIVE | `settings.gradle.kts` + module build files | AutoDrive-v73 | `module-graph-v13.md`; `module-graph-v14.md`; dependency-rule copies | Exactly 16 modules; drift checked in v75 |
| Database | `docs/data/DATABASE.md` | ACTIVE | `AutoDriveDatabase.kt` + schema 19 | AutoDrive-v73 | Old Room/database descriptions | Room 19; drift checked in v75 |
| Migrations | `docs/data/MIGRATIONS.md` | ACTIVE | `AutoDriveDatabase.ALL_MIGRATIONS` | AutoDrive-v73 | `docs/refactor/database-migrations.md` | Explicit chain 4→19; production unchanged |
| Synchronization | `docs/data/SYNC_ARCHITECTURE.md` | ACTIVE | `core:sync` + current repository SQL | AutoDrive-v73 | Earlier sync descriptions | Production behavior unchanged through v75 |
| Server/API contract | `docs/api/SERVER_CONTRACT.md` | ACTIVE | Production callers + repository SQL/functions | AutoDrive-v73 | `docs/autodrive-server-contract-v45.md` | Repository contract ≠ deployed-state proof |
| RPC inventory | `docs/api/RPC_CATALOG.md` | ACTIVE | Production `.rpc`/Edge Function callers + dynamic mappings | AutoDrive-v73 | NONE | 28 production operation identifiers; drift checked in v75 |
| Authentication | `docs/api/AUTH_CONTRACT.md` | ACTIVE | auth/session/profile/push production code + OTP functions | AutoDrive-v73 | Old distributed auth descriptions | Production behavior unchanged through v75 |
| Build/test operations | `docs/operations/BUILD_AND_TEST.md` | ACTIVE | Gradle/config/test sources + executed evidence | AutoDrive-v75 | `BUILD_REPORT_CURRENT.md` as authority | Current operational contract |
| Release | `docs/operations/RELEASE.md` | ACTIVE | app Gradle/config + executed release evidence | AutoDrive-v75 | NONE | Does not invent signing/deployment proof |
| Contribution rules | `docs/development/CONTRIBUTING.md` | ACTIVE | Repository governance contract | AutoDrive-v75 | NONE | Includes future session documentation impact |
| Documentation rules | `docs/development/DOCUMENTATION_STANDARD.md` | ACTIVE | Repository documentation governance contract | AutoDrive-v75 | NONE | Event-driven maintenance + metadata |
| KDoc rules | `docs/development/KDOC_STANDARD.md` | ACTIVE | Critical-contract documentation policy | AutoDrive-v75 | NONE | Targeted, not percentage-based |
| Architecture decisions | `docs/architecture/adr/ADR_INDEX.md` | ACTIVE | ADR registry + current implementation | AutoDrive-v75 | NONE | Four initial durable decisions |
| Troubleshooting | `docs/operations/TROUBLESHOOTING.md` | ACTIVE | Actionable current operational diagnostics | AutoDrive-v75 | Historical failure logs as authority | No historical log rewriting |

## Authority rule

When documents conflict: production code/schema/Gradle/current repository SQL → matching test-enforced invariants → current canonical owner → execution/build evidence → older docs/plans.

Historical filenames such as `BUILD_REPORT_CURRENT.md`, `active-*`, and `target-*` do not override this registry.

## Session 75 activation

The four Session 74 reserved concerns are now `ACTIVE`: build/test, release, contribution rules, and documentation rules. KDoc rules, architecture decisions, and troubleshooting were added, yielding **17 active concerns** with no reserved concern remaining.
