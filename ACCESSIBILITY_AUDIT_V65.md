# ACCESSIBILITY_AUDIT_V65

## Scope

- Input: `AutoDrive-v64-component-adoption-static-runtime-blocked.zip`
- Effective audited rows: **53 / 56**
- Reclassified by source evidence: **4**
- Verified exclusions: **3**
- Frozen static findings: **33**
- Production files mutated: **14**, exactly the frozen allowlist
- Production files outside v65 scope protected: **198**

## Static findings

| Rule | Discovered | Resolved | Open static |
|---|---:|---:|---:|
| DS-A11Y-001 | 29 | 29 | 0 |
| DS-A11Y-002 | 2 | 2 | 0 |
| DS-A11Y-003 | 0 | 0 | 0 |
| DS-CONTRAST-001 | 2 | 2 | 0 |

### Semantic classifications

- `SEM_BADGE_UNREAD`: 3
- `SEM_DISABLED`: 1
- `SEM_HEADING`: 6
- `SEM_LABEL`: 13
- `SEM_MODAL_FOCUS_OWNER`: 1
- `SEM_PROGRESS`: 1
- `SEM_SELECTED`: 6
- `SEM_STATE_DESCRIPTION`: 2

## Confirmed repairs

- Loading controls retain their accessible name and expose busy state.
- Bottom navigation and selection surfaces expose selected state rather than color alone.
- Step indicator exposes current/total progress semantics.
- Conversation and notification unread state is contextualized once.
- Actual headings receive heading semantics; decorative child icons are suppressed.
- Enabled TextField/SearchField placeholder pairing changed from Disabled (`~2.23:1`) to Secondary (`~5.57:1`).
- Raw small interactive targets repaired with `AutoDriveIconSize.TouchTarget`.
- Chat image backdrop keeps pointer dismissal while avoiding a duplicate semantic click action.
- Commission long-click/tap callbacks retained with explicit accessibility action labels.

## Reconciliation

`MediaActionGroup`, `SearchResultsList`, `RealtimeStatusBar`, and `CommissionStatusBadge` were reclassified into v65 by source evidence. Realtime and commission status surfaces required verification only because visible text already conveys state.

## Runtime truth

Gradle bootstrap remains blocked by `java.net.UnknownHostException: services.gradle.org`. Compile, instrumented semantics, focus, font-scale, touch-bounds, and screenshot runtime evidence are therefore **NOT_RUN**, not PASS.

## Static verdict

`STATIC_ACCESSIBILITY_REPAIR_COMPLETE / ADAPTIVE_UI_RUNTIME_BLOCKED`
