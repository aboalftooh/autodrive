# Verification v13

## النطاق

استخراج Gradle Modules من حدود Package-by-Feature المستقرة في V12، مع إبقاء `:app` نقطة تجميع وتنقل.

## ما تم

- أضيفت 15 وحدة Gradle:
  - 9 وحدات Core.
  - 6 وحدات Feature.
- نُقلت 223 ملفات Production إلى المالك المناسب أو أبقيت داخل `:app` للميزات غير المستقرة.
- نُقلت موارد Android إلى `:core:designsystem` كمالك مؤقت وحيد.
- فُصل `SyncManager` و`SyncDiagnostics` وChat DTOs عن مواقعها القديمة لكسر الدورات.
- أضيف `BottomNavBadgeSource` لعكس الاعتماد بين Design System وNotifications.
- حُدثت اختبارات المعمارية وأدوات Python لتقرأ جميع Modules.
- أضيف `GradleModuleArchitectureTest` و`tools/verify_modules_v13.py`.

## نتائج التحقق المنفذ

| التحقق | النتيجة |
|---|---:|
| اختبارات السلوك المستقلة | 48/48 ناجحة |
| المراجعات المعمارية | 74/74 ناجحة |
| فحوص Gradle Modules | 60/60 ناجحة |
| فحوص Package-by-Feature | 24/24 ناجحة |
| فحوص Room وSQLite | 74/74 ناجحة |
| فحوص Observability والأمان | 21/21 ناجحة |
| التجميع الساكن | 4/4 ناجح |

## التجميع الساكن

نجح تجميع:

1. `:core:model` كاملًا بواسطة `kotlinc`.
2. `:core:common` كاملًا مع `:core:model` على Classpath.
3. مجموعة سلوك عابرة للوحدات مع 48 اختبارًا.
4. مجموعة اختبارات المعمارية مع 74 مراجعة.

## ما لم يتم

لم تبدأ مهام Gradle التالية لأن Wrapper يحتاج تنزيل `gradle-9.0-milestone-1` والبيئة الحالية بلا شبكة ولا تحتوي التوزيع محليًا:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

الخطأ المثبت:

```text
java.net.UnknownHostException: services.gradle.org
```

## أوامر الإغلاق داخل بيئة متصلة

```bash
./gradlew clean
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

## الحالة

`BLOCKED`: التنفيذ والتحقق الساكن ناجحان، لكن الإغلاق النهائي ينتظر Gradle/KSP/Android resource linking.
