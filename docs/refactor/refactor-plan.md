# AutoDrive — خطة الإصلاح وإعادة الهيكلة التدريجية

## الهدف

تحويل المشروع من `Single-Module Layered Monolith` بحدود مخترقة إلى `Feature-first Modular Monolith` قابل للتوسع، مع حماية البيانات المالية والمحلية، والحفاظ على السلوك الحالي وقاعدة Room رقم 13، ودون إعادة كتابة شاملة.

## القرار المعماري

- لا يُعاد بناء التطبيق من الصفر.
- يبدأ الإصلاح داخل Module `app` الحالي.
- تُنشأ حدود ميزات وعقود واضحة أولًا.
- تُستخرج Gradle Modules فقط بعد استقرار الحدود.
- Room يبقى مصدر الحقيقة المحلي للقراءة.
- Supabase يبقى مصدر الحقيقة البعيد والقواعد المالية المعتمدة.
- كل كتابة غير مضمونة الاتصال تمر عبر Outbox موثوق.
- Realtime يحدّث Room ولا يكتب مباشرة إلى UI state.

## قواعد التنفيذ

1. لا يُغيَّر السلوك وقواعد العمل أثناء النقل إلا في إصلاح منفصل موثق.
2. لا تبدأ مرحلة قبل نجاح شرط إغلاق المرحلة السابقة.
3. لا تُقسَّم Room أو المزامنة أو Navigation دفعة واحدة.
4. كل مرحلة تنتهي ببناء واختبارات وتحقق يدوي واضح.
5. لا يُدّعى نجاح اختبار لم يُشغّل داخل جذر المشروع الكامل.
6. آخر نسخة كاملة من المشروع هي مصدر الحقيقة الوحيد.
7. لا يُنقل شيء إلى `core` إلا بوجود مستهلكين حقيقيين ومعنى ثابت.
8. تغييرات قاعدة البيانات والمال والمزامنة تُنفذ في إصدارات منفصلة قابلة للرجوع.
9. أي تعديل عبر Codex يجب أن يكون صغير النطاق وله شرط قبول محدد.
10. يمنع الجمع بين إصلاح معماري وتغيير واجهة أو قاعدة عمل في Commit واحد.

## حالات العمل

- `NOT_STARTED`: لم يبدأ.
- `IN_PROGRESS`: جارٍ.
- `BLOCKED`: متوقف بسبب نقص ملفات أو اعتماد خارجي.
- `DONE`: مكتمل ومتحقق.

## التقدم العام

| المرحلة | الحالة | النتيجة المطلوبة |
|---|---|---|
| 00. خط الأساس الكامل | BLOCKED | ملفات الجذر مكتملة؛ Gradle متوقف لغياب التوزيع محليًا وانقطاع الإنترنت |
| 01. حماية Room والبيانات | BLOCKED | التنفيذ مكتمل؛ إغلاق المرحلة ينتظر Gradle وInstrumentation |
| 02. الجلسة وعزل المستخدم | BLOCKED | التنفيذ والتجميع الساكن ناجحان؛ الإغلاق ينتظر Gradle وInstrumentation |
| 03. تنسيق المزامنة | BLOCKED | التنفيذ والاختبارات المستقلة والتجميع الساكن ناجحة؛ الإغلاق ينتظر Gradle |
| 04. Outbox والعمليات المعلقة | BLOCKED | التنفيذ والاختبارات المستقلة والتجميع الساكن ناجحة؛ الإغلاق ينتظر Gradle/KSP |
| 05. Realtime Participants | BLOCKED | التنفيذ والاختبارات المستقلة والتجميع الساكن ناجحة؛ الإغلاق ينتظر Gradle |
| 06. صحة الأموال | BLOCKED | التنفيذ والاختبارات والتجميع الساكن ناجحة؛ الإغلاق ينتظر Gradle/KSP/Instrumentation |
| 07. تنظيف حدود Domain وPresentation | BLOCKED | التنفيذ والاختبارات والتجميع الساكن ناجحة؛ الإغلاق ينتظر Gradle |
| 08. فصل الميزات المالكة | BLOCKED | التنفيذ والاختبارات والتجميع الساكن ناجحة؛ الإغلاق ينتظر Gradle |
| 09. تفكيك الملفات الضخمة | BLOCKED | التنفيذ والتحقق الساكن ناجحان؛ الإغلاق ينتظر Gradle |
| 10. أداء Room والعلاقات | BLOCKED | التنفيذ واختبارات SQLite والتجميع الساكن ناجحة؛ الإغلاق ينتظر Gradle/Instrumentation |
| 11. Observability والأمان | BLOCKED | التنفيذ والاختبارات الساكنة ناجحة؛ الإغلاق ينتظر Gradle والتحقق الفعلي من RLS |
| 12. Package-by-Feature | BLOCKED | التنفيذ والاختبارات والتجميع الساكن ناجحة؛ الإغلاق ينتظر Gradle |
| 13. استخراج Gradle Modules | BLOCKED | التنفيذ والتحقق الساكن ناجحان؛ الإغلاق ينتظر Gradle |
| 14. الإغلاق والتنظيف | BLOCKED | التنظيف والتحقق الساكن مكتملان؛ الإغلاق ينتظر Gradle وRelease والجهاز |

---

## المرحلة 00 — تثبيت خط الأساس الكامل

### الأعمال

- استلام جذر المشروع الكامل، ويتضمن:
  - `settings.gradle.kts`
  - Gradle Wrapper
  - `gradle/libs.versions.toml`
  - Module `app`
- إنشاء فرع Git مخصص للإصلاح.
- تشغيل:
  - `./gradlew clean`
  - `./gradlew testDebugUnitTest`
  - `./gradlew lintDebug`
  - `./gradlew assembleDebug`
- تسجيل الاختبارات الفاشلة الموجودة قبل التعديل.
- حفظ APK مرجعي وصور أو فيديو لمسارات الاستخدام الحرجة.
- تسجيل Room version الحالية: `10`.

### الاختبار اليدوي المرجعي

1. OTP والدخول والخروج.
2. كود الدعوة والتسجيل.
3. الصفحة الرئيسية والتحديث اليدوي.
4. عرض الفواتير والعمولات.
5. الرصيد وطلب السحب والإلغاء.
6. الإشعارات وفتح مسارها.
7. المحادثة والإرسال وإعادة المحاولة والوسائط.
8. تعديل الملف الشخصي.
9. فقد الشبكة ثم عودتها.
10. استقبال FCM أثناء فتح وإغلاق التطبيق.

### شرط الإغلاق

البناء والاختبارات الأساسية ناجحة، والسلوك المرجعي موثق قبل أي تعديل.

### الحالة الحالية

`BLOCKED`: استُلم جذر المشروع الكامل، لكن Gradle Wrapper يحاول تنزيل `gradle-9.0-milestone-1` والبيئة الحالية بلا إنترنت ولا تحتوي التوزيع محليًا؛ تعذر تثبيت خط الأساس التنفيذي.

---

## المرحلة 01 — حماية Room والبيانات المحلية

### المشكلة المثبتة

- `fallbackToDestructiveMigration()` مفعّل.
- `exportSchema = false`.
- قاعدة البيانات تحتوي بيانات مالية وعمليات معلقة ورسائل قد تُفقد عند Migration ناقصة.

### الأعمال

- تفعيل `exportSchema = true`.
- تحديد مجلد Schemas داخل إعدادات KSP.
- إزالة `fallbackToDestructiveMigration()`.
- توثيق جميع نسخ Room المدعومة ومسارات الترقية.
- إضافة Migration tests من أقدم نسخة مدعومة إلى النسخة الحالية.
- إضافة اختبار يحمي `pending_operations` والرسائل غير المرسلة أثناء الترقية.
- منع رفع إصدار Room دون Migration واختبار Schema.

### الملفات الأساسية

- `data/local/AutoDriveDatabase.kt`
- `di/AppModule.kt`
- `build.gradle.kts`
- `schemas/*`
- `src/androidTest/.../DatabaseMigrationTest.kt`

### شرط الإغلاق

- لا يوجد fallback تدميري.
- كل ترقية مدعومة تنجح دون فقد صفوف.
- Schema version 10 محفوظة ويمكن مقارنة النسخ اللاحقة بها.

### الحالة الحالية

`BLOCKED`: نُفذت تغييرات الحماية واختبارات Migration من 4 إلى 10 واختبارات منع الحذف. نجح التجميع الساكن ومحاكاة SQL، لكن لم يعمل Gradle/KSP/Instrumentation بسبب غياب توزيع Gradle والاتصال؛ لذلك لم يُولد `10.json` ولم تُغلق المرحلة نهائيًا.

---

## المرحلة 02 — الجلسة وعزل المستخدم

### المشكلة المثبتة

- ViewModels وUse Cases عديدة تقرأ `PreferencesManager` مباشرة.
- `NotificationRepositoryImpl.observeNotifications(userId)` يستخدم `observeAll()`.
- عدادات وعمليات قراءة الإشعارات غير مقيدة بالمستخدم.

### الأعمال

- إنشاء:
  - `SessionReader`
  - `SessionWriter`
  - `CurrentSession`
- جعل `userId` و`clientId` و`orgId` و`registrationState` من مصدر واحد.
- منع Domain وPresentation من استيراد `PreferencesManager`.
- تعديل Notification DAO ليقيد القراءة والعدد والتحديث بـ`user_id`.
- مراجعة جميع DAOs للتأكد من عزل بيانات مستخدم سابق بعد تسجيل الخروج أو تبديل الحساب.
- توثيق سياسة تنظيف البيانات عند الخروج دون حذف العمليات المطلوب الاحتفاظ بها خطأً.

### الملفات الأساسية

- `utils/PreferencesManager.kt`
- `domain/usecases/**`
- `ui/**/*ViewModel.kt`
- `data/local/dao/NotificationDao.kt`
- `data/repository/NotificationRepositoryImpl.kt`

### شرط الإغلاق

- لا يستورد أي ViewModel أو Domain class `PreferencesManager`.
- كل بيانات محلية خاصة بالمستخدم مفلترة بمعرف مالك واضح.
- اختبارات تبديل المستخدم تمنع تسرب البيانات بين الحسابات.

### الحالة الحالية

`BLOCKED`: اكتمل إنشاء عقود الجلسة، ونُقلت قراءات Domain وPresentation إليها، وقُيدت الإشعارات بـ`user_id`، وأضيف تنظيف بيانات الحساب واختبار تبديل المستخدم. نجح التجميع الساكن وفحص الحدود. تعذر Gradle وInstrumentation لأن توزيع Gradle غير متاح محليًا والاتصال بالشبكة معطل.

---

## المرحلة 03 — تنسيق المزامنة ومنع التداخل

### المشكلة المثبتة

- `fullSync()` يُستدعى من التطبيق والشبكة وFCM وعدة ViewModels.
- لا يوجد Single-flight أو `Mutex` ظاهر.
- الفشل داخل `runCatching` قد يوقف بقية المزامنة دون نتيجة واضحة.

### الأعمال

- إنشاء `SyncCoordinator` كواجهة وحيدة للمزامنة.
- إضافة Single-flight باستخدام `Mutex` أو Job deduplication.
- استبدال `fullSync()` العام بطلبات ذات سبب:
  - `APP_START`
  - `NETWORK_RESTORED`
  - `USER_REFRESH`
  - `FCM_HINT`
  - `LOGIN_SUCCESS`
- إضافة `SyncState` يحتوي الحالة والمرحلة والخطأ وآخر نجاح.
- جعل UI يطلب مزامنة عبر Contract ولا يعتمد على `SyncManager` مباشرة.
- فصل خطوات المزامنة بحيث لا يمنع فشل قسم مستقل بقية الأقسام غير التابعة له.
- توثيق ترتيب المزامنة وسياسة التعارض.

### الملفات الأساسية

- `data/sync/SyncManager.kt`
- `AutoDriveApp.kt`
- `notifications/AutoDriveFirebaseMessagingService.kt`
- `ui/screens/home/HomeViewModel.kt`
- `ui/screens/profile/ProfileViewModel.kt`
- `ui/screens/balance/BalanceViewModel.kt`
- `ui/screens/register/RegisterViewModel.kt`

### شرط الإغلاق

- لا يستدعي أي ViewModel `SyncManager` مباشرة.
- لا تعمل مزامنتان كاملتان في الوقت نفسه.
- لكل فشل نتيجة مسجلة وقابلة للعرض أو التشخيص.

### الحالة الحالية

`BLOCKED`: نُفذ `SyncCoordinator` وSingle-flight وأسباب المزامنة وحالة `SyncState`، وفُصلت خطوات السحب بحيث يستمر المستقل منها بعد الفشل. نجحت اختبارات السلوك المستقلة والتجميعات الساكنة، بينما تعذر Gradle لأن توزيعه غير متاح محليًا والبيئة بلا شبكة.

---

## المرحلة 04 — Outbox والعمليات المعلقة

### المشكلة المثبتة

`PendingOperationEntity` يحتوي `retryCount` فقط، دون حد أو موعد إعادة محاولة أو سبب فشل أو حالة نهائية.

### الأعمال

- توسيع نموذج العملية المعلقة ليشمل:
  - `status`
  - `attempt_count`
  - `next_retry_at`
  - `last_error_code`
  - `last_error_message`
  - `payload_version`
  - `idempotency_key`
- إضافة Backoff موحد مع Jitter.
- تحديد حد أقصى للمحاولات وحالة `DEAD_LETTER`.
- عدم حذف العملية إلا بعد تأكيد نجاح بعيد.
- توثيق العمليات القابلة للإعادة بأمان.
- منع ازدواج طلب السحب أو تحديث القراءة أو الملف الشخصي.
- إضافة شاشة أو سجل تشخيص داخلي للعمليات العالقة.

### شرط الإغلاق

كل عملية معلقة تنتهي إلى `SUCCEEDED` أو `DEAD_LETTER`، ولا تعاد بلا نهاية أو تتكرر ماليًا.

### الحالة الحالية

`BLOCKED`: رُفع Room إلى 11 مع Migration تحفظ العمليات القديمة، وأضيفت المطالبة والـBackoff والحد الأقصى و`DEAD_LETTER` ومفاتيح Idempotency وتشخيص العمليات. نجحت اختبارات السلوك وSQLite والحدود والتجميع الساكن. تعذر Gradle/KSP لعدم توفر التوزيع محليًا وانقطاع الشبكة.

---

## المرحلة 05 — فصل Realtime إلى Participants

### المشكلة المثبتة

- `SyncManager` يملك اشتراكات جميع الجداول.
- أغلب الاشتراكات غير مفلترة على السيرفر.
- معظم الجداول لا تعالج `Delete`.

### الأعمال

- إنشاء `RealtimeParticipant` لكل مجال:
  - Invoices
  - Payments
  - Commissions
  - Balance
  - Withdrawals
  - Notifications
  - Chat
- تطبيق فلتر `client_id` أو `user_id` على السيرفر كلما دعمه العقد.
- معالجة Insert وUpdate وDelete لكل جدول حسب سياسة المجال.
- جعل Participant يحدّث Room فقط.
- جعل Coordinator مسؤولًا عن الاشتراك والإلغاء وإعادة الاتصال فقط.
- إضافة اختبارات فك DTO والتصفية والحذف.

### شرط الإغلاق

`SyncCoordinator` لا يعرف DAOs أو DTOs لكل المجالات، ولا تبقى سجلات محذوفة على السيرفر كبيانات وهمية محليًا.

### الحالة الحالية

`BLOCKED`: نُقلت اشتراكات Realtime من `SyncManager` إلى أربعة Participants مستقلة: الفواتير والمدفوعات والعمولات، الرصيد والحركات والسحوبات، الدردشة، والإشعارات. طُبقت فلاتر `client_id` و`user_id` على الجداول المالكة، وأضيفت معالجة Insert/Update/Delete وحماية ملكية محلية قبل الحذف. نجحت اختبارات السلوك والحدود والتجميع الساكن؛ تعذر Gradle بسبب عدم توفر التوزيع محليًا وانقطاع الشبكة.

---

## المرحلة 06 — صحة نموذج الأموال

### المشكلة المثبتة

- القيم المالية كانت تستخدم `Double` في Domain وRoom وDTO وPresentation.
- العمليات العشرية قد تنتج فروقات ثنائية غير مقصودة.
- الرصيد والعمولة والسحب والترتيب الأسبوعي يجب أن تبقى دقيقة أثناء التخزين والنقل والحساب.

### الأعمال

- إنشاء `Money` فوق `BigDecimal` مع عملة موحدة وعمليات دقيقة.
- تحويل الحقول المالية في Domain وRoom وDTO إلى `Money` أو `BigDecimal`.
- إضافة Serializer وRoom converter دقيقين.
- رفع Room من 11 إلى 12 وإضافة Migration من `REAL` إلى `TEXT`.
- حصر التحويل إلى `Double` في العرض الرسومي والتوافق القديم فقط.
- تحديث الرصيد والسحب والعمولات والتقارير والترتيب والطباعة وFCM.
- إضافة اختبارات دقة وترحيل وحدود اعتماديات.

### شرط الإغلاق

- لا تستخدم القواعد أو التخزين أو DTOs المالية `Double`.
- Migration 11→12 تحفظ القيم والصفوف.
- اختبارات المال والترحيل والحدود ناجحة.
- Gradle وKSP وInstrumentation ناجحة داخل المشروع الكامل.

### الحالة الحالية

`BLOCKED`: اكتمل التحويل إلى `Money`/`BigDecimal`، ورُفعت Room إلى 12 مع Migration تحفظ البيانات. نجحت الاختبارات 8/8، والمراجعات المعمارية 6/6، والتجميع الساكن 16/16. تعذر Gradle لأن Wrapper يحتاج تنزيل التوزيع والبيئة بلا شبكة.

---

## المرحلة 07 — تنظيف حدود Domain وPresentation

### المشكلة المثبتة

- Domain Use Cases تستورد `PreferencesManager`.
- `DynamoState` داخل Domain يستورد `R.drawable`.
- `InvoiceDetailViewModel` يحقن `AutoDriveDatabase` مباشرة.

### الأعمال

- إبقاء Domain Kotlin خالصًا.
- نقل `imageRes()` و`arabicLabel()` إلى Presentation mapper أو Resources.
- إنشاء Query/Repository للعناصر التي يقرأها `InvoiceDetailViewModel`.
- منع ViewModel من حقن Database أو DAO أو Supabase أو WorkManager أو Firebase.
- إنشاء Ports للعلاقات العابرة بدل Concrete implementations.
- إضافة Architecture tests تمنع الاستيرادات المحظورة.

### شرط الإغلاق

Domain يعمل في Unit tests دون Android، وPresentation يعتمد على Contracts فقط.

### الحالة الحالية

`BLOCKED`: نُقلت موارد `DynamoState` إلى Presentation، واستُبدل وصول `InvoiceDetailViewModel` المباشر إلى Room بـ`GetInvoiceDetailsUseCase` و`InvoiceDetailRepository`. نجحت اختبارات السلوك 3/3، والمراجعات المعمارية 5/5، والتجميع الساكن 6/6. تعذر Gradle لأن Wrapper يحتاج تنزيل التوزيع والبيئة بلا شبكة.

---

## المرحلة 08 — تحديد ملكية الميزات

### الأعمال

إنشاء حدود داخل `app` أولًا:

```text
feature/auth
feature/profile
feature/home
feature/commission
feature/balance
feature/notifications
feature/chat
feature/competition
feature/reports
```

لكل ميزة:

```text
presentation/
domain/
data/
di/
```

### قواعد النقل

- Auth يملك الدخول والجلسة الأولية فقط.
- Profile يملك بيانات المستخدم وتعديلها.
- Commission يملك عرض الأهلية والعمولة.
- Balance يملك الرصيد والحركات والسحوبات.
- Notifications تملك التخزين والعرض وحالة القراءة.
- Chat تملك المحادثات والرسائل والوسائط.
- Home يستهلك Read Models ولا يملك قواعد بقية الميزات.

### شرط الإغلاق

لا تعتمد Feature على Concrete Repository أو DAO لميزة أخرى.

### الحالة الحالية

`BLOCKED`: نُقلت تسع ميزات إلى `feature/<name>/{presentation,domain,data,di}`، وفُصلت Bindings المركزية إلى وحدات DI مملوكة للميزات، وحُذف `RepositoryModule` المركزي. نجحت اختبارات السلوك 10/10، والمراجعات المعمارية 37/37، والتجميع الساكن 6/6، وفحص الربط الداخلي لـ205 ملفات دون أي استيراد داخلي مفقود. تعذر Gradle لأن Wrapper يحتاج تنزيل التوزيع والبيئة بلا شبكة.

---

## المرحلة 09 — تفكيك الملفات والكائنات الضخمة

### الأولويات

- `ChatScreen.kt`
- `HomeScreen.kt`
- `BalanceScreen.kt`
- `AppNavigation.kt`
- `ChatRepositoryImpl.kt`
- `SyncManager.kt`
- `SharedComponents.kt`

### الأعمال

- تقسيم الشاشة إلى Route وScreen ومكونات خاصة.
- نقل Side effects إلى ViewModel أو Infrastructure مناسبة.
- فصل رفع وتنزيل الوسائط عن `ChatRepositoryImpl`.
- إزالة `CoroutineScope(Dispatchers.IO + SupervisorJob())` المنفصل من تنزيل الصور.
- تقسيم Navigation إلى Graph لكل ميزة.
- عدم تحديد حد سطور آلي؛ المعيار هو مسؤولية واحدة وحدود قابلة للاختبار.

### شرط الإغلاق

كل ملف رئيسي له مسؤولية واضحة، ويمكن تعديل ميزة دون فتح ملفات مركزية متعددة غير مرتبطة.

### الحالة الحالية

`BLOCKED`: فُككت شاشات Chat وHome وBalance والمكونات المشتركة، وقُسم Navigation إلى Destinations وGraphs، وفُصلت وسائط Chat والحضور والـOutbox وتنظيف الحساب عن الكائنات المركزية. نجحت اختبارات السلوك 15/15، واختبارات الحدود 7/7، والمراجعات المعمارية 59/59، والتجميع الساكن 8/8، وتحليل الصياغة 29/29. تعذر Gradle لأن Wrapper يحتاج تنزيل التوزيع والبيئة بلا شبكة.

---

## المرحلة 10 — أداء Room والعلاقات

### الأعمال

- إضافة Indexes للاستعلامات الفعلية، خصوصًا:
  - `client_id`
  - `user_id`
  - `invoice_id`
  - `conversation_id`
  - `created_at`
  - `status`
- إضافة Foreign Keys حيث لا تتعارض مع Offline-first والحذف المرحلي.
- مراجعة حدود القوائم `LIMIT 20/50` وسياسة Pagination.
- استبدال استعلامات القوائم الضخمة بـPaging عند الحاجة الفعلية.
- إضافة Query plans واختبارات DAO.

### شرط الإغلاق

الاستعلامات الأساسية تستخدم فهارس، ولا تتدهور خطيًا بصورة غير مبررة مع نمو البيانات.

### الحالة الحالية

`BLOCKED`: رُفعت Room إلى 13 وأضيفت 20 فهرسًا مبنيًا على استعلامات DAO الفعلية، مع Migration 12→13 تحفظ البيانات. نجحت اختبارات SQLite 74/74، والمراجعات المعمارية 51/51، والتجميع الساكن 3/3. لم تُضف Foreign Keys لأن ترتيب Realtime المستقل قد يرسل السجل التابع قبل الأصل. تعذر Gradle وInstrumentation بسبب غياب التوزيع والشبكة.

---

## المرحلة 11 — Observability والأمان

### الأعمال

- إضافة Crash reporting وStructured logging في Release دون بيانات حساسة.
- تسجيل:
  - سبب المزامنة.
  - زمن كل Participant.
  - عدد العمليات المعلقة.
  - آخر خطأ وآخر نجاح.
  - حالات Realtime.
- مراجعة `READ_SMS` و`RECEIVE_SMS` وحذفهما إن كان SMS Retriever كافيًا.
- مراجعة سياسات RLS لكل جداول Realtime والقراءة والكتابة.
- نقل إعدادات البيئات إلى Build Types أو Properties مناسبة.
- منع تسجيل OTP أو Tokens أو محتوى مالي حساس.

### شرط الإغلاق

يمكن تشخيص فشل الإنتاج دون إعادة إنتاجه محليًا، ولا توجد صلاحية حساسة بلا ضرورة موثقة.

### الحالة الحالية

`BLOCKED`: أضيف Crashlytics وStructured logging منقّى، ومؤشرات سبب المزامنة وزمن المراحل وحالة Outbox وRealtime وآخر نجاح/خطأ. أزيلت صلاحيتا `READ_SMS` و`RECEIVE_SMS` واستُبدل استقبال PDU بـSMS Retriever. نُقلت إعدادات البيئة من `build.gradle.kts` إلى Properties/Environment، وأضيفت مراجعة RLS وأداة SQL. نجحت اختبارات السلوك 17/17، واختبارات SQLite الارتدادية 74/74، والمراجعات المعمارية 59/59، وفحوص الأمان 21/21، والتجميع الساكن 5/5. تعذر Gradle بسبب غياب الشبكة، ولم تُشغّل أداة RLS على Supabase الفعلي.

---

## المرحلة 12 — Package-by-Feature داخل app

### الأعمال

- نقل الميزات المستقرة تدريجيًا.
- إنشاء اختبارات حدود Packages.
- إبقاء `core` محدودًا في:
  - نماذج ثابتة مشتركة.
  - الوقت والـdispatchers والنتائج التقنية العامة.
  - Design system الحقيقي المشترك.
- منع مجلدات `utils` و`helpers` من استقبال كود جديد بلا مالك.

### شرط الإغلاق

هيكل الحزم يعكس ملكية الميزات، والاعتماديات واضحة حتى قبل فصل Gradle Modules.

### الحالة الحالية

`BLOCKED`: نُقلت الحزم القديمة إلى `core` و`feature` و`coordinator` و`navigation` و`di`، وحُذفت الجذور الطبقية القديمة. أصبحت الاستيرادات بين الميزات عبر Domain contracts فقط، ولا توجد Cycles. نجحت اختبارات السلوك 48/48، والمراجعات المعمارية 67/67، وفحوص Room والأمان 95/95، وفحوص الحزم 9/9، والتجميع الساكن 6/6. تعذر Gradle لأن Wrapper يحتاج تنزيل التوزيع والبيئة بلا شبكة.

---

## المرحلة 13 — استخراج Gradle Modules

### الترتيب المقترح

1. `core:model`
2. `core:common`
3. `core:designsystem`
4. `core:session`
5. `core:database`
6. `core:network`
7. `core:sync`
8. `feature:auth`
9. `feature:notifications`
10. `feature:profile`
11. `feature:commission`
12. `feature:balance`
13. `feature:chat`
14. بقية الميزات حسب الاستقرار

### شرط الإغلاق

- Dependency Graph أحادي الاتجاه.
- لا توجد Cycles.
- لا تعتمد Feature على implementation لميزة أخرى.
- أزمنة البناء لا تتدهور دون فائدة.

### الحالة الحالية

`BLOCKED`: استُخرجت 15 وحدة Gradle: تسع وحدات Core وست وحدات Feature. أصبح `:app` نقطة التجميع والتنقل، ولا توجد Cycles أو اعتماد من Core على Feature أو من Library على App. نجحت اختبارات السلوك 48/48، والمراجعات المعمارية 74/74، وفحوص الوحدات 60/60، وفحوص الحزم 24/24، وفحوص Room والأمان 95/95، والتجميع الساكن 4/4. تعذر Gradle لأن Wrapper يحتاج تنزيل التوزيع والبيئة بلا شبكة؛ لذلك لم يُتحقق بعد من KSP وAndroid resource linking وأزمنة البناء.

---

## المرحلة 14 — الإغلاق والتنظيف

### الأعمال

- حذف Adapters والواجهات الانتقالية غير المستخدمة.
- حذف المراجع القديمة وDead code.
- تحديث مخطط المعمارية الفعلية والمستهدفة.
- تشغيل Unit وDAO وMigration وArchitecture وRobolectric tests.
- تشغيل:
  - `./gradlew clean`
  - `./gradlew testDebugUnitTest`
  - `./gradlew lintDebug`
  - `./gradlew assembleDebug`
- بناء Release والتحقق على جهاز فعلي.
- اختبار ترقية التطبيق فوق نسخة إنتاج سابقة دون حذف البيانات.

### شرط النجاح النهائي

- لا يوجد فقد بيانات أثناء الترقية.
- لا توجد مزامنة متداخلة أو عمليات مالية مكررة.
- الأموال لا تستخدم `Double` في المنطق والتخزين.
- Domain وPresentation لا يعتمدان على التفاصيل التقنية.
- جميع الاختبارات والبناء ناجحة.
- يمكن إضافة ميزة جديدة دون تعديل God Objects مركزية.

### الحالة الحالية

`BLOCKED`: اكتمل حذف الـBridge وواجهات التوافق غير المستخدمة وكود Presentation الميت، ونُقلت عقود التسجيل والخروج العابرة إلى `core:common`، وأزيل اعتماد Profile على Auth، وثُبّت Gradle Wrapper على 8.7 المستقر. نجحت اختبارات السلوك 48/48 والمراجعات المعمارية 81/81 وفحوص الوحدات 60/60 والحزم 24/24 وRoom 74/74 والأمان 21/21 والتنظيف 15/15 والتجميع الساكن 6/6. تعذر Gradle وRelease والجهاز لأن البيئة بلا شبكة ولا تحتوي توزيع Gradle.

---

## بروتوكول تسليم كل مرحلة

كل إصدار يتضمن:

1. `app-full-vNN.zip`: نسخة Module كاملة بعد التعديل.
2. `patch-vNN.zip`: الملفات الجديدة والمعدلة فقط.
3. `PATCH_MANIFEST.md`: قائمة دقيقة بالملفات والتغييرات.
4. `verification-vNN.md`: ما تم وما لم يتم ونتائج الأوامر.
5. تحديث `refactor-plan.md` وحالة المرحلة.
6. تحديث `refactor-changelog.md`.
7. نتيجة البناء والاختبارات بوضوح، دون ادعاء نجاح اختبار لم يُشغّل.

## متى يجب إرسال نسخة محدثة؟

- عند تعديل أي ملف عبر Android Studio أو Codex أو دمج Patch آخر.
- عند اختلاف نسخة Room أو Supabase contracts أو Navigation.
- عند التعارض، أحدث نسخة كاملة يرسلها المستخدم هي مصدر الحقيقة الوحيد.
