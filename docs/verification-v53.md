# AutoDrive v53 — Reports Data Semantics + State Model

## Scope

تم تنفيذ الجلسة **v53 فقط** انطلاقاً من `AutoDrive-v52.zip` وفق خطة `AutoDrive_Competition_Reports_Settings_Execution_Plan_v49-v58.md`.

لم يتم تنفيذ إعادة تصميم التقارير الخاصة بـv54.

## 1. Week boundaries

تم حذف اعتماد `ReportsViewModel` على fallback المحلي:

```text
fallbackLastFriday9AM()
fallbackNextFriday9AM()
```

المصدر الوحيد لحدود الأسبوع في Reports أصبح:

```text
summary.weekStartMs
```

والحدود:

```text
currentWeekStart = summary.weekStartMs
currentWeekEnd   = currentWeekStart + 7 days
previousStart    = currentWeekStart - 7 days
previousEnd      = currentWeekStart
```

لا يوجد timezone جديد داخل Reports.

## 2. Current / previous week metrics

تمت إضافة وحساب:

```text
currentWeekPurchases
previousWeekPurchases
currentWeekCommissions
previousWeekCommissions
currentWeekInvoiceCount
previousWeekInvoiceCount
```

القواعد:

- المشتريات = `sum(invoice.totalAmount)` للفواتير التي يقع `createdAt` داخل حدود الأسبوع.
- العمولات = `sum(entry.amount)` للـCommission entries التي يقع `createdAt` داخل حدود الأسبوع.
- لم تتم إعادة كتابة أو إعادة تفسير أهلية العمولة محلياً.
- التاريخ غير القابل للتحليل لا يدخل الأسبوع الحالي أو السابق.

## 3. Lifetime / financial semantics

تم الاحتفاظ بـ:

```text
joinDate
balance
pending
lifetimeCommissions
```

`pending` يأتي من `CommissionSummary.pending` بدلاً من إنشاء منطق أهلية جديد داخل Reports.

`lifetimeCommissions` = مجموع كل Commission entries الموجودة في المصدر الحالي.

## 4. Trend model

تم إنشاء:

```kotlin
enum class TrendDirection {
    UP,
    DOWN,
    FLAT,
    NEW
}

data class TrendComparison(
    val direction: TrendDirection,
    val percent: Int? = null
)
```

القواعد المنفذة:

```text
previous=0,current=0 → FLAT,0
previous=0,current>0 → NEW,null
current=previous     → FLAT,0
current>previous     → UP,percent
current<previous     → DOWN,percent
```

الحساب يستخدم:

```text
BigDecimal
RoundingMode.HALF_UP
```

ولا يستخدم `Double` للحساب.

## 5. Reports load state

تم استبدال `isLoading: Boolean` بـ:

```text
ReportsLoadState.LOADING
ReportsLoadState.CONTENT
ReportsLoadState.ERROR
```

وأضيف:

```text
errorMessage
```

السلوك:

- خطأ قبل وجود Content → `ERROR`.
- خطأ بعد وجود Content → تبقى القيم السابقة و`CONTENT` مع `errorMessage` تحذيري.
- لا يتم تحويل الفشل إلى أرقام صفرية معروضة كمحتوى.
- تمت إضافة `retryReports()` لإعادة الاشتراك في المصادر بعد خطأ أولي.

## 6. Competition semantics

`winCount` أصبح:

```kotlin
Int?
```

المعنى:

```text
null = غير معروف
0    = تم التحميل ولم يفز
>0   = عدد مرات الفوز
```

تم منع Reports من طلب Competition data عند `DISABLED/LOCKED`:

- `ActivityLogScreen` يمرر قرار Gate الموجود أصلاً إلى ViewModel.
- ViewModel لا يبدأ Competition refresh في `init`.
- Competition stream يبدأ فقط عند `CompetitionAvailability.ACTIVE`.
- عند الخروج من ACTIVE يتم إلغاء Job وإرجاع `winCount=null`.

هذا تعديل تكاملي صغير فقط؛ لم تتم إعادة تصميم UI الخاصة بـv54.

## 7. UI compile / state integration only

تم تعديل `ActivityLogScreen.kt` بالحد الأدنى اللازم لـv53:

- استخدام `ReportsLoadState` بدلاً من `isLoading`.
- عدم عرض Fake zeros عند `ERROR`؛ استخدام `ErrorScreen` الحالي.
- استخدام `lifetimeCommissions` بدلاً من الاسم القديم `totalCommissions`.
- `winCount=null` يعرض `—` بدلاً من تفسيره كصفر.
- تمرير Competition Gate إلى ViewModel.

لم يتم تنفيذ Hero الأسبوع الحالي أو Trend cards أو hierarchy الجديدة؛ هذه تخص v54.

## 8. Tests written

تمت إضافة:

```text
app/src/test/kotlin/com/autodrive/app/feature/reports/presentation/log/ReportsViewModelTest.kt
```

ويغطي:

1. حدود الأسبوع الحالي تستخدم `summary.weekStartMs`.
2. حدود الأسبوع السابق صحيحة.
3. إجماليات المشتريات صحيحة.
4. إجماليات العمولات صحيحة.
5. التواريخ غير الصالحة لا تدخل الأسبوع.
6. `previous=0/current>0 → NEW`.
7. `previous=current → FLAT`.
8. الاتجاه السالب → `DOWN`.
9. لا `Double` في حساب Trend.
10. الخطأ الأولي → `ERROR` وليس fake content.
11. الخطأ اللاحق يحافظ على القيم السابقة.
12. Competition RPC مربوط بحالة ACTIVE فقط.
13. `winCount=null` يبقى unknown.
14. `pending` يستخدم `CommissionSummary` دون إعادة أهلية محلية.

## 9. Static verification executed

تم تشغيل فحص ساكن مستقل على شروط v53:

```text
15/15 checks passed
STATIC_V53=PASS
```

وتأكد من:

- وجود Load states الثلاث.
- وجود Trend states الأربع.
- وجود كل حقول الأسبوع الحالي والسابق.
- `winCount` nullable.
- `summary.weekStartMs` هو مصدر حدود الأسبوع.
- عدم وجود fallback المحلي داخل `ReportsViewModel`.
- استخدام `BigDecimal/HALF_UP`.
- عدم استخدام `Double`.
- semantics الخطأ الأولي واللاحق.
- Competition Gate قبل أي refresh.
- Error UI لا يعرض fake zeros.

## 10. Gradle execution

تمت محاولة تشغيل:

```text
./gradlew :app:testDebugUnitTest --tests com.autodrive.app.feature.reports.presentation.log.ReportsViewModelTest
```

تعذر التنفيذ لأن Gradle wrapper حاول تنزيل:

```text
https://services.gradle.org/distributions/gradle-8.7-bin.zip
```

والبيئة الحالية لا تملك وصولاً للشبكة:

```text
java.net.UnknownHostException: services.gradle.org
```

لم يتم تغيير Wrapper أو AGP أو Kotlin أو Dependencies أو تعطيل أي اختبار.

## 11. Files changed in v53

```text
app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ReportsUiState.kt
app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ReportsViewModel.kt
app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ActivityLogScreen.kt
app/src/test/kotlin/com/autodrive/app/feature/reports/presentation/log/ReportsViewModelTest.kt
docs/verification-v53.md
```

## Result

**v53 مكتملة بالفحص الساكن ودلالات البيانات والحالات المطلوبة، دون تنفيذ v54.**

`AutoDrive-v53.zip` هو Source of Truth للجلسة التالية `v54`.
