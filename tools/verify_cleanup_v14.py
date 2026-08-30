#!/usr/bin/env python3
"""Final static cleanup checks for AutoDrive V14."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
checks: list[tuple[str, bool, str]] = []

def check(name: str, ok: bool, detail: str = "") -> None:
    checks.append((name, ok, detail))
    print(("PASS" if ok else "FAIL") + f": {name}" + (f" — {detail}" if detail and not ok else ""))

bridge = ROOT / "app/src/main/kotlin/com/autodrive/app/di/RegistrationBridgeModule.kt"
old_registration_port = ROOT / "feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/domain/repository/RegistrationProfileWriter.kt"
old_signout = ROOT / "feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/domain/usecase/SignOutUseCase.kt"
core_registration_port = ROOT / "core/common/src/main/kotlin/com/autodrive/app/core/common/registration/RegistrationProfileWriter.kt"
core_signout_port = ROOT / "core/common/src/main/kotlin/com/autodrive/app/core/common/session/SignOutAction.kt"
profile_build = (ROOT / "feature/profile/build.gradle.kts").read_text(encoding="utf-8")
profile_vm = (ROOT / "feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileViewModel.kt").read_text(encoding="utf-8")
preferences = (ROOT / "core/session/src/main/kotlin/com/autodrive/app/core/session/data/PreferencesManager.kt").read_text(encoding="utf-8")
wrapper = (ROOT / "gradle/wrapper/gradle-wrapper.properties").read_text(encoding="utf-8")

check("app registration bridge removed", not bridge.exists())
check("auth-owned registration bridge contract removed", not old_registration_port.exists())
check("registration port owned by core common", core_registration_port.is_file())
check("sign-out port owned by core common", core_signout_port.is_file())
check("obsolete auth sign-out use case removed", not old_signout.exists())
check("profile has no Gradle dependency on auth", 'project(\":feature:auth\")' not in profile_build)
check("profile presentation has no auth import", "com.autodrive.app.feature.auth" not in profile_vm)
check("deprecated session clear alias removed", "fun clear() = clearSession()" not in preferences)
check("unused weekly competition view model removed", not (ROOT / "app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionViewModel.kt").exists())
check("unused weekly competition ui state removed", not (ROOT / "app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionUiState.kt").exists())
check("Gradle wrapper uses stable 8.7", "gradle-8.7-bin.zip" in wrapper and "milestone" not in wrapper)

production = [p for base in (ROOT/"app", ROOT/"core", ROOT/"feature") for p in base.rglob("*.kt") if "/src/main/" in p.as_posix()]
stale = []
markers = (
    "feature.auth.domain.repository.RegistrationProfileWriter",
    "feature.auth.domain.usecase.SignOutUseCase",
    "RegistrationBridgeModule",
)
for path in production:
    text = path.read_text(encoding="utf-8", errors="replace")
    if any(marker in text for marker in markers):
        stale.append(str(path.relative_to(ROOT)))
check("no stale bridge imports remain", not stale, str(stale))

todos = []
for path in production:
    text = path.read_text(encoding="utf-8", errors="replace")
    if re.search(r"\b(?:TODO|FIXME|HACK)\b", text):
        todos.append(str(path.relative_to(ROOT)))
check("production has no TODO FIXME HACK markers", not todos, str(todos))

deprecated = [str(path.relative_to(ROOT)) for path in production if "@Deprecated" in path.read_text(encoding="utf-8", errors="replace")]
check("production has no obsolete deprecated compatibility APIs", not deprecated, str(deprecated))

legacy_week_names = []
for path in production:
    text = path.read_text(encoding="utf-8", errors="replace")
    if re.search(r"(?<!fallback)(?:lastFriday9AM|nextFriday9AM)", text):
        legacy_week_names.append(str(path.relative_to(ROOT)))
check("week boundary helpers are explicitly fallback-only", not legacy_week_names, str(legacy_week_names))

passed = sum(ok for _, ok, _ in checks)
print(f"cleanup checks: {passed}/{len(checks)} PASS")
if passed != len(checks):
    sys.exit(1)
