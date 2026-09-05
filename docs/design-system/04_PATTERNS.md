# AutoDrive Design System — Session 04: UI Patterns

**Output:** `04_PATTERNS.md`  
**Session:** 04 — UI Patterns  
**Input source of truth:** `03_COMPONENT_SPEC.md` + approved `02_FOUNDATIONS.md`  
**Implementation changes:** None  
**Production code changes:** None  
**STATUS:** APPROVED

---

# 1. Purpose

This document defines the reusable UI compositions that sit between Design System components and complete screens.

A Pattern is a repeated composition of already-approved components. It may define hierarchy, spacing, state switching, and interaction structure, but it does not own business logic, repositories, ViewModels, feature data sources, or navigation decisions.

The V1 dependency chain is now:

```text
Foundations
   ↓
Components
   ↓
Patterns
   ↓
Screen Specifications
```

Session 04 does not implement Kotlin, migrate screens, or redefine visual foundations.

---

# 2. Source-of-truth gate

Session 03 explicitly stated that continuing to Session 04 counts as approval. The user requested execution of Session 04, therefore `03_COMPONENT_SPEC.md` is approved and authoritative.

No component contract from Session 03 is changed here.

---

# 3. Pattern rules

Every AutoDrive V1 Pattern must follow these rules:

1. Compose only approved Session 03 components.
2. Consume only approved Session 02 foundations through those components.
3. Receive content/state/callbacks from the feature or application shell.
4. Never call a ViewModel, Repository, Flow source, Hilt entry point, recorder, camera, network client, or database.
5. Never perform navigation directly; expose intent callbacks instead.
6. Never own domain enums merely to style a screen. The caller maps domain state to semantic pattern/component state.
7. Never expose arbitrary color/radius/spacing/border parameters as permanent styling escape hatches.
8. Preserve Arabic-first RTL composition.
9. Apply LTR only to the smallest numeric/technical island.
10. Pattern-level loading/error/empty states must preserve the surrounding screen structure where possible.
11. Feature-specific copy remains outside the Pattern.
12. Patterns may define slots where content varies, but slots must have a clear semantic purpose.

---

# 4. V1 Pattern inventory

| Pattern | Primary role | Main current targets |
|---|---|---|
| Screen Header | standard top-of-screen composition | Home, Conversations, Reports, Balance, Settings, supporting screens |
| Dashboard Hero | one dominant branded metric/action area | Home, Balance, competition/highlight surfaces |
| Metric Summary | compact group of related metrics | Reports, Balance, Commission report |
| Conversation Item | conversation/activity list entry | Conversations |
| Transaction Row | financial/history list entry | Balance, weekly commissions, invoice/history lists |
| Pending Request Card | actionable pending financial/request item | Balance pending withdrawals and future pending flows |
| Settings Group | grouped profile/settings section | Settings/Profile, info screens where applicable |
| Settings Row | one setting/profile detail/action | Settings/Profile, About/info menus |
| Report Stat Tile | one report metric navigation tile | Reports, Commission report |
| Media Action Group | camera/gallery/voice/send-ready media composition | New Chat, Chat composer |
| Search Results List | search + results + result states | Conversations, FAQ, future searchable lists |
| Empty Screen | complete valid no-content state | list/content destinations |
| Error Screen | complete recoverable failure state | data-backed destinations |
| Loading Screen | complete initial-load state | data-backed destinations |

These 14 patterns are the V1 pattern set required by the Session Plan.

---

# 5. Shared pattern geometry

Unless a Screen Specification explicitly needs a responsive exception:

```text
Screen horizontal gutter: Space.LG = 16dp
Primary section gap:      Space.2XL = 24dp
Compact section gap:      Space.LG = 16dp
Internal compact gap:     Space.SM / Space.MD = 8 / 12dp
Major separation:         Space.3XL = 32dp
```

Rules:

- Patterns do not create 6/10/14/18dp free spacing.
- A component may keep its own geometry defined in Session 03.
- Screen Specs may choose between approved spacing tokens but cannot invent new foundation values.

---

# 6. Screen Header Pattern

## 6.1 Function

Creates a consistent top-of-screen hierarchy using the approved `Top Header` or `Back Header` component, with optional context below it.

This Pattern resolves the current duplication of direct `TopAppBar` and manual Row/Column headers.

## 6.2 Composition

### Root destination

```text
Top Header
├── title / greeting slot
├── optional supporting text
└── optional trailing action(s)
```

### Nested destination

```text
Back Header
├── auto-mirrored back action
├── title
├── optional supporting text
└── optional trailing action
```

Optional context directly below:

```text
Search Field
or
Status Chip / Badge row
or
short contextual body text
```

## 6.3 Spacing

- header to first major content block: `Space.XL` or `Space.2XL`;
- supporting content under header: `Space.SM`;
- trailing controls retain >=48dp hit target;
- screen horizontal gutters remain owned by the screen container unless header component already includes them.

## 6.4 States

- default;
- optional unread Badge on a trailing action;
- optional disabled action;
- no header-level loading spinner unless the action itself is loading;
- header does not disappear during content loading/error states.

## 6.5 Interaction

- back callback is supplied by caller;
- trailing actions expose callbacks;
- no navigation controller inside Pattern;
- notification/badge counts are supplied externally.

## 6.6 RTL

- title starts on visual right;
- back icon uses AutoMirrored form;
- trailing actions are visual end/left;
- numeric badge content remains stable LTR internally.

## 6.7 Use in

- Home: root header with greeting + notification action;
- Conversations: title + optional contextual action, followed by search;
- Reports: root header;
- Balance: root header;
- Settings/Profile: root header;
- Notifications, Commission report, Chat, invoice/detail/history and info/legal screens: nested Back Header form.

## 6.8 Do not use when

- the content is a modal Dialog/Bottom Sheet;
- a hero itself is the only intended top composition and Screen Specs explicitly remove a conventional title bar.

---

# 7. Dashboard Hero Pattern

## 7.1 Function

Creates one dominant, branded, high-information area for a dashboard-like screen.

It is a composition, not a generic card. It may represent a balance, a branded pump/instrument state, or another single primary dashboard focus.

## 7.2 Composition

```text
Highlight Card
├── optional label / Status Chip
├── one dominant visual/value
│   ├── Stat Value (Hero)
│   └── or Instrument Number / specialized feature visual
├── supporting label/value row(s)
└── optional primary or secondary action
```

Home may place feature-owned instrument visuals inside the hero slot. Those visuals remain feature behavior until Session 05/06 decides their exact ownership.

## 7.3 Hierarchy

- exactly one dominant focus;
- only one `Stat Value.Hero` in the Pattern;
- supporting information must be visually subordinate;
- CTA cannot compete equally with the main metric.

## 7.4 Spacing

- internal padding: `Space.XL` or `Space.2XL`;
- label → dominant value: `Space.SM`/`Space.MD`;
- dominant value → support: `Space.LG`;
- support → action: `Space.XL`;
- Pattern itself is separated from adjacent major sections by `Space.2XL`.

## 7.5 Visual treatment

- container: Highlight Card;
- border/glow semantics come from the Highlight Card variant;
- strong glow is reserved for the single dominant focal area and must remain rare;
- no unrelated multi-color glow.

## 7.6 States

- default;
- loading: keep hero geometry; replace data region with component-level loading or placeholder agreed later;
- unavailable/empty value: show semantic copy/state supplied by feature without collapsing the hero;
- disabled action: preserve geometry;
- error: hero may show neutral last-known content only if product logic explicitly supplies it; otherwise Screen-level Error Pattern owns the content state.

## 7.7 Interaction

- hero may expose one primary action and at most one subordinate action;
- feature animation such as Home pump count remains feature-owned;
- no Pattern-owned business timer or calculation.

## 7.8 RTL

- overall composition RTL;
- dominant pure numeric/instrument display may use local LTR;
- labels remain Arabic RTL.

## 7.9 Use in

- Home pump/instrument hero;
- Balance hero;
- selected competition/highlight surfaces only when Screen Specs confirm dominant hierarchy.

## 7.10 Do not use when

- several metrics have equal weight: use Metric Summary;
- content is merely a clickable report tile: use Report Stat Tile.

---

# 8. Metric Summary Pattern

## 8.1 Function

Groups 2–6 related metrics into one coherent summary area without letting each screen invent a metric grid.

## 8.2 Composition

```text
optional Section Header
Metric Card × N
```

Each card contains:

```text
label
Stat Value (Small/Medium/Large)
optional Status Indicator / short supporting text
```

## 8.3 Layout

Preferred compact-phone behavior:

- 2-column grid where content remains readable;
- single-column fallback when text/value width cannot fit;
- no horizontal scrolling for core financial/report metrics;
- equal-height cards within one row where practical.

Exact responsive breakpoints belong to Session 05 Screen Specs.

## 8.4 Spacing

- grid gap: `Space.MD` or `Space.LG`;
- section header to grid: `Space.MD`;
- major section separation: `Space.2XL`.

## 8.5 States

- default;
- per-metric unavailable state supplied as content, not by removing the card unpredictably;
- content loading should preserve grid geometry;
- disabled does not normally apply because metrics are display-first;
- clickable metrics must visibly signal clickability through the card variant/content, not color alone.

## 8.6 Interaction

- entire Metric Card may be clickable when it navigates to a deeper metric destination;
- caller owns callback and navigation;
- avoid nested clickable controls inside a clickable metric card.

## 8.7 RTL

- labels start right;
- numeric `Stat Value` keeps digit order LTR;
- grid reading order follows RTL screen semantics.

## 8.8 Use in

- Reports summary metrics;
- Balance secondary metrics where multiple values have similar importance;
- Commission report.

## 8.9 Do not use when

- one metric dominates the screen: Dashboard Hero;
- each tile is primarily a navigation destination with stronger action semantics: Report Stat Tile.

---

# 9. Conversation Item Pattern

## 9.1 Function

Standardizes a conversation/recent-activity row while preserving unread, media, time, and selection semantics.

## 9.2 Composition

```text
Base Card or list container
└── List Row
    ├── Avatar / leading identity visual
    ├── title
    ├── message/activity preview
    ├── optional media/status indicator
    └── trailing column
        ├── timestamp
        └── optional unread Badge
```

## 9.3 Spacing

- use List Row internal geometry from Session 03;
- list item gap: `Space.SM` when cards are individually surfaced;
- use Divider instead of card gap only if Session 05 chooses a continuous list surface, never both by default.

## 9.4 States

- read;
- unread: Badge + higher text emphasis and/or selected accent signal; not color alone;
- selected/pressed;
- sending/failed media status only if caller maps it to an approved Status Indicator/Chip;
- disabled is generally not used;
- long preview text truncates rather than expanding row height without bound.

## 9.5 Interaction

- row click opens conversation/activity destination through caller callback;
- optional secondary action must not compete with primary row click;
- unread state mutation is feature-owned.

## 9.6 RTL

- Avatar at visual start/right;
- time and Badge at visual end/left;
- mixed Latin/technical fragments stay locally stable;
- no reversed timestamp digits.

## 9.7 Use in

- Conversations / `RecentActivityScreen`;
- future conversation-like notification/activity surfaces only if semantics match.

## 9.8 Do not use when

- the row is a generic notification with no conversation identity;
- the content is a financial transaction: use Transaction Row.

---

# 10. Transaction Row Pattern

## 10.1 Function

Represents one financial or ledger/history entry with amount, description, date, and semantic status.

## 10.2 Composition

```text
List Row
├── optional leading status/icon visual
├── primary label
├── secondary metadata/date/reference
└── trailing
    ├── Stat Value (Small) / formatted amount
    └── optional Status Chip or Status Indicator
```

Optional Divider belongs to the list parent.

## 10.3 Visual hierarchy

- transaction meaning/title first;
- amount is high-emphasis but not Hero typography;
- status is semantic and compact;
- identifiers/dates are secondary.

## 10.4 States

- completed/success;
- pending/warning;
- failed/error;
- paid/neutral finance semantic;
- selected/pressed if row opens details;
- loading handled by surrounding list/screen, not a special transaction-row spinner.

Caller maps domain state to `Finance.*` or `Status.*` semantics.

## 10.5 Interaction

- optional full-row click for details;
- no destructive inline action unless a Screen Spec proves it necessary;
- cancellation/confirmation should use a dedicated action + Dialog/Bottom Sheet flow, not hide behind row styling.

## 10.6 RTL

- description starts right;
- amount/date digits are LTR islands;
- amount may align visual end/left where the screen composition requires it.

## 10.7 Use in

- Balance transaction history;
- Weekly commissions;
- invoice/history lists when the entry is fundamentally financial;
- commission report detail lists.

## 10.8 Do not use when

- row is an actionable pending request with multiple actions: Pending Request Card;
- row is a generic profile/menu item: Settings Row/List Row.

---

# 11. Pending Request Card Pattern

## 11.1 Function

Displays a pending request that needs attention and may expose one or two explicit actions.

Current primary fit: pending withdrawal requests in Balance.

## 11.2 Composition

```text
Alert Card (Warning/Info semantic)
├── top row
│   ├── title / request identity
│   └── Status Chip
├── key value / amount
├── supporting metadata
└── action row
    ├── optional Secondary/Text action
    └── optional Primary/destructive semantic action as specified by flow
```

## 11.3 Spacing

- card internal padding from Alert Card;
- title → value: `Space.MD`;
- value → metadata: `Space.SM`;
- metadata → actions: `Space.LG`;
- actions gap: `Space.SM`.

## 11.4 States

- pending;
- processing/loading action: preserve card size and disable duplicate actions;
- resolved: should normally leave the pending list rather than remain visually pending;
- failed action: caller may surface Snackbar or concise inline error; Pattern does not invent business copy;
- disabled: actions disabled, content remains readable.

## 11.5 Interaction

- actions are explicit;
- destructive/cancel action requiring confirmation opens generic Dialog through caller-managed state;
- Pattern itself never calls cancellation use cases.

## 11.6 RTL

- title/value start right;
- actions follow RTL order while keeping primary action visually dominant;
- numeric amount locally LTR.

## 11.7 Use in

- Balance pending withdrawals;
- future approval/pending request flows with the same interaction shape.

## 11.8 Do not use when

- status is informational only with no attention/action need: Transaction Row or Alert Card alone;
- request requires a long form: use a screen or Bottom Sheet flow.

---

# 12. Settings Group Pattern

## 12.1 Function

Creates a coherent section of related settings/profile information and actions.

## 12.2 Composition

```text
Section Header
Base Card
└── Settings Row × N
    └── Divider between rows when needed
```

Optional section-level action may live in Section Header when appropriate.

## 12.3 Spacing

- group title → card: `Space.SM`/`Space.MD`;
- between groups: `Space.2XL`;
- row internal spacing is owned by Settings Row/List Row;
- no extra Divider after final row.

## 12.4 States

- read-only group;
- editable group via row-level actions;
- disabled row(s);
- group-level loading should keep heading and use Loading State inside the content area if needed;
- empty group should be omitted only if the product model says it is not applicable; otherwise show explanatory content.

## 12.5 Interaction

- section header action may open edit flow;
- row click/action callbacks are caller-owned;
- no ViewModel inside Pattern.

## 12.6 RTL

- Section Header and rows are RTL;
- values such as phone numbers can be local LTR.

## 12.7 Use in

- Settings/Profile: account data, workshop data, app information;
- About/info menus where content is genuinely a grouped menu.

---

# 13. Settings Row Pattern

## 13.1 Function

Represents one profile setting, value, navigation action, toggle-like destination, or account action using the generic List Row vocabulary.

## 13.2 Composition

```text
List Row
├── optional leading icon/avatar
├── label
├── optional supporting value/description
└── trailing
    ├── value
    ├── Status Chip
    ├── or auto-mirrored chevron / Icon Button
```

## 13.3 Variants

- value row;
- navigation row;
- editable row;
- destructive row;
- status row.

These are semantic variants of the Pattern, not separate components.

## 13.4 States

- default;
- pressed;
- disabled;
- destructive uses `Status.Error` semantically but does not glow;
- selected state is used only where a settings choice is actually selected.

## 13.5 Interaction

- one primary row action;
- icon action only when it represents a distinct secondary action;
- destructive operations requiring confirmation open a generic confirmation Dialog supplied by caller state.

## 13.6 RTL

- icon/label at visual start/right;
- value/chevron/action at visual end/left;
- directional chevron uses AutoMirrored form.

## 13.7 Use in

- Profile/Settings;
- About app navigation menu;
- other simple information/action menus.

## 13.8 Do not use when

- content is a financial transaction;
- content contains multiple prominent actions;
- complex form editing is embedded directly into the row.

---

# 14. Report Stat Tile Pattern

## 14.1 Function

Represents one report category/metric as a compact navigable tile.

This is more action-oriented than a passive Metric Card and less dominant than Dashboard Hero.

## 14.2 Composition

```text
Metric Card
├── optional semantic icon
├── label
├── Stat Value (Small/Medium)
├── optional supporting metadata
└── optional navigation affordance
```

## 14.3 Layout

- normally used in a 2-column report grid;
- equal-height tiles per row where possible;
- critical content must fit without relying on icon alone;
- exact grid width/breakpoint belongs to Session 05.

## 14.4 States

- default;
- pressed;
- selected only if the screen truly keeps a selected report filter;
- unavailable: content stays visible with disabled semantics if destination cannot be opened;
- loading value: preserve tile size.

## 14.5 Interaction

- whole tile is the primary target;
- caller supplies destination callback;
- no nested button inside a clickable tile.

## 14.6 RTL

- label and icon compose RTL;
- numeric value stays LTR internally;
- navigation affordance mirrors directionally.

## 14.7 Use in

- Reports dashboard;
- Commission report summary navigation;
- other report hubs with metric-driven destinations.

## 14.8 Do not use when

- metric has no navigation and is part of a simple summary: Metric Summary;
- tile requires several actions: use another screen-specific Pattern later if verified.

---

# 15. Media Action Group Pattern

## 15.1 Function

Unifies the repeated visual composition for camera, gallery, voice recording, pending media preview, and send readiness used by New Chat and Chat composer.

Recorder/camera/gallery logic remains entirely feature-owned.

## 15.2 Composition

Base idle form:

```text
media action row
├── Icon Button / compact semantic action: Camera
├── Icon Button / compact semantic action: Gallery
└── Icon Button / compact semantic action: Voice
```

Pending media form:

```text
Base Card / compact preview surface
├── media type icon
├── filename/status/short label
└── remove Icon Button
```

Recording form:

```text
Alert Card or compact status surface
├── Status Indicator (Error/live recording semantic)
├── recording timer
└── stop action
```

The overall New Chat container type remains unresolved until Session 05.

## 15.3 Visual rules

- Material Rounded icons replace emoji controls;
- WhatsApp color is not reused;
- camera/gallery/voice may use distinct approved semantic accents only when meaning is stable and documented;
- recording uses `Status.Error` + textual/timer signal, not color alone;
- no Pattern-owned raw color.

## 15.4 Spacing

- action gap: `Space.SM`/`Space.MD`;
- preview padding: component-owned + approved spacing;
- preview/recording block to composer/action row: `Space.MD`.

## 15.5 States

- idle;
- media selected;
- recording;
- recording stopped/pending voice;
- permission unavailable: caller opens generic permission rationale Dialog or OS permission flow;
- busy/loading send state: send action becomes loading/disabled without changing Pattern size;
- media error: caller supplies error feedback.

## 15.6 Interaction

Pattern exposes intents such as:

```text
onCamera
onGallery
onStartVoice
onStopVoice
onRemoveMedia
```

It does not:

- request Android permissions;
- create files;
- open launchers;
- record audio;
- upload media;
- send messages.

## 15.7 RTL

- action row follows RTL order chosen by Screen Spec;
- timer is local LTR;
- media technical identifiers remain local LTR where necessary.

## 15.8 Use in

- New Chat;
- Chat composer.

---

# 16. Search Results List Pattern

## 16.1 Function

Defines the repeated composition of a search field, result body, and loading/empty/error behavior.

## 16.2 Composition

```text
Search Field
Space.MD / Space.LG
result body
├── result item × N
├── Loading State
├── Empty State
└── concise recoverable error state
```

The result item may be `Conversation Item`, `Settings Row`, or another approved row Pattern depending on destination semantics.

## 16.3 States

- idle before query;
- query entered;
- results;
- no results;
- searching/loading;
- recoverable error.

The Pattern must distinguish:

```text
No content exists
≠
No search results match
```

Copy remains feature-owned.

## 16.4 Scroll behavior

- Search Field placement is defined by Screen Spec: fixed/sticky or scrolling with content;
- result list is the scroll owner when the search composition occupies the main body;
- do not nest an independently scrolling result list inside another vertical scroll without explicit Screen Spec justification.

## 16.5 Interaction

- query state and debounce/search execution are feature-owned;
- clear action is exposed from Search Field;
- item click callback is caller-owned;
- Pattern never performs filtering itself unless operating on already-supplied presentation data in a future explicit API design.

## 16.6 RTL

- Arabic query field is RTL;
- technical/phone query fragments may remain local LTR;
- result rows preserve their own RTL contracts.

## 16.7 Use in

- Conversations;
- FAQ search;
- future searchable lists.

---

# 17. Empty Screen Pattern

## 17.1 Function

Represents a complete destination/body whose valid content set is empty.

It composes the `Empty State` component with screen structure rather than replacing the whole application shell.

## 17.2 Composition

```text
Screen Header (when destination uses one)
body container
└── Empty State
    ├── icon
    ├── title
    ├── body
    └── optional CTA
Bottom Navigation (if root destination uses it)
```

## 17.3 Layout

- Empty State is centered in the available content region for standalone emptiness;
- keep screen header and bottom navigation visible where they normally belong;
- do not vertically center relative to the physical screen if bars/header consume part of the viewport; center within remaining body.

## 17.4 States and interaction

- one optional primary or secondary CTA;
- no emoji as default reusable icon;
- empty state is not an error;
- no automatic retry loop.

## 17.5 RTL

- centered standalone copy may be center-aligned;
- CTA/copy order remains logical in Arabic;
- directional CTA icon mirrors if used.

## 17.6 Use in

- empty Conversations;
- no transactions;
- no notifications;
- empty report list/history;
- other valid no-data destinations.

---

# 18. Error Screen Pattern

## 18.1 Function

Represents a complete recoverable destination/body failure using existing components without introducing a separate Error component in Session 03.

## 18.2 Composition

```text
Screen Header (when applicable)
body container
├── Status/Error semantic icon or Status Indicator
├── title
├── concise explanatory body
└── optional action row
    ├── Primary Button: Retry
    └── optional Text/Secondary Button: alternative action
Bottom Navigation (for root destinations, when appropriate)
```

A Base/Alert Card may contain the error content if the screen's visual hierarchy requires a surfaced block; the default full-body variant does not require a card.

## 18.3 Visual rules

- `Status.Error` communicates error emphasis;
- no error glow by default;
- error is not communicated by color alone: icon + text required;
- long diagnostics, stack traces, HTTP codes, local-storage details, or developer messages must never be shown as user-facing default copy.

## 18.4 States

- recoverable error with Retry;
- unrecoverable-for-now error with safe alternative action;
- retry loading transitions to Loading State/Screen while preserving shell;
- cached/partial content behavior is product-owned and must be defined in Screen Specs if needed.

## 18.5 Interaction

- Retry callback supplied by caller;
- Pattern does not execute network/database work;
- avoid multiple equal-priority actions.

## 18.6 RTL

- text is Arabic RTL/start-aligned unless standalone centered composition is chosen;
- icon is non-directional unless it represents navigation.

## 18.7 Use in

- any data-backed screen whose primary content cannot be loaded and cannot be meaningfully shown.

---

# 19. Loading Screen Pattern

## 19.1 Function

Represents the initial loading state of a complete destination/body while preserving structural navigation.

## 19.2 Composition

```text
Screen Header (when applicable)
body container
└── Loading State.Content
Bottom Navigation (for root destinations, when applicable)
```

## 19.3 Layout

- centered within available body;
- header/nav remain stable;
- no screen-size jump when loaded content appears;
- exact skeleton strategy is not introduced in V1 unless Session 05 proves a need.

## 19.4 States

- initial loading;
- refresh loading is not this Pattern: existing content should remain visible where product behavior supports pull-to-refresh;
- action/button loading remains component-level;
- long-running background sync must not hijack the whole screen with this Pattern if usable content exists.

## 19.5 Interaction

- usually non-interactive;
- cancellation only if a specific flow requires it and Screen Specs define it;
- no fake percentage.

## 19.6 RTL

- label is Arabic RTL/centered as chosen by Loading State component;
- numeric progress is shown only if real progress exists.

## 19.7 Use in

- initial loads for Home, Conversations, Reports, Balance, Settings, supporting data destinations when content is unavailable until load completes.

---

# 20. Pattern-to-screen matrix

The following matrix records where each Pattern is expected from the current code audit. Session 05 will decide the exact final composition of the six major redesigned surfaces.

| Pattern | Home | Conversations | New Chat | Reports | Balance | Settings | Supporting screens |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| Screen Header | ✓ | ✓ | — | ✓ | ✓ | ✓ | ✓ |
| Dashboard Hero | ✓ | — | — | optional | ✓ | — | competition |
| Metric Summary | optional | — | — | ✓ | optional | — | commission report |
| Conversation Item | — | ✓ | — | — | — | — | — |
| Transaction Row | — | — | — | optional | ✓ | — | invoices/weekly history |
| Pending Request Card | — | — | — | — | ✓ | — | future pending flows |
| Settings Group | — | — | — | — | — | ✓ | About/info where appropriate |
| Settings Row | — | — | — | — | — | ✓ | info menus |
| Report Stat Tile | — | — | — | ✓ | optional entry | — | commission report |
| Media Action Group | — | — | ✓ | — | — | — | Chat composer |
| Search Results List | — | ✓ | — | — | — | — | FAQ |
| Empty Screen | possible | ✓ | — | list states | ✓ | possible | ✓ |
| Error Screen | ✓ | ✓ | possible | ✓ | ✓ | ✓ | ✓ |
| Loading Screen | ✓ | ✓ | possible | ✓ | ✓ | ✓ | ✓ |

`optional` means the current information architecture contains related content, but Session 05 must confirm whether that Pattern is actually used in the final redesigned screen.

---

# 21. Pattern boundaries: what stays outside Design System

The following are explicitly **not** Pattern responsibilities:

## Home

- pump action calculation;
- counter/timer business behavior;
- weekly competition data logic;
- AI/Dynamo message generation;
- notification unread source;
- refresh execution.

## Conversations / Chat

- conversation queries;
- message sending;
- unread mutation;
- recorder lifecycle;
- camera/gallery launchers;
- permissions;
- media files/uploads;
- retry workers.

## Reports / Balance

- commission calculation;
- transaction formatting rules beyond presentation-ready text/value;
- withdrawal validation/cancellation execution;
- invoice filtering;
- database/network state.

## Settings/Profile

- profile persistence;
- sign-out execution;
- registration/business validation;
- repository ownership.

Patterns receive presentation-ready state and callbacks only.

---

# 22. Pattern state policy

To prevent each screen from inventing a different state grammar:

```text
Initial content unavailable + request running  → Loading Screen
Initial content unavailable + request failed   → Error Screen
Request succeeded but collection is empty      → Empty Screen
Usable content exists + refresh running         → keep content; refresh indicator belongs to screen/container
One action is running                           → component-level Loading state
One section failed while other content works    → section-local Alert/feedback, not whole Error Screen
```

This is a presentation policy only. The feature still determines the actual state.

---

# 23. Pattern interaction policy

Patterns expose semantic user intents, not navigation or use-case calls.

Examples:

```text
ScreenHeader       → onBack, onTrailingAction
DashboardHero      → onPrimaryAction, onSecondaryAction
ConversationItem   → onOpen
TransactionRow     → onOpenDetails
PendingRequestCard → onPrimaryAction, onSecondaryAction
SettingsRow        → onClick, optional onSecondaryAction
ReportStatTile     → onOpen
MediaActionGroup   → onCamera/onGallery/onStartVoice/onStopVoice/onRemoveMedia
SearchResultsList  → onQueryChange/onClear/onResultClick
ErrorScreen        → onRetry/onAlternativeAction
EmptyScreen        → onPrimaryAction
```

Exact Kotlin APIs are deferred to Sessions 06/07.

---

# 24. RTL rules at Pattern level

1. Pattern containers inherit the authoritative RTL application direction.
2. Leading content means visual right; trailing means visual left.
3. Back/forward/chevron use AutoMirrored directional icons.
4. Amounts, phone numbers, OTPs, technical identifiers, timers, and instrument displays may use local LTR.
5. Do not force an entire Pattern to LTR because one child contains numbers.
6. Two-column metric/report ordering follows the final RTL Screen Specification.
7. Action priority is semantic; do not reverse primary/secondary importance merely to imitate an LTR layout screenshot.

---

# 25. Accessibility rules at Pattern level

- no Pattern introduces a touch target below 48dp;
- critical state uses text/icon/shape in addition to color;
- a whole clickable tile/row must expose one coherent semantic action;
- avoid nested click targets unless secondary action is genuinely separate;
- loading does not leave duplicate enabled actions;
- dynamic badges/status values require accessible labels;
- text truncation must preserve the primary meaning of a row;
- full-screen state copy must remain feature-owned and concise.

---

# 26. Current local compositions mapped to V1 Patterns

| Current/local composition | V1 target |
|---|---|
| Home manual greeting row + bell | Screen Header |
| Pump hero composition | Dashboard Hero + feature-owned instrument content |
| Reports local metric grid | Metric Summary + Report Stat Tile |
| `ConversationRow` | Conversation Item |
| Balance transaction rows | Transaction Row |
| Balance pending withdrawal rows/cards | Pending Request Card |
| Profile grouped cards | Settings Group |
| `ProfileRow` / info menu items | Settings Row |
| New Chat media buttons + recording preview | Media Action Group |
| Conversations/FAQ custom search + list | Search Results List |
| feature-local centered empty UI | Empty Screen |
| feature-local full content error UI | Error Screen |
| feature-local full content initial progress | Loading Screen |

No production code is changed in this session; this table defines the future consolidation target.

---

# 27. Patterns intentionally not admitted to V1 yet

The audit identified additional reusable-looking structures, but they are not promoted to V1 Patterns until Screen Specs prove repeated need:

- generic Detail/Data Row;
- FAQ accordion/content-card Pattern;
- legal-content Pattern;
- authentication form Pattern;
- onboarding selectable-card Pattern;
- chat bubble Pattern;
- notification row Pattern;
- ranking/competition row Pattern;
- chart Pattern;
- skeleton Pattern.

Reason: Session 04 follows the approved Session Plan's initial Pattern set and avoids prematurely turning every local composition into a global abstraction.

If Session 05 proves one of these is required for the six target screens, it may be added only as a documented Pattern extension without changing Foundations.

---

# 28. Decisions

- The Session Plan's 14 initial UI Patterns are now formally specified.
- Screen Header unifies root and nested top structures through existing Top Header/Back Header components.
- Dashboard Hero is reserved for one dominant branded focus, not general cards.
- Metric Summary and Report Stat Tile are separate: passive grouped metrics vs action-oriented report destinations.
- Conversation, financial, pending-request, and settings rows remain separate Patterns while reusing List Row primitives.
- Media Action Group owns presentation only; permissions/recording/media logic remain in Chat feature.
- Search Results List defines search/result state composition but does not own query execution.
- Empty, Error, and Loading Screens preserve application shell/navigation where applicable.
- Full-screen state policy is standardized for initial load/error/empty cases.
- No additional Pattern is admitted prematurely merely because one local implementation exists.

---

# 29. Forbidden from this point forward

1. No feature may create a competing local version of one of these Patterns during migration.
2. No Pattern may call `hiltViewModel()` or own application/domain state sources.
3. No Pattern may navigate directly.
4. No Pattern may introduce raw visual constants or new foundation tokens.
5. No Pattern may expose arbitrary color/radius/border/spacing styling escape hatches.
6. No generic Pattern may embed feature-specific Arabic copy.
7. Media Action Group may not own Android permissions, media capture, recording, upload, or sending.
8. Search Results List may not own repository search execution.
9. Error Screen may not display developer diagnostics as user-facing default text.
10. Loading Screen may not replace usable content during ordinary background refresh.
11. Error/Warning states must not use glow as their primary signal.
12. No screen migration or Kotlin implementation occurs in Session 04.

---

# 30. Deferred

- exact Screen compositions and responsive breakpoints;
- final New Chat container: Dialog vs Bottom Sheet vs full screen;
- exact Home Dashboard Hero composition against approved visual reference;
- exact Reports grid structure;
- exact Balance hero/summary split;
- exact Settings group ordering;
- pattern Kotlin APIs;
- package ownership and architecture;
- implementation/previews;
- screen migration;
- pixel-level visual QA;
- optional additional Patterns proven by Session 05.

---

# 31. Open Issues

```text
- New Chat container remains unresolved and must be decided in Session 05.
- Exact responsive behavior for metric/report grids must be decided per screen in Session 05.
- Home feature-owned instrument/hero subcomposition requires Screen 05 and Architecture 06 decisions.
- Whether Reports needs a chart remains unresolved; no chart Pattern is admitted yet.
- Whether supporting screens justify Detail/Data Row or other new Patterns is deferred beyond the six primary Screen Specs unless directly required.
```

None of these block Session 05.

---

# 32. Next Session Input

Session 05 must use:

```text
02_FOUNDATIONS.md
        ↓
03_COMPONENT_SPEC.md
        ↓
04_PATTERNS.md
        ↓
05_SCREEN_SPECS.md
```

The six primary screens must be specified in this order from the Session Plan:

1. Home.
2. Conversations.
3. New Chat.
4. Reports.
5. Balance.
6. Settings.

For each screen Session 05 must decide:

- structure;
- section order;
- which approved Components and Patterns are used;
- spacing;
- normal/loading/empty/error behavior;
- scroll ownership;
- Bottom Navigation behavior;
- responsive behavior;
- RTL behavior;
- unresolved screen-specific decisions.

No screen may invent a local visual primitive when an approved Component or Pattern already covers it.

---

# 33. Session close

```text
STATUS: APPROVED

Decisions:
- 14 V1 UI Patterns are specified and mapped to actual current screens.
- Pattern state policy for loading/error/empty is standardized.
- Pattern ownership stops at presentation-ready state + callbacks.
- Media, search, navigation, and business execution remain outside Design System.
- Additional abstractions are deferred until Screen Specs prove repeated need.

Forbidden:
- No feature-specific business logic/state sources/navigation inside Patterns.
- No competing local Pattern during later migration.
- No new visual foundations or unmanaged constants.
- No Kotlin implementation or screen migration in Session 04.

Deferred:
- Screen composition, responsive details, New Chat container decision, architecture, implementation, migration, QA.

Open Issues:
- New Chat container, metric-grid responsiveness, Home hero ownership details, chart need, optional additional supporting patterns.

Next Session Input:
- 04_PATTERNS.md becomes the authoritative Pattern source for Session 05 once accepted by continuing to Session 05.
```

**Approval gate:** do not begin Session 05 until the user explicitly approves this document by continuing to Session 05 or otherwise accepting it.
