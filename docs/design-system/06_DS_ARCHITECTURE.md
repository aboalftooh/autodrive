# AutoDrive Design System — Session 06: Design System Architecture

**Output:** `06_DS_ARCHITECTURE.md`  
**Session:** 06 — Design System Architecture  
**Input source of truth:** `05_SCREEN_SPECS.md` + approved `04_PATTERNS.md` + `03_COMPONENT_SPEC.md` + `02_FOUNDATIONS.md`  
**Implementation changes:** None  
**Production code changes:** None  
**STATUS:** APPROVED

---

# 1. Purpose

This document defines the architectural ownership and dependency boundaries of AutoDrive Design System V1 so Session 07 can implement the system without moving business logic into UI infrastructure or changing screen behavior.

The Design System is a presentation library. Its job is to own reusable visual rules, reusable presentation components, reusable presentation patterns, and the application theme. It does not own product state, navigation execution, repositories, Android feature workflows, or domain rules.

The target chain is now:

```text
02 Foundations
      ↓
03 Components
      ↓
04 Patterns
      ↓
05 Screen Specifications
      ↓
06 Design System Architecture
      ↓
07 Design System Implementation
```

---

# 2. Source-of-truth gate

Session 05 explicitly states that continuing to Session 06 approves `05_SCREEN_SPECS.md`. The user requested Session 06, therefore Session 05 is approved and authoritative.

Session 06 may decide ownership, package placement, dependency direction, public API shape, and resource placement. It may not redefine approved visual foundations, component behavior, pattern behavior, or screen composition.

---

# 3. Architectural role of `:core:designsystem`

## 3.1 One responsibility

`:core:designsystem` is the single reusable **presentation-system module** for AutoDrive.

It owns only:

```text
visual foundations
+ theme mapping
+ reusable UI components
+ reusable UI patterns
+ presentation-only UI types required by those APIs
+ shared visual resources required by those APIs
```

It must remain independently understandable without knowing how AutoDrive authenticates users, loads chats, calculates commissions, performs withdrawals, sends media, synchronizes data, or navigates between product destinations.

## 3.2 It is not a generic dumping module

Being reusable does not automatically make a class a Design System class.

A type belongs in `:core:designsystem` only when all are true:

1. Its primary responsibility is visual/presentation behavior.
2. It is reusable across more than one screen or defines an approved DS primitive/pattern.
3. It can receive all required state from its caller.
4. It can emit user intent through callbacks without executing domain work.
5. Its API does not expose feature/domain models.
6. Its implementation does not require a ViewModel, Repository, use case, database, network client, session reader, navigation controller, permission launcher, or DI graph.

If any condition fails, ownership remains with `:app` shell or the relevant feature.

---

# 4. Target physical structure

Session 07 must evolve the current flat `theme/` + `components/` layout toward this structure:

```text
core/designsystem/
└── src/main/
    ├── kotlin/com/autodrive/app/core/designsystem/
    │   ├── foundation/
    │   │   ├── color/
    │   │   ├── typography/
    │   │   ├── spacing/
    │   │   ├── radius/
    │   │   ├── border/
    │   │   ├── motion/
    │   │   └── icon/
    │   │
    │   ├── theme/
    │   │   ├── AutoDriveTheme.kt
    │   │   └── MaterialColorMapping.kt
    │   │
    │   ├── components/
    │   │   ├── actions/
    │   │   ├── inputs/
    │   │   ├── containers/
    │   │   ├── navigation/
    │   │   ├── feedback/
    │   │   └── data/
    │   │
    │   └── patterns/
    │       ├── header/
    │       ├── dashboard/
    │       ├── metrics/
    │       ├── conversation/
    │       ├── finance/
    │       ├── settings/
    │       ├── reports/
    │       ├── media/
    │       ├── search/
    │       └── state/
    │
    └── res/
        ├── font/
        └── drawable/   # only shared DS/brand resources approved below
```

The exact filenames may differ in Session 07, but ownership must match this architecture.

---

# 5. Foundation ownership

`foundation/` owns the semantic tokens locked in Session 02.

## 5.1 Allowed content

```text
Color roles
Typography scale
Spacing scale
Radius scale
Border width/opacity rules
Motion durations/easing
Icon sizes
Glow/shadow tokens where representable as DS values
```

Foundation APIs may expose Compose value types such as:

```text
Color
Dp
TextStyle
Typography
Shape / CornerBasedShape where appropriate
Duration values / animation specs when presentation-only
```

## 5.2 Forbidden content

Foundation must not contain:

- feature names such as commission/chat/profile as token namespaces;
- business thresholds;
- domain statuses as data models;
- hardcoded Arabic product copy;
- screen layout compositions;
- repository/session/network dependencies.

## 5.3 Semantic naming rule

The canonical direction remains:

```text
Raw value → semantic foundation role → component/pattern → screen
```

Legacy names such as `GreenWithdraw`, `GoldPending`, `BgSurface2`, and one-off `20.dp` card radii are migration inputs, not the final public foundation API.

---

# 6. Theme ownership

`theme/` converts foundation roles into application-level Material3 theme behavior.

It owns:

- `AutoDriveTheme`;
- Material `ColorScheme` mapping;
- Material `Typography` mapping;
- dark-only V1 policy;
- future theme-local wiring needed to expose DS tokens consistently.

It does not own:

- screen background composition;
- feature-specific system bar behavior;
- navigation;
- current user/session state;
- runtime theme choice in V1, because V1 is locked dark-only.

### Dependency direction

```text
foundation → theme
foundation → components
foundation → patterns (normally through components)
theme must not depend on feature/app
```

Theme may depend on Foundation. Foundation must never depend on Theme.

---

# 7. Component ownership

`components/` owns the approved Session 03 component library.

Target categories:

```text
components/actions/
  AutoDrivePrimaryButton
  AutoDriveSecondaryButton
  AutoDriveTextButton
  AutoDriveIconButton
  AutoDriveFab

components/inputs/
  AutoDriveTextField
  AutoDriveSearchField
  AutoDriveNumericField
  AutoDriveSelectionField

components/containers/
  AutoDriveCard
  AutoDriveMetricCard
  AutoDriveHighlightCard
  AutoDriveAlertCard

components/navigation/
  AutoDriveBottomNavigation
  AutoDriveTopHeader
  AutoDriveBackHeader

components/feedback/
  AutoDriveBadge
  AutoDriveStatusChip
  AutoDriveSnackbarContent
  AutoDriveDialog
  AutoDriveBottomSheet
  AutoDriveLoadingState
  AutoDriveEmptyState

components/data/
  AutoDriveAvatar
  AutoDriveListRow
  AutoDriveSectionHeader
  AutoDriveDivider
  AutoDriveStatValue
  AutoDriveStatusIndicator
  AutoDriveStepIndicator
  AutoDriveInstrumentNumber
```

## 7.1 Component API contract

Every public component API follows this shape:

```text
presentation-ready values in
+ UI-only semantic variants in
+ callbacks out
```

Example conceptually:

```kotlin
AutoDriveBottomNavigation(
    selectedItem = selectedItem,
    unreadMessages = unreadMessages,
    onItemClick = onItemClick,
    onNewChatClick = onNewChatClick,
)
```

Not:

```kotlin
AutoDriveBottomNavigation(
    badgeViewModel = hiltViewModel(),
)
```

## 7.2 UI-only state types are allowed

The DS may define small presentation types when they describe appearance rather than domain behavior, for example:

```text
ButtonVariant
CardVariant
StatusTone
BadgeTone
StatEmphasis
NavigationItem presentation model
```

They must not contain domain entities or business logic.

## 7.3 Styling escape hatches

Public APIs should not normalize arbitrary styling parameters such as:

```text
color: Color
radius: Dp
borderWidth: Dp
textStyle: TextStyle
```

when an approved semantic variant exists.

`Modifier` remains allowed as the standard layout/interoperability hook. Semantic variants remain the styling contract.

---

# 8. Pattern ownership

`patterns/` owns reusable compositions approved in Session 04.

Target pattern groups:

```text
patterns/header/
  ScreenHeader

patterns/dashboard/
  DashboardHero

patterns/metrics/
  MetricSummary
  ReportStatTile

patterns/conversation/
  ConversationItem

patterns/finance/
  TransactionRow
  PendingRequestCard

patterns/settings/
  SettingsGroup
  SettingsRow

patterns/media/
  MediaActionGroup

patterns/search/
  SearchResultsList

patterns/state/
  EmptyScreen
  ErrorScreen
  LoadingScreen
```

Patterns may compose Components and Foundation values. They do not own screen navigation, ViewModels, repositories, permissions, timers, camera launchers, audio recorder lifecycle, withdrawal execution, search execution, or report calculations.

## 8.1 Pattern API contract

Patterns accept presentation-ready content and intents.

Allowed:

```text
String/AnnotatedString already prepared for display
primitive counts/amount display strings
UI-only semantic status enums
image/vector/painter presentation handles where appropriate
callbacks such as onClick/onRetry/onCancel/onMediaAction
slot lambdas for intentionally feature-owned visual content
```

Forbidden:

```text
ChatRepository
CommissionEntity
SessionReader
NavController
Flow from repository as hidden internal state source
feature ViewModel
permission launcher
Activity/Context for workflow execution
```

---

# 9. Dependency graph

## 9.1 Target high-level graph

```text
                  :core:designsystem
                   ↑      ↑      ↑
                   │      │      │
                :app   feature:* │
                   │      │      │
                   └──────┴──────┘

:core:designsystem → Compose/UI libraries only
:core:designsystem -X→ feature:*
:core:designsystem -X→ :app
:core:designsystem -X→ repositories/domain/session/network/database
```

Features and app may consume DS. DS must never consume them.

## 9.2 Core-module boundary

The preferred target is for `:core:designsystem` to remain independent of other AutoDrive core modules unless a dependency is purely presentation infrastructure and is explicitly justified.

For V1, no AutoDrive core dependency is required.

Therefore the target internal dependency rule is:

```text
:core:designsystem → no project(:core:*) dependency
```

This keeps DS portable and prevents UI infrastructure from gaining accidental access to session/network/data concepts.

## 9.3 `:core:platform` must not re-export Design System

Current state:

```text
:core:platform
  api(project(":core:designsystem"))
```

This is not the target architecture.

Reason:

- `:core:platform` represents broad platform/application infrastructure;
- re-exporting DS makes UI availability an accidental transitive property of a non-visual core module;
- feature dependencies become less explicit;
- presentation and platform boundaries blur.

Target for later implementation/refactor:

```text
UI consumer → explicit project(":core:designsystem")
```

Session 06 documents this; Session 07/08 may change Gradle only when safe and verified against current consumers.

---

# 10. Gradle dependency contract for `:core:designsystem`

## 10.1 Current problem

The current module applies/depends on:

```text
Hilt plugin
KSP plugin
lifecycle runtime
lifecycle viewmodel compose
hilt-android
hilt compiler
hilt-navigation-compose
```

These dependencies exist primarily because Bottom Navigation currently reaches into application state through `BottomNavBadgeViewModel` and Hilt.

That violates the approved DS ownership contract.

## 10.2 Target dependency set

The target module requires only Compose/presentation dependencies such as:

```text
Android library plugin
Kotlin Android
Kotlin Compose
Compose BOM
Compose UI
Compose UI Graphics
Material3
Material Icons
Compose Animation
UI tooling preview / debug tooling
```

Additional UI-only dependencies are allowed only when a specified DS component genuinely needs them.

Target removals:

```text
Hilt plugin
KSP plugin
hilt-android
hilt compiler
hilt-navigation-compose
lifecycle ViewModel Compose
```

`lifecycle-runtime-ktx` should also be absent unless Session 07 proves a presentation-only need that cannot be expressed with normal Compose runtime APIs.

## 10.3 `api` versus `implementation`

Use `api` only when a library type appears intentionally in DS public Kotlin signatures and downstream consumers need that type to compile.

Use `implementation` otherwise.

Do not use `api` to make unrelated UI dependencies conveniently transitively available to features.

---

# 11. Bottom Navigation ownership — resolved

This was an explicit Session 05 architecture question.

## 11.1 Design System owns

- Bottom Navigation visual component;
- item layout;
- selected/unselected presentation;
- center FAB presentation;
- Badge rendering;
- RTL behavior;
- touch/accessibility behavior.

## 11.2 Application shell owns

- currently selected root destination;
- navigation execution;
- opening New Chat Dialog;
- unread message count passed into DS;
- collection of any application-level state needed by the shell.

## 11.3 Feature owns unread data source

Unread message computation/observation belongs to the chat/notification application/domain side, not DS.

The current chain:

```text
BottomNavBadgeSource (inside DS)
      ↓
BottomNavBadgeViewModel (inside DS)
      ↓
AutoDriveBottomBar calls hiltViewModel()
```

must become conceptually:

```text
Chat/Notifications repository/state
      ↓
app-shell ViewModel/state owner
      ↓ unreadMessages: Int
AutoDriveBottomNavigation
```

The exact shell ViewModel class/package may be chosen during implementation without changing this ownership rule.

## 11.4 Current types disposition

```text
core/designsystem/.../BottomNavBadge.kt
  BottomNavBadgeSource      → leaves DS
  BottomNavBadgeViewModel   → leaves DS

feature/notifications/.../BottomNavBadgeSourceImpl.kt
  → may be retained/reworked as feature/application data adapter if still useful

feature/notifications/.../BottomNavBadgeModule.kt
  → may be retained/reworked only if DI is still needed outside DS
```

No DS interface is required merely to let a feature provide a badge count.

---

# 12. Home instrument ownership — resolved

Session 05 locked this distinction:

```text
Dashboard Hero composition → Design System Pattern
Pump/gauge/countdown behavior → Home feature
```

Session 06 makes the physical boundary explicit.

## 12.1 DS owns

- `DashboardHero` composition/container contract;
- generic `AutoDriveInstrumentNumber` presentation primitive specified in Session 03;
- generic visual status roles from Foundation, including the scoped Instrument palette;
- reusable layout slots required to place feature-owned hero content.

## 12.2 Home feature owns

- pump action logic;
- count/timer behavior;
- fuel/gauge business state mapping;
- business thresholds/state transitions;
- weekly competition content;
- Dynamo state computation/message selection;
- feature animation orchestration tied to product behavior.

## 12.3 Slot boundary

`DashboardHero` should support a deliberately constrained content slot rather than importing Home models.

Conceptual shape:

```kotlin
DashboardHero(
    title = ...,
    status = ...,
    heroContent = { HomePumpInstrument(...) },
    supportingContent = { ... },
    action = ...,
)
```

The DS controls the hero frame/hierarchy. The feature controls the specialized inner instrument behavior.

## 12.4 Seven-segment rule

The generic visual number renderer may live in DS as `AutoDriveInstrumentNumber`.

Its low-level primitives remain internal/private to the component implementation.

No domain threshold or pump state machine may enter DS.

---

# 13. Responsive architecture — resolved

Width adaptation is presentation behavior and may be calculated using Compose layout constraints/window information, but DS must not own navigation or activity business state to obtain it.

Preferred rule:

- components use local constraints when their own geometry must adapt;
- patterns may accept or derive presentation width classes when composition changes;
- screens remain responsible for top-level window/safe-area integration;
- no `NavController`, ViewModel, repository, or product state is introduced merely to calculate responsiveness.

For V1, define simple presentation width classes only if implementation needs them:

```text
Compact   < 600dp
Medium    600–839dp
Expanded  >= 840dp
```

If such a type is introduced, it is a UI-only DS type.

No navigation rail/master-detail behavior is allowed because Session 05 explicitly forbids it for V1.

---

# 14. Resource ownership

Resources follow the same ownership rule as Kotlin code: shared design identity belongs to DS; product/feature content belongs to its feature/app owner.

## 14.1 Resources that belong in DS

Allowed examples:

- Tajawal font files used by the global DS typography;
- reusable shared visual assets required by approved DS components;
- a global brand mark only if it is truly an application-wide identity resource rather than a screen-specific illustration.

## 14.2 Resources that do not belong in DS

The current module contains resources whose ownership is not Design System responsibility:

```text
am_dynamo_*.png
login_hero.png
launcher mipmaps / launcher foreground-background resources
file_paths.xml
```

Target ownership:

```text
am_dynamo_*.png     → Home/app feature ownership
login_hero.png      → :feature:auth ownership
launcher resources  → :app ownership
file_paths.xml      → owning app/feature/platform integration, not DS
```

These moves are implementation work, not performed in Session 06.

## 14.3 `logo_benzin.png`

`AutoDriveLogo` was already rejected as a reusable generic UI component in Session 03.

Architecture decision:

- if `logo_benzin.png` is the canonical application-wide brand mark, it may remain a shared brand resource in DS;
- the DS must not wrap it in a generic component solely for resource access;
- if later audit proves it is feature/auth-only or legacy branding, move/remove it during consolidation.

Therefore physical deletion/move is deferred until usage is validated in Session 07/10.

## 14.4 `whatsapp.png`

WhatsApp identity is not a generic AutoDrive visual token. If the asset is needed by a reusable DS action whose contract explicitly represents WhatsApp branding, it may be retained; otherwise it belongs to the consuming feature.

Current zero/unclear usage means no migration is required in Session 06.

---

# 15. Copy/text ownership

Generic visual components may contain only universal accessibility labels or framework-level defaults when truly generic and stable.

Feature/product copy is caller-owned.

Examples:

```text
"التطبيق لا يعمل بكامل الميزات" → not DS-owned
"امنح الأذونات"                 → feature/app permission flow
"رصيدي"                         → Balance screen
"تقاريري"                       → Reports screen
workshop specialty list          → Auth/Profile feature
```

Therefore `PermissionsDeniedDialog` must not remain a fixed-copy DS component. The approved `AutoDriveDialog` supplies presentation; the caller supplies title/body/actions.

---

# 16. Navigation ownership

Design System components and patterns emit intents; they do not execute navigation.

Allowed:

```kotlin
onBack: () -> Unit
onItemClick: (Item) -> Unit
onNewChatClick: () -> Unit
onOpenReport: () -> Unit
```

Forbidden inside DS:

```text
NavController
route strings
navigation graph registration
navigate(...)
popBackStack()
```

The root shell owns navigation selection and route execution.

New Chat remains a modal Dialog by Session 05; DS owns the Dialog visual container, while app/feature code owns whether and when it is shown.

---

# 17. State ownership

## 17.1 DS may own ephemeral visual state

A component may internally own state that exists only to render/interact with itself and has no product meaning, for example:

- pressed/expanded animation state;
- local focus state;
- dropdown open/closed state when the API contract permits it;
- animation progress derived directly from caller state.

## 17.2 Caller must own product state

The caller owns:

- loading request lifecycle;
- repository data;
- unread counts;
- selected route;
- send/submission state;
- validation results;
- withdrawal eligibility;
- recording lifecycle;
- permissions;
- user/session identity;
- business timers/counters;
- refresh execution.

Rule:

```text
If state must survive component replacement, drives business behavior,
or comes from domain/data/application layers → it does not belong in DS.
```

---

# 18. Data/API boundary

DS public APIs must not import feature/domain models.

## Preferred inputs

```text
String / AnnotatedString
Int / Long / Boolean where purely presentational
presentation-ready display values
ImageVector / Painter / resource abstraction when visual
UI-only semantic enums
slots
callbacks
```

## Avoid leaking

```text
Room entities
Supabase DTOs
repository models
feature ViewModel state classes
navigation destination classes owned by app
session/domain contracts
```

If a pattern requires more than a few fields, define a DS **presentation model** only when the model is stable, visual, and domain-neutral. Otherwise use explicit parameters/slots.

---

# 19. Feature-specific compositions remain outside DS

The following may use DS components/patterns but remain feature-owned:

```text
HomePumpInstrument
Dynamo content/state mapping
withdrawal form validation/workflow
chat recorder controls tied to recorder lifecycle
camera/gallery launch behavior
commission calculations
profile edit validation
permission workflows
invoice/report navigation behavior
```

A composition is not promoted into DS merely because it looks polished or is used once.

Promotion requires repeatable visual semantics and a stable domain-neutral contract.

---

# 20. Current module audit against target architecture

The current `:core:designsystem` contains approximately these implementation groups:

```text
theme/
  Theme.kt
  Typography.kt

components/
  SharedComponents.kt
  CardComponents.kt
  BottomNavigationComponents.kt
  BottomNavBadge.kt
  PermissionsRationaleDialog.kt
  SevenSegment.kt
  DonutChart.kt
```

## 20.1 Correct direction already present

- central theme exists;
- Tajawal is centralized;
- shared Compose components already exist;
- seven-segment visuals are already centralized enough to be formalized;
- most external imports are Compose/Android presentation APIs.

## 20.2 Architectural violations to correct in Session 07

### Critical

1. `BottomNavBadgeViewModel` is inside DS.
2. `BottomNavBadgeSource` is inside DS.
3. `AutoDriveBottomBar` calls `hiltViewModel()`.
4. DS requires Hilt/KSP/ViewModel dependencies because of the above.

### Structural

5. `SharedComponents.kt` combines unrelated component categories in one large file.
6. component folders do not reflect the approved Session 03 taxonomy.
7. patterns do not yet exist as their own package.
8. semantic Foundation files do not yet exist separately from Theme.
9. some resources are feature/app-owned but physically stored in DS.
10. `:core:platform` re-exports DS transitively.

### Scope/legacy

11. `DonutChart` is not admitted to V1 DS and should remain out of the approved public library until a real screen contract needs it.
12. fixed permission copy is embedded in DS.
13. workshop specialty data currently appears in shared component code and must not be part of generic DS APIs.

---

# 21. Session 07 implementation order

Session 07 must implement the architecture in dependency order, without migrating the six screens yet.

## Phase 1 — Foundations

Create/normalize semantic tokens from Session 02:

```text
Color
Typography
Spacing
Radius
Border
Motion
IconSize
```

Preserve visual behavior required by approved specs while replacing legacy public token naming.

## Phase 2 — Theme

Wire Foundation tokens into `AutoDriveTheme` and Material3 mappings.

## Phase 3 — Primitive Components

Implement approved components category by category, with preview/state coverage.

Priority:

```text
Actions
Inputs
Containers
Navigation
Feedback
Data display
```

## Phase 4 — Remove DS state ownership

Before Bottom Navigation is considered complete:

- eliminate `hiltViewModel()` from DS;
- remove/move `BottomNavBadgeViewModel` and `BottomNavBadgeSource` ownership;
- pass unread count explicitly.

## Phase 5 — Patterns

Implement Session 04 patterns using approved components.

## Phase 6 — Specialized visual primitive

Formalize `AutoDriveInstrumentNumber`; keep Home behavior outside DS.

## Phase 7 — Resource cleanup that is safe before migration

Move only resources whose ownership change can be proven not to break existing consumers. Do not delete legacy assets/components still referenced by unmigrated screens.

## Phase 8 — Implementation-state report

Produce `07_IMPLEMENTATION_STATE.md` with exact implementation/remaining gaps.

---

# 22. Session 07 non-goals

Session 07 must not:

- redesign screen information architecture;
- migrate Home/Conversations/New Chat/Reports/Balance/Settings;
- change ViewModel business behavior;
- change repositories;
- change Supabase/Room logic;
- rename routes/packages for cosmetic consistency;
- remove still-used legacy UI blindly;
- introduce Light Mode;
- add a Reports chart;
- change New Chat from Dialog;
- add navigation rail/master-detail layouts.

Screen migration belongs to Session 08.

---

# 23. Preview architecture

Every implemented public Component and Pattern should have previews demonstrating materially different visual states where practical.

Previews may use local fake **presentation-only** data.

Forbidden preview dependency patterns:

```text
real ViewModel
Hilt graph
Repository
network/database
session
feature model
```

Preview-only fixtures should remain private/internal and should not become production API merely to satisfy previews.

Required preview coverage should include where applicable:

- default;
- pressed/selected representation where previewable;
- disabled;
- loading;
- long Arabic text;
- RTL;
- error/status variants;
- compact/medium width pattern examples.

Dark Mode is the only V1 theme mode.

---

# 24. Visibility policy

Default to the narrowest visibility that supports actual reuse.

```text
public   → approved DS API consumed outside module
internal → shared DS implementation detail
private  → file/component primitive
```

Examples:

- `AutoDrivePrimaryButton` → public;
- low-level seven-segment digit geometry → internal/private;
- glow drawing helper → internal/private;
- raw foundation values not intended for screens → internal where possible;
- feature-specific visual helper → not in DS.

The goal is to make the DS public surface intentional rather than expose every helper by default.

---

# 25. Testing/static enforcement target

Session 07 should add or extend static verification so future regressions are caught automatically.

Recommended architecture checks:

```text
core/designsystem must not import:
  androidx.lifecycle.ViewModel
  androidx.hilt.*
  dagger.hilt.*
  javax.inject.*
  com.autodrive.app.feature.*
  com.autodrive.app.core.database.*
  com.autodrive.app.core.network.*
  com.autodrive.app.core.session.*

core/designsystem must not contain:
  @HiltViewModel
  hiltViewModel(
  Repository classes/interfaces
  NavController
```

Static checks should also flag direct raw styling in migrated target screens after Session 08, but enforcement must not block unmigrated legacy screens prematurely.

---

# 26. Ownership matrix

| Concern | DS | App shell | Feature |
|---|---:|---:|---:|
| semantic colors/spacing/radius | ✓ | — | — |
| Material theme mapping | ✓ | — | — |
| reusable buttons/cards/fields | ✓ | — | — |
| shared UI patterns | ✓ | — | — |
| Bottom Nav rendering | ✓ | — | — |
| selected root destination | — | ✓ | — |
| unread count collection | — | ✓ coordination | ✓ source |
| navigation execution | — | ✓ | optional destination intent owner |
| New Chat Dialog styling | ✓ | — | — |
| New Chat visibility/send workflow | — | ✓/feature | ✓ |
| camera/gallery/recording | — | — | ✓ |
| Dashboard Hero frame | ✓ | — | — |
| Home pump/gauge/countdown behavior | — | — | ✓ |
| generic instrument number rendering | ✓ | — | — |
| commission/withdrawal logic | — | — | ✓ |
| profile persistence/validation | — | — | ✓ |
| screen copy/content | — | optional shell copy | ✓ |
| global fonts | ✓ | — | — |
| launcher assets | — | ✓ | — |
| Home Dynamo images | — | Home inside app | — |
| Auth hero image | — | — | ✓ auth |

---

# 27. Target dependency rules

The following rules become normative for Design System V1:

```text
RULE DS-01
core:designsystem MUST NOT depend on app or feature modules.

RULE DS-02
core:designsystem MUST NOT own ViewModel, Repository, use case, data source, session reader, or DI binding.

RULE DS-03
Components/Patterns MUST receive product state from callers.

RULE DS-04
Components/Patterns MUST emit interaction intent by callback/slot; they MUST NOT navigate or execute product workflows.

RULE DS-05
Foundation owns semantic visual tokens; screens/features MUST NOT create competing generic visual token families.

RULE DS-06
Theme may depend on Foundation; Foundation MUST NOT depend on Theme.

RULE DS-07
Patterns may depend on Components/Foundation; Components MUST NOT depend on Patterns.

RULE DS-08
Feature-specific illustrations/assets MUST stay with their owning feature/app unless proven globally shared brand resources.

RULE DS-09
DS public APIs MUST NOT expose feature/domain models.

RULE DS-10
Hilt/KSP/ViewModel Compose are not part of the target DS Gradle surface.

RULE DS-11
Home specialized business instrumentation remains feature-owned; only generic visual primitives may be DS-owned.

RULE DS-12
Bottom Navigation unread state is caller-owned and passed as data.
```

---

# 28. Final target module shape

After Session 07 implementation and Session 08 migration, the intended architecture is:

```text
                     ┌──────────────────────┐
                     │  :core:designsystem  │
                     │──────────────────────│
                     │ foundation           │
                     │ theme                │
                     │ components           │
                     │ patterns             │
                     └──────────▲───────────┘
                                │ presentation dependency
                ┌───────────────┼────────────────┐
                │               │                │
              :app       :feature:chat    :feature:balance ...
                │               │                │
                │       ViewModels / state       │
                │       business workflows       │
                └───────────────┬────────────────┘
                                │
                   domain/data/core infrastructure
```

The arrow points **toward the Design System as a consumed UI library**, never from DS toward product layers.

---

# 29. Decisions

- `:core:designsystem` is strictly a presentation-system module.
- Its physical V1 structure is locked to `foundation/`, `theme/`, `components/`, and `patterns/`.
- Foundation tokens are separated from Material theme mapping.
- Components and Patterns receive presentation-ready state and callbacks only.
- Bottom Navigation unread state is owned outside DS; DS receives `unreadMessages` as data.
- `BottomNavBadgeViewModel`, `BottomNavBadgeSource`, Hilt lookup, and DI bindings do not belong in DS.
- Hilt/KSP/ViewModel dependencies are not part of the target DS Gradle surface.
- Application shell owns selected root destination and navigation execution.
- Home Dashboard Hero frame is DS-owned; pump/gauge/countdown/Dynamo business behavior remains Home-owned.
- Generic Instrument Number may live in DS; its low-level segment primitives remain internal.
- Width classes, if required, are UI-only presentation concepts; they do not depend on navigation/business state.
- Feature-specific images leave DS ownership; global font/approved shared brand resources may remain.
- `:core:platform` should not re-export `:core:designsystem`; consumers should depend on DS explicitly.
- DS public APIs must not leak feature/domain models.
- Session 07 implements DS only; screen migration remains Session 08.

---

# 30. Forbidden from this point forward

1. No ViewModel in `:core:designsystem`.
2. No Repository/use case/data source/session reader in `:core:designsystem`.
3. No `hiltViewModel()` or DI-owned product state lookup inside a DS Component/Pattern.
4. No direct dependency from DS to `:app` or any `:feature:*` module.
5. No navigation execution inside DS.
6. No Android permission/media/workflow execution inside DS.
7. No feature/domain model in a public DS API.
8. No business threshold/state machine inside Foundation, Components, or Patterns.
9. No feature-specific Arabic copy embedded in generic Components/Patterns.
10. No arbitrary public color/radius/border/text-style escape hatches when semantic variants exist.
11. No new feature illustration/launcher resource placed in DS merely for convenience.
12. No Home pump/gauge/countdown business logic moved into DS.
13. No unread-message collection inside Bottom Navigation.
14. No screen migration during Session 07 implementation.
15. No Light Mode implementation in V1.
16. No architecture shortcut through `:core:platform` to make DS transitively available.

---

# 31. Deferred

The following belong to later sessions:

- exact Kotlin filenames/signatures where this document gives conceptual APIs;
- actual Foundation/Theme/Component/Pattern implementation;
- actual Gradle dependency cleanup;
- physical movement of feature-specific resources;
- shell ViewModel implementation for Bottom Navigation state;
- screen migration;
- deletion of legacy components after zero-usage confirmation;
- final `logo_benzin.png` disposition after brand/usage verification;
- visual regression tooling;
- pixel-level QA;
- Design System V1 consolidation documentation.

---

# 32. Open Issues

No architecture issue blocks Session 07.

Non-blocking implementation questions:

```text
- exact public Kotlin type names for semantic variants;
- whether responsive width class is passed explicitly or derived locally per Pattern;
- exact app-shell class that collects unread counts;
- whether logo_benzin is canonical global branding or legacy branding;
- which zero-use DS assets/components can be safely removed before Session 10.
```

These questions may be resolved during implementation without changing the ownership rules in this document.

---

# 33. Next Session Input

Session 07 must use:

```text
02_FOUNDATIONS.md
        ↓
03_COMPONENT_SPEC.md
        ↓
04_PATTERNS.md
        ↓
05_SCREEN_SPECS.md
        ↓
06_DS_ARCHITECTURE.md
        ↓
07_IMPLEMENTATION_STATE.md
```

Session 07 implementation order:

```text
Foundations
   ↓
Theme
   ↓
Primitive Components
   ↓
Navigation state decoupling
   ↓
Patterns
   ↓
Previews + static verification
```

It must not migrate screen business/presentation code yet beyond changes strictly required to keep compilation architecture-safe while decoupling DS state ownership.

---

# 34. Session close

```text
STATUS: APPROVED

Decisions:
- core:designsystem is presentation-only.
- Physical ownership is foundation/theme/components/patterns.
- Bottom Navigation state and navigation execution live outside DS.
- Hilt/ViewModel/Repository/domain state are forbidden in DS.
- Home hero frame is DS-owned; specialized Home behavior remains feature-owned.
- Feature-specific resources move to their owners; global DS identity resources may remain.
- core:platform must not be the architectural re-export path for DS.
- Session 07 implements DS; Session 08 migrates screens.

Forbidden:
- Business logic, ViewModels, repositories, Hilt state lookup, navigation, permissions, or feature models inside DS.
- Local competing generic visual systems in feature code after migration.
- Screen migration before Session 08 except minimal compile-preserving wiring required by DS decoupling.

Deferred:
- Kotlin implementation, Gradle cleanup, resource moves, screen migration, visual QA, consolidation.

Open Issues:
- Only implementation-detail choices remain; none block Session 07.

Next Session Input:
- 06_DS_ARCHITECTURE.md is the authoritative architecture source of truth for Session 07.
```

**Approval gate:** continuing to Session 07 or otherwise accepting this output approves Session 06.
