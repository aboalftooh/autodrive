# AutoDrive Design System — Session 08: Screen Migration State

**Output:** `08_MIGRATION_STATE.md`  
**Session:** 08 — Screen Migration  
**Input source of truth:** `07_IMPLEMENTATION_STATE.md` + approved Sessions 02–06  
**Business/domain redesign:** None  
**STATUS:** APPROVED

---

## 1. Scope completed

Session 08 migrated the production presentation call sites in the required order:

1. Bottom Navigation
2. Headers
3. Settings
4. Balance
5. Conversations
6. New Chat
7. Reports
8. Home

The feature ViewModels, repositories, domain rules, persistence, sync contracts, withdrawal rules, chat recorder/media workflow, pump animation/timer behavior, and report calculations were not redesigned.

The only ViewModel change is in the **app shell** (`AppNavigationViewModel`) to coordinate the feature-owned unread-count stream required by Session 06/07 ownership rules.

---

## 2. Bottom Navigation — MIGRATED

The four root destinations now render the V1 `AutoDriveBottomNavigation` directly:

- Home → `home`
- Conversations → `messages`
- Reports → `reports`
- Settings → `settings`

All four use the governed center `AutoDriveFab` for New Chat.

Balance remains a child destination and has no root Bottom Navigation.
New Chat remains a modal Dialog and has no Bottom Navigation of its own.

### Live unread count

The Session 07 deferred unread wiring is closed:

```text
feature:notifications / UnreadMessagesObserver
                ↓
app shell / AppNavigationViewModel
                ↓
AppNavigation collects Flow<Int>
                ↓
mainGraph(unreadMessages)
                ↓
Home / Conversations / Reports / Settings
                ↓
AutoDriveNavigationItem.badgeCount
```

The Design System remains presentation-only and does not read repositories/session state.

---

## 3. Headers — MIGRATED

- Home → root `ScreenHeader`; kept inside the scroll body according to Session 05.
- Conversations → fixed `ScreenHeader` + Search Field composition.
- Reports → root `ScreenHeader` inside the scroll body.
- Settings → root `ScreenHeader` inside the body with governed Edit/Sign-out Icon Buttons.
- Balance → fixed `ScreenHeader` using the Back Header form with withdrawal action.

Directional behavior is provided by V1 navigation components and AutoMirrored assets.

---

## 4. Settings — MIGRATED

Target:

`feature/profile/.../ProfileScreen.kt`

Migrated presentation:

- root Bottom Navigation + center FAB;
- Screen Header;
- governed Avatar and Stat Value;
- Settings Group / Settings Row patterns;
- weekly target composition built only from V1 Base Card, Section Header, Stat Value, and Icon Buttons;
- Text Field, Numeric Field, Selection Field;
- Primary Button save state;
- governed Dialog for sign-out;
- governed Snackbar content for success feedback.

Preserved feature interactions:

- start/cancel editing;
- save profile;
- weekly target min/max/step behavior;
- sign-out confirmation/action;
- information destinations.

---

## 5. Balance — MIGRATED

Targets:

- `feature/balance/.../BalanceScreen.kt`
- `feature/balance/.../WithdrawalSheet.kt`

Migrated presentation:

- fixed Back Header, no root Bottom Navigation;
- Dashboard Hero for current balance;
- governed Secondary Button for commission report;
- Section Header + Pending Request Card;
- unified Transaction Row pattern;
- local governed Empty/Error/Loading states;
- withdrawal flow migrated to `AutoDriveBottomSheet`;
- Numeric/Text fields and V1 actions;
- cancellation confirmation uses V1 Dialog;
- feedback uses governed Snackbar content.

Preserved feature interactions:

- open/close withdrawal flow;
- amount/note updates;
- submit withdrawal;
- cancel pending withdrawals;
- report navigation;
- existing submission-success/offline timing.

---

## 6. Conversations — MIGRATED

Target:

`app/.../feature/reports/presentation/recent/RecentActivityScreen.kt`

Migrated presentation:

- fixed Screen Header;
- V1 Search Field;
- Conversation Item pattern;
- governed unread badges;
- Loading / Empty / Error screen patterns;
- governed Snackbar visual for refresh errors;
- root Bottom Navigation + center New Chat FAB.

Preserved behavior:

- query/filter updates;
- pull-to-refresh;
- open conversation;
- auto-start New Chat route behavior;
- create/open management conversation;
- non-destructive refresh error handling.

---

## 7. New Chat — MIGRATED

Target:

`feature/chat/.../NewChatDialog.kt`

V1 container remains the Session 05-approved modal Dialog.

Migrated presentation:

- `AutoDriveDialog`;
- governed multiline Text Field;
- Media Action Group;
- Primary/Secondary actions;
- governed inline error color/typography.

Preserved feature behavior:

- camera permission + capture;
- gallery selection;
- audio permission + recording;
- recording timer;
- media removal;
- `createAndSend` workflow;
- existing conversation-ready callback.

No recorder, permission, or media lifecycle logic moved into the Design System.

---

## 8. Reports — MIGRATED

Target:

`app/.../feature/reports/presentation/log/ActivityLogScreen.kt`

Migrated presentation:

- Screen Header in scroll body;
- dominant governed summary composition;
- Report Stat Tile for navigable report destinations;
- Metric Card for passive pending value;
- Stat Value roles;
- root Bottom Navigation + center New Chat FAB;
- governed initial Loading state.

Existing report destinations and callbacks are preserved.

---

## 9. Home — MIGRATED

Targets:

- `HomeScreen.kt`
- `HomeSupportCards.kt`
- `HomeHeroComponents.kt` (only the V1 presentation boundary required by migration)

Migrated presentation:

- root Screen Header inside scroll body;
- approved supporting line from the visual reference;
- governed notification Icon Button + Badge;
- V1 Bottom Navigation + center FAB;
- pump composition hosted by `DashboardHero`;
- legacy public `SevenSegmentNumber` call replaced with `AutoDriveInstrumentNumber`;
- weekly competition uses the governed Highlight Card while retaining the approved crown as content/illustration;
- Dynamo/insight card uses governed Insight Highlight Card and preserves `نافذة بنزين` identity.

### Intentional feature-owned Home instrumentation

Session 06 explicitly keeps Home's pump/gauge/countdown business instrumentation feature-owned.
Therefore the internal gauge geometry, pump animation, countdown timer, sound behavior, and instrument-only visual effects remain in `HomeHeroComponents.kt`.

They are not duplicated as generic Design System components.

---

## 10. Legacy compatibility state after migration

Production call sites now contain:

```text
AutoDriveBottomBar(...)       0
BottomNavItem.*               0
SevenSegmentNumber(...)       0
```

The legacy compatibility definition files themselves are intentionally retained until Session 10 consolidation because non-target legacy surfaces still use the broader compatibility layer and Session 10 owns final removals/governance cleanup.

`SharedComponents.kt` and legacy theme aliases therefore remain compatibility-only, not the V1 direction.

---

## 11. Business behavior preservation

Static comparison against the v07 source of truth confirmed that critical feature interactions remain present after presentation migration.

No feature ViewModel, Repository, Domain model, Sync component, database component, or network component was modified by Session 08.

Changed application coordination is limited to unread-count collection/passing required by the approved shell ownership model.

---

## 12. Verification

### Session 07 Design System verifier

```text
V07 DESIGN SYSTEM STATIC VERIFICATION: PASS
- foundations/theme files: 9
- V1 components: 31
- component previews: 31
- V1 patterns: 14
- RTL pattern previews: 14
- forbidden DS dependencies/state ownership: none
- Session 06 ownership boundaries: enforced
```

### Session 08 migration verifier

Verifier: `tools/verify_designsystem_v08.py`

```text
V08 SCREEN MIGRATION STATIC VERIFICATION: PASS
- root bottom navigation: 4/4 migrated
- root headers: 4/4 migrated
- Settings/Balance/Conversations/New Chat/Reports/Home: V1 call sites present
- live unread count: app-shell collected, feature-owned source
- legacy production bottom-nav/seven-segment call sites: 0
- raw foundation styling in migrated surface files: 0
- critical feature interactions: preserved
```

### Existing full static verification

```text
behavior tests:                 48/48 PASS
architecture reviews:           81/81 PASS
module checks:                  62/62 PASS
package checks:                 24/24 PASS
migration statements:           21/21 PASS
rows preserved:                 13/13 PASS
indexes created:                20/20 PASS
query plans:                    20/20 PASS
observability/security checks:  21/21 PASS
cleanup checks:                 15/15 PASS
v01 static verification:        PASS
```

### Android compilation

Attempted:

```text
./gradlew :app:compileDebugKotlin --offline --stacktrace
```

Compilation could not begin because Gradle 8.7 is not cached in the execution environment and the wrapper attempted to reach `services.gradle.org`, which is unavailable here (`UnknownHostException`).

This is an environment limitation, not recorded as a compile pass or compile failure of the migrated Kotlin source.

---

## 13. Visual QA deferred to Session 09

Session 08 establishes the production Design System call sites and screen composition contract.

The following remain intentionally for Session 09:

- emulator/device pixel comparison against every approved redesign;
- exact compact/short-device spacing adjustment;
- long-text and extreme-number visual inspection;
- full keyboard/IME visual traversal;
- Dialog/sheet scrim and motion inspection;
- complete RTL visual pass;
- touch/pressed/disabled rendering inspection;
- responsive 600dp+/840dp+ visual verification.

---

# Decisions

- The four root destinations use one V1 Bottom Navigation contract.
- Balance remains a child destination without root navigation.
- New Chat remains a modal Dialog.
- Unread message state is feature-owned, app-shell collected, and passed as presentation data.
- Home feature instrumentation stays feature-owned but is hosted by approved V1 presentation primitives/patterns.
- Screen migration does not change product/domain behavior.

# Forbidden

- No reintroduction of `AutoDriveBottomBar`, `BottomNavItem`, or public `SevenSegmentNumber` in production screens.
- No ViewModel/Repository/data lookup inside Design System.
- No new raw colors/radii/spacing/typography in migrated screen surfaces.
- No local duplicate of a V1 component/pattern.
- No root Bottom Navigation in Balance or New Chat.

# Deferred

- Pixel-level visual QA and responsive/device validation → Session 09.
- Final legacy compatibility deletion and governance cleanup → Session 10.
- Real Android compilation/build → environment with Gradle 8.7 available.

# Open Issues

- No source-level migration blocker remains.
- Real Android compilation has not been executed in this environment because the Gradle distribution is unavailable.
- Visual differences, if any, must be recorded by Session 09 rather than changing business logic during migration.

# Next Session Input

Session 09 must use this migrated project and `08_MIGRATION_STATE.md` as its source of truth, then perform Visual QA against the approved redesign references without reopening Session 08 architecture unless a concrete defect is demonstrated.
