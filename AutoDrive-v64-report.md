# AutoDrive v64 Report

## Input

- Source: `AutoDrive-v63-settings-static-runtime-blocked.zip`
- SHA-256: `b6c8f4c65c2a462cd2b5deedcc788a3478c50b3a097d413fa56e04a20b432c25`
- Archive entries: `632`

## Execution result

- Wave 0/A/B/C completed under the exact allowlist.
- Ratchet advanced by tool from `v63` to `v64`.
- Confirmed debt: `46 → 0`; Material: `43 → 0`; A11Y-001: `3 → 0`.
- Accepted candidates: `18 → 6`; new violations/candidates `0`; exceptions `0`.
- Business logic, Navigation, ViewModels, repositories/data flow, and Gradle configuration remained protected.

## Static verification

V07, V08, exception validator, V61, V62 historical, V63 historical, V64 adoption, Ratchet S64, fixtures v60-v64 (`v64=22/22`), and v58 static (`67/0`) all PASS. Parent gate passed twice pre-accept and twice post-accept deterministically.

## Runtime

`./gradlew --version` failed before Gradle bootstrap because `services.gradle.org` could not resolve (`UnknownHostException`). Runtime compile/UI tests/screenshots are not claimed.

## Final verdict

`STATIC_COMPONENT_ADOPTION_COMPLETE / UI_RUNTIME_BLOCKED`

Package SHA-256 is recorded externally after archive closure in the adjacent `.sha256` evidence file.
