# قواعد الاعتماديات — AutoDrive

## الاتجاه المسموح

```text
Presentation -> Feature Domain Contract
Data Adapter -> Feature Domain Contract
Coordinator -> Multiple Domain/Infrastructure Ports
Realtime Participant -> Owned Data Contracts
Infrastructure -> Android / Room / Supabase / Firebase / WorkManager
App -> Composition Root + Navigation فقط
```

## الممنوع

- `ViewModel -> DAO`
- `ViewModel -> AutoDriveDatabase`
- `ViewModel -> SyncManager`
- `ViewModel -> PreferencesManager`
- `ViewModel -> Supabase/Firebase/WorkManager`
- `Domain -> Android`
- `Domain -> R.drawable/R.string`
- `Domain -> PreferencesManager`
- `Domain -> Data DTO/Entity`
- `Feature A -> Feature B concrete repository`
- `Feature A -> Feature B DAO`
- حقن `AutoDriveDatabase` داخل Use Case جديد
- استدعاء `fullSync()` من UI
- إنشاء CoroutineScope منفصل بلا مالك Lifecycle واضح
- إضافة منطق جديد إلى `utils` بلا مالك واضح
- استخدام `Double` في نموذج مالي جديد
- استخدام `runCatching {}` لإخفاء فشل المزامنة دون تسجيل أو نتيجة

## قاعدة Room

- تبقى قاعدة واحدة أثناء الانتقال.
- DAO يستهلكه Data implementation التابع لمالكه فقط.
- يمنع `fallbackToDestructiveMigration()`.
- يجب تفعيل Schema export.
- كل زيادة Version تتطلب Migration واختبارًا.
- لا يُحذف جدول أو عمود مالي دون خطة ترحيل واسترجاع.

## قاعدة الجلسة

```text
Presentation/Domain -> SessionReader
Authentication Coordinator -> SessionWriter
Session Adapter -> Preferences/DataStore/Auth SDK
```

- `userId` و`clientId` و`orgId` لها مصدر واحد.
- لا تقرأ الميزات `PreferencesManager` مباشرة.
- تبديل المستخدم يجب ألا يعرض بيانات المستخدم السابق.

## عزل بيانات الحساب

- كل Query لإشعار خاص يجب أن يتضمن `user_id`.
- بيانات الفواتير والمدفوعات تُعزل عبر `client_id` وعلاقة الفاتورة.
- بيانات الدردشة تُعزل عبر `marketer_id` وعلاقة المحادثة.
- عند الخروج تُمسح كل Cache خاصة بالحساب قبل مسح الجلسة.
- إلى أن تصبح Pending Operations مملوكة صراحةً، تُمسح عند الخروج لمنع إعادة تشغيلها تحت حساب آخر.
- لا تُحفظ قيم لوحة المستخدم السابق بعد `clearSession()`.

## قاعدة المزامنة

```text
UI -> SyncRequestPort -> SyncCoordinator -> SyncParticipants
SyncParticipant -> Owned Repository/DAO/Remote API
```

- مزامنة كاملة واحدة فقط في الوقت نفسه.
- كل طلب مزامنة يحمل سببًا.
- كل Participant يعيد نتيجة قابلة للتسجيل.
- فشل Participant لا يُخفى ولا يفسد بقية المشاركين المستقلين.
- Realtime يحدّث Room فقط.

## قاعدة Realtime

- كل Feature تملك Participant الخاص بها.
- يجب استخدام فلتر `user_id` أو `client_id` على السيرفر عند توفره.
- يجب تحديد سياسة Insert وUpdate وDelete.
- اسم القناة ليس بديلًا عن RLS أو الفلترة.
- Coordinator يدير lifecycle وإعادة الاتصال فقط.

## قاعدة Outbox

- كل عملية قابلة للتكرار تستخدم `idempotency_key`.
- لا تُحذف العملية قبل تأكيد النجاح البعيد.
- يجب وجود حد للمحاولات وموعد إعادة المحاولة وسبب آخر فشل.
- العملية الفاشلة دائمًا تنتقل إلى حالة نهائية قابلة للتشخيص.
- العمليات المالية لا تُعاد عشوائيًا بعد Timeout غير محسوم.

## قاعدة الأموال

- يمنع الحساب المالي باستخدام `Double`.
- يستخدم نموذج `Money` موحد بدقة وعملة واضحتين.
- التحويل إلى نوع رسومي تقريبي يكون في Presentation فقط.
- التقريب يتم في نقطة موثقة واحدة.
- Migration المالية تُنفذ منفصلة مع اختبار تطابق كامل.

## حدود Notifications

```text
NotificationsScreen
  -> NotificationCenterViewModel
  -> NotificationCenterGateway
  -> NotificationRepositoryAdapter
  -> NotificationDao + Supabase
```

- كل Query وCount وUpdate مقيد بـ`user_id`.
- FCM وRealtime لا ينشئان نسختين من الإشعار نفسه.
- حالة القراءة المحلية لا تُستبدل بنتيجة أقدم من السيرفر.

## حدود Balance وWithdrawals

```text
BalanceScreen
  -> BalanceViewModel
  -> BalanceGateway
  -> BalanceRepositoryAdapter
  -> Room/Supabase/Outbox
```

- UI لا يطلب `fullSync()` مباشرة.
- طلب السحب يستخدم Idempotency.
- الرصيد لا يعدل محليًا كحقيقة نهائية قبل تأكيد العقد البعيد.

## حدود Commission وInvoices

```text
InvoiceDetailViewModel
  -> GetInvoiceDetailsUseCase
  -> InvoiceDetailRepository
  -> InvoiceDetailRepositoryImpl
  -> Room + Commission Domain Contract

Commission Presentation
  -> Commission Domain Contract
  -> Commission Data Adapter
  -> Owned DAOs/Remote APIs
```

- `InvoiceDetailViewModel` لا يحقن Database.
- قواعد أهلية العمولة مصدرها Contract البعيد المعتمد.
- تفاصيل DTO وEntity لا تعبر إلى Presentation.

## حدود Chat

```text
ChatScreen
  -> ChatViewModel
  -> ChatGateway
  -> Message/Conversation/Media Adapters
```

- رفع وتنزيل الوسائط ليس مسؤولية Composable.
- لا يوجد Scope منفصل داخل helper.
- الرسالة المحلية تبقى `PENDING` حتى تأكيد الإرسال.
- إعادة المحاولة محدودة ومسجلة.

## حدود Domain وResources

- Domain يحتوي قواعد وحالات محايدة فقط.
- أسماء العرض والصور والألوان والموارد في Presentation.
- `DynamoState` يبقى Domain، بينما `imageRes` و`arabicLabel` ينتقلان إلى UI mapper/resources.

## قاعدة العلاقات العابرة

تُنفذ عبر Port أو Coordinator، وليس باستدعاء Concrete Repository من Feature أخرى.

## قاعدة Core

لا يُنقل شيء إلى Core لمجرد أنه قابل لإعادة الاستخدام نظريًا. يجب وجود مستهلكين فعليين ومعنى ثابت.

## قاعدة Presentation

النمط التدريجي للميزات المعدلة:

```text
Route
Screen
ViewModel
UiState
UiEvent عند الحاجة
UiEffect للأحداث أحادية الاستهلاك
```

- Composable يعرض الحالة ويرسل Events.
- التنزيل والمشاركة والمزامنة والكتابة ليست منطقًا داخل Screen.
- Navigation يقسم إلى Graphs حسب الميزة.

## قاعدة الاختبارات

كل حد جديد يحتاج:

- Unit test للسلوك.
- Architecture test للاستيرادات.
- DAO test للاستعلامات المهمة.
- Migration test لأي Schema change.
- Integration test للمزامنة وOutbox حسب الإمكان.

## حدود Outbox

```text
Repository write failure
  -> PendingOperationEntity
  -> PendingOperationProcessor
  -> Remote API/RPC
```

- يمنع حذف العملية قبل تأكيد النجاح البعيد.
- يمنع استخدام حلقة إعادة غير محدودة أو `retry_count++` بلا موعد وحالة نهائية.
- كل عملية لها `payload_version` و`idempotency_key`.
- طلب السحب يستخدم نفس `client_request_id` محليًا وفي RPC.
- الأخطاء الدائمة تنتقل إلى `DEAD_LETTER` ولا يعاد تشغيلها تلقائيًا.
- المطالبة المنقطعة تُحرر بعد انتهاء Lease.
- `OutboxSynchronizer` يملك تفاصيل الإرسال فقط؛ سياسة الحالة والمحاولات مملوكة للـProcessor.

## حدود Realtime v05

```text
SyncCoordinator -> RealtimeController -> RealtimeManager -> Set<RealtimeParticipant>
RealtimeParticipant -> Supabase Realtime + Owned DAOs
```

- `SyncManager` ممنوع من امتلاك `postgresChangeFlow` أو `PostgresAction`.
- `RealtimeManager` لا يعرف Room أو DTOs أو أسماء الجداول.
- كل Participant يملك جداول مجال واحد فقط.
- الفلتر البعيد إلزامي عند وجود `client_id` أو `user_id`.
- جدول `payments` لا يملك مفتاح مالك في العقد الحالي؛ يعتمد على RLS ثم يتحقق محليًا عبر علاقة `invoice_id`.
- Delete لا ينفذ قبل التحقق من ملكية السجل محليًا عند توفرها.
- فشل Participant يعيد تشغيل مجموعة القنوات كلها بBackoff محدود.
- تسجيل الخروج يوقف القنوات قبل تنظيف بيانات الحساب.

## حدود الأموال v06

```text
Remote numeric/string -> BigDecimal DTO -> BigDecimal Entity -> Money Domain -> Presentation
```

- `Money` داخل Domain وUiState.
- `BigDecimal` داخل DTO وRoom Entity.
- يمنع `Double` و`Float` في القيم المالية.
- `toDisplayDouble()` مسموح فقط للرسم والتحريك.
- Preferences المالية الجديدة تخزن كنص عشري؛ قراءة الصيغة القديمة للتوافق فقط.
- خلط العملات مرفوض.
- التقريب صريح عبر `Money.rounded()`.
- أي حقل مالي جديد يحتاج Serializer وConverter واختبار دقة.
- أي تغيير مالي في Room يحتاج Migration مستقلة واختبار حفظ.

## حدود ملكية الميزات v08

```text
feature/<name>/
├── presentation/
├── domain/
├── data/
└── di/
```

الميزات المالكة الحالية:

- `auth`
- `profile`
- `home`
- `commission`
- `balance`
- `notifications`
- `chat`
- `competition`
- `reports`

القواعد:

- كل ميزة تملك شاشاتها وViewModels وعقود Domain وتنفيذ Data وBindings الخاصة بها.
- يجوز لميزة استيراد `domain` من ميزة أخرى عند وجود Contract أو Model مشترك ضروري.
- يمنع استيراد `data` أو `di` أو Concrete Repository أو DAO من ميزة أخرى.
- `Home` يستهلك عقود وRead Models ولا يملك قواعد العمولة أو الرصيد أو الإشعارات.
- `Chat` يملك نماذج `Conversation` و`ChatMessage` وحالات الرسالة وDTOs والـRealtime participant الخاص به.
- `RepositoryModule` المركزي ممنوع؛ كل Binding يوجد داخل DI المملوك للميزة.
- البنية العامة `data/di/domain/ui` لا تستقبل كود ميزة جديدًا بعد v08؛ النقل المتبقي يتم تدريجيًا عند تعديل المالك.

## حدود المسؤوليات v09

```text
Route -> Screen components -> Presentation callbacks
ChatRepository -> ChatMediaManager -> Android/Storage/HTTP
SyncManager -> PresenceReporter + OutboxSynchronizer + LocalDataCleaner
AppNavigation -> Feature Graph builders
```

- ملف Route لا يملك تسجيل الصوت أو حفظ الصور أو تفاصيل MediaStore.
- يمنع إنشاء `CoroutineScope(Dispatchers.IO + SupervisorJob())` داخل Composable.
- `ChatRepositoryImpl` لا يملك `Context` أو `HttpClient` أو Supabase Storage مباشرة.
- `SyncManager` ينسق المزامنة فقط؛ الحضور والـOutbox والتنظيف مسؤوليات مستقلة.
- `AppNavigation` لا يسجل Destinations مباشرة؛ التسجيل داخل Graphs المملوكة.
- التوجيه الناتج عن نوع الإشعار يمر عبر Resolver قابل للاختبار.
- `SharedComponents` للمكونات العامة الصغيرة فقط؛ Bottom Navigation والبطاقات المركبة في ملفات مالكة.
- لا يُفرض حد سطور عام؛ الحدود الرقمية في الاختبار تحمي نقاط الدخول الحالية من العودة إلى God Files.

## حدود Room والأداء v10

```text
DAO query -> matching Entity index -> MIGRATION_12_13 -> EXPLAIN QUERY PLAN
```

- كل Index جديد يجب أن يطابق استعلامًا فعليًا، لا اسم عمود شائعًا فقط.
- إعلان `@Entity(indices=...)` وMigration يجب أن يتطابقا بالاسم وترتيب الأعمدة.
- أي رفع لنسخة Room يحتاج Migration واختبار حفظ بيانات.
- القوائم ذات نافذة منتج محددة تبقى محدودة: 20 أو 50 أو Batch parameter.
- لا تُفرض Foreign Keys بين Aggregates تصل عبر Realtime مستقل دون ضمان ترتيب إدخال.
- الاستعلامات الحرجة تُراجع بواسطة `EXPLAIN QUERY PLAN`.
- يمنع حذف فهرس أو تغيير ترتيبه دون اختبار خطة الاستعلام المقابلة.

## حدود Observability والأمان v11

```text
Production code -> AppLogger -> SensitiveDataRedactor -> DiagnosticsReporter -> Crashlytics
Sync/Realtime/Outbox -> SyncDiagnostics -> AppLogger
```

- يمنع استدعاء `android.util.Log` خارج `AppLogger` وأداة SMS hash الخاصة بـDebug.
- لا يرسل Reporter OTP أو Token أو هاتفًا أو حسابًا بنكيًا أو مبلغًا أو رصيدًا أو عمولة أو محتوى رسالة.
- لا يرسل معرف المستخدم أو العميل إلى Crashlytics.
- Throwable يُحوّل إلى نوع الخطأ وStack trace مع حذف الرسالة الأصلية.
- مؤشرات المزامنة تشغيلية فقط: السبب، المرحلة، المدة، الحالة، وعدد الفشل.
- Outbox يسجل أعداد الحالات ولا يسجل Payload أو idempotency key أو معرف العملية.
- صلاحيات SMS العامة ممنوعة؛ OTP يستخدم SMS Retriever دون `READ_SMS` أو `RECEIVE_SMS`.
- إعدادات البيئة تأتي من Gradle Properties أو Environment أو `local.properties` غير المتعقب.
- `service_role` ممنوع داخل تطبيق Android دائمًا؛ `anon key` يخضع لـRLS.
- فلاتر العميل لا تعتبر حماية؛ كل جدول مكشوف يحتاج RLS واختبارًا عبر `verify_rls_v11.sql`.



## حدود Package-by-Feature v12

```text
com.autodrive.app/
├── core/          # تقنيات ونماذج مشتركة مستقرة
├── feature/       # ملكية الميزات: presentation/domain/data/di
├── coordinator/   # عمليات عابرة لأكثر من ميزة
├── navigation/    # الوجهات وتجميع Feature graphs
├── di/            # Composition root فقط
├── AutoDriveApp.kt
└── MainActivity.kt
```

- تُمنع الجذور القديمة: `data` و`domain` و`ui` و`utils` و`notifications` و`observability`.
- `core` لا يستورد `feature` أو `coordinator` أو `navigation` أو `di`.
- الاعتماد بين ميزتين مسموح فقط عبر `feature/<name>/domain`.
- يُمنع استيراد `data` أو `di` أو `presentation` لميزة أخرى.
- العمليات العابرة للميزات تُنفذ داخل `coordinator` عبر Contracts.
- `navigation` يجمع Graphs ولا يملك قواعد عمل أو وصولًا مباشرًا للبنية التحتية.
- `di` يربط التنفيذ بالعقد ولا يحتوي منطق عمل.
- اسم Package يجب أن يطابق المسار الفيزيائي في main وtest وandroidTest.
- لا تُنقل عناصر إلى `core` دون معنى ثابت ومستهلكين فعليين.
- أي اعتماد دائري بين Features يمنع قبول التعديل.

## حدود Gradle Modules v13

```text
:app -> core:* + feature:*
feature:* -> core:* + feature:<other>:domain contracts only
core:* -> core:* only
```

- يمنع أي اعتماد من `core:*` إلى `feature:*` أو `:app`.
- يمنع أي اعتماد من Library إلى `:app`.
- كل استيراد بين ميزتين يجب أن يقع داخل `feature/<target>/domain`.
- كل Module يعلن الاعتماديات التي يستوردها مباشرة؛ لا يعتمد على Transitive exposure عرضيًا.
- `:app` يملك Composition Root وNavigation والميزات غير المستقرة فقط.
- `:core:designsystem` هو مالك الموارد المؤقت في V13؛ توزيع الموارد على الميزات يُنفذ لاحقًا دون إعادة اعتماد على `:app`.
- `:core:model` و`:core:common` وحدتا Kotlin/JVM خالصتان بلا Android.
- إضافة Module جديد تتطلب:
  - تسجيله في `settings.gradle.kts`.
  - Build script مستقلًا.
  - Source root صحيحًا.
  - عدم إنشاء Cycle.
  - تحديث `verify_modules_v13.py` واختبار المعمارية.

## قواعد الإغلاق v14

```text
Auth registration UI -> core:common RegistrationProfileWriter -> Profile adapter
Profile UI -> core:common SignOutAction -> Auth implementation
```

- يمنع إعادة إنشاء Bridge داخل `:app` لربط ميزتين.
- العقد العابر يوضع في `core:common` فقط عندما يوجد مستهلك ومنفذ فعليان ومعنى ثابت.
- `:feature:profile` لا يعتمد على `:feature:auth`.
- يمنع إبقاء API توافقية غير مستخدمة أو `@Deprecated` بلا خطة إزالة.
- كود Presentation غير المتصل بمسار واجهة فعلي يُحذف بدل إبقائه احتياطيًا.
- Gradle Wrapper يستخدم إصدارًا مستقرًا متوافقًا مع AGP؛ إصدارات milestone ممنوعة في فرع الإنتاج.
- حدود الأسبوع المحلية في Commission للعرض الاحتياطي فقط، ولا تقرر أهلية أو قيمة مالية.
- ملفات `build/` و`.gradle/` ومخرجات التحقق لا تدخل النسخة الكاملة أو Patch.

