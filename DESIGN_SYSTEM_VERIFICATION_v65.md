# DESIGN_SYSTEM_VERIFICATION_v65

**Verdict:** `STATIC_ACCESSIBILITY_REPAIR_COMPLETE / ADAPTIVE_UI_RUNTIME_BLOCKED`

## Acceptance

- Ratchet: `v64 → v65`
- Confirmed findings: `0 → 0`
- Historical candidates: `6 → 6` exact Material set
- Active exceptions: `0`
- Production Kotlin: `251`

## Accessibility scope

- Coverage: `56` rows
- Declared v65: `49`
- Source-proven reclassifications: `4`
- Effective v65 audit: `53`
- Verified exclusions: `3`
- Frozen findings: `33`
- Resolved static findings: `33`
- Open static findings: `0`
- Mutated production files: `14` exact frozen allowlist

## Static gates

All v07/v08/v58, v61, historical v62/v63/v64, v65 verifier, ratchet, fixtures v60-v65, protected digests, coverage reconciliation, and parent determinism gates passed. Parent gate passed twice pre-accept and twice post-accept.

## Runtime truth

Final `./gradlew --version` retried and failed with `java.net.UnknownHostException: services.gradle.org`. Therefore compile, unit/instrumented semantics, focus, font scale, touch bounds and screenshots are `NOT_RUN`, not PASS.

`runtimeAccessibilityVerified=false`  
`fullV65Completion=false`

## Package integrity

Archive: `AutoDrive-v65-accessibility-static-runtime-blocked.zip`  
Final SHA-256 is emitted in the companion `.sha256` sidecar after ZIP creation because an archive cannot contain its own stable digest.
