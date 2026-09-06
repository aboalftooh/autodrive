# Verification v14

## النطاق

إغلاق إعادة الهيكلة: حذف الجسور وواجهات التوافق والكود غير المستخدم، تثبيت مخطط الاعتماديات النهائي، وتحديث التوثيق دون تغيير قواعد العمل أو Room schema 13.

## ما تم

- أزيل Bridge التسجيل من `:app`.
- نُقلت عقود التسجيل والخروج العابرة للميزات إلى `:core:common`.
- أزيل اعتماد `:feature:profile` على `:feature:auth`.
- حُذف alias جلسة غير مستخدم وكود Competition presentation غير المتصل بالشاشة.
- أزيلت APIs الانتقالية الموسومة `@Deprecated`، وسُميت حدود الأسبوع المحلية كـFallback للعرض فقط.
- ثُبّت Gradle Wrapper على 8.7 المستقر.
- حُدثت المعمارية الفعلية والمستهدفة وقواعد الاعتماديات.

## نتائج التحقق المنفذ

| التحقق | النتيجة |
|---|---:|
| اختبارات السلوك المستقلة | 48/48 ناجحة |
| المراجعات المعمارية | 81/81 ناجحة |
| فحوص Gradle Modules | 60/60 ناجحة |
| فحوص Package-by-Feature | 24/24 ناجحة |
| Migration statements | 21/21 ناجحة |
| حفظ الصفوف أثناء Migration | 13/13 ناجحة |
| فهارس Room | 20/20 ناجحة |
| خطط الاستعلام | 20/20 ناجحة |
| Observability والأمان | 21/21 ناجحة |
| فحوص التنظيف | 15/15 ناجحة |
| التجميع الساكن | 6/6 ناجح |

## التجميع الساكن

نجح تجميع:

1. `:core:model` و`:core:common` كاملين.
2. عقود التسجيل والخروج وAdapters/Actions المعدلة.
3. Hilt bindings المعدلة.
4. `RegisterViewModel` و`ProfileViewModel` مع عقود Stub.
5. مجموعة اختبارات السلوك كاملة.
6. مجموعة اختبارات المعمارية كاملة.

## Gradle

حاول الأمر التالي بعد تثبيت Wrapper على Gradle 8.7:

```bash
./gradlew clean testDebugUnitTest lintDebug assembleDebug
```

لم يبدأ البناء لأن البيئة بلا شبكة ولا تحتوي توزيع Gradle 8.7 محليًا:

```text
java.net.UnknownHostException: services.gradle.org
```

## ما لم يتم

- KSP وRoom schema generation الفعليان.
- Android resource linking.
- Robolectric عبر Gradle.
- `assembleDebug` و`assembleRelease`.
- اختبار APK على جهاز فعلي.
- اختبار ترقية التطبيق فوق نسخة إنتاج مثبتة.

## أوامر الإغلاق داخل بيئة متصلة

```bash
./gradlew clean
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew assembleRelease
```

ثم تثبيت النسخة فوق APK إنتاج سابق والتحقق من بقاء البيانات ومسارات الاستخدام الحرجة.

## الحالة

`BLOCKED`: التنفيذ والتحقق الساكن مكتملان؛ الإغلاق التنفيذي ينتظر Gradle وRelease واختبار الجهاز والترقية.
