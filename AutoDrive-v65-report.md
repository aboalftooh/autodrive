# AutoDrive v65 Report

## Result

`STATIC_ACCESSIBILITY_REPAIR_COMPLETE / ADAPTIVE_UI_RUNTIME_BLOCKED`

- Input identity: exact (`2a5fe10e…8b63a`, 1109 entries).
- Discovery lock: 33 confirmed static accessibility findings.
- Static repair: 33/33 resolved; open static = 0.
- Production mutations: 14 files, exact frozen allowlist.
- Protected production: 198-file digest exact.
- Protected DS-main: 12-file digest exact.
- Material ratchet: 0 confirmed; exact six historical candidates retained.
- Exceptions: 0.
- Ratchet acceptedVersion: `v65`.
- Parent static gate: PASS ×2 pre-accept and PASS ×2 post-accept.
- Runtime: BLOCKED by `UnknownHostException: services.gradle.org`; no runtime PASS claimed.

## Main repairs

Stable loading names/busy semantics; selected/unread/progress/heading semantics; >=48dp static targets; enabled placeholder contrast; non-duplicated icon/badge speech; image-viewer semantics; tap/long-press labels.

## Handoff to v66

Use `DESIGN_SYSTEM_UI_COVERAGE_v65.csv`, verification JSON/MD, accessibility audit/matrices, frozen findings lock, ratchet state, v65 verifier/fixtures, and `scripts/verify-v65-static.sh`. Preserve `runtimeAccessibilityVerified=false` and rerun runtime gates.

## Package integrity

`AutoDrive-v65-accessibility-static-runtime-blocked.zip` is accompanied by `AutoDrive-v65-accessibility-static-runtime-blocked.zip.sha256`; the sidecar is the authoritative final archive digest.
