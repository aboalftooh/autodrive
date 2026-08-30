# AutoDrive V14 — مخطط Gradle Modules

## المخطط

```text
:app
  -> :core:model
  -> :core:common
  -> :core:database
  -> :core:network
  -> :core:observability
  -> :core:session
  -> :core:sync
  -> :core:designsystem
  -> :core:platform
  -> :feature:auth
  -> :feature:chat
  -> :feature:notifications
  -> :feature:commission
  -> :feature:balance
  -> :feature:profile
```

## العلاقات بين الميزات

- `:feature:notifications -> :feature:chat` عبر عقد Domain للعداد غير المقروء.
- `:feature:profile -> :feature:balance` عبر Use Case/Domain فقط.
- التسجيل بعد المصادقة يمر عبر `RegistrationProfileWriter` في `:core:common`.
- تسجيل الخروج يمر عبر `SignOutAction` في `:core:common`.
- لا يوجد اعتماد مباشر من Profile إلى Auth.

## النتيجة

- الوحدات: 15.
- الدورات: 0.
- Core → Feature: ممنوع وغير موجود.
- Library → App: ممنوع وغير موجود.
- الاستيرادات مغطاة باعتماديات مباشرة.
