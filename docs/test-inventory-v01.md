# Test Inventory v01

## الإجمالي

المشروع يعرّف **142** حالة `@Test` داخل **32** ملف اختبار فعلي؛ ملف `ProjectLayout.kt` مساعد وليس ملف اختبارات.

| الفئة | الملفات | الحالات | الحالة في v01 |
|---|---:|---:|---|
| Unit خالصة قابلة للتشغيل بـKotlin | 14 | 48 | شُغلت |
| Unit مرتبطة بـMockK/Android | 2 | 10 | لم تُشغّل بطلب المستخدم بعدم استخدام Gradle |
| Architecture | 14 | 81 | شُغلت |
| Instrumentation | 2 | 3 | لم تُشغّل؛ تحتاج Android Runtime/Device |
| **الإجمالي** | **32** | **142** | **129 شُغلت، 13 مؤجلة** |

## الاختبارات المؤجلة

- `ChatRepositoryImplTest`: خمس حالات تعتمد على Android Context وMockK وسلسلة اعتماديات Android/Database.
- `NotificationRepositoryImplTest`: خمس حالات تعتمد على MockK وRoom/Supabase contracts.
- `DatabaseMigrationTest`: حالتان Instrumentation للتحقق من Room فعليًا.
- `SessionSwitchIsolationTest`: حالة Instrumentation لعزل الجلسات داخل قاعدة فعلية.

هذه الحالات ليست فاشلة ولا مجهولة؛ حالتها `NOT_RUN_BY_SCOPE` حتى تشغيلها داخل Android Studio.
