# AutoDrive — المعمارية المستهدفة بعد V14

## الهدف المستقر

الإبقاء على `Feature-first Modular Monolith`، وليس التحول إلى Microservices أو إعادة كتابة التطبيق.

```text
:app
  Composition Root + Navigation + Application lifecycle

:core:*
  عقود ونماذج وتقنيات مشتركة مستقرة فقط

:feature:*
  presentation / domain / data / di
```

## القواعد النهائية

1. لا تعتمد Feature على تنفيذ Data أو DI لميزة أخرى.
2. العلاقات العابرة تمر عبر Port في Core أو Domain Contract ثابت.
3. ViewModel لا يصل إلى DAO أو Database أو Supabase أو SyncManager.
4. Room مصدر القراءة المحلي؛ Supabase مصدر الحقيقة البعيد للقواعد المالية.
5. Realtime يحدّث Room، والواجهة تراقب Room/Repositories.
6. الكتابات القابلة للتأجيل تمر عبر Outbox موثوق.
7. المال يستخدم `Money/BigDecimal` فقط.
8. كل تغيير Schema يتطلب Migration واختبار حفظ بيانات.
9. أي Module جديد يجب أن يحل مشكلة اعتماد فعلية، لا أن يكون تقسيمًا شكليًا.

## التوسع المستقبلي المشروط

يمكن استخراج Home وReports وCompetition إلى Modules مستقلة فقط عند:

- استقرار حدودها.
- وجود حاجة لبناء مستقل أو تقليل زمن البناء.
- عدم إنشاء دورة اعتماد.
- وجود اختبارات تغطي النقل.

## بوابة القبول لأي تعديل لاحق

```text
Unit tests
Architecture tests
Room/Migration checks عند الحاجة
Static compilation
Gradle test + lint + assemble
Release/device verification عند الإصدار
```
