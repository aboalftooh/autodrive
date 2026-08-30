# تقرير بناء AutoDrive 1.0.0

التاريخ: 2026-08-29

## النتيجة

- الحالة: `BUILD SUCCESSFUL`
- الأمر المطلوب: `./gradlew assembleRelease --offline --build-cache`
- إعادة تحقق الإعدادات: نفس الأمر مع `--rerun-tasks` فقط لضمان إعادة توليد BuildConfig المعتمد على متغيرات البيئة.
- Gradle: 8.7
- المهمة: `:app:assembleRelease`
- التطبيق: `com.autodrive.app`
- الإصدار: `1.0.0`، versionCode `1`
- APK Gradle الخام: `app/build/outputs/apk/release/app-release-unsigned.apk`
- APK التسليم: `AutoDrive-1.0.0.apk` في جذر المشروع
- حجم APK التسليم: 10,394,002 بايت
- SHA-256 لـ APK التسليم: `7911fb80120c493299e10d778cf2a8ff270a20a7959d8e36d98b220fd4a4bc67`
- التوقيع: صالح باستخدام APK Signature Scheme v2 وv3، بمفتاح محلي؛ ليس مفتاح إنتاج/Play Store.

## ربط الخادم

- استُخدمت قيم Supabase من `secret.md` عبر متغيرات بيئة مؤقتة فقط، دون كتابتها في المصدر أو التقرير.
- اختبار `auth/v1/settings`: `HTTP 200`.
- تم التحقق من وجود قيم الاتصال في BuildConfig وداخل DEX في APK دون تسجيل القيم.
- لم يُعثر على توكن الإدارة داخل APK.

## التحقق والتنظيف

- البناء offline ولم يُنزّل Gradle أو dependencies.
- استفاد البناء الأول من Gradle build cache؛ ثم أُعيد تنفيذ المهام للتحقق من إعدادات البيئة.
- لم تُستخدم `clean` ولم تُحذف أي build أو cache سابق.
- الأرشيف يستبعد مجلدات build وGradle/Kotlin cache، الأسرار، `local.properties`، ملف Google Services المحلي، وكل ملفات APK.
- حجم الأرشيف النهائي أقل من 20MB.

## ملاحظات غير مانعة

- تحذير توافق AGP 8.5.2 مع compileSdk 35.
- تحذيرات deprecation في Compose/AndroidX ووجود جلسات Kotlin متعددة.
- تعذر strip لبعض المكتبات native فتم تضمينها كما هي.
- المحاولة الأولى احتاجت تحديد Android SDK الموجود مسبقًا في البيئة لأن `ANDROID_HOME` لم يكن مضبوطًا؛ لم يتطلب ذلك تعديل المشروع.

