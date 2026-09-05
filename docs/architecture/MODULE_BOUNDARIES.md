---
status: ACTIVE
scope: current Gradle module topology and dependency boundaries
owner: AutoDrive Engineering
last_verified_against: AutoDrive-v73
last_verified_date: 2026-08-22
supersedes: module-graph-v13.md; module-graph-v14.md; dependency-rule copies
---

# Module Boundaries

Derived from `settings.gradle.kts` and each module `build.gradle.kts` in AutoDrive-v73.

## Modules: 16 exactly

| Module | Responsibility | Direct project dependencies declared in Gradle |
|---|---|---|
| `:app` | Application composition, navigation, app-level DI; also hosts Home/Reports/Competition/Information code. | `:core:common`, `:core:database`, `:core:designsystem`, `:core:model`, `:core:network`, `:core:observability`, `:core:platform`, `:core:session`, `:core:sync`, `:feature:auth`, `:feature:balance`, `:feature:chat`, `:feature:commission`, `:feature:notifications`, `:feature:profile` |
| `:core:model` | Cross-module account and money models. | None |
| `:core:common` | Small cross-cutting contracts/utilities, including sign-out and registration ports. | `:core:model` |
| `:core:database` | Room database, entities, DAOs, converters, explicit migrations. | None |
| `:core:network` | Supabase client and shared network DTO/serialization. | None |
| `:core:observability` | Logging, redaction, diagnostic reporting/Crashlytics integration. | None |
| `:core:session` | Encrypted local session/preferences contracts and implementation. | `:core:model` |
| `:core:sync` | Sync coordinator/engine, Outbox/Inbox, change feed, bootstrap, reconciliation, realtime coordination. | `:core:database`, `:core:network`, `:core:observability`, `:core:session` |
| `:core:designsystem` | Design tokens, reusable components, patterns, application theme. | None |
| `:core:platform` | Android/platform integrations: notifications/push tokens, PDF export, sharing. | `:core:common`, `:core:designsystem`, `:core:model`, `:core:network`, `:core:observability`, `:core:session` |
| `:feature:auth` | Phone OTP, invite verification, session restore/sign-out and auth UI. | `:core:common`, `:core:designsystem`, `:core:model`, `:core:network`, `:core:platform`, `:core:session`, `:core:sync` |
| `:feature:chat` | Conversation/message UI, local chat repository, realtime hint participant, media transfer. | `:core:common`, `:core:database`, `:core:designsystem`, `:core:network`, `:core:observability`, `:core:platform`, `:core:session`, `:core:sync` |
| `:feature:notifications` | Notification UI/repository, unread observer, realtime participant. | `:core:common`, `:core:database`, `:core:designsystem`, `:core:network`, `:core:observability`, `:core:session`, `:core:sync`, `:feature:chat` |
| `:feature:commission` | Commission/invoice domain, repository, reports and billing realtime participant. | `:core:common`, `:core:database`, `:core:designsystem`, `:core:model`, `:core:network`, `:core:observability`, `:core:platform`, `:core:session`, `:core:sync` |
| `:feature:balance` | Balance/withdrawal domain, repository, UI and realtime participant. | `:core:common`, `:core:database`, `:core:designsystem`, `:core:model`, `:core:network`, `:core:observability`, `:core:session`, `:core:sync` |
| `:feature:profile` | Profile observation/update and registration profile completion. | `:core:common`, `:core:database`, `:core:designsystem`, `:core:model`, `:core:network`, `:core:platform`, `:core:session`, `:core:sync`, `:feature:balance` |

## Current dependency shape

- `:app` aggregates every `core:*` and `feature:*` Gradle module and owns application navigation/composition.
- No `core:*` module declares a dependency on `:feature:*` or `:app` in the inspected build files.
- `:core:sync` directly depends on database, network, session, and observability.
- `:core:platform` exposes model/network/observability/session and uses common/designsystem.
- Cross-feature dependencies exist and are therefore **descriptive current facts**, not forbidden assumptions: `:feature:notifications → :feature:chat` and `:feature:profile → :feature:balance`.
- Several feature modules expose core dependencies with Gradle `api`; that is a current build choice, not proof that every dependency is architecturally ideal.

## Enforced versus descriptive boundaries

**Enforced by Gradle:** a module can reference only dependencies made available by its build configuration; the 16-module list is fixed by `settings.gradle.kts` for this baseline.

**Descriptive/invariant expectations:** `:app` as composition root, core not depending on feature/app, and feature ownership boundaries are architecture expectations corroborated by the current graph and historical verifiers. Session 74 does not add a new enforcement task.

## Change rule

Any module addition/removal or project-dependency direction change must update `settings.gradle.kts`/module build files first, then this document. Documentation never drives a Gradle mutation in Session 74.
