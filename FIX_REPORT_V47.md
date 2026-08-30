# AutoDrive v47 Fix Report

- Chat list now refreshes conversation title, last message, timestamp and unread count before opening the chat.
- Realtime admin messages publish a local Android notification and navigate to the conversation.
- FCM chat notifications prefer conversation title/subject when provided.
- Blank admin conversation titles fall back to a unique date/time conversation title.
- Reports oversized headline/stat values reduced.
- Balance screen hero value reduced and withdrawal action moved directly below the balance card.
- Registration flow changed to OTP → account type → details → invite-code request → code verification → welcome → home.
- Invite request uses collected registration data to prefill WhatsApp automatically.
- New-user profile is only redeemed/saved after invite-code verification.
- Incomplete restored sessions resume registration rather than jumping straight to code input.

Verification: package/module/cleanup static checks all PASS. Gradle compile could not run because the environment cannot reach services.gradle.org.
