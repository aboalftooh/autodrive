# Build fixes applied to v01

## Source

Applied from `build-fixes-delivery.zip` after file-by-file comparison with the v01 baseline.

## Accepted fixes

### 1. Material3 opt-in

- File: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/SharedComponents.kt`
- Change: added `@OptIn(ExperimentalMaterial3Api::class)` to `SpecialtyPicker`.
- Classification: compiler acknowledgement only; no behavior change.

### 2. Design system resource namespace

- File: `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/theme/Theme.kt`
- Change: imported `com.autodrive.app.core.designsystem.R` and used that module's `R.font.*` references.
- Classification: correct module resource ownership; no font or UI change.

## Remaining build blocker

`WORKSHOP_SPECIALTIES` is referenced by `SpecialtyPicker` but is not defined anywhere in the supplied v14, v00, or v01 sources.

No value list was invented because it controls user-facing workshop specialties and may affect persisted `specialty` values and content filtering.

## Build status

Gradle build was not run in this environment. The last external Android Studio/Codex build remained blocked by the missing `WORKSHOP_SPECIALTIES` definition.

## Static verification after applying fixes

- Test inventory: `142/142` classified.
- Executed pure behavior tests: `48/48` passed.
- Architecture reviews: `81/81` passed.
- Core pure Kotlin compilation: passed.
- Module checks: `60/60` passed.
- Package checks: `24/24` passed.
- Room migration/preservation/index/query-plan checks: `74/74` passed.
- Observability/security checks: `21/21` passed.
- Cleanup checks: `15/15` passed.
- Combined static gates: `324/324` passed.

These static gates do not compile Android Compose resources; therefore the external Gradle blocker for `WORKSHOP_SPECIALTIES` remains unresolved.
