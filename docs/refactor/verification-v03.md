# Verification v03

## النطاق

منع تداخل المزامنة، توحيد أسبابها ونتيجتها، واستمرار الأقسام المستقلة بعد الفشل.

## الاختبارات المنفذة

- Single-flight: طلبان متزامنان نفذا محرك المزامنة مرة واحدة وانتظرا النتيجة نفسها.
- Partial success: فشل قسم واحد ظهر داخل `SyncState` دون إخفائه.
- Step isolation: فشل Profile لم يمنع تشغيل Notifications.
- النتيجة: `BEHAVIOR_TESTS_PASSED=3`.

## التجميع الساكن

نجح برمز خروج `0` للمجموعات التالية:

- عقود المزامنة و`DefaultSyncCoordinator` و`SyncStepExecutor`.
- `SyncManager` كاملًا مع عقود Stub.
- Home/Profile/Register ViewModels.
- Balance ViewModel.
- `NetworkMonitor` و`SyncModule`.
- `AutoDriveApp` وFCM وRealtime Status UI.

## فحص الحدود

- لا توجد استدعاءات `fullSync()` داخل Production.
- لا يستورد أي ViewModel `SyncManager`.
- جميع طلبات المزامنة تحمل `SyncReason`.
- `git diff --check`: ناجح.

## Gradle

```text
./gradlew testDebugUnitTest --tests ...
Exit code: 1
السبب: Gradle Wrapper حاول تنزيل gradle-9.0-milestone-1 وفشل بسبب عدم توفر الشبكة.
```

## المطلوب لإغلاق المرحلة

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```
