---
status: ACTIVE
scope: current v73 phone auth, invite onboarding, session, sign-out and push-token contract
owner: AutoDrive Engineering
last_verified_against: AutoDrive-v73
last_verified_date: 2026-08-22
supersedes: NONE
---

# Authentication Contract

This document describes **implemented v73 behavior only**. It does not document any future join-request/approval redesign as current behavior.

## Components

- `AuthRepositoryImpl`
- `PhoneAuthViewModel` and invite/registration presentation flows
- `CurrentSession`, `SessionReader`, `SessionWriter`, `PreferencesManager`
- `ProfileRepositoryImpl` for new-user invite redemption/profile completion
- `PushTokenRepository` / `FcmTokenUploader`
- `SyncManager` + `RealtimeController` sign-out boundaries
- `supabase/functions/send-phone-otp`
- `supabase/functions/verify-phone-otp`

## 1. Send phone OTP

Android invokes `send-phone-otp` with a normalized/input phone value. Repository Edge Function source normalizes Sudanese numbers to `249#########`, rate-limits by phone/IP, invalidates any previous unused OTP before creating a new one, stores only an OTP hash, gives the code a five-minute expiry, and sends it through the configured SMS provider.

Android surfaces an optional `dev_otp` only in DEBUG if the response happens to contain one; the v73 repository function itself returns `{success:true}` and does not emit the OTP.

## 2. Verify OTP and import session

Android invokes `verify-phone-otp(phone, otp)`. The function validates rate/attempt limits, selects the latest live unused OTP, compares its SHA-256 hash, creates a Supabase auth session, then consumes the OTP with a conditional one-time update. It returns access/refresh token data.

Android imports that session with `supabase.client.auth.importSession`, resolves `userId`, stores phone/user identity locally, best-effort calls `link_phone_user_by_phone`, then refreshes registration state from `autodrive_users`.

## 3. Invite verification

`verifyInviteCode` requires a current Supabase session and calls `verify_invite_code_v2(p_code)`.

A valid result supplies `clientId` and `orgId`, which are saved with the resolved user id and `pendingInviteCode` in `CurrentSession`. If an existing `autodrive_users` row is found for the client, Android calls `link_phone_user`, marks registration complete/logged in, clears the pending invite, and triggers push-token upload.

The server source for `verify_invite_code_v2`, `link_phone_user_by_phone`, and `link_phone_user` is absent from the v73 repository, so their live enforcement remains `UNVERIFIED` in Session 74.

## 4. New-user profile completion

For a user not already linked, `ProfileRepositoryImpl.saveUser` requires the pending invite and calls `redeem_invite_code` with profile/account fields. Caller comments expect the server RPC to atomically mark the invite used and create user/balance state. Android then marks `onboarding_completed=true`, updates local session to `COMPLETE`, clears the pending invite, and triggers push-token upload.

The server source for `redeem_invite_code` is absent from v73; exact runtime atomicity is therefore not promoted beyond caller expectation.

## 5. Current-session ownership

`PreferencesManager` persists session state in `EncryptedSharedPreferences` using AndroidX Security. `CurrentSession` contains:

`isLoggedIn`, registration state, `userId`, `clientId`, `orgId`, user name, account type, phone, and pending invite code.

`SyncScope.from` accepts a session only when user/client/org are all non-blank. Sync never invents a missing tenant scope.

## 6. Restore session

`restoreSession` awaits Supabase auth initialization, rejects a session whose user has neither email nor phone (anonymous-style), restores user id when needed, then refreshes local registration/profile state from `autodrive_users`. A missing Supabase session returns false.

## 7. Sign-out cleanup

Sign-out sequence in v73:

1. capture departing exact sync scope if available;
2. best-effort revoke current push token;
3. stop Realtime;
4. block the departing sync scope from new sync work;
5. clear local session so new feature mutations cannot resolve the old scope;
6. under sync lifecycle serialization, clear local account-scoped data;
7. best-effort `supabase.auth.signOut()`;
8. release logout barrier.

This ordering protects against stale-account callbacks/mutations crossing an account switch.

## 8. Push token relationship

After successful existing-user linking or registration completion, `FcmTokenUploader` can register the current token. `PushTokenRepository` validates local session/user alignment, then calls authenticated receipt-style RPCs:

- `autodrive_register_push_token_command_v1`
- `autodrive_revoke_push_token_command_v1`

User/client/org are not sent as authority to those command RPCs.

## 9. Sync / Realtime relationship

Authentication establishes the Supabase session and exact local scope needed for canonical sync. Sync start is skipped when required scope/session is unavailable. Realtime is stopped during sign-out and restarted by the coordinator after a sync run; it is not authentication authority.

## Explicit non-current flow

A future flow where a user submits a join request, Verto approves it, AutoDrive later asks for phone and only then sends OTP is **not implemented in v73** and is deliberately excluded from this current contract.
