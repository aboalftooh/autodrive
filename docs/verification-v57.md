# AutoDrive v57 — Settings UX Rebuild Verification

## Scope

تم تنفيذ الجلسة **v57 فقط** فوق `AutoDrive-v56.zip` وفق `AutoDrive_Competition_Reports_Settings_Execution_Plan_v49-v58.md`.

الهدف المنفذ: إعادة بناء hierarchy شاشة الإعدادات فوق منطق v56 دون تغيير Repository أو Outbox أو Session أو Navigation contracts.

## Production files changed

1. `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileScreen.kt`
2. `core/designsystem/src/main/kotlin/com/autodrive/app/core/designsystem/components/inputs/InputComponents.kt`
   - إضافة `keyboardActions` اختيارية إلى `AutoDriveNumericField` فقط لتمرير IME actions المطلوبة في v57؛ تغيير backward-compatible ولا يغير السلوك الافتراضي.

## Test file added

- `feature/profile/src/test/profile-v57-contract.sh`

## Implemented UX

- Header بعنوان `الإعدادات` فقط؛ لا زر تعديل عام ولا تسجيل خروج في Header.
- Identity: Avatar + fullName + join date إن وجدت، بلا Action.
- `الحساب الشخصي`: الاسم والهاتف مع محرر Bottom Sheet مستقل.
- `بيانات استلام العمولة`: البنك والحساب/IBAN مع محرر مستقل؛ IBAN يستخدم Text/Ascii ويقبل الحروف.
- `بيانات الورشة`: تظهر لكل `WORKSHOP_OWNER` حتى إن كانت القيم الحالية null، وتختفي عن `MARKETER`.
- `الأهداف والتخصيص`: الهدف الأسبوعي يفتح Bottom Sheet مستقل ويحافظ على حدود v56 وخطوة 50,000.
- النص المعتمد للهدف: `هدف شخصي لعرض تقدمك في الشاشة الرئيسية، ولا يؤثر على ترتيب المسابقة.`
- `المساعدة والمعلومات`: About / Privacy / FAQ مع نفس callbacks/routes.
- تسجيل الخروج أصبح آخر المحتوى باستخدام `SettingsRowVariant.Destructive` مع Dialog التأكيد الحالي ودون تغيير `SignOutAction`.
- Bottom Navigation الحالي لم يتغير.

## Form behavior

- الحقول المحلية تستخدم `rememberSaveable`.
- Bottom Sheet لا يغلق أثناء `isSaving`.
- نجاح الحفظ يغلق المحرر عبر `editingSection = null` في v56 ViewModel.
- الخطأ يبقي المحرر مفتوحًا ويظهر داخله.
- Phone: `KeyboardType.Phone`.
- Workers count: `KeyboardType.Number`.
- IBAN: `KeyboardType.Ascii`.
- Text fields تستخدم `ImeAction.Next/Done` حسب موضعها.

## Verification

### v56 regression contract

`feature/profile/src/test/profile-v56-contract.sh`

- Result: **16/16 PASS**.

### v57 static contract

`feature/profile/src/test/profile-v57-contract.sh`

- Result: **20/20 PASS**.
- Covers all 12 acceptance items in v57 plus sheet isolation, save dismissal behavior, IME support, `rememberSaveable`, and removal of the global form.

### Additional static checks

- `Color(0x...)` in `ProfileScreen.kt`: **0**.
- Main screen `verticalScroll`: **1**; no nested main-content scroll added.
- Production changes versus v56 are limited to the two allowed files above.

## Gradle

Attempted:

```text
./gradlew :feature:profile:compileDebugKotlin --no-daemon
```

Gradle Wrapper attempted to download `gradle-8.7-bin.zip` and failed with:

```text
java.net.UnknownHostException: services.gradle.org
```

Per the execution-plan rule, no Gradle/build/dependency workaround was introduced. Static verification and existing regression contracts were completed instead.

## Architecture preservation

Unchanged in v57:

- `ProfileViewModel` save semantics from v56.
- `ProfileRepositoryImpl`.
- optimistic Room update.
- direct Supabase update.
- Outbox fallback.
- `profile:<userId>` idempotency key.
- Session/sign-out clearing behavior.
- Navigation routes.
- Bottom navigation behavior.
- Competition and Reports features.
