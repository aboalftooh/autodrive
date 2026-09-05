# AutoDrive v56 — Profile Update Semantics + Editing State Verification

## Scope

تم تنفيذ الجلسة **v56 فقط** فوق `AutoDrive-v55.zip` وفق `AutoDrive_Competition_Reports_Settings_Execution_Plan_v49-v58.md`.

الهدف المنفذ: إصلاح منطق تحديث الملف الشخصي وحالة التحرير قبل إعادة بناء واجهة الإعدادات في v57، مع الحفاظ على المعمارية وتدفق Repository/Outbox الحالي.

## Modified files

1. `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileUiState.kt`
2. `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileViewModel.kt`
3. `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/data/ProfileRepositoryImpl.kt`
4. `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt` — compile adaptation only for `editingSection`; no UX redesign.
5. `feature/profile/src/test/profile-v56-contract.sh`
6. `docs/verification-v56.md`

No other production files changed relative to `AutoDrive-v55.zip`.

## 1. Editing state

Replaced the global state:

```text
isEditing: Boolean
```

with:

```kotlin
enum class ProfileEditSection {
    ACCOUNT,
    PAYOUT,
    WORKSHOP,
    WEEKLY_TARGET
}

val editingSection: ProfileEditSection? = null
```

`ProfileScreen.kt` was changed only where required to compile against the new state. The v55 global form remains intentionally until the v57 UX rebuild.

## 2. Account save semantics

Added:

```text
saveAccount(fullName, phone)
```

Behavior:

- trims both fields;
- blank `fullName` is rejected;
- blank `phone` is rejected;
- updates account fields only;
- preserves payout and workshop fields;
- does not use `ifBlank { oldValue }` to hide validation.

## 3. Payout save semantics

Added:

```text
savePayout(bankName, bankAccount)
```

Behavior:

- trims values only at save time;
- blank `bankName` becomes `null`;
- blank `bankAccount` becomes `null`;
- letters/spaces are not rejected by ViewModel validation;
- no numeric or IBAN regex validation was added.

This allows an existing payout value to be cleared rather than silently restoring the previous value.

## 4. Workshop save semantics

Added:

```text
saveWorkshop(workshopName, specialty, workersCount, address)
```

Behavior:

- available for `AccountType.WORKSHOP_OWNER` only at the ViewModel boundary;
- blank optional text fields become `null`;
- blank `workersCount` becomes `null`;
- invalid nonblank `workersCount` produces validation error and does not save;
- account and payout fields remain unchanged.

## 5. Weekly target

The existing local preference behavior remains unchanged:

```text
minimum = 100,000
maximum = 5,000,000
step    = 50,000
```

`setWeeklyTarget()` still writes only to `DashboardPreferences`. No Competition dependency or server write was introduced.

The v57 screen rebuild remains responsible for the new explanatory copy and field presentation.

## 6. Save success/error semantics

All section save paths use the same save boundary:

```text
start → isSaving=true
success → editingSection=null + successMessage="تم الحفظ بنجاح"
error → editor remains open + saveError
```

Unrelated profile fields are preserved because each section creates an updated copy of the current domain model containing only its owned changes.

## 7. Repository / Outbox preservation

The existing update sequence was preserved:

```text
optimistic Room update
→ syncStatus = PENDING
→ SessionWriter update
→ direct Supabase update
→ Outbox fallback on direct failure
```

Preserved identifiers:

```text
operation      = UPDATE_PROFILE
idempotencyKey = profile:<userId>
```

No Outbox schema, worker, sync coordinator, database schema, or migration changed.

### Nullable clearing

The direct Supabase PATCH now builds an explicit JSON payload and writes cleared optional fields as JSON `null`.

The Outbox payload serializer now uses:

```text
encodeDefaults = true
explicitNulls  = true
```

so nullable fields are recorded explicitly in the queued payload instead of being lost while creating the operation.

## 8. Sign-out / architecture regression

Unchanged:

- `SignOutAction` flow;
- authentication/session ownership;
- `SyncCoordinator` behavior;
- finance and withdrawal flows;
- competition feature;
- reports feature;
- Gradle/AGP/Kotlin/dependencies;
- Room schema/version.

## 9. Tests written and executed

Created:

```text
feature/profile/src/test/profile-v56-contract.sh
```

Result:

```text
16 / 16 PASS
```

Verified statically:

1. section editing state replaces global boolean;
2. `saveAccount` updates required account fields;
3. `saveAccount` preserves payout/workshop;
4. blank name/phone are rejected;
5. blank bank name clears to null;
6. blank bank account clears to null;
7. payout account accepts nonnumeric text;
8. blank workers count clears to null;
9. invalid workers count is rejected;
10. marketer cannot save workshop data;
11. weekly target remains local and Competition-independent;
12. optimistic Room row remains `PENDING`;
13. profile idempotency key is unchanged;
14. Outbox operation remains `UPDATE_PROFILE`;
15. direct PATCH contains explicit JSON nulls;
16. queued Outbox payload records explicit nullable fields.

## 10. Gradle verification

Attempted:

```text
./gradlew :feature:profile:compileDebugKotlin --no-daemon
```

Execution was blocked before compilation because the environment could not download the Gradle 8.7 distribution:

```text
java.net.UnknownHostException: services.gradle.org
```

Per the execution plan, no wrapper, plugin, SDK, target/min SDK, dependency, lint, or test hack was introduced.

## 11. Deferred to v57 by design

The following v55 UI remains intentionally unchanged because v56 explicitly forbids Profile screen redesign except compile fixes:

- global edit-form presentation;
- numeric IBAN field presentation;
- final section-based Settings hierarchy;
- weekly-target explanatory copy.

These are v57 responsibilities; v56 establishes the correct state and save semantics underneath them.

## Result

**v56 implementation complete within the defined scope.**

`AutoDrive-v56.zip` is the Source of Truth for v57.
