---
status: ACTIVE
scope: reproducible local build and verification commands with evidence semantics
owner: AutoDrive Engineering
last_verified_against: AutoDrive-v75
last_verified_date: 2026-08-22
supersedes: BUILD_REPORT_CURRENT.md as current operational authority
---

# Build and Test

## Toolchain facts from this repository

| Item | Repository value |
|---|---:|
| Gradle wrapper distribution | 8.7 |
| Android Gradle Plugin | 8.5.2 |
| Kotlin | 2.0.21 |
| Java source/target | 17 |
| Kotlin JVM target | 17 |
| compileSdk | 35 |
| targetSdk | 35 |
| minSdk | 26 |
| Room | 2.6.1 |
| Supabase BOM | 3.0.2 |

The wrapper JAR is present. ZIP extraction may not preserve the executable bit on `gradlew`; therefore these commands intentionally invoke it through `bash`.

## Build commands

From repository root:

```bash
bash ./gradlew assembleDebug
bash ./gradlew assembleRelease
bash ./gradlew test
```

A command shown here is not evidence that it passed on a particular machine/session. Record its actual exit/result separately.

## Configuration boundaries

Repository package facts for Session 75:

- `gradle/wrapper/gradle-wrapper.jar`: present.
- `app/google-services.json`: absent from the packaged source baseline.
- `local.properties`: absent from the packaged source baseline.
- application configuration keys read by `app/build.gradle.kts`: `AUTODRIVE_SUPABASE_URL`, `AUTODRIVE_SUPABASE_ANON_KEY`, `AUTODRIVE_ADMIN_WHATSAPP`.
- the build script also accepts the legacy local-property aliases `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and `ADMIN_WHATSAPP`.

`app/build.gradle.kts` supplies empty-string fallbacks for these three values, so their absence is not documented as an unconditional compile failure. Meaningful authenticated/runtime service behavior requires valid Supabase configuration. Google/Firebase service integration may require the service artifact/configuration expected by the applied plugins/environment; this repository package alone does not prove that integration is configured.

## Verification taxonomy

Treat these as distinct evidence classes:

1. **Static verification** — repository parsers/invariant checks such as the documentation gate.
2. **JVM/unit tests** — Gradle `test` tasks for local JVM tests.
3. **Robolectric tests** — local Android-behavior tests where test sources use Robolectric.
4. **Android instrumentation** — device/emulator `androidTest` execution.
5. **Migration/runtime verification** — Room migration/runtime behavior under an Android-capable environment.
6. **Live server verification** — direct validation against the deployed Supabase/server environment.

One class cannot be promoted into another.

## Status semantics

- `PASS`: the named check ran and passed.
- `BLOCKED`: attempted, but an environment/dependency prerequisite prevented completion.
- `NOT_RUN`: not executed.
- `NOT_APPLICABLE`: irrelevant to the scoped change.
- `UNVERIFIED`: state cannot be proven from available evidence.

Forbidden inference examples: `BLOCKED → PASS`, `NOT_RUN → PASS`, and `static PASS → runtime PASS`.

## Documentation gate

Run:

```bash
bash scripts/run-documentation-gate.sh
```

This gate is local, deterministic, network-independent, and suitable as the repository-level CI entry point. Hosted CI is not claimed by this document.
