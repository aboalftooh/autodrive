# AutoDrive v78 — Static Closeout

Date: 2026-09-03
Scope: Verto join-code authentication cutover closeout.
Verification policy: static checks only per owner instruction; real-device E2E and Gradle/network build are deferred.

## Result

**STATIC PASS**

- Existing unique phone contract: `Phone → OTP → Dashboard`.
- New phone contract: `Phone → Verto 8-digit code → OTP → profile → Dashboard`.
- Old `join request → approval → approved OTP` runtime: removed.
- Static v77 gate: PASS.
- Extended source/contract gate: **23/23 PASS**.
- Production Kotlin scanned: **286 files**.
- Active legacy approval-flow tokens: **0**.
- Service-role secret references in Android production source: **0**.

## Contract checks

1. Legacy Waiting/AccountType screens are absent from runtime source.
2. Navigation contains CodeInput and OTP, with no Waiting/AccountType onboarding route.
3. PhoneAuth routes only LoginOtp / JoinCodeRequired / fail-closed AccountSelectionRequired.
4. AuthRepository uses `autodrive-registration` `verify_join_code` and common `send-phone-otp` / `verify-phone-otp`.
5. Session stores `pendingInviteCode`, not pending join-request state.
6. `autodrive-registration` contains `phone_entry` + `verify_join_code` and no active submit/status approval actions.
7. New-user OTP requires a valid invite code and verification activates `autodrive_activate_join_code_v1`.
8. v77 SQL removes the old join-request/approval RPC runtime.
9. Join-code verification/activation helpers are service-role internal; Android contains no service-role credential.
10. Profile completion derives account type from the server-bound session.

## Compile issue previously caught

GitHub CI run 57 exposed one Kotlin compile error in `restoreSession()`: a suspending `resolveUserId()` call was inside the non-suspending session mutation lambda. Source was corrected in commit `bdb6ee51bedbbee8ada879fbbc2595dbaffb3719` by resolving the user id first, then mutating the session. Build re-verification is deferred by owner instruction.

## Data exception

The live server has one normalized phone shared by two AutoDrive membership rows. The server intentionally returns `ACCOUNT_SELECTION_REQUIRED` instead of guessing a membership, preventing cross-client/org login. No records were merged or deleted automatically. This preserves all data; an administrator must correct the wrong phone value for that specific duplicate before direct OTP can work for that number.

## Deferred by owner

- Gradle/network build and compiled unit suite.
- Real-device E2E/smoke tests.

These are **deferred**, not claimed as PASS. Under the requested static-only acceptance policy, the authentication cutover is closed as **STATIC PASS**.
