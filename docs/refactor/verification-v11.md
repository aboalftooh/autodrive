# Verification v11

## النطاق

Observability والأمان: Crashlytics، تنقية السجلات، مؤشرات المزامنة، إزالة صلاحيات SMS، إعدادات البيئة، ومراجعة RLS.

## النتائج

- اختبارات السلوك: **17/17 ناجحة**.
  - تنقية البيانات الحساسة: 7/7.
  - سلوك المزامنة ومؤشراتها: 7/7.
  - تنقية أخطاء Outbox: 3/3.
- اختبارات SQLite الارتدادية: **74/74 ناجحة**.
- المراجعات المعمارية الكاملة: **59/59 ناجحة**.
  - مراجعات V11 الجديدة: 8/8.
- فحوص Observability والأمان: **21/21 ناجحة**.
- التجميع الساكن: **5/5 ناجح**.
  - Observability وLogger.
  - Sync Coordinator وStep Executor.
  - Realtime Manager.
  - Outbox policy/processor.
  - اختبارات المعمارية.

## Gradle

الأمر المطلوب:

```bash
./gradlew testDebugUnitTest assembleDebug
```

لم يبدأ لأن Gradle Wrapper حاول تنزيل `gradle-9.0-milestone-1` والبيئة بلا اتصال بالإنترنت.

## RLS

أُضيفت أداة `tools/verify_rls_v11.sql`، لكنها لم تُشغّل على قاعدة Supabase من هذه البيئة؛ لذلك صحة سياسات الإنتاج غير مثبتة بعد.

## اختبار يدوي مطلوب

1. تسجيل OTP تلقائيًا دون ظهور إذن الرسائل.
2. اختبار Debug والتأكد أن Crashlytics collection معطلة.
3. اختبار Release وإرسال Non-fatal تجريبي من بيئة اختبار.
4. فصل الشبكة وإعادتها ومراجعة أحداث Sync/Realtime.
5. إنشاء عملية Outbox فاشلة ومراجعة العدادات دون ظهور Payload أو مبلغ.
6. تشغيل `verify_rls_v11.sql` في Supabase ومراجعة النتائج.
