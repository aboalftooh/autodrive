# AutoDrive Design System — Session 03: Component Specification

**Output:** `03_COMPONENT_SPEC.md`  
**Session:** 03 — Component Specification  
**Input source of truth:** `02_FOUNDATIONS.md`  
**Implementation changes:** None  
**Production code changes:** None  
**STATUS:** APPROVED

---

# 1. Purpose

This document defines the initial reusable component library for AutoDrive Design System V1 before any implementation or screen migration.

It translates Session 02 foundations into component-level contracts covering:

- responsibility;
- visual structure;
- size and geometry;
- semantic colors;
- typography;
- interaction states;
- disabled state;
- loading state;
- RTL behavior;
- usage rules;
- non-usage rules;
- ownership boundaries.

This session does **not** implement Kotlin components, alter feature behavior, change navigation, or migrate screens.

---

# 2. Source-of-truth gate

Session 02 was left in `REVIEW_REQUIRED` with an explicit gate stating that a direct instruction to continue to Session 03 counts as approval.

The user explicitly requested execution of Session 03. Therefore Session 02 is approved and becomes the sole foundation source of truth for this specification.

No foundation value is redefined here.

---

# 3. Component design principles

Every V1 component must follow these rules:

1. Components are presentation-only.
2. Components receive state and callbacks from callers.
3. No ViewModel, Repository, Flow source, Hilt lookup, or domain data source belongs inside a component.
4. Components consume semantic foundation roles, never raw feature colors.
5. Components do not accept arbitrary radii, border widths, or raw colors as general styling escape hatches.
6. Variants are named by semantic purpose, not by feature name.
7. Layout is Arabic-first and RTL-first.
8. Numeric/technical content may create the smallest necessary LTR island.
9. Loading preserves geometry and disables duplicate actions.
10. Disabled state preserves geometry, removes glow/press behavior, and uses governed disabled semantics.
11. Touch targets are at least 48dp where interactive.
12. A component does one visual job. Multi-component screen structures belong to Session 04 Patterns.

---

# 4. Component inventory for V1

## 4.1 Actions

- Primary Button
- Secondary Button
- Text Button
- Icon Button
- FAB

## 4.2 Inputs

- Text Field
- Search Field
- Numeric Field
- Selection Field — retained as a generic replacement for the current domain-owned `SpecialtyPicker`

## 4.3 Containers

- Base Card
- Metric Card
- Highlight Card
- Alert Card

## 4.4 Navigation

- Bottom Navigation
- Top Header
- Back Header

## 4.5 Feedback

- Badge
- Status Chip
- Snackbar
- Dialog
- Bottom Sheet
- Loading State
- Empty State

## 4.6 Data display

- Avatar
- List Row
- Section Header
- Divider
- Stat Value
- Status Indicator
- Step Indicator — retained because onboarding already uses it successfully
- Instrument Number — formalized replacement/owner for the seven-segment display primitive

---

# 5. Shared state contract

All interactive components use the following common state model where applicable:

```text
Default
Pressed
Focused
Selected / Active
Disabled
Loading
Error
```

Not every component exposes every state. A state is included only when it makes semantic sense.

### Common pressed behavior

- duration: `Motion.Fast` — 120ms;
- semantic `Opacity.Tint` overlay where applicable;
- no layout shift;
- only high-emphasis controls such as Primary Button and FAB may scale to approximately `0.97`.

### Common disabled behavior

- text/icon: `Text.Disabled`;
- neutral border where outlined;
- no glow;
- no press animation;
- same dimensions as enabled state.

### Common loading behavior

- preserve component width/height;
- prevent duplicate click actions;
- indicator uses the component action semantics;
- caller owns the operation and final state.

---

# 6. Actions

## 6.1 Primary Button

### Function

Primary action for the current surface: continue, save, confirm, submit, send, or the single strongest CTA.

### Visual specification

| Property | Specification |
|---|---|
| Height | 56dp |
| Minimum touch target | 56dp high |
| Horizontal padding | `Space.XL` — 20dp |
| Internal gap | `Space.SM` — 8dp |
| Radius | `Radius.MD` — 12dp |
| Default background | `Brand.Primary` |
| Text/icon on background | `Text.OnBrand` |
| Typography | `labelLarge` |
| Leading/trailing icon | `Icon.SM` — 20dp |
| Border | none by default |
| Glow | `Glow.None`; `Glow.Medium` only for an explicitly highlighted CTA variant |

### States

- **Default:** `Brand.Primary`, `Text.OnBrand`.
- **Pressed:** `Opacity.Tint` feedback + scale ~0.97 using `Motion.Fast`.
- **Focused:** visible `Border.Strong` focus treatment using `Brand.Primary` while preserving container geometry.
- **Disabled:** `Surface.Overlay` + `Text.Disabled`; no glow.
- **Loading:** centered progress indicator; text is replaced or visually suppressed; click disabled; dimensions unchanged.

### RTL

- icon/text order follows start/end semantics.
- directional icons use AutoMirrored variants.

### Use when

- there is one clearly dominant action in a section/surface.

### Do not use when

- multiple equal actions compete;
- action is merely navigation/help;
- action is destructive without an explicit destructive confirmation treatment.

---

## 6.2 Secondary Button

### Function

Alternative or lower-priority action paired with a primary action.

### Visual specification

| Property | Specification |
|---|---|
| Height | 56dp |
| Horizontal padding | `Space.XL` — 20dp |
| Gap | `Space.SM` — 8dp |
| Radius | `Radius.MD` — 12dp |
| Background | `Surface.Raised` |
| Border | `Border.Thin` + `Border.Default` |
| Text | `Text.Primary` |
| Typography | `labelLarge` |
| Icon | `Icon.SM` — 20dp |
| Glow | none |

### States

- pressed: semantic tint using `Opacity.Tint`;
- focused: `Border.Strong` + `Brand.Primary`;
- disabled: `Text.Disabled` + neutral border;
- loading: same geometry, inline/center progress, no click.

### Use when

- cancel, later, alternative flow, or lower-priority confirmation.

### Do not use when

- the action should be visually invisible/minimal; use Text Button instead.

---

## 6.3 Text Button

### Function

Low-emphasis action without a persistent container.

### Visual specification

| Property | Specification |
|---|---|
| Minimum touch target | 48 × 48dp |
| Horizontal padding | `Space.MD` — 12dp |
| Gap | `Space.SM` — 8dp |
| Text | `Text.Secondary`; `Brand.Primary` for intentional action emphasis |
| Typography | `labelMedium` or `labelLarge` according to hierarchy |
| Icon | `Icon.SM` — 20dp |
| Background | transparent |
| Radius | `Radius.MD` for press/focus hit surface |

### States

- pressed: `Opacity.Tint` background;
- disabled: `Text.Disabled`;
- loading: only when the action initiates asynchronous work; preserve 48dp target.

### Use when

- tertiary action, retry, skip, learn more, or inline action.

### Do not use when

- it is the only critical action on the surface.

---

## 6.4 Icon Button

### Function

Compact action represented by a governed icon.

### Visual specification

| Property | Specification |
|---|---|
| Touch target | 48 × 48dp |
| Default icon | `Icon.MD` — 24dp |
| Compact visual icon | `Icon.SM` — 20dp where justified |
| Radius | `Radius.Full` for circular hit feedback or `Radius.MD` for square container variant |
| Default icon color | `Text.Secondary` |
| High-emphasis | `Text.Primary` |
| Active | semantic accent, normally `Brand.Active` |
| Destructive | `Status.Error` |

### States

- pressed: `Opacity.Tint` surface;
- selected: icon accent + tint surface; selection must not rely on color alone when state matters;
- disabled: `Text.Disabled`;
- loading: spinner replaces icon only if action itself is pending.

### RTL

Directional icons must use AutoMirrored equivalents.

### Do not use when

- icon meaning is ambiguous without a label in the current context.

---

## 6.5 FAB

### Function

One dominant floating creation/add action.

### Visual specification

| Property | Specification |
|---|---|
| Container | 56 × 56dp |
| Radius | `Radius.Full` |
| Background | `Brand.Primary` |
| Icon | `Icon.LG` — 28dp |
| Icon color | `Text.OnBrand` |
| Glow | `Glow.Strong`, `Brand.Primary` only |
| Press scale | ~0.97 |
| Motion | `Motion.Fast` |

### States

- disabled FAB is not preferred; hide or present a clear disabled state only when product logic requires visibility;
- loading may replace icon with progress while preserving size;
- no badge inside FAB.

### Use when

- one persistent create/add action is central to the current navigation shell.

### Do not use when

- more than one floating primary action exists on the same screen.

---

# 7. Inputs

## 7.1 Text Field

### Function

General text entry.

### Visual specification

| Property | Specification |
|---|---|
| Single-line minimum height | 56dp |
| Multiline minimum height | 112dp |
| Horizontal padding | `Space.LG` — 16dp |
| Internal vertical padding | `Space.MD` — 12dp |
| Radius | `Radius.MD` — 12dp |
| Surface | `Surface.Raised` |
| Default border | `Border.Thin` + `Border.Default` |
| Focus border | `Border.Strong` + `Brand.Primary` |
| Error border | `Border.Strong` + `Status.Error` at `Opacity.High` |
| Input text | `bodyLarge`, `Text.Primary` |
| Label/support text | `bodySmall` / `labelSmall`, `Text.Secondary` |
| Placeholder | `Text.Disabled` |
| Leading/trailing icon | `Icon.SM` — 20dp |

### States

- focused: strong primary border, cursor `Brand.Primary`;
- error: status error border + support message; do not rely on color alone;
- disabled: `Text.Disabled`, neutral border, no cursor;
- read-only: normal readable text, neutral container, no editable cursor;
- loading is normally not owned by a text field; use trailing progress only for validation/search situations explicitly specified by the caller.

### RTL

- Arabic text is RTL and start-aligned.
- technical strings use local LTR only where necessary.

---

## 7.2 Search Field

### Function

Search/query input for list or result surfaces.

### Visual specification

| Property | Specification |
|---|---|
| Height | 52dp |
| Horizontal padding | `Space.LG` — 16dp |
| Gap | `Space.SM` — 8dp |
| Radius | `Radius.MD` — 12dp |
| Surface | `Surface.Raised` |
| Border | `Border.Thin` + `Border.Default` |
| Focus border | `Border.Accent` + `Brand.Primary` |
| Search icon | `Icon.MD` — 24dp |
| Clear icon target | 48dp, visual icon `Icon.SM` |
| Text | `bodyMedium` or `bodyLarge` |

### States

- default empty;
- focused;
- populated with clear action;
- searching: optional trailing spinner;
- disabled where search is unavailable.

### Rules

- search results are not part of this component;
- debouncing/query execution belongs outside DS.

---

## 7.3 Numeric Field

### Function

Amounts, counts, phone numbers, OTP-like numeric values where a visible field is appropriate.

### Visual specification

Uses Text Field geometry and states, with these differences:

- numeric value subtree is LTR;
- numeric keyboard is requested by the caller;
- value uses Tajawal numeric glyphs unless the component is explicitly Instrument Number;
- optional unit/prefix/suffix remains semantically positioned outside the LTR value where necessary;
- no reversal of digit order in RTL.

### Use when

- a normal editable numeric value is required.

### Do not use when

- OTP needs segmented boxes; that belongs to an auth-specific pattern/component later if still required;
- seven-segment display is non-editable; use Instrument Number.

---

## 7.4 Selection Field

### Function

Generic visual control for selecting one value from caller-provided options.

### Reason for inclusion

The existing `SpecialtyPicker` is reusable visually but incorrectly owns workshop-specific options. V1 keeps the selection visual and removes domain ownership.

### Visual specification

- uses Text Field geometry;
- read-only text entry surface;
- trailing dropdown icon `Icon.SM` or `Icon.MD`;
- options, labels, identifiers, selected value, and callbacks are supplied by the feature;
- menu surface: `Surface.Raised`/`Surface.Overlay`, radius `Radius.MD`, border `Border.Thin`;
- selected option may use `Brand.Active` text/icon plus a secondary selection signal.

### Forbidden

- no workshop specialty list in Design System;
- no feature enum or repository lookup inside the component.

---

# 8. Containers

## 8.1 Base Card

### Function

Default reusable content container.

### Visual specification

| Property | Specification |
|---|---|
| Surface | `Surface.Base` |
| Border | `Border.Thin` + `Border.Default` |
| Radius | `Radius.LG` — 16dp |
| Default padding | `Space.LG` — 16dp |
| Internal gap | `Space.MD` or `Space.LG` |
| Glow | none |
| Shadow | none by default |

### States

- non-interactive card has no press state;
- clickable variant uses tint on press, no scale;
- selected variant must use accent border + secondary signal;
- disabled interactive card uses disabled content and no press behavior.

### Do not use when

- the card is the primary hero/highlight; use Highlight Card.

---

## 8.2 Metric Card

### Function

Compact display of one metric with label and optional status/context.

### Visual specification

| Property | Specification |
|---|---|
| Minimum height | 96dp |
| Surface | `Surface.Raised` |
| Border | `Border.Thin` + `Border.Default` |
| Radius | `Radius.LG` — 16dp |
| Padding | `Space.LG` — 16dp |
| Label | `labelMedium`, `Text.Secondary` |
| Value | `displaySmall` by default |
| Secondary value | `bodySmall`/`labelSmall` |
| Optional icon | `Icon.SM` or `Icon.MD` |
| Glow | none by default |

### Variants

- **Default:** neutral metric.
- **Accent:** one semantic brand/status accent on value or icon.
- **Clickable:** press tint only; navigation handled externally.

### Rule

`MiniStatCard` is superseded by this component and does not survive as a separate V1 component.

---

## 8.3 Highlight Card

### Function

Elevated visual priority for one important insight, action, achievement, or hero-support item.

### Visual specification

| Property | Specification |
|---|---|
| Surface | `Surface.Raised` |
| Radius | `Radius.XL` — 20dp |
| Border | `Border.Accent` with one semantic accent at `Opacity.High` |
| Padding | `Space.XL` — 20dp |
| Glow | `Glow.Soft` or `Glow.Medium`; never multiple colors |
| Title | `titleLarge`/`headlineSmall` |
| Body | `bodyMedium` |

### Rules

- one semantic accent per card;
- glow is optional, never necessary to understand meaning;
- avoid multiple Highlight Cards competing in one viewport.

---

## 8.4 Alert Card

### Function

Inline success, warning, error, or information message requiring attention.

### Visual specification

| Property | Specification |
|---|---|
| Surface | `Surface.Raised` |
| Radius | `Radius.LG` — 16dp |
| Border | `Border.Accent` using semantic status at `Opacity.High` |
| Optional tint | semantic status at `Opacity.Tint` |
| Padding | `Space.LG` — 16dp |
| Icon | `Icon.MD` — 24dp |
| Title | `titleMedium` |
| Body | `bodyMedium` |
| Glow | none |

### Variants

`Success`, `Warning`, `Error`, `Info`.

### Rules

- state is communicated by icon/label/border, not color alone;
- destructive action inside Alert Card uses explicit action copy and is not implied by card color.

---

# 9. Navigation

## 9.1 Bottom Navigation

### Function

Primary destination switcher for the app shell.

### Ownership contract

The component receives:

- items;
- selected item identifier;
- unread/badge values;
- click callbacks;
- optional center action slot/FAB configuration.

It does **not** obtain unread values itself.

### Visual specification

| Property | Specification |
|---|---|
| Base bar height | 72dp excluding system navigation inset |
| Surface | `Surface.Base` |
| Top/outer radius | `Radius.2XL` — 24dp where container shape is visible |
| Border | `Border.Thin` + `Border.Default` |
| Item touch target | minimum 48dp |
| Icon | `Icon.MD` — 24dp |
| Label | `labelSmall` — 11sp |
| Default item | `Text.Secondary` |
| Selected item | `Brand.Active` + selected indicator/tint |
| Selected tint | `Brand.Active` at `Opacity.Tint` |
| Center action | separate FAB contract |

### Selected state

Must use at least two signals: selected color + indicator/tint/label emphasis.

### Badge

Consumes the Badge component; unread count is passed from outside.

### RTL

Item order follows the screen shell specification; individual labels/icons align naturally. No internal forced LTR.

### Critical migration rule

Current `BottomNavBadgeViewModel` and `hiltViewModel()` lookup are forbidden inside the V1 visual component.

---

## 9.2 Top Header

### Function

Standard screen header without mandatory back navigation.

### Visual specification

| Property | Specification |
|---|---|
| Content height | 64dp excluding status-bar inset |
| Horizontal padding | `Space.LG` — 16dp |
| Gap | `Space.SM`/`Space.MD` |
| Surface | transparent on `Surface.Canvas` by default; `Surface.Base` only when screen pattern requires it |
| Title | `headlineMedium` |
| Subtitle | `bodySmall`/`bodyMedium`, `Text.Secondary` |
| Action icon target | 48dp |
| Action icon | `Icon.MD` — 24dp |

### Slots

- title;
- optional subtitle;
- optional start content such as avatar/brand mark;
- up to two direct trailing actions before overflow is preferred.

### Rules

- no business state retrieval;
- home-specific hero content is not embedded here; that belongs to a pattern.

---

## 9.3 Back Header

### Function

Header for child/detail screens with a clear return action.

### Visual specification

| Property | Specification |
|---|---|
| Content height | 56dp excluding status-bar inset |
| Horizontal padding | `Space.SM` to `Space.LG` according to containing screen gutter |
| Back target | 48 × 48dp |
| Back icon | AutoMirrored, `Icon.MD` — 24dp |
| Title | `headlineSmall` or `titleLarge` according to density |
| Optional trailing action | one 48dp Icon Button |

### RTL

The back icon must be AutoMirrored. No manually rotated arrow.

---

# 10. Feedback

## 10.1 Badge

### Function

Small count or presence marker attached to an icon/navigation item.

### Variants

- **Dot:** 8dp, no text.
- **Count:** minimum 18dp height/width; horizontal expansion for 2–3 characters.

### Visual specification

| Property | Specification |
|---|---|
| Default count background | `Brand.Primary` |
| Text | `Text.OnBrand` |
| Typography | `labelSmall` |
| Radius | `Radius.Full` |
| Maximum display | `99+` |

### Rules

- badge indicates quantity/presence only;
- status meaning uses Status Chip/Indicator instead.

---

## 10.2 Status Chip

### Function

Compact textual state label.

### Visual specification

| Property | Specification |
|---|---|
| Height | 32dp |
| Horizontal padding | `Space.MD` — 12dp |
| Gap | `Space.XS` — 4dp |
| Radius | `Radius.Full` |
| Background | semantic status at `Opacity.Tint` |
| Border | `Border.Thin`, semantic status at `Opacity.Muted` |
| Text | semantic status color |
| Typography | `labelMedium` |
| Optional icon | `Icon.XS` — 16dp |

### Variants

`Success`, `Warning`, `Error`, `Info`, plus neutral state using `Text.Secondary`/`Border.Default`.

### Rule

Chip is descriptive, not a primary action.

---

## 10.3 Snackbar

### Function

Temporary non-blocking feedback.

### Visual specification

| Property | Specification |
|---|---|
| Minimum height | 56dp |
| Surface | `Surface.Overlay` |
| Radius | `Radius.MD` — 12dp |
| Border | `Border.Thin` + `Border.Default` |
| Padding | `Space.LG` — 16dp |
| Text | `bodyMedium`, `Text.Primary` |
| Optional action | Text Button semantics with `Brand.Primary` |
| Optional icon | `Icon.SM` — 20dp |

### Rules

- max message length should remain concise; long explanations belong in dialog/screen;
- only one optional action;
- timing/dismiss policy belongs to host/application layer.

---

## 10.4 Dialog

### Function

Blocking confirmation, decision, warning, or concise information surface.

### Visual specification

| Property | Specification |
|---|---|
| Width | screen width minus `Space.3XL` total minimum gutter; max target 360dp |
| Surface | `Surface.Raised` |
| Radius | `Radius.XL` — 20dp |
| Border | `Border.Thin` + `Border.Default` |
| Padding | `Space.2XL` — 24dp |
| Title | `headlineSmall` |
| Body | `bodyMedium` |
| Action gap | `Space.SM` — 8dp |
| Modal shadow | allowed |
| Glow | none |

### Variants

- information;
- confirmation;
- destructive confirmation.

### Ownership

Dialog receives title/body/content/actions. Fixed permission copy is not owned by DS.

### Rules

- no nested dialog;
- avoid using dialog for a long form or long-scrolling workflow.

---

## 10.5 Bottom Sheet

### Function

Contextual action/details/form surface anchored from the bottom.

### Visual specification

| Property | Specification |
|---|---|
| Surface | `Surface.Raised` |
| Top radius | `Radius.2XL` — 24dp |
| Border | optional top/outer `Border.Thin` + `Border.Default` |
| Content padding | `Space.LG`/`Space.XL` |
| Drag handle | 32 × 4dp, `Text.Disabled`, `Radius.Full` |
| Max default height | approximately 90% of available window before full-screen behavior is considered |
| Title | `headlineSmall` |

### Rules

- sheet owns presentation only;
- business validation/loading is supplied from caller state;
- long flows that become screen-like must be resolved in Screen Specs.

---

## 10.6 Loading State

### Function

Reusable non-content state while a surface is waiting for data.

### Variants

- **Inline:** progress only or progress + short label.
- **Content:** centered state for an empty screen/body region.

### Visual specification

| Property | Specification |
|---|---|
| Progress size | 32dp content state; 20–24dp inline |
| Progress color | semantic action color, default `Brand.Primary` |
| Label | `bodyMedium`, `Text.Secondary` |
| Gap | `Space.MD` — 12dp |

### Rules

- does not replace button-level loading;
- no fake progress percentage unless actual progress exists;
- full-screen layout composition belongs to Loading Screen pattern in Session 04.

---

## 10.7 Empty State

### Function

Reusable representation for a valid state with no content.

### Visual specification

| Property | Specification |
|---|---|
| Icon/illustration icon | `Icon.Hero` — 40dp |
| Icon color | `Text.Secondary` or one approved semantic accent |
| Title | `headlineSmall` or `titleLarge` |
| Body | `bodyMedium`, `Text.Secondary` |
| Main gap | `Space.MD`/`Space.LG` |
| Optional CTA | Primary or Secondary Button according to action priority |
| Max text alignment | centered only when used as a standalone empty state; list-local empty states may be start-aligned by pattern |

### Rules

- no emoji as default reusable empty-state icon;
- copy and action remain feature-owned;
- Error Screen is a pattern, not this component.

---

# 11. Data display

## 11.1 Avatar

### Function

Visual identity for a person/account using image or initials.

### Sizes

| Variant | Size |
|---|---:|
| Small | 32dp |
| Default | 40dp |
| Large | 48dp |
| Hero | 64dp |

### Visual specification

- shape: circle;
- fallback background: one semantic accent at `Opacity.Subtle`;
- fallback border: same accent at `Opacity.Muted`, `Border.Thin`;
- initial: Tajawal Bold, centered;
- image fills/crops safely inside circle;
- no caller-supplied arbitrary raw color.

### Rules

Feature may provide semantic accent choice, image, initials, or name. Networking/image loading ownership remains outside DS.

---

## 11.2 List Row

### Function

Reusable row for lists with title, supporting text, optional leading visual and trailing metadata/action.

### Visual specification

| Property | Specification |
|---|---|
| Minimum height | 64dp |
| Horizontal padding | `Space.LG` — 16dp |
| Vertical padding | `Space.MD` — 12dp |
| Main gap | `Space.MD` — 12dp |
| Title | `titleMedium` or `bodyLarge` |
| Supporting text | `bodySmall`/`bodyMedium`, `Text.Secondary` |
| Leading visual | 40–48dp slot |
| Trailing icon target | 48dp if interactive |
| Surface | transparent by default; container provided by parent/card/pattern |

### States

- clickable: pressed tint, no scale;
- selected: accent indicator/tint + text/icon signal;
- disabled: disabled semantics;
- loading row is not a default state; skeleton/loading pattern is handled later if needed.

### RTL

Leading is visual start (right). Trailing metadata/action is visual end (left).

---

## 11.3 Section Header

### Function

Title row separating groups of content.

### Visual specification

| Property | Specification |
|---|---|
| Minimum height | 40dp |
| Title | `titleLarge` |
| Subtitle | optional `bodySmall`, `Text.Secondary` |
| Optional action | Text Button / Icon Button semantics |
| Gap below | controlled by parent pattern, normally `Space.SM` or `Space.MD` |

### Rules

- no decorative raw colors;
- if a section needs a larger hero title, it is no longer a simple Section Header.

---

## 11.4 Divider

### Function

Low-emphasis separation when spacing alone is insufficient.

### Visual specification

- thickness: `Border.Thin` — 1dp;
- color: `Border.Default`;
- no glow/shadow;
- full-width or caller-defined semantic inset variant;
- avoid stacking divider + card border unnecessarily.

---

## 11.5 Stat Value

### Function

Governed display of a significant number/amount without defining the whole card.

### Variants

| Variant | Typography | Typical use |
|---|---|---|
| Small | `displaySmall` | secondary metric |
| Medium | `displayMedium` | strong metric |
| Large | `displayLarge` | dashboard hero-support metric |
| Hero | `statXL` | single dominant metric only |

### Visual specification

- default color `Text.Primary`;
- semantic finance/status/brand accent allowed when meaning is real;
- numeric run is local LTR;
- optional unit uses smaller text role and remains semantically ordered;
- formatting/locale/value calculation belongs to caller.

### Rule

Only one `Hero` value should dominate a visual hero area.

---

## 11.6 Status Indicator

### Function

Small non-text state signal used alongside a label/value.

### Visual specification

| Property | Specification |
|---|---|
| Dot size | 8dp |
| Shape | circle |
| Color | `Status.Success`, `Status.Warning`, `Status.Error`, `Status.Info`, or neutral `Text.Secondary` |
| Optional ring | `Border.Thin` only if contrast requires it |

### Motion

No pulse by default. `Motion.Pulse` is permitted only for deliberate live/recording/attention semantics defined by the caller/pattern.

### Rule

Indicator alone must not communicate a critical state without accessible text/content description.

---

## 11.7 Step Indicator

### Function

Progress through a small finite onboarding/setup sequence.

### Visual specification

| Property | Specification |
|---|---|
| Inactive segment | 8 × 8dp |
| Active segment | 32 × 8dp |
| Gap | `Space.SM` — 8dp |
| Radius | `Radius.Full` |
| Active color | `Brand.Active` |
| Completed color | `Brand.Active` at `Opacity.Medium` |
| Inactive color | `Border.Default` |
| Transition | `Motion.Emphasized` — 300ms |

### Ownership

Caller provides current step and total steps. No navigation logic exists inside the component.

---

## 11.8 Instrument Number

### Function

Special technical/brand numeric display based on the existing seven-segment implementation.

### Ownership

This is a specialized data-display component, not general typography.

### Visual specification

- numeric content always LTR;
- semantic color comes from `Brand.Secondary`, `Brand.Active`, or `Instrument.*`;
- inactive segment treatment is component-internal;
- `SegmentDigit`, `SegmentCard`, and `SegmentSeparatorDot` become implementation primitives, not public general-purpose components;
- geometry may keep a documented instrument-only radius exception;
- feature screens provide value/state, not raw segment colors.

### Rules

- do not use for ordinary amounts or form numbers;
- do not expose internal geometry/color escape hatches to feature screens.

---

# 12. Current component disposition map

This section resolves how current `core:designsystem` components map into V1.

| Current component | V1 decision | Target |
|---|---|---|
| `AutoDriveButton` | redesign/retain concept | Primary Button + Secondary Button |
| `AutoDriveTextButton` | redesign/retain | Text Button |
| `AutoDriveTextField` | redesign/expand | Text Field + Numeric Field |
| `SpecialtyPicker` | remove domain ownership | generic Selection Field; options remain in feature |
| `AutoDriveCard` | redesign/retain | Base Card |
| `MiniStatCard` | merge; no separate V1 component | Metric Card |
| `UserAvatar` | redesign/retain | Avatar |
| `StepIndicator` | retain | Step Indicator |
| `AutoDriveBottomBar` | redesign and decouple | Bottom Navigation |
| `BottomNavBadgeViewModel` | remove from DS ownership | caller/app shell supplies state |
| `BottomNavBadgeSource` | remove from DS ownership | notifications/application layer |
| `PermissionsDeniedDialog` | remove fixed copy from DS | generic Dialog; permission copy stays feature/app-owned |
| `SevenSegmentNumber` | retain under formal scope | Instrument Number |
| `SegmentDigit` | internalize | Instrument Number implementation primitive |
| `SegmentCard` | internalize | Instrument Number implementation primitive |
| `SegmentSeparatorDot` | internalize | Instrument Number implementation primitive |
| `DonutChart` | not admitted to V1 component library now | keep untouched until Session 10; remove if still unused |
| `AutoDriveLogo` | not a reusable UI component | retain asset until Session 06 decides resource ownership |

---

# 13. Decisions on Session 01 open component questions

## 13.1 `MiniStatCard`

Resolved: merge into Metric Card. Separate component is unnecessary.

## 13.2 `DonutChart`

Resolved for V1 component scope: not part of the approved initial library because it has zero active usage and no stable chart contract yet. It remains untouched until consolidation; if a later screen specification proves a real need, a chart component may be specified deliberately.

## 13.3 `AutoDriveLogo`

Resolved at component level: it is a brand asset/presentation resource, not a generic component. Physical resource ownership remains deferred to Session 06.

## 13.4 `SpecialtyPicker`

Resolved: workshop data leaves DS; only the generic selection visual survives.

## 13.5 Bottom navigation unread state

Resolved at component level: unread/badge state must be passed in. Exact application-state owner remains an architecture decision for Session 06.

---

# 14. Component boundaries — what belongs to Patterns instead

The following are intentionally **not** components in Session 03 because they combine several components and define screen-level structure:

- Screen Header composition;
- Dashboard Hero;
- Metric Summary group;
- Conversation Item composition;
- Transaction Row composition;
- Pending Request Card composition;
- Settings Group;
- Settings Row;
- Report Stat Tile;
- Media Action Group;
- Search Results List;
- Empty Screen;
- Error Screen;
- Loading Screen.

These belong to Session 04.

---

# 15. Accessibility and interaction requirements

Every implemented component in Session 07 must satisfy:

1. interactive target >= 48dp;
2. content descriptions for icon-only actions;
3. state not communicated by color alone when state is important;
4. text uses approved typography and remains legible under normal Android font scaling;
5. disabled controls remain identifiable but clearly inactive;
6. loading state does not create duplicate actionable controls;
7. touch feedback is visible but does not move layout;
8. directional icons mirror correctly in RTL;
9. local LTR applies only to numeric/technical islands;
10. semantic labels/copy are supplied by features where domain-specific.

---

# 16. Naming target for implementation

Session 07 may choose exact Kotlin names, but V1 naming should follow one coherent family rather than preserve legacy names blindly.

Recommended conceptual names:

```text
AutoDrivePrimaryButton
AutoDriveSecondaryButton
AutoDriveTextButton
AutoDriveIconButton
AutoDriveFab

AutoDriveTextField
AutoDriveSearchField
AutoDriveNumericField
AutoDriveSelectionField

AutoDriveCard
AutoDriveMetricCard
AutoDriveHighlightCard
AutoDriveAlertCard

AutoDriveBottomNavigation
AutoDriveTopHeader
AutoDriveBackHeader

AutoDriveBadge
AutoDriveStatusChip
AutoDriveSnackbarContent
AutoDriveDialog
AutoDriveBottomSheet
AutoDriveLoadingState
AutoDriveEmptyState

AutoDriveAvatar
AutoDriveListRow
AutoDriveSectionHeader
AutoDriveDivider
AutoDriveStatValue
AutoDriveStatusIndicator
AutoDriveStepIndicator
AutoDriveInstrumentNumber
```

This is a naming target, not implementation in Session 03.

---

# 17. Forbidden from this point forward

1. No component may call `hiltViewModel()` or instantiate application state ownership internally.
2. No ViewModel, Repository, Flow source, or domain list inside `core:designsystem` components.
3. No new raw colors in component APIs or feature calls.
4. No arbitrary radius/border/spacing/color parameters used as permanent public customization escape hatches.
5. No separate `MiniStatCard` in V1; use Metric Card.
6. No workshop-specific `SpecialtyPicker` data inside DS.
7. No fixed permission/business copy inside generic Dialog.
8. No generic public exposure of seven-segment internal primitives.
9. No badge data retrieval inside Bottom Navigation.
10. No generic card glow by default.
11. No UI icon emoji as reusable action/status/navigation controls.
12. No screen migration or business-logic modification before later sessions.
13. No new component category may silently introduce a new foundation token.

---

# 18. Deferred

- exact Kotlin APIs and package placement;
- implementation of tokens/components;
- previews;
- architecture relocation of resources;
- application owner of unread navigation state;
- exact New Chat container form;
- screen-specific combinations and layout patterns;
- screen migration;
- visual QA;
- removal of unused legacy code;
- any future chart system if screen specs prove a real requirement.

---

# 19. Open Issues

```text
- New Chat final container: dialog vs sheet vs screen — Session 05.
- Feature image/resource ownership — Session 06.
- Exact application owner of bottom-navigation unread state — Session 06.
- Exact Kotlin public APIs and package structure — Sessions 06/07.
- Whether a chart component is actually required — re-evaluate from Session 05 screen specs; consolidate in Session 10.
- Pixel-level dimensions that depend on approved screen composition — Sessions 05/09.
```

None of these block Session 04 Pattern Specification.

---

# 20. Next Session Input

Session 04 must treat this document and the already-approved Foundations as the authoritative component vocabulary.

The key composition rule is:

```text
Foundations
   ↓
Components defined here
   ↓
Patterns in Session 04
   ↓
Screen specifications in Session 05
```

Session 04 must build repeated structures by composing these components instead of inventing new screen-local visual primitives.

---

# 21. Session close

```text
STATUS: APPROVED

Decisions:
- Initial V1 component library is fully specified across actions, inputs, containers, navigation, feedback, and data display.
- Bottom Navigation is presentation-only and receives badge state externally.
- SpecialtyPicker domain data leaves Design System; generic Selection Field remains.
- MiniStatCard merges into Metric Card.
- DonutChart is not admitted to V1 unless a later verified screen need appears.
- AutoDriveLogo is a brand asset, not a generic component.
- Seven-segment display becomes governed Instrument Number; internals become private implementation primitives.
- Step Indicator remains an approved reusable component.

Forbidden:
- No application/business state inside DS components.
- No unmanaged visual constants or arbitrary style escape hatches.
- No screen migration or Kotlin implementation in Session 03.

Deferred:
- Patterns, screen specs, architecture, Kotlin implementation, migration, QA, final cleanup.

Open Issues:
- New Chat container form, resource ownership, unread-state architecture owner, exact APIs, chart need, pixel QA.

Next Session Input:
- 03_COMPONENT_SPEC.md becomes the source of truth for Session 04 once approved.
```

**Approval gate:** do not begin Session 04 until the user explicitly approves this document by continuing to Session 04 or otherwise accepting it.


---

## Approval record

Session 03 was approved by the user by explicitly instructing execution of Session 04. No production code was changed during Session 03.
