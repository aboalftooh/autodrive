# AutoDrive — Launch Readiness Handoff

**Repository:** `aboalftooh/autodrive`  
**Purpose:** handoff between audit conversations while preparing AutoDrive for launch.  
**Last completed audit area:** Part 2 — Authentication & Sessions.  
**Recommended next audit area:** Part 3 — Sync & Database.

---

## 1. Audit map

| # | Area | Status |
|---|---|---|
| 1 | Build / Release / Secrets | PENDING full audit |
| 2 | Authentication / Sessions | **PASS — completed** |
| 3 | Sync / Database | **NEXT** |
| 4 | Invoices / Commissions | PENDING |
| 5 | Balance / Withdrawals | PENDING |
| 6 | Notifications | PENDING |
| 7 | Chat | PENDING |
| 8 | Competition | PENDING |
| 9 | Home + Reports | PENDING |
| 10 | Profile + Info | PENDING |
| 11 | UI/UX + Accessibility + Devices | PENDING |
| 12 | Final Launch Testing / E2E | PENDING |

> Part 1 was inventoried at a high level but has not yet received its dedicated deep audit. We intentionally completed Part 2 first. Continue with Part 3, then return to remaining parts according to the launch plan.

---

# 2. Part 2 — Authentication & Sessions

## Final status: PASS

The authentication/session path was audited, repaired, cleaned of obsolete onboarding paths, covered with unit/regression tests, and built successfully in GitHub Actions.

### Completed fixes

- Sudan phone normalization hardened, including Arabic/Persian digit handling where applicable.
- OTP remains six digits and sanitizes Arabic/Persian digits to English digits.
- SMS OTP Autofill uses Google SMS Retriever / User Consent flow.
- OTP listener starts before the send request to avoid the fast-SMS/navigation race.
- Old captured OTP state is cleared appropriately between challenges.
- Verified phone identity is immutable during onboarding.
- Registration/profile persistence no longer trusts an editable phone field.
- Final profile write uses the phone identity established by the authenticated session.
- Session restoration no longer downgrades a valid locally-complete user to incomplete registration merely because a remote refresh fails temporarily.
- A successful server response showing no linked membership is still treated differently from a transport/server failure.
- Pending approval state is persisted securely so closing/reopening the app returns the user to the correct waiting state.
- Stable device/install identity is persisted for the approval/activation path.
- New-user onboarding now follows the approval-based flow instead of the legacy invite-code flow.
- Existing active users and new/pending users are handled as separate states.
- New registration flow is effectively:
  `Phone -> server decision -> registration details/request -> waiting for approval -> OTP -> membership activation/onboarding completion -> app`.
- Legacy invite-code navigation and code-input flow were removed.
- `VerifyInviteCodeUseCase` was removed.
- Dead `WorkshopInfo` navigation/screen path was removed after confirming current registration already collects the workshop fields in the active flow.
- Legacy/auth alternate path references were cleaned from navigation.
- Logout preserves the sync logout barrier and clears account-scoped local state before session teardown completes.
- Account-scoped Room caches, sync cursors/queues/recovery state and related user data remain covered by session-isolation regression checks.
- Server-side onboarding completion was changed to operate on the authenticated membership rather than trusting a client-provided phone/organization/client identity.
- Legacy OTP behavior was hardened so it cannot be used as a back door to create an unrelated AutoDrive identity outside the approved membership flow.

### Important files touched/audited

- `feature/auth/.../PhoneAuthViewModel.kt`
- `feature/auth/.../AuthRepositoryImpl.kt`
- `feature/auth/.../OtpInputScreen.kt`
- `feature/auth/.../SmsOtpAutofillCoordinator.kt`
- `feature/auth/.../WaitingScreen.kt`
- `feature/auth/.../RegisterViewModel.kt`
- `feature/auth/.../RegisterScreens.kt`
- `feature/auth/.../SplashViewModel.kt`
- `app/.../navigation/NavigationGraphs.kt`
- `core/session/.../PreferencesManager.kt`
- `core/sync/.../LocalDataCleaner.kt`
- Auth/session regression tests under `feature/auth/src/test` and `app/src/test/.../architecture`
- `.github/workflows/unpack-build-apk.yml`

### Removed legacy items

- Invite-code `CodeInput` screen/navigation path.
- Legacy `VerifyInviteCodeUseCase`.
- Dead `WorkshopInfo` navigation path/screen.
- Remaining direct references to those removed paths were searched and cleaned.

---

# 3. Tests and build evidence for Part 2

GitHub Actions final auth release gate:

- Workflow run: **#55**
- Run ID: `33375069813`
- Head SHA tested: `de49d7c18d47cb7e554cae125af65ed6f8de7042`

Results:

| Check | Result |
|---|---|
| Supabase client configuration | PASS |
| Firebase configuration | PASS |
| Complete `feature/auth` unit suite | **PASS** |
| Auth architecture regressions | **PASS** |
| SMS Autofill architecture regression | **PASS** |
| Session isolation/logout regression | **PASS** |
| Full debug APK compilation | **PASS** |
| APK integrity verification | **PASS** |
| Artifact upload | **PASS** |

Generated artifact from that run:

- Name: `AutoDrive-debug-apk`
- Artifact ID: `9751675410`
- SHA-256 digest recorded by GitHub: `697572c0c59ae7f217a545a28e3f70e95f8e7c487b68cf1f1ab40b9c49cef79a`

---

# 4. Known failures discovered outside Part 2

During an earlier attempt to run the entire `app:testDebugUnitTest` suite, unrelated failures were exposed. They are **not considered fixed by the Auth work** and must be audited in their corresponding launch parts rather than suppressed.

Known affected areas include:

- Competition gating/contracts.
- Reports contracts/routes.
- Room migration architecture assertions.
- Exact-money migration assertion.
- Outbox sensitive-error behavior/test.
- Chat retry behavior/test.
- Notification repository coroutine tests that do not complete.
- A reports invoice-details test initialization error.
- Responsibility-split architecture assertions.
- Some older closure/contract tests whose expectations no longer match current code.

The Auth CI gate was narrowed deliberately to the complete Auth suite plus Auth/SMS/Session regressions, followed by a **full application APK build**. Do not interpret this as the whole project test suite being green. These failures must be resolved when their respective parts are audited.

---

# 5. NEXT — Part 3: Sync & Database

Start here in the next conversation.

## Scope

Perform a deep audit of the current code only; do not assume historical sync findings still apply.

Audit at minimum:

1. Room database schema/version and every migration chain.
2. Migration safety for existing production data.
3. Outbox durability, retries, idempotency and ordering.
4. Push/Pull ordering and conflict behavior.
5. Sync cursors and bootstrap state.
6. Realtime responsibilities: signal-only vs state mutation.
7. Offline edits and reconnection recovery.
8. Tombstones/deletions and atomicity.
9. Account/org/client scope isolation in every sync table/query.
10. Logout/session-switch barriers and stale worker prevention.
11. Duplicate delivery/replay handling.
12. Clock-skew / `updated_at` assumptions.
13. Pagination and large-data behavior.
14. Crash/restart recovery halfway through sync.
15. WorkManager scheduling/concurrency.
16. Chat recovery/media sync interaction with the central sync engine.
17. Determine whether `LegacyRemotePuller.kt` is active, dead, or capable of racing the current sync path.
18. Search for any second/legacy synchronization implementation and remove it only after proving it is unused.
19. Run existing sync/database/architecture/migration tests.
20. Add missing tests for every discovered bug before declaring PASS.

## Acceptance rule for Part 3

Do **not** mark Part 3 PASS merely because the project builds. Required:

- no active competing legacy sync path;
- migration chain verified;
- account isolation verified;
- deletion/tombstone behavior verified;
- outbox retry/idempotency verified;
- recovery after interruption verified;
- relevant tests green;
- full app compilation still green.

---

# 6. Suggested command for the next conversation

Use this message:

> افتح مشروع `aboalftooh/autodrive` على GitHub واقرأ ملف `AUTODRIVE_LAUNCH_READINESS_HANDOFF.md`. ابدأ مباشرة من **Part 3 — Sync & Database**. افحصه فحصًا عميقًا، أصلح المشاكل، اكتب الاختبارات وشغلها، ولا تعتبر الجزء PASS إلا بعد تحقق شروط القبول المكتوبة في التقرير.

---

## Handoff rule

After finishing each audit part, update this same file with:

- status PASS/FAIL;
- fixes performed;
- tests added/run;
- CI/build evidence;
- unresolved findings;
- exact next part.

This file is the handoff/source-of-truth for the launch-readiness audit across conversations.
