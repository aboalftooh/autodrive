# DESIGN_SYSTEM_BASELINE_v59

**Status:** `BASELINE_STATIC_COMPLETE / RUNTIME_CAPTURE_BLOCKED`  
**Source:** `AutoDrive-v58(3).zip`  
**SHA-256:** `f867f1b3fae63d586172b52e12106dfcc6a9307c826b1a182eddd43374db84ee`  

## 1. Input identity & structural fingerprints

- Archive entries: **883**
- Production Kotlin: **251**; manifest SHA-256 `3b684bd91c390eeade95b5594d8924156465c88be2510caa843540c37c877751`
- Compose-bearing production source: **58**
- Preview-only DS Compose excluded: **2**
- Coverage eligible: **56** = 39 App/Feature + 17 DS runtime; UI manifest `4e3a94666454e9308e445d1de5fd3d88aed7dbb9646121ee3ae023f004f58883`
- `core/designsystem` Kotlin: **27**; `src/main` files: **32**

## 2. Tool baseline

| Tool | Exit | Result | Evidence |
|---|---:|---|---|
| `python3 tools/verify_designsystem_v07.py` | 0 | PASS | stdout SHA `4252a141eec02d6bea420f8e71cbf136f9d6e78adaa9d264fbc7d0623c103bff`; failures 0 |
| `python3 tools/verify_designsystem_v08.py` | 1 | TOOL_REPORTED_FAILURES | stdout SHA `37b48418e4a9a4cdcbcc7d0a4f04a68c8fd6f49d0f8cda24daddffc17107e932`; failures 16 |
| `python3 tools/verify_designsystem_v09.py` | 1 | TOOL_REPORTED_FAILURES | stdout SHA `12f7970d4a5c7413f0d00c94ea18dfc62d7a887f75fb699b605719a0fd779cca`; failures 8 |
| `python3 tools/verify_designsystem_v10.py` | 1 | TOOL_REPORTED_FAILURES | stdout SHA `303d133dba076c654c16cf218ef1223e5da99e418eaccc2e79c70e869afe5ec9`; failures 4 |
| `bash scripts/verify-v58-static.sh` | 0 | PASS | stdout SHA `7a4ee04b50471cc2909752d14a594f69dfc5e2c769f86d2b9a06b10ae261b802`; failures 0 |

V08/V09/V10 failures are preserved as current tool assertions; false positives/stale syntax are not converted into product defects. V58 static behavior/architecture gate remains **67 passed / 0 failed**.

## 3. Manual triage highlights

- V08 unread-count assertion: **FALSE_POSITIVE_STALE_SYNTAX** — `AppNavigation.kt` collects unread flow and passes `unreadMessages` to `mainGraph`.
- V08 `viewModel::startEditing`: **FALSE_POSITIVE_SYNTAX** — current `ProfileScreen.kt` calls `viewModel.startEditing(...)` in section lambdas.
- V08 `viewModel.saveProfile`: **STALE_SYMBOL_EXPECTATION** — current behavior uses `saveAccount`, `savePayout`, `saveWorkshop`, `setWeeklyTarget`.
- V09 first-name emphasis: visual emphasis **exists locally** through `AutoDriveBrand.Primary`; missing item is ScreenHeader/titleContent contract adoption.
- All 16 V08, 8 V09, and 4 V10 messages are represented with per-message classifications in `DESIGN_SYSTEM_BASELINE_v59.json`.

## 4. Coverage reconciliation

- CSV rows: **56/56**; unique paths: **56**; missing eligible paths: **0**; extra paths: **0**; duplicates: **0**.
- Candidate lexical sentinels across 39 App/Feature Compose files: **52 Color**, **44 sp**, **529 dp**, **85 RoundedCornerShape**.
- Candidates are not violations. Repeated raw dp/sp/radius values are confirmed only when repetition establishes a shared decision; raw Home palette is grouped at file/palette level instead of one violation per Color call.

## 5. Rule-by-rule counts

| Rule | Candidates | Confirmed unapproved baseline | Approved exceptions |
|---|---:|---:|---:|
| `DS-A11Y-001` | 3 | 3 | 0 |
| `DS-A11Y-002` | 6 | 0 | 0 |
| `DS-A11Y-003` | 0 | 0 | 0 |
| `DS-BORDER-001` | 0 | 0 | 0 |
| `DS-COLOR-001` | 52 | 3 | 0 |
| `DS-CONTRACT-001` | 12 | 12 | 0 |
| `DS-CONTRAST-001` | 0 | 0 | 0 |
| `DS-DUP-001` | 1 | 0 | 0 |
| `DS-ELEVATION-001` | 0 | 0 | 0 |
| `DS-EXCEPTION-001` | 0 | 0 | 0 |
| `DS-MATERIAL-001` | 62 | 50 | 0 |
| `DS-SHAPE-001` | 85 | 3 | 0 |
| `DS-SPACE-001` | 529 | 3 | 0 |
| `DS-TYPE-001` | 44 | 3 | 0 |

`baseline_count` counts confirmed findings, not raw lexical occurrences. Exception Ledger is intentionally empty: `[]`.

## 6. Home / Reports / Settings current state

### Home
- Local `HomeHeader`; `ScreenHeader` absent.
- Supporting line and first-name brand emphasis exist locally.
- `DashboardHero` and `AutoDriveInstrumentNumber` adoption absent from current Home hero.
- Dashboard width contract absent.
- Local Home palette/shared raw styling debt is recorded for v61.

### Reports
- `ScreenHeader` and `DashboardHero` are already used.
- `ReportStatTile`, Dashboard max-width, and narrow fallback are absent from root Reports composition.
- `LOADING / ERROR / CONTENT`, retry, competition state, calculations/data source, and navigation are frozen behavior.

### Settings
- `SettingsGroup`, `SettingsRow`, DS fields/sheets/dialogs are consumed.
- `SettingsRow` computes destructive `titleColor` but does not apply it: `DS59-SETTINGS-001`.
- Section-specific edit/save APIs and WORKSHOP_OWNER guard are frozen behavior.

## 7. Exception Ledger

- Path: `core/designsystem/verification/designsystem-exceptions.json`
- Active exceptions: **0**.
- Legacy debt is baseline debt for the v60 ratchet, not an exception. Wildcards/whole-rule disables are not introduced.

## 8. Visual evidence status

- `home.png` — SHA `7f9e8b...d950`, 941×1672, **LEGACY_VISUAL_REFERENCE**.
- `reports.png` — SHA `af53a725...a7b0`, 941×1672, **LEGACY_VISUAL_REFERENCE**.
- Current runtime Home/Reports/Settings captures are **missing** because Gradle wrapper bootstrap cannot reach `services.gradle.org`. No legacy PNG is promoted to current-runtime evidence.

## 9. Runtime/build limitation

Command: `./gradlew --version`  
Result: exit `1`  
Blocking error: `java.net.UnknownHostException: services.gradle.org` while requesting `gradle-8.7-bin.zip`.

Therefore Full PASS is not claimed; static baseline is complete and runtime capture remains blocked.

## 10. Behavior baseline

### HOME_V1
- route: `home`
- root: `HomeScreen`
- selected root nav item: `home`
- ViewModel/API calls frozen: refreshDynamoMessage on Lifecycle.ON_RESUME, refresh, onPumpTapped, onPumpAnimationComplete
- must not change: navigation callbacks; competition availability branching; pump interaction lifecycle; ON_RESUME Dynamo refresh; unread count presentation

### REPORTS_V1
- route: `activity_log?filter={filter}`
- root: `ActivityLogScreen`
- selected root nav item: `reports`
- ViewModel/API calls frozen: setCompetitionActive(competitionAvailability == ACTIVE), retryReports
- must not change: report calculations/data source; retry behavior; financial/invoice/competition navigation

### SETTINGS_V1
- route: `profile`
- root: `ProfileScreen`
- selected root nav item: `settings`
- ViewModel/API calls frozen: startEditing(ACCOUNT), startEditing(PAYOUT), startEditing(WORKSHOP), startEditing(WEEKLY_TARGET), cancelEditing, saveAccount, savePayout, saveWorkshop, setWeeklyTarget, signOut
- must not change: section-specific save APIs; IBAN text behavior; weekly target local preference; WORKSHOP_OWNER guard; sign-out confirmation

## 11. Acceptance verdict

- Input identity/fingerprints: **PASS**
- Static tool evidence captured: **PASS**
- 56-row coverage set equality: **PASS**
- 48-contract registry: **PASS**
- Exception Ledger schema/policy: **PASS**
- Production Kotlin mutation: **0**
- Historical DS docs/verifiers/Gradle files mutation: **0**
- Runtime screenshot baseline: **BLOCKED**

**Final v59 status: `BASELINE_STATIC_COMPLETE / RUNTIME_CAPTURE_BLOCKED`.**

## 12. v60 handoff

v60 must consume this JSON/CSV/registry/ledger as the ratchet authority, preserve stale-verifier triage, and must not recompute debt from memory or historical APPROVED labels.
