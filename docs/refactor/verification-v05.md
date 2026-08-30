# Verification v05

## النطاق

فصل Realtime عن `SyncManager` إلى Participants حسب المجال، مع فلترة الحساب ومعالجة Insert وUpdate وDelete.

## الاختبارات المنفذة

- اختبار دورة Realtime الفعلية بعقود Fake:
  - بدء جميع Participants.
  - تمرير `userId` و`clientId` الصحيحين.
  - إعادة الاتصال بعد فشل Participant.
  - الانتقال إلى `CONNECTED` بعد اشتراك الجميع.
  - الإيقاف والعودة إلى `DISCONNECTED`.
- اختبار سياسة الملكية:
  - رفض `client_id` لحساب آخر.
  - رفض `user_id` لمستخدم آخر.
  - تمييز حدث Delete.
- اختبارات حدود معمارية:
  - `SyncManager` لا يملك Realtime APIs.
  - كل Participant يعالج Delete.
  - الجداول المالكة تستخدم فلاتر السيرفر.
  - `RealtimeManager` لا يعرف Room أو DTOs أو أسماء الجداول.

## نتيجة الاختبارات

- اختبارات السلوك المستقلة: ناجحة — Exit code 0.
- اختبارات الحدود الساكنة: ناجحة — Exit code 0.

## التجميع الساكن

جُمعت الملفات المعدلة ضمن أربع مجموعات:

1. Realtime + Sync + Coordinator + DI + Mappers: ناجح — Exit code 0.
2. DAOs المعدلة: ناجح — Exit code 0.
3. `AuthRepositoryImpl`: ناجح — Exit code 0.
4. اختبارات Kotlin المعدلة والجديدة: ناجح — Exit code 0.

## Gradle

جرى تشغيل:

```bash
./gradlew testDebugUnitTest \
  --tests "com.autodrive.app.core.sync.realtime.*" \
  --tests "com.autodrive.app.architecture.RealtimeArchitectureTest"
```

لم يبدأ Gradle لأن Wrapper حاول تنزيل `gradle-9.0-milestone-1` والبيئة بلا اتصال DNS إلى `services.gradle.org`.

## ملاحظات

- Room schema/version بقيت 11؛ لا توجد Migration في v05.
- جدول `payments` لا يحتوي `client_id` في العقد الحالي؛ يعتمد البث على RLS ثم يتحقق محليًا من ملكية الفاتورة قبل الكتابة أو الحذف.
- نجاح التجميع الساكن لا يحل محل Gradle/KSP واختبار الجهاز داخل بيئة المشروع الكاملة.
