# AutoDrive v76 — OTP Autofill Repair

## Implemented
- Removed automatic clipboard OTP preload that could reuse an expired/consumed code.
- Every OTP screen initialization starts with a fresh empty OTP in production.
- Any rejected/expired OTP is cleared immediately after verification failure.
- Kept Google SMS Retriever for silent OTP fill when the SMS contains the official app hash.
- Added SMS User Consent fallback for devices/messages where the app hash is missing or mismatched; no READ_SMS/RECEIVE_SMS permission is required.
- Six-digit OTP is verified automatically immediately after fill/entry.

## Verification
- `tools/verify_observability_v11.py`: 21/21 PASS.
- Confirmed no clipboard auto-preload references remain in production OTP UI.
- Confirmed SMS Retriever + User Consent + six-digit auto-verification are wired.
- Gradle compilation could not run because the environment does not contain the Gradle 8.7 distribution and internet access is unavailable.
