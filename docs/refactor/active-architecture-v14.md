# AutoDrive V14 — المعمارية الفعلية

## النمط

`Feature-first Modular Monolith` لتطبيق Android، مع `:app` كنقطة تجميع وتنقل فقط.

## الوحدات

```text
:app
├── :core:model
├── :core:common
├── :core:database
├── :core:network
├── :core:observability
├── :core:session
├── :core:sync
├── :core:designsystem
├── :core:platform
├── :feature:auth
├── :feature:chat
├── :feature:notifications
├── :feature:commission
├── :feature:balance
└── :feature:profile
```

## اتجاه الاعتماديات

```text
Presentation -> Domain Contract -> Data Implementation -> Room/Supabase
:app -> core:* + feature:*
feature:* -> core:* + domain contracts الضرورية فقط
core:* -> core:* فقط
```

- مخطط Gradle بلا دورات.
- `core` لا يعتمد على `feature` أو `app`.
- Libraries لا تعتمد على `app`.
- التسجيل وحركة الخروج العابرة للميزات تمر عبر Ports داخل `:core:common`.
- `:feature:profile` لا يعتمد على `:feature:auth`.

## البيانات والمزامنة

- Room version: `13`، دون fallback تدميري.
- القيم المالية: `Money/BigDecimal`، وليس `Double`.
- المزامنة تمر عبر `SyncCoordinator` وSingle-flight.
- Realtime مقسم إلى Participants مملوكة للمجالات.
- الكتابات غير المضمونة تمر عبر Outbox محدود المحاولات ويدعم `DEAD_LETTER` وIdempotency.

## الميزات الباقية داخل app

تبقى Home وReports وCompetition وInformation داخل `:app`؛ حدودها Feature-first مستقرة، لكنها لم تُستخرج إلى Gradle Modules لأن الاستخراج ليس شرطًا بلا حاجة تشغيلية واضحة.

## الملاحظات المتبقية

- موارد Android ما زالت مملوكة مركزيًا بواسطة `:core:designsystem`.
- حدود الأسبوع المحلية في Commission مخصصة للعرض الاحتياطي فقط؛ أهلية العمولة مصدرها Supabase.
- نجاح Gradle وKSP وAndroid resource linking وRelease build لم يُثبت داخل البيئة الحالية لغياب الإنترنت وتوزيع Gradle المحلي.
