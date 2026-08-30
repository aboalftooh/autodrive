# بنزين — خطة تحسين المعمارية إلى 9.9/10

## الهدف

رفع التقييم من **8.8/10** إلى **9.9/10** دون إعادة كتابة التطبيق، ودون تغيير قواعد العمل أو عقود الخادم أو بيانات Room إلا بمبرر واختبارات هجرة.

## خط الأساس

- بناء Debug: **ناجح حسب التحقق الحالي**.
- الاختبارات: **موجودة ولم تُشغّل بعد**.
- حالات `@Test` المعرفة في الجزء المراجع: **142**.
- المراجعات المعمارية المعرفة: **81** ضمن 14 ملف اختبار معماري.
- حدود Gradle المعرفة: **15 Module مستخرجة**.
- أبرز نقاط التحسين:
  - `HomeViewModel`: نحو **296 سطرًا** و**10 اعتماديات**.
  - `AutoDriveFirebaseMessagingService`: يصل مباشرة إلى قاعدة البيانات ويحسب محتوى ماليًا.
  - `WeeklyCompetitionRepositoryImpl`: نحو **354 سطرًا** ويملك Scope وDispatchers داخليًا.
  - `NavigationGraphs`: نحو **348 سطرًا**.
  - عدة شاشات Compose تتجاوز **300–400 سطر**.
  - استخدام مباشر لـ`System.currentTimeMillis()` و`Dispatchers.IO` داخل منطق قابل للاختبار.

## مبادئ التنفيذ

1. التعديل تدريجي؛ لا إعادة كتابة شاملة.
2. كل جلسة تحل مسؤولية واحدة واضحة.
3. يمنع تغيير السلوك بالتزامن مع نقل الكود إلا بضرورة موثقة.
4. لا تغيير لـRoom schema دون Migration واختبار هجرة.
5. لا تعتمد Feature على تنفيذ Feature أخرى.
6. Android Services وActivities تبقى Adapters رفيعة.
7. الوقت وDispatchers وRandomness مصادر قابلة للحقن.
8. كل قاعدة جديدة تُحمى باختبار معماري.
9. كل إصلاح يتبعه اختبار سلوك مناسب.
10. التقييم 9.9 لا يعتمد قبل نجاح جميع بوابات الإطلاق.

## مراحل الخطة

| الجلسة | العنوان | الهدف التنفيذي | معيار الإغلاق |
|---|---|---|---|
| v01 | Baseline and Test Gates | تشغيل جميع Unit/Architecture/Lint وبناء خط أساس موثوق | كل النتائج موثقة؛ لا اختبارات مجهولة |
| v02 | Deterministic Runtime | حقن Clock وDispatcherProvider وإزالة الوقت وIO المباشرين من المنطق | اختبارات الزمن والتزامن حتمية |
| v03 | Home Orchestration Split | تفكيك `HomeViewModel` إلى منسقات/Use Cases مستقلة | ViewModel ≤180 سطرًا و≤6 اعتماديات |
| v04 | Push Notification Boundary | جعل FCM Service Adapter رفيعًا | لا DB أو حسابات أعمال داخل Service |
| v05 | Competition Data Split | تقسيم Repository إلى Local/Remote/Cache/Refresh | Repository منسق صغير واختبارات لكل جزء |
| v06 | Reports Read Models | فصل الاستعلامات والتنسيق وتجميع التقارير | ViewModels لا تعرف DB ولا تجمع منطقًا ماليًا |
| v07 | Compose Responsibility Split | تقسيم الشاشات والمكونات الكبيرة حسب المسؤولية | لا ملف شاشة يتجاوز الحد المتفق دون استثناء موثق |
| v08 | Navigation Ownership | توزيع Graphs على الميزات وتثبيت Typed Destinations | App navigation يركب Graphs فقط |
| v09 | App Startup and Lifecycle | تفكيك `AutoDriveApp` و`MainActivity` إلى Startup Coordinators | Activity/Application بلا منطق بنية تحتية متشعب |
| v10 | DI and Module APIs | تنظيف Hilt Modules وإظهار Public APIs فقط | Composition Root واضح ولا bindings عامة غير لازمة |
| v11 | Stable Feature Extraction | استخراج Competition وReports عند تحقق الاستقرار | Module graph بلا Cycles ولا مصادر مكررة |
| v12 | Critical Integration Tests | اختبار Sync/Realtime/Outbox/FCM/Offline كمسارات مترابطة | السيناريوهات الحرجة ناجحة بالكامل |
| v13 | Data and Session Safety | تشديد Migration، تبديل الجلسة، تنظيف البيانات، والعمليات الذرية | لا تسرب بيانات بين المستخدمين أو فقد أثناء الهجرة |
| v14 | Security and Observability | Redaction، أسرار، إشعارات حساسة، Crash context | لا بيانات حساسة في Logs/Crash/Notifications |
| v15 | Performance and Release Gates | Compose stability، Startup، R8، Release، Baseline Profile | Release موقّع وناجح مع قياسات مقبولة |
| v16 | Final Audit and Documentation | مراجعة نهائية مستقلة وتجميد القواعد | جميع البوابات ناجحة والقرار `GO_9_9` |

## تفاصيل الجلسات

### v01 — Baseline and Test Gates

**التنفيذ:**

- تشغيل `testDebugUnitTest` وتسجيل الناجح/الإجمالي.
- عزل عدد اختبارات المعمارية وتسجيل **81/81** أو الأخطاء الفعلية.
- تشغيل `lintDebug` و`assembleDebug`.
- إصلاح بنية الاختبار فقط إن منعت التشغيل؛ لا إصلاح معماري واسع هنا.
- إضافة Script موحد للتحقق.

**المخرجات:**

- `docs/verification-v01.md`.
- `scripts/verify-v01.sh`.
- قائمة فشل مرتبة: Build، Tests، Architecture، Lint.

**شرط الانتقال:** لا توجد نتيجة غير معروفة أو اختبار لا يعمل بسبب البيئة دون توثيق سببه.

### v02 — Deterministic Runtime

**التنفيذ:**

- اعتماد `ClockProvider` و`DispatcherProvider` من Core.
- استبدال الاستخدامات المباشرة المؤثرة في المنطق لـ`System.currentTimeMillis()` و`Dispatchers.IO`.
- تمرير ApplicationScope المملوك للتطبيق بدل Scopes المنفصلة غير المضبوطة.
- إضافة اختبارات للعد التنازلي، cooldown، cache TTL، وتوقيت الأسبوع.

**شرط الانتقال:** اختبارات الزمن لا تستخدم نومًا حقيقيًا، واختبارات Coroutines تعمل بـTestDispatcher.

### v03 — Home Orchestration Split

**التنفيذ:**

- استخراج مراقبة ملخص العمولة والرصيد والإشعارات إلى Dashboard State Producer.
- استخراج تدوير رسائل Dynamo إلى `DynamoMessageController`.
- استخراج AI Insight rotation/loading إلى Controller مستقل.
- نقل حساب حدود الأسبوع إلى Use Case نقي.
- تحويل أحداث الشاشة إلى واجهة Events واضحة بدل دوال متفرقة.

**شرط الانتقال:**

- `HomeViewModel` ≤180 سطرًا.
- Constructor ≤6 اعتماديات.
- لا Repository خام إذا أمكن التعبير عنه Use Case.
- اختبارات State transitions ناجحة.

### v04 — Push Notification Boundary

**التنفيذ:**

- جعل `AutoDriveFirebaseMessagingService` مسؤولًا عن استقبال FCM وتمرير الرسالة فقط.
- إنشاء `PushMessageHandler` لمعالجة النوع والمسار.
- إنشاء `FinanceNotificationContentProvider` لبناء المحتوى المالي عبر عقود Domain.
- إنشاء `SystemNotificationPublisher` لواجهة Android.
- منع الوصول المباشر إلى `AutoDriveDatabase` من Service.
- حقن NotificationIdGenerator بدل الوقت المباشر.

**شرط الانتقال:** Service لا يستورد DB أو DAO أو CommissionCalculator أو Repository أعمال.

### v05 — Competition Data Split

**التنفيذ:**

- فصل RemoteDataSource وLocalDataSource وCachePolicy.
- نقل إدارة Realtime subscription إلى مكون يملك دورة حياة واضحة.
- نقل التحويلات DTO/Entity/Domain إلى Mappers مستقلة.
- إزالة Scope الداخلي أو استبداله بـApplicationScope محقون.
- منع `withContext(Dispatchers.IO)` المباشر.

**شرط الانتقال:** Repository ينظم التدفق فقط، واختبارات cache/realtime/fallback مستقلة.

### v06 — Reports Read Models

**التنفيذ:**

- استبدال حقن قاعدة البيانات كاملة بـQueries/DAOs محددة الملكية.
- إنشاء Read Models للتفاصيل والقوائم والنشاط الأخير.
- نقل التنسيق الزمني والمالي إلى Mappers قابلة للاختبار.
- تبسيط `ReportsViewModel` و`RecentActivityViewModel`.
- اختبار حالات البيانات الفارغة، الجزئية، والمتأخرة.

**شرط الانتقال:** Presentation لا تستورد DB/DAO ولا تنشئ منطق تجميع مالي.

### v07 — Compose Responsibility Split

**التنفيذ:**

- تقسيم `ActivityLogScreen`، `InvoiceDetailScreen`، `RecentActivityScreen`، و`HomeHeroComponents`.
- فصل Stateless content عن Route/state collection.
- تثبيت Models مستقرة وتمرير أقل عدد من Lambdas.
- إضافة Preview وسيناريوهات UI أساسية.
- وضع حدود أحجام ملفات تُفرض معماريًا.

**شرط الانتقال:** Route صغير، Content قابل للاختبار، والمكونات الكبيرة مقسمة حسب المعنى لا حسب عدد الأسطر فقط.

### v08 — Navigation Ownership

**التنفيذ:**

- نقل كل Graph إلى Feature المالك.
- تثبيت Destination contracts وArguments في مكان واحد.
- إبقاء App Navigation كـComposition فقط.
- اختبار Notification routes وDeep links وBack stack.
- منع النصوص الحرة للمسارات عند توفر Typed destinations.

**شرط الانتقال:** `NavigationGraphs.kt` يُحذف أو يصبح مجموعة ملفات Feature صغيرة؛ لا Composable شاشة من Feature أخرى داخل App graph.

### v09 — App Startup and Lifecycle

**التنفيذ:**

- استخراج تهيئة التسجيل، Push token، القناة، والمزامنة الأولية.
- جعل `AutoDriveApp` ينفذ Startup pipeline واحدًا.
- جعل `MainActivity` Host للواجهة والتنقل فقط.
- توحيد Scopes المملوكة للتطبيق وإغلاقها الصحيح.
- اختبار startup idempotency وعدم تكرار التسجيل.

**شرط الانتقال:** لا عمليات شبكة أو تفرعات أعمال داخل Activity/Application مباشرة.

### v10 — DI and Module APIs

**التنفيذ:**

- تقسيم AppModule إلى Database/Platform/Coordinator modules.
- تقليل الأنواع المكشوفة من Modules.
- منع حقن DB كاملة عندما يكفي DAO أو Query contract.
- توحيد Qualifiers للDispatchers وScopes.
- اختبار عدم وجود bindings متكررة أو Cycles خفية.

**شرط الانتقال:** كل Binding له مالك واضح، وApp هو Composition Root الوحيد.

### v11 — Stable Feature Extraction

**التنفيذ:**

- استخراج `feature:competition` أولًا.
- استخراج `feature:reports` بعد تثبيت Read Models.
- إبقاء Dashboard داخل App إن ظل منسقًا لميزات متعددة؛ لا استخراج شكلي.
- تعريف Navigation/API contracts بدل اعتماد التنفيذات.
- تحديث اختبارات رسم Gradle.

**شرط الانتقال:** لا Cycles، لا مصدر مكرر، ولا Feature تعتمد على Data implementation لFeature أخرى.

### v12 — Critical Integration Tests

**التنفيذ:**

- Sync hint من FCM حتى تحديث Room ثم UI.
- Realtime duplicate/out-of-order events.
- Outbox retry، backoff، idempotency، وفشل جزئي.
- Offline-first ثم reconnect.
- Withdrawal/commission/balance consistency.

**شرط الانتقال:** كل مسار حرج له اختبار نجاح وفشل واستعادة، وليس Happy path فقط.

### v13 — Data and Session Safety

**التنفيذ:**

- اختبار جميع Migrations المتاحة.
- اختبار تبديل المستخدم والمؤسسة أثناء وجود WorkManager/Realtime.
- إيقاف العمال والاشتراكات قبل تنظيف البيانات.
- مراجعة Transactions للعمليات المالية المترابطة.
- اختبار restore/backup إن كانت ضمن المنتج.

**شرط الانتقال:** لا بيانات قديمة تظهر في جلسة جديدة، ولا فقد بيانات في Migration.

### v14 — Security and Observability

**التنفيذ:**

- توسيع SensitiveDataRedactor للهواتف والتوكنات والرسائل والمعرفات الحساسة.
- مراجعة Crashlytics breadcrumbs وcustom keys.
- منع تفاصيل مالية حساسة من شاشة القفل وفق سياسة واضحة.
- فحص BuildConfig وSecrets ونسخ Debug/Release.
- إضافة Security architecture tests.

**شرط الانتقال:** فحص ثابت وسلوكي يثبت عدم تسرب الأسرار والبيانات الحساسة.

### v15 — Performance and Release Gates

**التنفيذ:**

- تفعيل وفحص R8 وShrink Resources في Release.
- إضافة Baseline Profile لمسارات الدخول والرئيسية والتقارير والمحادثة.
- قياس Startup وjank وCompose recompositions في النقاط الحرجة.
- مراجعة Room indexes والاستعلامات الثقيلة بالقياس لا التخمين.
- بناء APK/AAB Release موقّع واختباره على جهاز حقيقي.

**شرط الانتقال:** Release يعمل دون أعطال Minify، والمسارات الأساسية ضمن ميزانية الأداء المتفق عليها.

### v16 — Final Audit and Documentation

**التنفيذ:**

- تشغيل جميع Unit/Architecture/Integration/Instrumentation tests.
- تشغيل Lint وDebug/Release builds.
- فحص dependency graph وpackage/path والملفات الكبيرة والقواعد الممنوعة.
- تحديث Active Architecture وDependency Rules وRelease Readiness.
- مراجعة مستقلة لأي استثناء معماري باقٍ.

**شرط الإغلاق:**

- جميع الاختبارات الناجحة = الإجمالي.
- جميع المراجعات المعمارية الناجحة = الإجمالي.
- Static compilation للملفات المعدلة = الإجمالي.
- 0 Cycles.
- 0 تسربات حدود عالية الخطورة.
- Release موقّع ناجح على جهاز.
- القرار النهائي: `GO_9_9`.

## نقاط التقييم المشروطة

| الحزمة | الجلسات | النتيجة المتوقعة بعد النجاح الكامل |
|---|---|---:|
| تأسيس القياس والحتمية | v01–v02 | 9.1/10 |
| تفكيك المسؤوليات | v03–v10 | 9.5/10 |
| التكامل وسلامة البيانات | v11–v13 | 9.8/10 |
| الأمان والأداء والإطلاق | v14–v16 | 9.9/10 |

هذه الدرجات لا تُمنح تلقائيًا بمجرد تعديل الملفات؛ تعتمد على نتائج التحقق الفعلية.

## تسليم كل جلسة

يجب أن تحتوي الحزمة المسلمة على:

```text
project-full-vXX.zip
docs/Refactor_vXX_Documentation.md
docs/verification-vXX.md
docs/refactor-changelog.md
docs/refactor-plan.md
scripts/verify-vXX-static.sh
```

ويجب أن يذكر الرد النهائي فقط بوضوح:

- ما نُفذ.
- الاختبارات: `الناجح/الإجمالي`.
- المراجعات المعمارية: `الناجح/الإجمالي`.
- التجميع الساكن: `الناجح/الإجمالي`.
- Gradle Build: ناجح/فاشل مع الأمر.
- المرحلة التالية.
