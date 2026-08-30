# AutoDrive v71 — Runtime Blocked Report

Packaging override: **AUTHORIZED BY USER**.

The implementation is retained and packaged despite the runtime bootstrap blocker. The blocker is not hidden and is not converted into PASS.

```text
GRADLE_BOOTSTRAP_UNKNOWN_HOST
java.net.UnknownHostException: services.gradle.org
```

Consequences:

```text
COMPILED=false
UNIT_TESTED=false
ANDROID_MIGRATION_TESTED=false
SERVER_CHAT_RUNTIME_VERIFIED=false
MEDIA_STORAGE_RUNTIME_VERIFIED=false
PREDECESSOR_GATE_SATISFIED=false
handoff72Authorized=false
```

Verified locally without Android runtime:

```text
v71 static: 59/59 PASS
v71 model: 38/38 PASS
v71 migration model: 9/9 PASS
10k chat recovery model: PASS
```
