# AutoDrive Design System — Session 02: Foundations

**Output:** `02_FOUNDATIONS.md`  
**Session:** 02 — Foundations  
**Input source of truth:** `01_DESIGN_AUDIT.md` (`STATUS: APPROVED`)  
**Implementation changes:** None  
**Production code changes:** None  
**STATUS:** APPROVED

---

## 1. Purpose

This document locks the visual foundations for AutoDrive Design System V1 before component specification begins.

It resolves the foundation-level problems identified by Session 01:

- semantic color ownership;
- typography scale;
- spacing;
- radius;
- border widths and opacity;
- glow/shadow;
- icon sizing and style;
- interaction/motion timing;
- disabled/pressed/focus behavior;
- RTL/LTR rules;
- special numeric/instrument display rules.

This session does **not** implement Kotlin tokens or migrate any screen. Implementation belongs to later sessions.

---

# 2. Foundation strategy

AutoDrive V1 uses a **semantic token system**, not feature-named visual constants.

The hierarchy is:

```text
Raw value
   ↓
Foundation semantic token
   ↓
Component token / variant
   ↓
Pattern
   ↓
Screen
```

A screen must never decide a raw color, radius, border, typography size, or animation duration when a foundation token can express the intent.

### Governing principle

A token answers **why the value exists**, not merely what value it contains.

Good:

```text
Brand.Primary
Status.Success
Surface.Raised
Text.Secondary
Border.Default
```

Forbidden as final global names:

```text
GreenWithdraw
GoldPending
GrayPaid
```

Financial states may currently share the same raw values as visual semantic tokens, but they remain separate semantic roles so they can diverge safely later.

---

# 3. Theme mode

## Decision: Dark-only for Design System V1

AutoDrive V1 is intentionally **Dark-only**.

Evidence:

- the current `AutoDriveTheme` defines only a dark scheme;
- the approved Home visual reference is dark;
- the approved visual language depends on deep dark surfaces, luminous borders, glow, orange/gold highlights, and mint active states.

### Rule

No Light Mode tokens or Light Mode component variants are required in V1.

Light Mode is a future product decision and must not be simulated by inverting the current palette.

---

# 4. Color foundations

## 4.1 Core surfaces

These values retain the stable central palette already present in the project while renaming them semantically.

| Semantic token | Hex | Current source | Purpose |
|---|---:|---|---|
| `Surface.Canvas` | `#08090C` | `BgDeep` | application/background canvas |
| `Surface.Base` | `#0F1117` | `BgSurface1` | default cards, bars, sheets |
| `Surface.Raised` | `#161820` | `BgSurface2` | emphasized cards/containers |
| `Surface.Overlay` | `#1C1F2A` | `BgSurface3` | nested controls, selected sub-surfaces |
| `Border.Default` | `#232635` | `BorderColor` | standard neutral border/divider |

### Surface rules

1. `Surface.Canvas` is the screen background.
2. `Surface.Base` is the default component surface.
3. `Surface.Raised` is used when a component needs stronger hierarchy without glow.
4. `Surface.Overlay` is used for nested/interactive sub-surfaces, not as another arbitrary card color.
5. New raw dark colors are forbidden in screens.

---

## 4.2 Brand and interactive colors

| Semantic token | Hex | Source | Purpose |
|---|---:|---|---|
| `Brand.Primary` | `#FF6B00` | current `OrangeAccent` | primary AutoDrive action / strongest brand action |
| `Brand.Secondary` | `#FFB547` | current `GoldPending` | highlighted data, secondary brand emphasis |
| `Brand.Active` | `#4FFFB0` | current `GreenWithdraw` | active/selected state when the visual language calls for mint |
| `Brand.Info` | `#4F8FFF` | current `AccentBlue` | informational emphasis |
| `Brand.Insight` | `#DB9AF3` | approved Home reference | insight/idea accent only |
| `Brand.WhatsApp` | `#25D366` | current `WhatsAppGreen` | WhatsApp identity only |

### Brand rules

- Orange is the primary branded action color in V1.
- Gold is a secondary highlight and large-data accent, not a replacement for warning semantics.
- Mint is the preferred active/success visual accent, including selected navigation where specified by components.
- Blue is informational, not a generic decoration color.
- Purple is scoped to insight/idea semantics. It is not a generic third accent.
- WhatsApp green must never be reused for non-WhatsApp UI.

`Brand.Insight` is a Session 02 canonicalization derived from the approved Home reference because the current code did not contain a formal purple token.

---

## 4.3 Status colors

Status semantics are independent from brand semantics even when values currently match.

| Token | Hex | Meaning |
|---|---:|---|
| `Status.Success` | `#4FFFB0` | completed, healthy, available, success |
| `Status.Warning` | `#FFB547` | pending, attention, caution |
| `Status.Error` | `#FF5252` | failure, destructive action, error |
| `Status.Info` | `#4F8FFF` | neutral information |

### Rule

A component receives a **status role**, not a color chosen by the feature.

---

## 4.4 Financial state colors

Financial states remain explicit domain semantics and do not own the global visual identity.

| Token | Hex | Current equivalent |
|---|---:|---|
| `Finance.Withdrawable` | `#4FFFB0` | `GreenWithdraw` |
| `Finance.Pending` | `#FFB547` | `GoldPending` |
| `Finance.Paid` | `#8890A8` | `GrayPaid` |

These may share values with `Status.*` today. They are still separate tokens.

---

## 4.5 Text colors

| Token | Hex | Purpose |
|---|---:|---|
| `Text.Primary` | `#EFF1F7` | primary text / high-emphasis content |
| `Text.Secondary` | `#8890A8` | secondary descriptions, metadata |
| `Text.Disabled` | `#4A5068` | disabled controls/content |
| `Text.OnBrand` | `#08090C` | text/icon placed on bright brand containers |

### Text color rules

- Ordinary body text does not use status or brand colors.
- Brand/status colors may be used for a short value, badge, title, or state indicator when semantic meaning exists.
- Disabled text uses `Text.Disabled`; do not create disabled text by random alpha overrides.

---

## 4.6 Special instrument palette

The Home fuel/LED visual is intentionally more technical than standard application UI. Its palette is scoped to reusable **instrument/display components** only.

### Gauge semantic colors

| Token | Hex | Current source |
|---|---:|---|
| `Instrument.Full` | `#22C55E` | `HomeHeroComponents.kt` |
| `Instrument.Good` | `#84CC16` | `HomeHeroComponents.kt` |
| `Instrument.Caution` | `#F59E0B` | `HomeHeroComponents.kt` |
| `Instrument.Low` | `#F97316` | `HomeHeroComponents.kt` |
| `Instrument.Empty` | `#EF4444` | `HomeHeroComponents.kt` |
| `Instrument.Track` | `#1F2937` | `HomeHeroComponents.kt` |
| `Instrument.Muted` | `#6B7280` | `HomeHeroComponents.kt` |

### Instrument internal surfaces

The following existing raw values are permitted only inside the future Instrument/LED implementation, never directly in screens:

```text
#020409
#050805
#050810
#0A0A0A
#0A1208
#1A1F2A
```

They are **effect-local implementation colors**, not general application surfaces.

### Rule

A feature screen may request an instrument state such as `Good`, `Low`, or `Empty`; it must not select these raw colors itself.

---

# 5. Material color-role mapping target

When implemented in Session 07, the Material3 scheme should map semantically as follows:

```text
primary          → Brand.Primary
onPrimary        → Text.OnBrand
secondary        → Brand.Secondary
onSecondary      → Text.OnBrand
tertiary         → Brand.Info
background       → Surface.Canvas
surface          → Surface.Base
surfaceVariant   → Surface.Raised
onBackground     → Text.Primary
onSurface        → Text.Primary
onSurfaceVariant → Text.Secondary
outline          → Border.Default
error            → Status.Error
```

This replaces the current accidental mapping where financial-state names act as general Material colors.

---

# 6. Opacity foundations

The current project contains many one-off alpha values. V1 reduces them to a governed scale.

| Token | Alpha | Use |
|---|---:|---|
| `Opacity.Full` | `1.00` | normal content |
| `Opacity.High` | `0.72` | high-but-not-primary emphasis |
| `Opacity.Medium` | `0.60` | secondary icon/border emphasis |
| `Opacity.Muted` | `0.40` | subdued state |
| `Opacity.Subtle` | `0.20` | soft accent/background |
| `Opacity.Tint` | `0.12` | selected/pressed tint |
| `Opacity.Ghost` | `0.06` | very subtle decorative effect |

### Rules

- Do not create new alpha values in feature screens.
- Component specification may select from this scale.
- Special rendering primitives such as seven-segment inactive segments may keep a component-internal exception when visually required; the exception must be owned by that component, not by screens.

---

# 7. Typography foundations

## 7.1 Font family

**Primary font: Tajawal**

Keep the four bundled weights:

```text
Regular    → 400
Medium     → 500
Bold       → 700
ExtraBold  → 800
```

No additional font family is introduced for ordinary UI.

---

## 7.2 Core typography scale

The existing central typography is the most mature current foundation and is retained.

| Role | Size | Line height | Weight | Letter spacing |
|---|---:|---:|---|---:|
| `displayLarge` | 48sp | 56sp | ExtraBold | -0.5sp |
| `displayMedium` | 36sp | 44sp | Bold | default |
| `displaySmall` | 28sp | 36sp | Bold | default |
| `headlineLarge` | 24sp | 32sp | Bold | default |
| `headlineMedium` | 20sp | 28sp | Bold | default |
| `headlineSmall` | 18sp | 26sp | Medium | default |
| `titleLarge` | 16sp | 24sp | Medium | default |
| `titleMedium` | 14sp | 20sp | Medium | 0.1sp |
| `titleSmall` | 13sp | 20sp | Medium | default |
| `bodyLarge` | 16sp | 24sp | Regular | default |
| `bodyMedium` | 14sp | 20sp | Regular | default |
| `bodySmall` | 12sp | 16sp | Regular | default |
| `labelLarge` | 16sp | 20sp | Bold | 0.5sp |
| `labelMedium` | 13sp | 16sp | Medium | default |
| `labelSmall` | 11sp | 16sp | Medium | default |

---

## 7.3 Large numeric extension

One additional semantic display role is approved because the current Balance screen and approved dashboard direction require a stronger hero number than ordinary Material roles.

| Role | Size | Line height | Weight | Usage |
|---|---:|---:|---|---|
| `statXL` | 60sp | 68sp | ExtraBold | one dominant financial/metric value per hero only |

### Rules

- `statXL` is not a generic title size.
- Use `displayLarge` (48sp) for ordinary large dashboard values.
- Use `displayMedium` (36sp) and `displaySmall` (28sp) for secondary metrics.
- Local `60.sp`, `52.sp`, `40.sp`, etc. are forbidden unless the Component Specification defines a named special display role.

---

## 7.4 Seven-segment / instrument numbers

Seven-segment rendering remains an intentional brand primitive.

It is **not** represented as a standard `TextStyle` because it is drawn geometrically.

Rules:

1. It always renders numeric content LTR.
2. Its color comes from `Brand.Secondary`, `Brand.Active`, or `Instrument.*` roles.
3. Digit dimensions belong to the instrument component specification, not feature screens.
4. Inactive segments remain a component-internal optical treatment.

---

## 7.5 Typography guardrails

- General-purpose UI text must not be smaller than `labelSmall` (11sp).
- `9sp` legacy navigation labels are not part of V1 foundations.
- Screen titles use headline roles.
- Card titles use title roles.
- Body copy uses body roles.
- Buttons/controls use label roles.
- Arbitrary `FontWeight` overrides in features are forbidden after migration.
- Ordinary numbers use Tajawal; only the instrument system may use geometric seven-segment rendering.

---

# 8. Spacing foundations

## 8.1 Base grid

AutoDrive uses a **4dp base spacing grid**.

A 2dp token exists only for optical/micro adjustment.

| Token | Value | Typical intent |
|---|---:|---|
| `Space.Optical` | 2dp | micro optical correction only |
| `Space.XS` | 4dp | tight internal spacing |
| `Space.SM` | 8dp | icon/text or compact row gap |
| `Space.MD` | 12dp | compact component padding |
| `Space.LG` | 16dp | default screen/card padding |
| `Space.XL` | 20dp | emphasized internal separation |
| `Space.2XL` | 24dp | section separation |
| `Space.3XL` | 32dp | large section gap |
| `Space.4XL` | 40dp | major layout separation |
| `Space.5XL` | 48dp | large structural spacing |
| `Space.6XL` | 64dp | rare hero/layout spacing |

### Evidence

The current code strongly clusters around 4, 8, 12, 16, 20, 24, 32, 40, and 48dp. This scale formalizes those dominant values rather than inventing a different rhythm.

### Rules

- `6dp`, `10dp`, `14dp`, `18dp`, etc. are no longer general spacing tokens.
- Such values may still exist as component geometry if Session 03 explicitly specifies them, but screens cannot use them as free spacing decisions.
- Default compact-screen horizontal gutter target is `Space.LG` (16dp); responsive exceptions belong to Screen Specs.

---

# 9. Radius foundations

The current project has too many nearly identical radii. V1 reduces them to a smaller intentional family.

| Token | Value | Primary purpose |
|---|---:|---|
| `Radius.None` | 0dp | square / structural only |
| `Radius.SM` | 8dp | compact internal control |
| `Radius.MD` | 12dp | fields, buttons, small cards |
| `Radius.LG` | 16dp | default cards |
| `Radius.XL` | 20dp | highlighted cards/sheets |
| `Radius.2XL` | 24dp | hero/nav/large containers |
| `Radius.Full` | 999dp | pills, circular-like capsules |

### Special exception

The seven-segment mini card may retain a small instrument-only radius if required by its visual specification. That value does not become a general application radius.

### Rules

- Legacy 14/18/22dp radii must migrate to the nearest semantic component radius during Session 08.
- A component may expose a named variant, but a feature may not supply arbitrary radii.

---

# 10. Border foundations

## 10.1 Widths

| Token | Width | Use |
|---|---:|---|
| `Border.Thin` | 1dp | default outline/divider |
| `Border.Accent` | 1.5dp | highlighted or selected container |
| `Border.Strong` | 2dp | focus/error/strong emphasis when required |

No additional border width should be introduced without a documented component requirement.

---

## 10.2 Border color/state rules

| State | Border rule |
|---|---|
| Default | `Border.Default` at full token color |
| Soft accent | semantic accent color at `Opacity.Muted` (40%) |
| Selected / active | semantic accent color at `Opacity.High` (72%) |
| Focused | `Brand.Primary`, `Border.Strong` |
| Warning | `Status.Warning` at `Opacity.High` |
| Error / destructive | `Status.Error` at `Opacity.High` |
| Disabled | `Border.Default`; no glow |

Borders communicate hierarchy/state. They are not decoration to be added independently on every surface.

---

# 11. Glow and shadow foundations

AutoDrive's approved visual identity uses **controlled luminous glow**, not generic Material elevation on every card.

## 11.1 Shadow

Standard cards should normally use:

```text
surface contrast + border
```

rather than drop shadow.

Shadow is reserved for:

- modal surfaces;
- floating controls;
- the central FAB;
- temporary overlays requiring separation from the background.

Exact component elevation belongs to Session 03.

---

## 11.2 Glow levels

| Token | Visual radius target | Accent opacity target | Use |
|---|---:|---:|---|
| `Glow.None` | 0dp | 0 | ordinary surfaces |
| `Glow.Soft` | 8dp | 18% | selected icon/small accent |
| `Glow.Medium` | 16dp | 24% | highlighted metric/action |
| `Glow.Strong` | 24dp | 32% | hero/FAB/special instrument only |

### Allowed glow colors

```text
Brand.Primary
Brand.Secondary
Brand.Active
Brand.Info
Brand.Insight
```

Status error/warning glow is not used by default. Error/warning should communicate through border/icon/text first.

### Rules

- No glow on ordinary cards.
- No multiple unrelated glow colors in one component.
- Strong glow is rare and reserved for a single dominant visual focal point.
- Glow must never be required to understand a state.

---

# 12. Icon foundations

## 12.1 Icon family

Default UI icon language:

```text
Material Rounded
```

Directional icons must use AutoMirrored variants where available.

Custom icons are permitted for:

- AutoDrive brand marks;
- fuel/instrument graphics;
- service-specific marks such as WhatsApp.

Generic actions/status/navigation must not use emoji as the primary control icon.

---

## 12.2 Icon sizes

| Token | Size | Use |
|---|---:|---|
| `Icon.XS` | 16dp | small inline/meta icon |
| `Icon.SM` | 20dp | compact control/status |
| `Icon.MD` | 24dp | default action/navigation icon |
| `Icon.LG` | 28dp | emphasized action |
| `Icon.XL` | 32dp | large navigation/feature icon |
| `Icon.Hero` | 40dp | rare hero/empty-state illustration icon |

### Touch target

An icon may be 16–32dp while its interactive hit target remains at least **48dp × 48dp**.

---

## 12.3 Icon weight and color

- Use one Material Rounded optical family per component.
- Do not mix thin/outlined/filled icon languages arbitrarily.
- Default icon: `Text.Secondary`.
- High-emphasis icon: `Text.Primary`.
- Active icon: component-selected semantic brand/status token.
- Destructive icon: `Status.Error`.
- Disabled icon: `Text.Disabled`.
- WhatsApp icon: `Brand.WhatsApp` only.

---

# 13. Motion foundations

Current code already contains 120ms, 200–300ms, 500–600ms, and longer feature animations. V1 formalizes the reusable interaction layer without forcing feature data animations into the same timing scale.

## 13.1 Durations

| Token | Duration | Use |
|---|---:|---|
| `Motion.Fast` | 120ms | press/release, icon feedback |
| `Motion.Standard` | 200ms | color/opacity/content change |
| `Motion.Expand` | 250ms | expand/collapse |
| `Motion.Emphasized` | 300ms | component/layout transition |
| `Motion.Pulse` | 500ms | deliberate recording/attention pulse only |

### Feature-specific animation

Longer data animations such as the Home pump counter (`800–2500ms`) remain feature behavior and are not general Design System navigation timings.

---

## 13.2 Easing

- Standard movement: `FastOutSlowInEasing`.
- Linear easing: only continuous loops/technical indicators where constant speed is intentional.
- Repeating pulse/recording animation must not be used as generic decoration.

---

## 13.3 Press behavior

Default press feedback:

```text
semantic tint/opacity change
+ optional small scale only for high-emphasis controls
```

Rules:

- Ordinary cards do not scale by default.
- FAB / large CTA may scale to approximately `0.97` during press.
- Press transition uses `Motion.Fast`.
- Disabled controls do not animate as if actionable.

---

# 14. Interaction-state foundations

## Default

Use normal semantic surface, text, icon, and border tokens.

## Pressed

- use the component's semantic accent as a `Opacity.Tint` overlay;
- preserve layout size;
- `Motion.Fast`.

## Selected / active

- selected state must not depend on color alone;
- use at least two signals where practical: tint + border, icon state + label, indicator + color, etc.;
- selected navigation may use `Brand.Active`.

## Focused

- `Border.Strong` using `Brand.Primary`;
- focus treatment must remain visible against dark surfaces.

## Disabled

- content: `Text.Disabled`;
- neutral border;
- no glow;
- no active press motion;
- component size/layout must remain unchanged.

## Loading

- preserve component dimensions to avoid layout shift;
- loading indicators use the component semantic action color;
- loading must not expose a second enabled click target.

---

# 15. RTL foundations

## 15.1 Global direction

**AutoDrive product UI is Arabic-first and RTL-first.**

V1 screen composition should be RTL regardless of accidental device-local assumptions in individual feature files.

The application shell/theme architecture in later sessions should provide a single authoritative RTL policy rather than each screen calling `CompositionLocalProvider` independently.

---

## 15.2 Start/end rules

- Layout spacing uses `start` / `end`, not `left` / `right`.
- Text titles and body copy align to `Start` unless a pattern explicitly centers them.
- Leading content is on the visual start side (right in RTL).
- Trailing metadata/actions are on the visual end side (left in RTL).

---

## 15.3 Directional icons

- Back: use AutoMirrored back icon.
- Forward/chevron: use AutoMirrored directional icon.
- Non-directional icons never mirror merely because the layout is RTL.
- Fuel pump, clock, bell, settings, camera, etc. keep their intrinsic orientation unless the icon itself carries directional meaning.

---

## 15.4 Approved LTR islands

The following content may intentionally use LTR inside an RTL screen:

```text
seven-segment displays
countdowns composed of numeric blocks
phone numbers
OTP / verification codes
technical identifiers
URLs / email addresses
pure numeric measurement runs where order must remain stable
```

### Rule

LTR is applied to the smallest necessary subtree, never to the whole screen to solve a local numeric problem.

---

## 15.5 Numbers and amounts

- Numeric glyph order remains LTR.
- The containing row/card remains RTL.
- Numeric values may align to the visual end when the pattern requires it.
- Do not reverse digit order to imitate RTL.

---

# 16. Content/icon policy

Emoji may remain as **content or intentional illustration**, but not as the default visual language for reusable controls.

Allowed examples:

- celebratory/content illustration;
- intentionally playful empty/education content when approved by screen spec.

Not allowed:

- back/navigation actions;
- generic send/call/settings controls;
- generic status indicators where a governed icon exists.

This resolves the Session 01 emoji-policy question at foundation level.

---

# 17. Foundation-to-component contract

Session 03 must build component specifications using only these foundation roles.

A component specification may decide:

- which foundation token is used by each variant;
- component height/width;
- internal spacing from the spacing scale;
- component radius from the radius scale;
- border width/state;
- icon size;
- text role;
- motion timing;
- RTL behavior.

A component specification may **not** introduce a new global raw color, type size, spacing scale, radius family, border width, or motion duration without reopening Session 02.

---

# 18. Legacy-token migration map

This map is normative for later implementation.

| Current name/value | V1 semantic owner |
|---|---|
| `BgDeep` | `Surface.Canvas` |
| `BgSurface1` | `Surface.Base` |
| `BgSurface2` | `Surface.Raised` |
| `BgSurface3` | `Surface.Overlay` |
| `BorderColor` | `Border.Default` |
| `OrangeAccent` | `Brand.Primary` |
| `GoldPending` used as brand accent | `Brand.Secondary` |
| `GoldPending` used as pending state | `Status.Warning` / `Finance.Pending` |
| `GreenWithdraw` used as active accent | `Brand.Active` |
| `GreenWithdraw` used as success | `Status.Success` |
| `GreenWithdraw` used as withdrawal state | `Finance.Withdrawable` |
| `GrayPaid` | `Finance.Paid` or `Text.Secondary`, based on semantics |
| `AccentBlue` | `Brand.Info` / `Status.Info` |
| `WhatsAppGreen` | `Brand.WhatsApp` |
| `TextPrimary` | `Text.Primary` |
| `TextSecondary` | `Text.Secondary` |
| `TextDisabled` | `Text.Disabled` |

The mapping is semantic. A search-and-replace based only on old symbol names is unsafe.

---

# 19. Foundation conformance checklist

Before a component can be approved in Session 03, it must answer yes to all applicable items:

- Color comes from a semantic foundation role.
- Typography comes from the approved scale or named special display role.
- Internal spacing comes from `Space.*`.
- Radius comes from `Radius.*` or an explicitly scoped instrument exception.
- Border width comes from `Border.*`.
- Opacity comes from the approved opacity scale.
- Glow is justified and uses an allowed level/color.
- Icon size comes from `Icon.*`.
- Interactive hit target is at least 48dp where applicable.
- Motion uses the approved duration/easing set unless clearly feature-specific.
- RTL behavior is explicitly defined.
- LTR is scoped only to approved numeric/technical islands.
- Disabled/pressed/loading behavior is specified.

---

# 20. Resolved issues from Session 01

## Dark-only policy

**Resolved:** Dark-only for Design System V1.

## RTL policy

**Resolved:** Arabic-first RTL globally; tightly scoped LTR numeric/technical islands.

## Home special palette

**Resolved at foundation level:** gauge/instrument colors are formal special-use semantic roles; effect-only dark colors are confined to the instrument implementation.

## Emoji policy

**Resolved:** emoji may be content/illustration, not the default reusable control icon language.

---

# 21. Issues intentionally still open

These are not foundation decisions and remain for later sessions:

1. Final New Chat container form: screen vs dialog vs bottom sheet — Session 05.
2. Final APIs and variants for every component — Session 03.
3. Pattern composition and screen usage — Session 04.
4. Feature image/resource ownership — Session 06.
5. Bottom-navigation unread-state owner — Session 06.
6. Keep/remove decision for unused `DonutChart`, `MiniStatCard`, `AutoDriveLogo` — Sessions 03/10.
7. Exact component dimensions beyond foundation scales — Session 03.
8. Pixel-level matching to all approved designs — Sessions 05/09.

---

# 22. Decisions recorded by Session 02

## Decisions

1. AutoDrive Design System V1 is Dark-only.
2. AutoDrive product composition is Arabic-first and RTL-first.
3. Tajawal remains the sole general UI font family.
4. Existing Material typography roles are retained unchanged.
5. `statXL` 60/68 ExtraBold is added as a named hero metric role.
6. Orange `#FF6B00` is `Brand.Primary`.
7. Gold `#FFB547` is `Brand.Secondary` and separately `Status.Warning`/`Finance.Pending` by semantics.
8. Mint `#4FFFB0` is active/success/withdrawable by separate semantic ownership.
9. Blue `#4F8FFF` is the information accent.
10. Purple `#DB9AF3` is scoped to insight/idea semantics.
11. The 4dp spacing rhythm is authoritative, with a 2dp optical exception.
12. General radii are reduced to 8/12/16/20/24/full.
13. Border widths are 1/1.5/2dp only.
14. Ordinary cards rely on surfaces + borders; glow is controlled and sparse.
15. Material Rounded is the default UI icon family.
16. Directional icons use AutoMirrored variants.
17. Interactive icon targets are at least 48dp even when visual icons are smaller.
18. Reusable motion uses 120/200/250/300/500ms roles.
19. LTR is permitted only for tightly scoped numeric/technical content.
20. Screens do not own raw foundation values after migration.

---

# 23. Forbidden from this point forward

1. No new raw colors inside feature UI.
2. No feature-named global visual tokens.
3. No new global typography sizes outside this scale without reopening Foundations.
4. No arbitrary screen-local spacing/radius/border widths as permanent V1 styling.
5. No random alpha values outside the approved opacity scale in feature UI.
6. No generic card glow/elevation by default.
7. No emoji as reusable action/navigation/status iconography.
8. No non-mirrored directional icon in RTL when an AutoMirrored equivalent exists.
9. No screen-wide LTR workaround for a local number/code.
10. No Light Mode implementation in V1 unless product scope is explicitly reopened.
11. No direct legacy-token rename by text replacement without semantic review.
12. No component specification may invent new foundation categories silently.

---

# 24. Deferred

- Kotlin token implementation.
- Material theme refactor.
- Component APIs.
- Component sizes and variants.
- UI pattern composition.
- Screen-level exact spacing/layout.
- Resource relocation.
- Migration of legacy local literals.
- component previews.
- runtime/visual QA.
- Light Mode.

---

# 25. Open Issues

```text
- New Chat container form.
- Feature image/resource ownership.
- Bottom navigation state ownership.
- Final fate of unused DS components.
- Exact component dimensions/variants.
- Pixel-level reconciliation against every approved redesign image.
```

None of these blocks Session 03 Component Specification.

---

# 26. Next Session Input

Session 03 must treat this document as its sole foundation source of truth.

```text
Theme: Dark-only
Direction: Arabic-first RTL
Font: Tajawal
Brand Primary: #FF6B00
Brand Secondary: #FFB547
Active/Success: #4FFFB0
Info: #4F8FFF
Insight: #DB9AF3
Error: #FF5252
Canvas: #08090C
Surfaces: #0F1117 / #161820 / #1C1F2A
Border: #232635
Text: #EFF1F7 / #8890A8 / #4A5068
Spacing: 2(optical), 4,8,12,16,20,24,32,40,48,64
Radius: 8,12,16,20,24,full
Borders: 1,1.5,2
Icons: 16,20,24,28,32,40; touch target >=48
Motion: 120,200,250,300,500ms
LTR islands: numeric/technical only
```

Session 03 must define the initial component library without modifying business logic.

---

# 27. Session close

```text
STATUS: APPROVED

Decisions:
- Dark-only V1 and Arabic-first RTL are locked.
- Semantic color system replaces feature-named visual ownership.
- Tajawal + current central type scale are retained, with statXL added.
- Spacing/radius/border/opacity/icon/motion scales are fixed.
- Controlled glow and special instrument palette are governed.

Forbidden:
- No unmanaged visual constants.
- No new global foundation values from component/screen code without reopening Session 02.
- No generic UI emoji icons or non-mirrored directional arrows.

Deferred:
- Component APIs, pattern composition, architecture refactor, implementation, migration, QA.

Open Issues:
- New Chat form, resource ownership, bottom-nav state ownership, unused components, exact component dimensions.

Next Session Input:
- 02_FOUNDATIONS.md, once approved, is the sole foundation source of truth for Session 03.
```

**Approval gate:** do not begin Session 03 until the user approves this document by explicitly continuing to Session 03 or otherwise accepting it.


---

## Approval record

Session 02 was approved by the user by explicitly instructing execution of Session 03. No production code was changed during Session 02.
