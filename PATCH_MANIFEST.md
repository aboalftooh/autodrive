# PATCH MANIFEST — AutoDrive V14

## النطاق

الإغلاق والتنظيف بعد استخراج Gradle Modules، دون تغيير Room schema 13 أو قواعد العمل.

## الملخص

- ملفات مضافة: 11.
- ملفات معدلة: 22.
- ملفات محذوفة: 5.
- Bridge انتقالي محذوف: 1.
- اعتماد Feature مباشر محذوف: `:feature:profile -> :feature:auth`.
- كود Presentation ميت محذوف: ملفان.
- Gradle Wrapper: 8.7 مستقر.

## النتائج

- اختبارات السلوك: 48/48.
- المراجعات المعمارية: 81/81.
- فحوص الوحدات: 60/60.
- فحوص الحزم: 24/24.
- فحوص Room: 74/74.
- فحوص الأمان: 21/21.
- فحوص التنظيف: 15/15.
- التجميع الساكن: 6/6.
- Gradle: لم يبدأ بسبب غياب الشبكة وتوزيع Gradle المحلي.

## الملفات المضافة

- `app/src/test/kotlin/com/autodrive/app/architecture/ClosureCleanupArchitectureTest.kt`
- `core/common/src/main/kotlin/com/autodrive/app/core/common/registration/RegistrationProfileWriter.kt`
- `core/common/src/main/kotlin/com/autodrive/app/core/common/session/SignOutAction.kt`
- `docs/refactor/active-architecture-v14.md`
- `docs/refactor/module-graph-v14.md`
- `docs/refactor/target-architecture-v14.md`
- `docs/refactor/verification-v14.md`
- `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/domain/usecase/AuthSignOutAction.kt`
- `module-graph-v14.md`
- `tools/verify_cleanup_v14.py`
- `verification-v14.md`

## الملفات المعدلة

- `PATCH_MANIFEST.md`
- `app/src/main/kotlin/com/autodrive/app/feature/home/presentation/HomeViewModel.kt`
- `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/InvoiceListScreen.kt`
- `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/ReportsViewModel.kt`
- `app/src/main/kotlin/com/autodrive/app/feature/reports/presentation/log/WeeklyCommissionsScreen.kt`
- `core/session/src/main/kotlin/com/autodrive/app/core/session/data/PreferencesManager.kt`
- `dependency-rules.md`
- `docs/refactor/dependency-rules.md`
- `docs/refactor/refactor-changelog.md`
- `docs/refactor/refactor-plan.md`
- `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/di/AuthFeatureModule.kt`
- `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/register/RegisterViewModel.kt`
- `feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/data/CommissionRepositoryImpl.kt`
- `feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/domain/CommissionCalculator.kt`
- `feature/commission/src/main/kotlin/com/autodrive/app/feature/commission/presentation/CommissionReportScreen.kt`
- `feature/profile/build.gradle.kts`
- `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/data/RegistrationProfileWriterAdapter.kt`
- `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/di/ProfileFeatureModule.kt`
- `feature/profile/src/main/kotlin/com/autodrive/app/feature/profile/presentation/ProfileViewModel.kt`
- `gradle/wrapper/gradle-wrapper.properties`
- `refactor-changelog.md`
- `refactor-plan.md`

## الملفات المحذوفة

- `app/src/main/kotlin/com/autodrive/app/di/RegistrationBridgeModule.kt`
- `app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionUiState.kt`
- `app/src/main/kotlin/com/autodrive/app/feature/competition/presentation/WeeklyCompetitionViewModel.kt`
- `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/domain/repository/RegistrationProfileWriter.kt`
- `feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/domain/usecase/SignOutUseCase.kt`
