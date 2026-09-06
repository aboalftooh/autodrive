# AutoDrive Design System — Session 01: Design Audit

**Output:** `01_DESIGN_AUDIT.md`  
**Audit type:** Static design/code audit only  
**Code changes:** None  
**Theme changes:** None  
**Build/runtime assumptions:** None  
**STATUS:** APPROVED

---

## 1. Purpose

This audit establishes the visual/UI source of truth before defining the new foundations.

The scope follows Session 01 exactly:

- inventory every navigable screen and important modal surface;
- inventory the current shared UI components;
- inspect `core:designsystem`;
- identify styling declared directly inside screens/features;
- identify duplication and inconsistencies;
- compare the current implementation with the approved redesign direction;
- classify what should be kept, redesigned, merged, moved, or removed from Design System ownership.

No implementation decisions from later sessions are executed here.

---

# 2. Executive finding

The current project **has a theme and a shared UI module, but it does not yet have a complete Design System**.

The current structure is best described as:

```text
Central colors + central typography
        +
small set of shared components
        +
large amount of screen-local styling
```

The main problem is not that every screen ignores the theme. Most screens correctly use the shared color constants and `MaterialTheme.typography`.

The deeper problem is that visual decisions still live inside features:

- spacing;
- radii;
- border thickness;
- alpha levels;
- component heights;
- card structure;
- headers;
- rows;
- empty states;
- loading states;
- dialogs;
- search fields;
- icon sizes;
- screen-specific variants.

Therefore a global visual change currently requires editing many feature files instead of changing a small number of Design System definitions.

### Audit numbers

Static scan of the uploaded project found:

- **270 Kotlin files** overall.
- **44 Kotlin files containing Compose UI**.
- **25 navigation destinations** in `AppDestinations.kt`.
- **17 public composables/theme entry points** currently exposed from `core:designsystem`.
- **630 direct `dp` literals inside feature/app Compose files**.
- **119 direct `RoundedCornerShape(...)` declarations inside feature/app Compose files**.
- **51 direct `sp` font-size literals inside feature/app Compose files**.
- **67 local `FontWeight` overrides inside feature/app Compose files**.
- **95 local alpha overrides using `.copy(alpha = ...)` inside feature/app Compose files**.
- **44 local border declarations inside feature/app Compose files**.
- **16 raw hexadecimal colors outside the central theme**, all inside `HomeHeroComponents.kt`.
- **0 `@Preview` declarations** in the inspected UI/Design System code.

This is enough evidence to justify building foundations before migrating screens.

---

# 3. Screen inventory

## 3.1 Authentication and onboarding

| # | Surface | Route / form | Source | Current visual structure | Audit classification |
|---|---|---|---|---|---|
| 1 | Session expired | `session_expired` | `feature/auth/.../login/LoginScreen.kt` | status icon, avatar, CTA, secondary action | Keep behavior; redesign with shared auth pattern |
| 2 | Waiting | `waiting` | `feature/auth/.../join/WaitingScreen.kt` | back action, status information, shared card/button | Keep content; merge into auth/onboarding pattern |
| 3 | Phone input | `phone_input` | `feature/auth/.../login/PhoneInputScreen.kt` | login hero, phone field, CTA, security hint | Keep flow; redesign through shared input/button foundations |
| 4 | Terms | embedded inside Phone Input | `feature/auth/.../login/TermsScreen.kt` | scrollable legal content + CTA | Keep; treat as reusable legal-content screen pattern |
| 5 | OTP input | `otp_input/...` | `feature/auth/.../login/OtpInputScreen.kt` | manual back header, hidden input + OTP boxes, help card | Keep behavior; redesign header/input/status components |
| 6 | Invite/code input | `code_input` | `feature/auth/.../join/CodeInputScreen.kt` | manual back header, code input, button | Keep flow; merge header/input treatment |
| 7 | Account type | `account_type` | `feature/auth/.../register/AccountTypeScreen.kt` | step indicator + selectable account cards | Keep structure; redesign cards as selectable-card variant |
| 8 | Basic info | `basic_info/{accountType}` | `feature/auth/.../register/RegisterScreens.kt` | form fields + step indicator | Keep flow; reuse form pattern |
| 9 | Workshop info | `workshop_info` | `feature/auth/.../register/RegisterScreens.kt` | form + specialty picker | Keep flow; move specialty semantics out of Design System |

### Auth conclusion

Auth already reuses `AutoDriveButton`, `StepIndicator`, and some shared fields. It is one of the better candidates for migration after foundations are stable.

---

## 3.2 Primary product surfaces

These are the six surfaces explicitly expected to define the new visual language later.

| # | Product surface | Current implementation | Source | Current condition | Audit action |
|---|---|---|---|---|---|
| 10 | Home | `HomeScreen` + multiple feature-local hero/support components | `app/.../feature/home/presentation/` | closest screen to the approved visual direction; highly custom | Keep product hierarchy; refactor visual rules into tokens/patterns |
| 11 | Conversations | `RecentActivityScreen` | `app/.../reports/presentation/recent/RecentActivityScreen.kt` | functional but visually flatter and more Material-like | Redesign; keep search/list/refresh behavior |
| 12 | New chat | global `Dialog`, not a navigation destination | `feature/chat/.../NewChatDialog.kt` | message field, media actions, recording state, send/cancel | Redesign and resolve whether final form remains Dialog or becomes screen/sheet |
| 13 | Reports | `ActivityLogScreen` | `app/.../reports/presentation/log/ActivityLogScreen.kt` | dense metric grid built almost entirely locally | Keep information architecture; rebuild with metric/report patterns |
| 14 | Balance | `BalanceScreen` + local components | `feature/balance/...` | hero balance + withdrawal + history rows | Keep information hierarchy; redesign components/patterns |
| 15 | Settings/Profile | `ProfileScreen` | `feature/profile/.../ProfileScreen.kt` | header + avatar + target + cards + edit + app info | Keep content/flows; redesign grouping/header/rows/forms |

---

## 3.3 Supporting main-app destinations

| # | Surface | Route | Source | Audit action |
|---|---|---|---|---|
| 16 | Notifications | `notifications` | `feature/notifications/.../NotificationsScreen.kt` | Merge header and notification row into shared patterns |
| 17 | Commission report | `commission_report` | `feature/commission/.../CommissionReportScreen.kt` | Merge summary/metric cards with report metric system |
| 18 | Invoice detail | `invoice_detail/{invoiceId}` | `app/.../reports/presentation/log/InvoiceDetailScreen.kt` | Merge back header, detail sections, rows |
| 19 | Weekly competition | `weekly_competition` | `app/.../competition/presentation/WeeklyCompetitionScreen.kt` | Keep destination; visual treatment should follow branded highlight pattern |
| 20 | Invoice list | `invoice_list?...` | `app/.../reports/presentation/log/InvoiceListScreen.kt` | Merge back header and list-row pattern |
| 21 | Win weeks | `win_weeks` | `app/.../reports/presentation/log/WinWeeksScreen.kt` | Merge back header and result-row pattern |
| 22 | Weekly commissions | `weekly_commissions` | `app/.../reports/presentation/log/WeeklyCommissionsScreen.kt` | Merge back header and financial-row pattern |
| 23 | Competition history | `competition_history` | `app/.../reports/presentation/log/CompetitionHistoryScreen.kt` | Merge back header and ranking-row pattern |
| 24 | Chat | `chat/{conversationId}?...` | `feature/chat/.../ChatScreen.kt` | Keep message flow; redesign top bar/composer/bubbles through coherent chat pattern |

---

## 3.4 Info and legal destinations

| # | Surface | Route | Source | Audit action |
|---|---|---|---|---|
| 25 | About app | `about_app` | `app/.../info/presentation/AboutAppScreen.kt` | Merge header/sections/list items |
| 26 | Privacy policy | `privacy_policy` | `app/.../info/presentation/PrivacyPolicyScreen.kt` | Merge header/content-card pattern |
| 27 | FAQ | `faq` | `app/.../info/presentation/FaqScreen.kt` | Merge header/search/card pattern |

> Navigation contains 25 route destinations. The inventory above reaches 27 visual screens because `TermsScreen` is embedded rather than routed, and `NewChatDialog` is a global modal surface rather than a route.

---

# 4. Modal / overlay inventory

Important non-screen surfaces are also part of the design language and must not be ignored.

| Surface | Source | Current form | Audit action |
|---|---|---|---|
| New chat | `NewChatDialog.kt` | `Dialog` | Redesign; final container form remains open decision |
| Withdrawal | `WithdrawalSheet.kt` | `ModalBottomSheet` | Keep behavior; merge into Bottom Sheet specification |
| Commission invoice detail | `CommissionEntryComponents.kt` | `ModalBottomSheet` | Merge with generic detail-sheet pattern |
| Entry information | `CommissionEntryComponents.kt` | `AlertDialog` | Merge into dialog system |
| Permission rationale | `core:designsystem/.../PermissionsRationaleDialog.kt` | `AlertDialog` | Move ownership out of generic DS or make dialog fully generic |
| Balance cancellation confirmation | `BalanceScreen.kt` | `AlertDialog` | Merge into confirmation dialog pattern |
| Profile sign-out confirmation | `ProfileScreen.kt` | `AlertDialog` | Merge into confirmation dialog pattern |
| Chat media action dialog | `ChatComposer.kt` | `AlertDialog` | Merge into media action pattern |
| Full-screen image viewer | `ChatImageViewer.kt` | `Dialog` | Keep feature-specific viewer; consume shared chrome/actions |

---

# 5. Current `core:designsystem` audit

## 5.1 Existing foundations

### Colors

Defined centrally in:

`core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/theme/Theme.kt`

Current tokens:

```text
BgDeep
BgSurface1
BgSurface2
BgSurface3
BorderColor
GreenWithdraw
GoldPending
GrayPaid
WhatsAppGreen
TextPrimary
TextSecondary
TextDisabled
AccentBlue
OrangeAccent
Material error color
```

### Strengths

- central dark palette already exists;
- background/surface hierarchy exists;
- text hierarchy exists;
- the approved visual direction is already broadly dark, so this is a useful starting point;
- Tajawal is already packaged and globally attached to Material typography.

### Problems

1. **Token naming mixes visual semantics and business semantics.**

`GreenWithdraw`, `GoldPending`, and `GrayPaid` are financial-state names but are reused as broad visual accents across unrelated surfaces.

Example:

- `GreenWithdraw` is also used as the primary button color, focus color, selected navigation color, loading color, avatar color, etc.
- `GoldPending` is used as a generic highlight/accent well beyond the pending financial state.

This makes future theming dangerous because changing a business-state color can unintentionally change global UI identity.

2. **No explicit semantic layer exists.**

There is no distinction such as:

```text
brand.primary
brand.accent
status.success
status.warning
status.error
surface.base
surface.raised
text.primary
text.secondary
border.subtle
border.active
```

3. **Home has 16 raw colors outside the theme.**

All were found in:

`app/.../home/presentation/HomeHeroComponents.kt`

They include fuel-gauge greens/yellows/oranges/reds, LED backgrounds, scanline color, borders, and gauge internals.

These colors are visually intentional but currently not governed.

---

## 5.2 Typography

Defined in:

`core/designsystem/.../theme/Typography.kt`

Current system already defines all Material3 typography roles with Tajawal:

- displayLarge / Medium / Small;
- headlineLarge / Medium / Small;
- titleLarge / Medium / Small;
- bodyLarge / Medium / Small;
- labelLarge / Medium / Small.

### Strengths

- one font family;
- coherent Material role coverage;
- central line heights and base weights;
- good foundation to retain.

### Problems

- feature code still contains **51 direct `sp` literals**;
- local font weights are overridden **67 times**;
- several special numbers use ad-hoc monospace text instead of a documented numeric style;
- the seven-segment style is an intentional brand primitive but has no formal typography/data-display specification yet.

Conclusion: **Typography is the most mature current foundation, but it is not fully enforced.**

---

## 5.3 Missing foundations

There are no central foundations for:

- spacing;
- radius;
- border widths;
- opacity tiers;
- shadow/glow;
- icon sizes;
- component heights;
- motion durations/easing;
- disabled-state opacity;
- focus/pressed states;
- RTL rules.

These missing foundations explain most of the repeated local styling.

---

# 6. Current shared component inventory

Public composables currently exposed by `core:designsystem`:

## Navigation

- `AutoDriveBottomBar`

## Actions / input

- `AutoDriveButton`
- `AutoDriveTextButton`
- `AutoDriveTextField`
- `SpecialtyPicker`

## Containers / data

- `AutoDriveCard`
- `MiniStatCard`
- `UserAvatar`
- `StepIndicator`
- `DonutChart`

## Brand / display primitives

- `SegmentDigit`
- `SevenSegmentNumber`
- `SegmentCard`
- `SegmentSeparatorDot`
- `AutoDriveLogo`

## Feedback

- `PermissionsDeniedDialog`

## Theme

- `AutoDriveTheme`

---

# 7. Shared component adoption

Current reuse is uneven.

| Component | Feature/app files using it | Finding |
|---|---:|---|
| `AutoDriveButton` | 12 | Good adoption; worth keeping as basis for new button family |
| `AutoDriveBottomBar` | 4 | Centralized visually, but architecturally coupled |
| `AutoDriveTextField` | 2 | Underused; many fields remain local |
| `AutoDriveCard` | 2 | Underused; most cards are feature-local |
| `UserAvatar` | 2 | Reusable primitive; keep |
| `StepIndicator` | 2 | Useful onboarding primitive |
| `SpecialtyPicker` | 2 | Reused, but feature-domain content is incorrectly owned by DS |
| `AutoDriveTextButton` | 1 | Very low adoption |
| `SevenSegmentNumber` | 1 | Intentional home-specific brand primitive |
| `SegmentCard` | 1 | Home-specific display primitive |
| `SegmentSeparatorDot` | 1 | Home-specific display primitive |
| `PermissionsDeniedDialog` | 1 | App-specific content inside generic DS module |
| `MiniStatCard` | 0 | Unused |
| `DonutChart` | 0 | Unused |
| `AutoDriveLogo` | 0 | Unused in active UI |
| `SegmentDigit` | 0 direct external use | implementation primitive only |

The Design System is therefore currently a **mixed bucket of generic UI, domain-specific UI, brand primitives, app logic, and unused experiments**.

---

# 8. Critical ownership problems inside `core:designsystem`

## 8.1 ViewModel inside Design System

`BottomNavBadge.kt` contains:

- `BottomNavBadgeSource`;
- `BottomNavBadgeViewModel`;
- Hilt injection;
- Flow/StateFlow lifecycle behavior.

`AutoDriveBottomBar` then calls `hiltViewModel()` internally.

This creates a hidden runtime dependency inside a supposedly visual component.

### Why this matters

The bottom bar cannot be treated as a pure visual component because it decides how to obtain business/application state.

For the later architecture target, the component should only receive state such as:

```text
selected item
unread count
click callbacks
```

The source of that data belongs outside the Design System.

**Classification:** remove application-state ownership from Design System.

---

## 8.2 Feature-domain content inside Design System

`SharedComponents.kt` contains the hardcoded workshop specialty list:

```text
ميكانيكا عامة
كهرباء سيارات
سمكرة ودهان
تكييف سيارات
إطارات وبطاريات
زيوت وصيانة
```

That is product/domain content, not a visual primitive.

`SpecialtyPicker` may consume a generic picker visual later, but the list and workshop semantics must not define the generic component.

**Classification:** move domain data/behavior out; retain only a generic selection-field primitive if needed.

---

## 8.3 App-specific dialog copy inside Design System

`PermissionsDeniedDialog` contains fixed application copy and permission behavior semantics.

The visual dialog pattern is reusable; the content is not.

**Classification:** either move this component out of DS or replace it with a generic dialog component/pattern receiving title/body/actions.

---

## 8.4 Feature imagery owned by Design System

`core:designsystem/src/main/res` currently owns product-content assets including:

- Dynamo character images;
- `login_hero.png`;
- `logo_benzin.png`;
- `whatsapp.png`;
- launcher assets.

Not all visual resources should automatically belong to the Design System.

A future ownership rule is required:

- DS owns generic visual resources/tokens/icons needed by reusable components;
- feature-specific content imagery should live with its feature/application owner.

This is architectural cleanup for later sessions, not a Session 01 code change.

---

# 9. Inline styling audit

## 9.1 Direct spacing and sizing

Feature/app Compose files contain **630 `dp` literals**.

Most frequent values across the UI are:

```text
16.dp  -> 111 occurrences
12.dp  -> 75
8.dp   -> 63
4.dp   -> 53
20.dp  -> 50
1.dp   -> 44
14.dp  -> 42
10.dp  -> 35
24.dp  -> 25
18.dp  -> 22
6.dp   -> 21
```

The repetition shows that an implicit spacing system already exists, but it is not encoded as a foundation.

This is positive: Session 02 does not need to invent spacing from zero; it should formalize the values already dominant, then reconcile them with the approved designs.

---

## 9.2 Radius fragmentation

Feature/app Compose code contains **119 direct `RoundedCornerShape` declarations**.

Most frequent radius values:

```text
14.dp -> 28 occurrences
16.dp -> 22
12.dp -> 17
20.dp -> 13
10.dp -> 13
18.dp -> 12
22.dp -> 4
8.dp  -> 3
```

There is a visible family, but no rule explains when to use 12, 14, 16, 18, 20, or 22.

Result: visually similar components can receive different radii simply because they were authored in different files.

---

## 9.3 Borders and opacity

Feature/app Compose code contains:

- **44 direct `.border(...)` declarations**;
- **95 local `.copy(alpha = ...)` overrides**.

Common visual ideas such as:

- subtle border;
- highlighted border;
- success border;
- warning border;
- inactive surface;
- tinted icon background;
- selected state;

are repeatedly recreated rather than expressed as tokens/variants.

---

## 9.4 Hardcoded colors

There are **16 raw hex colors outside the theme**, all in `HomeHeroComponents.kt`.

This is a contained problem, not a project-wide color disaster.

The correct conclusion is:

> Global screens mostly consume theme colors correctly, but the most visually branded Home hero still carries its own private palette.

Those values should later become brand/special-display tokens rather than being deleted indiscriminately.

---

## 9.5 Hardcoded typography

Feature/app UI contains:

- **51 direct `sp` literals**;
- **67 direct weight overrides**.

Legitimate exceptions exist:

- emoji sizing;
- seven-segment/LED display;
- special numeric displays.

However ordinary UI text also frequently overrides the central scale.

Session 02 must distinguish **intentional display typography** from accidental local styling.

---

# 10. Styling hotspots

Files with the heaviest local visual responsibility:

| File | `dp` literals | Radii | `sp` literals | Raw colors | Audit interpretation |
|---|---:|---:|---:|---:|---|
| `HomeHeroComponents.kt` | 37 | 11 | 4 | 16 | most branded and most independent visual island |
| `AboutAppScreen.kt` | 37 | 11 | 4 | 0 | simple content screen carrying too much local styling |
| `BalanceComponents.kt` | 40 | 12 | 1 | 0 | strong candidate for reusable card/list patterns |
| `ChatMessageComponents.kt` | 50 | 4 | 3 | 0 | large chat-specific system lacking shared spacing/icon foundations |
| `NewChatDialog.kt` | 29 | 8 | 3 | 0 | duplicates media/recording/button concepts |
| `ActivityLogScreen.kt` | 16 | 10 | 7 | 0 | report-card family implemented locally |
| `HomeSupportCards.kt` | 23 | 7 | 6 | 0 | branded support cards implemented locally |
| `ProfileScreen.kt` | 36 | 4 | 2 | 0 | settings rows/forms/cards built partly locally |
| `OtpInputScreen.kt` | 25 | 5 | 1 | 0 | custom auth state not represented in DS |
| `InvoiceDetailScreen.kt` | 23 | 6 | 0 | 0 | detail sections/rows are reusable patterns |

---

# 11. Duplication clusters

## 11.1 Headers

There are at least **9 direct `TopAppBar` implementations**, plus several manual Row/Column headers.

Repeated examples occur in:

- Balance;
- Notifications;
- Commission report;
- Chat;
- Invoice detail;
- Invoice list;
- weekly commissions;
- win weeks;
- competition history;
- info/legal screens;
- auth flows.

### Current inconsistency

Some back arrows use `Icons.AutoMirrored.Rounded.ArrowBack`, while others use the non-mirrored `Icons.Rounded.ArrowBack`.

Static scan found:

- 9 non-auto-mirrored ArrowBack references;
- 9 auto-mirrored ArrowBack references.

**Classification:** merge into `Screen Header` / `Back Header` patterns.

---

## 11.2 Cards

Feature/app UI contains **34 direct `Surface(...)` usages** in addition to shared `AutoDriveCard`.

Current local card families include:

- Home hero;
- AI insight;
- weekly competition teaser;
- report metric cards;
- total commissions card;
- balance hero;
- pending requests;
- transaction rows;
- invoice/detail sections;
- FAQ cards;
- privacy cards;
- account type cards;
- profile cards;
- notification rows.

Many differ only by:

- radius;
- border color/alpha;
- padding;
- emphasis level;
- clickability;
- title/value arrangement.

**Classification:** merge into Base / Metric / Highlight / Alert card families plus higher-level patterns.

---

## 11.3 List rows

Feature-local rows include:

- `ConversationRow`;
- `NotificationItem`;
- `TransactionRow`;
- `PendingWithdrawalRow`;
- `InvoiceListRow`;
- `WeeklyCommRow`;
- `WinWeekRow`;
- `CompetitionWeekRow`;
- `ProfileRow`;
- `InfoMenuItem`;
- invoice detail rows;
- report rows.

They repeatedly implement:

```text
leading icon/avatar
primary text
secondary text
trailing value/status
divider/click state
```

**Classification:** define one generic `List Row` primitive plus specialized patterns.

---

## 11.4 Search fields

Search UI is currently implemented directly, for example in Conversations and FAQ.

The Conversations search field duplicates the same color, radius, and focus behavior already close to `AutoDriveTextField`, but with search-specific icons and clear action.

**Classification:** create a dedicated Search Field component rather than continuing local `OutlinedTextField` styling.

---

## 11.5 Empty / loading / error states

Current states are implemented separately per feature:

- centered progress indicators;
- emoji/icon-based empty states;
- inline error text;
- Snackbar errors;
- AlertDialogs;
- banners.

The concepts are valid but there is no shared visual grammar.

**Classification:** merge into standardized loading/empty/error patterns while preserving feature-specific copy.

---

## 11.6 Media and recording UI duplication

`ChatComposer.kt` and `NewChatDialog.kt` both implement concepts for:

- voice recording;
- recording timer;
- image/media actions;
- cancellation;
- send-ready media state.

They are separately styled and partially duplicated.

**Classification:** move shared visual states into a media-action pattern; keep recorder logic in the feature layer.

---

# 12. RTL audit

The manifest has:

```text
android:supportsRtl="true"
```

But RTL treatment is inconsistent in Compose:

- several report screens explicitly force `LayoutDirection.Rtl`;
- Home intentionally forces `LayoutDirection.Ltr` only for numeric LED/countdown subcomponents;
- many other Arabic screens rely on the device/application layout direction;
- back icons are split between mirrored and non-mirrored variants.

### Finding

The application is Arabic-first visually, but RTL is not yet governed as a Design System rule.

Session 02 must decide:

- global RTL policy;
- which numeric/data-display islands are intentionally LTR;
- start/end padding rules;
- icon mirroring rules;
- directional chevrons/arrows;
- alignment of amounts, dates, and mixed Arabic/Latin values.

---

# 13. Preview / component verification audit

No `@Preview` declarations were found in the inspected Design System or UI Compose code.

This means shared components currently lack a fast visual catalogue for:

- default state;
- disabled state;
- loading state;
- selected state;
- error state;
- long text;
- RTL;
- unusual values.

This is not fixed in Session 01, but it must become a requirement in the implementation session.

---

# 14. Comparison with the approved redesign direction

## Reference quality

The project ZIP contains `home.png`, which provides a concrete visual reference for the approved dark AutoDrive language.

The other approved redesigns were produced/reviewed outside the ZIP, so this Session 01 audit uses them as design direction rather than claiming pixel-level comparison. Pixel matching belongs to later Screen Specs and Visual QA.

## Approved visual language visible from the reference

The direction is clearly based on:

- very deep dark background;
- layered navy/black surfaces;
- thin luminous borders;
- controlled glow rather than generic Material elevation;
- orange/gold emphasis for primary branded actions/data;
- mint/green success/active accents;
- large high-contrast metric presentation;
- rounded but technical/instrument-like cards;
- a strong center action in bottom navigation;
- selective visual personality rather than flat default Material styling.

## Current code alignment

### Home

**Closest implementation to the approved direction.**

Already contains:

- dark surfaces;
- gold/orange LED display;
- fuel gauge;
- seven-segment digits;
- branded hero;
- competition teaser;
- highlighted AI card;
- central add action in bottom navigation.

However, it achieves this through a large amount of local custom styling.

**Conclusion:** Home is visually valuable as a design reference, but architecturally it is not yet reusable.

### Conversations

Current screen has:

- dark background;
- title;
- search field;
- list rows;
- unread badge;
- bottom navigation.

But it remains visually flatter and structurally closer to generic messaging UI than the stronger approved AutoDrive language.

**Conclusion:** keep behavior/content; redesign header, search, rows, states, and selection emphasis.

### New chat

Current UI is a standard dark dialog with direct Material buttons/fields.

Its interaction set is useful, but the container, media actions, recording state, and action hierarchy are not yet part of a coherent AutoDrive component family.

**Conclusion:** keep behavior; redesign visual composition. Final container type is an open decision.

### Reports

Current reports dashboard already has a useful information hierarchy:

- total commissions;
- balance;
- pending;
- invoice count;
- weekly wins;
- weekly commissions;
- competition entry.

But the screen locally defines almost all metric-card treatment.

**Conclusion:** preserve information architecture; rebuild using Dashboard/Metric/Report patterns.

### Balance

Current Balance has the right functional hierarchy:

- balance hero;
- withdrawal action;
- commission report entry;
- pending withdrawals;
- history.

The visual system is locally authored and the hero value uses a custom 60sp override.

**Conclusion:** preserve hierarchy and interactions; redesign with Stat Value, Highlight Card, Transaction Row, Status Chip, Sheet patterns.

### Settings/Profile

Current content model is strong:

- user identity;
- weekly target;
- account data;
- workshop data;
- edit flow;
- app information;
- sign-out.

But it mixes shared cards with feature-local rows/buttons and custom numeric styling.

**Conclusion:** preserve sections/flows; standardize Screen Header, Settings Group, Settings Row, form fields, dialogs, numeric controls.

---

# 15. Keep / Redesign / Merge / Remove-or-Move matrix

## KEEP as valid foundations or product concepts

| Item | Decision |
|---|---|
| `AutoDriveTheme` entry point | Keep |
| Tajawal font family | Keep |
| Central typography role approach | Keep, then refine |
| Deep dark visual direction | Keep |
| Existing background/surface/text token concept | Keep, rename/refine in Session 02 |
| Seven-segment / LED display idea | Keep as intentional branded data-display primitive |
| Bottom navigation information architecture | Keep |
| Central add action | Keep |
| Home pump/competition/insight hierarchy | Keep as product pattern |
| Conversations search/list behavior | Keep |
| Reports information architecture | Keep |
| Balance information hierarchy | Keep |
| Settings information hierarchy | Keep |
| Existing loading/empty/error concepts | Keep behavior, not visual implementations |

---

## REDESIGN

| Area | Reason |
|---|---|
| Color semantic model | business names currently act as global brand tokens |
| Spacing system | not encoded |
| Radius system | fragmented across 8+ common values |
| Border system | thickness/alpha recreated locally |
| Glow/shadow rules | not formalized |
| Icon sizing/state system | not formalized |
| Motion rules | animations exist but are screen-local |
| Bottom bar visual/state API | hidden ViewModel dependency + no explicit variants |
| Search field | duplicated local styling |
| Headers | many inconsistent implementations |
| Cards | many local families |
| Metric display | report/balance/home each define separately |
| Dialogs/sheets | inconsistent and content-coupled |
| Empty/loading/error states | inconsistent |
| Major six product surfaces | migrate to the approved visual system |

---

## MERGE into shared components/patterns

| Current scattered implementations | Target concept later |
|---|---|
| manual `TopAppBar` + Row headers | `Screen Header` / `Back Header` |
| report/balance/home/profile cards | Base / Metric / Highlight / Alert Card |
| conversation/notification/transaction/report/profile rows | `List Row` + feature patterns |
| local OutlinedTextFields | Text Field / Search Field / Numeric Field |
| local CTA variants | Primary / Secondary / Text / Icon Button |
| local badges | Badge / Status Chip / Status Indicator |
| repeated detail label/value rows | Detail/Data Row pattern |
| ChatComposer + NewChat recording visuals | Media Action / Recording pattern |
| feature-local empty/loading UI | Empty / Loading / Error patterns |
| repeated settings content | Settings Group / Settings Row |

---

## REMOVE OR MOVE FROM DESIGN SYSTEM OWNERSHIP

> This classification does **not** mean delete product functionality now. It means the item should not remain a generic DS responsibility in its current form.

| Item | Action later | Reason |
|---|---|---|
| `BottomNavBadgeViewModel` | Move out of DS | application state/business dependency |
| `BottomNavBadgeSource` | Move out of DS | data-source contract does not belong to visual layer |
| hardcoded workshop specialties | Move to auth/profile/domain owner | product content |
| `SpecialtyPicker` in current feature-specific form | Replace by generic picker + feature-provided options | DS should not know workshop domain |
| `PermissionsDeniedDialog` fixed copy | Move or genericize | app-specific content |
| `MiniStatCard` | Quarantine/remove if still unused after specs | zero active usage |
| `DonutChart` | Quarantine/remove if still unused after specs | zero active usage |
| `AutoDriveLogo` component | Reassess | zero active usage; brand asset ownership needs rule |
| feature imagery inside DS resources | Move to proper owners where feature-specific | ownership pollution |

---

# 16. Highest-risk design inconsistencies

## Critical

### A. Design System contains application state

`AutoDriveBottomBar` internally resolving a Hilt ViewModel is the clearest architectural violation.

### B. Visual constants are scattered

The project has central colors/typography but no central geometry/interaction foundations.

### C. Home is visually ahead of the rest of the application

The branded Home screen and flatter secondary screens currently feel like different maturity levels of the same product.

### D. RTL has no single governing rule

Arabic UI, explicit RTL islands, LTR numeric islands, and mixed back-arrow behavior need one policy.

---

## High

### E. Cards are reinvented per feature

This is the largest source of future visual drift.

### F. Headers are reinvented per screen

This produces inconsistent title size, spacing, back behavior, and icon treatment.

### G. No component previews

There is no systematic visual verification environment for shared components.

### H. Component naming is too broad or too feature-specific

Examples:

- `AutoDriveCard` is too generic to express variants;
- `SpecialtyPicker` is too domain-specific for a DS primitive;
- `GreenWithdraw` is too domain-specific for a global visual role.

---

## Medium

### I. Direct emoji usage as interface iconography

Emoji are used in multiple production UI states/cards.

They may remain where intentionally branded, but generic navigation/status/action iconography should use a controlled icon set.

### J. Feature files carry many local font-size overrides

This weakens typography consistency and makes responsive tuning harder.

---

# 17. Screen-by-screen migration priority from audit evidence

This is **not implementation order yet**. It identifies where the Design System must be capable before migration.

| Surface | Needs from Design System |
|---|---|
| Home | branded hero primitives, metric/data display, highlight cards, bottom nav, glow/border rules |
| Conversations | screen header, search field, list row, avatar, unread badge, empty/loading states |
| New chat | dialog/sheet container, text input, media action group, recording state, primary/secondary actions |
| Reports | metric summary, stat tile, action card, section title, responsive grid rules |
| Balance | back header, highlight card, stat value, status chip, transaction row, bottom sheet |
| Settings | screen header, avatar, settings group/row, form fields, numeric control, confirmation dialog |

This confirms the Session 03/04 component and pattern list in the plan is directionally correct.

---

# 18. What Session 02 must receive from this audit

Session 02 should **not start from Material defaults or from arbitrary new values**.

It should reconcile three sources:

1. the dominant values already used in code;
2. the approved redesign visual language;
3. the need for a small governed token system.

The foundations requiring explicit decisions are:

```text
Colors
Typography
Spacing
Radius
Borders
Opacity
Glow / Shadow
Icon sizes
Motion
RTL
Disabled / pressed / focus states
Special numeric/data-display styles
```

Special attention is required for the semantic separation of:

```text
brand colors
status colors
financial-state colors
surface colors
text colors
interactive-state colors
```

---

# 19. Decisions recorded by Session 01

## Decisions

1. The current `core:designsystem` is **not yet the final Design System**; it is a partial shared-UI module.
2. The current dark palette and Tajawal typography are valid starting assets, not final specifications.
3. Home is the strongest existing visual reference in code and should influence the foundations without allowing Home-specific implementation details to become global by accident.
4. Feature screens must stop owning repeated geometry/style decisions after migration.
5. Business/domain state must not live inside reusable visual components.
6. `AutoDriveBottomBar` must eventually become a pure presentational component receiving state from outside.
7. Workshop-specific content must leave generic DS ownership.
8. Repeated headers, cards, rows, fields, states, dialogs, and sheets are confirmed candidates for shared components/patterns.
9. RTL must become an explicit foundation, not a screen-by-screen choice.
10. Unused DS components must not automatically survive into V1 merely because they already exist.

---

# 20. Forbidden from this point forward

Until later sessions explicitly define otherwise:

1. Do not add new raw colors inside feature screens.
2. Do not introduce another local radius family as a permanent pattern.
3. Do not create another reusable-looking card/header/search component inside a feature without checking DS/pattern ownership.
4. Do not put ViewModels, repositories, data sources, or business state inside Design System components.
5. Do not add feature-specific option lists/content to the Design System.
6. Do not use the current Home hardcoded values as global tokens without Session 02 review.
7. Do not delete current components solely because this audit classifies them for reassessment; actual removal belongs to later implementation/consolidation sessions.
8. Do not migrate screens before foundations and component specifications are approved.

---

# 21. Deferred

The following are intentionally deferred:

- exact color hex values for the final system;
- exact spacing scale;
- exact radius scale;
- exact border opacity/width scale;
- glow/shadow token definitions;
- final icon family and icon-size scale;
- final motion durations/easing;
- final component APIs;
- final screen measurements;
- code relocation;
- component deletion;
- visual implementation;
- pixel-level QA against every redesign image.

These belong to Sessions 02–10.

---

# 22. Open issues

1. **New Chat container:** the current code implements it as a global `Dialog`, while the planning language treats “محادثة جديدة” as a screen. Final form must be locked in Screen Specs.
2. **Dark-only policy:** current theme is explicitly dark-only. Confirm in Foundations whether this is an intentional product requirement or only current implementation state.
3. **RTL policy:** Arabic-first layout must be explicitly governed even when device locale is not Arabic.
4. **Home special palette:** decide which gauge/LED colors become formal special-display tokens.
5. **Emoji policy:** decide where emoji remain intentional brand/content elements versus where controlled icons replace them.
6. **Unused DS components:** `DonutChart`, `MiniStatCard`, and `AutoDriveLogo` require a final keep/remove decision after component specifications.
7. **Feature imagery ownership:** define whether Dynamo/Login/WhatsApp imagery belongs to feature modules, app branding, or DS resources.
8. **Bottom navigation unread badge:** state owner must be outside DS, but exact navigation-state owner is an architecture decision for later.

---

# 23. Next Session Input

Session 02 receives the following source-of-truth conclusions:

```text
- Keep Tajawal as the font family.
- Keep the deep-dark AutoDrive visual direction.
- Keep central theme ownership.
- Do not preserve current token names blindly.
- Separate brand semantics from business/status semantics.
- Formalize spacing, radius, borders, opacity, glow, icons, motion, RTL.
- Use dominant existing values as evidence, not as automatic final values.
- Home is the strongest existing branded reference.
- Secondary screens currently need stronger visual unification.
- No ViewModel/data source/domain list inside Design System.
- Repeated headers/cards/rows/fields/states must become shared components/patterns.
```

---

# 24. Session close

```text
STATUS: APPROVED

Decisions:
- Current DS is partial shared UI, not final Design System.
- Dark visual direction + Tajawal remain the baseline.
- Repeated visual structures must be centralized.
- DS must become presentation-only.
- RTL becomes a formal foundation.

Forbidden:
- No new unmanaged colors/radii/reusable feature-local visual patterns.
- No business/application state inside DS.
- No screen migration before Foundations + Component Spec approval.

Deferred:
- Exact tokens, APIs, implementation, migrations, removals, Visual QA.

Open Issues:
- New Chat container form.
- Dark-only confirmation.
- RTL policy.
- Home special palette.
- Emoji/icon policy.
- Unused DS components.
- Feature image ownership.

Next Session Input:
- This document, once approved, is the sole source of truth for Session 02 — Foundations.
```

**Approval gate:** Do not start Session 02 until this file is accepted and `STATUS` is changed to `APPROVED`.


---

## Approval record

Session 01 was approved by the user by explicitly instructing execution of Session 02. No production code was changed during Session 01.
