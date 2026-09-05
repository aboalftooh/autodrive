# ACCESSIBILITY_RUNTIME_MATRIX_v65

Runtime accessibility is **not verified**. Gradle bootstrap is blocked by `java.net.UnknownHostException: services.gradle.org`.

| Area | Required matrix | Status |
|---|---|---|
| Gradle bootstrap | `./gradlew --version` | BLOCKED |
| Affected-module compile | DS/Auth/Profile/Balance/Chat/Commission/Notifications/App | NOT_RUN |
| Unit tests | `:app:testDebugUnitTest` | NOT_RUN |
| androidTest compile | `:app:compileDebugAndroidTestKotlin` | NOT_RUN |
| Semantics | bottom-nav/loading/selection/unread/progress/dialog/sheet | NOT_RUN |
| Touch bounds | representative controls >=48dp | NOT_RUN |
| Focus | logical traversal + modal containment | NOT_RUN |
| Font scale | 1.0 / 1.3 / 2.0 | NOT_RUN |
| Direction | supported RTL path / intentional numeric LTR islands | NOT_RUN |
| Screenshots | required v65 screen set | NOT_RUN |

`runtimeAccessibilityVerified=false`
