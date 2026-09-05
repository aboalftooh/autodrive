# مراجعة RLS — V11

## النطاق

هذه المراجعة تغطي الجداول وRPCs التي يستدعيها تطبيق AutoDrive من العميل. الكود يستخدم `anon key` فقط، لذلك الحماية النهائية يجب أن تكون داخل Supabase RLS، وليس داخل فلاتر التطبيق وحدها.

## الجداول التي يجب أن يكون RLS مفعّلًا عليها

```text
autodrive_users
invoices
invoice_items
payments
commission_payments
commission_eligibility
marketer_balance
balance_transactions
withdrawal_requests
notifications
conversations
internal_messages
push_tokens
weekly_competition_results
weekly_competition_weeks
ai_insights
dynamo_content
```

## قواعد الملكية المطلوبة

| النطاق | قاعدة القراءة/الكتابة المطلوبة |
|---|---|
| المستخدم | `auth.uid() = user_id` |
| بيانات العميل | ربط `auth.uid()` بسجل `autodrive_users` ثم السماح فقط بـ`client_id` المرتبط |
| الفواتير والمدفوعات والعمولات | القراءة فقط للـ`client_id` المملوك، ومنع الكتابة المباشرة من العميل ما عدا العمليات المصرح بها |
| الرصيد والحركات | القراءة فقط؛ التعديل عبر RPC أو خدمة موثوقة |
| السحب | الإدخال/الإلغاء عبر RPC idempotent، والقراءة للمالك فقط |
| الإشعارات | القراءة وتحديث `is_read` للمالك فقط |
| المحادثات والرسائل | القراءة والكتابة داخل محادثة تخص `client_id` المملوك |
| push_tokens | المستخدم يكتب ويحذف توكناته فقط |
| المحتوى العام | قراءة فقط؛ لا كتابة من `anon/authenticated` |

## RPCs التي يجب مراجعتها

```text
request_withdrawal
cancel_pending_withdrawals
touch_last_seen
get_current_week_number
get_weekly_competition
get_my_competition_history
get_my_win_weeks
```

القواعد المطلوبة:

- لا تقبل RPC معرف مستخدم أو عميل من التطبيق إذا أمكن اشتقاقه من `auth.uid()`.
- أي `SECURITY DEFINER` يحدد `search_path` صراحةً.
- لا يوجد `EXECUTE` لـ`anon` إلا للوظائف المطلوبة قبل تسجيل الدخول.
- عمليات المال تتحقق من الملكية والرصيد و`idempotency key` داخل transaction واحدة.

## التحقق

أُضيف `tools/verify_rls_v11.sql` للتحقق من:

- تفعيل RLS على الجداول المطلوبة.
- وجود Policy واحدة على الأقل لكل جدول.
- عرض صلاحيات RPCs الحساسة وكونها `SECURITY DEFINER` أو `SECURITY INVOKER`.

## حدود النتيجة

لم تُشغّل الاستعلامات على قاعدة Supabase من هذه البيئة؛ لذلك النتيجة الحالية **مراجعة عقد العميل وأداة تحقق جاهزة**، وليست إثباتًا بأن سياسات الإنتاج صحيحة.
