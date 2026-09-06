# AutoDrive Design System V1 — Consolidation & Governance

**Output:** `DESIGN_SYSTEM_V1.md`  
**Session:** 10 — Consolidation & Governance  
**Input source of truth:** Session 09 project + `09_VISUAL_QA.md`  
**STATUS:** APPROVED

---

## 1. Authority

This document is the final visual source of truth for AutoDrive Design System V1. Historical session documents remain evidence of how V1 was reached; when a historical migration alias or temporary compatibility rule conflicts with this document, this document wins.

Canonical dependency direction:

```text
Foundation → Theme / Components → Patterns → Screens
```

`:core:designsystem` is presentation-only. Business logic, ViewModels, repositories, domain models, navigation execution, permissions, media infrastructure, persistence and networking stay outside it.

---

## 2. Canonical physical structure

```text
core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/
├── foundation/
│   ├── color/
│   ├── typography/
│   ├── spacing/
│   ├── radius/
│   ├── border/
│   ├── motion/
│   └── icon/
├── theme/
│   └── Theme.kt
├── components/
│   ├── actions/
│   ├── inputs/
│   ├── containers/
│   ├── navigation/
│   ├── feedback/
│   └── data/
└── patterns/
    ├── header/
    ├── dashboard/
    ├── metrics/
    ├── conversation/
    ├── finance/
    ├── settings/
    ├── reports/
    ├── media/
    ├── search/
    └── state/
```

No compatibility bucket is part of V1.

---

## 3. Foundation rules

All reusable visual values come from semantic tokens. The canonical flow is:

```text
raw value → semantic token → component/pattern → screen
```

Approved namespaces include `AutoDriveBrand`, `AutoDriveSurface`, `AutoDriveText`, `AutoDriveBorderColor`, `AutoDriveFinance`, `AutoDriveSpace`, `AutoDriveRadius`, `AutoDriveBorder`, `AutoDriveIconSize`, `AutoDriveMotion`, `AutoDriveTypography`, and responsive `AutoDriveContentWidth` roles.

Rules:

- screens do not invent reusable colors, spacing, radius, borders or typography;
- feature-specific geometry may remain private inside a feature when it is not a reusable visual primitive;
- domain status must be mapped by the caller to a semantic presentation tone;
- Foundation never depends on Theme, Components, Patterns, app or feature modules.

---

## 4. Component inventory and naming

V1 contains 31 canonical public component functions under responsibility packages. Public reusable primitives use the `AutoDrive<Noun>` naming convention, for example:

- actions: `AutoDrivePrimaryButton`, `AutoDriveSecondaryButton`, `AutoDriveTextButton`, `AutoDriveIconButton`, `AutoDriveFab`;
- inputs: `AutoDriveTextField`, `AutoDriveNumericField`, `AutoDriveSearchField`, `AutoDriveSelectionField`;
- containers: `AutoDriveCard`, `AutoDriveHighlightCard`;
- navigation: `AutoDriveTopHeader`, `AutoDriveBackHeader`, `AutoDriveBottomNavigation`;
- feedback: `AutoDriveDialog`, `AutoDriveBottomSheet`, `AutoDriveEmptyState`, `AutoDriveLoadingState`, `AutoDriveSnackbarContent`;
- data/display: `AutoDriveAvatar`, `AutoDriveBadge`, `AutoDriveStatusChip`, `AutoDriveStatusIndicator`, `AutoDriveMetricCard`, `AutoDriveListRow`, `AutoDriveInstrumentNumber`, `AutoDriveStatValue`.

Presentation-only public types owned by the DS also use the `AutoDrive` prefix where they describe a component API, such as `AutoDriveAccent`, `AutoDriveStatusTone`, `AutoDriveAvatarSize`, and `AutoDriveSelectionOption`.

A component must not expose arbitrary color/radius/spacing overrides merely to bypass V1 styling. Add a semantic variant only when the use case is reusable and approved.

---

## 5. Pattern inventory and naming

The approved 14 V1 patterns are:

1. `ScreenHeader`
2. `DashboardHero`
3. `MetricSummary`
4. `ConversationItem`
5. `TransactionRow`
6. `PendingRequestCard`
7. `SettingsGroup`
8. `SettingsRow`
9. `ReportStatTile`
10. `MediaActionGroup`
11. `SearchResultsList`
12. `EmptyScreen`
13. `ErrorScreen`
14. `LoadingScreen`

Patterns use semantic composition names rather than duplicating primitive component names. Their state/helper types follow the same pattern namespace, for example `SettingsRowVariant` and `SearchResultsState`.

Patterns receive state and callbacks. They do not obtain a ViewModel, navigate, execute domain work, access storage/networking, or own feature copy.

---

## 6. Ownership test

Before putting code in `:core:designsystem`, all answers below must be **yes**:

1. Is its primary responsibility visual/presentation behavior?
2. Is it reusable or an explicitly approved V1 primitive/pattern?
3. Can all state be supplied by the caller?
4. Can all intent leave through callbacks?
5. Is its API free of feature/domain models?
6. Is it independent of ViewModels, repositories, DI, navigation controllers, databases, networking and platform workflows?

Otherwise it stays in the relevant feature/app layer.

---

## 7. Adding a new Component

Do not start by creating code. Apply this sequence:

1. Confirm no existing Component solves the need.
2. Confirm no existing Pattern already owns the composition.
3. Check whether a semantic Variant of an existing Component is enough.
4. Only then propose a new Component.
5. Define its semantic role, states, RTL behavior, accessibility semantics and token usage.
6. Place it in the correct responsibility package.
7. Keep its API presentation-only: data in, callbacks out.
8. Add/update previews and static governance checks.
9. Migrate the consuming screen without copying the component locally.
10. Review against this document before merge.

A new Component is rejected if its main reason is “this screen needs a slightly different local style”.

---

## 8. Modifying an existing Component

1. Identify all production call sites first.
2. Decide whether the requested behavior is universal, a semantic Variant, or feature-local.
3. Preserve the existing semantic contract unless an intentional migration is approved.
4. Do not add raw styling escape hatches to avoid token decisions.
5. Keep business/domain logic outside the Component.
6. Update previews, relevant specifications and verification rules.
7. Run all Design System static gates and affected architecture tests.
8. Re-run runtime visual QA when the modification can affect layout, motion, touch behavior or accessibility.

Breaking public DS changes require migration in the same change; compatibility aliases are not a permanent V1 strategy.

---

## 9. New-screen review gate

Every new or redesigned screen must answer these four questions first:

1. **Does an existing Component already solve it?**
2. **Does an existing Pattern already solve the composition?**
3. **Would a semantic Variant solve the remaining difference?**
4. **Is a new Component genuinely required?**

Review checklist:

- uses semantic Foundation tokens;
- uses canonical Components/Patterns rather than local copies;
- Arabic-first RTL is correct; LTR is restricted to numeric/technical islands;
- loading, empty, error, disabled and long-content states are accounted for when applicable;
- responsive width uses central roles, not screen-local breakpoints;
- touch/accessibility semantics are preserved;
- no ViewModel/domain/repository dependency is introduced into DS;
- no raw reusable colors/radii/spacing/type styles are introduced in the screen;
- no wildcard Design System imports are used;
- runtime/device QA is repeated where behavior cannot be proven statically.

---

## 10. Consolidation completed in Session 10

Removed obsolete compatibility files:

- `components/SharedComponents.kt`
- `components/BottomNavigationComponents.kt`
- `components/SevenSegment.kt`
- `theme/Typography.kt`

Removed legacy theme aliases and migrated production callers to semantic Foundation roles, including the old `Bg*`, `Text*`, `Accent*`, withdraw/pending/paid and WhatsApp aliases.

Migrated remaining production users of old shared wrappers to canonical V1 APIs. Home countdown segment geometry is feature-owned and private because it is instrumentation-specific rather than a reusable DS primitive.

Architecture/static checks now treat restoration of the deleted compatibility files or aliases as a regression.

---

## 11. Forbidden in V1

- restoring legacy compatibility files or legacy color aliases;
- creating a second local copy of an existing Component or Pattern;
- direct feature/domain dependencies inside `:core:designsystem`;
- ViewModel/Hilt/repository/network/database/navigation execution inside DS;
- reusable raw colors, dimensions, typography or shapes in migrated screens;
- arbitrary styling escape-hatch parameters that bypass semantic tokens;
- wildcard imports from `core.designsystem.theme.*` or `core.designsystem.components.*`;
- feature-specific business names in Foundation token namespaces;
- claiming pixel-perfect, motion, IME, touch-target or accessibility PASS without runtime evidence.

---

## 12. Verification and release gate

Session 10 closes the **static/code-governance** portion of Design System V1. The runtime items explicitly deferred by Session 09 remain release gates and are not converted to PASS by this document:

- pixel-level screenshot comparison on Android;
- short-height and 600–839dp / 840dp+ device widths;
- long Arabic text and extreme numeric values;
- IME/keyboard traversal and overlap;
- Dialog/Bottom Sheet motion and dismissal;
- pressed/loading/disabled animation behavior;
- physical RTL rendering;
- touch targets, font fallback and TalkBack/accessibility behavior.

The application-level Settings initial-load error-state limitation also remains outside the Design System contract.

Session 10 verification evidence:

```text
python3 tools/verify_designsystem_v07.py  → PASS
python3 tools/verify_designsystem_v08.py  → PASS
python3 tools/verify_designsystem_v09.py  → PASS
python3 tools/verify_designsystem_v10.py  → PASS
bash scripts/verify-v01-static.sh         → PASS
./gradlew :app:compileDebugKotlin --offline --no-daemon → BLOCKED (Gradle 8.7 distribution unavailable locally)
```

No Android compile-success claim is made.

---

# Decisions

- `DESIGN_SYSTEM_V1.md` is the final official visual source of truth for AutoDrive V1.
- Semantic Foundation tokens are the only canonical reusable visual values.
- Public reusable Components use `AutoDrive<Noun>` naming and responsibility packages.
- The 14 Session 04 Patterns remain the approved V1 pattern set.
- Compatibility aliases/files are removed rather than kept indefinitely.
- Feature-specific non-reusable instrumentation geometry remains private to the feature.
- Future screen work must pass the four-question Component/Pattern/Variant/New-Component gate.

# Forbidden

- legacy DS compatibility APIs;
- duplicated local components/styles;
- business logic in Design System;
- local reusable visual constants that bypass tokens;
- ungoverned public DS API additions;
- runtime PASS claims without runtime evidence.

# Deferred

- the runtime/device visual QA gates carried from Session 09;
- application-level Settings initial profile-load error-state contract.

# Open Issues

- Android/Gradle compilation remains environment-dependent until the required Gradle distribution is available locally.
- Runtime/device-only QA remains required before a release-level visual acceptance claim.

# Next Session Input

There is no Session 11 in this Design System V1 plan. Any future visual work starts from this project ZIP plus `DESIGN_SYSTEM_V1.md`; changes must follow the governance gates above.
