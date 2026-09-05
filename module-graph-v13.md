# AutoDrive V13 — مخطط Gradle Modules

## الهدف

تحويل حدود الحزم المستقرة في V12 إلى حدود Gradle فعلية دون تغيير قواعد العمل أو Room schema أو سلوك الواجهات.

## الوحدات المستخرجة

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
:app
  -> core modules
  -> feature modules

feature modules
  -> core modules
  -> domain contracts لميزات أخرى عند الضرورة فقط

core modules
  -> core modules أدنى منها فقط
  -X-> feature modules
  -X-> :app
```

## العلاقات الخاصة

- `:feature:notifications -> :feature:chat` لاستهلاك عقد عداد المحادثات غير المقروءة فقط.
- `:feature:profile -> :feature:auth` لاستهلاك عقد التسجيل فقط.
- `:feature:profile -> :feature:balance` لاستهلاك عقد الرصيد فقط.
- الاستيراد بين الميزات مقيد بحزم `domain`، وتمنع اختبارات المعمارية استيراد `data` أو `di` أو `presentation`.

## قرارات انتقالية

- بقيت الموارد في `:core:designsystem` كمالك Android resource وحيد لتجنب اعتماد المكتبات على `:app`.
- بقيت ميزات Home وReports وCompetition وInformation داخل `:app` إلى أن تستقر حدودها قبل استخراجها.
- بقيت اختبارات المشروع في `:app` مؤقتًا، لكنها أصبحت تبحث في جميع Source Sets عبر `ProjectLayout`.

## التحقق

- الوحدات المعلنة: 15/15.
- مخطط Gradle: بلا دورات.
- الاستيرادات مغطاة باعتماديات مباشرة.
- Core لا يعتمد على Feature أو App.
- المكتبات لا تعتمد على `:app`.
- الاستيراد بين الميزات عبر Domain فقط.
