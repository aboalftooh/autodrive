# AutoDrive — Refactor v01 Documentation

## الجلسة

`v01 — Baseline and Test Gates`

## النطاق المعتمد

تنفيذ بوابات خط الأساس بالتجميع والفحص الساكن فقط، وفق قرار المستخدم بعدم تشغيل Gradle أو Android Build داخل هذه البيئة.

## ما أُضيف

- `scripts/verify-v01-static.sh`: بوابة تحقق موحدة قابلة لإعادة التشغيل.
- `scripts/test-inventory-v01.py`: جرد حتمي لكل حالات `@Test` وتصنيفها حسب قابلية التشغيل الساكن.
- `scripts/static-v01-support/`: Runner وStubs مستقلة لتشغيل اختبارات Kotlin غير المرتبطة بـAndroid.
- `docs/verification-v01.md`: نتائج الجلسة وحدودها.
- `docs/test-inventory-v01.md`: توزيع الاختبارات وسبب عدم تشغيل بعض الفئات.
- `docs/refactor-plan.md`: نسخة الخطة النشطة التي طلب المستخدم التنفيذ وفقها.

## ما لم يتغير

- كود الإنتاج داخل `app` و`core` و`feature`.
- Room schema/version 13.
- قواعد الأعمال والمزامنة وOutbox.
- الواجهات والتنقل والموارد.
- ملفات Gradle الخاصة بالبناء.

## قرار التنفيذ

لا تُجرى إصلاحات معمارية في v01. الجلسة تثبت القياس فقط؛ يبدأ التغيير المعماري في v02 بعد اعتماد نتائج البناء الفعلي من Android Studio.
