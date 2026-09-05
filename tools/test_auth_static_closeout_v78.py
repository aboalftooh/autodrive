from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
checks: list[tuple[str, bool, str]] = []

def check(name: str, condition: bool, detail: str = "") -> None:
    checks.append((name, bool(condition), detail))

prod_files = list(ROOT.glob("**/src/main/**/*.kt"))
prod_text = {p: p.read_text(errors="ignore") for p in prod_files}
legacy_tokens = [
    "WAIT_APPROVAL", "APPROVED_OTP", "NEW_REQUEST", "pendingJoinRequestId",
    "submitJoinRequest", "getJoinRequestStatus", "sendApprovedPhoneOtp",
    "verifyApprovedPhoneOtp", "autodrive-send-otp", "autodrive-verify-otp",
    "Screen.Waiting", "WaitingScreen", "WaitingViewModel",
]
legacy_hits = {token: [str(p.relative_to(ROOT)) for p, text in prod_text.items() if token in text] for token in legacy_tokens}
check("production Kotlin legacy runtime tokens = 0", all(not hits for hits in legacy_hits.values()), f"{len(prod_files)} Kotlin files")

for rel in [
    "feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/WaitingScreen.kt",
    "feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/join/WaitingViewModel.kt",
    "feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/register/AccountTypeScreen.kt",
]:
    check(f"deleted {rel}", not (ROOT / rel).exists())

nav = (ROOT / "app/src/main/kotlin/com/autodrive/app/navigation/NavigationGraphs.kt").read_text()
dest = (ROOT / "app/src/main/kotlin/com/autodrive/app/navigation/AppDestinations.kt").read_text()
check("navigation has join code + otp", "Screen.CodeInput" in nav and "Screen.OtpInput" in nav)
check("navigation has no waiting/account-type route", "Screen.Waiting" not in nav + dest and "Screen.AccountType" not in nav + dest)

vm = (ROOT / "feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/PhoneAuthViewModel.kt").read_text()
check("phone split handles existing/new", "PhoneEntryResult.LoginOtp" in vm and "PhoneEntryResult.JoinCodeRequired" in vm)
check("duplicate phone fails closed", "PhoneEntryResult.AccountSelectionRequired" in vm and "تواصل مع الإدارة" in vm)

repo = (ROOT / "feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/data/AuthRepositoryImpl.kt").read_text()
check("repository verifies Verto join code", '"verify_join_code"' in repo)
check("repository uses common OTP endpoints", 'function = "send-phone-otp"' in repo and 'function = "verify-phone-otp"' in repo)
check("repository has no approval OTP endpoints", "autodrive-send-otp" not in repo and "autodrive-verify-otp" not in repo)

session = (ROOT / "core/session/src/main/kotlin/com/autodrive/app/core/session/domain/CurrentSession.kt").read_text()
check("session stores invite code only", "pendingInviteCode" in session and "pendingJoinRequest" not in session)

registration = (ROOT / "supabase/functions/autodrive-registration/index.ts").read_text()
check("registration edge supports phone_entry + verify_join_code", "action === 'phone_entry'" in registration and "action === 'verify_join_code'" in registration)
check("registration edge has no submit/status approval actions", "action === 'submit'" not in registration and "action === 'status'" not in registration)

send = (ROOT / "supabase/functions/send-phone-otp/index.ts").read_text()
verify = (ROOT / "supabase/functions/verify-phone-otp/index.ts").read_text()
check("new-user send OTP requires join code", "join_code_required" in send and "invite_code" in send)
check("new-user verify OTP activates join code", "autodrive_activate_join_code_v1" in verify and "invite_code" in verify)

drop_migration = (ROOT / "supabase/migrations/20260903070029_autodrive_v77_drop_join_request_runtime.sql").read_text().lower()
for fn in [
    "autodrive_submit_join_request_v1", "autodrive_get_join_request_status_v1",
    "verto_approve_autodrive_join_request_v1", "verto_reject_autodrive_join_request_v1",
]:
    check(f"drop migration removes {fn}", fn in drop_migration and "drop function" in drop_migration)

migrations = "\n".join(p.read_text(errors="ignore") for p in ROOT.glob("supabase/migrations/20260903*autodrive_v77*.sql"))
check(
    "join-code helpers restricted from anon/authenticated",
    "revoke all on function public.autodrive_verify_join_code_v1" in migrations
    and "revoke all on function public.autodrive_activate_join_code_v1" in migrations
    and "grant execute on function public.autodrive_verify_join_code_v1" in migrations
    and "to service_role" in migrations,
)

android_text = "\n".join(
    p.read_text(errors="ignore")
    for p in ROOT.glob("**/src/main/**/*")
    if p.is_file() and p.suffix in {".kt", ".kts", ".xml", ".properties", ".json"}
)
check("no service-role secret in Android production", "SUPABASE_SERVICE_ROLE_KEY" not in android_text and "service_role_key" not in android_text.lower())

profile = (ROOT / "feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/register/RegisterViewModel.kt").read_text()
check("profile derives account type from server session", "serverAccountType = session.accountType" in profile)

failed = [item for item in checks if not item[1]]
for name, passed, detail in checks:
    suffix = f" | {detail}" if detail else ""
    print(f"{'PASS' if passed else 'FAIL'} | {name}{suffix}")
print(f"SUMMARY | PASS={len(checks) - len(failed)} FAIL={len(failed)} TOTAL={len(checks)}")
if failed:
    raise SystemExit(1)
