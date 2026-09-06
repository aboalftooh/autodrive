# AutoDrive Design System — Session 09: Visual QA

**Output:** `09_VISUAL_QA.md`  
**Session:** 09 — Visual QA  
**Input source of truth:** migrated Session 08 project + `08_MIGRATION_STATE.md` (`STATUS: APPROVED`)  
**Primary local visual reference:** `home.png`  
**STATUS:** APPROVED

---

## 1. Scope and evidence policy

Session 09 checks the six V1 surfaces after migration:

1. Home.
2. Conversations.
3. New Chat.
4. Reports.
5. Balance.
6. Settings.

The QA contract covers:

- colors, typography, spacing, radius, borders, glow and semantic hierarchy;
- scrolling, navigation shell, dialogs, sheets and actions;
- normal/large/long/empty/loading/error/disabled presentation paths where the current state contract exposes them;
- RTL behavior;
- compact, medium and expanded width behavior.

### Evidence boundary

This environment cannot truthfully perform emulator/device pixel comparison because the Gradle 8.7 distribution is not cached and network access is unavailable. Therefore:

- static/code-level visual QA is complete;
- approved visual references were used to verify hierarchy and presentation intent;
- no claim is made that pixel-perfect rendering, IME behavior, pressed animation, TalkBack, or device-specific clipping was observed at runtime;
- the device-only checks are preserved as explicit deferred runtime gates below.

This limitation does not change business logic and does not invalidate the source-level Design System migration.

---

## 2. Visual references reviewed

### Packaged source of truth

- `home.png` — strongest concrete local reference for Home and the global shell language.

### Approved conversation/library references used for QA

- `667801.jpg` — Home/dashboard direction.
- `667811.jpg` — Conversations.
- `667812.jpg` — New Chat dialog.
- `667813.jpg` — Reports.
- `image-gen-1(20260811-093814).png` — Balance redesign direction.
- `image-gen-1(20260811-094202).png` — Settings redesign direction.

The screen specifications in `05_SCREEN_SPECS.md` remain authoritative when a screenshot does not define a state or responsive behavior.

---

## 3. Defects found and resolved

### QA-09-01 — Home greeting lacked brand emphasis

**Finding:** Session 08 rendered greeting + first name using one uniform `Text` style. The approved Home direction requires the first name to receive brand emphasis.

**Resolution:**

- `AutoDriveTopHeader` now supports an optional presentation-only `titleContent` slot.
- `ScreenHeader` forwards that slot.
- Home renders one annotated heading and applies `Brand.Primary` only to the first name.
- the plain string `title` is retained as the fallback/API semantic value.

**Business/domain impact:** none.

**Status:** RESOLVED.

### QA-09-02 — Wide layouts could stretch indefinitely

**Finding:** Home, Conversations, Reports, Balance and Settings filled arbitrary viewport width. This violates the approved medium/expanded behavior.

**Resolution:** semantic width tokens were centralized in `SpacingTokens.kt`:

```text
AutoDriveContentWidth.Readable  = 600dp
AutoDriveContentWidth.Dashboard = 840dp
```

Applied presentation constraints:

- Conversations → `Readable`.
- Settings → `Readable`.
- Home → `Dashboard`.
- Reports → `Dashboard`.
- Balance → `Dashboard`.
- Bottom Navigation remains full-width as required by V1.
- New Chat remains constrained by the existing Design System Dialog.

**Business/domain impact:** none.

**Status:** RESOLVED.

### QA-09-03 — Report grid had no narrow-width readability fallback

**Finding:** report pairs were always forced into two equal columns. Long Arabic labels/values could become unreadable on very narrow screens.

**Resolution:**

- centralized `AutoDriveContentWidth.ReportTwoColumn = 360dp`;
- below that available width, each report pair becomes a vertical single-column stack;
- at/above that width, the approved two-column reading order is preserved;
- no horizontal scrolling or chart was introduced.

**Business/domain impact:** none.

**Status:** RESOLVED.

---

## 4. Screen QA matrix

| Surface | Visual hierarchy | States/behavior | RTL | Responsive | Result |
|---|---|---|---|---|---|
| Home | Header → hero → weekly competition → insight → root nav | pull-to-refresh and pump callbacks preserved | Arabic-first; numeric islands remain feature-owned | dashboard constrained to 840dp | PASS static |
| Conversations | Header + fixed search → results list → root nav | loading/error/empty/results + refresh preserved | logical alignment and shared Conversation Item | readable width constrained to 600dp | PASS static |
| New Chat | governed Dialog → text field → media actions → actions | send disabled without content; camera/gallery/voice callbacks preserved | governed DS components | Dialog remains width constrained | PASS static; runtime media/IME pending |
| Reports | Header → hero → metric/report tiles → root nav | zero remains valid data; loading geometry preserved | logical reading order preserved | 840dp body + 1/2-column fallback | PASS static |
| Balance | Back Header → balance hero → report entry → pending → history | loading/error/empty; withdraw sheet; cancel dialog; snackbar preserved | child navigation and logical amount/status layout | dashboard constrained to 840dp | PASS static; sheet motion pending |
| Settings | Header/actions → identity → target → data → optional workshop → app info | view/edit, save loading/error, sign-out dialog preserved | one RTL vertical flow | readable width constrained to 600dp | PASS static |

---

## 5. Foundation/component consistency

Static verification confirms the migrated surfaces still obey the V1 Design System constraints:

- no raw `Color(...)` in the six migrated surfaces;
- no local `RoundedCornerShape(...)` in the six migrated surfaces;
- no raw `dp`/`sp` values in the migrated surfaces;
- width limits are centralized semantic presentation tokens;
- root screens use the shared Bottom Navigation;
- Balance remains a child surface with no root Bottom Navigation;
- New Chat remains a Dialog;
- Home/Conversations/Reports/Settings use shared `ScreenHeader`;
- no ViewModel/Repository/domain ownership was moved into `core:designsystem`.

---

## 6. Verification executed

### Design System implementation gate

```text
python3 tools/verify_designsystem_v07.py
PASS
```

Result summary:

- 31 V1 components present;
- 31 component previews;
- 14 V1 patterns present;
- 14 RTL pattern previews;
- no forbidden DS business/data dependencies.

### Screen migration regression gate

```text
python3 tools/verify_designsystem_v08.py
PASS
```

Result summary:

- 4/4 root Bottom Navigation call sites migrated;
- 4/4 root headers migrated;
- six target screens use V1 call sites;
- live unread count remains shell/feature owned;
- legacy production bottom-nav/seven-segment call sites: 0;
- raw foundation styling in migrated surface files: 0;
- critical feature interactions preserved.

### Repository static gate

```text
bash scripts/verify-v01-static.sh
PASS
```

Observed results:

- behavior tests: 48/48 PASS;
- architecture reviews: 81/81 PASS;
- module checks: 62/62 PASS;
- package checks: 24/24 PASS;
- migrations: 21/21 PASS;
- rows preserved: 13/13 PASS;
- indexes: 20/20 PASS;
- query plans: 20/20 PASS;
- observability/security: 21/21 PASS;
- cleanup: 15/15 PASS.

### Android/Gradle compile attempt

Attempted:

```text
./gradlew :core:designsystem:compileDebugKotlin \
          :app:compileDebugKotlin \
          :feature:balance:compileDebugKotlin \
          :feature:profile:compileDebugKotlin \
          --no-daemon --offline
```

**Result:** BLOCKED before project configuration. The wrapper attempted to download Gradle 8.7 and failed because `services.gradle.org` is unreachable in this environment.

No build-success claim is made.

---

## 7. State coverage notes

### Verified from source/state contracts

- Conversations: loading, error, empty, populated, search-empty and background refresh.
- New Chat: empty composer, selected media, recording, creating/disabled action and inline error.
- Reports: loading and valid zero/populated metrics.
- Balance: loading, fatal error, empty history, pending requests, populated history, sheet/dialog/snackbar states.
- Settings: view, edit, saving, save error, sign-out confirmation and optional workshop data.
- Home: normal and refresh presentation; feature data values remain presentation-safe through DS/instrument components.

### Existing state-contract limitation

Settings does not currently expose a dedicated **initial profile-load error** in `ProfileUiState`; a missing profile remains in the loading presentation. Session 09 does not invent repository/sync error ownership inside a visual QA pass.

This is recorded as an application-state debt, not a Design System defect.

---

## 8. Runtime device QA gate — deferred, not claimed

The following still require a real Android runtime and screenshots/interaction evidence:

- pixel-level comparison of all six screens against approved references;
- very short-height phones;
- 600–839dp and 840dp+ physical/emulated widths;
- long Arabic names, long conversation previews and extreme financial numbers;
- IME traversal and keyboard overlap in Settings/New Chat;
- Dialog/Bottom Sheet scrim, motion and dismissal behavior;
- press/disabled/loading animations;
- RTL visual pass on actual rendering;
- touch target confirmation;
- font loading/fallback inspection;
- TalkBack/accessibility semantics.

These are release/runtime gates and must not be marked PASS without device evidence.

---

# Decisions

- Session 08 migration remains the source baseline; no feature/business behavior was redesigned.
- Home first-name emphasis is part of the approved header contract.
- responsive width limits are centralized rather than hardcoded inside screens.
- readable single-column surfaces cap at 600dp; dashboard surfaces cap at 840dp.
- Reports keeps two columns when readable and falls back to one column below the validated minimum.
- root Bottom Navigation remains unchanged and full-width.

# Forbidden

- no raw width breakpoint may be reintroduced directly into migrated screen files;
- no screen may remove the responsive width constraints without reopening the screen contract;
- no report tile may be forced into an unreadable two-column layout;
- no business/domain state may be moved into Design System to solve a visual problem;
- no pixel-perfect/device-runtime PASS may be claimed without actual runtime evidence.

# Deferred

- device/emulator visual comparison;
- keyboard/IME QA;
- motion/pressed-state QA;
- accessibility/TalkBack QA;
- application-level initial Settings load-error state contract.

# Open Issues

- Android/Gradle compile is blocked by the unavailable Gradle 8.7 distribution in this environment.
- Settings lacks a dedicated initial profile-load error state at the presentation contract level.
- device-only visual/runtime gates remain unexecuted.

# Next Session Input

Session 10 must start only from this Session 09 project and this document.

Session 10 may consolidate and govern the Design System, remove confirmed-unused legacy styling/components, lock naming/documentation rules, and preserve the runtime QA items above as explicit release gates. It must not silently convert deferred runtime checks into PASS results.
