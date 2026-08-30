# Verification v02

## النطاق

توحيد مصدر الجلسة وعزل بيانات المستخدم والإشعارات والتنظيف عند تسجيل الخروج.

## التحقق المنجز

- لا يستورد Domain أو Presentation `PreferencesManager`.
- عقود الجلسة Kotlin خالصة.
- جميع عمليات Notification DAO الخاصة مقيدة بـ`user_id`.
- تنظيف الخروج يشمل البيانات المرتبطة بالمستخدم والعميل والمحادثات والعمليات المعلقة.
- أضيف اختبار تبديل حساب يمنع بقاء هوية أو حالة لوحة الحساب السابق.
- التجميع الساكن للمجموعات المعدلة: ناجح، Exit code 0.
- `git diff --check`: ناجح.

## Gradle

```text
./gradlew --offline testDebugUnitTest assembleDebug
Exit code: 1
سبب التوقف: Gradle Wrapper حاول تنزيل gradle-9.0-milestone-1، والبيئة بلا اتصال.
```

## التحقق المطلوب داخل بيئة Gradle مكتملة

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
```
