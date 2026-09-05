# سياسة Room Migrations — AutoDrive

## النسخة الحالية

- قاعدة البيانات: `AutoDriveDatabase`
- الإصدار الحالي: `10`
- أقدم إصدار مدعوم: `4`

## مسار الترقية المدعوم

| من | إلى | التغيير |
|---:|---:|---|
| 4 | 5 | إضافة `conversations.subject` |
| 5 | 6 | إضافة أعمدة وسائط الرسائل |
| 6 | 7 | إنشاء `dynamo_content` |
| 7 | 8 | إضافة `chat_messages.local_path` |
| 8 | 9 | إنشاء `weekly_leaderboard_cache` |
| 9 | 10 | إضافة `notifications.nav_route` |

## قواعد إلزامية

1. يمنع استخدام `fallbackToDestructiveMigration()`.
2. يمنع رفع `AUTODRIVE_DATABASE_VERSION` دون Migration متصلة.
3. تضاف كل Migration إلى `ALL_MIGRATIONS`.
4. يجب نجاح `DatabaseMigrationTest` قبل الدمج.
5. يجب حفظ ملف Schema الذي يولده KSP داخل `app/schemas`.
6. النسخة الأقدم من 4 تُرفض دون حذف البيانات تلقائيًا.

## أوامر التحقق

```bash
./gradlew testDebugUnitTest \
  --tests "com.autodrive.app.architecture.DatabaseSafetyArchitectureTest"

./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.autodrive.app.core.database.DatabaseMigrationTest

./gradlew lintDebug assembleDebug
```

## Version 12 — Exact Money

- تحويل الأعمدة المالية في سبعة جداول من `REAL` إلى `TEXT`.
- استخدام `CAST(... AS TEXT)` للقيم القديمة و`BigDecimalConverters` للجديدة.
- إعادة فهرس `marketer_balance.user_id`.
- الحفاظ على الفواتير والمدفوعات والعمولات والأرصدة والحركات والسحوبات والترتيب.
- المسار المدعوم: `4 → 5 → 6 → 7 → 8 → 9 → 10 → 11 → 12`.

