# AutoDrive 1.0.1 — FCM + OTP Autofill Repair

## Implemented
- Added dynamic SMS Retriever app-hash generation from the installed APK signature.
- `send-phone-otp` now receives the app hash from the client and emits the exact hash in the SMS; static server hash remains only as backward-compatible fallback.
- OTP listener now starts before the server dispatch request and survives the navigation gap, preventing fast SMS delivery from being missed.
- SMS User Consent remains as fallback without SMS permissions.
- FCM token upload is persisted as pending before upload and reconciled with the server on every authenticated process start.
- Server `send-phone-otp` deployed as v55 and uses cryptographically secure OTP generation.
- App version bumped to 1.0.1 / versionCode 2.

## Verification
- Server-side AutoDrive sync/withdrawal/invoice command tests were PASS before this client patch.
- Edge Function deployment status: ACTIVE.
- Full Gradle compile was attempted but blocked because Gradle 8.7 distribution is not cached in this execution environment and internet is disabled.
