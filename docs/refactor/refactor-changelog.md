# سجل إعادة الهيكلة

## v00 — تحليل المشروع وخطة الإصلاح

### أضيف

- `refactor-plan.md`
- `dependency-rules.md`
- `verification-v00.md`
- `verification-template.md`
- خطة انتقال من Layered Monolith إلى Feature-first Modular Monolith.
- ترتيب إصلاح يبدأ بحماية البيانات ثم الجلسة والمزامنة والمال.
- شروط إغلاق واختبارات مطلوبة لكل مرحلة.

### ثُبّت بالتحليل

- المشروع Module واحد باسم `app`.
- Room version الحالية: `10`.
- `fallbackToDestructiveMigration()` مفعّل.
- `exportSchema = false`.
- 144 ملف Kotlin Production بحوالي 17,038 سطرًا.
- 5 ملفات Unit Test فقط، ولا توجد Android/Migration tests في الملف المستلم.
- `SyncManager` يجمع المزامنة الكاملة وRealtime وOutbox والحضور والتحويلات.
- `fullSync()` يُستدعى من عدة ViewModels وFCM وبداية التطبيق.
- ViewModels وDomain Use Cases تقرأ `PreferencesManager` مباشرة.
- `InvoiceDetailViewModel` يعتمد على `AutoDriveDatabase` مباشرة.
- Notifications المحلية غير مقيدة بالكامل بـ`userId`.
- نماذج مالية كثيرة تستخدم `Double`.
- معظم Realtime subscriptions غير مفلترة على السيرفر ولا تعالج Delete.
- ملفات مركزية كبيرة في Chat وHome وBalance وNavigation وSync.

### لم يتغير

- أي ملف Kotlin أو XML أو Gradle داخل التطبيق.
- سلوك الدخول والتسجيل والمزامنة والدردشة والرصيد.
- Supabase schema وRLS وRPCs.
- Room schema/version 10.
- تصميم الشاشات ومسارات Navigation.

### التحقق

- فحص ساكن لبنية المشروع والاستيرادات والاستدعاءات: ناجح.
- Gradle build/tests: لم تُشغّل لأن الملف المستلم لا يحتوي جذر المشروع وGradle Wrapper وVersion Catalog.
- لا توجد ادعاءات بنجاح Runtime أو Migration أو Release build.

## v01 — حماية Room والبيانات المحلية

### أضيف

- `AUTODRIVE_DATABASE_VERSION` كمصدر واحد لإصدار Room.
- `ALL_MIGRATIONS` كمسار رسمي متصل من النسخة 4 إلى 10.
- إعداد KSP لتصدير Schemas إلى `app/schemas`.
- `DatabaseMigrationTest` لاختبار الترقية 4→10 وحفظ العمليات المعلقة والرسائل غير المرسلة.
- اختبار يثبت أن نسخة غير مدعومة تفشل دون حذف قاعدة المستخدم.
- `DatabaseSafetyArchitectureTest` لمنع عودة fallback التدميري أو تعطيل تصدير Schema.
- توثيق سياسة إصدارات Room والترقية.

### عُدل

- `AutoDriveDatabase`: تفعيل `exportSchema = true`.
- `AppModule`: التسجيل عبر `ALL_MIGRATIONS` فقط.
- إعدادات الاختبار لإضافة AndroidJUnitRunner واعتماديات Instrumentation.

### حُذف

- `fallbackToDestructiveMigration()`.

### لم يتغير

- Room version بقيت `10`.
- أسماء الجداول والأعمدة الحالية.
- قواعد العمل والمزامنة والواجهات.
- بيانات Supabase وRLS وRPCs.

### التحقق

- فحص عدم وجود fallback تدميري داخل Production: ناجح.
- تجميع Kotlin للملفات المعدلة واختباراتها بعقود Stub: ناجح.
- محاكاة SQLite لمسار 4→10 وحفظ الصفوف الحساسة: ناجحة.
- Gradle/KSP/Instrumentation: متوقفة لأن توزيع Gradle غير موجود محليًا والبيئة بلا إنترنت.

## v02 — الجلسة وعزل المستخدم

### أضيف

- `CurrentSession` و`RegistrationState`.
- `SessionReader` و`SessionWriter`.
- `DashboardPreferences` لفصل إعدادات العرض عن هوية الجلسة.
- `SessionModule` لربط التنفيذ الحالي بعقود Domain.
- `SessionIsolationArchitectureTest`.
- `CurrentSessionTest`.
- `SessionSwitchIsolationTest` على Android للتحقق من تبديل الحساب.

### عُدّل

- ViewModels وUse Cases تعتمد على `SessionReader` بدل `PreferencesManager`.
- مستودعات Auth/Profile/Balance/Chat وSync وFCM تعتمد على عقود الجلسة.
- `NotificationDao` يقيد القراءة والعدد والتحديث والحذف بـ`user_id`.
- `NotificationRepositoryImpl` يمرر `userId` إلى Room وSupabase في عمليات القراءة.
- تسجيل الخروج يمسح بيانات المستخدم والفواتير والمدفوعات والمحادثات والإشعارات والترتيب والعمليات المعلقة.
- `clearSession()` يمسح الهوية وحالة التسجيل وحالة لوحة المستخدم السابق.

### لم يتغير

- Room schema/version 10.
- Supabase schema وRLS وRPCs.
- تصميم الشاشات ومسارات Navigation.
- قواعد الرصيد والعمولة والدردشة.

### التحقق

- التجميع الساكن لعقود الجلسة وUse Cases وViewModels وSession adapter وNotification repository وDAOs واختبارات الحدود: ناجح.
- فحص عدم استيراد `PreferencesManager` من Domain وPresentation: ناجح.
- Gradle وInstrumentation: لم يعملا بسبب غياب توزيع Gradle محليًا وانقطاع الشبكة.

## v03 — تنسيق المزامنة ومنع التداخل

### أضيف

- `SyncCoordinator` كعقد المزامنة الوحيد للواجهة والتطبيق وFCM.
- أسباب المزامنة: `APP_START` و`NETWORK_RESTORED` و`USER_REFRESH` و`FCM_HINT` و`LOGIN_SUCCESS`.
- `SyncState` و`SyncResult` و`SyncFailure` ومرحلة المزامنة الحالية.
- Single-flight يجعل الطلبات المتزامنة تنتظر العملية نفسها.
- `SyncStepExecutor` لعزل فشل كل قسم واستمرار الأقسام المستقلة.
- `RealtimeConnectionObserver` لفصل ViewModel حالة الاتصال عن `SyncManager`.
- اختبارات سلوك وحدود اعتماديات للمزامنة.

### عُدل

- `SyncManager` أصبح محرك تنفيذ خلف `SyncCoordinator`.
- المزامنة الكاملة قُسمت إلى Profile وInvoices وPayments وCommissions وBalance وTransactions وWithdrawals وNotifications وChat.
- `AutoDriveApp` وFCM وHome/Profile/Balance/Register تستخدم `SyncCoordinator`.
- `NetworkMonitor` ينفذ عقد `SyncConnectivity`.
- `RealtimeStatusViewModel` يعتمد على عقد Domain.

### حُذف

- استدعاءات `fullSync()` العامة.
- اعتماد ViewModels على `SyncManager`.
- `SyncManager.onLoginSuccess()`.

### لم يتغير

- Room schema/version 10.
- Supabase schema وRLS وRPCs.
- قواعد الرصيد والعمولة والسحب والدردشة.
- تصميم الشاشات ومسارات Navigation.

### التحقق

- اختبارات سلوك مستقلة: 3 ناجحة.
- التجميع الساكن لجميع مجموعات v03 المعدلة: ناجح.
- فحص الحدود وعدم وجود `fullSync()` أو ViewModel يعتمد على `SyncManager`: ناجح.
- Gradle لم يبدأ بسبب تعذر تنزيل التوزيع في البيئة غير المتصلة.

## v04 — Outbox والعمليات المعلقة

### أضيف

- `OutboxRetryPolicy` بحد أقصى خمس محاولات وBackoff أسي مع Jitter.
- `PendingOperationProcessor` للمطالبة، الإرسال، النجاح، إعادة المحاولة و`DEAD_LETTER`.
- `status` و`attempt_count` و`next_retry_at` وأسباب الفشل وإصدار Payload ومفتاح Idempotency.
- Migration من Room 10 إلى 11 تحفظ العمليات القديمة وعداد محاولاتها.
- فهارس للحالة وموعد الإعادة ومفتاح Idempotency الفريد.
- اختبارات سلوك وMigration وحدود اعتماديات للـOutbox.

### عُدل

- `SyncManager` يرسل العمليات عبر المعالج بدل حلقة غير محدودة.
- تحديث الملف الشخصي يستخدم مفتاحًا ثابتًا لمنع تراكم نسخ قديمة.
- طلب السحب يستخدم `client_request_id` كمفتاح Idempotency محلي وبعيد.
- العمليات الناجحة لا تُحذف إلا بعد تأكيد الإرسال البعيد.
- العمليات غير المعروفة أو Payload غير المدعوم تنتقل مباشرة إلى `DEAD_LETTER`.

### لم يتغير

- Supabase schema وRLS وRPC الحالي.
- قواعد الرصيد والعمولة والسحب.
- تصميم الشاشات ومسارات Navigation.

### التحقق

- اختبارات سلوك مستقلة: 7 ناجحة.
- محاكاة Migration SQLite: 3 ناجحة.
- اختبارات حدود ساكنة: 16 ناجحة.
- التجميع الساكن لجميع ملفات v04 المعدلة واختباراتها: ناجح.
- Gradle/KSP لم يبدأ بسبب غياب توزيع Gradle محليًا وانقطاع الشبكة.

## v05 — Realtime Participants

### أضيف

- `RealtimeManager` لإدارة دورة الاتصال وإعادة الاتصال فقط.
- `RealtimeParticipant` وعقد جلسة Realtime.
- `BillingRealtimeParticipant`.
- `BalanceRealtimeParticipant`.
- `ChatRealtimeParticipant`.
- `NotificationsRealtimeParticipant`.
- `RealtimeController` لعزل المنسق وتسجيل الخروج عن التنفيذ.
- `RealtimeModule` باستخدام Hilt multibinding.
- اختبارات سلوك وملكية وحدود اعتماديات لـRealtime.

### عُدل

- `SyncManager` لم يعد يملك قنوات أو أحداث Realtime.
- `DefaultSyncCoordinator` يشغّل `RealtimeController` بدل `SyncEngine`.
- اشتراكات الجداول المالكة تستخدم فلاتر `client_id` أو `user_id`.
- كل Participant يعالج Insert وUpdate وDelete ويكتب إلى Room فقط.
- حذف الفاتورة يحذف مدفوعاتها المحلية التابعة.
- تحديث الفاتورة غير المؤهلة يزيلها محليًا بدل إبقاء سجل وهمي.
- إشعارات القراءة المحلية غير المزامنة لا تُستبدل بحالة أقدم من السيرفر.
- تسجيل الخروج يوقف Realtime قبل تنظيف البيانات والجلسة.
- أضيفت استعلامات DAO محددة للحذف والتحقق من الملكية.
- DTO-to-Entity mappers نُقلت إلى `SyncEntityMappers.kt`.

### لم يتغير

- Room schema/version 11.
- Supabase schema وRLS وRPCs.
- قواعد الرصيد والعمولة والسحب والدردشة.
- تصميم الشاشات ومسارات Navigation.

### التحقق

- اختبارات سلوك Realtime وإعادة الاتصال: ناجحة.
- اختبارات عزل المستخدم والحذف وحدود الاعتماديات: ناجحة.
- التجميع الساكن لملفات Realtime وSync وDAOs وAuth والاختبارات: ناجح.
- Gradle لم يبدأ بسبب تعذر تنزيل التوزيع في البيئة غير المتصلة.

## v06 — صحة الأموال

### أضيف

- `Money` مبني على `BigDecimal`.
- `BigDecimalConverters` لـRoom.
- `BigDecimalSerializer` للـDTOs وRPCs.
- Migration من Room 11 إلى 12 من `REAL` إلى `TEXT`.
- اختبارات المال والترحيل والحدود.

### عُدل

- Domain المالي يستخدم `Money`.
- Entities وDTOs المالية تستخدم `BigDecimal`.
- الرصيد والسحب والعمولة والتقارير والترتيب والطباعة وFCM تستخدم الحساب الدقيق.
- Preferences تخزن القيم المالية كنص عشري وتقرأ الصيغة القديمة للتوافق فقط.
- Room version أصبحت 12.

### لم يتغير

- Supabase schema وRLS وRPCs الحالية.
- قواعد استحقاق العمولة والسحب.
- تصميم الشاشات ومسارات Navigation.

### التحقق

- الاختبارات: 8/8 ناجحة.
- المراجعات المعمارية: 6/6 ناجحة.
- التجميع الساكن: 16/16 ناجح.
- Gradle/KSP/Instrumentation تعذر بسبب غياب الشبكة والتوزيع المحلي.

## v07 — تنظيف حدود Domain وPresentation

### أضيف

- `InvoiceDetailRepository` و`InvoiceDetails` كعقد قراءة للواجهة.
- `InvoiceDetailRepositoryImpl` لإخفاء Room خلف Data Adapter.
- `GetInvoiceDetailsUseCase`.
- `DynamoStateUiMapper` لملكية أسماء العرض والصور داخل Presentation.
- اختبارات سلوك وحدود اعتماديات للمرحلة.

### عُدل

- `InvoiceDetailViewModel` يعتمد على Use Case فقط بدل `AutoDriveDatabase`.
- `DynamoState` أصبح Kotlin خالصًا بلا `R.drawable` أو نصوص عرض.
- `RepositoryModule` يوفر Binding لعقد تفاصيل الفاتورة.

### لم يتغير

- Room schema/version 12.
- Supabase schema وRLS وRPCs.
- قواعد الفاتورة والعمولة وحالاتها.
- تصميم شاشة تفاصيل الفاتورة ومسارات Navigation.

### التحقق

- اختبارات السلوك: 3/3 ناجحة.
- المراجعات المعمارية: 5/5 ناجحة.
- التجميع الساكن: 6/6 ناجح.
- Gradle تعذر بسبب غياب الشبكة وتوزيع Gradle المحلي.

## v08 — تحديد ملكية الميزات

### أضيف

- هيكل `feature/<name>/{presentation,domain,data,di}` لتسع ميزات.
- وحدات Hilt مستقلة لكل ميزة.
- `FeatureOwnershipArchitectureTest` لحماية الملكية واتجاه الاعتماديات.
- مكوّنات عرض العمولة داخل ميزة Commission بدل امتلاك Home لها.

### عُدل

- نُقلت شاشات وViewModels وعقود Domain وUse Cases وRepository implementations إلى مالكيها.
- نُقلت DTOs وRealtime participants الخاصة بـChat وCompetition وBalance وNotifications إلى الميزات المالكة.
- حُدثت Navigation وApp وSync وFCM والاستيرادات لتستخدم الحزم الجديدة.
- نُقلت اختبارات السلوك إلى حزم الميزات المالكة.
- حُدثت اختبارات الحدود السابقة لمسارات الملفات الجديدة.

### حُذف

- `di/RepositoryModule.kt` المركزي.
- الحزم القديمة للميزات تحت `ui/screens` و`data/repository` و`domain/repository` و`domain/usecases` بعد نقل محتواها.
- ملكية Home لمكوّنات عرض العمولة.

### لم يتغير

- Room schema/version 12.
- Supabase schema وRLS وRPCs.
- قواعد المصادقة والعمولة والرصيد والسحب والدردشة والتقارير.
- تصميم الشاشات ومسارات الاستخدام المقصودة.

### التحقق

- اختبارات السلوك: 10/10 ناجحة.
- المراجعات المعمارية: 37/37 ناجحة.
- التجميع الساكن: 6/6 ناجح.
- فحص الربط الداخلي: 205 ملفات، 334 رمزًا، 0 استيراد داخلي مفقود.
- Gradle تعذر بسبب غياب الشبكة وتوزيع Gradle المحلي.

## v09 — تفكيك الملفات والكائنات الضخمة

### أضيف

- `ChatMessageComponents` و`ChatComposer` و`ChatImageViewer`.
- `HomeHeroComponents` و`HomeSupportCards`.
- `BalanceComponents` و`WithdrawalSheet`.
- `AppDestinations` و`NavigationGraphs` و`NotificationDestinationResolver`.
- `ChatMediaManager` لفصل تجهيز ورفع وتنزيل وسائط الدردشة.
- `PresenceReporter` و`OutboxSynchronizer` و`LocalDataCleaner`.
- `BottomNavigationComponents` و`CardComponents`.
- اختبارات توجيه الإشعارات وأخطاء الوسائط وحدود المسؤوليات.

### عُدل

- `ChatScreen` أصبح Route مسؤولًا عن الربط والحالة فقط.
- `HomeScreen` و`BalanceScreen` احتفظا بتجميع الشاشة ونُقلت المكونات التفصيلية.
- `AppNavigation` أصبح Composition Root يستدعي Graphs مستقلة.
- `ChatRepositoryImpl` يفوض تفاصيل الوسائط إلى `ChatMediaManager`.
- `SyncManager` يفوض الحضور والـOutbox وتنظيف الحساب إلى مالكيها.
- تحميل صور الدردشة أصبح داخل `rememberCoroutineScope` المرتبط بعمر الشاشة بدل Scope منفصل.

### لم يتغير

- Room schema/version 12.
- Supabase schema وRLS وRPCs.
- قواعد المصادقة والعمولة والرصيد والسحب والدردشة.
- تصميم الشاشات ومسارات الاستخدام المقصودة.

### التحقق

- اختبارات السلوك: 15/15 ناجحة.
- اختبارات الحدود المضافة: 7/7 ناجحة.
- المراجعات المعمارية التفصيلية: 59/59 ناجحة.
- مجموعات التجميع الساكن: 8/8 ناجحة.
- تحليل صياغة Kotlin: 29/29 ملفًا ناجحًا.
- فحص الربط الداخلي: 225 ملفًا، 355 رمزًا، 0 استيراد داخلي مفقود.
- Gradle تعذر بسبب غياب الشبكة وتوزيع Gradle المحلي.

## v10 — أداء Room والعلاقات

### أضيف

- Migration من Room 12 إلى 13.
- 20 فهرسًا للاستعلامات الفعلية حسب المالك والحالة والترتيب.
- `RoomPerformanceArchitectureTest`.
- `tools/verify_room_v10.py` للتحقق من Migration وخطط الاستعلام.
- توثيق قرار العلاقات في `room-performance-v10.md`.

### عُدل

- Entities تعلن الفهارس نفسها التي تنشئها Migration.
- فهرس Outbox أصبح يغطي `status + next_retry_at + created_at`.
- `DatabaseMigrationTest` يتحقق من الفهارس وخطط الاستعلام بعد الترقية 4→13.
- اختبارات معمارية قديمة حُدثت لمسارات V09 وRoom 13 دون تغيير سلوك الإنتاج.

### لم يتغير

- Supabase schema وRLS وRPCs.
- قواعد المال والعمولة والسحب والدردشة.
- تصميم الشاشات ومسارات الاستخدام.
- لم تُفرض Foreign Keys على Aggregates تُزامَن مستقلًا عبر Realtime.

### التحقق

- اختبارات SQLite: 74/74 ناجحة.
- المراجعات المعمارية: 51/51 ناجحة.
- التجميع الساكن: 3/3 ناجح.
- Gradle وInstrumentation تعذرا بسبب غياب الشبكة وتوزيع Gradle المحلي.

## v11 — Observability والأمان

### أضيف

- `FirebaseCrashlyticsReporter` لتسجيل أخطاء Release غير القاتلة.
- `SensitiveDataRedactor` لتنقية النصوص والحقول وThrowable قبل التسجيل.
- `SyncDiagnostics` لتسجيل سبب المزامنة، زمن المراحل، آخر نجاح/خطأ، Outbox، وRealtime.
- `ObservabilityModule` لربط مراقبة المزامنة عبر Hilt.
- `local.properties.example` وإعدادات BuildConfig من Properties أو Environment.
- `ObservabilitySecurityArchitectureTest` واختبارات تنقية السجلات.
- `observability-v11.md` و`rls-review-v11.md`.
- `tools/verify_observability_v11.py` و`tools/verify_rls_v11.sql`.

### عُدل

- `AppLogger` أصبح Structured Logger يعمل في Release عبر Reporter بعد التنقية.
- `DefaultSyncCoordinator` يسجل سبب المزامنة ونتيجتها ومدتها وآخر نجاح.
- `SyncStepExecutor` يسجل زمن ونتيجة كل مرحلة دون Payload أعمال.
- `OutboxSynchronizer` يسجل أعداد الحالات بدل البيانات الفردية.
- `RealtimeManager` يسجل حالات الاتصال وBackoff.
- أخطاء Outbox المخزنة تُنقّى قبل الحفظ.
- إعدادات Supabase ورقم الإدارة أزيلت من `build.gradle.kts`.
- OTP يستخدم SMS Retriever دون قراءة الرسائل العامة.

### حُذف

- صلاحيتا `READ_SMS` و`RECEIVE_SMS`.
- استقبال `Telephony.SMS_RECEIVED` وفك PDU داخل شاشة OTP.
- Crash dump النصي المحلي الذي كان قد يحفظ Stack trace كاملًا داخل الملفات.
- Supabase URL وanon key من كود Gradle المباشر.

### لم يتغير

- Room schema/version 13.
- قواعد المال والعمولة والسحب.
- Supabase schema وسياسات RLS الفعلية؛ أضيفت أداة تحقق فقط ولم تُشغّل على الإنتاج.
- تصميم الشاشات ومسارات الاستخدام.

### التحقق

- اختبارات السلوك: 17/17 ناجحة.
- اختبارات SQLite الارتدادية: 74/74 ناجحة.
- المراجعات المعمارية: 59/59 ناجحة.
- فحوص Observability والأمان: 21/21 ناجحة.
- التجميع الساكن: 5/5 ناجح.
- Gradle تعذر بسبب غياب الشبكة وتوزيع Gradle المحلي.
- تحقق RLS الفعلي مطلوب داخل Supabase.



## v12 — Package-by-Feature داخل app

### أضيف

- بنية `core` للخدمات والنماذج المشتركة المستقرة.
- بنية `coordinator` للعمليات العابرة للميزات.
- حزمة `navigation` للوجهات وتجميع Feature graphs.
- `RegistrationProfileWriter` لفصل التسجيل عن تنفيذ الملف الشخصي.
- Mappers مملوكة لميزات Balance وCommission وChat وNotifications.
- `PackageByFeatureArchitectureTest` و`tools/verify_package_v12.py`.
- وحدات DI مستقلة للجلسة والمزامنة والمراقبة وربط التسجيل.

### عُدل

- نُقلت قاعدة البيانات والشبكة والجلسة والمزامنة والمراقبة والتصميم إلى `core`.
- نُقل `SyncManager` إلى `coordinator/sync` وخدمة FCM إلى `coordinator/notifications`.
- نُقلت الشاشات والموديلات والمستودعات إلى الميزات المالكة.
- فُصلت نماذج الحساب والمال والإشعارات والعمولات إلى ملاك واضحين.
- نُقلت الوجهات ومكونات Bottom Navigation إلى `navigation`.
- حُدثت Manifest وHilt bindings والاختبارات والمسارات والأدوات.
- حُدث اختبار Outbox ليؤكد عدم تسريب معرف العملية داخل التشخيص.

### حُذف

- الجذور الطبقية القديمة: `data` و`domain` و`ui` و`utils` و`notifications` و`observability`.
- الاعتماد الدائري بين Auth وProfile عبر فصل عقد كتابة ملف التسجيل.
- الاعتماد من `core` على نماذج Features في PDF عبر نماذج تصدير محايدة.

### لم يتغير

- Room schema/version 13.
- Supabase schema وRLS وRPCs.
- قواعد المال والعمولة والسحب والمزامنة وOutbox.
- تصميم الشاشات ومسارات الاستخدام المقصودة.

### التحقق

- اختبارات السلوك: 48/48 ناجحة.
- المراجعات المعمارية: 67/67 ناجحة.
- فحوص Room: 74/74 ناجحة.
- فحوص Observability والأمان: 21/21 ناجحة.
- فحوص الحزم والاستيرادات: 9/9 ناجحة.
- التجميع الساكن: 6/6 ناجح.
- Gradle تعذر لأن Wrapper يحتاج تنزيل التوزيع والبيئة بلا شبكة.

## v13 — استخراج Gradle Modules

### أضيف

- 9 وحدات Core: Model وCommon وDatabase وNetwork وObservability وSession وSync وDesign System وPlatform.
- 6 وحدات Feature: Auth وChat وNotifications وCommission وBalance وProfile.
- Build script وManifest مستقلان لكل Android library.
- `GradleModuleArchitectureTest` و`ProjectLayout` متعدد الوحدات.
- `tools/project_layout.py` و`tools/verify_modules_v13.py`.
- `module-graph-v13.md` لتوثيق مخطط الاعتماديات.
- عقد `BottomNavBadgeSource` لعكس اعتماد Design System على Notifications.

### عُدل

- `settings.gradle.kts` يسجل الوحدات الخمس عشرة.
- `:app` يجمع الوحدات ويحتفظ بالتنقل والميزات غير المستقرة فقط.
- اختبارات المعمارية وأدوات Room والأمان والحزم أصبحت متعددة الوحدات.
- `SplashViewModel` يعيد وجهة محايدة وتحوّلها `MainActivity` إلى Route.
- نُقلت Chat DTOs إلى Network وSync diagnostics إلى Sync لكسر الاعتماديات الدائرية.
- نُقلت موارد Android إلى Design System كملكية انتقالية واحدة.

### حُذف

- نسخ Core والميزات المستخرجة من Source Set الخاص بـ`:app`.
- اعتماد Design System المباشر على Notification ViewModel.
- مواقع SyncManager وChat DTOs القديمة.

### لم يتغير

- Room schema/version 13.
- Supabase schema وRLS وRPCs.
- قواعد المال والعمولة والسحب والمزامنة وOutbox.
- تصميم الشاشات ومسارات الاستخدام المقصودة.

### التحقق

- اختبارات السلوك: 48/48 ناجحة.
- المراجعات المعمارية: 74/74 ناجحة.
- فحوص Gradle Modules: 60/60 ناجحة.
- فحوص الحزم: 24/24 ناجحة.
- فحوص Room والأمان: 95/95 ناجحة.
- التجميع الساكن: 4/4 ناجح.
- Gradle/KSP/Android resource linking تعذرت بسبب غياب توزيع Gradle والشبكة.

## v14 — الإغلاق والتنظيف

### أضيف

- `RegistrationProfileWriter` كمنفذ عابر للميزات داخل `core:common`.
- `SignOutAction` كمنفذ خروج محايد داخل `core:common`.
- `AuthSignOutAction` كتنفيذ مملوك لميزة المصادقة.
- `ClosureCleanupArchitectureTest`.
- `tools/verify_cleanup_v14.py`.
- توثيق المعمارية الفعلية والمستهدفة ومخطط الوحدات النهائي.

### عُدل

- `RegisterViewModel` يعتمد على منفذ Core بدل عقد مملوك لميزة Auth.
- `ProfileViewModel` يعتمد على `SignOutAction` بدل Use Case من Auth.
- `ProfileFeatureModule` يربط منفذ كتابة ملف التسجيل محليًا.
- `AuthFeatureModule` يربط تنفيذ تسجيل الخروج.
- أزيل اعتماد `:feature:profile` على `:feature:auth`.
- سُميت حدود الأسبوع المحلية بوضوح كـFallback للعرض فقط.
- ثُبّت Gradle Wrapper على `8.7` المستقر المتوافق مع AGP 8.5.2 بدل نسخة milestone.

### حُذف

- `RegistrationBridgeModule` من `:app`.
- عقد `RegistrationProfileWriter` القديم داخل Auth.
- `SignOutUseCase` القديم المسبب لاعتماد Profile على Auth.
- alias التوافقي غير المستخدم `PreferencesManager.clear()`.
- `WeeklyCompetitionViewModel` و`WeeklyCompetitionUiState` غير المتصلين بالشاشة الحالية.
- جميع `@Deprecated` الانتقالية غير اللازمة من Production.

### لم يتغير

- Room schema/version 13.
- Supabase schema وRLS وRPCs.
- قواعد المال والعمولة والسحب والمزامنة وOutbox.
- تصميم الشاشات ومسارات الاستخدام المقصودة.

### التحقق

- اختبارات السلوك: 48/48 ناجحة.
- المراجعات المعمارية: 81/81 ناجحة.
- فحوص Gradle Modules: 60/60 ناجحة.
- فحوص الحزم: 24/24 ناجحة.
- فحوص Room: 74/74 ناجحة.
- فحوص Observability والأمان: 21/21 ناجحة.
- فحوص التنظيف: 15/15 ناجحة.
- التجميع الساكن: 6/6 ناجح.
- Gradle وRelease والجهاز تعذرت بسبب غياب الشبكة وتوزيع Gradle المحلي.
