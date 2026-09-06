# Verification v01

## النطاق

حماية Room والبيانات المحلية دون تغيير Room version 10 أو قواعد العمل.

## ما تم تنفيذه

- تفعيل `exportSchema = true`.
- إعداد `room.schemaLocation` لـKSP.
- إزالة `fallbackToDestructiveMigration()`.
- توحيد تسجيل Migrations داخل `ALL_MIGRATIONS`.
- توثيق الدعم المتصل من Room 4 إلى Room 10.
- إضافة Migration test يحفظ:
  - صفًا داخل `pending_operations`.
  - رسالة `PENDING` داخل `chat_messages`.
  - بيانات المحادثة والإشعار أثناء الترقية.
- إضافة اختبار يثبت رفض Room 3 دون حذف جدول وبيانات Sentinel.
- إضافة اختبارات معمارية تمنع رجوع الإعدادات الخطرة.

## التحقق المنجز

### 1. الفحص الساكن

- لا توجد `fallbackToDestructiveMigration()` داخل Production.
- `exportSchema = true` موجودة.
- `room.schemaLocation` موجود.
- `AppModule` يستخدم `ALL_MIGRATIONS`.

النتيجة: **ناجح**.

### 2. التجميع الساكن الفعلي لـKotlin

جُمعت الملفات المعدلة التالية مع عقود Stub لـAndroid وRoom وHilt وJUnit:

- `AutoDriveDatabase.kt`
- `AppModule.kt`
- `DatabaseMigrationTest.kt`
- `DatabaseSafetyArchitectureTest.kt`

أداة التجميع:

```text
kotlinc-jvm 1.9.0 — JRE 21.0.10
```

الأمر المختصر:

```bash
kotlinc static-compile-v01/src/*.kt \
  -d static-compile-v01/out/v01-static.jar
```

النتيجة:

```text
Exit code: 0
Output: v01-static.jar
SHA-256: 0436740509dae12e22a2c634257211ad46c6f1cd83f4cd4ff6441d28e0d14c78
```

الحكم: **ناجح**. لا توجد أخطاء صياغة أو أنواع في الملفات الأربعة ضمن العقود المستخدمة.

> حدود هذا التحقق: عقود Stub تثبت سلامة Kotlin الأساسية، لكنها لا تستبدل Gradle أو KSP أو Android Instrumentation الفعلي.

### 3. محاكاة SQLite لمسار 4→10

تم إنشاء Schema 4، وإدخال عملية معلقة ورسالة غير مرسلة، ثم تطبيق SQL الخاص بالمراحل 4→10.

النتيجة: **ناجح**؛ بقيت الصفوف، وأضيفت الجداول والأعمدة المتوقعة.

## ما لم يتم

تعذر تشغيل:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug --offline
```

السبب:

```text
UnknownHostException: services.gradle.org
```

Gradle Wrapper يحتاج `gradle-9.0-milestone-1-bin.zip`، وهو غير موجود في Cache والبيئة بلا اتصال. لذلك:

- لم تعمل Unit Tests عبر Gradle.
- لم يعمل KSP، ولم يُولد `app/schemas/.../10.json`.
- لم يعمل Instrumentation test على جهاز أو Emulator.
- لا تُعد المرحلة `DONE` بعد.

## أوامر الإغلاق المطلوبة

```bash
./gradlew testDebugUnitTest \
  --tests "com.autodrive.app.architecture.DatabaseSafetyArchitectureTest"

./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.autodrive.app.core.database.DatabaseMigrationTest

./gradlew lintDebug
./gradlew assembleDebug
```

بعد نجاح KSP يجب التأكد من وجود:

```text
app/schemas/com.autodrive.app.core.database.AutoDriveDatabase/10.json
```

## اختبار يدوي مطلوب

1. تثبيت النسخة الجديدة فوق نسخة تحتوي قاعدة Room قديمة.
2. التأكد من بقاء الفواتير والرصيد والإشعارات والمحادثات.
3. التأكد من بقاء الرسائل المعلقة والعمليات غير المرسلة.
4. فتح التطبيق دون Crash بعد الترقية.

## الحكم

- تنفيذ المرحلة: **مكتمل**.
- الفحص الساكن: **ناجح**.
- التجميع الساكن: **ناجح**.
- محاكاة SQLite: **ناجحة**.
- الإغلاق النهائي: **BLOCKED** حتى نجاح Gradle وKSP وInstrumentation.
