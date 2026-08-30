---
status: ACTIVE
scope: release prerequisites, evidence, artifact verification, and known boundaries
owner: AutoDrive Engineering
last_verified_against: AutoDrive-v75
last_verified_date: 2026-08-22
supersedes: NONE
---

# Release

## Preconditions

Before calling an artifact release-ready, establish:

1. the intended source-of-truth baseline and versioning decision;
2. required environment/service configuration for the target deployment;
3. documentation impact is resolved and `bash scripts/run-documentation-gate.sh` passes;
4. the intended Gradle build/test checks have explicit evidence states;
5. runtime/server boundaries that were not tested remain `NOT_RUN`/`UNVERIFIED`, not implied PASS.

## Versioning responsibility

`app/build.gradle.kts` currently declares `versionCode = 1` and `versionName = "1.0.0"`. Session 75 does not change them. A future release-changing session owns any version bump and its documentation impact.

## Release build

Repository command:

```bash
bash ./gradlew assembleRelease
```

The command must finish successfully in the target build environment before recording build `PASS`.

## Configuration and service prerequisites

The application build reads `AUTODRIVE_SUPABASE_URL`, `AUTODRIVE_SUPABASE_ANON_KEY`, and `AUTODRIVE_ADMIN_WHATSAPP` (plus documented legacy local-property aliases). The packaged baseline does not include `local.properties` or `app/google-services.json`; supply target-environment configuration through the approved environment/build mechanism without committing secrets.

Repository server SQL/functions document intended contracts only. A release that depends on a live migration/function/RLS state needs direct deployment/runtime evidence; repository presence alone is insufficient.

## Signing truth

The current `app/build.gradle.kts` has no explicit custom `signingConfig` block. This document therefore does not invent a keystore workflow or claim production-store signing is configured. Record the actual Gradle artifact/signing behavior from the release environment and apply the distribution channel's signing process separately if required.

## Required evidence

At minimum record:

- source input/hash;
- documentation gate result;
- release build command/result;
- relevant unit/instrumentation/migration test result or explicit non-run/blocker;
- artifact path/hash for the artifact being distributed;
- service/environment prerequisites used without exposing secrets;
- signing state actually observed;
- live-server/runtime verification state.

## Artifact verification

After a successful build, hash the exact produced artifact, for example:

```bash
sha256sum app/build/outputs/apk/release/*.apk
```

Use the actual output path produced by Gradle. Do not report an artifact hash for a file that was not generated in the current release evidence.
