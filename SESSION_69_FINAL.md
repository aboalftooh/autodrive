# SESSION_69_FINAL.md

## AutoDrive Sync Modernization — Session 69

### Unified Idempotent Server Command Contract + Durable Receipts + Typed Retry/Reconciliation

**نوع المستند:** عقد تنفيذ مستقل وصارم للجلسة الثالثة من مسار تحديث مزامنة AutoDrive  
**الجلسة:** 69  
**الحالة:** `IMPLEMENTED SOURCE — STATIC/MODEL VERIFIED; PREDECESSOR + BUILD/SERVER RUNTIME BLOCKED`  
**تاريخ الصياغة:** 2026-08-21  
**مصدر الكود المفحوص:** `AutoDrive-v68-atomic-transactional-outbox.zip`  
**SHA-256 للمصدر المفحوص:** `8b6f148923900208fa1386a4c68d7f05375b4bb21dfa3e1c67091d643e8682b5`  
**Archive entries:** `1278`  
**Production Kotlin files:** `259`  
**Test Kotlin files:** `45`  
**Room الحالي:** `15`  
**Room المستهدف في 69:** `15` — لا migration محلية افتراضيًا  
**مرجع الجلسة السابقة:** `SESSION_68_FINAL.md`  
**SHA-256 لمرجع الجلسة السابقة:** `fae03d859353db258000e20d7a249e99b2bf0e2e8697444b570b1851c92a60b1`  
**تقرير تحقق v68:** `AUTODRIVE_SYNC_VERIFICATION_v68.json/.md`  
**SHA-256 v68 JSON:** `ca9bc61310b7882645ec0cadb0df2280de8b3c59ba06676dca22385ddc5db02b`  
**SHA-256 v68 MD:** `b5c3b13854082715e6e891bcabf4c60232a6e1e94a92966f45bb7bf3e71248fa`  
**v68 final verdict الحالي:** `IMPLEMENTED_STATIC_BUT_PREDECESSOR_BLOCKED`  
**v68 handoff69Authorized الحالي:** `false`  
**v68 static/model:** `36/36 PASS`  
**v68 build/runtime:** `BLOCKED_ENVIRONMENT_NETWORK_BOOTSTRAP — UnknownHostException: services.gradle.org`  
**v67 inherited blocker:** `BLOCKED_SERVER_TOMBSTONE_CONTRACT`  
**الخطة المضغوطة المرجعية:** `AUTODRIVE_SYNC_MODERNIZATION_REPAIR_PLAN_v67-v73.md` كما ثبت في `SESSION_67_FINAL.md`  
**مرجع النطاق الأصلي:** `AUTODRIVE_SYNC_MODERNIZATION_REPAIR_PLAN_v67-v80.md` — بند Idempotent Command Contract الأصلي فقط، مع إعادة ترقيمه إلى 69 بسبب ضغط الخطة.

---

# 0. الحكم التنفيذي المختصر

Session 69 ليست تحسينًا لـOutbox المحلية.

v68 حلت بالفعل:

```text
Local mutation + scoped durable Outbox
```

لكن الإرسال الحالي ما زال في عدة أوامر بالشكل:

```text
Outbox mutationId
      ↓
direct PostgREST/RPC call
      ↓
success inferred from one response
      ↓
local finalize
```

والخطر المركزي هو:

```text
server committed
→ response lost / timeout
→ Android cannot prove commit for every command
→ retry may duplicate an effect OR a valid mutation may die as DEAD_LETTER
```

الحالة المستهدفة بعد 69:

```text
Scoped durable Outbox operation
      ↓
Typed server command with immutable mutationId
      ↓
Server transaction {
    derive authenticated scope
    claim unique command identity
    verify request fingerprint
    apply business effect exactly once
    persist canonical command receipt
}
      ↓
Canonical receipt returned/replayed
      ↓
Android typed outcome
      ↓
reconcile/finalize exact local operation
```

القواعد المطلقة:

```text
Same logical mutation + same mutationId + same command = same committed result.
```

```text
Same mutationId reused with different command or different canonical request = CONFLICT, never a second effect.
```

```text
Timeout after server commit must be recoverable by receipt lookup/replay.
```

```text
Retryability must not depend on parsing human-readable error text.
```

```text
No command receipt may trust userId/clientId/orgId supplied by Android as ownership authority.
```

```text
Receipt revision introduced in 69 is not a Sync cursor and must not masquerade as the future global change-feed revision.
```

```text
Session 69 must preserve v67/v68 cursor, generation, atomic Outbox, lease, and logout isolation semantics.
```

---

# 1. لماذا 69 الحالية هي Idempotent Commands وليست Push-before-Pull

الخطة الأصلية v67→v80 كانت تضع:

```text
69 = Push Before Pull + Sync Generation
70 = Idempotent Command Contract
```

لكن `SESSION_67_FINAL.md` المضغوطة v67→v73 ضمت Push-before-Pull + Generation إلى Session 67 نفسها، وصرحت أن:

```text
Unified idempotent server command protocol -> 69
typed retry taxonomy النهائية             -> 69
```

وكود v68 يثبت بقاء:

```text
Recover leases
→ Push Outbox
→ Pull
→ generation drain
```

لذلك:

```text
SESSION_69_SCOPE = IDEMPOTENT_SERVER_COMMANDS + TYPED_RETRY
```

وأي إعادة تنفيذ Generation كهدف 69 تعتبر:

```text
BLOCKED_DUPLICATE_SCOPE
```

---

# 2. بوابة البداية — v68 Handoff Gate

قبل أي mutation في 69 يجب قراءة:

```text
AUTODRIVE_SYNC_VERIFICATION_v68.json
```

والتحقق من:

```text
finalVerdict
handoff69Authorized
predecessorGateSatisfied
staticGatesPassed
newV68WaiverCount
roomVersionAfter
outboxScopeFieldsPresent
retryLeaseSeparated
```

الحالة الحالية المثبتة:

```text
finalVerdict                = IMPLEMENTED_STATIC_BUT_PREDECESSOR_BLOCKED
handoff69Authorized         = false
predecessorGateSatisfied    = false
staticGatesPassed           = true
newV68WaiverCount           = 0
roomVersionAfter            = 15
```

إذًا:

```text
SESSION_69_EXECUTION_GATE = BLOCKED_BY_PREDECESSOR_CHAIN
```

يجوز إعداد العقد والمراجعة.

لا يجوز إعلان 69 `PASS` كسلسلة release ما لم:

```text
A) يُغلق blocker الموروث من 67 رسميًا وتصبح handoff صحيحة،
أو
B) يصدر المستخدم Override صريح لتنفيذ 69 فوق السلسلة المحجوبة،
   ويُسجل inherited risk دون تحويله إلى PASS.
```

---

# 3. بوابة ثانية — Authoritative Server Command Evidence

v68 archive يحتوي فقط ثلاث migrations حالية تحت:

```text
supabase/migrations/
```

ولا يحتوي تعريفًا كاملاً وموثوقًا لـ:

```text
request_withdrawal
cancel_pending_withdrawals
autodrive_users table
internal_messages table
notifications table
push_tokens table
RLS policies/grants الحالية
```

المتاح:

```text
Android DTO/call-sites
docs/autodrive-server-contract-v45.md
migration جزئية تخص withdrawal index
```

هذا لا يكفي وحده لإعادة تعريف server functions بأمان.

قبل تنفيذ SQL production في 69 يجب توفير واحد من:

```text
A) schema.sql حديث موثوق
B) live pg_dump schema-only
C) migrations الأصلية التي تعرف الجداول/RPCs/RLS المستخدمة
D) live introspection موثق يغطي objects التي ستُعدل
```

بدون ذلك:

```text
SERVER_COMMAND_SCHEMA_EVIDENCE = INCOMPLETE
```

ويجوز تجهيز Android abstractions/tests فقط، لكن:

```text
FULL_SESSION_69_PASS = BLOCKED_SERVER_COMMAND_CONTRACT
```

ممنوع اختراع function signatures أو RLS semantics.

---

# 4. Baseline Gate — هوية v68

قبل التعديل:

```text
ZIP SHA-256            = 8b6f148923900208fa1386a4c68d7f05375b4bb21dfa3e1c67091d643e8682b5
archive entries        = 1278
production Kotlin      = 259
test Kotlin            = 45
Room                   = 15
v68 fixtures           = 36/36 PASS
v68 UI changes         = 0
v68 unexpected changes = 0
v68 waivers            = 0
```

أي اختلاف غير موثق:

```text
BLOCKED_INPUT_DRIFT
```

إذا Room ليست 15:

```text
BLOCKED_ROOM_BASELINE_DRIFT
```

---

# 5. ترتيب السلطات — Authority Order

عند التعارض:

1. `AutoDrive-v68-atomic-transactional-outbox.zip` بالـSHA أعلاه.
2. `AUTODRIVE_SYNC_VERIFICATION_v68.json/.md`.
3. `SESSION_68_FINAL.md`.
4. `SESSION_67_FINAL.md` لتحديد نطاق 69 المضغوط وحدود 70–73.
5. `AUTODRIVE_SYNC_MODERNIZATION_REPAIR_PLAN_v67-v73.md` إذا قُدمت بنفس SHA المثبت.
6. `AUTODRIVE_SYNC_MODERNIZATION_REPAIR_PLAN_v67-v80.md` فقط لبند Idempotent Command Contract الأصلي.
7. authoritative current server schema/introspection عند توفره.
8. كود v68 الفعلي.
9. هذا العقد.

ممنوع استيراد RPCs/SQL من Verto أو Optimal.

---

# 6. بصمات Authority عند البداية

يجب تسجيل:

```text
AutoDrive-v68-atomic-transactional-outbox.zip
  8b6f148923900208fa1386a4c68d7f05375b4bb21dfa3e1c67091d643e8682b5

SESSION_68_FINAL.md
  fae03d859353db258000e20d7a249e99b2bf0e2e8697444b570b1851c92a60b1

AUTODRIVE_SYNC_VERIFICATION_v68.json
  ca9bc61310b7882645ec0cadb0df2280de8b3c59ba06676dca22385ddc5db02b

AUTODRIVE_SYNC_VERIFICATION_v68.md
  b5c3b13854082715e6e891bcabf4c60232a6e1e94a92966f45bb7bf3e71248fa

Entities.kt
  5235b58a2445d0bba9ac08abbfe11b80740ab2f460fb7cfc7ade11d67e837d75

PendingOperationDao.kt
  74d69660ba66f70e2044ced891e95c3f13cee8b6e9b06225b0be617a07af755d

AutoDriveDatabase.kt
  75ee82632d32b389ac6efd065e8592c7461037287f69a3e447b5d3a3ea29878f

OutboxSynchronizer.kt
  d8177ca795510651dc590061d090b741429ec0f3c2215ffda485d15ca5a6bc95

OutboxContracts.kt
  cf32c39eefbb966f004a5b65a56ace51c7fa559042c61e13349c98b51492853a

OutboxRetryPolicy.kt
  aef58638c5d9d908fc623250eb4d08a84bbbfc364c6461d7bd8937060474e30f

PendingOperationProcessor.kt
  39979b9d5cfe924904ec7dfb7e1ea9230d1e8155a3b930fa734ce6e08fe8daef

ProfileRepositoryImpl.kt
  d2db9f9d63433a27f89dce6aae1d366d35610c97b7fefc1204e9dec9eb2f6ece

BalanceRepositoryImpl.kt
  4ca638a1db1c21b225158369ddefbb31c74028e367c0d9e301ae17b7b3414d9d

ChatRepositoryImpl.kt
  c21c5fc458e59df5f1b2fc98d2124b2d6947f3bb4a383dae897bc7f30f0a79de

NotificationRepositoryImpl.kt
  abd71ac58d634b29199c590d064021b0ccb0758b9e5ef611f001cb0aad92a698

PushTokenRepository.kt
  cca8f4078336089bd6a0c20f146cbb049414676c58e2036fd25afbbe69d547a2

AuthRepositoryImpl.kt
  60da6ec20ff510286176e533d570e1941f2709c9b2d83452ebb296aa4ddef227

docs/autodrive-server-contract-v45.md
  083b086f1ed1949f24f0837105face1477738e3b723903ce1e2bbf986a924819
```

---

# 7. حقيقة v68 — command receipt الحالية

`OutboxDeliveryReceipt` تحمل فقط:

```text
serverEntityId
serverCreatedAt
```

ولا تحمل:

```text
mutationId
commandType
resultStatus
serverRevision
replayed
errorCode
```

الحكم:

```text
CLIENT_RECEIPT_CONTRACT = PARTIAL
SERVER_RECEIPT_AUTHORITY = ABSENT/UNPROVEN
```

---

# 8. حقيقة v68 — UPDATE_PROFILE

الإرسال الحالي:

```text
postgrest["autodrive_users"].update(dto)
WHERE user_id/client_id/org_id
```

ثم:

```text
OutboxDeliveryReceipt()
```

لا يرسل `mutationId` للسيرفر.

Timeout بعد commit لا يمكن إثباته من receipt.

الحكم:

```text
PROFILE_SERVER_IDEMPOTENCY = ABSENT
```

---

# 9. حقيقة v68 — REQUEST_WITHDRAWAL_RPC

الإرسال الحالي يرسل:

```text
client_request_id == mutationId
```

ويفعل:

```text
request_withdrawal(...)
fallback:
find withdrawal_requests by client_request_id
```

هذا أفضل command حاليًا.

لكن لا يوجد في المصدر الحالي تعريف authoritative للـRPC نفسه ولا receipt موحد.

الحكم:

```text
WITHDRAWAL_MUTATION_IDENTITY = PRESENT
WITHDRAWAL_GENERIC_RECEIPT = ABSENT
WITHDRAWAL_SERVER_ATOMIC_RECEIPT = UNPROVEN
```

---

# 10. حقيقة v68 — SEND_CHAT_MESSAGE

الإرسال الحالي:

```text
INSERT internal_messages(payload)
```

والـmessage id هو mutation identity منطقيًا.

لكن لا يوجد command receipt ledger.

Timeout after commit قد يجعل retry يعتمد على insert conflict/behavior غير موثق.

الحكم:

```text
CHAT_SEND_STABLE_ID = PRESENT
CHAT_SEND_RECEIPT = ABSENT
```

---

# 11. حقيقة v68 — Read Receipts

Chat read:

```text
UPDATE internal_messages SET is_read=true ...
```

Notification read:

```text
UPDATE notifications SET is_read=true ...
```

هما state-setting idempotent منطقيًا، لكن:

```text
no mutation receipt
no deterministic replay response
no typed command outcome
```

---

# 12. حقيقة v68 — Push Tokens

`PushTokenRepository` يفعل:

```text
upsert push_tokens ON CONFLICT user_id
delete push_tokens WHERE user_id
```

ويستخدم retry محلي 3 مرات.

لا توجد:

```text
mutationId
Outbox
receipt
typed retry classification
```

Original Idempotent Command plan يشمل token registration/revocation.

---

# 13. حقيقة v68 — typed retry الحالية

`OutboxErrorClassifier` يعتمد على:

```text
error.message lowercased
contains("http 400")
contains("permission denied")
contains("timeout")
...
```

والunknown يعاد تلقائيًا بعدد محدود.

الحكم:

```text
TYPED_RETRY_TAXONOMY = ABSENT
HUMAN_TEXT_PARSING_AUTHORITY = PRESENT
```

يجب أن يصبح النص البشري diagnostic فقط.

---

# 14. حقيقة v68 — mutating server surfaces

الفحص يثبت server mutations خارج القراءات على الأقل في:

```text
OutboxSynchronizer
PushTokenRepository
PresenceReporter
AuthRepositoryImpl
BalanceRepositoryImpl.cancelAllPendingWithdrawals
Profile registration/linking
Chat conversation RPCs
```

Session 69 لا يجوز أن تفترض أن كل RPC متشابه.

يجب إنشاء inventory وتصنيف كل mutation قبل التعديل.

---

# 15. الهدف الدقيق لـ69

عند النهاية يجب إثبات:

```text
1. canonical server command receipt contract exists.
2. stable mutationId crosses Android→server boundary for every in-scope replayable command.
3. server effect + receipt commit atomically.
4. duplicate same mutation replays same receipt without duplicate effect.
5. mutationId reuse with changed request fails deterministically.
6. timeout-before-commit is safe to retry.
7. timeout-after-commit is resolvable by receipt replay/lookup.
8. Outbox success finalization depends on canonical typed receipt where required.
9. typed retry taxonomy replaces message parsing.
10. profile command is idempotent.
11. chat send command is idempotent.
12. withdrawal uses the unified receipt path without losing client_request_id semantics.
13. chat read command is idempotent and receipted.
14. notification read command is idempotent and receipted.
15. token register/revoke receive stable command semantics.
16. financial replayable commands discovered in the source are explicitly classified.
17. no Room migration is introduced unless genuinely required.
18. v67/v68 foundations do not regress.
```

---

# 16. ما ليست عليه 69

ممنوع سحب:

```text
Durable Inbox                                      -> 70
Realtime hint-only rewrite                         -> 70
Chat 10k pagination/recovery                       -> 71
Durable media transfer queue                       -> 71
Unified server change feed                         -> 72
Global monotonic data revision                     -> 72
CURSOR_EXPIRED bootstrap/rebootstrap                -> 72
Anti-entropy manifests                             -> 72
Final sync observability dashboard                 -> 73
Full fault-injection campaign                       -> 73
UI dead-letter recovery screen                     -> later/final closure
```

---

# 17. Room schema policy

69 لا تحتاج افتراضيًا إلى أعمدة Room جديدة.

```text
current Room = 15
target Room  = 15
```

ممنوع رفعها إلى 16 لمجرد تخزين receipt مؤقتة.

Receipt server response يمكن أن تستخدم فقط لإتمام transaction المحلية الحالية.

إذا ثبت احتياج durable receipt محلي منفصل:

```text
STOP
classify whether it is actually Durable Inbox scope
```

ولا تسحب Session 70.

---

# 18. Server migration policy

المتوقع في 69:

```text
new server migration(s) = append-only
historical migration mutations = 0
```

ممنوع:

```text
edit old migration
DROP production tables
disable RLS
grant broad table write to anon/authenticated
service-role key in Android
dynamic SQL from client payload
```

---

# 19. Canonical server receipt — الحد الأدنى

يجب أن تعيد كل command in-scope دلاليًا:

```text
mutationId
commandType
resultStatus
serverEntityId
serverRevision
replayed
errorCode?
```

يجوز اختلاف أسماء JSON، لكن semantics إلزامية.

---

# 20. resultStatus

الحد الأدنى:

```text
APPLIED
REJECTED
CONFLICT
```

`replayed=true` ليست resultStatus جديدة؛ هي metadata لاستدعاء أعاد receipt موجودة.

Transport failure ليس receipt.

---

# 21. serverRevision semantics في69

لأن global data revision مؤجلة إلى72:

```text
serverRevision in 69 MUST NOT be treated as global sync cursor.
```

المقبول:

```text
command-receipt revision/sequence generated by server
```

ويجب توصيفه صراحة:

```text
revisionKind = COMMAND_RECEIPT
```

أو equivalent ثابت.

ممنوع كتابة هذا الرقم إلى `sync_cursors`.

---

# 22. Server receipt ledger

يفضل إنشاء table مخصصة مثل:

```text
autodrive_command_receipts
```

الاسم قابل للتغيير.

الـsemantics المطلوبة:

```text
scope owner
mutationId
commandType
entityType
entityId
requestFingerprint
resultStatus
serverEntityId
serverRevision/receiptRevision
errorCode nullable
createdAt
```

---

# 23. Receipt ownership key

الـunique authority يجب أن تشمل:

```text
userId + clientId + orgId + mutationId
```

أو key أقوى مكافئة مشتقة من authenticated principal.

ممنوع global mutationId دون scope إلا إذا server يثبت UUID عالمي مع ownership validation؛ default العقد scoped.

---

# 24. Scope authority على السيرفر

Android قد يرسل client/org داخل payload لأسباب command data.

لكن ownership authority يجب أن تُشتق من:

```text
auth.uid()
→ canonical autodrive_users membership
→ server-derived clientId/orgId
```

إذا payload scope لا تطابق canonical scope:

```text
SCOPE_MISMATCH
```

ولا effect ولا receipt نجاح.

---

# 25. mutationId contract

`mutationId`:

```text
nonblank
stable across retries
immutable for one logical mutation
never regenerated because request timed out
never generated by server for a client mutation
```

---

# 26. mutationId reuse conflict

إذا server وجد receipt لنفس scope+mutationId لكن:

```text
commandType مختلف
or requestFingerprint مختلف
or entity identity مختلفة
```

يجب:

```text
MUTATION_ID_REUSE_CONFLICT
```

ولا يعاد تنفيذ أي effect.

---

# 27. requestFingerprint

يجب كشف تغير الطلب عبر fingerprint server-side من canonical command input.

المطلوب:

```text
SHA-256 أو equivalent cryptographic fingerprint
```

ممنوع الاعتماد على hash supplied by client وحده.

يفضل:

```text
pgcrypto digest(canonical_jsonb::text, 'sha256')
```

إذا استخدم بديل يجب توثيق determinism.

---

# 28. Sensitive request protection

Receipt ledger لا تخزن raw:

```text
phone
bank account
message body
profile bank fields
push token
```

تخزن fingerprint + non-sensitive identity/result فقط.

---

# 29. Receipt transaction invariant

داخل RPC واحدة:

```text
derive scope
validate
claim mutation identity
apply effect
persist final receipt
return receipt
```

كل ذلك في PostgreSQL transaction واحدة.

الممنوع:

```text
effect COMMIT
then separate receipt INSERT
```

لأنه يعيد نفس crash window على السيرفر.

---

# 30. Processing marker semantics

يجوز internal `PROCESSING` داخل transaction.

لكن لا يجوز أن يبقى committed `PROCESSING` بسبب crash عادي.

السبب:

```text
RPC transaction rollback should remove both claim and effect.
```

أي persisted processing state يحتاج recovery contract صريح وإلا FAIL.

---

# 31. Concurrent duplicate calls

Scenario:

```text
request A mutation M starts
request B same M starts concurrently
```

PASS:

```text
one effect only
one canonical receipt
B waits/replays canonical committed result
```

---

# 32. Timeout before commit

إذا network timeout قبل server commit:

```text
no receipt may exist
or transaction may still later commit
```

Android لا يفترض failure.

الخطوة:

```text
reconcile receipt by same mutationId before a new effect attempt
```

أو replay same command safely.

---

# 33. Timeout after commit

إذا server commit حدث وضاع response:

```text
retry same mutationId
→ returns same receipt
→ no duplicate effect
```

هذا acceptance مركزي.

---

# 34. Duplicate retry N times

Fixture:

```text
same exact command M repeated 100 times
```

PASS:

```text
effect count = 1
receipt identity = 1
all successful responses agree on canonical outcome
```

---

# 35. Deterministic rejection

Business validation مثل:

```text
INSUFFICIENT_BALANCE
INVALID_AMOUNT
```

إذا كانت validation قطعية على command snapshot، يمكن أن تنتج:

```text
REJECTED + typed errorCode
```

لنفس mutation.

إعادة نفس mutation تعيد نفس rejection.

تصحيح input يتطلب mutationId جديدة.

---

# 36. Transient system failure

DB unavailable / internal error قبل commit:

```text
no successful receipt
no partial effect
Android classifies retryable/unknown appropriately
```

لا تسجل `REJECTED` لمشكلة transient.

---

# 37. Auth failure

غير authenticated:

```text
AUTH_REQUIRED
```

لا تُسجل receipt business نجاح.

Android لا يحولها تلقائيًا إلى DEAD_LETTER نهائي إذا الجلسة قابلة للتجديد.

---

# 38. Permission/RLS failure

authenticated لكن لا يملك scope:

```text
PERMISSION_DENIED
```

غالبًا terminal لهذا mutation تحت نفس principal.

لا parsing لعبارة `row-level security`.

---

# 39. Canonical command allowlist

69 يجب أن تملك allowlist compile-time/server-side.

المطلوب الأساسي:

```text
UPDATE_PROFILE
REQUEST_WITHDRAWAL
SEND_CHAT_MESSAGE
MARK_CHAT_READ
MARK_NOTIFICATION_READ
REGISTER_PUSH_TOKEN
REVOKE_PUSH_TOKEN
```

وأي financial replayable command ثبت أنه داخل scope.

---

# 40. ممنوع Generic Dynamic SQL Router

ممنوع RPC من نوع:

```text
execute_command(tableName, operation, payload)
```

إذا كان ينفذ table/column names من client.

المفضل:

```text
typed RPC per command
or strict static dispatcher with no dynamic identifiers
```

---

# 41. Profile command — target

`UPDATE_PROFILE` يجب أن يصبح server command receipted.

Android يرسل:

```text
mutationId
editable profile fields
```

Server يشتق owner scope.

---

# 42. Profile command — no raw PostgREST authority

بعد 69 production Outbox path لا يعتمد على:

```text
postgrest["autodrive_users"].update(...)
```

كـcommand authority.

المطلوب typed RPC/command endpoint.

---

# 43. Profile command — repeated mutation

same profile mutation M:

```text
first call applies update
second call replays receipt
```

لا يحدث second update effect حتى لو values نفسها.

---

# 44. Profile command — newer edits

M1 ثم M2:

```text
M1 != M2
```

كلاهما commands مستقلة.

Receipt M1 لا يجوز أن finalize local profile إلى SYNCED إذا M2 unresolved؛ invariant v68 يبقى.

---

# 45. Profile receipt

الحد الأدنى:

```text
mutationId=M
commandType=UPDATE_PROFILE
serverEntityId=userId
resultStatus=APPLIED|REJECTED|CONFLICT
serverRevision=<command receipt revision>
```

---

# 46. Withdrawal command — preserve identity

يجب الحفاظ على:

```text
mutationId == client_request_id
```

لا wrapper يعيد توليده.

---

# 47. Withdrawal command — wrapper/upgrade

إذا current `request_withdrawal` لا يعيد canonical receipt:

يجوز:

```text
new typed wrapper RPC
```

فقط بعد authoritative schema/function evidence.

المفضل:

```text
request_withdrawal_v2(...)
```

أو اسم compatible موثق.

---

# 48. Withdrawal atomicity

إذا wrapper يستدعي legacy function:

```text
legacy effect + receipt write
```

يجب أن يكونا في PostgreSQL transaction نفسها.

ممنوع HTTP call من Function إلى Function.

---

# 49. Withdrawal reconciliation

البحث الحالي بـ:

```text
withdrawal_requests.client_request_id
```

يبقى fallback/reconciliation evidence.

بعد 69 الأولوية:

```text
canonical command receipt
then target row reconciliation if needed
```

---

# 50. Withdrawal rejection

error codes business الحالية مثل:

```text
INSUFFICIENT_BALANCE
PENDING_REQUEST_EXISTS
BANK_DETAILS_MISSING
INVALID_AMOUNT
USER_NOT_REGISTERED
```

تتحول typed، لا parsing text.

---

# 51. Chat send command — stable identity

الحالي:

```text
messageId == mutationId
```

يبقى مسموحًا.

Server يجب أن يربط receipt بهذا identity.

---

# 52. Chat send command — insert exactly once

same SEND_CHAT_MESSAGE mutation:

```text
internal_messages insert count <= 1
```

إذا row كانت committed قبل response loss، retry يعيد receipt.

---

# 53. Chat payload fingerprint

Fingerprint يجب أن يغطي canonical remote fields اللازمة، مثل:

```text
message id
conversation id
sender identity derived/validated
type
body
media metadata already remote-ready
```

لا يدخل `localPath`.

---

# 54. Chat mutation conflict

same message/mutation id لكن body مختلف:

```text
MUTATION_ID_REUSE_CONFLICT
```

لا overwrite للرسالة الأولى.

---

# 55. Chat server entity id

إذا server PK يساوي client message id:

```text
serverEntityId = messageId
```

إذا مختلف:

```text
serverEntityId = actual server id
```

لكن relation deterministic.

---

# 56. Chat media boundary

69 لا تبني upload durability.

إذا media upload حدث قبل Outbox commit كما في v68:

```text
known deferred risk remains
```

لا تدعي end-to-end exactly-once media semantics.

---

# 57. Chat read command

`MARK_CHAT_READ` يتحول إلى command typed.

الـeffect state-setting:

```text
is_read=true
```

لكن receipt تثبت أن mutation وصلت.

---

# 58. Chat read duplicate

same M repeated:

```text
one canonical receipt
no semantic duplicate
```

حتى إذا update matched zero rows في replay.

---

# 59. Notification read command

نفس القاعدة:

```text
MARK_NOTIFICATION_READ
mutationId stable
notificationId exact
owner derived from auth
```

---

# 60. Notification read receipt

بعد response:

```text
confirmReadSynced + Outbox finalize
```

يبقيان local transaction واحدة كما في v68.

---

# 61. Push token registration — scope

Original plan يشمل Token registration/revocation.

69 يجب إلغاء retry النصي المنفصل كauthority غير موحدة.

---

# 62. Push token mutation identity

لكل logical token-registration attempt:

```text
mutationId stable across all retries
```

لا تستخدم token نفسه كmutationId لأنه sensitive وقد يتغير.

---

# 63. Push token result

REGISTER command receipt لا تخزن raw token.

`serverEntityId` يمكن أن يكون:

```text
userId/device-token row id
```

وفق schema الفعلية.

---

# 64. Push token revocation

DELETE state-setting طبيعيًا idempotent.

لكن replay نفس mutation يجب أن يعيد receipt نفسها، حتى لو row لم تعد موجودة.

---

# 65. Push token durability boundary

69 لا تُلزم بإضافة Room Outbox لتوكنات push إذا lifecycle الحالي يملك recovery كافيًا.

لكن يجب:

```text
stable mutation identity during retry
typed command semantics
no duplicate side effect
```

إذا التنفيذ قرر جعلها Outbox، يجب ألا يرفع Room schema بلا ضرورة.

---

# 66. cancel_pending_withdrawals

هذا command مالي mutating موجود فعليًا خارج Outbox.

قبل التنفيذ يجب تصنيفه صراحة:

```text
IN_SCOPE_IDEMPOTENT_COMMAND
or
DEFERRED_WITH_JUSTIFICATION
```

الافتراضي الصارم:

```text
IN_SCOPE
```

لأنه قابل لتكرار غامض ويغير حالة مالية.

---

# 67. cancel withdrawal receipt

إذا ضُم:

```text
stable mutationId
same result on replay
no double cancellation effect
typed result
```

ولا تعتمد النتيجة على count يتغير في replay بلا receipt.

---

# 68. العمليات المالية الأخرى

يجب scan كل production server mutations.

أي عملية مالية قابلة لإعادة الإرسال:

```text
must be in inventory
```

إذا لا توجد في Android الحالي:

```text
financialReplayableCommandCountOutsideInventory = 0
```

---

# 69. Auth/bootstrap RPCs

RPCs مثل:

```text
verify_invite_code_v2
link_phone_user
link_phone_user_by_phone
registration RPCs
```

ليست تلقائيًا Outbox sync commands.

69 يجب inventory لها، لكن لا توسع steady-state sync protocol دون سبب.

تصنيف إلزامي لكل واحدة:

```text
AUTH_BOOTSTRAP_IDEMPOTENT_EXISTING
NEEDS_IDEMPOTENCY_BLOCKER
OUT_OF_SCOPE_NOT_RETRIED
```

ولا تجاهل صامت.

---

# 70. PresenceReporter

`touch_last_seen` هو presence/ephemeral signal.

يجوز استبعاده من durable receipt:

```text
EPHEMERAL_BEST_EFFORT
```

بشرط عدم تحويل فشله إلى business sync failure.

---

# 71. Chat conversation creation RPCs

`get_or_create_conversation` و`create_new_conversation` يجب inventory.

`get_or_create` قد يكون server-idempotent بالتصميم، لكن يجب إثباته لا تخمينه.

`create_new_conversation` قد يخلق duplicate عند timeout؛ إذا لم يدخل69 يجب تسجيله:

```text
DEFER_TO_CHAT_REPAIR_71
```

مع blocker/known risk واضح.

---

# 72. Command client abstraction

يفضل component مثل:

```text
ServerCommandClient
IdempotentCommandGateway
```

المهم:

```text
typed request
typed receipt
typed transport failure
```

ولا `Map<String, Any>` ديناميكي.

---

# 73. Android receipt model

يجب توسيع `OutboxDeliveryReceipt` أو استبدالها دلاليًا بـ:

```text
mutationId
commandType
resultStatus
serverEntityId
serverRevision
revisionKind
replayed
errorCode
```

---

# 74. Receipt validation

قبل finalize:

```text
receipt.mutationId == operation.mutationId
receipt.commandType == operation.operation
receipt server entity matches operation contract
receipt status is recognized
```

أي mismatch:

```text
INVALID_SERVER_RECEIPT
```

ولا finalize.

---

# 75. APPLIED receipt

إذا:

```text
resultStatus=APPLIED
```

يُسمح local ack/finalize وفق v68 transaction.

---

# 76. REJECTED receipt

لا يُعتبر network failure.

يجب تحويله إلى typed terminal domain outcome.

الـOutbox:

```text
DEAD_LETTER أو atomic business rollback
```

حسب command contract.

لا retry blind.

---

# 77. CONFLICT receipt

لا يحذف العملية.

التصرف:

```text
typed reconciliation required
```

إما resolution مثبتة في 69 أو transition typed terminal.

ممنوع retry endless.

---

# 78. replayed receipt

`replayed=true` مع APPLIED:

```text
success
```

لا تعتبر duplicate error.

هذا هو timeout-after-commit recovery path.

---

# 79. Unknown receipt status

```text
UNSUPPORTED_SERVER_RECEIPT
```

لا finalize.

---

# 80. Typed retry taxonomy — الفئات المطلوبة

الحد الأدنى:

```text
TRANSIENT
AUTH
PERMISSION
VALIDATION
CONFLICT
ALREADY_COMMITTED
AMBIGUOUS
PERMANENT_PROTOCOL
```

يجوز أسماء مختلفة؛ semantics لا.

---

# 81. Retry action mapping

المطلوب:

```text
TRANSIENT          -> RETRY
AUTH               -> PAUSE/REAUTH, not blind terminal
PERMISSION         -> TERMINAL
VALIDATION         -> TERMINAL
CONFLICT           -> RECONCILE
ALREADY_COMMITTED  -> SUCCESS_FROM_RECEIPT
AMBIGUOUS          -> RECONCILE_THEN_RETRY_SAME_MUTATION
PERMANENT_PROTOCOL -> TERMINAL
```

---

# 82. No message parsing authority

بعد 69:

```text
message.contains("http 400")
message.contains("permission denied")
message.contains("timeout")
```

لا يجوز أن تكون decision authority في production Outbox retry.

النص فقط:

```text
diagnostic fallback
```

والـfallback لا يغير classification دون typed source.

---

# 83. Transport errors

IOException/timeout يمكن تصنيفها typed من exception class.

PostgREST/HTTP errors يجب استخدام structured:

```text
HTTP status
Postgres code
RPC typed errorCode/receipt
```

لا الرسالة البشرية.

---

# 84. HTTP classification

Default:

```text
408/425/429/5xx -> TRANSIENT
401              -> AUTH
403              -> PERMISSION
409              -> CONFLICT unless command receipt proves replay
400/422          -> VALIDATION/PROTOCOL according structured body
```

لكن RPC typed errorCode يتقدم على generic mapping حيث موثق.

---

# 85. PostgreSQL codes

إذا exposed:

```text
unique violation on receipt key
```

ليست permanent failure؛ يجب تحويلها إلى receipt replay lookup.

RLS/permission code:

```text
PERMISSION
```

Constraint violation على business input:

```text
VALIDATION/CONFLICT
```

وفق command contract.

---

# 86. Unknown failures

الممنوع:

```text
unknown -> retry N times blindly -> dead letter
```

للأوامر ذات outcome غامض.

المطلوب:

```text
AMBIGUOUS
→ receipt reconciliation
→ retry same mutation only إذا no committed receipt
```

---

# 87. Reconciliation endpoint

يجوز:

```text
get_command_receipt(mutationId)
```

أو replay نفس typed RPC.

الشرط:

```text
same authenticated scope
no receipt data for other principal
```

---

# 88. Receipt lookup privacy

lookup by mutationId يجب ألا يتيح enumeration.

Server يشتق scope من auth.

لا query raw table من Android إن أمكن.

---

# 89. Receipt retention

يجب تحديد retention policy.

الحد الأدنى يجب أن يتجاوز أقصى نافذة retry/offline الواقعية للمشروع.

ممنوع cleanup يجعل old Outbox retry يعيد effect بعد حذف receipt.

إذا retention غير مثبت:

```text
BLOCKED_RECEIPT_RETENTION_CONTRACT
```

---

# 90. Receipt cleanup safety

لا يجوز حذف receipt ما دام نفس mutation قد يعود من جهاز offline ضمن supported horizon.

Cleanup يعتمد على:

```text
retention + product offline support policy
```

لا timestamp عشوائي.

---

# 91. Multi-device semantics

same user/device different mutationIds:

```text
independent commands
```

same logical mutation duplicated عبر devices لا تُفترض deduped إلا إذا يشتركان mutationId.

69 لا تحل conflict resolution العام بين تعديلات مختلفة؛ ذلك لاحق.

---

# 92. Cross-account receipt isolation

scope A receipt لا يمكن قراءتها/replay تحت B.

Fixture إلزامي.

---

# 93. Cross-org isolation

حتى user anomaly أو malformed payload:

```text
receipt/effect must remain canonical org/client scope
```

---

# 94. RLS policy

Receipt table نفسها يفضل:

```text
no direct authenticated mutation grants
```

والتعامل عبر typed functions.

إذا direct SELECT متاح:

```text
strict owner RLS
```

لكن RPC-only أفضل.

---

# 95. SECURITY DEFINER policy

إذا استخدمت functions `SECURITY DEFINER`:

```text
SET search_path explicitly
derive auth.uid()
validate membership
fully qualify tables
revoke broad EXECUTE where needed
grant only intended role
```

أي function تتجاوز RLS بلا ownership check:

```text
BLOCKED_PRIVILEGE_ESCALATION
```

---

# 96. No Android service role

عدد service role keys داخل APK/source:

```text
0
```

---

# 97. Receipt payload logging

ممنوع log:

```text
raw request payload
push token
profile bank data
chat body
auth token
full command receipt raw JSON if sensitive
```

---

# 98. Correlation logging

مسموح:

```text
mutationId truncated/hashed
commandType
resultStatus
replayed
typed errorCode
serverRevision
```

مع redaction الحالية.

---

# 99. Outbox finalization invariant

v68 finalizer transaction تبقى.

69 تضيف receipt validation قبل:

```text
local ack
delete exact claimed Outbox row
```

---

# 100. No server success by HTTP 2xx only

HTTP success بدون valid canonical receipt:

```text
PROTOCOL_FAILURE
```

لـcommands التي تحولت إلى receipt contract.

---

# 101. No local success from target row guess only

Profile/Chat لا finalize بمجرد “لم يحدث exception”.

يجب valid receipt أو reconciliation مثبتة.

---

# 102. Withdrawal compatibility

يجوز target-row reconciliation الحالي كfallback انتقالي.

لكن بعد 69 يجب أن ينتج canonical receipt قبل local finalization إن كانت server contract متاحة.

---

# 103. Retry attempt count

`attemptCount` يبقى عدد failed delivery attempts.

Receipt replay success لا تزيده.

Reconciliation lookup وحدها لا تُحسب failed command effect attempt إلا إذا policy موثقة.

---

# 104. Backoff

يحافظ exponential+jitter، لكن classification source typed.

429 يمكن احترام:

```text
Retry-After
```

إن كان SDK يوفره.

---

# 105. Max attempts

`OUTBOX_MAX_ATTEMPTS = 5` يمكن بقاؤه.

لكن AMBIGUOUS بعد server timeout لا ينتقل terminal قبل receipt reconciliation.

---

# 106. DEAD_LETTER semantics بعد69

كل dead letter يجب أن تحمل typed:

```text
errorCode
errorClass/category
```

حتى لو Room schema لا تضيف column، يمكن encoding category في stable errorCode contract.

UI recovery ليست scope69.

---

# 107. No silent discard

أي terminal command:

```text
local intent remains inspectable
```

إلا إذا command contract يفرض atomic local rollback مثبتًا.

---

# 108. Profile failure finalization

VALIDATION rejection:

```text
do not mark profile SYNCED
```

ويجب منع infinite retry.

---

# 109. Withdrawal rejection finalization

definitive rejection يمكن أن:

```text
atomically remove optimistic local request + finalize Outbox
```

أو mark rejected محليًا.

الاختيار يجب أن يحافظ business semantics الحالية.

---

# 110. Chat send rejection

لا تحذف local message على ambiguous/transport failure.

Permanent validation يمكن:

```text
status=FAILED
Outbox terminal
```

مع mutationId محفوظ.

---

# 111. Read receipt rejection

PERMISSION أو invalid target:

```text
terminal typed
```

لكن local `isRead=true` UX قد يبقى local preference إن كان contract الحالي يقصد ذلك؛ يجب توثيق divergence/reconcile policy.

---

# 112. Token auth failure

إذا session انتهت أثناء token register:

```text
AUTH
```

لا تكتب توكن لحساب آخر بعد login switch.

حماية scope v68 تبقى.

---

# 113. Server command versioning

كل RPC/receipt contract يجب أن يملك version واضح:

```text
v1
```

إما في RPC name أو request/response field.

لا تغير semantics silently.

---

# 114. Outbox contractVersion

لا ترفع `OUTBOX_CONTRACT_VERSION` إلا إذا payload المحلي يتغير فعليًا.

Server command version ≠ Room Outbox version تلقائيًا.

---

# 115. Backward compatibility

pending operations من v68 بعد app update إلى69 يجب أن تظل قابلة للإرسال.

لا migration Room.

Android sender الجديد يحول payload v68 الحالي إلى command v1 مع نفس mutationId.

---

# 116. In-flight upgrade

Scenario:

```text
v68 Outbox row exists
app upgrades to v69
```

PASS:

```text
row sent via new idempotent server command
same mutationId preserved
```

---

# 117. Server rollout order

الترتيب الآمن:

```text
1. deploy server receipt schema/RPCs
2. verify grants/RLS/duplicate behavior
3. ship Android sender using new RPCs
```

ممنوع Android update يعتمد على RPC غير deployed دون feature/compatibility gate.

---

# 118. Mixed client versions

v68 clients قد تستمر direct PostgREST writes.

69 server changes يجب ألا تكسرهم فورًا إلا إذا rollout يضمن forced update.

يجب توثيق compatibility window.

---

# 119. Migration rollback honesty

SQL migration append-only.

لا تعتمد على destructive downgrade.

إذا server deploy فشل:

```text
roll forward fix preferred
```

---

# 120. Required server tests — receipt ledger

يجب اختبار:

```text
unique scoped mutation
same request replay
different request same mutation conflict
cross-user lookup denied
cross-client denied
cross-org denied
concurrent duplicate
rollback leaves no receipt/effect
```

---

# 121. Required server tests — Profile

```text
apply once
duplicate replay
payload mismatch conflict
timeout-after-commit simulation/replay
scope spoof rejected
```

---

# 122. Required server tests — Withdrawal

```text
client_request_id preserved
duplicate no second debit/reservation
rejection replay deterministic
timeout-after-commit returns same server request id
```

---

# 123. Required server tests — Chat Send

```text
same message mutation inserted once
duplicate returns same entity id
changed body same mutation conflicts
```

---

# 124. Required server tests — Read receipts

```text
repeat chat read N times = one receipt
repeat notification read N times = one receipt
target outside scope rejected
```

---

# 125. Required server tests — Tokens

```text
same register mutation replay
same revoke mutation replay
no token leakage in receipt table
scope switch cannot replay old mutation
```

---

# 126. Typed retry fixtures

الحد الأدنى:

```text
timeout -> TRANSIENT/AMBIGUOUS + reconcile
401 -> AUTH
403 -> PERMISSION
409 mutation mismatch -> CONFLICT
422 domain validation -> VALIDATION
5xx -> TRANSIENT
valid replay receipt -> ALREADY_COMMITTED/SUCCESS
invalid receipt -> PERMANENT_PROTOCOL
unknown exception -> AMBIGUOUS, not blind terminal
```

---

# 127. Process death after server commit

Scenario:

```text
server effect + receipt committed
Android dies before local finalize
```

PASS بعد restart:

```text
Outbox row survives
same mutation resent
server replays receipt
Android finalizes local row once
```

---

# 128. Process death before server commit

PASS:

```text
no partial server effect
same mutation safe to retry
```

---

# 129. Lost response fixture

Inject loss after SQL commit before HTTP response reaches Android.

Expected:

```text
second call same mutation
replayed=true
same serverEntityId/result/serverRevision
```

---

# 130. Duplicate response fixture

إذا client sees same receipt twice:

```text
local finalizer remains idempotent
second finalization cannot affect other operation/scope
```

---

# 131. Receipt mismatch fixture

Server/mock returns mutationId M2 for operation M1:

```text
INVALID_SERVER_RECEIPT
Outbox not finalized
```

---

# 132. Receipt entity mismatch

SEND_CHAT_MESSAGE receipt points to unrelated entity:

```text
INVALID_SERVER_RECEIPT
```

---

# 133. Receipt revision fixture

`serverRevision`:

```text
positive/valid
revisionKind=COMMAND_RECEIPT
```

ولا يوجد call site يكتبه إلى sync cursor.

---

# 134. Message parsing static gate

Static verifier يفشل إذا decision logic في production retry تعتمد على:

```text
contains("timeout")
contains("http 400")
contains("permission denied")
```

استثناء diagnostic-only موثق.

---

# 135. Direct Profile write static gate

بعد 69:

```text
OutboxSynchronizer direct autodrive_users.update = 0
```

إذا compatibility fallback موجود يجب أن يكون disabled/explicitly gated ولا يملك retry authority.

---

# 136. Direct Chat insert static gate

بعد 69:

```text
Outbox SEND_CHAT_MESSAGE direct table insert = 0
```

يستبدل typed command.

---

# 137. Direct Read write static gate

بعد 69 Outbox delivery لا تستخدم direct table updates كـcommand authority لـ:

```text
MARK_CHAT_READ
MARK_NOTIFICATION_READ
```

---

# 138. Withdrawal direct RPC gate

`request_withdrawal` legacy call يمكن أن يبقى فقط داخل server wrapper/compatibility adapter، لا كـAndroid final command إذا v2 receipt contract deployed.

---

# 139. Push token retry gate

string/three-attempt loop في `PushTokenRepository` لا يبقى كretry authority مستقلة إذا typed command client يتولى classification/retry.

---

# 140. Historical migration integrity

```text
historicalMigrationMutationCount = 0
```

يشمل جميع migrations الموجودة قبل69.

---

# 141. Production UI gate

```text
productionUiFilesChanged = 0
```

---

# 142. Room migration gate

```text
roomVersionAfter = 15
newRoomMigrationCount = 0
```

إلا blocker موثق يثبت ضرورة غير Inbox-related.

---

# 143. Allowed Android production scope

مسموح عند الحاجة:

```text
core/network/**
core/sync/**
core/platform/.../notifications/**
feature/profile/**/data/**
feature/balance/**/data/**
feature/chat/**/data/**
feature/notifications/**/data/**
feature/auth/**/data/** فقط لتصنيف/ربط token/auth command إن لزم
DI bindings المباشرة
```

---

# 144. Allowed server scope

فقط:

```text
new supabase migration(s) for command receipt infrastructure
typed command RPCs/functions
RLS/grants required for those objects
non-destructive helper functions
```

---

# 145. Forbidden production drift

ممنوع:

```text
Compose/UI redesign
business commission formulas
withdrawal eligibility change
chat pagination redesign
media queue redesign
Realtime participant rewrite
Inbox table
change feed
global revision
bootstrap
anti-entropy
analytics/metrics overhaul
```

---

# 146. Existing server contract preservation

استمر:

```text
no service role on Android
withdrawal remains server-owned RPC business logic
Room remains UI source
Realtime not correctness authority
```

---

# 147. Current tombstone blocker preservation

69 لا “تصلح” Session67 عبر fake tombstones.

```text
BLOCKED_SERVER_TOMBSTONE_CONTRACT
```

يبقى inherited حتى يصل source authoritative.

---

# 148. v68 Outbox scope regression

يجب أن تبقى:

```text
userId/clientId/orgId
scoped due
scoped claim
scoped finalize
scoped delete
leaseUntil separate
```

---

# 149. v68 atomic mutation regression

يجب إعادة fixtures الأساسية لـ:

```text
Profile
Withdrawal
Chat send
Chat read
Notification read
```

entity+Outbox atomicity لا تتراجع أثناء استبدال sender.

---

# 150. v67 generation regression

يجب استمرار:

```text
hint during push
hint during pull
completion-edge
burst coalescing
```

69 لا تغير coordinator algorithm.

---

# 151. Push-before-Pull regression

يبقى:

```text
RECOVER_LEASES
→ PUSH
→ PULL
```

---

# 152. No serverRevision cursor contamination

Static scan:

```text
receipt.serverRevision
```

لا يدخل:

```text
syncCursorDao
SyncCursorEntity
DeletionFeed cursor
```

في69.

---

# 153. Server command inventory artifact

يجب إنتاج machine-readable inventory مثل:

```text
AUTODRIVE_SERVER_COMMAND_INVENTORY_v69.json
```

لكل mutating call-site:

```text
path
command
category
inScope
idempotencyStateBefore
idempotencyStateAfter
receiptState
deferReason
```

---

# 154. Required verification JSON

يجب إنتاج:

```text
AUTODRIVE_SYNC_VERIFICATION_v69.json
```

ويحتوي على الأقل:

```text
sourceSha256
roomVersionBefore
roomVersionAfter
session68Verdict
session68Handoff69Authorized
userExecutionOverrideAccepted
predecessorGateSatisfied
authoritativeServerSchemaProvided
serverCommandInventoryCount
serverCommandInScopeCount
serverCommandDeferredCount
commandReceiptContractPresent
receiptLedgerPresent
receiptScopeDerivedServerSide
requestFingerprintServerSide
sameMutationReplayVerified
mutationReuseConflictVerified
profileIdempotentVerified
withdrawalIdempotentVerified
chatSendIdempotentVerified
chatReadIdempotentVerified
notificationReadIdempotentVerified
pushTokenRegisterClassified
pushTokenRevokeClassified
financialCommandCoverageVerified
timeoutBeforeCommitVerified
timeoutAfterCommitVerified
concurrentDuplicateVerified
typedRetryTaxonomyPresent
messageParsingDecisionCount
ambiguousOutcomeReconcileVerified
invalidReceiptRejected
receiptRevisionCursorContaminationCount
historicalMigrationMutationCount
newServerMigrationCount
newRoomMigrationCount
productionUiFilesChanged
unexpectedProductionMutationCount
newV69WaiverCount
staticFixturePassed
staticFixtureTotal
serverTestPassed
serverTestTotal
buildStatus
androidRuntimeStatus
serverRuntimeStatus
finalVerdict
handoff70Authorized
```

---

# 155. Required verification Markdown

```text
AUTODRIVE_SYNC_VERIFICATION_v69.md
```

الترتيب:

```text
1. Baseline + predecessor gate
2. Server schema evidence
3. v68 command defect inventory
4. Command inventory/classification
5. Receipt schema/ownership
6. Typed RPCs
7. Profile
8. Withdrawal
9. Chat Send
10. Read receipts
11. Push tokens
12. Other financial commands
13. Typed retry taxonomy
14. Ambiguous outcome reconciliation
15. Server tests
16. Android static/model tests
17. Build/runtime truth
18. Diff/scope inventory
19. Deferred work
20. Final verdict + handoff70
```

---

# 156. Required static/model fixtures

الحد الأدنى: **40 checks**:

```text
01 v68 Outbox schema preserved
02 Room remains 15
03 profile command sends mutationId
04 profile replay same receipt
05 withdrawal keeps client_request_id
06 withdrawal replay same receipt
07 chat send mutationId stable
08 chat duplicate no second effect
09 chat changed payload same mutation conflicts
10 chat read receipted
11 notification read receipted
12 token register classified
13 token revoke classified
14 cancel withdrawal classified
15 receipt includes mutationId
16 receipt includes commandType
17 receipt includes resultStatus
18 receipt includes serverEntityId contract
19 receipt includes serverRevision
20 revisionKind prevents cursor authority
21 server derives scope
22 request fingerprint server-side
23 same mutation same hash replay
24 same mutation changed hash conflict
25 concurrent duplicate one effect
26 timeout before commit safe
27 timeout after commit safe
28 invalid receipt mutation rejected
29 invalid receipt entity rejected
30 401 typed AUTH
31 403 typed PERMISSION
32 409 typed CONFLICT
33 422 typed VALIDATION
34 5xx typed TRANSIENT
35 unknown typed AMBIGUOUS
36 message parsing decision count zero
37 v68 atomic mutation regressions
38 v68 cross-account Outbox regressions
39 v67 generation regressions
40 push-before-pull regression
```

يفضل إضافة:

```text
41 receipt table has no direct broad grants
42 receipt retention contract present
43 secret scan pass
44 historical migrations unchanged
45 UI files unchanged
46 no Room 16 migration
47 v68 pending operations upgrade without rewrite
48 process death after server commit
```

---

# 157. Server SQL tests

إذا authoritative schema متاحة، يجب أن تكون server tests executable، لا text-only.

يفضل transaction-wrapped SQL test script.

---

# 158. Live server test requirement

Full `PASS` يتطلب server implementation/runtime proof لأن جوهر69 server-side.

Static-only لا يكفي لـfull PASS.

---

# 159. Build/runtime policy

Gradle blocker الحالي:

```text
UnknownHostException: services.gradle.org
```

إذا استمر:

```text
Android build/unit = NOT_RUN_BUILD_BOOTSTRAP_BLOCKED
```

لكن server SQL tests يمكن تشغيلها مستقلًا إذا البيئة متاحة.

---

# 160. PASS_STATIC_SERVER_VERIFIED_ANDROID_BLOCKED

يجوز verdict فرعي فقط إذا:

```text
predecessor gate satisfied
authoritative server schema present
server migrations/tests pass
Android static/model pass
Gradle blocked environmental
no correctness ambiguity
```

---

# 161. IMPLEMENTED_STATIC_BUT_PREDECESSOR_BLOCKED

إذا نفذت69 بOverride بينما v67/v68 chain blocked:

```text
finalVerdict = IMPLEMENTED_STATIC_BUT_PREDECESSOR_BLOCKED
handoff70Authorized = false
```

حتى غلق chain أو policy صريحة.

---

# 162. BLOCKED_SERVER_COMMAND_CONTRACT

إذا لم يوجد authoritative server schema يكفي لتطبيق typed RPCs بأمان:

```text
finalVerdict = BLOCKED_SERVER_COMMAND_CONTRACT
```

ولا تخترع SQL.

---

# 163. Acceptance counters — يجب أن تساوي صفر

```text
inputDriftCount
predecessorBypassWithoutAuthorization
messageParsingDecisionCount
sameMutationDuplicateEffectCount
mutationReuseAcceptedWithChangedPayloadCount
crossScopeReceiptLeakCount
invalidReceiptFinalizeCount
receiptRevisionCursorContaminationCount
profileDirectCommandAuthorityCount
chatSendDirectCommandAuthorityCount
chatReadDirectCommandAuthorityCount
notificationReadDirectCommandAuthorityCount
unclassifiedFinancialMutationCount
historicalMigrationMutationCount
newRoomMigrationCount
productionUiFilesChanged
unrelatedProductionMutationCount
newSensitiveLogViolationCount
newV69WaiverCount
```

---

# 164. Acceptance values — يجب أن تكون true/positive

```text
Room = 15
commandReceiptContractPresent = true
serverScopeDerivedFromAuth = true
stableMutationIdentity = true
serverRequestFingerprint = true
sameMutationReplay = true
mutationReuseConflict = true
serverEffectAndReceiptAtomic = true
timeoutAfterCommitRecoverable = true
typedRetryTaxonomy = true
ambiguousOutcomeReconcile = true
profileIdempotent = true
withdrawalIdempotent = true
chatSendIdempotent = true
chatReadIdempotent = true
notificationReadIdempotent = true
tokenCommandsClassified = true
v68AtomicityRegression = true
v68ScopeIsolationRegression = true
v67GenerationRegression = true
```

---

# 165. Failure codes

الحد الأدنى:

```text
BLOCKED_PREDECESSOR_HANDOFF
BLOCKED_SERVER_COMMAND_CONTRACT
BLOCKED_INPUT_DRIFT
BLOCKED_SCOPE_DRIFT
BLOCKED_PRIVILEGE_ESCALATION
BLOCKED_RECEIPT_RETENTION_CONTRACT
FAIL_COMMAND_RECEIPT_MISSING
FAIL_SERVER_SCOPE_TRUSTS_CLIENT
FAIL_MUTATION_REPLAY_DUPLICATES_EFFECT
FAIL_MUTATION_REUSE_CONFLICT_MISSING
FAIL_PROFILE_NOT_IDEMPOTENT
FAIL_WITHDRAWAL_NOT_IDEMPOTENT
FAIL_CHAT_SEND_NOT_IDEMPOTENT
FAIL_READ_RECEIPT_NOT_IDEMPOTENT
FAIL_TYPED_RETRY_MISSING
FAIL_AMBIGUOUS_OUTCOME_RETRY_UNSAFE
FAIL_INVALID_RECEIPT_FINALIZED
FAIL_RECEIPT_REVISION_USED_AS_CURSOR
FAIL_V68_ATOMICITY_REGRESSION
FAIL_V68_SCOPE_REGRESSION
FAIL_V67_GENERATION_REGRESSION
PASS_STATIC_SERVER_VERIFIED_ANDROID_BLOCKED
PASS
```

---

# 166. PASS الكامل

`PASS` يتطلب:

```text
predecessorGateSatisfied = true
authoritativeServerSchemaProvided = true
server receipt infrastructure deployed/verified
all in-scope commands use stable mutationId
all duplicate/replay tests pass
timeout-after-commit test passes
typed retry taxonomy passes
no message parsing authority
server RLS/ownership tests pass
Android compile/tests run and pass
v67/v68 regressions pass
newV69WaiverCount = 0
```

---

# 167. Handoff إلى70

بحسب الخطة المضغوطة، 70 تملك:

```text
Durable Inbox
Atomic Apply
Realtime direct-write removal / hint-only transition
```

`handoff70Authorized=true` فقط إذا:

```text
idempotent command protocol complete
typed retry complete
receipt replay proven
no unresolved command ambiguity
v68 safety preserved
predecessor chain valid
zero new waivers
```

---

# 168. ما يجب أن تستلمه70

```text
AutoDrive-v69-idempotent-command-contract.zip
SESSION_69_FINAL.md
AUTODRIVE_SYNC_VERIFICATION_v69.json
AUTODRIVE_SYNC_VERIFICATION_v69.md
AUTODRIVE_SERVER_COMMAND_INVENTORY_v69.json
new append-only server migration(s)
server command tests
static/model verifier(s)
```

---

# 169. ما لا تعيده70

إذا69 PASS:

```text
server mutation identity
command receipt ledger
duplicate replay semantics
timeout-after-commit reconciliation
typed retry taxonomy
```

تصبح foundation ثابتة.

---

# 170. Implementation order — إلزامي

```text
1. Verify v68 ZIP SHA and extract clean baseline.
2. Read v68 JSON/MD and enforce predecessor gate.
3. Confirm Room 15.
4. Freeze critical fingerprints.
5. Scan every mutating server call-site.
6. Produce command inventory and classifications.
7. Obtain authoritative current server schema/introspection.
8. Verify existing request_withdrawal and RLS/function signatures.
9. Design canonical receipt ledger + retention + grants.
10. Design typed command response.
11. Add append-only server migration.
12. Add receipt replay/lookup mechanics.
13. Convert Profile server write.
14. Upgrade Withdrawal to canonical receipt.
15. Convert Chat Send.
16. Convert Chat Read.
17. Convert Notification Read.
18. Convert/classify token register/revoke.
19. Convert/classify cancel_pending_withdrawals and any replayable financial mutation.
20. Add Android typed command client.
21. Expand/replace OutboxDeliveryReceipt.
22. Replace message-parsing retry classifier.
23. Add ambiguous-outcome reconciliation.
24. Preserve v68 local finalization transactions.
25. Add server SQL tests.
26. Add Android static/model fixtures.
27. Re-run v67/v68 regression verifiers.
28. Attempt Gradle build/unit tests if environment permits.
29. Run live/local server tests if environment permits.
30. Produce v69 JSON/MD + command inventory.
31. Run verifier twice for determinism.
32. Package clean v69 ZIP.
33. Fresh-extract replay.
34. Generate SHA-256 artifacts.
```

---

# 171. Pre-implementation questions — يجب إجابتها من evidence

```text
Q1  ما كل mutating server call-sites؟
Q2  أيها steady-state sync commands؟
Q3  أيها auth/bootstrap فقط؟
Q4  أيها ephemeral؟
Q5  هل request_withdrawal function definition متاحة؟
Q6  كيف يثبت server user→client→org؟
Q7  ما RLS الحالية للجداول المستهدفة؟
Q8  هل pgcrypto متاح؟
Q9  ما retention الممكنة للreceipt؟
Q10 هل chat message id client-generated PK على server؟
Q11 هل notification id globally unique أم scope-only؟
Q12 هل push_tokens unique by user_id فعلًا في schema؟
Q13 هل cancel_pending_withdrawals idempotent حاليًا؟
Q14 هل create_new_conversation قابلة لتكرار غامض؟
Q15 ما HTTP/Postgres structured fields التي يعرضها SDK الحالي؟
Q16 هل receipt revision يمكن توليدها بدون التداخل مع global revision المستقبلية؟
Q17 هل mixed v68/v69 client rollout مطلوب؟
Q18 ما كل domain error codes للwithdrawal؟
Q19 هل profile command قد يغير bank fields؟
Q20 ما أي command مالي آخر قابل لإعادة الإرسال؟
```

أي مجهول correctness-critical:

```text
BLOCK
```

لا guess.

---

# 172. Final acceptance questions

قبل packaging يجب الإجابة YES:

```text
Q1  هل source هو v68 المحدد؟
Q2  هل Room بقيت15؟
Q3  هل predecessor gate مصرح؟
Q4  هل server schema authoritative متاحة؟
Q5  هل كل server mutation مصنفة؟
Q6  هل receipt contract موجود؟
Q7  هل server يشتق scope من auth؟
Q8  هل mutationId ثابتة؟
Q9  هل request fingerprint server-side؟
Q10 هل نفس mutation يعيد نفس receipt؟
Q11 هل changed payload same mutation يرفض؟
Q12 هل effect+receipt transaction واحدة؟
Q13 هل concurrent duplicate لا يكرر effect؟
Q14 هل timeout-before-commit آمن؟
Q15 هل timeout-after-commit قابل للتسوية؟
Q16 هل Profile idempotent؟
Q17 هل Withdrawal idempotent؟
Q18 هل Chat Send idempotent؟
Q19 هل Chat Read idempotent؟
Q20 هل Notification Read idempotent؟
Q21 هل Token register/revoke مصنفان ومغطيان؟
Q22 هل cancel withdrawal/financial commands مغطاة؟
Q23 هل receipt mismatch يمنع finalize؟
Q24 هل typed retry موجود؟
Q25 هل message parsing خرج من decision authority؟
Q26 هل AUTH لا يتحول terminal عشوائيًا؟
Q27 هل AMBIGUOUS يعمل reconciliation؟
Q28 هل receipt revision لا تستخدم cursor؟
Q29 هل receipt retention موثق؟
Q30 هل RLS/grants تمنع cross-account؟
Q31 هل لا service role في Android؟
Q32 هل v68 atomicity لم تتراجع؟
Q33 هل v68 Outbox scope لم تتراجع؟
Q34 هل v67 generation لم تتراجع؟
Q35 هل push-before-pull بقي؟
Q36 هل Inbox لم تُسحب؟
Q37 هل Realtime rewrite لم تُسحب؟
Q38 هل Chat pagination/media queue لم تُسحب؟
Q39 هل global server revision لم تُسحب؟
Q40 هل historical SQL untouched؟
Q41 هل UI unchanged؟
Q42 هل newRoomMigrationCount=0؟
Q43 هل newV69WaiverCount=0؟
Q44 هل runtime claims صادقة؟
Q45 هل final ZIP fresh-extract verifier مطابق؟
```

---

# 173. Required implementation truth table

التقرير النهائي يفصل:

```text
IMPLEMENTED_ANDROID
IMPLEMENTED_SERVER
STATIC_VERIFIED
SERVER_SQL_TESTED
SERVER_LIVE_TESTED
COMPILED
UNIT_TESTED
ANDROID_RUNTIME_TESTED
PREDECESSOR_GATE_SATISFIED
AUTHORITATIVE_SERVER_SCHEMA_USED
```

لا كلمة PASS عامة تخفي NOT_RUN.

---

# 174. Packaging

اسم archive المستهدف:

```text
AutoDrive-v69-idempotent-command-contract.zip
```

ويحتوي:

```text
source tree
SESSION_69_FINAL.md
AUTODRIVE_SYNC_VERIFICATION_v69.json
AUTODRIVE_SYNC_VERIFICATION_v69.md
AUTODRIVE_SERVER_COMMAND_INVENTORY_v69.json
server migration(s)
server tests
static/model verifiers
```

---

# 175. SHA-256

ينتج:

```text
AutoDrive-v69-idempotent-command-contract.zip.sha256
SESSION_69_FINAL.md.sha256
```

---

# 176. Archive integrity

إلزامي:

```text
unzip -t
fresh extract
run v69 verifier
run inherited v68 static verifier
compare semantic output
```

---

# 177. Secret scan

افحص:

```text
service_role
JWT secret
database password
access token
refresh token
OTP
push token dump
bank account dump
raw profile payload
raw command payload
```

أي leak:

```text
BLOCKED_SECRET_LEAK
```

---

# 178. No generated junk

ممنوع:

```text
.gradle/
build/
IDE caches
local.properties real
keystores
DB dumps containing user data
temporary SQL credential files
```

---

# 179. Final architecture after69

```text
Room local mutation + scoped Outbox
        ↓
stable mutationId
        ↓
typed command RPC
        ↓
server derives principal scope
        ↓
unique receipt claim + request fingerprint
        ↓
business effect + receipt atomically
        ↓
canonical receipt/replay
        ↓
typed Android classification
        ↓
local atomic ack + Outbox finalize
```

---

# 180. What 69 changes conceptually

قبل69:

```text
local mutation identity exists
server duplicate semantics vary by command
success often inferred from no exception
retry classification parses text
timeout-after-commit only Withdrawal has targeted reconciliation
```

بعد69:

```text
one server command identity model
one receipt model
one replay rule
one typed retry model
ambiguous outcome reconciled before retry
same mutation never creates a second logical effect
```

---

# 181. أهم invariant

```text
If the server can commit an in-scope command
without leaving a replayable canonical receipt for its mutationId,
Session 69 is not complete.
```

---

# 182. invariant الثاني

```text
If Android can classify retry/permanent/conflict
only by reading human-facing exception text,
Session 69 is not complete.
```

---

# 183. invariant الثالث

```text
If a duplicate retry can produce a second financial/chat/profile effect,
Session 69 is not complete.
```

---

# 184. invariant الرابع

```text
If receipt ownership can be spoofed by userId/clientId/orgId sent by Android,
Session 69 is not complete.
```

---

# 185. حالة العقد الحالية

بناء على v68 المفحوص:

```text
CONTRACT_READY = true
v68 handoff69Authorized = false
server authoritative schema = incomplete in supplied archive
```

إذًا:

```text
EXECUTION_CHAIN_GATE = BLOCKED_BY_PREDECESSOR
FULL_SERVER_IMPLEMENTATION_GATE = BLOCKED_UNTIL_AUTHORITATIVE_SCHEMA_EVIDENCE
```

ولا يمنع ذلك تجهيز Android-side contract/static work تحت override، لكنه يمنع Full PASS.

---

# 186. الخلاصة النهائية للعقد

69 لا تنجح بمجرد تمرير `mutationId` إلى RPC.

تنجح فقط إذا أصبح البروتوكول:

```text
Stable mutation identity
+ server-derived ownership
+ request fingerprint
+ atomic effect/receipt
+ deterministic replay
+ timeout-after-commit reconciliation
+ typed retry taxonomy
```

على الأقل لـ:

```text
Profile
Withdrawal
Chat Send
Chat Read
Notification Read
Push Token register/revoke
أي financial replayable command مثبت في inventory
```

مع الحفاظ على:

```text
Room 15
v68 scoped transactional Outbox
v68 lease/retry separation
v68 logout isolation
v67 push-before-pull
v67 generation safety
zero historical SQL mutation
zero UI drift
zero new waivers
```

والقاعدة النهائية:

```text
Same mutation must be safe to send again after the client has forgotten whether the previous response arrived.
```

---


---

# 187. Execution Outcome — 2026-08-21

تم تنفيذ Session 69 بــ user execution override فوق predecessor chain المحجوبة، مع الالتزام بعدم تحويلها إلى PASS release.

```text
source SHA-256                    = 8b6f148923900208fa1386a4c68d7f05375b4bb21dfa3e1c67091d643e8682b5
authoritative server schema      = PROVIDED externally: schema.sql modified 2026-08-20 20:45:56Z
Room                              = 15
new Room migrations               = 0
new server migrations             = 1 append-only
production UI drift               = 0
historical migration mutations    = 0
new v69 waivers                   = 0
v69 static verifier               = 93/93 PASS, deterministic twice
v69 model verifier                = 15/15 PASS
v67 inherited model               = 22/22 PASS
v68 inherited model               = 36/36 PASS
v68 migration model               = PASS
Gradle compile                    = BLOCKED_ENVIRONMENT_NETWORK_BOOTSTRAP
Gradle blocker                    = UnknownHostException: services.gradle.org
server SQL runtime                = NOT RUN — psql/supabase/docker unavailable
server deployment                 = NOT DEPLOYED by this session
predecessorGateSatisfied          = false
handoff70Authorized               = false
finalVerdict                      = IMPLEMENTED_STATIC_BUT_PREDECESSOR_BLOCKED
```

Implemented source scope:

```text
canonical scoped command receipt ledger
server-derived auth.uid() → user/client/org scope
server SHA-256 request fingerprints
atomic effect + receipt typed RPCs
Profile / Withdrawal / Chat Send / Chat Read / Notification Read
Push token Register / Revoke
Cancel Pending Withdrawals
canonical receipt lookup/replay
Android typed receipt validation
typed Outbox retry taxonomy
ambiguous outcome preserves same mutation for safe replay
zero human-readable message parsing in Outbox retry decisions
```

Inherited v67 tombstone adapter remains deliberately blocked; Session 69 did not fake predecessor completion.

# END OF SESSION_69_FINAL.md
