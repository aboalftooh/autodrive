# AutoDrive v52 — Competition Closure / Regression Guard

## Scope

تم تنفيذ الجلسة **v52 فقط** فوق `AutoDrive-v51.zip` وفق خطة `AutoDrive_Competition_Reports_Settings_Execution_Plan_v49-v58.md`.

هذه الجلسة Verification + Guard فقط. لم يتم تغيير أي Feature behavior ولم يتم تنفيذ أي بند من v53 أو ما بعدها.

## 1. Mandatory forbidden-source scan

تم فحص كل ملفات Kotlin داخل:

```text
app/src/main/kotlin/com/autodrive/app/feature/competition
```

النتيجة:

| النص المحظور | عدد النتائج |
|---|---:|
| `startPolling` | 0 |
| `delay(60_000` | 0 |
| `fetchLeaderboardDirectly` | 0 |
| `fetchCompetitionHistoryDirectly` | 0 |
| `fetchWinWeeksDirectly` | 0 |
| `currentFriday9AM` | 0 |
| `postgrest["invoices"]` | 0 |

النتيجة: **PASS**.

## 2. Gate matrix

| حالة السيرفر | Home | Reports competition | Competition route | RPC leaderboard |
|---|---|---|---|---|
| `DISABLED` | مخفي | مخفي | unavailable | لا |
| `LOCKED` | قريباً | مخفي | locked | لا |
| `ACTIVE` | ظاهر | ظاهر | active | نعم |

### Evidence

- Home ينشئ teaser فقط عندما الحالة ليست `DISABLED`، ويعرض `قريباً` عند `LOCKED`.
- Reports يعرض عناصر المسابقة فقط عند `CompetitionAvailability.ACTIVE` مع إبقاء عناصر التقارير غير التابعة للمسابقة في الفرع الآخر.
- `WeeklyCompetitionScreen` يحتوي فروعاً صريحة لـ`DISABLED` و`LOCKED` و`ACTIVE`.
- `onActiveEntry()` موجود داخل فرع `ACTIVE` فقط؛ لا يوجد refresh للـleaderboard في فرعي `DISABLED` أو `LOCKED`.
- `WinWeeks` و`CompetitionHistory` محميان في Navigation ولا يفتحان المحتوى الأصلي إلا عند `ACTIVE`.

النتيجة: **PASS**.

## 3. Server rollback / rollout check

تم التحقق من أن `public.autodrive_feature_flags` هو مصدر حالة rollout، وأن Android يملك `SELECT` فقط ولا يملك `INSERT/UPDATE/DELETE`.

الحالات الوحيدة في Domain:

```text
DISABLED
LOCKED
ACTIVE
```

وبالتالي انتقالات السيرفر:

```text
ACTIVE → DISABLED
DISABLED → LOCKED → ACTIVE
```

لا تتطلب APK جديداً؛ التطبيق يقرأ الحالة من السيرفر ويخزن آخر حالة صالحة في DataStore.

Safe default يبقى `DISABLED`، وفشل الشبكة لا يمسح Cache صالحاً.

النتيجة: **PASS**.

## 4. Winner / rank independence

تم التحقق أن Feature Flag لا يعتمد على RPCs الخاصة بالترتيب والفوز، وأن `WeeklyCompetitionRepositoryImpl` لا يعتمد على `CompetitionAvailability`.

مصادر بيانات المسابقة تبقى:

```text
get_weekly_competition
get_my_competition_history
get_my_win_weeks
```

ولا يوجد rank/winner calculation داخل Feature Gate.

النتيجة: **PASS**.

## 5. Regression guard added

تمت إضافة:

```text
app/src/test/kotlin/com/autodrive/app/architecture/CompetitionV52RegressionGuardTest.kt
```

ويحمي مستقبلاً من:

1. إعادة polling أو direct ranking sources.
2. إضافة حالة رابعة للـFeature Gate.
3. كسر Matrix `DISABLED / LOCKED / ACTIVE`.
4. تشغيل leaderboard refresh من `DISABLED` أو `LOCKED`.
5. منح Android صلاحية كتابة على Feature Flag.
6. ربط winner/rank بمنطق rollout.

## 6. Server contract documentation

تم تحديث:

```text
docs/autodrive-server-contract-v45.md
```

بقسم `Weekly competition rollout closure — v52` الذي يوثق:

- `autodrive_feature_flags`.
- الحالات الثلاث.
- Android read-only.
- Server-controlled rollout.
- Safe default.
- Matrix السلوك.
- عدم علاقة الـFlag بحساب winner/rank.
- أن التفعيل/الإيقاف لا يتطلب APK جديداً.

## 7. Static verification result

تم تشغيل فحص ساكن مستقل شمل:

- 7 checks للنصوص المحظورة.
- 8 checks للـGate matrix والمسارات.
- 5 checks لعقد السيرفر والصلاحيات والـseed.

النتيجة:

```text
20/20 static checks passed
STATIC_V52=PASS
```

## 8. Gradle execution

تمت محاولة تشغيل:

```text
./gradlew :app:testDebugUnitTest --tests com.autodrive.app.architecture.CompetitionV52RegressionGuardTest --offline
```

تعذر التنفيذ لأن Gradle wrapper حاول تنزيل:

```text
https://services.gradle.org/distributions/gradle-8.7-bin.zip
```

والبيئة الحالية لا تملك وصولاً للشبكة، وكانت النتيجة:

```text
java.net.UnknownHostException: services.gradle.org
```

لم يتم تغيير Gradle wrapper أو AGP أو Kotlin أو تعطيل أي اختبار.

## 9. Files changed in v52

لا يوجد أي تعديل على production Kotlin أو Navigation أو UI أو Repository أو Database.

التغييرات محصورة في:

```text
app/src/test/kotlin/com/autodrive/app/architecture/CompetitionV52RegressionGuardTest.kt
docs/autodrive-server-contract-v45.md
docs/verification-v52.md
```

## Result

**v52 مكتملة بالفحص الساكن والحماية من regression، ومرحلة المسابقة الأسبوعية مغلقة.**

`AutoDrive-v52.zip` يصبح Source of Truth الوحيد للمرحلة الثانية `v53–v55`.
