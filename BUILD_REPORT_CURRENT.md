# تقرير البناء الحالي — AutoDrive-v52

التاريخ: 2026-08-13

## النتيجة

- البناء: `BUILD SUCCESSFUL`
- المهمة: `:app:assembleDebug`
- ملف APK: `autodrive-debug.apk`
- ناتج Gradle: `app/build/outputs/apk/debug/app-debug.apk`
- الحجم: `41,815,510` بايت
- SHA-256: `750693bce57ff9fccf9182cce46364faae421f7133213933c5c3664e114ab691`
- الحزمة: `com.autodrive.app`
- الإصدار: `1.0.0` (versionCode `1`)
- minSdk: `26` — targetSdk: `35`
- التوقيع: APK Signature Scheme v2 صالح

## ربط السيرفر وتنفيذ SQL

- تم استخدام توكن الإدارة المقدم للوصول إلى مشروع Supabase `madkfvggyolmdberzmtb`.
- حالة المشروع عند التحقق: `ACTIVE_HEALTHY`، المنطقة: `eu-west-1`.
- تم تنفيذ نقطة SQL الإدارية بنجاح (`HTTP 201`).
- تم تنفيذ migrations الناقصة فقط داخل معاملة واحدة، ثم التحقق منها:
  - `20260812160000_phone_otp_single_active`
  - `20260813070000_weekly_competition_feature_gate`
- تم التحقق من الفهارس `phone_otps_one_unused_per_phone` و`idx_one_pending_withdrawal_per_client`.
- تم التحقق من feature flag: `weekly_competition = DISABLED`.
- لم يتم حفظ توكن الإدارة في الكود أو APK أو التقرير أو الأرشيف.
- يستخدم التطبيق إعدادات الاتصال الموجودة أصلًا عبر `local.properties` وحقلي `AUTODRIVE_SUPABASE_URL` و`AUTODRIVE_SUPABASE_ANON_KEY`؛ لم تتغير المعمارية.

## التغييرات المحدودة

1. إضافة `@OptIn(ExperimentalMaterial3Api::class)` إلى composable الداخلي الذي يستخدم `PullToRefreshBox` في شاشة المنافسة الأسبوعية، لإصلاح خطأ الترجمة.
2. أُنشئ `app/google-services.json` مؤقتًا لنسخة Debug فقط لأن إضافة Google Services تتطلبه، ثم حُذف بعد نجاح البناء. لا يوجد هذا الملف في الأرشيف النهائي.

لم تتغير حدود الوحدات أو اتجاهات الاعتماد أو المعمارية متعددة الوحدات.

## التحقق

- `:app:assembleDebug`: ناجح.
- فحص الحزمة والتوقيع: ناجح.
- فحص عدم وجود توكن الإدارة داخل APK: ناجح.
- `verify_modules_v13.py`: `62/62 PASS`.
- `verify_package_v12.py`: `24/24 PASS`.
- `verify_room_v10.py`: `21/21 PASS`.
- `verify_observability_v11.py`: `21/21 PASS`.
- `verify_cleanup_v14.py`: `13/15 PASS`؛ الفشلان متعلقان بوجود ملفات Weekly Competition غير مستخدمة سابقًا، وليس بفشل البناء.
- مجمّع `scripts/verify-v01.sh` لم يبدأ لأن `kotlinc` غير مثبت مستقلًا في البيئة؛ Gradle Kotlin compilation نجح.

## التحذيرات غير المانعة

- AGP `8.5.2` يحذر من استخدام `compileSdk 35`.
- Kotlin Gradle Plugin محمل في عدة وحدات.
- توجد تحذيرات deprecation في Compose وAndroidX.
- تعذر strip لبعض مكتبات native فتم تضمينها كما هي.

## مصدر الحقيقة

النسخة المرجعية بعد هذا البناء هي `AutoDrive-v52.zip` المرفق بجانب هذا التقرير، ويحتوي على المصدر وmigrations وتقرير البناء و`autodrive-debug.apk`، ويستبعد `local.properties` وملفات Gradle الناتجة والأسرار.
