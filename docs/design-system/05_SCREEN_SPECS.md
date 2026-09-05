# AutoDrive Design System — Session 05: Screen Specifications

**Output:** `05_SCREEN_SPECS.md`  
**Session:** 05 — Screen Specifications  
**Input source of truth:** `04_PATTERNS.md` + approved `03_COMPONENT_SPEC.md` + approved `02_FOUNDATIONS.md`  
**Implementation changes:** None  
**Production code changes:** None  
**STATUS:** APPROVED

---

# 1. Purpose

This document converts the approved AutoDrive visual direction, Foundations, Components, and UI Patterns into executable screen-level contracts for the six primary product surfaces:

1. Home.
2. Conversations.
3. New Chat.
4. Reports.
5. Balance.
6. Settings.

The screen contracts define hierarchy, composition, state behavior, scrolling, navigation shell behavior, responsiveness, and RTL. They do not implement Kotlin or change business behavior.

The V1 dependency chain is now:

```text
01 Design Audit
      ↓
02 Foundations
      ↓
03 Components
      ↓
04 Patterns
      ↓
05 Screen Specifications
      ↓
06 Design System Architecture
```

---

# 2. Source-of-truth gate

Session 04 states that explicitly continuing to Session 05 approves `04_PATTERNS.md`. The user requested execution of Session 05, therefore Session 04 is approved and authoritative.

Screen Specifications may resolve screen-level decisions deferred by Session 04, but may not redefine Foundations, Components, or Pattern contracts.

---

# 3. Reference priority

When implementation details conflict, use this priority:

```text
Approved redesign intent / approved visual reference
        ↓
05_SCREEN_SPECS.md
        ↓
04_PATTERNS.md
        ↓
03_COMPONENT_SPEC.md
        ↓
02_FOUNDATIONS.md
        ↓
Current production UI behavior/content
```

Rules:

- Current screen content and business interactions are preserved unless this document explicitly changes presentation structure.
- Current local styling is never a source of truth when it conflicts with the approved Design System.
- `home.png` remains the strongest concrete visual reference currently present inside the project archive.
- Existing secondary-screen screenshots/code are behavioral/content evidence, not visual authority.

---

# 4. Current-code screen mapping

The product names used by the Design System map to current code as follows:

| Product screen | Current implementation target |
|---|---|
| Home | `app/.../feature/home/presentation/HomeScreen.kt` |
| Conversations | `app/.../feature/reports/presentation/recent/RecentActivityScreen.kt` |
| New Chat | `feature/chat/.../presentation/NewChatDialog.kt` |
| Reports | `app/.../feature/reports/presentation/log/ActivityLogScreen.kt` |
| Balance | `feature/balance/.../presentation/BalanceScreen.kt` |
| Settings | `feature/profile/.../presentation/ProfileScreen.kt` |

The naming mismatch for Conversations/Reports is an implementation detail. Session 05 does not rename packages or routes.

---

# 5. Global shell contract

## 5.1 Root destinations

The four root destinations are:

```text
Home
Conversations
Reports
Settings
```

They use the shared Bottom Navigation with the center New Chat FAB.

### Bottom Navigation rules

- fixed outside scrollable screen content;
- never scrolls with body content;
- current root destination is visibly selected;
- center FAB remains visually dominant and launches New Chat;
- unread badges are passed from shell/application state, never read by the visual component itself;
- screen content receives bottom inset/padding so the final item is never obscured.

## 5.2 Child destinations

Balance is a child destination and therefore:

- uses `Back Header`;
- does not show the root Bottom Navigation;
- does not show the center FAB.

## 5.3 Modal destination

New Chat is locked as a **modal Dialog composition**, not a route, full-screen destination, or Bottom Sheet in V1.

Reason:

- it is launched as a transient create action from every root destination;
- the current and approved interaction model is one initial message with optional media;
- preserving the current destination behind the modal prevents unnecessary navigation state changes;
- the Dialog contract already exists in Session 03 and Media Action Group exists in Session 04.

The underlying root shell remains visually present but dimmed and non-interactive while the Dialog is open.

---

# 6. Global screen geometry

## 6.1 Compact width — primary Android phone target

For widths below 600dp:

```text
Horizontal screen gutter: Space.LG = 16dp
Major section gap:        Space.2XL = 24dp
Compact section gap:      Space.LG = 16dp
Dense internal gap:       Space.SM / Space.MD = 8 / 12dp
```

A component keeps its own internal geometry from Session 03.

## 6.2 Medium width

For widths from 600dp to 839dp:

- body horizontal gutter may increase to `Space.2XL` = 24dp;
- single-column content may be centered with a sensible maximum content width;
- Reports may use wider two-column grids without changing semantic order;
- Conversations remains one readable list column;
- Settings keeps one main reading column rather than stretching rows edge-to-edge.

## 6.3 Expanded width

At 840dp and above:

- V1 does **not** introduce a navigation rail or desktop-specific information architecture;
- root Bottom Navigation remains the V1 navigation contract;
- body content is centered and width-constrained;
- Reports may distribute tiles into additional columns only if semantic reading order remains stable;
- Dialog width is constrained and never expands to the full viewport.

Responsive implementation technology is deferred to Session 06/07. The behavior above is authoritative.

---

# 7. Global vertical behavior

- Do not force a complex screen to fit one viewport by shrinking cards below their intended hierarchy.
- Short-height phones must scroll rather than compress major content.
- Root Bottom Navigation remains fixed.
- Header fixation is screen-specific below.
- Keyboard/IME must not cover editable fields or primary actions.
- Pull-to-refresh, where business behavior already exists, remains a feature behavior and must preserve stable screen structure.

---

# 8. Global state policy

## Initial loading

Use `Loading Screen` or structure-preserving loading only when there is no usable content yet.

## Background refresh

- keep current usable content visible;
- show a refresh indicator only where the existing behavior requires it;
- never replace the whole screen with Loading Screen during ordinary refresh.

## Empty

Use Empty Screen only when the absence of content is a valid complete screen state.

Zero financial values are data, not an empty state.

## Error

- if no usable data exists, use Error Screen inside the persistent shell/header rules of that screen;
- if usable stale/current data exists, retain it and surface recoverable feedback without destroying the screen;
- developer diagnostics are forbidden as user copy.

---

# 9. HOME — Screen Specification

## 9.1 Role

Home is the branded primary dashboard and strongest expression of the AutoDrive visual language.

Current product hierarchy is preserved:

```text
Greeting / identity
Fuel / pump hero
Weekly competition teaser
Dynamo / insight content
Bottom Navigation
```

## 9.2 Structure

```text
Scaffold / Surface.Canvas
├── Scrollable body
│   ├── Screen Header
│   │   ├── Greeting + first name
│   │   ├── Short supporting line
│   │   └── Notification Icon Button + optional Badge
│   ├── Dashboard Hero
│   │   └── feature-owned pump/instrument composition
│   ├── Weekly Competition highlight card
│   └── Dynamo / insight highlight card
└── Bottom Navigation
    └── center New Chat FAB
```

## 9.3 Header

Use `Screen Header` with the root `Top Header` form.

Required content:

- dynamic greeting;
- first name emphasized with approved brand emphasis;
- short supporting line beneath greeting as seen in the approved visual direction;
- notification action with unread Badge when count > 0.

Rules:

- no back action;
- maximum one direct notification action in this screen;
- notification badge data is supplied by feature/shell state;
- header remains part of the body composition, not a visually heavy Material toolbar.

## 9.4 Dashboard Hero

Use `Dashboard Hero`.

Required hierarchy:

1. instrument context/label;
2. dominant odometer/pump numeric display;
3. unit/context;
4. supporting countdown metrics;
5. fuel gauge/status visualization;
6. pump action.

The existing seven-segment/instrument display remains feature-owned content inserted into the approved Pattern.

### Hero constraints

- one dominant numeric focus only;
- special gauge/LED colors come only from Session 02 special instrument palette;
- local LTR is allowed only inside pure numeric/instrument islands;
- labels remain RTL;
- pump animation/business timer remain in Home feature, never in Design System.

## 9.5 Weekly Competition section

Use a `Highlight Card` composition consistent with the approved competition teaser.

Content:

- competition identity/title;
- concise support copy;
- clear directional affordance/action;
- feature-owned crown/competition visual where approved.

Interaction opens the existing competition destination.

## 9.6 Dynamo / insight section

Use a visually subordinate Highlight/Base Card composition.

Content:

- title/identity;
- short insight text;
- optional feature-owned visual.

It must not visually compete with the Dashboard Hero.

## 9.7 Spacing

Compact target:

```text
Header → Hero:          Space.XL / Space.2XL
Hero → Competition:     Space.2XL
Competition → Insight:  Space.LG / Space.2XL
Body horizontal gutter: Space.LG
Body bottom padding:    Space.2XL + nav-safe inset
```

Use only approved spacing tokens.

## 9.8 Scroll behavior

- body scrolls vertically as one composition;
- do not use `weight(1f)` to force the insight card to fill leftover height as a permanent layout rule;
- on a normal-height phone the composition may visually fit most/all content;
- on short devices it must scroll naturally;
- Bottom Navigation stays fixed.

## 9.9 Loading

Initial loading keeps:

- Header shell;
- Dashboard Hero geometry;
- competition/insight geometry where feasible;
- Bottom Navigation.

Do not show a lone centered spinner on an otherwise empty canvas if the shell can be retained.

## 9.10 Empty

Home has no global empty state.

If a specific remote card has no content:

- keep the screen structure;
- render that card's feature-appropriate unavailable/empty content;
- do not collapse the entire Home screen.

## 9.11 Error

- preserve cached/usable hero content when feature logic provides it;
- remote insight failure must not replace the full screen;
- complete initial data failure may use Error Screen beneath the Header while keeping Bottom Navigation available if navigation is safe.

## 9.12 Bottom Navigation

- visible;
- Home selected;
- center FAB launches New Chat Dialog;
- Messages unread Badge may appear if supplied by shell state.

## 9.13 RTL

- screen composition RTL;
- greeting starts on visual right;
- notification action is visual end/left;
- pump/gauge numeric islands may be local LTR;
- all mixed Arabic labels retain logical start/end spacing.

## 9.14 Responsive behavior

- hero stays one dominant block on phones and tablets;
- never split the pump instrument and its gauge into unrelated cards merely to fill width;
- on wider devices, constrain hero width and optionally place subordinate Home cards more efficiently only if the approved hierarchy remains unchanged;
- no side navigation in V1.

---

# 10. CONVERSATIONS — Screen Specification

## 10.1 Role

Primary list of conversations with search, unread state, and direct opening of a thread.

## 10.2 Structure

```text
Scaffold / Surface.Canvas
├── Fixed top composition
│   ├── Screen Header / Top Header: "المحادثات"
│   └── Search Field
├── Content region
│   └── Search Results List
│       ├── Conversation Item
│       ├── Divider
│       └── ...
└── Bottom Navigation
    └── center New Chat FAB
```

## 10.3 Header

Use `Screen Header` + `Top Header`.

- title: `المحادثات`;
- no back action;
- no redundant create action in the header because the center FAB is the global create affordance;
- search sits immediately below header as part of the fixed top composition.

## 10.4 Search

Use approved `Search Field` inside `Search Results List`.

Behavior preserved:

- query updates results;
- clear action appears only when query is non-empty;
- search remains visible while list scrolls;
- no local OutlinedTextField styling.

## 10.5 Conversation Item

Use `Conversation Item` Pattern.

Required content:

- Avatar/brand identity at visual start/right;
- title;
- latest-message preview;
- time/date;
- unread Badge if count > 0;
- optional state indicator only when semantically required.

Rules:

- one whole row is the click target;
- preview truncates before title meaning is lost;
- unread state must use more than raw text color alone;
- Divider aligns after the avatar region according to Pattern geometry.

## 10.6 Spacing

```text
Header horizontal gutter:  Space.LG
Header → Search:            Space.SM / Space.MD
Search → list:              Space.SM / Space.LG
List row spacing:           owned by Conversation Item
Bottom list padding:        Space.2XL + nav-safe inset
```

## 10.7 Scroll behavior

- header + search remain fixed;
- conversation list owns vertical scrolling;
- pull-to-refresh may wrap the list region while preserving header/search;
- query position/state should survive ordinary root-tab navigation where current architecture allows it.

## 10.8 Loading

Initial loading:

- Header + Search remain visible;
- content region uses Loading Screen/structured loading;
- Bottom Navigation remains visible.

Background refresh:

- keep current list visible;
- show refresh progress without clearing rows.

## 10.9 Empty

Two distinct states are mandatory.

### No conversations

Use Empty Screen/content-region form:

- no-conversation icon from governed icon set, not decorative emoji;
- title: no conversations;
- short support copy;
- primary action: start New Chat.

### No search results

Use search-empty state:

- title: no results;
- concise suggestion to change query;
- no create CTA unless product behavior specifically requires it;
- Search Field remains visible with current query.

## 10.10 Error

If no usable list exists:

- keep Header/Search/Bottom Navigation;
- Error Screen occupies results region;
- retry callback comes from feature.

If a refresh fails while rows exist:

- retain rows;
- use governed Snackbar/error feedback;
- do not replace list.

## 10.11 Bottom Navigation

- visible;
- Messages/Conversations selected;
- center FAB opens New Chat Dialog;
- unread count may remain visible as shell state.

## 10.12 RTL

- row title/preview start on visual right;
- timestamp and unread Badge occupy visual end/left;
- pure numeric unread count may use local LTR;
- directional icons are AutoMirrored where applicable.

## 10.13 Responsive behavior

- keep a single readable list column;
- on wider devices center/constrain list width rather than stretching message previews excessively;
- no master-detail layout in V1.

---

# 11. NEW CHAT — Screen/Modal Specification

## 11.1 Container decision

**V1 decision: Modal Dialog.**

This resolves the Session 01/04 open issue.

It remains launched from the center FAB and overlays the current root destination.

It is **not**:

- a full-screen route;
- a Bottom Sheet;
- a child screen with Back Header;
- a destination with its own Bottom Navigation.

## 11.2 Structure

```text
Dialog
└── modal Surface
    ├── title: "محادثة جديدة"
    ├── Text Field / composer
    ├── optional selected-media preview
    ├── optional recording state
    ├── Media Action Group
    │   ├── Voice
    │   ├── Gallery
    │   └── Camera
    ├── inline validation/error region when required
    └── action row
        ├── Primary Button: Send
        └── Text/Secondary Button: Cancel
```

## 11.3 Dialog geometry

- uses approved Dialog component;
- radius and border from Session 03;
- compact width respects screen side safe space;
- maximum width is constrained on tablets;
- content may scroll inside the Dialog if height is insufficient;
- IME-safe: composer and Send action cannot be covered by keyboard.

## 11.4 Composer

Use approved Text Field contract with multi-line behavior.

- placeholder remains concise;
- min/max line behavior may remain feature-owned within the approved field API;
- sending an empty text is allowed only when valid media is attached, matching existing behavior;
- draft content remains feature state, not Design System state.

## 11.5 Media Action Group

Use `Media Action Group` Pattern.

Required actions:

1. voice;
2. gallery;
3. camera.

Visual rules:

- equal visual weight;
- governed icons, no emoji as action icons;
- minimum 48dp touch targets;
- selected/active recording state uses semantic status treatment;
- group owns presentation only.

Permissions, camera launch, gallery picker, recorder lifecycle, file creation, and media preparation remain Chat feature responsibilities.

## 11.6 Media preview

When media is ready:

- show one compact preview/status container;
- communicate media type with governed icon + text;
- provide remove action;
- removal does not dismiss the Dialog or erase unrelated draft text.

## 11.7 Recording state

While recording:

- Media Action Group switches to explicit recording presentation;
- use Status Indicator with deliberate pulse only because recording is a live semantic state;
- show elapsed duration;
- provide clear stop/cancel affordance according to feature behavior;
- do not use uncontrolled decorative animation.

## 11.8 Send state

Primary Button states:

- default enabled when draft is sendable;
- disabled when neither valid text nor media exists;
- loading while conversation/message creation is in progress;
- loading must prevent duplicate submission.

Cancel remains available unless feature submission has entered a non-interruptible state.

## 11.9 Loading

There is no full-screen Loading Screen.

Submission loading is local to the Primary Button/Dialog state.

## 11.10 Empty

No Empty Screen applies. An empty draft is the normal initial state.

## 11.11 Error

- validation error: inline near relevant content;
- recoverable send/media error: governed Snackbar or inline message without dismissing Dialog;
- retain user draft and prepared media where safely possible;
- never expose technical diagnostics.

## 11.12 Dismissal

- explicit Cancel dismisses;
- system back dismisses when safe;
- outside-tap dismissal may remain only if it cannot cause accidental loss without an accepted product rule;
- if non-empty draft-loss protection is later needed, it is product behavior outside DS and must use governed Dialog confirmation.

## 11.13 Bottom Navigation

The Dialog owns no Bottom Navigation.

Underlying root Bottom Navigation remains visible through scrim/dimming but cannot be interacted with until the Dialog closes.

## 11.14 RTL

- Dialog content RTL;
- title/composer align to visual right/start;
- media actions read in approved logical order in RTL;
- Send icon, if directional, uses AutoMirrored form;
- elapsed time may be a local LTR numeric island.

## 11.15 Responsive behavior

- compact phone: width fills available safe width without touching screen edges;
- short-height/keyboard-open: internal content scrolls;
- tablet: fixed/constrained modal width centered in viewport;
- never become a full-width tablet panel in V1.

---

# 12. REPORTS — Screen Specification

## 12.1 Role

Reports is a root dashboard summarizing financial/performance data and linking to deeper report destinations.

Information architecture is preserved:

```text
Total commissions
Balance
Pending
Invoices
Win/leader count
Weekly commissions
Weekly competition/history
```

## 12.2 Structure

```text
Scaffold / Surface.Canvas
├── Scrollable body
│   ├── Screen Header: "تقاريري"
│   ├── dominant total-commission summary
│   └── report grid / Metric Summary
│       ├── Balance tile
│       ├── Pending tile
│       ├── Invoice tile
│       ├── Win/leader tile
│       ├── Weekly commissions tile
│       └── Competition tile
└── Bottom Navigation
    └── center New Chat FAB
```

## 12.3 Header

Use `Screen Header` + root `Top Header`.

- title: `تقاريري`;
- no back action;
- no direct header actions required in V1.

## 12.4 Total commissions

Use `Metric Summary` with a visually dominant first metric or a single emphasized `Metric Card`/Highlight composition according to Session 04 hierarchy.

Required content:

- label indicating total commissions since join date;
- formatted amount;
- join-date context when available.

Rules:

- amount may use a strong `Stat Value` but must not use Home's `Instrument Number`;
- zero is displayed as a valid value;
- date/amount formatting remains feature-owned.

## 12.5 Report tiles

Use `Report Stat Tile` for navigable report destinations and Metric Card treatment for passive metrics.

### Navigable

- Balance → opens Balance;
- Invoices → invoice list/current-week behavior;
- Win/leader metric → win weeks/history;
- Weekly commissions → weekly commissions report;
- Weekly competition → competition/history destination.

### Passive

- Pending amount remains non-navigation unless product behavior later assigns a destination.

Each navigable tile must have a visible interaction affordance and full-tile click target.

## 12.6 Grid

Compact phone:

- dominant total summary spans full width;
- remaining tiles use a stable two-column grid where content fits;
- if a tile's text/value cannot fit accessibly, it spans full width rather than reducing typography below spec.

Medium/expanded:

- may use 2–3 columns according to available width;
- semantic order remains RTL reading order;
- tile heights align within each row where practical.

## 12.7 Spacing

```text
Header → total summary:      Space.XL / Space.2XL
Summary → report grid:       Space.LG / Space.2XL
Grid horizontal gap:         Space.SM / Space.MD
Grid vertical gap:           Space.SM / Space.MD
Body horizontal gutter:      Space.LG compact
Bottom padding:              Space.2XL + nav-safe inset
```

## 12.8 Scroll behavior

- body owns vertical scroll;
- do not use equal `weight(1f)` rows to force all report cards into one viewport;
- a normal phone may show most content without scrolling, but small/short devices must scroll;
- Bottom Navigation fixed.

## 12.9 Loading

Initial loading:

- Header remains;
- report region uses Loading Screen/structured loading;
- Bottom Navigation remains.

Background refresh retains current values.

## 12.10 Empty

Reports has no global Empty Screen for all-zero metrics.

All-zero metrics are valid report data and must render as `0`/formatted zero values.

## 12.11 Error

- if all report data is unavailable and no usable cached state exists: Error Screen beneath Header;
- if one metric fails independently and product state can distinguish it, that tile shows unavailable state without hiding valid metrics;
- do not silently convert failures to zero.

## 12.12 Bottom Navigation

- visible;
- Reports selected;
- center FAB opens New Chat Dialog.

## 12.13 RTL

- grid order follows RTL reading order;
- labels align start/right;
- pure amount/count runs are local LTR as needed;
- units/currency remain semantically associated with values;
- no screen-level forced LTR.

## 12.14 Responsive behavior

- preserve metric hierarchy, not pixel dimensions;
- increase columns only when tile minimum readability is preserved;
- body remains centered/constrained on tablets;
- no chart is introduced in V1 because Session 04 did not approve a chart Pattern.

---

# 13. BALANCE — Screen Specification

## 13.1 Role

Balance is a child financial destination for current balance, withdrawal action/state, report access, pending requests, and transaction history.

Hierarchy is preserved:

```text
Back Header + withdrawal action
Balance Hero
Commission report entry
Pending withdrawal section (when present)
Balance history
```

## 13.2 Structure

```text
Scaffold / Surface.Canvas
├── Fixed Back Header
│   ├── title: "رصيدي"
│   ├── back action
│   └── withdrawal action
└── Scrollable body
    ├── Dashboard Hero / Highlight balance hero
    ├── report-entry action
    ├── pending requests section (conditional)
    └── transaction/history section
```

No root Bottom Navigation.

## 13.3 Header

Use `Screen Header` with `Back Header`.

- AutoMirrored back icon;
- title: `رصيدي`;
- one trailing withdrawal action;
- withdrawal action disabled when balance/business state makes withdrawal unavailable;
- if balance refresh is in progress, preserve header geometry and use local action loading state rather than changing the whole screen.

## 13.4 Balance Hero

Use `Dashboard Hero` or Highlight Card composition with `Stat Value.Hero` for one dominant current balance value.

Required content:

- current available balance;
- pending-withdrawal amount/status if present;
- last updated context when supplied by feature.

Rules:

- one Hero value: available balance;
- pending amount is subordinate;
- financial semantics use approved financial/status colors, not arbitrary brand color;
- no local `60sp` override.

## 13.5 Commission report entry

Use approved Secondary Button / Report Stat Tile-like action according to Session 03/04 APIs.

- full-width on compact phone;
- clear label `تقرير عمولاتي`;
- opens existing commission report destination;
- do not create a one-off OutlinedButton style.

## 13.6 Pending requests

When pending withdrawals exist:

```text
Section Header
└── one or more Pending Request Card
```

Required information:

- amount;
- pending status;
- relevant submitted time/context when available;
- cancellation behavior where existing product logic permits it.

`إلغاء الكل` remains a feature action and must use approved action component styling.

Cancellation confirmation uses approved Dialog component.

## 13.7 History

Use `Section Header` + `Transaction Row` Pattern.

The unified history keeps current semantic entries:

- commission/balance transaction;
- pending withdrawal entry where feature state includes it;
- other existing financial history items.

Rows must distinguish direction/status using icon/status semantics plus text, not color alone.

## 13.8 Withdrawal flow

Use approved Bottom Sheet component for the existing withdrawal request flow.

The Bottom Sheet owns presentation only. Amount validation/submission/business rules remain feature-owned.

Submission feedback uses governed Snackbar.

## 13.9 Spacing

```text
Body horizontal gutter:      Space.LG compact
Header → Hero:               Space.XL / Space.2XL
Hero → report entry:         Space.LG
Report → pending/history:    Space.2XL
History row separation:      owned by Transaction Row/Divider
Bottom body padding:         Space.3XL + system nav inset
```

## 13.10 Scroll behavior

- Back Header fixed;
- body is one vertical scrolling region;
- pending section appears before history;
- Bottom Sheet is modal and independently scrollable when required.

## 13.11 Loading

Initial load:

- Back Header remains;
- body uses Loading Screen/structure-preserving hero loading;
- withdrawal action is disabled until safe balance state exists.

Background refresh retains current values.

## 13.12 Empty

Balance itself is not empty when amount is zero.

If there is no history:

- keep Hero and report entry visible;
- history section uses a local empty state: no balance movements yet;
- do not replace the entire screen with Empty Screen.

## 13.13 Error

- no usable balance/history: Error Screen beneath Back Header with retry;
- stale usable balance: keep content and surface refresh failure through governed feedback;
- submission error stays in withdrawal flow and never clears main screen data.

## 13.14 Bottom Navigation

Hidden because Balance is a child destination.

## 13.15 RTL

- Back Header icon AutoMirrored;
- labels RTL;
- amounts local LTR where needed;
- financial signs/direction must remain visually unambiguous;
- status chips remain readable without relying on position alone.

## 13.16 Responsive behavior

- compact: one column;
- medium/expanded: hero and content width constrained; history remains readable list;
- do not create a side-by-side pending/history layout that changes chronology in V1.

---

# 14. SETTINGS — Screen Specification

## 14.1 Role

Settings/Profile is a root destination containing identity, weekly target, account/workshop information, edit mode, app information, and sign-out.

Current information architecture is preserved.

## 14.2 View-mode structure

```text
Scaffold / Surface.Canvas
├── Scrollable body
│   ├── Screen Header: "الإعدادات"
│   │   ├── Edit action
│   │   └── Sign-out action
│   ├── identity block
│   │   ├── Avatar
│   │   ├── full name
│   │   └── join date
│   ├── Settings Group: weekly target
│   ├── Settings Group: basic data
│   ├── Settings Group: workshop data (conditional)
│   └── Settings Group: app information
└── Bottom Navigation
    └── center New Chat FAB
```

## 14.3 Header

Use root `Screen Header` + `Top Header`.

- title: `الإعدادات`;
- direct Edit action;
- Sign-out action;
- when edit mode starts, Edit becomes Cancel/close according to approved Icon Button state;
- two direct actions are the maximum allowed by Top Header contract.

Sign-out confirmation uses approved Dialog.

## 14.4 Identity block

Use:

- Avatar component at approved prominent size;
- user full name;
- join date as supporting text.

This is not a second screen header.

## 14.5 Weekly target

Use `Settings Group` containing a dedicated numeric-control composition built from approved components.

Required content:

- group title `الهدف الأسبوعي`;
- current amount/value;
- increment action;
- decrement action;
- short explanatory text.

Rules:

- value uses approved `Stat Value`/numeric typography, never local monospace styling unless the numeric component contract explicitly requires it;
- plus/minus controls use governed Icon Buttons/actions;
- feature owns min/max/step/business persistence;
- group does not invent local border/background values.

## 14.6 Basic data

Use `Settings Group` + `Settings Row`.

Rows:

- phone;
- bank;
- account / IBAN.

Rows are read-only in view mode.

## 14.7 Workshop data

Conditionally shown when workshop data exists.

Use `Settings Group` + Settings Rows:

- workshop name;
- specialty;
- workers count;
- address.

If absent, do not show an empty workshop group in view mode.

## 14.8 App information

Use `Settings Group` + navigable Settings Rows:

- About app;
- Privacy Policy;
- FAQ.

Each row:

- governed leading icon;
- title;
- optional supporting value/version where appropriate;
- AutoMirrored navigation affordance where used;
- full-row click target.

## 14.9 Edit mode

Edit mode replaces read-only data groups with governed form groups while preserving identity/header context.

Use:

- Text Field;
- Numeric Field for numeric data;
- Selection Field for specialty if selection behavior is retained;
- Primary Button `حفظ التغييرات`;
- governed inline validation/error feedback.

Rules:

- no separate styling family for edit mode;
- form sections follow same semantic grouping as view mode;
- Save loading is local to Primary Button;
- Cancel returns to view mode without changing navigation destination;
- IME traversal follows logical RTL form order.

## 14.10 Spacing

```text
Body horizontal gutter:       Space.LG compact
Header → identity:            Space.XL / Space.2XL
Identity → first group:       Space.2XL
Group → group:                Space.LG / Space.2XL
Rows inside group:            Settings Group Pattern rules
Bottom body padding:          Space.2XL + nav-safe inset
```

## 14.11 Scroll behavior

- body scrolls vertically;
- Header may be part of the body composition on Settings rather than a sticky top bar, matching the approved calm profile hierarchy;
- Bottom Navigation remains fixed;
- edit mode uses IME padding and scroll-to-focused-field behavior as needed.

## 14.12 Loading

Initial user/profile loading:

- root shell and Bottom Navigation remain;
- use Loading Screen/structured identity loading;
- do not show a tiny spinner inside an otherwise empty Settings page.

Saving profile:

- keep form visible;
- Primary Button shows loading;
- prevent duplicate save.

## 14.13 Empty

A missing user/profile object is not a valid empty product state.

After loading completes, missing required profile data is an error/recovery state, not Empty Screen.

Optional workshop data may simply omit that group.

## 14.14 Error

- initial profile load failure with no usable data: Error Screen inside root shell;
- save error: inline form error/governed Snackbar while preserving entered data;
- weekly-target update failure: keep previous confirmed value and surface recoverable feedback;
- sign-out failure must not clear the screen until session behavior confirms logout.

## 14.15 Bottom Navigation

- visible;
- Settings selected;
- center FAB opens New Chat Dialog.

## 14.16 RTL

- entire screen RTL;
- labels and values use logical start/end alignment;
- phone/IBAN/numeric sequences may be local LTR islands;
- plus/minus semantics do not mirror mathematically;
- navigation chevrons/arrows use AutoMirrored assets where directional.

## 14.17 Responsive behavior

- compact: one column;
- medium/expanded: center a constrained settings column for readability;
- do not stretch Settings Rows across an excessively wide screen;
- edit form remains one logical vertical flow in V1 rather than multi-column form layout.

---

# 15. Screen-to-Pattern matrix

| Screen | Screen Header | Dashboard Hero | Metric Summary | Conversation Item | Transaction Row | Pending Request Card | Settings Group/Row | Report Stat Tile | Media Action Group | Search Results List | Empty/Error/Loading |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Home | Yes | Yes | No | No | No | No | No | No | No | No | Partial/screen states |
| Conversations | Yes | No | No | Yes | No | No | No | No | No | Yes | Yes |
| New Chat | No root header | No | No | No | No | No | No | No | Yes | No | Local only |
| Reports | Yes | Optional dominant metric composition | Yes | No | No | No | No | Yes | No | No | Loading/Error; no global Empty |
| Balance | Back form | Yes | Optional support | No | Yes | Yes | No | report-entry action | No | No | Yes/local Empty |
| Settings | Yes | No | No | No | No | No | Yes | No | No | No | Loading/Error |

---

# 16. Screen-to-Component matrix

| Screen | Main approved Components |
|---|---|
| Home | Top Header, Icon Button, Badge, Highlight Card, Stat/Instrument Number, Status Indicator, Bottom Navigation, FAB |
| Conversations | Top Header, Search Field, Avatar, Badge, List Row, Divider, Bottom Navigation, FAB, Snackbar |
| New Chat | Dialog, Text Field, Icon/Button actions, Status Indicator, Primary Button, Secondary/Text Button, Snackbar |
| Reports | Top Header, Metric Card, Highlight/Base Card, Stat Value, Section Header, Bottom Navigation, FAB |
| Balance | Back Header, Highlight Card, Stat Value, Status Chip, Secondary Button, Divider, Bottom Sheet, Dialog, Snackbar |
| Settings | Top Header, Icon Button, Avatar, Base Card, List Row, Text Field, Numeric Field, Selection Field, Primary Button, Dialog, Bottom Navigation, FAB |

---

# 17. Bottom Navigation visibility matrix

| Surface | Bottom Navigation | Selected item | Center FAB |
|---|---|---|---|
| Home | Visible | Home | Visible |
| Conversations | Visible | Messages | Visible |
| Reports | Visible | Reports | Visible |
| Settings | Visible | Settings | Visible |
| Balance | Hidden | — | Hidden |
| New Chat Dialog | Underlying root bar visible through modal scrim but disabled | unchanged | disabled while modal open |

No screen may invent a second root navigation bar.

---

# 18. Scroll ownership matrix

| Screen | Fixed | Scroll owner |
|---|---|---|
| Home | Bottom Navigation | whole body |
| Conversations | Header + Search + Bottom Navigation | conversation results list |
| New Chat | modal position | Dialog content only when needed |
| Reports | Bottom Navigation | whole body |
| Balance | Back Header | body; sheet separately when open |
| Settings | Bottom Navigation | whole body including header/identity/groups |

---

# 19. State acceptance matrix

## Home

- valid empty remote insight does not erase hero;
- refresh keeps usable content;
- full failure does not remove safe root navigation.

## Conversations

- initial loading distinct from refresh;
- no conversations distinct from no search results;
- refresh error preserves existing list.

## New Chat

- empty draft is normal;
- send loading stays local;
- errors preserve draft/media where safely possible.

## Reports

- zero values are valid data;
- no global empty state;
- failures must not be silently rendered as zero.

## Balance

- zero balance is valid;
- empty history is local only;
- withdrawal submission states do not erase main data.

## Settings

- missing optional workshop group is valid;
- missing required profile after loading is error, not empty;
- save error preserves form data.

---

# 20. Accessibility and interaction requirements

All six screens inherit Session 03 component requirements plus:

1. interactive rows/cards have >=48dp effective touch target;
2. color is never the sole status signal;
3. unread/pending/error states have text/icon/shape support;
4. long Arabic text can wrap without overlapping icons/actions;
5. large financial values may scale/wrap within component contract but must not be arbitrarily shrunk;
6. TalkBack descriptions are required for non-text notification, back, edit, logout, media, remove, and recording controls;
7. decorative imagery has no misleading content description;
8. center FAB retains a clear semantic accessibility label.

---

# 21. Explicit visual migration targets

During Session 08 migration, remove these screen-local visual ownership patterns from the six target screens:

- raw `TopAppBar`/manual title rows when Screen Header covers the case;
- raw `OutlinedTextField` search/composer styling when approved fields cover the case;
- feature-local radii/border constants for generic cards;
- raw screen-defined metric-card color/radius/border families;
- local empty states built from emoji;
- local centered spinners as the only initial state presentation;
- raw `AlertDialog` styling where approved Dialog exists;
- raw withdrawal/modal styling where approved Bottom Sheet/Dialog exists;
- custom profile rows/info menu rows where Settings Row exists;
- feature-local unread badges when Badge component exists;
- local 60sp/monospace numeric styling when Stat Value/Instrument Number owns the display role.

Behavior remains feature-owned.

---

# 22. Session 05 resolved issues

## 22.1 New Chat container

**Resolved:** remain a modal Dialog in V1.

## 22.2 Report responsiveness

**Resolved:** two-column phone grid with full-width fallback for unreadable tiles; wider widths may increase columns without changing semantic order.

## 22.3 Home hero ownership

**Resolved at screen level:** Dashboard Hero Pattern owns composition/hierarchy; pump/gauge/countdown visuals and behavior remain feature-owned content. Exact package/API ownership belongs to Session 06.

## 22.4 Reports chart

**Resolved:** no chart in the six-screen V1 specification. Current information architecture does not require one.

## 22.5 Settings ordering

**Resolved:** Header → identity → weekly target → basic data → workshop data (conditional) → app information.

## 22.6 Root vs child navigation

**Resolved:** Home/Conversations/Reports/Settings show Bottom Navigation; Balance does not; New Chat is modal over the current root shell.

---

# 23. Decisions

- The six primary screen compositions are now specified.
- Current business/content hierarchy is preserved while local visual styling is replaced by approved DS contracts during migration.
- Home remains the strongest branded screen and uses Dashboard Hero with feature-owned instrument content.
- Conversations keeps a fixed Header/Search while only results scroll.
- New Chat is locked as a modal Dialog in V1.
- Reports uses a scrollable metric/report dashboard; zero values are valid data, not empty state.
- Balance is a child destination with Back Header and no Bottom Navigation.
- Settings remains a single vertical root flow with grouped view/edit states.
- Root Bottom Navigation is limited to Home, Conversations, Reports, Settings.
- Responsive V1 preserves the same information architecture and does not introduce navigation rail/master-detail layouts.
- Loading/empty/error behavior is now explicit per screen.

---

# 24. Forbidden from this point forward

1. No six-screen migration may alter the section order defined here without a new documented design decision.
2. No root screen may remove or duplicate the shared Bottom Navigation during V1 migration.
3. No child Balance screen may add root Bottom Navigation.
4. New Chat may not be converted to Bottom Sheet/full-screen route during V1 implementation without reopening Session 05.
5. No screen may invent local card/search/header/badge/state styling when an approved Component/Pattern covers it.
6. No screen may use arbitrary raw spacing/radius/color values for generic UI structure.
7. No reports implementation may treat zero values as empty or silently convert data-load failures to zero.
8. No screen may replace usable data with a full-screen spinner during background refresh.
9. No emoji may be used as generic Empty/Error action-state iconography where governed icons exist.
10. No Design System screen composition may own ViewModel, Repository, permissions, recording, camera, navigation execution, or business rules.
11. No tablet-specific IA change (rail/master-detail) is admitted in V1.
12. No new chart Pattern is introduced for Reports in V1.

---

# 25. Deferred

The following belong to later sessions:

- exact Kotlin APIs for Screen Header/Patterns;
- package/folder ownership of Foundations, Components, Patterns;
- extraction of Home pump/instrument UI from current feature files;
- exact responsive implementation mechanism;
- Previews;
- actual component implementation;
- actual screen migration;
- route/package renaming for `RecentActivityScreen` / `ActivityLogScreen`;
- pixel-level comparison against every approved redesign image;
- accessibility QA on-device;
- visual regression testing;
- final deletion of legacy local components.

---

# 26. Open Issues

No screen-level issue blocks Session 06.

Architecture-level questions intentionally remain:

```text
- Which package owns application-shell Bottom Navigation state and unread counts?
- Which Home instrument primitives remain feature-owned vs become specialized DS data primitives?
- What are the exact public APIs for Patterns without leaking feature models?
- How are width classes exposed to screen/pattern code without coupling DS to navigation/business layers?
- Which legacy component files can be removed only after Session 08 migration confirms zero usage?
```

These are Session 06 concerns, not reasons to reopen Session 05.

---

# 27. Next Session Input

Session 06 must use the complete approved chain:

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
```

Session 06 must define how `core:designsystem` physically owns:

```text
foundation/
components/
patterns/
theme/
```

while ensuring:

- feature business logic remains outside;
- no ViewModel/Repository enters Design System;
- Bottom Navigation state is external;
- Home special instrument behavior stays correctly separated;
- Patterns receive presentation-ready state + callbacks only;
- Session 07 can implement the DS without touching screen business logic.

---

# 28. Session close

```text
STATUS: APPROVED

Decisions:
- Six primary screen specifications are locked.
- New Chat remains a modal Dialog.
- Root Bottom Navigation exists only on Home, Conversations, Reports, Settings.
- Balance is a child destination without Bottom Navigation.
- Per-screen structure, spacing, states, scroll, responsive, and RTL behavior are defined.
- Reports zero values are valid data; no chart/global empty state is added.
- Home instrument behavior remains feature-owned inside the Dashboard Hero composition.

Forbidden:
- No local competing visual primitives during migration.
- No New Chat container change without reopening Session 05.
- No root/child navigation-shell drift.
- No full-screen loading replacement during background refresh.
- No silent error-to-zero conversion in Reports.
- No business logic or state sources in Design System.

Deferred:
- Architecture/package ownership, Kotlin APIs, implementation, migration, pixel QA, legacy deletion.

Open Issues:
- Only architecture-level ownership/API questions remain; none block Session 06.

Next Session Input:
- 05_SCREEN_SPECS.md is the authoritative screen-level source of truth for Session 06.
```

**Approval gate:** continuing to Session 06 or otherwise accepting this output approves Session 05.
