---
status: ACTIVE
scope: actionable build, test, runtime-configuration, and documentation-gate troubleshooting
owner: AutoDrive Engineering
last_verified_against: AutoDrive-v75
last_verified_date: 2026-08-22
supersedes: NONE
---

# Troubleshooting

## Gradle wrapper or dependency resolution

**Symptom:** wrapper/distribution or dependencies cannot be resolved.  
**Likely scope:** local network/cache/build environment.  
**Safe diagnostics:** `bash ./gradlew --version`; rerun the intended task without converting network failure into source failure.  
**Do not infer:** that source is invalid solely from resolution failure, or that a later cached build rewrites older blocked evidence.  
**Next evidence:** successful wrapper/task execution in an environment with required artifacts.

## Offline cache limitation

**Symptom:** `--offline` build/test cannot locate an artifact.  
**Likely scope:** Gradle cache completeness.  
**Safe diagnostics:** inspect the missing dependency in Gradle output and retry with an approved connected environment.  
**Do not infer:** test PASS from compilation/static checks.  
**Next evidence:** the exact Gradle test/build task completing with exit code 0.

## Missing application/service configuration

**Symptom:** Supabase/auth/service behavior is unavailable or invalid at runtime.  
**Likely scope:** `AUTODRIVE_SUPABASE_URL`, `AUTODRIVE_SUPABASE_ANON_KEY`, `AUTODRIVE_ADMIN_WHATSAPP`, or target service integration.  
**Safe diagnostics:** verify that the target environment supplies non-secret values through its approved mechanism; do not print secrets into reports.  
**Do not infer:** compile success means meaningful live-service runtime success.  
**Next evidence:** authenticated/runtime verification against the intended environment.

## Missing Google services artifact/integration

**Symptom:** Google/Firebase plugin/service configuration fails in the build or runtime environment.  
**Likely scope:** target service configuration; `app/google-services.json` is absent from the Session 75 source package.  
**Safe diagnostics:** inspect the exact Gradle/plugin error and environment-provided service configuration.  
**Do not infer:** the repository contains production Firebase credentials/configuration.  
**Next evidence:** successful target build/service initialization using approved configuration.

## Android tests unavailable

**Symptom:** instrumentation/device-dependent verification cannot run.  
**Likely scope:** emulator/device/SDK environment.  
**Safe diagnostics:** record the unavailable prerequisite and keep instrumentation status `BLOCKED` or `NOT_RUN`.  
**Do not infer:** JVM/static PASS is instrumentation PASS.  
**Next evidence:** the intended `androidTest` execution on a supported device/emulator.

## Server runtime not verified

**Symptom:** repository SQL/RPC/function sources exist but deployment state is unknown.  
**Likely scope:** live Supabase/server environment.  
**Safe diagnostics:** compare deployed functions/migrations/RLS/RPC behavior using approved server tooling.  
**Do not infer:** repository source equals deployed state.  
**Next evidence:** direct live-server verification report.

## Documentation drift gate fails

**Symptom:** `bash scripts/run-documentation-gate.sh` exits non-zero with `D1`–`D11`.  
**Likely scope:** code/documentation authority drift.  
**Safe diagnostics:** run `python3 tools/documentation/documentation_drift.py --root . --json` and fix the named concern at its canonical owner.  
**Do not infer:** a warning-only bypass is acceptable; the gate is fail-closed.  
**Next evidence:** base gate PASS plus negative mutation suite `9/9`.
