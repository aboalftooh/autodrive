#!/usr/bin/env python3
from pathlib import Path
import re
import sys
import tomllib
import xml.etree.ElementTree as ET

from project_layout import MAIN_SOURCE_ROOTS, ROOT, module_path, source_file

checks = []

def check(name: str, condition: bool) -> None:
    checks.append((name, condition))
    print(("PASS" if condition else "FAIL") + f": {name}")

app = module_path("app")
manifest = app / "src/main/AndroidManifest.xml"
manifest_text = manifest.read_text(encoding="utf-8")
ET.parse(manifest)
check("manifest XML parses", True)
check("READ_SMS removed", "READ_SMS" not in manifest_text)
check("RECEIVE_SMS removed", "RECEIVE_SMS" not in manifest_text)

otp = source_file("feature/auth", "feature/auth/presentation/login/OtpInputScreen.kt").read_text(encoding="utf-8")
check("OTP uses SMS Retriever", "SmsRetriever.SMS_RETRIEVED_ACTION" in otp)
check("OTP no longer decodes raw PDUs", "SmsMessage.createFromPdu" not in otp)

network_build = (module_path("core/network") / "build.gradle.kts").read_text(encoding="utf-8")
observability_build = (module_path("core/observability") / "build.gradle.kts").read_text(encoding="utf-8")
check("Supabase URL not hardcoded in network module", "madkfvggyolmdberzmtb.supabase.co" not in network_build)
check("Supabase anon JWT not hardcoded in network module", "eyJhbGciOiJIUzI1Ni" not in network_build)
check("network configuration comes from properties or environment", "configurationValue" in network_build)
check("Crashlytics release flag exists", "CRASH_REPORTING_ENABLED" in observability_build)

with (ROOT / "gradle/libs.versions.toml").open("rb") as handle:
    catalog = tomllib.load(handle)
check("version catalog parses", bool(catalog))
check("Crashlytics dependency registered", "firebase-crashlytics" in catalog.get("libraries", {}))
check("Crashlytics plugin registered", "firebase-crashlytics" in catalog.get("plugins", {}))

logger = source_file("core/observability", "core/observability/AppLogger.kt").read_text(encoding="utf-8")
check("logger sanitizes text", "SensitiveDataRedactor.sanitizeText" in logger)
check("logger sanitizes fields", "SensitiveDataRedactor.sanitizeFields" in logger)

sync_required = {
    source_file("core/sync", "core/sync/data/DefaultSyncCoordinator.kt"): ["diagnostics.syncStarted", "diagnostics.syncFinished"],
    source_file("core/sync", "core/sync/data/SyncStepExecutor.kt"): ["diagnostics.phaseFinished"],
    source_file("core/sync", "core/sync/data/OutboxSynchronizer.kt"): ["diagnostics.outboxState"],
    source_file("core/sync", "core/sync/realtime/RealtimeManager.kt"): ["diagnostics.realtimeState"],
}
check(
    "required sync diagnostics are connected",
    all(all(token in file.read_text(encoding="utf-8") for token in tokens) for file, tokens in sync_required.items()),
)

all_main_files = [file for root in MAIN_SOURCE_ROOTS for file in root.rglob("*.kt")]
raw_log_offenders = []
for file in all_main_files:
    if file.name in {"AppLogger.kt", "SmsHashLogger.kt"}:
        continue
    if re.search(r"\bLog\.(?:d|i|w|e|v)\(", file.read_text(encoding="utf-8", errors="replace")):
        raw_log_offenders.append(file)
check("raw Android logging confined", not raw_log_offenders)

sensitive_interpolation = re.compile(r"\$\{?(?:otp|token|amount|balance|commission|password|payload|bankAccount|phone)\}?", re.I)
log_offenders = []
for file in all_main_files:
    for line in file.read_text(encoding="utf-8", errors="replace").splitlines():
        if "AppLogger." in line and sensitive_interpolation.search(line):
            log_offenders.append(f"{file}:{line.strip()}")
check("no direct sensitive interpolation in logger calls", not log_offenders)

example = (ROOT / "local.properties.example").read_text(encoding="utf-8")
check("environment example contains required keys", all(key in example for key in [
    "AUTODRIVE_SUPABASE_URL",
    "AUTODRIVE_SUPABASE_ANON_KEY",
    "AUTODRIVE_ADMIN_WHATSAPP",
]))

rls_doc = ROOT / "docs/refactor/rls-review-v11.md"
rls_sql = ROOT / "tools/verify_rls_v11.sql"
check("RLS review exists", rls_doc.is_file())
check("RLS verifier checks pg_policies", rls_sql.is_file() and "pg_policies" in rls_sql.read_text(encoding="utf-8"))
check("RLS verifier checks relrowsecurity", rls_sql.is_file() and "relrowsecurity" in rls_sql.read_text(encoding="utf-8"))

passed = sum(1 for _, ok in checks if ok)
print(f"observability/security checks: {passed}/{len(checks)} PASS")
if passed != len(checks):
    sys.exit(1)
