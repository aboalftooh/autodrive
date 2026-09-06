# SESSION_68_FINAL.md

## AutoDrive Sync Modernization — Session 68

### Atomic Local Mutation + Scoped Transactional Outbox — Profile, Withdrawal, Chat Send, Read Receipts & Logout-Safe Ownership

**نوع المستند:** عقد تنفيذ مستقل وصارم للجلسة الثانية من مسار تحديث مزامنة AutoDrive  
**الجلسة:** 68  
**الحالة:** `PLAN ONLY — READY AS CONTRACT; EXECUTION GATED BY SESSION 67 HANDOFF`  
**تاريخ الصياغة:** 2026-08-21  
**مصدر الكود المفحوص:** `AutoDrive-v67-sync-safety-foundation.zip`  
**SHA-256 للمصدر المفحوص:** `de7ea3186147eb7fc0dedcb8f45397d62a6349d4e5fdcff6320123cca25c3e71`  
**Archive entries:** `1268`  
**Production Kotlin files:** `258`  
**Test Kotlin files:** `45`  
**Room الحالي:** `14`  
**Room المستهدف في 68:** `15`  
**مرجع الجلسة السابقة:** `SESSION_67_FINAL.md`  
**SHA-256 لمرجع الجلسة السابقة:** `0f67bc09d36244b0447dffbb6f75a5871db1640580db35dbf0cd9837c865e226`  
**تقرير تحقق v67:** `AUTODRIVE_SYNC_VERIFICATION_v67.json/.md`  
**v67 final verdict الحالي:** `BLOCKED_SERVER_TOMBSTONE_CONTRACT`  
**v67 handoff68Authorized الحالي:** `false`  
**v67 static/model:** `22/22 PASS`  
**v67 build/runtime:** `BLOCKED_ENVIRONMENT_NETWORK_BOOTSTRAP — UnknownHostException: services.gradle.org`  
**الخطة المضغوطة المشار إليها في 67:** `AUTODRIVE_SYNC_MODERNIZATION_REPAIR_PLAN_v67-v73.md` — غير موجودة ضمن المدخلات الحالية؛ المتاح منها فقط الاسم وSHA داخل عقد 67.  
**مرجع scope إضافي متاح:** `AUTODRIVE_SYNC_MODERNIZATION_REPAIR_PLAN_v67-v80.md` — يُستخدم فقط حيث يطابق handoff الصريح من 67 والكود الفعلي.

---

# 0. الحكم التنفيذي المختصر

Session 68 ليست إعادة تسمية لـOutbox الحالية.

هي جلسة إغلاق race مثبتة في v67:

```text
Local Room mutation
        ↓ crash window
Outbox enqueue / direct network attempt
```

في v67 يمكن أن توجد نية محلية دون durable operation مكافئة بسبب اختلاف توقيت الكتابتين.

الحالة المستهدفة:

```text
Resolve SyncScope
      ↓
Create immutable mutation identity
      ↓
Room.withTransaction {
    mutate local entity/state
    insert exact scoped Outbox operation
}
      ↓
commit
      ↓
optional immediate send / normal Sync / WorkManager
      ↓
network outside Room transaction
      ↓
Room.withTransaction {
    revalidate scope
    apply acknowledgement/reconciliation
    finalize/remove exact Outbox operation
}
```

القواعد المطلقة:

```text
No syncable local pending mutation without a matching durable scoped Outbox row.
```

```text
No Outbox row without userId + clientId + orgId + entityType + entityId + mutationId.
```

```text
Local entity mutation + Outbox enqueue must commit or roll back together.
```

```text
A worker/sync run may claim and send only operations owned by its captured SyncScope.
```

```text
nextRetryAt and leaseUntil are different concepts and must not share one column after 68.
```

```text
Logout must not leave a sendable operation belonging to the account being removed.
```

```text
Session 68 must not silently repair Session 67's unresolved server tombstone contract.
```

---

# 1. بوابة البداية — Session 67 Handoff Gate

قبل أي mutation في 68 يجب قراءة:

```text
AUTODRIVE_SYNC_VERIFICATION_v67.json
```

والتحقق من:

```text
finalVerdict
handoff68Authorized
serverTombstoneContractVerified
serverCursorSemanticsVerified
staticGatesPassed
newV67WaiverCount
```

الحالة الحالية في المصدر المفحوص:

```text
finalVerdict                         = BLOCKED_SERVER_TOMBSTONE_CONTRACT
handoff68Authorized                  = false
serverTombstoneContractVerified      = false
serverCursorSemanticsVerified        = false
staticGatesPassed                    = true
newV67WaiverCount                    = 0
```

إذًا الحكم الحالي:

```text
SESSION_68_EXECUTION_GATE = BLOCKED_BY_V67_HANDOFF
```

يجوز استخدام هذا العقد للتحضير والمراجعة فقط.

لا يجوز إعلان تنفيذ 68 `PASS` أو `PASS_STATIC_RUNTIME_BLOCKED` على هذا baseline ما لم يحدث أحد التالي:

```text
A) تُغلق 67 رسميًا ويصبح handoff68Authorized=true، أو
B) يصدر المستخدم Override صريح يقبل البناء فوق 67 المحجوبة، ويُسجل كـ inherited risk لا كـ waiver خفية.
```

بدون A أو B:

```text
BLOCKED_PREDECESSOR_HANDOFF
```

---

# 2. Baseline Gate — هوية v67

قبل أي تعديل يجب تثبيت:

```text
ZIP SHA-256                   = de7ea3186147eb7fc0dedcb8f45397d62a6349d4e5fdcff6320123cca25c3e71
archive entries               = 1268
production Kotlin             = 258
test Kotlin                   = 45
Room version                  = 14
v67 model fixtures            = 22/22 PASS
v67 production UI changed     = 0
v67 unexpected prod mutations = 0
v67 new waivers               = 0
```

إذا اختلف ZIP أو Room baseline دون توثيق:

```text
BLOCKED_INPUT_DRIFT
BLOCKED_ROOM_BASELINE_DRIFT
```

إذا كانت Outbox قد عُدلت بعد هذا المصدر قبل بدء 68:

```text
BLOCKED_OUTBOX_BASELINE_DRIFT
```

---

# 3. ترتيب السلطات — Authority Order

عند التعارض:

1. `AutoDrive-v67-sync-safety-foundation.zip` بالـSHA أعلاه.
2. `AUTODRIVE_SYNC_VERIFICATION_v67.json/.md` لحقيقة ما نُفذ وما بقي محجوبًا.
3. `SESSION_67_FINAL.md`، خصوصًا handoff إلى 68 وحدود 68/69/70+.
4. `AUTODRIVE_SYNC_MODERNIZATION_REPAIR_PLAN_v67-v73.md` **إذا قُدمت لاحقًا بنفس SHA المثبت في 67**.
5. `AUTODRIVE_SYNC_MODERNIZATION_REPAIR_PLAN_v67-v80.md` فقط لنطاق Session 68 الأصلي حيث لا يتعارض مع 67 المضغوطة.
6. كود v67 الفعلي في `core/database`, `core/sync`, `feature/profile`, `feature/balance`, `feature/chat`, `feature/notifications`, `feature/auth`.
7. هذا العقد.

ممنوع استيراد semantics من Verto أو Optimal إلى AutoDrive.

---

# 4. بصمات Authority عند البداية

يجب تسجيل هذه fingerprints قبل mutation:

```text
AutoDrive-v67-sync-safety-foundation.zip
  de7ea3186147eb7fc0dedcb8f45397d62a6349d4e5fdcff6320123cca25c3e71

SESSION_67_FINAL.md
  0f67bc09d36244b0447dffbb6f75a5871db1640580db35dbf0cd9837c865e226

AUTODRIVE_SYNC_VERIFICATION_v67.json
  eb78ee08b57b03a18446c61ec26e2b48edac5e0860be43421d067d0916661f6e

AUTODRIVE_SYNC_VERIFICATION_v67.md
  29c7d20535c2d3e83dd2f7ff24c998b00541787fd6467555023b10082cfb937f

core/database/.../Entities.kt
  e82143d146997543ee720ec8f04aa36780412793069a396e3b67efba17e9f7d0

core/database/.../PendingOperationDao.kt
  013e97f871c6ed4f89571c83b892a7005a6a3d587bb00c0bfaa1e1682d23bd8b

core/database/.../AutoDriveDatabase.kt
  6dcc06dbdeb143dc63dfa32f8b5b786af50ad90a2b9c44fd3095a7e8999de703

core/sync/.../OutboxSynchronizer.kt
  0277eda6039954d9f02bb76957354d7237ba7a4c915d18344d8f03f70831d8b3

core/sync/.../PendingOperationProcessor.kt
  a58513bd73403ed792a7110e7027f26b2c31389b726b79804d2e18551993d703

core/sync/.../SyncScope.kt
  aa3eaf0f58334dc114e9befab373b54c0761ae51200444e233a18eec28202ebd

core/sync/.../DefaultSyncCoordinator.kt
  c153691154ce5662524ffe0981d81f3b5d27d5aa20e146a8d7996a22ee849f5b

core/sync/.../LocalDataCleaner.kt
  cd8d110d4b453ac8dd7f8d84fe1cb31eb079caf51cfeb6475611e015dec868ac

feature/profile/.../ProfileRepositoryImpl.kt
  762fe6ab43fa59df544fcc9acc476cdb33bb3f22f0271aa18e98fe9f01f8ed97

feature/balance/.../BalanceRepositoryImpl.kt
  0fa3e5002ed1f519158e4dbb18bd353e87e7fb07e1ab773bd90ddeb4e0e03345

feature/chat/.../ChatRepositoryImpl.kt
  8d429975c0940d3e0b1d9c4cfcd2542907cc0c6046140507e7cac761c8393db1

feature/notifications/.../NotificationRepositoryImpl.kt
  71302e7f59b87ee9792a9e4bcb491fba12e6af9136d2b0538d73e38bb4372c98

feature/auth/.../AuthRepositoryImpl.kt
  2abc26249f07c46beae2458cc0d3bab9c776e473638e30334ee2ccfad35dd0fe

core/session/.../CurrentSession.kt
  35925f92637ba39678a526f3d8e3d2e2e84fcc38f694df1a22d2fe05f6a95900

core/session/.../PreferencesManager.kt
  e3da7bed72b54a9f97f0cdd0e7f6dc63324ae1d1cdc7141847bac9e8caa541de
```

هذه fingerprints prestate evidence وليست منعًا لتعديل الملفات داخل scope.

---

# 5. حقيقة v67 — Outbox schema الحالية

`PendingOperationEntity` في v67 تحمل:

```text
id
tableName
operation
payload
createdAt
status
attemptCount
nextRetryAt
lastErrorCode
lastErrorMessage
payloadVersion
idempotencyKey
```

ولا تحمل:

```text
userId
clientId
orgId
mutationId
entityType
entityId
leaseUntil مستقل
contractVersion باسم موحد
```

الحكم:

```text
OUTBOX_OWNER_SCOPE = ABSENT
OUTBOX_ENTITY_IDENTITY = PARTIAL/IMPLICIT
OUTBOX_MUTATION_IDENTITY = LEGACY_MIXED
LEASE_AND_RETRY_TIME = OVERLOADED
```

---

# 6. حقيقة v67 — DAO queries غير scoped

`PendingOperationDao` الحالي ينفذ:

```text
getDue(now, limit)
claim(id, now, leaseUntil)
releaseExpiredClaims(now)
releaseClaim(id, nextRetryAt)
countByStatus(status)
deleteAll()
```

بدون `userId/clientId/orgId`.

هذا يعني أن worker أو sync للحساب B يمكن نظريًا أن يلتقط row قديمة للحساب A إذا بقيت محليًا.

الحكم:

```text
GLOBAL_OUTBOX_QUERY_SURFACE = PRESENT
```

يجب أن تصبح صفرًا بعد 68 لمسار الإنتاج.

---

# 7. حقيقة v67 — Profile write race

`ProfileRepositoryImpl.updateUser()` يفعل:

```text
1. Room upsert profile with syncStatus=PENDING
2. Encrypted prefs update
3. direct server UPDATE
4. if server fails -> insert PendingOperationEntity
```

لا توجد Room transaction تجمع 1 و4.

Crash بعد 1 وقبل 4 يمكن أن يترك:

```text
local profile = PENDING
matching Outbox = absent
```

الحكم:

```text
PROFILE_ATOMIC_OUTBOX = ABSENT
```

---

# 8. حقيقة v67 — Withdrawal write race

`BalanceRepositoryImpl.requestWithdrawal()` يفعل:

```text
1. create clientRequestId
2. upsert optimistic WithdrawalRequestEntity(PENDING_SYNC)
3. call request_withdrawal RPC
4. only on ambiguous failure -> insert Outbox
```

Crash بين 2 و4 يترك intent محلي بلا Outbox.

الحكم:

```text
WITHDRAWAL_ATOMIC_OUTBOX = ABSENT
```

---

# 9. حقيقة v67 — Chat send

`ChatRepositoryImpl.sendMessage()`:

```text
prepare media
insert ChatMessageEntity(status=PENDING)
update conversation preview
insert remote internal_messages directly
on failure -> local status=FAILED
```

لا توجد `PendingOperationEntity` للرسالة.

الحكم:

```text
CHAT_SEND_OUTBOX = ABSENT
```

---

# 10. حقيقة v67 — Chat read receipt

`markMessagesAsRead()`:

```text
mark local admin messages read
reset conversation unread count
best-effort remote UPDATE internal_messages
```

فشل remote لا ينشئ durable Outbox.

الحكم:

```text
CHAT_READ_RECEIPT_OUTBOX = ABSENT
```

---

# 11. حقيقة v67 — Notification read

`NotificationRepositoryImpl` يستخدم:

```text
read_synced=false
```

كـlocal pending marker، ثم يحاول server مباشرة.

`OutboxSynchronizer.flush()` يملك loop منفصلًا:

```text
notificationDao.getUnsynced(userId)
→ remote update
→ confirmReadSynced
```

هذا ليس `pending_operations` الموحد.

الحكم:

```text
NOTIFICATION_READ = DURABLE_IN_ENTITY_MARKER
BUT NOT UNIFIED_OUTBOX
```

68 يجب أن توحده دون كسر `read_synced` كcompatibility marker.

---

# 12. حقيقة v67 — lease timing

في v67:

```text
claim() sets next_retry_at = leaseUntil
releaseExpiredClaims() compares next_retry_at to now
```

إذًا `next_retry_at` تستخدم كـ:

```text
retry schedule
AND
claim lease expiry
```

بعد 68:

```text
nextRetryAt != leaseUntil
```

دلاليًا وفي schema.

---

# 13. حقيقة v67 — Logout

`AuthRepositoryImpl.signOut()` الحالي:

```text
push token delete attempt
realtime stop
syncManager.clearLocalData()
supabase auth signOut
sessionWriter.clearSession()
```

`LocalDataCleaner.clearCurrentAccount()`:

```text
deletes account data through many DAO calls
pendingOperationDao.deleteAll()
```

ولا يستخدم `Room.withTransaction`.

الحكم:

```text
OUTBOX_LOGOUT_CLEANUP = GLOBAL
ACCOUNT_DATA_CLEANUP_ATOMICITY = ABSENT
ACTIVE_SYNC_QUIESCENCE_BEFORE_CLEANUP = NOT PROVEN
```

---

# 14. الهدف الدقيق لـ68

عند نهاية 68 يجب أن يثبت الكود:

```text
1. PendingOperationEntity canonical ownership fields exist.
2. Room 14→15 safely migrates Outbox.
3. Every targeted local syncable mutation commits Entity+Outbox atomically.
4. Profile uses transactional Outbox.
5. Withdrawal uses transactional Outbox.
6. Chat send uses transactional Outbox envelope.
7. Chat read receipt uses transactional Outbox.
8. Notification read uses the same Outbox infrastructure.
9. Outbox claims/queries are exact-scope only.
10. retry schedule and lease expiry are separate.
11. network I/O never occurs inside Room transaction.
12. acknowledgement/finalization cannot silently detach local state from its operation.
13. logout removes/quiesces the departing scope safely.
14. worker cannot replay another account's operation.
15. v67 cursor/generation/push-before-pull foundation is not regressed.
```

---

# 15. ما ليست عليه 68

ممنوع سحب بقية الخطة إلى هذه الجلسة.

ليست 68:

```text
- Server-wide idempotent command receipt protocol.       -> later session
- Generic server mutation receipt with serverRevision.   -> later session
- Durable Inbox.                                         -> later session
- Realtime hint-only full rewrite.                        -> later session
- Chat 10k pull pagination.                               -> later session
- Durable media transfer queue.                           -> later session
- Unified server revision/change feed.                   -> later session
- Safe bootstrap/CURSOR_EXPIRED recovery.                 -> later session
- Anti-entropy manifests/digests.                         -> later session
- Final dead-letter recovery UX.                          -> later session
- Final observability/fault-injection suite.              -> later session
```

أي تنفيذ واسع لهذه البنود:

```text
BLOCKED_SCOPE_DRIFT
```

---

# 16. Runtime policy

بسبب baseline v67:

```text
Gradle 8.7 bootstrap = BLOCKED by services.gradle.org DNS/network
```

68 تميز بين:

```text
68-S = source/static/model/database-contract proof
68-R = Gradle/unit/instrumentation/runtime proof
```

يجوز:

```text
PASS_STATIC_RUNTIME_BLOCKED
```

فقط إذا:

```text
- predecessor handoff 67 أصبح مصرحًا أو يوجد override صريح.
- كل static/model gates ناجحة.
- migration 14→15 structurally verified.
- no new v68 waiver.
- التقرير لا يدعي runtime لم يُنفذ.
```

---

# 17. Room schema policy

68 تتطلب Outbox owner/entity/lease fields، لذلك:

```text
current Room = 14
target Room  = 15
```

ممنوع:

```text
fallbackToDestructiveMigration()
DROP user data as shortcut
schema jump > 15
historical migration edits
unrelated table redesign
```

---

# 18. Canonical Outbox schema — الحد الأدنى

`pending_operations` بعد 68 يجب أن يحمل دلاليًا:

```text
operation_id       TEXT NOT NULL PRIMARY KEY
mutation_id        TEXT NOT NULL
user_id            TEXT NOT NULL
client_id          TEXT NOT NULL
org_id             TEXT NOT NULL
entity_type        TEXT NOT NULL
entity_id          TEXT NOT NULL
operation_type     TEXT NOT NULL
payload            TEXT NOT NULL
contract_version   INTEGER NOT NULL
created_at         INTEGER NOT NULL
status             TEXT NOT NULL
attempt_count      INTEGER NOT NULL
next_retry_at      INTEGER NOT NULL
lease_until        INTEGER NOT NULL
last_error_code    TEXT
last_error_message TEXT
```

يجوز إبقاء اسم Kotlin مختلفًا إذا mapping Room واضح، لكن semantics أعلاه إلزامية.

---

# 19. operationId contract

`operationId` هو هوية row المحلية في Outbox.

يجب أن يكون:

```text
stable for the stored row
nonblank
unique primary key
not regenerated on claim/retry
```

ممنوع استخدامه كـserver ordering cursor.

---

# 20. mutationId contract

`mutationId` يمثل العملية المنطقية التي تعاد محاولتها.

القواعد:

```text
immutable across retries of the same logical mutation
nonblank
scoped
not wall-clock based
not replaced merely because lease expired
```

Session 68 لا تفترض أن كل server command يملك receipt كامل؛ ذلك مؤجل.

لكن Android يجب أن يملك mutation identity صحيحة من الآن.

---

# 21. Scope fields contract

كل Outbox row يجب أن تحمل:

```text
userId
clientId
orgId
```

وتساوي `SyncScope` لحظة إنشاء mutation.

لا يجوز nullable scope للعمليات الجديدة.

إذا `SyncScope.from(currentSession)` يرجع null:

```text
LOCAL_MUTATION_SCOPE_MISSING
```

ولا يجوز إنشاء syncable local pending state بلا Outbox.

---

# 22. entityType contract

`entityType` هو canonical logical/server entity identity.

أمثلة v68 المتوقعة:

```text
autodrive_users
withdrawal_requests
internal_messages
notifications
```

يجوز registry/type-safe constants بدل strings المتناثرة.

ممنوع dynamic table execution من payload.

---

# 23. entityId contract

`entityId` يجب أن يشير إلى target المحلي/المنطقي للعملية.

الحد الأدنى:

```text
Profile           -> userId
Withdrawal        -> clientRequestId/local pending id
Chat send         -> messageId
Chat read receipt -> conversationId
Notification read -> notificationId
```

bulk notification read يجب ألا تستخدم `*` إذا كان يمكن تمثيل كل notification كعملية محددة.

---

# 24. operationType contract

الأنواع الإنتاجية المطلوبة في 68 يجب أن تكون explicit allowlist.

مثال مسموح:

```text
UPDATE_PROFILE
REQUEST_WITHDRAWAL_RPC
SEND_CHAT_MESSAGE
MARK_CHAT_READ
MARK_NOTIFICATION_READ
```

الأسماء قابلة للتغيير، لكن generic dynamic operation executor ممنوع.

---

# 25. contractVersion

`contractVersion` هو إصدار payload/operation contract المحلي.

يبدأ للعمليات الجديدة بـ:

```text
1
```

إذا كان payload الحالي متوافقًا مع `payloadVersion=1` يمكن migration mapping مباشر.

لا تنشئ version 2 بلا تغيير عقد فعلي.

---

# 26. createdAt

`createdAt` يحدد local queue ordering/diagnostics فقط.

يجوز `System.currentTimeMillis()` هنا.

ممنوع استخدامه كـ:

```text
server cursor
server revision
proof of commit order across devices
```

---

# 27. attemptCount

يحافظ على معنى v67:

```text
number of failed delivery attempts already recorded
```

لا يزيد عند claim فقط.

لا يُعاد إلى صفر عند lease expiry.

---

# 28. nextRetryAt

بعد 68:

```text
nextRetryAt = earliest local time operation may be retried
```

ولا يحمل lease expiry.

---

# 29. leaseUntil

`leaseUntil` حقل مستقل.

المعنى:

```text
0 / null-equivalent => not currently leased
> now              => claimed by an active processor
<= now             => expired claim eligible for recovery
```

يفضل `Long NOT NULL DEFAULT 0` لتبسيط migration.

---

# 30. lastError fields

يجوز الإبقاء على:

```text
lastErrorCode
lastErrorMessage
```

مع redaction الحالية.

68 لا تبني typed retry taxonomy النهائية.

لكن لا يجوز حذف diagnostic data بلا بديل.

---

# 31. Indexes الإلزامية

يجب دعم الكفاءة والعزل على الأقل عبر indexes equivalent لـ:

```text
(user_id, client_id, org_id, status, next_retry_at, created_at)
(user_id, client_id, org_id, entity_type, entity_id, status)
(user_id, client_id, org_id, mutation_id)
```

يفضل unique index على:

```text
(user_id, client_id, org_id, mutation_id)
```

إذا implementation يسمح أكثر من row لنفس mutation، يجب إثبات لماذا لا يسبب duplicate delivery.

---

# 32. No global Outbox authority

بعد 68 production code ممنوع أن يعتمد على:

```text
getDue(now, limit)
countByStatus(status)
releaseExpiredClaims(now)
deleteAll()
```

بدون scope.

كل production queue query يجب أن تستقبل exact `SyncScope` أو عناصره الثلاثة.

---

# 33. Scoped due query

المطلوب دلاليًا:

```text
getDue(scope, now, limit)
```

والـSQL يجب أن يحتوي:

```text
user_id   = scope.userId
client_id = scope.clientId
org_id    = scope.orgId
status    = 'PENDING'
next_retry_at <= now
```

---

# 34. Scoped claim

claim يجب أن يفشل إذا scope لا تطابق row حتى لو `operationId` صحيح.

المطلوب:

```text
UPDATE ...
WHERE operation_id = :id
  AND user_id = :userId
  AND client_id = :clientId
  AND org_id = :orgId
  AND status='PENDING'
```

PASS فقط إذا affected rows = 1.

---

# 35. Scoped expired-lease recovery

`releaseExpiredClaims` يجب أن تعمل للحساب الحالي فقط.

ممنوع أن sync للحساب B يعيد PENDING lease للحساب A.

---

# 36. Scoped status counts

Diagnostics في `OutboxSynchronizer` يجب أن تعد:

```text
pending/in_progress/dead_letter
```

داخل scope الحالية فقط.

لا global count في sync result.

---

# 37. Scoped delete

يجب إضافة:

```text
deleteForScope(userId, clientId, orgId)
```

لـlogout/cleanup.

`deleteAll()` يمكن أن يبقى test/admin-only إن كان لا بد، لكن:

```text
production call site count = 0
```

---

# 38. Migration 14→15 — السياسة

لأن `pending_operations` تتغير جوهريًا، يفضل rebuild table داخل migration واحدة:

```text
pending_operations_v15
copy/transform
validate
DROP old
RENAME new
create indexes
```

ممنوع تعديل `MIGRATION_10_11`, `12_13`, أو `13_14`.

---

# 39. Legacy row mapping

v67 لا يُنتج في `pending_operations` سوى paths معروفة أساسًا:

```text
UPDATE_PROFILE
REQUEST_WITHDRAWAL_RPC
```

المmigration يجب أن تحفظ rows القديمة ولا تحولها إلى عمليات بلا owner.

---

# 40. Legacy UPDATE_PROFILE mapping

للـlegacy profile operation:

```text
operation_id     = old.id
mutation_id      = stable legacy mutation identity; يفضل old.id لا profile:<user>
entity_type      = autodrive_users
operation_type   = UPDATE_PROFILE
entity_id        = userId المستخرج/المثبت
contract_version = old.payload_version
```

`userId` يمكن إثباته من legacy `idempotency_key=profile:<userId>` مع تطابق `autodrive_users.user_id`.

`clientId/orgId` يجب أن يأتيا من `autodrive_users` لنفس المستخدم، لا من session preferences داخل migration.

# 41. Legacy REQUEST_WITHDRAWAL_RPC mapping

للعملية القديمة:

```text
operation_id     = old.id
mutation_id      = old.idempotency_key = client_request_id
entity_type      = withdrawal_requests
entity_id        = client_request_id/local pending id
operation_type   = REQUEST_WITHDRAWAL_RPC
contract_version = old.payload_version
```

`userId/clientId` يجب إثباتهما من `withdrawal_requests` المحلية المطابقة.

`orgId` يجب إثباته من `autodrive_users` لنفس user/client.

---

# 42. Legacy lease migration

v67 تستعمل `next_retry_at` كـlease عندما status=`IN_PROGRESS`.

لذلك migration 14→15 يجب أن تفصل المعنى:

```text
if old.status == IN_PROGRESS:
    lease_until  = old.next_retry_at
    next_retry_at = 0 أو قيمة retry صحيحة موثقة
else:
    lease_until  = 0
    next_retry_at = old.next_retry_at
```

ممنوع نسخ نفس timestamp إلى الحقلين بلا تفسير.

---

# 43. Legacy unresolved scope — fail closed

إذا row قديمة لا يمكن إثبات:

```text
userId/clientId/orgId/entityId/mutationId
```

فالممنوع:

```text
assign current session blindly
assign empty scope
assign wildcard scope
drop row silently
send it under whichever account logs in next
```

الخيارات المقبولة:

```text
A) abort migration transaction with explicit MIGRATION_UNSCOPED_OUTBOX_ROW, preserving old DB unchanged,
or
B) quarantine through an explicitly modeled non-sendable migration surface if implementation can prove no scope drift.
```

الأفضل في 68: A، لأن baseline v67 معروف النوعين فقط.

---

# 44. Migration preservation

`MIGRATION_14_15` يجب أن تحفظ:

```text
payload
status
attemptCount
retry schedule semantics
lastErrorCode
lastErrorMessage
createdAt
legacy operation identity
```

ولا تمس:

```text
sync_cursors
financial decimal storage
chat/media files
unrelated tables
```

---

# 45. Room schema 15 evidence

يجب إنتاج عند توفر Gradle:

```text
core/database/schemas/com.autodrive.app.core.database.AutoDriveDatabase/15.json
```

غيابه بسبب blocker البيئي:

```text
ROOM_SCHEMA_15_RUNTIME_EXPORT = NOT_RUN
```

ممنوع إنشاء JSON يدوي وادعاء أنه Room-generated.

---

# 46. Transactional Mutation Writer

يفضل عزل النمط في component واضح، مثل:

```text
TransactionalOutboxWriter
LocalMutationWriter
OutboxMutationStore
```

لكن الاسم غير مفروض.

المهم أن feature repositories لا تكرر schema construction بطريقة غير منضبطة.

---

# 47. Mutation creation order

لكل local mutation:

```text
1. resolve current SyncScope
2. validate command inputs
3. allocate immutable operationId/mutationId
4. build local entity state
5. build Outbox row
6. db.withTransaction { entity mutation + Outbox insert }
7. only after commit: network/sync trigger
```

ممنوع network قبل الخطوة 6 إذا كان نجاحه قد يترك server effect بلا durable local mutation identity.

---

# 48. No network inside Room transaction

ممنوع داخل `withTransaction`:

```text
PostgREST
RPC
Storage upload
HTTP
Auth refresh
Realtime await
WorkManager enqueue
```

الـtransaction محلية وقصيرة فقط.

---

# 49. Transaction failure invariant

إذا Outbox insert فشل:

```text
local entity mutation must roll back
```

إذا entity write فشل:

```text
Outbox insert must roll back
```

لا partial commit.

---

# 50. Outbox insert conflict policy

الـdefault insert للـoperation identity يجب ألا يستخدم `REPLACE` بطريقة قد تمحو row `IN_PROGRESS` أو metadata محاولة قائمة.

المطلوب:

```text
ABORT / explicit upsert policy based on immutable mutation identity
```

أي `REPLACE` يجب أن يكون محصورًا بحالة مثبتة وآمنة.

---

# 51. Profile — atomic local intent

`updateUser()` بعد 68 يجب أن يحقق:

```text
db.withTransaction {
    validate scope/user
    update AutoDriveUserEntity editable fields
    syncStatus = PENDING
    insert scoped UPDATE_PROFILE Outbox row
}
```

الـpayload يجب أن يمثل mutation المحددة التي وافق عليها المستخدم.

---

# 52. Profile — no direct-first write

الممنوع:

```text
local PENDING
→ direct server
→ only enqueue on failure
```

المطلوب:

```text
local PENDING + Outbox commit
→ optional immediate delivery after commit
```

---

# 53. Profile — session preferences

`sessionWriter.updateSession(userName/phone)` ليست Room.

يجوز تنفيذها بعد نجاح transaction المحلية.

لكن crash بينها وبين Room لا يجوز أن يؤثر على durability للmutation.

إذا أمكن جعل UI تعتمد Room profile كمصدر الاسم، فهذا خارج 68 ما لم يلزم compile/correctness.

---

# 54. Profile — repeated edits

إذا عدّل المستخدم profile مرتين Offline:

```text
M1 = first mutation
M2 = second mutation
```

المسموح:

```text
append M1 then M2 with ordered creation
```

أو safe coalescing مثبت قبل claim فقط.

الممنوع:

```text
replace an IN_PROGRESS mutation silently
lose M2
mark entity SYNCED after M1 while M2 remains unresolved
```

---

# 55. Profile — syncStatus acknowledgement

بعد نجاح mutation M:

```text
syncStatus may become SYNCED only if no newer unresolved profile mutation exists for same exact scope/entity.
```

إذا M2 pending:

```text
profile remains PENDING
```

حتى تنتهي آخر mutation.

---

# 56. Profile — legacy idempotencyKey

`profile:<userId>` في v67 هو dedupe/entity key، وليس mutation identity جيدة لتعديلات متتالية.

بعد 68:

```text
entityId   = userId
mutationId = unique logical mutation id
```

لا يجوز استخدام `profile:<userId>` كـmutationId لكل تعديل جديد.

---

# 57. Withdrawal — atomic creation

قبل أي RPC:

```text
db.withTransaction {
    insert WithdrawalRequestEntity(
      id = clientRequestId,
      syncStatus = PENDING_SYNC,
      ...
    )
    insert scoped REQUEST_WITHDRAWAL_RPC Outbox(
      mutationId = clientRequestId,
      entityId = clientRequestId,
      ...
    )
}
```

هذا شرط أساسي.

---

# 58. Withdrawal — server call timing

`request_withdrawal` لا يجوز أن يُستدعى قبل نجاح transaction أعلاه.

يجوز بعد commit:

```text
immediate attempt
or
normal Outbox flush
```

لكن في الحالتين الـdurable operation موجود مسبقًا.

---

# 59. Withdrawal — stable identity

يستمر invariant الحالي:

```text
mutationId = client_request_id
```

ويجب أن يبقى ثابتًا عبر:

```text
initial send
retry
process restart
worker retry
reconciliation
```

---

# 60. Withdrawal — committed server identity

إذا server يرجع `serverId`:

```text
local temp row -> server identity reconciliation
```

يجب أن يتم مع Outbox finalization داخل local transaction واحدة قدر الإمكان.

الهدف:

```text
no state where operation disappears but local temp row remains unresolved due local crash
```

---

# 61. Withdrawal — ambiguous response

السلوك الحالي `findCommittedWithdrawal(client_request_id)` يبقى مسموحًا.

68 لا تعيد تصميم server idempotency contract.

لكن ambiguous network result:

```text
must leave Outbox durable and retryable/reconcilable
```

ولا يجوز حذف local intent.

---

# 62. Withdrawal — permanent rejection

إذا server يرفض العملية قطعًا قبل commit وفق semantics الحالية:

أي rollback للoptimistic local entity يجب أن يتزامن مع إزالة/إنهاء exact Outbox row داخل transaction.

الممنوع:

```text
delete entity
leave sendable Outbox
```

أو:

```text
delete Outbox
leave PENDING_SYNC entity
```

---

# 63. Withdrawal — business logic protection

68 لا تغير:

```text
balance eligibility
pending request business rules
bank detail requirements
error wording UX إلا لضرورة correctness
```

المطلوب فقط transactional durability/ownership.

---

# 64. Chat Send — target

كل رسالة outgoing يجب أن يكون لها:

```text
ChatMessageEntity
matching SEND_CHAT_MESSAGE Outbox row
```

بنفس logical message identity.

---

# 65. Chat Send — message identity

لرسالة Chat:

```text
entityId   = message.id
mutationId = stable id for same send intent
```

يجوز أن يساوي `mutationId == messageId` لأن client-generated UUID هو logical send identity الحالية.

`operationId` يمكن أن يساويه أو يكون UUID آخر؛ يجب ألا يتغير على retry لنفس row.

---

# 66. Chat Send — atomic local envelope

بعد `PreparedChatMedia`:

```text
db.withTransaction {
    insert ChatMessageEntity(status=PENDING)
    update conversation preview/local timestamp
    insert scoped SEND_CHAT_MESSAGE Outbox
}
```

الثلاثة يجب أن تنجح أو تفشل معًا.

---

# 67. Chat Send — media boundary

`ChatMediaManager.prepareOutgoing()` في v67 قد يرفع media إلى Storage قبل Room write.

Session 68 **لا** تبني durable media transfer queue؛ ذلك مؤجل.

لذلك ضمان 68 هو:

```text
message envelope + Outbox atomic after media preparation
```

ولا يجوز الادعاء:

```text
media upload is crash-durable end-to-end
```

---

# 68. Chat Send — text message full guarantee

لـ`MessageType.TEXT` لا يوجد media network pre-step.

إذًا يجب إثبات fully:

```text
crash cannot leave local PENDING message without Outbox
and cannot leave Outbox without local message
```

---

# 69. Chat Send — media orphan honesty

إذا Storage upload نجح ثم process مات قبل Room transaction:

قد يبقى remote media object بلا message.

هذا known defer إلى media-transfer session.

لا يعد فشلًا لـ68 إذا:

```text
no local message intent was committed
no false claim of media durability
```

لكن يجب توثيقه.

---

# 70. Chat Send — direct remote path

`insertRemoteMessage()` لا يجوز أن يبقى هو durability authority.

بعد 68 يكون دوره:

```text
delivery implementation for Outbox
or optional post-commit fast path backed by existing Outbox row
```

الممنوع:

```text
local insert -> direct remote -> FAILED without durable operation
```

---

# 71. Chat status mapping

بعد 68 يفضل الحفاظ على compatibility:

```text
PENDING = matching Outbox unresolved
SENT    = server delivery reconciled/acknowledged
FAILED  = permanent/dead-letter equivalent or explicit manual state
READ    = existing remote/read semantics if used
```

لا تجعل `FAILED` تعني “network failed once” بينما Outbox ستعيد تلقائيًا دون توثيق واضح.

---

# 72. Chat retrySend

`retrySend(messageId)` يجب ألا ينشئ duplicate mutations عشوائية.

المطلوب:

```text
if matching unresolved mutation exists -> reuse/reactivate exact operation safely
if no matching mutation and local message is retryable -> create one atomic with status transition
```

`mutationId` لنفس logical message يبقى ثابتًا.

---

# 73. Chat server duplicate ambiguity

إذا server commit تم ثم client فقد response، retry قد يصطدم بالـsame message id.

68 لا تلزم ببناء generic server receipt.

يجب فقط:

```text
preserve mutation identity and durable operation so 70/reconciliation can resolve it
```

ولا يجوز حذف الرسالة المحلية نهائيًا بسبب timeout غامض.

---

# 74. Chat Read Receipt — target

`markMessagesAsRead(conversationId)` يجب أن يتحول من best-effort network إلى durable intent.

المطلوب:

```text
local read state + conversation unread reset + MARK_CHAT_READ Outbox
```

داخل transaction واحدة.

---

# 75. Chat Read Receipt — entity identity

المعنى المقترح:

```text
entityType    = internal_messages
entityId      = conversationId
operationType = MARK_CHAT_READ
```

الـpayload يحمل فقط ما يحتاجه server الحالي، دون dump للرسائل.

---

# 76. Chat Read Receipt — idempotency

الـserver update الحالي:

```text
set is_read=true where conversation_id/client_id/sender_type=ADMIN/is_read=false
```

هو state-setting idempotent بطبيعته.

68 يمكنه retry مع نفس mutation identity دون إنشاء أثر مضاعف منطقي.

---

# 77. Chat Read Receipt — local protection

طالما Outbox read receipt unresolved:

remote/realtime data لا يجوز أن تعيد الرسائل التي قرأها المستخدم إلى unread محليًا بلا policy صريحة.

يجوز guard محدود في chat data path.

ممنوع تنفيذ Realtime hint-only rewrite الكامل داخل 68.

---

# 78. Notification Read — unify Outbox

`read_synced=false` يبقى compatibility marker، لكن durability authority تنتقل إلى Outbox.

المطلوب عند read one:

```text
db.withTransaction {
    mark notification isRead=true, readSynced=false
    insert MARK_NOTIFICATION_READ Outbox
}
```

---

# 79. Notification Read — remove ad-hoc sender loop

بعد 68 لا يجوز أن يظل `OutboxSynchronizer.flush()` يرسل notifications من:

```text
notificationDao.getUnsynced(userId)
```

كقناة مستقلة عن pending_operations.

المطلوب:

```text
notification read operations flow through the same scoped Outbox processor
```

`getUnsynced` قد يبقى للreconciliation/tests، لا كdelivery authority.

---

# 80. Notification Read — exact owner

Outbox row يجب أن تحمل:

```text
userId = notification.userId/current scope user
clientId = current scope client
orgId = current scope org
entityId = notificationId
```

إذا local notification لا تطابق current scope:

```text
NOTIFICATION_SCOPE_MISMATCH
```

ولا mutation.

---

# 81. Notification markAllRead

`markAllRead(userId)` يجب أن يكون atomic من منظور local intent.

المفضل:

```text
load target unread notification ids for exact user/scope
withTransaction {
    mark exact rows readSynced=false
    insert one Outbox mutation per notification
}
```

لأن كل row تملك entityId ثابتة.

إذا implementation يستخدم bulk Outbox operation واحدة، يجب إثبات crash safety وعدم فقد subset semantics.

---

# 82. Notification acknowledgement

بعد نجاح server update:

```text
confirmReadSynced(notificationId)
+ finalize exact Outbox row
```

يجب أن يكونا transaction محلية واحدة أو equivalent atomic finalizer.

---

# 83. Read receipt payload privacy

ممنوع وضع في payload:

```text
notification title/body
chat full message bodies
phone
bank data
raw auth tokens
```

read receipt تحتاج identities فقط.

---

# 84. Unified Outbox delivery registry

بعد 68 sender يجب أن يدعم allowlist واضحة:

```text
UPDATE_PROFILE
REQUEST_WITHDRAWAL_RPC
SEND_CHAT_MESSAGE
MARK_CHAT_READ
MARK_NOTIFICATION_READ
```

Unknown operation:

```text
UNSUPPORTED_OUTBOX_OPERATION
```

ولا dynamic network table execution.

---

# 85. Scope capture at flush start

`OutboxSynchronizer.flush()` يجب أن يعمل على:

```text
scopeAtStart = SyncScope.from(currentSession)
```

إذا null:

```text
SKIPPED_MISSING_SYNC_SCOPE
```

ولا query global.

---

# 86. Scope validation before send

قبل network send لكل claimed operation:

```text
operation.scope == scopeAtStart
currentSession scope == scopeAtStart
```

إذا تغيرت:

```text
STALE_OUTBOX_SCOPE
```

والعملية لا تُرسل تحت session الجديدة.

---

# 87. Scope validation after network

بعد network response وقبل local acknowledgement:

```text
current scope must still equal operation scope
```

إذا تغيرت session:

```text
no cross-account local acknowledgement
```

الـserver effect إن حدث يجب أن يبقى قابلًا للتسوية لاحقًا بنفس mutationId.

---

# 88. Processor API بعد 68

`PendingOperationProcessor` يمكن إعادة تصميمه، لكن يجب إثبات:

```text
scoped due selection
scoped claim
separate leaseUntil
cancellation releases exact scope claim
retry preserves scope/mutation identity
success finalization exact operation only
```

---

# 89. Claim lease timing

عند claim:

```text
status = IN_PROGRESS
leaseUntil = now + CLAIM_LEASE_MILLIS
nextRetryAt unchanged as retry schedule
```

لا overwrite لـnextRetryAt بالlease.

---

# 90. Expired claim recovery

عند expiry:

```text
status = PENDING
leaseUntil = 0
```

ويحافظ `nextRetryAt` على retry policy المناسبة.

# 91. Cancellation semantics

`CancellationException` يجب أن يعاد رميه.

إذا operation كانت claimed ولم يبدأ/يكتمل acknowledgement:

```text
release exact claim safely
```

ولا تحول cancellation إلى success أو dead letter.

---

# 92. Success acknowledgement boundary

الشكل المطلوب:

```text
network send outside transaction
↓
db.withTransaction {
    revalidate operation/scope
    apply local acknowledgement/reconciliation
    finalize/remove exact Outbox row
}
```

لا network داخل transaction.

---

# 93. No detached local acknowledgement

الممنوع مثل v67:

```text
sender updates local entity to SYNCED
returns
processor later marks/deletes operation separately
```

إذا crash بينهما قد تصبح الحالة منفصلة.

68 يجب أن تجمع final local state + Outbox finalization حيث توجد local acknowledgement.

---

# 94. Outbox finalization

يجوز أحد نمطين:

```text
A) mark SUCCEEDED + delete within same transaction
B) direct delete exact operation after local acknowledgement within same transaction
```

المهم:

```text
no externally observable half-finalized state after commit
```

---

# 95. Retry failure recording

عند retryable failure:

```text
status = PENDING
attemptCount += 1
nextRetryAt = retryPolicy result
leaseUntil = 0
lastError* sanitized
```

كل update يجب أن يكون scoped by operationId+scope.

---

# 96. Dead letter compatibility

عند permanent/dead-letter حسب policy الحالية:

```text
status = DEAD_LETTER
leaseUntil = 0
```

لا تحذف local intent تلقائيًا.

68 لا تبني recovery UI النهائية.

---

# 97. No blind payload parsing authority

الـpayload يبقى typed حسب `operationType + contractVersion`.

إذا decode فشل:

```text
PERMANENT/INVALID_OUTBOX_PAYLOAD
```

ولا fallback إلى dynamic map/table execution.

---

# 98. Worker scope contract

`PendingOperationsWorker` بعد 68 يجب ألا يشغّل global queue.

المطلوب:

```text
current valid SyncScope
→ flush only exact scope
```

إذا session مفقودة:

```text
Result.success/skip حسب policy
```

لكن لا يرسل أي row.

---

# 99. Worker stale-session fixture

سيناريو:

```text
worker scheduled under account A
logout
login B
worker wakes
```

PASS:

```text
A operations are never claimed/sent under B
```

حتى لو WorkManager work name نفسها.

---

# 100. WorkManager scheduling is not ownership

اسم work أو unique work لا يعتبر tenant boundary.

الownership authority هي:

```text
Outbox row scope + current SyncScope validation
```

---

# 101. Logout — scope snapshot

قبل مسح session preferences يجب التقاط:

```text
scopeToLogout = SyncScope.from(currentSession)
```

كل cleanup يعتمد على هذه snapshot، لا على قراءة session بعد clear.

---

# 102. Logout — quiesce new sync

قبل تنظيف Outbox للحساب يجب منع بدء sync جديدة لنفس scope أثناء logout.

يمكن عبر:

```text
SyncCoordinator logout barrier / quiesce API
```

أو equivalent مثبت.

الممنوع:

```text
clear queue while another coroutine can immediately claim/create/send for same departing scope
```

---

# 103. Logout — active sync

إذا sync/outbox active للحساب A:

logout يجب أن يضمن واحدًا من:

```text
A) cancel active owner and await quiescence
or
B) hard scope barrier that prevents any further send/apply for A before cleanup commits
```

الأفضل A+B.

---

# 104. Logout — Realtime order

Realtime يجب أن يتوقف قبل final local cleanup كما هو intent الحالي.

لكن stop Realtime وحده لا يكفي؛ Sync/Worker may still run.

---

# 105. Logout — local cleanup transaction

`LocalDataCleaner.clearCurrentAccount(scope)` يجب أن يستخدم:

```text
db.withTransaction { ... }
```

على الأقل لكل Room cleanup لحساب المغادرة.

---

# 106. Logout — scoped Outbox cleanup

داخل cleanup transaction:

```text
pendingOperationDao.deleteForScope(scope)
```

الممنوع:

```text
pendingOperationDao.deleteAll()
```

في production logout.

---

# 107. Logout — cursor cleanup

بما أن v67 أضاف scoped `sync_cursors`، logout safety الكاملة لهذا المسار يجب أن تقرر policy صريحة.

الاختيار الافتراضي الآمن في 68:

```text
delete sync cursors for departing scope during local account cleanup
```

لأن local account data تُحذف أيضًا.

الممنوع:

```text
leave cursor for scope A after deleting A's Room data then later resume from that cursor against empty local state
```

---

# 108. Logout — account tables

يستمر حذف بيانات الحساب الحالي كما في v67:

```text
invoices/payments by client relation
commission payments by client
balance/transactions/withdrawals by user
notifications by user
profile by user
chat messages by conversation ids
conversations by marketer
```

لكن داخل transaction واحدة قدر الإمكان.

---

# 109. Logout — shared caches

`weeklyLeaderboardDao.clear()` موجود حاليًا كglobal cache cleanup.

يجوز بقاؤه لأن logout يمسح cache غير scoped بدل تسريبها.

لا تعيد تصميم cache schema في68.

---

# 110. Logout — session preferences clear

بعد نجاح local cleanup/quiescence:

```text
supabase signOut attempt
sessionWriter.clearSession()
```

الترتيب exact مع push-token revocation يمكن الحفاظ عليه وفق auth semantics، بشرط ألا توجد نافذة تسمح بـold-scope Outbox send بعد cleanup.

---

# 111. Logout — push token

Push token registration/revocation ليست ضمن transactional Outbox 68 الأساسي.

لا توسع 68 إلى generic token command protocol.

لكن logout يجب ألا يُكسر behavior الحالي لمحاولة حذف token قبل auth session disappearance.

---

# 112. Login after logout

بعد login بحساب B:

```text
new sync requests must be accepted for B
```

إذا أضيف logout barrier، يجب أن تملك deterministic resume behavior.

يمكن `LOGIN_SUCCESS` أن يفتح gate للscope الجديدة أو API صريحة.

الممنوع:

```text
coordinator permanently blocked after first logout
```

---

# 113. Same-scope relogin

إذا خرج المستخدم A ثم دخل A مرة أخرى في نفس process:

logout barrier يجب أن يسمح session جديدة بعد نجاح auth/login.

لا تعتمد فقط على `scope != blockedScope` لفتحها.

---

# 114. Cross-account Outbox isolation

اختبار إلزامي:

```text
A row for scope A
B row for scope B
current session B
flush
```

PASS:

```text
B may be claimed/sent
A untouched
```

---

# 115. Cross-account claim by operationId

حتى إذا عرف caller `operationId` للحساب A، claim تحت scope B يجب أن يرجع:

```text
0 rows
```

---

# 116. Cross-account finalization

ack/failure/delete operations يجب أن تشمل scope.

لا يكفي:

```text
WHERE operation_id=:id
```

إذا ID وحدها ليست حاجز ownership في API.

---

# 117. Scope immutability per mutation

Outbox row owner scope immutable بعد insert.

ممنوع:

```text
UPDATE pending_operations SET user_id=currentUser ...
```

لإعادة استخدام row بعد account switch.

---

# 118. Scope immutability per delivery attempt

`scopeAtClaim` تظل authority حتى نهاية attempt.

إذا session تغيرت:

```text
STALE_OUTBOX_SCOPE
```

ولا تُعاد العملية تلقائيًا باسم الحساب الجديد.

---

# 119. SyncScope reuse

يجب إعادة استخدام `SyncScope` التي أضافتها67.

ممنوع إنشاء type ثانٍ متضارب مثل:

```text
OutboxScope(user, client) // بدون org
```

إلا wrapper واضح يحافظ الثلاثة.

---

# 120. Preserve v67 pipeline order

68 لا تعيد order إلى:

```text
Pull → Push
```

يجب الحفاظ على v67:

```text
AUTH
→ RECOVER_LEASES
→ PUSH_OUTBOX
→ positive pulls
→ tombstone delta
→ reconcile
```

حتى لو v67 server tombstone adapter ما زال blocked.

---

# 121. Preserve generation-safe coordinator

`requestedGeneration/completedGeneration` لا تُحذف ولا تُضعف.

إذا أضيف logout quiescence:

يجب الحفاظ على:

```text
no lost hint
no stuck activeSync
future LOGIN_SUCCESS can run
```

---

# 122. Preserve durable cursor semantics

68 لا تعدل `sync_cursors` إلا cleanup scope policy/required migration registration.

ممنوع:

```text
cursor reset on every mutation
cursor tied to Outbox lease
cursor moved into SharedPreferences
```

---

# 123. Preserve tombstone fail-closed state

بما أن v67 لم تملك server contract:

68 لا يجوز أن تحوّل `BlockedServerDeletionFeed` إلى fake production feed لتجاوز predecessor gate.

ذلك يبقى:

```text
BLOCKED_SERVER_TOMBSTONE_CONTRACT
```

حتى يصل contract الحقيقي.

---

# 124. Preserve PendingLocalMutationGuard

حماية profile/withdrawal/notification من stale pull يجب أن تستمر.

لكن بعد إضافة Outbox scope، يفضل أن تعتمد guard على:

```text
exact scoped active operations
```

بدل unscoped `findActiveByIdempotencyKey`.

---

# 125. Pending guard scope

أي query للـpending local conflict يجب أن تمر:

```text
scope + entityType + entityId / mutationId
```

ممنوع row من A تحمي/تمنع apply لحساب B.

---

# 126. Profile pending guard after schema 15

الـprofile guard يجب أن يعرف أن profile pending إذا:

```text
local syncStatus != SYNCED
OR
there is unresolved scoped UPDATE_PROFILE for entityId=userId
```

ويفضل الاثنين للcompatibility أثناء الانتقال.

---

# 127. Withdrawal reconciliation after schema 15

البحث عن operation المطابقة يجب أن يعتمد:

```text
scope
operationType=REQUEST_WITHDRAWAL_RPC
mutationId=client_request_id
```

لا global idempotency lookup.

---

# 128. Notification guard after Outbox unification

`readSynced=false` + active scoped `MARK_NOTIFICATION_READ` يجب أن يعبرا عن نفس intent.

لا يجوز أن يكونا قناتين مستقلتين يمكن أن diverge بعد commit.

---

# 129. Chat read guard

إذا `MARK_CHAT_READ` unresolved:

incoming remote state لا يجوز أن يخفض local read state لنفس conversation.

يمكن تنفيذ guard بسيط scoped في Chat repository/realtime participant دون إزالة Realtime writes بالكامل.

---

# 130. No duplicate outbound authorities

بعد 68 لكل target operation يجب أن يوجد delivery authority واحد:

```text
pending_operations
```

الممنوع:

```text
profile direct retry loop + Outbox independent
notification getUnsynced loop + Outbox independent
chat FAILED retry path + Outbox independent
```

يجوز fast-path لكن يجب أن يستهلك/ينفذ exact existing Outbox operation، لا create parallel protocol.

---

# 131. Fast-path delivery policy

إذا feature تريد immediate UX:

```text
transactional enqueue first
then request exact Outbox flush or sync
```

الممنوع:

```text
server call first, enqueue only on failure
```

---

# 132. Outbox sender ownership

يفضل نقل server delivery logic للعمليات الخمس إلى `OutboxSynchronizer`/typed senders.

Feature repository مسؤول عن:

```text
validate
local mutate
transactional enqueue
optional trigger
```

وليس ownership النهائي للretry protocol.

---

# 133. Operation payload contracts

يجب تعريف payload typed لكل operation.

المطلوب على الأقل:

```text
Profile update payload
Withdrawal request params
Chat message send payload
Chat read receipt payload
Notification read payload
```

كل واحد versioned بالـcontractVersion.

---

# 134. Chat payload

يجب أن يحمل فقط fields اللازمة لإعادة إرسال الرسالة الحالية:

```text
message id
conversation id
sender id/type
client/org identity as needed by server command
message type/body
media URL/mime/duration if already prepared
```

لا يحمل `localPath` إلى server كحقيقة remote.

---

# 135. Chat localPath

`localPath` يبقى local-only.

إذا payload يحتاج إعادة media upload مستقبلًا، هذا ضمن durable transfer queue اللاحقة، لا68.

---

# 136. Profile sensitive payload

Profile Outbox قد يحتوي phone/bank details لأنها mutation لازمة.

لذلك:

```text
never log raw payload
never include payload in verification artifact
never emit payload in exception text
```

---

# 137. Withdrawal sensitive payload

`RequestWithdrawalParams` لا يجب أن يكرر bank account إذا RPC الحالي لا يحتاجه.

الحساب البنكي يبقى في local entity/server profile حسب العقد الحالي.

ممنوع توسيع payload الحساسة بلا حاجة.

---

# 138. Error logging

استمر في `SensitiveDataRedactor`/sanitized error handling.

ممنوع logging:

```text
raw Outbox payload
phone
bank account
access token
refresh token
full scope triple in public report
```

---

# 139. Static source scan — global Outbox API

`verify-v68-static` يجب أن يفشل إذا وجد production call sites لـ:

```text
pendingOperationDao.getDue(now,...)
pendingOperationDao.releaseExpiredClaims(now)
pendingOperationDao.countByStatus(status)
pendingOperationDao.deleteAll()
```

بلا scope.

---

# 140. Static source scan — direct-first paths

Static gate يجب أن يلتقط الأنماط الحالية المحظورة:

```text
profile: local PENDING then direct server then enqueue on failure
withdrawal: local pending then RPC then enqueue only on ambiguous failure
chat send: local PENDING then direct insert without Outbox
chat read: local read then best-effort remote without Outbox
notification read: getUnsynced loop as standalone sender
```

بعد 68 counts يجب أن تكون صفرًا أو موثقة كnon-authoritative compatibility path.

---

# 141. Allowed production scope

المسموح تعديله في 68 عند الحاجة المباشرة فقط:

```text
core/database/src/main/kotlin/com/autodrive/app/core/database/**
core/sync/src/main/kotlin/com/autodrive/app/core/sync/**
feature/profile/**/data/**
feature/balance/**/data/**
feature/chat/**/data/**
feature/notifications/**/data/**
feature/auth/**/data/**                       [logout/quiesce فقط]
core/session/**                              [فقط إن لزم ربط SyncScope دون تغيير auth semantics]
```

يجوز تعديل DI bindings المرتبطة مباشرة بهذه المكونات إذا تطلب compile ذلك.

---

# 142. Allowed database files

الحد المتوقع:

```text
PendingOperationEntity / Entities.kt
PendingOperationDao.kt
AutoDriveDatabase.kt
Migrations.kt أو موضع MIGRATION_14_15 الفعلي
Room schema export configuration فقط إذا كان موجودًا أصلًا
```

لا يجوز لمس جداول business غير اللازمة للـtransactional mutation إلا لإضافة query محددة تخدم transaction واحدة.

---

# 143. Allowed sync files

مسموح تعديل:

```text
PendingOperationProcessor
OutboxSynchronizer
SyncManager / SyncEngine contract عند الحاجة
SyncScope
Pending-local guards/reconciliation
LocalDataCleaner
DefaultSyncCoordinator only for logout/quiesce integration and compile adaptation
```

ممنوع إعادة تصميم generation algorithm الذي أثبتته 67 إلا لإصلاح خلل مثبت.

---

# 144. Allowed feature scope

Profile:

```text
ProfileRepositoryImpl
profile DAO/service adapter only as required
```

Withdrawal:

```text
BalanceRepositoryImpl
withdrawal DAO/service adapter only as required
```

Chat:

```text
ChatRepositoryImpl
ChatMediaManager only for minimal boundary adaptation
chat DAO queries needed for atomic envelope/read transitions
```

Notifications:

```text
NotificationRepositoryImpl
notification DAO queries needed for atomic read+Outbox
```

Auth:

```text
AuthRepositoryImpl / sign-out orchestration only as required for safe scoped cleanup
```

---

# 145. Forbidden production drift

ممنوع تعديل:

```text
core/designsystem/**
UI/Compose screens
home/report/competition UX
commission formulas
withdrawal eligibility business rules
invoice/payment business math
fonts/colors/theme
media transfer architecture
chat pagination/recovery architecture
server command protocol redesign
Realtime hint-only conversion
Inbox/change-feed/bootstrap/anti-entropy
```

أي تغير غير مطلوب مباشرة:

```text
BLOCKED_UNRELATED_MUTATION
```

---

# 146. Server-side scope

التوقع الطبيعي في 68:

```text
new server migration count = 0
```

68 تعالج **local mutation + Outbox atomicity** وتستخدم server surfaces الموجودة.

إذا ظهر احتياج لتغيير server command semantics كي يصبح retry idempotent، فهذا يخص الجلسة التالية الخاصة بالـcommand protocol، لا يُسحب إلى68 إلا كـblocking finding موثق.

---

# 147. Historical SQL integrity

حتى لو لم تُضف SQL جديدة:

```text
historicalMigrationMutationCount = 0
```

أي تعديل على migration تاريخية:

```text
BLOCKED_HISTORICAL_MIGRATION_MUTATION
```

---

# 148. UI mutation gate

العدد المتوقع:

```text
productionUiFilesChanged = 0
```

أي UI change يحتاج تبرير compile-only واضح؛ غير ذلك FAIL scope.

---

# 149. New waiver policy

```text
newV68WaiverCount = 0
```

غير مقبول waiver من نوع:

```text
"enqueue usually succeeds"
"logout is rare"
"scope can be inferred later"
"next_retry_at can continue to act as lease"
"chat can remain best-effort"
```

هذه failures وليست waivers.

---

# 150. Migration test contract

عند توفر Gradle/Android يجب اختبار:

```text
13 → 15 preserves data through 13→14→15
14 → 15 preserves all v67 rows
pending_operations legacy profile row maps deterministically
pending_operations legacy withdrawal row maps deterministically
unresolvable legacy row is not cross-account executable
sync_cursors remain intact unless logout scope cleanup test invokes removal
composite scope indexes behave as expected
lease_until is independent from next_retry_at
no destructive migration fallback exists
```

---

# 151. Exact pending_operations schema test

Android migration test يجب أن يثبت وجود الحقول الدلالية:

```text
id / operation_id
mutation_id
user_id
client_id
org_id
entity_type
entity_id
operation_type
contract_version
payload
created_at
status
attempt_count
next_retry_at
lease_until
last_error_code
last_error_message
payload_version if retained for compatibility
idempotency_key if retained for compatibility
```

الأسماء قد تختلف تقنيًا إذا schema الحالية تفرض compatibility، لكن verifier يجب أن يربطها بالدلالات المذكورة بلا غموض.

---

# 152. Transaction fixture — Profile

Fixture إلزامي:

```text
begin update profile
write local PENDING
force Outbox insert failure
```

PASS:

```text
profile change rolled back
no orphan PENDING entity
no partial preferences authority used as proof of pending mutation
```

ثم العكس:

```text
entity write succeeds + Outbox row succeeds -> transaction commits both
```

---

# 153. Transaction fixture — Withdrawal

Fixture:

```text
create local withdrawal pending
force Outbox insert failure
```

PASS:

```text
withdrawal local creation rolls back
no server call occurs
```

وفي success:

```text
local pending row + scoped Outbox commit together before RPC attempt
```

---

# 154. Transaction fixture — Chat send

لـTEXT على الأقل:

```text
create PENDING chat message
update conversation preview if current behavior requires
insert CHAT_SEND Outbox
```

كلها في transaction واحدة.

إذا Outbox insertion يفشل:

```text
message + preview mutation roll back
```

لا تظهر رسالة محلية Pending بلا durable intent.

---

# 155. Transaction fixture — Chat read receipt

Fixture:

```text
mark messages read / reset unread
insert durable read receipt intent
force Outbox insert failure
```

PASS:

```text
local read transition rolls back OR no final synced-looking state is exposed
```

السياسة المختارة يجب أن تكون واحدة وثابتة؛ الموصى بها rollback الكامل لأن العقد يطلب local mutation + Outbox atomicity.

---

# 156. Transaction fixture — Notification read

Fixture:

```text
notification is unread
mark local read
insert NOTIFICATION_READ Outbox
force insert failure
```

PASS:

```text
transaction rolls back
```

وعند النجاح:

```text
isRead=true + readSynced=false + matching scoped Outbox row
```

---

# 157. Pending-without-Outbox invariant fixture

بعد كل public local mutation API في scope 68، افحص:

```text
pending local intent exists
=> exactly one active matching Outbox identity exists
```

الاستثناء الوحيد:

```text
state already acknowledged/synced or operation intentionally rejected atomically
```

---

# 158. Outbox-without-entity fixture

بعض operations مثل read receipt قد تشير إلى entity موجودة أصلًا.

لكن لكل operation يجب أن يحدد contract ما إذا كان target row يجب أن يوجد.

ممنوع orphan Outbox غير قابل للتفسير.

Unknown/missing target عند send:

```text
TARGET_NOT_FOUND / RECONCILIATION_REQUIRED
```

ولا حذف صامت للـoperation.

---

# 159. Scope A/B due-query fixture

أنشئ:

```text
scope A operation due
scope B operation due
processor runs under scope A
```

PASS:

```text
A may be claimed
B remains untouched
```

---

# 160. Scope A/B claim race fixture

حتى إذا عرف caller ID لعملية B:

```text
claim(scopeA, operationIdB)
```

يجب أن يفشل.

PASS:

```text
B status unchanged
lease unchanged
attempt unchanged
```

---

# 161. Lease independence fixture

Scenario:

```text
operation scheduled nextRetryAt = Tfuture
another operation claimed with leaseUntil = Tlease
```

PASS:

```text
claim does not rewrite retry schedule as lease authority
release-expired logic consults leaseUntil only
getDue consults nextRetryAt + status only
```

---

# 162. Process death after local commit fixture

Scenario:

```text
Room transaction commits Entity + Outbox
process dies before network send
```

PASS:

```text
both remain durable
worker/app-start can rediscover Outbox by exact scope
```

هذا هو السيناريو المركزي لـ68.

---

# 163. Process death before transaction commit fixture

Scenario:

```text
entity write executed in transaction body
process/cancellation before commit
```

Expected Room semantics:

```text
entity mutation absent
Outbox mutation absent
```

---

# 164. Cancellation fixture

`CancellationException` أثناء mutation transaction أو send processing:

```text
must be rethrown
```

إذا cancellation قبل transaction commit:

```text
rollback both
```

إذا بعد durable commit وأثناء send:

```text
Outbox remains claimable/recoverable after lease expiry/release policy
```

---

# 165. Logout queue fixture

Scenario:

```text
scope A owns pending operations
logout A
```

PASS فقط إذا policy المختارة تحقق:

```text
no A operation is executable after logout completion
no unscoped delete affects B
```

إذا cleanup يحذف Outbox A، يجب أن يكون scoped delete فقط.

---

# 166. Logout during claim fixture

Scenario:

```text
A operation claimed
logout begins
```

يجب أن يمنع finalization callback القديم من:

```text
writing into new scope B
marking/deleting B rows
repopulating A business rows after cleanup
```

الحد الأدنى:

```text
scope snapshot + scope recheck + coordinator quiesce/cancel barrier
```

---

# 167. Immediate B login fixture

Scenario:

```text
logout A completes
login B immediately
worker/sync starts
```

PASS:

```text
B sees only B Outbox
B cannot claim A rows
A cursor/queue policy from logout is respected
no A callback writes after B session established
```

---

# 168. Same IDs across scopes fixture

أنشئ نفس:

```text
entityId
operationType
```

في A وB.

PASS:

```text
mutationId/operationId identities remain independent
query/claim/finalize use scope + operation identity
```

لا تعتمد على entityId وحده كـglobal identity.

---

# 169. Legacy row safety fixture

ضع row v14 بلا scope ثم migrate.

إذا يمكن إثبات owner deterministic:

```text
row becomes fully scoped
```

إذا لا يمكن:

```text
row becomes quarantined/non-executable or migration blocks with explicit evidence
```

ممنوع أن يصبح row صالحًا لأول logged-in account.

---

# 170. Profile overwrite regression

يجب الحفاظ على fixture 67:

```text
local profile pending
server stale profile returned
```

بعد 68:

```text
local pending survives
matching Outbox is scoped and durable
```

---

# 171. Notification read regression

حافظ على:

```text
isRead=true + readSynced=false
remote says false
```

PASS:

```text
local read remains true
Outbox remains unresolved until ack
```

---

# 172. Withdrawal reconciliation regression

حافظ على:

```text
client_request_id
```

ووسّع الاختبار للتأكد أن reconciliation لا تلمس operation من scope آخر حتى لو key متشابه.

---

# 173. v67 cursor regression

يجب أن تبقى:

```text
cursor scoped user/client/org/stream
cursor apply atomic
no clock cursor
no ephemeral authority
```

68 لا تعدل هذه semantics.

---

# 174. v67 generation regression

يجب استمرار fixtures:

```text
hint during push
hint during pull
completion-edge race
burst coalescing
cancellation clears owner
```

إذا تغيّر coordinator للـlogout integration، يجب إعادة هذه الاختبارات.

---

# 175. Required model/static fixtures

الحد الأدنى لـ68: **30 fixture** أو equivalent semantic checks:

```text
01 profile entity+Outbox atomic success
02 profile Outbox failure rolls back entity
03 withdrawal entity+Outbox atomic success
04 withdrawal Outbox failure rolls back entity
05 chat text envelope+Outbox atomic success
06 chat Outbox failure rolls back message/preview
07 chat read+Outbox atomic success
08 chat read Outbox failure rolls back read transition
09 notification read+Outbox atomic success
10 notification Outbox failure rolls back read transition
11 no pending profile without Outbox
12 no pending withdrawal without Outbox
13 no pending chat send without Outbox
14 no unsynced read receipt without Outbox
15 scope A due query excludes B
16 scope A cannot claim B
17 scope A cannot finalize B
18 leaseUntil independent from nextRetryAt
19 expired lease recovery scoped
20 process death after commit leaves both durable
21 cancellation before commit leaves neither
22 cancellation during send keeps recoverable intent
23 logout A makes A operations non-executable
24 logout A does not delete B operations
25 login B cannot see/claim A
26 legacy scoped row migration preserved
27 legacy unresolvable row fails closed
28 profile stale-pull guard regression
29 notification read guard regression
30 withdrawal client_request_id reconciliation scoped
```

يفضل إضافة:

```text
31 chat retry uses same mutationId
32 duplicate local API call cannot create accidental active duplicate
33 worker with stale session cannot send
34 media preparation failure creates no chat Outbox
35 current v67 generation fixtures unchanged
36 current v67 cursor fixtures unchanged
```

---

# 176. Unit tests عند توفر Gradle

الحد الأدنى المتوقع:

```text
ProfileTransactionalMutationTest
WithdrawalTransactionalMutationTest
ChatTransactionalOutboxTest
ReadReceiptTransactionalOutboxTest
NotificationTransactionalOutboxTest
ScopedPendingOperationDaoTest
PendingOperationProcessorScopeTest
OutboxLeaseSemanticsTest
LogoutOutboxIsolationTest
LegacyOutboxMigrationModelTest
DefaultSyncCoordinatorGenerationTest regression
```

الأسماء قابلة للتغيير؛ المعاني ليست اختيارية.

---

# 177. Android database tests

عند توفر instrumentation:

```text
MIGRATION_14_15
MIGRATION_13_15 chain
Room transaction rollback for each mutation class where practical
scoped DAO query behavior
lease/retry column independence
logout scoped cleanup transaction
```

إذا غير متاح:

```text
NOT_RUN_ANDROID_RUNTIME_UNAVAILABLE
```

---

# 178. Build gate

إذا Gradle distribution متاحة:

```text
compile relevant modules
run unit tests
run architecture/static tests
```

إذا استمر blocker:

```text
BUILD = BLOCKED_ENVIRONMENT_NETWORK_BOOTSTRAP
```

ولا يجوز الادعاء بأن compile PASS.

---

# 179. Server runtime gate

68 لا تتطلب server schema جديدة، لكن أي server interaction tests يمكن أن تثبت عدم regression.

إذا server غير متاح:

```text
SERVER_RUNTIME = NOT_RUN
```

هذا لا يبرر تخمين idempotency guarantees غير المثبتة.

---

# 180. Predecessor runtime truth

حتى لو نجحت 68 static بالكامل، إذا 67 ما تزال:

```text
BLOCKED_SERVER_TOMBSTONE_CONTRACT
handoff68Authorized=false
```

فالنتيجة العليا لـ68 لا تصبح سلسلة release PASS.

يجب تصنيفها مثلًا:

```text
IMPLEMENTED_STATIC_BUT_PREDECESSOR_BLOCKED
```

أو عدم بدء التنفيذ أصلًا وفق البوابة الصارمة.

---

# 181. Static verifier determinism

يجب إنشاء:

```text
scripts/verify-v68-static.sh
scripts/verify-v68-model.py
```

أو equivalent.

الشروط:

```text
offline
non-zero exit on gate failure
deterministic semantic JSON
run twice
no generated timestamp inside semantic hash comparison
```

---

# 182. Required verification JSON

يجب إنتاج:

```text
AUTODRIVE_SYNC_VERIFICATION_v68.json
```

ويحتوي على الأقل:

```text
sourceSha256
sourceRoomVersion
roomVersionAfter
session67Verdict
session67Handoff68Authorized
predecessorGateSatisfied
pendingOperationLegacyFieldInventory
outboxScopeFieldsPresent
operationIdPresent
mutationIdPresent
entityTypePresent
entityIdPresent
operationTypePresent
contractVersionPresent
leaseUntilPresent
retryLeaseSeparated
unscopedOutboxQueryCount
profileAtomicVerified
withdrawalAtomicVerified
chatSendAtomicVerified
chatReadAtomicVerified
notificationReadAtomicVerified
pendingWithoutOutboxFailureCount
scopedDueVerified
scopedClaimVerified
scopedFinalizeVerified
scopeIsolationVerified
logoutQuiesceVerified
logoutScopedCleanupVerified
staleCallbackBlockedVerified
legacyRowMigrationVerified
legacyUnknownOwnerFailClosed
v67CursorRegressionPassed
v67GenerationRegressionPassed
historicalMigrationMutationCount
productionUiFilesChanged
unexpectedProductionMutationCount
newV68WaiverCount
staticFixturePassed
staticFixtureTotal
buildStatus
androidRuntimeStatus
serverRuntimeStatus
finalVerdict
handoff69Authorized
```

---

# 183. Required verification Markdown

يجب إنتاج:

```text
AUTODRIVE_SYNC_VERIFICATION_v68.md
```

والترتيب المقترح:

```text
1. Baseline + predecessor gate
2. v67 source identity
3. Existing Outbox defect inventory
4. Room 14→15 migration
5. Canonical scoped Outbox schema
6. Atomic mutation writer
7. Profile conversion
8. Withdrawal conversion
9. Chat send conversion
10. Chat/read receipt conversion
11. Notification read conversion
12. Scoped processor + lease separation
13. Logout/quiesce isolation
14. Legacy-row migration policy
15. Static/model evidence
16. Build/runtime truth
17. Diff/scope inventory
18. Deferred work
19. Final verdict
20. handoff69Authorized
```

---

# 184. Required diff inventory

يجب تسجيل:

```text
production files touched
test files touched
Room migration files touched
server files touched
UI files touched
unexpected files touched
new scripts/artifacts
```

أي production mutation خارج القائمة دون تفسير:

```text
FAIL_SCOPE_INTEGRITY
```

---

# 185. Acceptance counters — يجب أن تساوي صفر

```text
inputDriftCount                         = 0
predecessorBypassWithoutAuthorization  = 0
newV68WaiverCount                      = 0
unscopedOutboxQueryCount               = 0
unscopedOutboxClaimCount               = 0
unscopedOutboxFinalizeCount            = 0
globalOutboxDeleteCount                = 0
retryLeaseOverloadCount                = 0
pendingWithoutOutboxFixtureFailures    = 0
atomicMutationFixtureFailures          = 0
crossScopeOutboxFixtureFailures        = 0
logoutIsolationFixtureFailures         = 0
legacyUnknownOwnerExecutableCount      = 0
directFirstProfilePathCount            = 0
directFirstWithdrawalPathCount         = 0
chatSendWithoutOutboxCount             = 0
readReceiptWithoutOutboxCount          = 0
standaloneNotificationUnsyncedSenderCount = 0
historicalMigrationMutationCount       = 0
productionUiFilesChanged               = 0
unrelatedProductionMutationCount       = 0
newSensitiveLogViolationCount          = 0
```

---

# 186. Acceptance values — exact/positive

```text
Room version                            = 15
MIGRATION_14_15                         = present
Outbox scope dimensions                 = userId + clientId + orgId
operationId                             = present/stable
mutationId                              = present/stable
entityType                              = present
entityId                                = present
operationType                           = present
contractVersion                         >= 1
leaseUntil                              = separate from nextRetryAt
profile atomic local+Outbox             = true
withdrawal atomic local+Outbox          = true
chat send atomic local+Outbox           = true for message envelope
chat read receipt atomic local+Outbox   = true
notification read atomic local+Outbox   = true
scope due/claim/finalize                = true
logout scoped cleanup                   = true
stale callback protection               = true
v67 cursor regression                   = true
v67 generation regression               = true
```

---

# 187. Failure codes

يجب دعم verdict/failure classification واضح، مثل:

```text
BLOCKED_PREDECESSOR_HANDOFF
BLOCKED_INPUT_DRIFT
BLOCKED_ROOM_BASELINE_DRIFT
BLOCKED_SCOPE_DRIFT
BLOCKED_UNRELATED_MUTATION
FAIL_OUTBOX_NOT_SCOPED
FAIL_OUTBOX_LEASE_OVERLOAD
FAIL_PROFILE_NOT_ATOMIC
FAIL_WITHDRAWAL_NOT_ATOMIC
FAIL_CHAT_SEND_NOT_ATOMIC
FAIL_CHAT_READ_NOT_ATOMIC
FAIL_NOTIFICATION_READ_NOT_ATOMIC
FAIL_PENDING_WITHOUT_OUTBOX
FAIL_CROSS_SCOPE_CLAIM
FAIL_CROSS_SCOPE_FINALIZE
FAIL_LOGOUT_QUEUE_LEAK
FAIL_STALE_CALLBACK_WRITE
FAIL_LEGACY_ROW_OWNER_AMBIGUOUS
FAIL_ROOM_MIGRATION_14_15
FAIL_V67_CURSOR_REGRESSION
FAIL_V67_GENERATION_REGRESSION
PASS_STATIC_RUNTIME_BLOCKED
PASS
```

---

# 188. PASS الكامل

`PASS` يتطلب:

```text
predecessorGateSatisfied = true
Room 15 migration verified
all four mandatory mutation families transactional
notification read path unified as required by actual v67 code defect
Outbox fully scoped
retry/lease separated
logout queue isolation proven
no stale callback cross-account write
legacy rows safely migrated/fail-closed
v67 cursor/generation regressions pass
compile/tests required for full PASS actually run and pass
newV68WaiverCount = 0
```

---

# 189. PASS_STATIC_RUNTIME_BLOCKED

يجوز فقط إذا:

```text
predecessor gate is satisfied
all source/static/model gates pass
Room 15 migration structurally verified
no correctness ambiguity remains
Gradle/runtime unavailable for environmental reason
```

ولا يعني:

```text
APK built
Room migration executed on device
server commands were E2E-tested
```

---

# 190. IMPLEMENTED_STATIC_BUT_PREDECESSOR_BLOCKED

إذا تم تنفيذ 68 رغم بقاء 67 blocked بناءً على override صريح، يجب أن يكون verdict واضحًا:

```text
IMPLEMENTED_STATIC_BUT_PREDECESSOR_BLOCKED
handoff69Authorized = false
```

إلى أن تُغلق 67 ويعاد تشغيل regression gates.

هذا يمنع سلسلة PASS وهمية.

---

# 191. Handoff إلى69

`handoff69Authorized = true` فقط إذا:

```text
predecessorGateSatisfied = true
staticGatesPassed = true
Room = 15
atomicMutationCoverage complete
Outbox scope complete
lease separation complete
logout isolation gates pass
v67 foundations not regressed
newV68WaiverCount = 0
```

إذا runtime blocked بيئيًا يمكن handoff مشروط فقط وفق سياسة المشروع، مع توثيق `NOT_RUN` بدقة.

---

# 192. ما يجب أن تستلمه69

```text
AutoDrive-v68-atomic-transactional-outbox.zip
SESSION_68_FINAL.md
AUTODRIVE_SYNC_VERIFICATION_v68.json
AUTODRIVE_SYNC_VERIFICATION_v68.md
Room 15 schema evidence if genuinely generated
static/model verifiers
```

وبالتالي يجب ألا تعيد69 بناء:

```text
atomic entity+Outbox transaction
principal-scoped Outbox
lease separation
basic logout queue isolation
```

---

# 193. Work intentionally deferred after68

يبقى مؤجلًا:

```text
full server idempotent receipt protocol
Durable Inbox
Realtime hint-only conversion
10k chat recovery/pagination
media durable transfer queue
complete session isolation across every cache/job/callback
Dead Letter recovery UX
Unified Change Feed
Safe Bootstrap/CURSOR_EXPIRED recovery
Anti-Entropy
final observability
full fault injection campaign
```

68 لا تدّعي حل هذه المساحات.

---

# 194. Implementation order — إلزامي

```text
1. Verify v67 ZIP hash and extract clean baseline.
2. Read v67 verification JSON/MD and enforce predecessor handoff gate.
3. Freeze critical-file fingerprints.
4. Inventory every PendingOperationEntity field/query/call-site.
5. Inventory four required local mutation families + notification read compatibility path.
6. Design canonical scoped Outbox row and deterministic legacy mapping.
7. Add Room 15 entity/schema changes.
8. Add MIGRATION_14_15 without destructive fallback.
9. Make PendingOperationDao fully scope-aware.
10. Separate leaseUntil from nextRetryAt.
11. Add reusable transactional mutation boundary/helper only if it reduces duplicated correctness logic.
12. Convert Profile to commit Entity + Outbox before network.
13. Convert Withdrawal to commit Entity + Outbox before RPC.
14. Convert Chat Send envelope to commit local state + Outbox before send.
15. Convert Chat Read Receipt to durable Outbox.
16. Convert Notification Read to the same Outbox authority; remove standalone unsynced sender authority.
17. Refactor OutboxSynchronizer/Processor to scoped due/claim/send/finalize.
18. Update Worker to require/resolve exact current SyncScope and fail closed when stale/missing.
19. Implement logout quiesce/cancel + scoped local cleanup transaction.
20. Add cross-account and stale-callback guards.
21. Preserve v67 cursor, tombstone, push-before-pull, generation behavior.
22. Add deterministic static/model fixtures.
23. Add Room/unit tests where environment permits.
24. Run static verifier twice.
25. Run build/unit/android gates if available.
26. Produce verification JSON/MD and diff inventory.
27. Package clean v68 ZIP.
28. Extract final ZIP fresh and replay static verifier.
29. Generate SHA-256 artifacts.
```

---

# 195. Pre-implementation questions answered from code, not guesses

المنفذ يجب أن يسجل إجابات فعلية قبل mutation:

```text
Q1  ما كل call-sites التي تنشئ PendingOperationEntity؟
Q2  ما كل call-sites التي تعدل local syncable state دون Outbox؟
Q3  ما active statuses التي تمثل intent غير محسوم؟
Q4  ما exact existing idempotency keys للProfile/Withdrawal؟
Q5  ما identity المستقرة للChat message؟
Q6  ما granularity الفعلية لـRead Receipt؟
Q7  هل markAllRead يعمل per-row أم batch؟
Q8  ما كل global PendingOperationDao queries الحالية؟
Q9  كيف يمثل v67 lease حاليًا؟
Q10 ما ترتيب signOut الحالي؟
Q11 ما البيانات التي يمسحها LocalDataCleaner؟
Q12 هل worker يمكن أن يبدأ بلا CurrentSession كامل؟
Q13 ما callbacks التي يمكن أن تعود بعد logout؟
Q14 ما legacy rows الممكنة في pending_operations v14؟
Q15 أي legacy row لا يمكن إثبات owner له؟
```

أي إجابة جوهرية مجهولة تمنع implementation assumption الصامت.

---

# 196. Required final acceptance questions

قبل packaging يجب الإجابة `YES` على كل التالي، أو تصنيف runtime-only blocker بوضوح:

```text
Q1  هل predecessor handoff gate مُغلق/مصرح؟
Q2  هل source ZIP هو v67 المحدد؟
Q3  هل Room baseline = 14؟
Q4  هل target Room = 15؟
Q5  هل MIGRATION_14_15 append-only وغير destructive؟
Q6  هل كل Outbox row لها userId/clientId/orgId؟
Q7  هل operationId ثابت؟
Q8  هل mutationId ثابت؟
Q9  هل entityType/entityId صريحان؟
Q10 هل operationType typed/stable؟
Q11 هل contractVersion موجود؟
Q12 هل leaseUntil منفصل عن nextRetryAt؟
Q13 هل كل due query scoped؟
Q14 هل كل claim scoped؟
Q15 هل كل finalize/delete scoped؟
Q16 هل Profile entity+Outbox ذرية؟
Q17 هل Withdrawal entity+Outbox ذرية؟
Q18 هل Chat Send envelope+Outbox ذري؟
Q19 هل Chat Read Receipt+Outbox ذري؟
Q20 هل Notification Read+Outbox ذري؟
Q21 هل standalone notification sender authority أزيلت؟
Q22 هل لا يوجد Pending محلي بلا matching Outbox؟
Q23 هل network خارج Room transaction؟
Q24 هل cancellation لا تحول intent إلى success؟
Q25 هل legacy rows migrated أو fail-closed؟
Q26 هل scope A لا يرى/يطالب/finalize B؟
Q27 هل logout يمنع claims جديدة؟
Q28 هل active old-scope callbacks ممنوعة بعد logout؟
Q29 هل cleanup scoped ولا يستخدم deleteAll global؟
Q30 هل login B لا يرى queue A؟
Q31 هل v67 cursor semantics لم تتراجع؟
Q32 هل v67 generation semantics لم تتراجع؟
Q33 هل Realtime rewrite لم تُسحب مبكرًا؟
Q34 هل media queue redesign لم تُسحب مبكرًا؟
Q35 هل server idempotency redesign لم يُسحب مبكرًا؟
Q36 هل historical SQL mutation count = 0؟
Q37 هل production UI files changed = 0؟
Q38 هل newV68WaiverCount = 0؟
Q39 هل كل NOT_RUN موثق بصدق؟
Q40 هل archive النهائي اجتاز fresh-extract replay؟
```

أي `NO` correctness غير runtime-only يمنع PASS.

---

# 197. Required implementation report truth table

التقرير النهائي يجب أن يفرّق صراحة بين:

```text
IMPLEMENTED
STATIC_VERIFIED
COMPILED
UNIT_TESTED
ANDROID_MIGRATION_TESTED
SERVER_E2E_TESTED
PREDECESSOR_GATE_SATISFIED
```

لا تستخدم كلمة `PASS` كمظلة تخفي `NOT_RUN`.

---

# 198. Packaging

اسم archive المستهدف:

```text
AutoDrive-v68-atomic-transactional-outbox.zip
```

ويحتوي على الأقل:

```text
modified source tree
SESSION_68_FINAL.md
AUTODRIVE_SYNC_VERIFICATION_v68.json
AUTODRIVE_SYNC_VERIFICATION_v68.md
static/model verifier(s)
Room 15 schema evidence if genuinely generated
```

---

# 199. Output SHA-256

بعد packaging:

```text
sha256sum AutoDrive-v68-atomic-transactional-outbox.zip
```

وينتج:

```text
AutoDrive-v68-atomic-transactional-outbox.zip.sha256
```

ويفضل كذلك:

```text
SESSION_68_FINAL.md.sha256
```

---

# 200. Archive integrity

يجب:

```text
unzip -t AutoDrive-v68-atomic-transactional-outbox.zip
```

ثم:

```text
extract into clean directory
run verify-v68-static on extracted tree
compare semantic result with working-tree verifier result
```

Packaging drift = FAIL.

---

# 201. No generated junk

ممنوع تضمين:

```text
.gradle caches
build/ directories غير اللازمة
IDE caches
local.properties الحقيقي
keystores
Supabase secrets
service-role keys
access/refresh tokens
runtime DB dumps containing user data
```

---

# 202. Secret scan

قبل packaging افحص:

```text
service_role
JWT secret
access token
refresh token
password
OTP
bank account dumps
raw profile payloads
raw Outbox payload dumps
```

أي secret:

```text
BLOCKED_SECRET_LEAK
```

---

# 203. Final static replay

الترتيب الإلزامي:

```text
1. verify working tree
2. verify second time for determinism
3. package ZIP
4. unzip -t
5. extract fresh
6. verify extracted ZIP
7. compare semantic JSON
```

كل gates يجب أن تعطي نفس verdict دلاليًا.

---

# 204. Expected architectural state after68

المسار المستهدف:

```text
User local action
    ↓
Capture immutable SyncScope
    ↓
Room.withTransaction
    ├─ mutate local entity/state
    └─ insert scoped Outbox row
    ↓ COMMIT
Network send may start
    ↓
Scoped claim + separate lease
    ↓
Server attempt
    ↓
Scoped atomic/local acknowledgement or retry transition
```

وعند crash بعد commit:

```text
Outbox survives → retry/recovery remains possible
```

---

# 205. What 68 changes conceptually

قبل 68 في v67:

```text
local state and durable outbound intent are separate events
Outbox owner is implicit
lease and retry time share one field
Chat/read paths bypass unified Outbox
logout clears pending operations globally
```

بعد 68:

```text
local syncable intent = one Room transaction
Outbox owner is explicit
lease != retry schedule
required mutation families share one durable outbound authority
logout cannot execute previous principal's queue
```

---

# 206. Final verdict for this contract

هذا المستند **عقد تنفيذ فقط**.

بناءً على v67 المرفق حاليًا:

```text
SESSION_67 finalVerdict = BLOCKED_SERVER_TOMBSTONE_CONTRACT
handoff68Authorized = false
```

لذلك حالة البدء الصحيحة لـ68 هي:

```text
CONTRACT_READY
EXECUTION_BLOCKED_BY_PREDECESSOR_HANDOFF
```

حتى يتحقق أحد التالي:

```text
A) إغلاق 67 رسميًا وتصبح handoff68Authorized=true
أو
B) override صريح من المستخدم يسمح بتنفيذ 68 رغم predecessor blocker، مع بقاء verdict غير قابل للترقية إلى chain PASS حتى إغلاق 67.
```

---

# 207. الخلاصة النهائية للعقد

جلسة68 لا تنجح بمجرد إضافة أعمدة إلى `pending_operations`.

تنجح فقط إذا تحولت الكتابة المحلية من:

```text
Entity write
→ network attempt
→ maybe enqueue later
```

إلى:

```text
Scoped local mutation
→ Room transaction(Entity + Outbox)
→ durable commit
→ scoped claim/send/retry
```

لـ:

```text
Profile
Withdrawal
Chat Send
Chat Read Receipt
Notification Read compatibility path الموجود فعليًا في v67
```

مع:

```text
Room 14→15
explicit user/client/org ownership
stable operationId + mutationId
entityType + entityId
separate leaseUntil
no global Outbox queries
logout-safe queue ownership
v67 cursor/generation foundations preserved
zero new waivers
```

والقاعدة النهائية:

```text
If a syncable local intent can exist without its matching scoped Outbox row,
or if an old account can still claim/finalize that row,
Session 68 is not complete.
```

---

# END OF SESSION_68_FINAL.md
