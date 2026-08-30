# SESSION_67_FINAL.md

## AutoDrive Sync Modernization — Session 67

### Sync Safety Foundation — Tombstones, Durable Scoped Cursor, Atomic Apply, Push-Before-Pull & Generation-Safe Hints

**نوع المستند:** عقد تنفيذ مستقل وصارم للجلسة الأولى من خطة مزامنة AutoDrive v67→v73  
**الجلسة:** 67  
**الحالة:** `PLAN ONLY — EXECUTABLE ON AUTODRIVE-v66; FINAL PASS REQUIRES VERIFIED TOMBSTONE/CURSOR SERVER CONTRACT`  
**تاريخ الصياغة:** 2026-08-21  
**مصدر الكود المفحوص:** `AutoDrive-v66.zip`  
**SHA-256 للمصدر المفحوص:** `d61fb5c0c44e7b5eb2341589faedc3dd6f3fe3e2aad7e6639d734663c35fa9e8`  
**Archive entries:** `1250`  
**Production Kotlin files:** `251`  
**Test Kotlin files:** `45`  
**Room الحالي:** `13`  
**Room المستهدف في 67:** `14`  
**الخطة الأم:** `AUTODRIVE_SYNC_MODERNIZATION_REPAIR_PLAN_v67-v73.md`  
**SHA-256 للخطة:** `7442530be1c861e2a52d90211eea26d1df51a80f7d6e5ceac7df8fd049c73b8a`  
**مرجع الصرامة والبنية:** `SESSION_313_FINAL.md`  
**SHA-256 لمرجع الصرامة:** `7a420fab0082b3fce37340727ae010b457b485567bbd7a46541c8fc6b05a7bd9`  
**SESSION_314_FINAL.md:** سياق إضافي للصرامة فقط؛ لا تُستورد منه أي semantics خاصة بـVerto.  
**ملف SHA-256 المرفق لـ313:** **مُتجاهَل بالكامل حسب طلب المستخدم**؛ ليس Authority ولا Gate ولا Input.  
**v66 static verdict:** `STATIC_ZERO_DRIFT_COMPLETE / FINAL_RUNTIME_BLOCKED`  
**v66 runtime blocker:** `ENVIRONMENT_NETWORK_BOOTSTRAP_BLOCKER — UnknownHostException: services.gradle.org`  
**Gradle runtime/build/tests في v66:** `NOT_RUN`  

---

# 0. الحكم التنفيذي المختصر

67 ليست جلسة تحسين شكلية ولا إعادة تسمية للمزامنة.

هي جلسة إغلاق خمس فجوات correctness مثبتة في v66:

```text
1) DeletionFeed موجود كـ extension point فقط ولا يدخل الإنتاج.
2) SyncCheckpoint موجود في الذاكرة فقط وغير durable وغير مستخدم إنتاجيًا.
3) دورة المزامنة الحالية Pull أولًا ثم Outbox Push.
4) Remote apply الحالي موزع على DAO calls مستقلة بلا Room transaction جامعة مع cursor.
5) requestSync() الحالي single-flight يشارك نفس الرحلة ولا يسجل Generation لاحقة للـhint الجديد.
```

الحالة المستهدفة بعد 67:

```text
Recover expired Outbox leases
        ↓
Push existing Outbox
        ↓
Pull current positive-row state + authoritative tombstone delta
        ↓
Atomic Room apply
(upserts/deletes + cursor)
        ↓
Pending-local reconciliation
        ↓
Generation drain check
        ↓
Repeat once/more while requestedGeneration > completedGeneration
```

القواعد المطلقة:

```text
No authoritative tombstone contract = no deletion PASS.
```

```text
No durable scoped cursor = no v67 PASS.
```

```text
Cursor advance outside the same Room transaction as its apply = failure.
```

```text
Pending local intent must never be silently overwritten by Pull.
```

```text
A hint arriving during an active sync must cause the active owner to observe
requestedGeneration > completedGeneration and service another generation.
```

```text
Do not fabricate a cursor for legacy bounded/full reads.
```

---

# 1. بوابة البداية — Baseline Gate

قبل أي تعديل يجب تثبيت الآتي من `AutoDrive-v66.zip` نفسه:

```text
ZIP SHA-256                                  = d61fb5c0...
archive entries                              = 1250
production Kotlin                            = 251
Room version                                 = 13
v66 static verdict                           = STATIC_ZERO_DRIFT_COMPLETE
v66 runtime                                  = BLOCKED at Gradle bootstrap
DeletionFeed production implementation       = absent
SyncCheckpoint durable persistence            = absent
requestedGeneration                          = absent
completedGeneration                          = absent
Room.withTransaction in production sync      = absent
sync_tombstones production adapter           = absent
Sync order                                   = Pull → Outbox Push → Realtime restart
Outbox expired-lease release                 = present inside PendingOperationProcessor.flush()
Realtime direct Room writes                  = present and intentionally deferred to v70
```

إذا اختلف source ZIP أو أحد fingerprints الحرجة دون توثيق:

```text
BLOCKED_INPUT_DRIFT
```

إذا تغير Room عن 13 قبل التنفيذ:

```text
BLOCKED_ROOM_BASELINE_DRIFT
```

إذا كانت v66 تحتوي تعديلات sync غير موثقة مقارنة بالمصدر المفحوص:

```text
BLOCKED_SYNC_BASELINE_DRIFT
```

---

# 2. ترتيب السلطات — Authority Order

عند التعارض، يكون الترتيب:

1. `AutoDrive-v66.zip` ذو SHA المثبت أعلاه.
2. `AUTODRIVE_SYNC_MODERNIZATION_REPAIR_PLAN_v67-v73.md`.
3. `docs/autodrive-server-contract-v45.md` فيما يخص Android/server sync contract التاريخي.
4. كود v66 الفعلي داخل `core/sync`, `core/database`, `core/network`, والـfeature boundaries المتأثرة.
5. `AutoDrive-v66-report.md` و`V66_BLOCKED_REPORT.md` لحقيقة runtime/static الخاصة بـv66.
6. هذا العقد.
7. `SESSION_313_FINAL.md` **كمرجع صرامة فقط** لا كمرجع معماري لـAutoDrive.
8. `SESSION_314_FINAL.md` **كمرجع تنسيق ثانوي فقط**.

ممنوع استيراد أسماء جداول أو RPCs أو revisions أو contracts من Verto إلى AutoDrive لمجرد ظهورها في عقد 313/314.

---

# 3. بصمات Authority عند البداية

يجب على المنفذ تسجيل هذه البصمات قبل mutation:

```text
AutoDrive-v66.zip
  d61fb5c0c44e7b5eb2341589faedc3dd6f3fe3e2aad7e6639d734663c35fa9e8

AUTODRIVE_SYNC_MODERNIZATION_REPAIR_PLAN_v67-v73.md
  7442530be1c861e2a52d90211eea26d1df51a80f7d6e5ceac7df8fd049c73b8a

core/sync/.../RemoteSyncSemantics.kt
  c386facf32947775a68e81281ecf81ee3b554e8c55d9da9a66c0b8be11cb88db

core/sync/.../DefaultSyncCoordinator.kt
  57a334b51b73177b48eb9d3871195c8a8e78e121de754ad2e4b1f42552b96a97

core/sync/.../SyncManager.kt
  422d8ff7ee86a7415292119a0130da8b6955e0ebd8449578c07488dfdf6a77e0

core/sync/.../OutboxSynchronizer.kt
  0d0c110782eb7cc98af681d3a1a945a0ea731312b165e3499893f6f848c7094a

core/sync/.../PendingOperationProcessor.kt
  5e31740c5c98b130e56aeffeb53c053bced6806e92eb881fb0aaabd0f1358f21

core/database/.../Entities.kt
  e82143d146997543ee720ec8f04aa36780412793069a396e3b67efba17e9f7d0

core/database/.../PendingOperationDao.kt
  bc4f20041b0e238db82b500bb08ad5f3564c8a41e4582ba447d342059a910d66

core/database/.../AutoDriveDatabase.kt
  2321cfb8bfb5cddb92b8a9cc5a4598111dc4e847a65b0ac95ae562de2ef4beda

core/session/.../CurrentSession.kt
  35925f92637ba39678a526f3d8e3d2e2e84fcc38f694df1a22d2fe05f6a95900

app/src/test/.../DefaultSyncCoordinatorTest.kt
  5665c69b8542d3f35ad1534aeaa58b2ae81f5d16d572224e305d2c0cc81645ca

docs/autodrive-server-contract-v45.md
  083b086f1ed1949f24f0837105face1477738e3b723903ce1e2bbf986a924819

AutoDrive-v66-report.md
  3dd9752becc61e8e687dc38f9cc3eb80ec63a3346782ba9dc4e3b4b0de27e58f

V66_BLOCKED_REPORT.md
  7515a629dc9d70439e8d8b98e3a07a42db254232e2a3a9bfbef11c9f8f4da7c2
```

هذه fingerprints هي prestate evidence وليست شرطًا أن تبقى الملفات دون تعديل؛ المطلوب أن يكون أي اختلاف بعد التنفيذ داخل scope ومفسرًا في report.

---

# 4. حقيقة v66 المثبتة من الفحص

## 4.1 DeletionFeed

`RemoteSyncSemantics.kt` يحتوي:

```text
DeletionFeed<T>
DeletionBatch<T>
RemoteSyncChange.ExplicitDeletion
SyncCheckpoint
```

لكن لا يوجد production implementation أو call site لـ`DeletionFeed`.

الحكم:

```text
DEFINED != WIRED
```

## 4.2 SyncCheckpoint

الحالي:

```text
internal class SyncCheckpoint(private var committed: String? = null)
```

هو:

```text
in-memory
not scoped
not persistent
not crash-safe
not process-death-safe
not production-wired
```

لا يجوز إبقاؤه كـauthority بعد 67.

## 4.3 ترتيب المزامنة

`DefaultSyncCoordinator.execute()` يفعل حاليًا:

```text
engine.synchronize()       // Remote → Room pulls
engine.flushPendingOperations() // Outbox push afterwards
realtimeController.restart()
```

هذا يخالف خطة 67.

## 4.4 Leases

`PendingOperationProcessor.flush()` يفعل:

```text
dao.releaseExpiredClaims(now)
dao.getDue(now, limit)
claim(...)
send(...)
```

إذًا lease recovery موجود، لكن مخفي داخل flush وليس مرحلة orchestration مستقلة قابلة لإثبات ترتيبها.

## 4.5 requestSync single-flight

الحالي:

```text
activeSync ?: new CompletableDeferred
non-owner => shared.await()
```

إذا وصل `FCM_HINT` أو `REALTIME_HINT` أثناء active sync، الطلب الثاني ينتظر الرحلة نفسها ولا يسجل ضرورة دورة لاحقة.

الحكم:

```text
single-flight = present
generation-drain = absent
hint-after-stage safety = absent
```

## 4.6 Atomic apply

لا يوجد `Room.withTransaction` في production sync path الحالية.

الـDAO writes تقع على خطوات منفصلة.

الحكم:

```text
batch apply atomicity = absent
cursor/apply atomicity = impossible in v66 because cursor persistence is absent
```

## 4.7 Pending local writes الموجودة فعليًا

v66 يملك على الأقل:

```text
Profile update:
  AutoDriveUserEntity.syncStatus = PENDING
  PendingOperationEntity(operation = UPDATE_PROFILE)

Withdrawal ambiguous result:
  WithdrawalRequestEntity.syncStatus = PENDING
  PendingOperationEntity(operation = REQUEST_WITHDRAWAL_RPC)

Notification read:
  NotificationEntity.readSynced = false
  direct outbound reconciliation before current pending-operation processor
```

هذه يجب ألا يدهسها pull في 67.

## 4.8 Realtime

بعض participants ما زالت تكتب مباشرة إلى Room.

هذا **مثبت لكنه ليس scope 67**؛ إصلاحه محجوز لـ70.

67 مسؤولة فقط عن:

```text
أي hint يصل إلى SyncCoordinator يجب ألا يضيع أثناء active sync.
```

---

# 5. الفجوة الخاصة بمخطط sync_tombstones

الخطة الأم تنص صراحة على:

```text
ربط sync_tombstones بالتنفيذ الإنتاجي وتفعيل DeletionFeed.
```

لكن `AutoDrive-v66.zip` لا يحتوي schema أو migration يثبت البنية الفعلية الحالية لـ`sync_tombstones`.

الموجود في `docs/autodrive-server-contract-v45.md` هو contract دلالي فقط:

```text
stable row identity
scope/tenant identity
deletion ordering
checkpoint/cursor safe for resume
```

لذلك:

```text
ممنوع اختراع أسماء أعمدة sync_tombstones.
```

قبل كتابة production adapter يجب توفير واحد من التالي:

```text
A) schema.sql حديث موثوق، أو
B) migration الأصلية التي أنشأت sync_tombstones، أو
C) live introspection موثق من PostgreSQL/Supabase، أو
D) RPC contract رسمي يعزل Android عن شكل الجدول.
```

إذا لم يتوفر أي منها:

```text
BLOCKED_SERVER_TOMBSTONE_CONTRACT
```

يمكن تجهيز local abstractions/tests، لكن لا يجوز إعلان v67 مكتملة.

---

# 6. الهدف الدقيق لـ67

عند نهاية 67 يجب أن يثبت الكود:

```text
1. deletion feed production-wired.
2. cursor durable in Room.
3. cursor key fully scoped by userId/clientId/orgId/stream.
4. remote apply and cursor commit atomic.
5. push happens before pull.
6. expired leases are recovered before push.
7. current pending local intent survives pull.
8. tombstone cannot silently resurrect through stale local state.
9. hint during active sync creates pending generation.
10. final coordinator drains requested generations before completing shared flight.
```

---

# 7. ما ليست عليه 67

ممنوع توسيعها إلى 68–73.

ليست 67:

```text
- Transactional Outbox لكل local mutation في التطبيق.       -> 68
- إضافة userId/clientId/orgId/entityType/entityId لكل Outbox row. -> 68
- logout atomic isolation الكامل.                          -> 68
- Unified idempotent server command protocol.               -> 69
- typed retry taxonomy النهائية.                            -> 69
- durable Inbox.                                            -> 70
- إزالة كل Realtime → Room direct write.                    -> 70
- Chat 10k pagination.                                      -> 71
- media durable transfer queue.                             -> 71
- Unified Change Feed لكل entities.                         -> 72
- global monotonic server revision لكل النظام.              -> 72
- CURSOR_EXPIRED bootstrap/rebootstrap الكامل.              -> 72
- anti-entropy manifest/digests النهائي.                    -> 72
- fault injection الشامل والـmetrics النهائية.              -> 73
```

أي تنفيذ لهذه البنود داخل 67 بلا ضرورة مباشرة لإغلاق correctness gate يعتبر:

```text
BLOCKED_SCOPE_DRIFT
```

---

# 8. Runtime policy

بسبب baseline v66:

```text
Gradle bootstrap = BLOCKED by network
```

لذلك 67 تميز بين:

```text
67-S = static/model/database-contract implementation proof
67-R = build/unit/instrumentation/live-server proof
```

يجوز إصدار:

```text
PASS_STATIC / RUNTIME_BLOCKED_ENVIRONMENT
```

فقط إذا:

```text
- كل static/model gates ناجحة.
- لا يوجد server-contract ambiguity.
- migration 13→14 structurally verified.
- no new waiver.
- التقرير يقول صراحة إن build/runtime لم يُنفذا.
```

لا يجوز في هذه الحالة ادعاء:

```text
- APK builds.
- Room migration ran on device.
- live sync_tombstones query succeeded.
- process death was reproduced on Android runtime.
- RLS verified live.
```

---

# 9. Room schema policy

67 تحتاج durable cursor؛ لذلك Room migration مطلوبة.

```text
current = 13
target  = 14
```

ممنوع:

```text
fallbackToDestructiveMigration()
destructive reset
schema version jump > 14
changing unrelated existing columns
rewriting pending_operations schema (reserved mainly for 68)
```

---

# 10. Migration 13→14 — الحد الأدنى الإلزامي

يجب إضافة جدول واحد canonical على الأقل:

```text
sync_cursors
```

الشكل المحلي المطلوب دلاليًا:

```text
user_id          TEXT NOT NULL
client_id        TEXT NOT NULL
org_id           TEXT NOT NULL
stream           TEXT NOT NULL
cursor_token     TEXT NOT NULL
contract_version INTEGER NOT NULL DEFAULT 1
updated_at       INTEGER NOT NULL
```

المفتاح الأساسي المركب:

```text
PRIMARY KEY(user_id, client_id, org_id, stream)
```

`updated_at` للتشخيص فقط.

ممنوع استخدامه في correctness أو ordering.

---

# 11. SyncCursorEntity contract

يجب أن يوجد model واضح، مثال تسمية مسموحة:

```text
SyncCursorEntity
```

ويحمل بالضبط معنى:

```text
principal scope + logical stream + opaque server cursor
```

لا يجوز أن يحمل:

```text
access token
refresh token
raw auth session
phone
bank data
payload snapshots
```

---

# 12. SyncCursorDao contract

يجب دعم:

```text
get(scope, stream)
upsert(cursor)
deleteForScope(scope)     // يمكن أن يبقى غير مستخدم حتى 68
countForScope(scope)      // للاختبارات/التشخيص إن لزم
```

كل lookup/update يجب أن يمر بجميع عناصر scope:

```text
userId + clientId + orgId + stream
```

ممنوع:

```text
getByStream(stream) فقط
getByUserId(userId) فقط
shared singleton cursor
DataStore cursor غير scoped
```

---

# 13. Cursor opacity

Android لا يفسر cursor.

الصحيح:

```text
String/opaque token in Android
server owns ordering meaning
```

الممنوع:

```text
cursor = System.currentTimeMillis()
cursor = entity.updatedAt
cursor = local clock ISO string
cursor = max(created_at) computed on device
cursor = hash without server resume semantics
```

---

# 14. Cursor monotonicity contract

إذا كان server cursor قابلًا للمقارنة عدديًا، يمكن server adapter التحقق من عدم الرجوع.

لكن core Android يجب ألا يعتمد على معرفة نوعه.

الحد الأدنى:

```text
nextCursor from server may replace current cursor only after successful atomic apply.
```

إذا أعاد server cursor أقدم/غير صالح وفق عقده:

```text
CURSOR_REGRESSION
```

ولا يحدث commit.

---

# 15. Stream identity

`stream` يجب أن يكون stable key مملوكًا للعقد، لا اسم class عشوائي.

مسموح في 67:

```text
one authoritative tombstone stream
or multiple explicit tombstone streams
```

حسب server contract الحقيقي.

الممنوع:

```text
UUID جديد لكل run
screen name
Coroutine name
current phase label المتغير
```

إذا كان server يملك feed واحدًا عالميًا للحذف، stream واحد مثل:

```text
core-tombstones-v1
```

يكفي من ناحية scope.

لا يجوز اختراع 10 cursors مستقلة إذا server ordering لا يدعم ذلك.

---

# 16. SyncScope contract

يجب إدخال value object واضح، مثال:

```text
SyncScope(
  userId,
  clientId,
  orgId,
)
```

يُشتق مرة في بداية cycle من `CurrentSession`.

بما أن `CurrentSession` الحالي يحتوي الثلاثة، فلا حاجة لتغيير session schema.

إذا نقص أحدها:

```text
SKIPPED_MISSING_SYNC_SCOPE
```

ولا يجوز إنشاء cursor ناقص scope.

---

# 17. Scope immutability داخل cycle

في بداية كل generation:

```text
scopeAtStart = currentSession scope
```

قبل أي atomic apply/cursor commit:

```text
currentScope == scopeAtStart
```

إذا تغيرت الجلسة:

```text
STALE_SYNC_SCOPE
```

ويجب:

```text
- عدم commit cursor
- عدم إكمال apply المتبقي
- إرجاع failure/skip واضح
```

العزل الكامل لـlogout callbacks مؤجل لـ68، لكن 67 لا يجوز أن تكتب cursor لحساب غير الحالي.

---

# 18. DeletionFeed production activation

الـinterface الحالي يمكن تعديله أو استبداله، لكن يجب أن يصبح له production implementation فعلي.

المطلوب:

```text
server adapter -> canonical deletion batch -> atomic local applier -> durable cursor
```

وجود interface غير مستخدمة بعد التنفيذ = FAIL.

Gate:

```text
deletionFeedProductionCallSiteCount >= 1
```

---

# 19. Canonical deletion model

Android-side canonical model يجب أن يحمل دلاليًا:

```text
event/tombstone identity
entity type
target entity id
principal scope or enough fields to validate it
batch/ordering cursor semantics
```

الأسماء الفعلية ليست مفروضة.

مثال دلالي فقط:

```text
DeletionEnvelope(
  eventId,
  entityType,
  entityId,
  scope,
)
```

`nextCursor` يفضل أن يكون على مستوى batch لا مستخرجًا من آخر element في client code.

---

# 20. Server tombstone field binding

لأن schema غير مرفق:

```text
field names MUST be discovered, not guessed.
```

الـadapter يمكنه mapping مثل:

```text
actual_server_column -> canonical Android field
```

لكن التقرير النهائي يجب أن يسجل:

```text
server source used
exact server fields consumed
scope predicate
ordering predicate
cursor resume predicate
retention fact if known
```

---

# 21. Server cursor requirement للحذف

`sync_tombstones` يجب أن يسمح resume deterministic.

المطلوب واحد من:

```text
monotonic sequence
server revision
authoritative opaque cursor RPC
```

غير المسموح:

```text
deleted_at alone as correctness cursor
created_at alone as correctness cursor
Android wall clock
```

إذا الجدول الحالي لا يملك أي resume-safe ordering، يجوز لـ67 إضافة **server migration محدودة للحذف فقط**.

هذا لا يُعد Unified Server Revision الخاص بـ72.

---

# 22. Server migration policy لـ67

يجوز إضافة migration واحدة فقط إذا كانت ضرورية لتمكين tombstone cursor الآمن.

اسمها يجب أن يحمل `v67` بوضوح.

مسؤوليتها القصوى:

```text
- expose/ensure safe tombstone ordering or cursor
- expose scope-safe read surface/RPC if needed
- preserve existing tombstones
- preserve existing delete triggers
- preserve retention behavior unless explicitly required and justified
```

ممنوع أن تتحول إلى:

```text
unified change feed for all upserts      -> 72
bootstrap snapshot protocol              -> 72
financial transaction-group revision     -> 72
```

---

# 23. Historical SQL integrity

ممنوع تعديل migrations السابقة داخل `supabase/migrations`.

القاعدة:

```text
append-only migration history
```

أي تعديل لملف SQL تاريخي:

```text
BLOCKED_HISTORICAL_MIGRATION_MUTATION
```

---

# 24. Tombstone scope validation

حتى مع RLS، Android يجب ألا يقبل tombstone غير متوافق مع scope الحالي إذا كانت scope fields متاحة في payload.

Gate:

```text
payloadScope == current SyncScope or documented server-projected subset with equivalent ownership proof
```

إذا mismatch:

```text
TOMBSTONE_SCOPE_MISMATCH
```

ولا apply ولا cursor commit.

---

# 25. Supported deletion entity registry

ممنوع تنفيذ delete باستخدام dynamic table name قادم من server.

يجب وجود allowlist/registry compile-time.

السطوح الحالية التي يجب تقييمها:

```text
autodrive_users
invoices
payments
commission_payments
marketer_balance
balance_transactions
withdrawal_requests
notifications
conversations
internal_messages -> chat_messages local mapping
```

ليس شرطًا أن server tombstone contract يدعم جميعها بنفس الاسم؛ المطلوب mapping صريح للمدعوم فعليًا.

---

# 26. No dynamic SQL

ممنوع:

```text
"DELETE FROM ${serverTable} WHERE id = ..."
```

المطلوب:

```text
when(entityType) -> known DAO deletion method
```

أي unknown entity type:

```text
UNSUPPORTED_TOMBSTONE_ENTITY
```

ولا cursor commit للbatch التي تحتويه.

---

# 27. Local DAO deletion coverage

معظم DAOs الحالية تملك delete-by-id بالفعل.

الملاحظة المهمة:

```text
ConversationDao lacks deleteById in v66.
```

إذا tombstone يدعم conversation deletion، يجوز إضافة:

```text
deleteById(id)
```

مع التعامل مع رسائل المحادثة محليًا حسب policy صريحة.

ممنوع حذف بيانات خارج target scope.

---

# 28. Delete idempotency

تطبيق نفس tombstone مرتين يجب أن يكون آمنًا.

```text
row exists -> delete
row absent -> success/no-op
```

ممنوع اعتبار `0 rows deleted` فشلًا إذا identity/scope صحيحة.

---

# 29. Absence is not deletion

يبقى invariant v45:

```text
absence from bounded/paginated/RLS-filtered query != deletion
```

67 لا يجوز أن تضيف أي:

```text
remoteIds vs localIds set-difference delete
```

خصوصًا للسطوح المحدودة:

```text
balance_transactions limit 50
withdrawal_requests limit 20
notifications limit 50
internal_messages limit 100
```

---

# 30. Legacy positive-row pull policy

v67 لا تدعي أنها حولت كل reads إلى delta.

الحقيقة المسموحة:

```text
legacy positive-row pulls remain compatibility reads
+ authoritative deletion delta is introduced
```

مصطلح `Pull Delta` في 67 يعني على الأقل:

```text
authoritative tombstone delta using safe server cursor
```

ولا يجوز fabricating cursor للlegacy pulls.

الـUnified upsert/delete revision feed مؤجل لـ72.

---

# 31. Remote fetch vs Room apply

Network fetch يجب أن يحدث خارج Room transaction.

الممنوع:

```text
open Room transaction
  -> await network
```

المطلوب:

```text
fetch remote page
validate page
open short Room transaction
  apply accepted local mutations/deletions
  commit cursor
close transaction
```

---

# 32. Atomic apply contract

لكل authoritative cursor page:

```text
db.withTransaction {
    validate current scope again
    apply accepted upserts/deletes for that page
    persist next cursor
}
```

إذا أي local operation يرمي exception:

```text
transaction rolls back
cursor remains previous
```

---

# 33. Cursor commit location

`SyncCursorDao.upsert(nextCursor)` لا يجوز استدعاؤه من:

```text
RemoteDataSource
Network DTO mapper
Coordinator before apply
finally block
post-transaction callback
```

يجب أن يكون داخل atomic apply boundary فقط.

---

# 34. Null/empty next cursor

إذا server contract يسمح `nextCursor = null` بمعنى no advancement، يجب الحفاظ على current cursor.

إذا null غير صالح وفق العقد:

```text
INVALID_CURSOR_BATCH
```

الممنوع:

```text
replace valid cursor with empty string silently
```

---

# 35. Crash قبل transaction

سيناريو:

```text
remote page fetched
process dies before Room transaction
```

النتيجة الصحيحة:

```text
old cursor remains
page is fetched again next time
```

---

# 36. Crash أثناء transaction

سيناريو:

```text
some deletes/upserts executed
process dies before transaction commit
```

النتيجة المطلوبة من SQLite/Room:

```text
all page writes rolled back
cursor not advanced
```

لا يكفي test يختبر cursor وحده؛ يجب نموذج يختبر entity + cursor كحزمة واحدة.

---

# 37. Crash بعد transaction commit

النتيجة:

```text
entities/deletes and cursor are both durable
same page must not be required for correctness
```

إذا server يعيد الصفحة، application يجب أن تبقى idempotent.

---

# 38. Cancellation semantics

`CancellationException` يجب أن يُعاد رميه.

ممنوع تحويل cancellation إلى:

```text
SUCCESS
PARTIAL_SUCCESS
cursor commit
```

داخل transaction cancellation تؤدي rollback.

---

# 39. Batch validation قبل apply

كل batch يجب أن يمر على validation قبل transaction أو في أولها:

```text
scope valid
entity types supported
entity ids nonblank
cursor shape accepted by remote contract
no impossible duplicate identity with contradictory operations in same batch unless server contract defines order
```

أي batch malformed:

```text
REMOTE_BATCH_VALIDATION_FAILED
```

---

# 40. Duplicate tombstones

نفس tombstone/event قد يظهر أكثر من مرة قبل durable Inbox في70.

67 يجب أن تكون آمنة بالـidempotent delete + cursor transaction.

لا حاجة لإضافة Inbox مبكرًا.

---

# 41. Pending local mutation protection — القاعدة

إذا local entity تحمل intent لم يُحسم بعد، remote positive-row pull لا يجوز أن يعيدها إلى remote state القديم.

القواعد الحالية يجب أن تعتمد على **existing v66 markers** قدر الإمكان، دون توسيع Outbox schema إلى68.

---

# 42. Profile pending guard

في v66:

```text
AutoDriveUserEntity.syncStatus == PENDING
PendingOperationEntity.operation == UPDATE_PROFILE
idempotencyKey == profile:<userId>
```

إذا profile محلي PENDING:

```text
remote profile upsert must not replace editable local fields with stale server values
```

المسموح:

```text
preserve local pending entity until push resolves
```

بعد نجاح push يصبح SYNCED ثم pull التالي يمكنه apply server canonical state.

---

# 43. Profile tombstone مع pending local update

بسبب عدم وجود conflict protocol النهائي حتى69/72:

إذا tombstone authoritative يصطدم بـ`UPDATE_PROFILE` active غير محسوم:

```text
do not silently delete local intent
do not advance the affected tombstone cursor past the unresolved conflict
surface PENDING_LOCAL_CONFLICT
```

Push-before-pull يجب أن يقلل حدوث الحالة.

ممنوع:

```text
delete entity + keep outbox row that may later recreate/update it silently
```

---

# 44. Withdrawal pending guard

`WithdrawalRequestDto` الحالي يحتوي:

```text
client_request_id
```

وهو مفتاح reconciliation الموجود أصلًا.

إذا remote withdrawal يحمل `client_request_id` مطابقًا لطلب محلي pending:

```text
reconcile temp local identity with committed server identity
```

ويجب ألا ينشأ duplicate local row.

أي mutation لـpending operation/withdrawal أثناء هذا reconciliation يجب أن تكون atomic إذا كانت ضمن نفس local correctness transition.

---

# 45. Notification read guard

في v66:

```text
readSynced=false
```

يعني local read intent لم يُؤكد.

إذا push فشل ثم pull أعاد remote `is_read=false`:

```text
local isRead=true must be preserved
readSynced=false remains pending
```

الممنوع:

```text
blind upsert that converts locally-read notification back to unread
```

---

# 46. Other sync_status markers

أي entity تحمل marker محلي PENDING في current schema يجب ألا تُحوّل تلقائيًا إلى SYNCED بواسطة generic remote mapper دون policy صريحة.

المنفذ يجب أن يسجل inventory لجميع:

```text
sync_status
read_synced
status=SENDING/FAILED/PENDING where it represents local intent
```

داخل sync surfaces الحالية.

---

# 47. No premature Outbox redesign

67 لا تضيف fields التالية إلى `PendingOperationEntity` إلا إذا كانت ضرورة لا يمكن تجنبها:

```text
userId
clientId
orgId
entityType
entityId
mutationId
contractVersion
leaseUntil field separate from nextRetryAt
```

هذه ملك 68/69.

الحل في67 يجب أن يتعامل مع schema الحالية ويترك التوحيد للجلسة التالية.

---

# 48. Recover leases — مرحلة أولى

يجب أن يكون ترتيب التنفيذ مثبتًا:

```text
recover expired IN_PROGRESS claims
before selecting/sending due operations
```

يمكن إعادة استخدام منطق v66 الحالي.

لكن يفضل جعله operation قابلة للاختبار صراحة، مثل:

```text
outboxSynchronizer.recoverExpiredLeases()
```

أو equivalent واضح.

---

# 49. Push Outbox — قبل Pull

بعد recovery مباشرة:

```text
push existing outbound intent
```

يشمل فقط ما تدعمه v66 حاليًا:

```text
notification read sync
UPDATE_PROFILE
REQUEST_WITHDRAWAL_RPC
```

لا توسعة coverage إلى chat/token/financial commands في67.

---

# 50. Push failure policy

فشل عملية outbound واحدة لا يبرر دهس local pending state بالـpull.

إذا outbox flush يرجع summary:

```text
PENDING/IN_PROGRESS/DEAD_LETTER unresolved operations stay protected
```

الـpull يمكن أن يستمر للبيانات المستقلة، بشرط pending guards.

فشل auth/scope يجب أن يوقف الدورة قبل remote apply للحساب الخطأ.

---

# 51. Outbox lease timing

v66 يستخدم wall clock لتوقيت retry/lease.

هذا مسموح في67 لأن:

```text
lease timing != sync correctness cursor
```

لكن ممنوع إعادة استخدام `next_retry_at` كـremote cursor.

---

# 52. SyncEngine contract بعد67

يجب إزالة inversion الحالي حيث coordinator يعمل pull ثم `flushPendingOperations()`.

مسموح أحد تصميمين:

```text
A) SyncEngine.synchronize() owns full ordered pipeline.
B) Coordinator owns explicit ordered phases via narrower SyncEngine methods.
```

لكن الاختبار يجب أن يثبت order الفعلي.

غير المقبول:

```text
method names changed while call order remains Pull → Push
```

---

# 53. Ordered cycle — العقد الإلزامي

الدورة المنطقية لكل generation:

```text
AUTH/SCOPE VALIDATION
→ RECOVER_LEASES
→ PUSH_OUTBOX
→ PULL_REMOTE_POSITIVE_STATE
→ PULL_TOMBSTONE_DELTA
→ ATOMIC_APPLY
→ RECONCILE_PENDING_LOCAL
→ REALTIME RESTART/ENSURE
→ GENERATION COMPLETE CHECK
```

الخطة المختصرة تقول:

```text
Recover leases → Push Outbox → Pull Delta → Apply → Reconcile
```

الـpositive-row legacy pull موثق هنا كمرحلة توافق مؤقتة قبل tombstone delta.

---

# 54. Reconcile في67 — التعريف المحدود

`Reconcile` في 67 لا يعني anti-entropy server manifest.

تعريفه المحدود:

```text
verify/preserve unresolved local intent after remote apply
reconcile committed withdrawal by client_request_id when discoverable
ensure pending markers were not cleared by pull
```

ممنوع الادعاء:

```text
full divergence detection
hash reconciliation
partition repair
```

هذه لـ72.

---

# 55. Generation model — الهدف

نريد:

```text
requestedGeneration >= completedGeneration
```

دائمًا.

كل `requestSync(reason)`:

```text
increments requestedGeneration
```

الـactive owner ينفذ cycle على snapshot من requested generation.

بعد cycle:

```text
completedGeneration = generation serviced by that cycle
```

إذا:

```text
requestedGeneration > completedGeneration
```

يجب تشغيل cycle إضافية قبل إغلاق shared flight.

---

# 56. requestedGeneration

الخصائص المطلوبة:

```text
monotonic in-process Long
synchronized/atomic under same coordinator lock discipline
incremented for every accepted requestSync()
not wall-clock based
not persisted as correctness state
```

process death لا يحتاج استرجاع generation؛ APP_START/network recovery paths هي recovery authority الحالية.

---

# 57. completedGeneration

يمثل:

```text
highest request generation that has been serviced by a completed sync attempt
```

ليس:

```text
last successful server revision
cursor
success count
```

إذا cycle فشلت، يمكن اعتبار generation serviced، لكن أي request وصل أثناءها يجب ألا يضيع ويجب أن يسبب follow-up إذا كان أعلى.

---

# 58. Single-flight بعد67

single-flight يبقى مرغوبًا.

لكن semantics تصبح:

```text
one active owner
many requesters
owner drains pending generations
```

وليس:

```text
one active cycle forever represents every future hint that arrived during it
```

---

# 59. Hint أثناء Push

Fixture إلزامي:

```text
cycle G1 is blocked in PUSH_OUTBOX
requestSync(REALTIME_HINT) arrives => requestedGeneration becomes G2
G1 finishes
coordinator observes G2 > completed G1
second cycle runs
```

PASS فقط إذا:

```text
syncCalls == 2
```

أو equivalent يثبت أن G2 خُدمت فعليًا.

---

# 60. Hint أثناء Pull

نفس invariant.

ممنوع أن يرجع requester الثاني بنتيجة G1 ثم يغلق owner بلا دورة جديدة.

---

# 61. Multiple hints burst

إذا وصل 100 hint أثناء G1:

```text
requestedGeneration may advance 100 times
```

لا يلزم 100 sync cycles.

يكفي coalescing إلى cycle لاحقة واحدة تغطي آخر generation observed عند بدايتها.

الهدف:

```text
no lost generation
no unbounded one-cycle-per-hint fanout
```

---

# 62. Race عند نهاية cycle

أخطر race:

```text
owner checks no pending generation
hint arrives
owner clears activeSync
```

يجب أن يكون check + active-flight transition تحت lock واحد بحيث إما:

```text
A) hint sees active owner and increments pending generation owner will drain
or
B) hint sees no owner and becomes owner of a new flight
```

لا توجد نافذة ثالثة تفقد الطلب.

---

# 63. Generation waiter semantics

كل caller يجب ألا يُعتبر مُخدومًا قبل:

```text
completedGeneration >= callerRequestedGeneration
```

يمكن استخدام shared deferred واحد ينتهي بعد drain الكامل، أو per-generation waiter؛ كلاهما مسموح.

الممنوع:

```text
non-owner always awaits only first active cycle regardless of its own generation
```

---

# 64. SyncReason semantics

يمكن للـowner حفظ آخر/أعلى reason لأغراض diagnostics.

لكن correctness لا يعتمد على reason priority.

كل reasons الحالية تبقى:

```text
APP_START
NETWORK_RESTORED
USER_REFRESH
FCM_HINT
REALTIME_HINT
LOGIN_SUCCESS
```

---

# 65. Realtime restart

لا تجعل Realtime restart شرطًا لنجاح convergence.

يبقى:

```text
Realtime = acceleration/hint
```

إذا restart فشل:

```text
sync data path can still be success/partial according to existing reporting
```

لكن generation accounting يجب أن يكتمل بصورة deterministic.

---

# 66. Realtime direct Room writes — protected defer

67 ممنوعة من حملة واسعة لإزالة هذه الكتابات:

```text
ChatRealtimeParticipant
NotificationsRealtimeParticipant
BalanceRealtimeParticipant
BillingRealtimeParticipant
```

هذه 70.

لكن أي call منها إلى:

```text
syncCoordinator.requestSync(REALTIME_HINT)
```

يجب أن يستفيد من generation-safe coordinator.

---

# 67. FCM hints

`AutoDriveFirebaseMessagingService` يطلب sync.

Fixture 67 يجب أن يغطي reason من نوع FCM أو generic hint أثناء active cycle.

لا حاجة لتعديل Firebase payload contract.

---

# 68. WorkManager impact

`PendingOperationsWorker` الحالي يشغل:

```text
syncEngine.flushPendingOperations()
```

بعد إعادة تشكيل `SyncEngine` يجب الحفاظ على worker functionality.

مسموح:

```text
worker -> recover leases + push outbox only
```

ولا يلزم أن يشغل full pull.

ممنوع كسر compile contract للworker دون تحديثه واختباره.

---

# 69. PresenceReporter

Presence ليست جزءًا من cursor/tombstone correctness.

يجوز بقاؤها بعد auth كما هي.

ممنوع أن يفشل touch presence فيمنع outbox/pull إذا كان سلوك v66 لا يفرض ذلك بلا مبرر.

---

# 70. SyncStepExecutor

يمكن الإبقاء عليه لخطوات legacy positive pulls.

لكن atomic cursor apply لا يجوز أن يعتمد على pattern:

```text
run step, catch exception, continue, then commit cursor anyway
```

أي cursor-bearing batch يجب أن يكون all-or-nothing محليًا.

---

# 71. Partial success semantics

يجوز استمرار `PARTIAL_SUCCESS` للـlegacy independent sections.

لكن:

```text
partial success must never advance a cursor for a failed cursor-bearing batch
```

تقرير failures يجب أن يميز على الأقل:

```text
legacy section failure
outbox failure
remote deletion batch failure
scope/cursor failure
reconciliation failure
```

دون تسريب بيانات حساسة.

---

# 72. Sensitive logging

يستمر استخدام redaction الموجود.

ممنوع logging لـ:

```text
full tombstone payload
phone
bank account
auth token
raw outbox payload
raw user/client/org combination إذا كان التقرير public artifact
```

مسموح diagnostics IDs مختصرة/hashed عند الحاجة.

---

# 73. Server error handling

67 لا تبني retry taxonomy النهائية لـ69.

لكن أخطاء tombstone/cursor يجب ألا تتحول إلى generic success.

الحد الأدنى لأكواد 67:

```text
SERVER_TOMBSTONE_CONTRACT_UNAVAILABLE
TOMBSTONE_SCOPE_MISMATCH
UNSUPPORTED_TOMBSTONE_ENTITY
REMOTE_BATCH_VALIDATION_FAILED
CURSOR_REGRESSION
CURSOR_APPLY_FAILED
STALE_SYNC_SCOPE
PENDING_LOCAL_CONFLICT
```

يمكن تغيير الأسماء، لا المعاني.

---

# 74. Room migration integrity

`MIGRATION_13_14` يجب أن:

```text
CREATE sync_cursors
preserve every existing v13 table/row
not rewrite pending_operations
not drop any table
not alter financial decimal representation
```

---

# 75. Exported Room schema

يجب إنتاج:

```text
core/database/schemas/com.autodrive.app.core.database.AutoDriveDatabase/14.json
```

ويجب أن يتطابق مع annotations.

غياب schema 14:

```text
FAIL_ROOM_SCHEMA_EXPORT_MISSING
```

إذا Gradle غير متاح ولا يمكن توليده آليًا، لا يجوز اختراع JSON يدويًا وادعاء أنه Room-generated.

في هذه الحالة:

```text
STATIC implementation may be complete
runtime/schema-export proof remains blocked
```

ويجب توثيق ذلك.

---

# 76. Migration tests

عند توفر Android/Gradle، يجب تحديث الاختبارات لتغطي:

```text
4 → 14 preserves historical data
13 → 14 preserves existing data
unsupported 3 still fails without destructive migration
sync_cursors exact primary key
scope A/B isolation
cursor upsert updates only exact scope+stream row
pending_operations preserved unchanged
```

---

# 77. Static migration model gate

لأن runtime قد يكون blocked، يجب إضافة static/model verifier يفحص على الأقل:

```text
AUTODRIVE_DATABASE_VERSION == 14
MIGRATION_13_14 exists
ALL_MIGRATIONS includes MIGRATION_13_14
sync_cursors entity registered
primary key scope fields present
no destructive migration fallback introduced
```

---

# 78. Deletion mapping tests

يجب وجود model/unit fixtures لكل entity type المدعوم فعليًا من server contract.

لكل type:

```text
known type + existing local row -> row deleted
known type + missing local row -> idempotent success
unknown type -> failure + cursor unchanged
scope mismatch -> failure + cursor unchanged
```

---

# 79. Offline deletion resurrection fixture

السيناريو الإلزامي:

```text
1. local Room contains row R.
2. device goes offline.
3. server deletes R and creates authoritative tombstone T.
4. device returns online.
5. outbox has no pending local mutation for R.
6. sync reads T.
7. atomic apply deletes R and advances cursor.
8. next sync does not recreate R from stale local state.
```

PASS:

```text
R absent
cursor advanced exactly with deletion commit
no outbound mutation generated from R
```

---

# 80. Pending local mutation vs pull fixture

Profile minimum:

```text
local profile = NEW_NAME + syncStatus=PENDING
outbox UPDATE_PROFILE exists
remote profile = OLD_NAME
push fails transiently
pull returns OLD_NAME
```

PASS:

```text
Room still NEW_NAME/PENDING
outbox still unresolved
```

---

# 81. Notification read fixture

```text
local notification isRead=true, readSynced=false
remote isRead=false
push fails
pull applies remote row
```

PASS:

```text
isRead=true
readSynced=false
```

---

# 82. Withdrawal reconciliation fixture

```text
local temp id = client_request_id C
outbox REQUEST_WITHDRAWAL_RPC(C)
server row exists with id=S and client_request_id=C
```

PASS:

```text
no duplicate local temp + server row pair
local state reconciles to server identity
outbox state reconciled according to existing semantics
```

---

# 83. Crash-before-cursor fixture

Model transaction fixture:

```text
apply row mutation
throw before cursor write/commit
```

PASS:

```text
row mutation rolled back
cursor old
```

---

# 84. Cursor-write failure fixture

```text
entity apply succeeds in transaction body
cursor DAO throws
```

PASS:

```text
entity apply rolls back
cursor old
```

---

# 85. Replay same page fixture

```text
apply page P + cursor C2
server replays P
```

PASS:

```text
no duplicate local effect
no resurrected delete
cursor remains valid
```

Inbox dedupe غير مطلوب حتى70.

---

# 86. Scope isolation fixture

أنشئ:

```text
scope A = userA/clientA/orgA
scope B = userB/clientB/orgB
same stream name
```

PASS:

```text
cursor A != cursor B independently
write A cannot update B
read A cannot return B
```

---

# 87. Scope-switch-before-commit fixture

```text
fetch under scope A
session changes to scope B
attempt apply
```

PASS:

```text
no A data applied after switch
no A cursor committed under B
STALE_SYNC_SCOPE surfaced
```

---

# 88. Pipeline order fixture

Fake components تسجل order.

المتوقع:

```text
AUTH
RECOVER_LEASES
PUSH_OUTBOX
PULL
APPLY
RECONCILE
```

أي:

```text
PULL before PUSH
```

= FAIL.

---

# 89. Generation fixture — one hint

```text
G1 starts
hint arrives mid-cycle -> G2 requested
G1 completes
G2 cycle executes
```

PASS:

```text
requestedGeneration == 2
completedGeneration == 2
executedCycles == 2
```

الأرقام مثال لبدء العداد من صفر؛ المهم العلاقة.

---

# 90. Generation fixture — burst

```text
G1 active
N hints arrive
```

PASS:

```text
no lost requested generation
follow-up cycles are coalesced
final completedGeneration == final requestedGeneration
```

لا تفرض عدد cycles = N.

---

# 91. Generation race fixture — completion edge

اختبار deterministic للنافذة بين:

```text
last pending check
and activeSync clear
```

يجب حقن synchronization barrier.

PASS:

```text
hint becomes either pending generation of current owner or owner of new flight
never disappears
```

---

# 92. Cancellation generation fixture

إذا owner coroutine أُلغي:

```text
shared waiters must not hang forever
active flight must be cleared safely
future request must be able to become owner
```

لا يجوز ترك:

```text
activeSync != null forever
```

---

# 93. Existing DefaultSyncCoordinatorTest regression

الاختبار الحالي الذي يثبت:

```text
concurrent requests share one active flight
```

يجب تحديث معناه.

بدل توقع دائمًا:

```text
syncCalls == 1
```

يجب التمييز:

```text
concurrent request before first cycle starts may coalesce into one cycle
request arriving after cycle snapshot/stage must create follow-up generation
```

الاختبار يجب أن يكون deterministic، لا يعتمد على sleep.

---

# 94. Outbox processor regression

يجب الحفاظ على:

```text
release expired claims
claim once
success -> mark/delete
cancellation -> release claim
retryable failure -> reschedule
permanent/dead-letter behavior الحالي
sensitive error redaction الحالي
```

67 لا تعيد كتابة retry policy.

---

# 95. Current outbox identity protection

لا يجوز كسر:

```text
profile idempotency key = profile:<userId>
withdrawal client_request_id stable identity
```

أي refactor orchestration يجب أن يحافظ عليهما.

---

# 96. Billing/payment ordering protection

v45 invariant يبقى:

```text
payments ownership is client_id, not local invoice existence
```

67 لا تعيد تقديم dependency على Invoice Room presence عند apply أو tombstone validation.

---

# 97. Bounded-window protection

التعليقات الحالية التي تقول إن bounded pull ليس authoritative snapshot يجب أن تبقى صحيحة في السلوك، لا فقط النص.

Static verifier يجب أن يبحث عن أي delete-by-absence جديد.

---

# 98. No timestamp cursor scan

Static gate يجب أن يفشل إذا وجد correctness cursor مبنيًا على:

```text
System.currentTimeMillis
Instant.now
OffsetDateTime.now
updatedAt
createdAt
syncedAt
```

داخل cursor-resume logic.

لا يشمل timestamps التشخيصية أو lease timing.

---

# 99. No ephemeral SyncCheckpoint authority

إما:

```text
remove SyncCheckpoint
```

أو تحوله إلى facade يستخدم durable store.

غير مسموح بعد67 أن يكون:

```text
private var committed: String?
```

هو authority الفعلية.

Gate:

```text
ephemeralCursorAuthorityCount = 0
```

---

# 100. Database transaction API

يجب استخدام:

```text
androidx.room.withTransaction
```

أو equivalent Room transaction API الصحيح للمشروع.

ممنوع hand-written transaction flags غير مرتبطة بـSQLite commit.

---

# 101. Transaction size

لا تفتح transaction حول كامل network sync.

الـtransaction تكون فقط local apply page/batch.

هذا يمنع:

```text
long DB locks
UI starvation
network wait inside transaction
```

---

# 102. Tombstone page size

يجب أن يكون bounded.

القيمة exact يحددها التنفيذ وفق server contract، لكن يجب وجود max واضح.

ممنوع:

```text
unbounded select all tombstones forever
```

إذا توجد pagination loop، كل page لها atomic apply+cursor.

---

# 103. Page loop

المنطق الصحيح:

```text
cursor = durable cursor
repeat bounded pages:
  page = remote.fetch(cursor)
  if empty/no-advance -> stop
  atomicApply(page)
  cursor = page.nextCursor from durable store/committed value
```

ممنوع تحديث variable cursor فقط دون persistence.

---

# 104. No infinite page loop

إذا server يعيد:

```text
non-empty page + same cursor repeatedly
```

يجب إيقافها كـcontract failure:

```text
NON_ADVANCING_CURSOR
```

لا loop بلا حد.

---

# 105. Reconciliation after push ambiguity

Withdrawal existing reconciliation يبقى قبل duplicate retry.

67 لا تحذفه أثناء نقل push إلى بداية الدورة.

---

# 106. Reconciliation after remote apply

بعد apply، يجب التحقق من أن pending-local markers المحمية ما زالت موجودة أو حُسمت عبر push/reconciliation.

أي disappearance بلا success proof:

```text
PENDING_LOCAL_LOST
```

في model fixtures.

---

# 107. LocalDataCleaner policy

v66 `LocalDataCleaner` يمسح account data و`pending_operations` عند الطلب.

إضافة `sync_cursors` لا تعني في67 ضرورة تغيير logout lifecycle الكامل.

لكن يجب توفير DAO API يسمح لـ68 بمسح cursors حسب scope.

ممنوع:

```text
cursor global delete on every normal sync
```

---

# 108. Session 68 handoff preparedness

بعد67 يجب أن يكون من السهل على68 إضافة owner fields للOutbox دون إعادة كسر cursor layer.

لذلك:

```text
SyncScope should be first-class
cursor APIs should accept SyncScope
pending guard should be isolated behind interface/component
```

---

# 109. Session 70 handoff preparedness

Deletion applier لا يعتمد على Realtime payload.

يجب أن يعمل حتى لو:

```text
Realtime disabled
all participants disconnected
```

هذا يمهد لـ70 hint-only.

---

# 110. Session 72 handoff preparedness

`SyncCursorStore` يجب أن يكون generic بما يكفي ليحمل مستقبلًا unified server revision cursor.

لكن ممنوع إضافة fake `serverRevision` field لا يستخدمه server الحالي.

الأفضل:

```text
opaque cursor_token + stream
```

ثم 72 يمكن أن يقرر معنى token.

---

# 111. Required new/changed production components

الحد الأدنى المتوقع، الأسماء قابلة للتعديل مع الحفاظ على المعنى:

```text
core/database
  SyncCursorEntity
  SyncCursorDao
  AutoDriveDatabase version 14 + MIGRATION_13_14

core/sync
  SyncScope
  DurableSyncCursorStore / repository
  production DeletionFeed adapter boundary
  Tombstone/Deletion registry/applier
  Atomic remote batch applier
  Pending local mutation guard/reconciler
  generation-safe DefaultSyncCoordinator
  reordered SyncManager/SyncEngine orchestration

core/network
  tombstone DTO / RPC response model only after real server schema is verified
```

---

# 112. Allowed file scope — Production

مسموح تعديل الملفات التالية عند الحاجة:

```text
core/sync/src/main/kotlin/com/autodrive/app/core/sync/**
core/database/src/main/kotlin/com/autodrive/app/core/database/**
core/network/src/main/kotlin/com/autodrive/app/core/network/**    [tombstone adapter only]
core/session/.../CurrentSession.kt                               [prefer no change]
core/session/.../SessionReader.kt                                [prefer no change]
```

ومسموح تعديل DAOs في sync surface لإضافة delete/merge queries محددة فقط.

---

# 113. Allowed file scope — Feature data

مسموح لمس محدود جدًا إذا كان ضروريًا لمنع pull overwrite أو reuse mapper:

```text
feature/profile/.../data/**
feature/balance/.../data/**
feature/notifications/.../data/**
```

لكن ممنوع تحويل local mutation paths إلى full transactional outbox في67؛ ذلك 68.

أي feature mutation يجب أن يبقى minimal ومبررًا في report.

---

# 114. Allowed file scope — Tests & verification

مسموح:

```text
app/src/test/kotlin/com/autodrive/app/core/sync/**
app/src/test/kotlin/com/autodrive/app/architecture/**
app/src/androidTest/kotlin/com/autodrive/app/core/database/**
scripts/verify-v67-static.sh
scripts/verify-v67-model.py أو equivalent
AUTODRIVE_SYNC_VERIFICATION_v67.json
AUTODRIVE_SYNC_VERIFICATION_v67.md
```

---

# 115. Allowed file scope — Server

مسموح فقط إذا server contract الحالي غير كافٍ:

```text
supabase/migrations/<new>_v67_sync_tombstone_cursor_contract.sql
```

لا تعدل historical migration.

---

# 116. Forbidden file-scope drift

ممنوع تعديل:

```text
core/designsystem/**
UI screens غير المرتبطة
home redesign
reports UX
competition UX
fonts/colors/theme
business calculations
commission formulas
withdrawal business eligibility
invite/auth flows غير اللازمة للscope guard
media upload pipeline
chat pagination
feature flags unrelated to sync
```

أي تغير منها:

```text
BLOCKED_UNRELATED_MUTATION
```

---

# 117. Realtime participants protected in67

ممنوع استخدام 67 لتعديل direct Room behavior فيها إلا إذا required compile adaptation بسيط جدًا.

إذا تغيرت behavior فعليًا:

```text
must be reverted or deferred to70
```

---

# 118. Server-side delete triggers protection

إذا server schema يثبت وجود delete triggers إلى `sync_tombstones`، يجب الحفاظ عليها.

لا يجوز تعطيلها للوصول لاختبار أسهل.

يجب التحقق من أن feed التي يقرأها Android ترى tombstones الناتجة من هذه triggers وفق RLS/contract.

---

# 119. Tombstone retention

إذا server contract يملك retention محددًا، 67 توثقه ولا تدعي حل offline أطول من retention.

التعافي بعد تجاوز retention:

```text
CURSOR_EXPIRED/bootstrap
```

محجوز أساسًا لـ72.

67 يجب فقط ألا تكذب وتعتبر cursor القديمة صالحة إذا server يرفضها.

---

# 120. Cursor expiry في67

إذا server يعيد signal واضح بأن cursor غير صالحة/قديمة:

```text
report typed failure / recovery-needed state
```

ممنوع:

```text
reset cursor to null and full wipe silently
```

الـsafe bootstrap يأتي في72.

---

# 121. No wipe as correctness repair

67 ممنوع أن تصلح deletion divergence عبر:

```text
delete all Room
full re-download
```

كحل افتراضي.

الهدف هو tombstone+cursor الصحيح.

---

# 122. No fullSync correctness claim

يمكن أن تبقى legacy broader sync fallback للتوافق.

لكن لا يجوز أن يقال:

```text
fullSync guarantees deletion convergence
```

بدون tombstones.

---

# 123. Static verifier determinism

`verify-v67-static` يجب أن:

```text
run offline
produce deterministic JSON ordering
avoid timestamps inside hashed semantic result or isolate them from determinism comparison
exit non-zero on gate failure
```

ويجب تشغيله مرتين عند التنفيذ النهائي ومقارنة semantic output.

---

# 124. Required verification JSON

يجب إنتاج:

```text
AUTODRIVE_SYNC_VERIFICATION_v67.json
```

ويحتوي على الأقل:

```text
sourceSha256
planSha256
roomVersionBefore
roomVersionAfter
serverTombstoneContractVerified
serverTombstoneSource
serverCursorSemanticsVerified
syncCursorTablePresent
syncCursorScopeFields
cursorOpaque
cursorClockAuthorityCount
cursorAdvanceOutsideTransactionCount
deletionFeedProductionWired
supportedDeletionEntityCount
unsupportedDynamicDeleteCount
pendingProfileProtectionVerified
pendingWithdrawalReconciliationVerified
notificationReadProtectionVerified
pipelineOrderVerified
recoverBeforePushVerified
pushBeforePullVerified
requestedGenerationPresent
completedGenerationPresent
generationDrainVerified
hintDuringPushVerified
hintDuringPullVerified
completionRaceVerified
scopeIsolationVerified
scopeRecheckBeforeCommitVerified
newV67WaiverCount
staticGatesPassed
runtimeStatus
serverRuntimeStatus
finalVerdict
handoff68Authorized
```

---

# 125. Required verification Markdown

يجب إنتاج:

```text
AUTODRIVE_SYNC_VERIFICATION_v67.md
```

ويشرح:

```text
what changed
why
exact server contract consumed
Room migration
pipeline order
cursor atomicity
pending-local policy
generation algorithm
static/model evidence
runtime/build truth
remaining v68+ work
```

---

# 126. Required source inventory artifact

يجب إنتاج inventory صغير على الأقل داخل verification JSON أو artifact منفصل يحصي:

```text
production files touched
tests added/changed
migrations added
server files added
unexpected files touched
```

أي unexpected production mutation غير مفسر:

```text
FAIL_SCOPE_INTEGRITY
```

---

# 127. Acceptance counters — يجب أن تساوي صفر

```text
inputDriftCount                         = 0
newV67WaiverCount                      = 0
unscopedCursorAccessCount              = 0
cursorClockAuthorityCount              = 0
cursorAdvanceOutsideTransactionCount   = 0
ephemeralCursorAuthorityCount          = 0
dynamicServerTableDeleteCount          = 0
deleteByAbsenceCount                   = 0
unsupportedTombstoneSilentlyIgnored    = 0
scopeMismatchSilentlyAppliedCount      = 0
pendingLocalOverwriteFixtureFailures   = 0
oLostHintFixtureFailures               = 0
atomicApplyFixtureFailures             = 0
scopeIsolationFixtureFailures          = 0
historicalMigrationMutationCount       = 0
unrelatedProductionMutationCount       = 0
newSensitiveLogViolationCount          = 0
```

---

# 128. Acceptance values — exact/positive

```text
Room version                            = 14
sync cursor entity count                = 1 canonical store
cursor scope dimensions                 = 4 key dimensions including stream
serverTombstoneContractVerified         = true for final PASS
serverCursorSemanticsVerified           = true for final PASS
deletionFeedProductionWired             = true
atomicApplyWithCursorVerified           = true
recoverBeforePushVerified               = true
pushBeforePullVerified                  = true
requestedGenerationPresent              = true
completedGenerationPresent              = true
generationDrainVerified                 = true
scopeIsolationVerified                  = true
pendingProfileProtectionVerified        = true
notificationReadProtectionVerified      = true
```

Withdrawal reconciliation:

```text
verified = true
```

إذا كانت feature/server path الفعلية موجودة كما هي في v66، وهي موجودة بالفعل.

---

# 129. Static model fixtures — الحد الأدنى

يجب أن يغطي verifier/model harness على الأقل 18 fixtures:

```text
01 cursor scope A/B isolation
02 cursor unchanged on apply failure
03 entity changes rolled back on cursor failure
04 replay same deletion page idempotent
05 unknown entity blocks cursor
06 scope mismatch blocks cursor
07 stale session scope before commit blocks cursor
08 offline delete removes stale local row
09 no delete from absence
10 pending profile survives stale pull
11 notification read survives stale pull
12 withdrawal client_request_id reconciliation
13 recover lease before send
14 push before pull
15 hint during push produces follow-up generation
16 hint during pull produces follow-up generation
17 hint completion-edge race not lost
18 hint burst coalesces and drains to latest generation
```

يفضل إضافة:

```text
19 cancellation clears active owner safely
20 non-advancing cursor terminates with failure
21 empty page does not corrupt cursor
22 malformed tombstone id blocks commit
```

---

# 130. Unit tests عند توفر Gradle

الحد الأدنى المقترح:

```text
DefaultSyncCoordinatorGenerationTest
SyncPipelineOrderTest
DurableSyncCursorStoreTest
DeletionBatchApplierTest
PendingLocalMutationGuardTest
PendingOperationProcessorTest regression
OutboxRetryPolicyTest regression
RealtimeEventPolicyTest regression
```

الأسماء قابلة للتغيير.

---

# 131. Android database tests عند توفر runtime

يجب اختبار:

```text
MIGRATION_13_14
MIGRATION_4_14 chain
transaction rollback entity+cursor
composite cursor PK behavior
```

إذا emulator/device غير متاح:

```text
NOT_RUN_ANDROID_RUNTIME_UNAVAILABLE
```

ولا يحول إلى PASS runtime.

---

# 132. Live server tests

إذا اتصال Supabase/Postgres متاح، يجب اختبار:

```text
1. tombstone row/contract visible للscope الصحيح.
2. scope آخر غير مرئي.
3. ordering/cursor resumes without gap or duplicate correctness issue.
4. delete server row -> tombstone appears.
5. second fetch from committed cursor does not replay older unseen semantics incorrectly.
```

إذا غير متاح:

```text
NOT_RUN_SERVER_RUNTIME_UNAVAILABLE
```

لكن final full PASS يتطلب على الأقل field-level server contract verification، حتى لو live E2E مؤجل.

---

# 133. RLS truth

لا يكفي client-side filter لإثبات العزل.

إذا RLS لا يمكن فحصها runtime:

```text
RLS_RUNTIME = NOT_RUN
```

ولا تُدّعى verified.

67 تمنع cross-scope cursor محليًا، بينما live RLS evidence يمكن استكمالها لاحقًا وفق بيئة التنفيذ.

---

# 134. Build gate

إذا Gradle 8.7 أصبح متاحًا:

يجب تشغيل ما يلزم لإثبات compile على الأقل.

إذا تعذر بسبب نفس blocker الموروث:

```text
BUILD = BLOCKED_ENVIRONMENT_NETWORK_BOOTSTRAP
```

ولا تعدل build files لمحاولة workaround خطير غير مطلوب.

---

# 135. Existing v58-v66 regression gates

يجب ألا تكسر 67 نظام التصميم أو static governance السابق.

عند توفر scripts المحلية:

```text
run relevant existing static verification chain
```

على الأقل يجب أن يثبت report:

```text
no design-system production drift caused by67
```

---

# 136. No production UI mutation gate

عدد production UI files المتغيرة في67 يجب أن يكون:

```text
0
```

إلا إذا كان compile adaptation لا يمكن تجنبه، ويحتاج توثيقًا صريحًا.

التوقع الطبيعي:

```text
0
```

---

# 137. New waiver policy

```text
newV67WaiverCount = 0
```

شرط PASS.

ممنوع إنشاء waiver تقول مثلًا:

```text
cursor is in memory but acceptable
server tombstones unknown but assumed
hint loss is rare
transaction can be added later
```

هذه ليست waivers؛ هذه failures.

---

# 138. Failure codes الإلزامية للتقرير

يجب دعم verdict/failure classification واضح، مثل:

```text
BLOCKED_INPUT_DRIFT
BLOCKED_ROOM_BASELINE_DRIFT
BLOCKED_SERVER_TOMBSTONE_CONTRACT
BLOCKED_SERVER_CURSOR_CONTRACT
BLOCKED_SCOPE_DRIFT
BLOCKED_UNRELATED_MUTATION
FAIL_CURSOR_NOT_DURABLE
FAIL_CURSOR_NOT_SCOPED
FAIL_CURSOR_CLOCK_AUTHORITY
FAIL_CURSOR_ADVANCE_OUTSIDE_TRANSACTION
FAIL_TOMBSTONE_NOT_WIRED
FAIL_PENDING_LOCAL_OVERWRITE
FAIL_TOMBSTONE_RESURRECTION_RISK
FAIL_SYNC_ORDER
FAIL_HINT_GENERATION_LOSS
FAIL_SCOPE_ISOLATION
PASS_STATIC_RUNTIME_BLOCKED
PASS
```

---

# 139. Expected PASS verdict

`PASS` الكامل يعني:

```text
server contract verified
Room 14 implemented and migration tests pass where runtime available
DeletionFeed production-wired
cursor durable/scoped/atomic
pipeline order correct
pending local protections pass
no-lost-hint generation tests pass
compile/unit/runtime gates run and pass according to declared full PASS profile
newV67WaiverCount=0
```

---

# 140. معنى PASS_STATIC_RUNTIME_BLOCKED

يعني فقط:

```text
source implementation complete
static/model gates complete
server field-level contract verified
no correctness ambiguity remains in code contract
build/runtime unavailable for environmental reason
```

ولا يعني:

```text
live Supabase tested
Room migration executed on device
APK compiled
```

---

# 141. Handoff إلى68

`handoff68Authorized = true` إذا:

```text
staticGatesPassed = true
serverTombstoneContractVerified = true
serverCursorSemanticsVerified = true
Room target = 14
atomic cursor/apply model gates = pass
generation gates = pass
pending-local gates = pass
newV67WaiverCount = 0
```

يجوز أن يكون runtime build blocked بيئيًا مع handoff مشروط، بشرط عدم وجود ambiguity في server contract.

إذا server tombstone contract لم يُتحقق:

```text
handoff68Authorized = false
```

لأن 68 ستبني فوق foundation غير مثبتة.

---

# 142. Implementation order — إلزامي

الترتيب المطلوب:

```text
1. Freeze baseline + fingerprints.
2. Inspect/verify actual sync_tombstones server contract.
3. Decide whether existing server cursor is safe.
4. If needed, append v67 server migration limited to tombstone cursor.
5. Add SyncScope.
6. Add Room sync_cursors entity + DAO.
7. Add MIGRATION_13_14 + register database entity/DAO.
8. Replace ephemeral checkpoint authority with durable cursor store.
9. Implement production tombstone remote adapter.
10. Implement explicit deletion registry/applier.
11. Add pending-local guard/merge policies for current v66 local intents.
12. Wrap cursor-bearing apply in Room.withTransaction.
13. Refactor outbox recovery/push API as needed.
14. Reorder cycle to Recover → Push → Pull → Apply → Reconcile.
15. Add requestedGeneration/completedGeneration drain semantics.
16. Add deterministic model/unit tests.
17. Update migration tests/schema evidence.
18. Run static verifier twice.
19. Run Gradle/runtime gates if environment permits.
20. Produce verification JSON/MD.
21. Package v67 ZIP and hashes.
```

لا يبدأ coordinator generation refactor قبل تثبيت semantics المطلوبة، لكن يمكن coding order تغييره قليلًا بشرط final diff يطابق العقد.

---

# 143. Pre-implementation server questions

على المنفذ الإجابة من schema الفعلية لا بالتخمين:

```text
Q1. ما اسم primary/event identity في sync_tombstones؟
Q2. ما entity/table identity field؟
Q3. ما row/entity id field؟
Q4. ما fields العزل: user/client/org؟
Q5. ما ordering authority؟
Q6. هل يوجد cursor/revision monotonic بالفعل؟
Q7. هل feed يمكن filter/paginate safely؟
Q8. ما retention؟
Q9. ما RLS policies؟
Q10. هل delete triggers تغطي كل sync surfaces المطلوبة؟
```

أي سؤال جوهري بلا جواب يمنع server PASS.

---

# 144. Server migration acceptance إن أضيفت

إذا أضيفت migration:

```text
new migration count for v67 = 1
historical migrations changed = 0
existing tombstone rows preserved = true
existing trigger behavior preserved = true
safe ordering/cursor exposed = true
RLS not weakened = true
```

ممنوع grant واسع لـanon/authenticated يتجاوز ownership.

---

# 145. Cursor schema upgrade future compatibility

`contract_version` المحلي يبدأ:

```text
1
```

الغرض أن 72 يمكنها تمييز cursor semantics إذا تغيرت.

لكن 67 لا تبني migration future وهمية.

---

# 146. Current account scope fields

`CurrentSession` يحتوي بالفعل:

```text
userId
clientId
orgId
```

67 يجب أن تستخدم الثلاثة.

ممنوع إسقاط `orgId` لأن بعض current pulls تعتمد فقط client/user.

الخطة الأم تطلب صراحة scope كامل.

---

# 147. Entity ownership validation

عند mapping remote upsert أو tombstone، لا يكفي cursor scope وحده إذا DTO نفسه يحمل scope fields متعارضة.

يجب تطبيق defense-in-depth للـDTOs التي تحمل:

```text
clientId
orgId
userId
```

المخالفة توقف ذلك item/batch حسب policy وتمنع cursor commit إذا batch authoritative.

---

# 148. Marketer balance special case

`marketer_balance` محليًا مرتبط بـuserId، بينما remote pull في v66 يستخدم `client_id`.

أي tombstone mapping له يجب ألا يحذف balance مستخدم آخر بالخطأ.

الـscope validation يجب أن يربط:

```text
current user + client + org
```

قبل `deleteByUserId(currentUser)`.

---

# 149. Payment special case

PaymentEntity المحلي لا يحمل `client_id`.

لذلك tombstone delete by payment id يجب الاعتماد على:

```text
server scope enforcement + validated tombstone scope
```

ولا يجوز محاولة استنتاج tenant من local invoice existence كشرط وحيد.

---

# 150. Internal messages special case

Remote table:

```text
internal_messages
```

Local table:

```text
chat_messages
```

المmapping يجب أن يكون explicit.

ممنوع dynamic same-name assumption.

---

# 151. Conversation deletion ordering

إذا tombstone batch يحتوي conversation ورسائلها:

67 لا تملك transactionGroup global protocol.

المطلوب local delete semantics deterministic:

```text
conversation delete may also delete its local messages explicitly
```

أو حسب proven schema behavior.

لا تضف Foreign Keys مدمرة لمجرد convenience إذا architecture السابقة تعمدت تحمل out-of-order Realtime.

---

# 152. Unknown tombstone behavior

Unknown entity لا تُسقط بصمت.

السبب:

```text
advancing cursor while ignoring unknown delete causes permanent divergence
```

إذًا:

```text
fail batch
keep cursor
report entity type sanitized
```

---

# 153. Malformed ID behavior

`entityId.isBlank()` أو invalid required identity:

```text
fail batch
keep cursor
```

ممنوع:

```text
skip item and advance cursor
```

إلا إذا server contract يعرّف dead-letter server-side مثبتًا، وهو غير موجود في scope الحالي.

---

# 154. Tombstone vs local pending conflict behavior

إذا conflict unresolved:

```text
keep cursor before conflicting tombstone
surface reconciliation-needed failure
```

هذا أكثر أمانًا من:

```text
silently dropping local intent
or
silently resurrecting remote delete
```

69/72 ستضيف conflict protocol أقوى.

---

# 155. Cursor row creation timing

لا تنشئ cursor row بـempty token قبل أول successful server page إلا لسبب contract واضح.

الأفضل:

```text
no row = beginning of stream
```

بعد first successful page:

```text
row exists with authoritative nextCursor
```

---

# 156. Initial tombstone sync

إذا no cursor يعني beginning of retained tombstone stream، يجب server contract أن يعرّف ذلك.

إذا server يتطلب bootstrap cursor منفصلًا، لا تخترع واحدًا.

في غياب safe initial semantics:

```text
BLOCKED_SERVER_CURSOR_CONTRACT
```

---

# 157. Empty tombstone page

إذا page فارغة:

```text
no local deletion
cursor advances only if server explicitly returns a safe nextCursor advancement contract
```

لا تفترض أن empty page يعني head known ما لم server يقول ذلك.

---

# 158. Cursor commit after no-op deletes

إذا كل tombstones في page تشير لصفوف غير موجودة محليًا لكن valid:

```text
page is successfully applied
cursor may advance
```

لأن delete idempotent.

---

# 159. Existing pending operations query additions

يجوز إضافة DAO queries للـguard، مثل:

```text
active operations by table/operation/idempotencyKey
```

ممنوع rewrite schema.

الـactive unresolved statuses في v67 يجب أن تعتبر على الأقل:

```text
PENDING
IN_PROGRESS
DEAD_LETTER when it still represents unresolved local intent
```

ولا يعتبر `SUCCEEDED` intent pending.

---

# 160. DEAD_LETTER semantics في67

67 لا تضيف recovery UX.

لكن pull لا يجوز أن يمحو local state المرتبط بـDEAD_LETTER بلا قرار صريح.

69 ستعطي dead-letter workflow كامل.

---

# 161. Outbox success deletion atomicity

v66 يعمل:

```text
markSucceeded()
deleteSucceededById()
```

في عمليتين.

67 لا تُلزم بإعادة تصميم هذا إلى transaction عامة، لأن 68/69 تغطي Outbox hardening.

لكن لا يجوز أن يجعل refactor الوضع أسوأ.

---

# 162. Outbox push summary

يفضل أن يصبح `flush()` يعيد summary بدل Unit حتى orchestration/reporting تعرف:

```text
succeeded
retry scheduled
dead letter
```

لكن ليس شرطًا إذا يوجد equivalent observable state واختبارات order/pending guard مكتملة.

---

# 163. Diagnostics fields لـ67

مسموح إضافة الحد الأدنى:

```text
requestedGeneration
completedGeneration
cursor stream count
last deletion batch result
```

لكن observability الشاملة لـ73.

لا توسع telemetry بلا حاجة.

---

# 164. SyncState changes

يجوز إضافة:

```text
requestedGeneration
completedGeneration
```

إلى `SyncState` إذا مفيد للdiagnostics/UI-free verification.

أو إبقاؤها private مع expose تشخيصي مناسب.

المهم إثبات invariant.

---

# 165. No UI dependency on generation

67 لا تضيف UI لعرض generations.

كلها internal correctness/diagnostics.

---

# 166. Completion status عبر عدة generations

إذا owner نفذ أكثر من cycle بسبب hints:

النتيجة النهائية للshared flight يجب أن تكون واضحة.

سياسة موصى بها:

```text
return result of last serviced cycle
retain failures in diagnostics per cycle
```

أو aggregate deterministically.

لكن ممنوع إخفاء failure حرج مثل stale scope/cursor apply failure كـSUCCESS لأن cycle لاحقة كانت empty.

---

# 167. No silent partial PASS

إذا أي من الأربعة شروط الأصلية للخطة لم يثبت:

```text
Offline delete safety
Pending local protection
Crash-safe cursor
No lost hint
```

لا يجوز إصدار PASS.

`3/4` = FAIL/PARTIAL، وليس PASS.

---

# 168. Required final acceptance questions

قبل packaging يجب الإجابة `YES` على كل التالي:

```text
Q1  هل DeletionFeed مستخدمة إنتاجيًا؟
Q2  هل sync_tombstones contract الفعلي موثق ومتحقق؟
Q3  هل cursor resumable بدون device clock؟
Q4  هل cursor durable في Room؟
Q5  هل cursor scoped بـuserId/clientId/orgId/stream؟
Q6  هل Room = 14؟
Q7  هل migration 13→14 append-only وآمنة؟
Q8  هل cursor commit داخل نفس transaction مع apply؟
Q9  هل apply failure يترك cursor القديم؟
Q10 هل cursor failure يرجع entity changes؟
Q11 هل delete-by-absence = صفر؟
Q12 هل unknown tombstone لا يُتجاهل؟
Q13 هل scope mismatch يمنع apply؟
Q14 هل pending profile لا يُدهس؟
Q15 هل notification read المحلي لا يرجع unread؟
Q16 هل withdrawal reconciliation ما زالت idempotent؟
Q17 هل expired leases تُسترد قبل push؟
Q18 هل push يحدث قبل pull؟
Q19 هل requestedGeneration موجود؟
Q20 هل completedGeneration موجود؟
Q21 هل hint أثناء push يسبب generation لاحقة؟
Q22 هل hint أثناء pull يسبب generation لاحقة؟
Q23 هل race عند إغلاق activeSync لا يفقد hint؟
Q24 هل burst hints coalesced بلا loss؟
Q25 هل Realtime direct Room cleanup لم يُسحب مبكرًا من70؟
Q26 هل Outbox schema redesign لم يُسحب مبكرًا من68؟
Q27 هل Unified Change Feed لم يُسحب مبكرًا من72؟
Q28 هل newV67WaiverCount=0؟
Q29 هل كل runtime NOT_RUN موصوف بصدق؟
Q30 هل output archive يحمل verification artifacts؟
```

أي `NO` غير مصنف صراحة كـruntime-only blocker يمنع PASS.

---

# 169. Required implementation report structure

`AUTODRIVE_SYNC_VERIFICATION_v67.md` يجب أن يتبع ترتيبًا قريبًا من:

```text
1. Baseline identity
2. Server tombstone contract evidence
3. Room 13→14 migration
4. Durable cursor design
5. Tombstone adapter and registry
6. Atomic apply proof
7. Pending-local merge/guard proof
8. Pipeline reordering proof
9. Generation-safe coordinator proof
10. Static/model tests
11. Gradle/runtime/server tests
12. Scope diff inventory
13. Remaining known deferrals v68-v73
14. Final verdict
15. handoff68Authorized
```

---

# 170. Required packaging

اسم archive المستهدف:

```text
AutoDrive-v67-sync-safety-foundation.zip
```

ويجب أن يحتوي على الأقل:

```text
source tree المعدل
SESSION_67_FINAL.md
AUTODRIVE_SYNC_VERIFICATION_v67.json
AUTODRIVE_SYNC_VERIFICATION_v67.md
Room schema 14 evidence إذا أمكن توليده فعليًا
new v67 server migration إن أضيفت
static/model verifier(s)
```

---

# 171. Output SHA-256

بعد packaging:

```text
sha256sum AutoDrive-v67-sync-safety-foundation.zip
```

ويُنتج ملف:

```text
AutoDrive-v67-sync-safety-foundation.zip.sha256
```

يمكن أيضًا إنتاج:

```text
SESSION_67_FINAL.md.sha256
```

لكن input `SESSION_313_FINAL.md.sha256` المرفق لا يدخل في أي قرار حسب طلب المستخدم.

---

# 172. Archive integrity

بعد إنشاء ZIP:

```text
unzip -t AutoDrive-v67-sync-safety-foundation.zip
```

يجب أن ينجح.

ثم يعاد استخراج archive النهائي إلى directory نظيف وتشغيل static verifier عليه، لا على working tree فقط.

هذا يمنع packaging drift.

---

# 173. Final static replay

الترتيب:

```text
1. verify working tree
2. package ZIP
3. extract ZIP fresh
4. verify extracted ZIP
5. compare semantic verification outputs
```

يجب ألا تختلف النتيجة.

---

# 174. No generated junk

ممنوع تضمين:

```text
.gradle caches
build outputs الضخمة غير المطلوبة
IDE caches
local secrets
local.properties الحقيقي
Supabase service-role keys
```

يحافظ على `local.properties.example` فقط كما في baseline إن كان مطلوبًا.

---

# 175. Secret scan

قبل packaging يجب فحص diff/new artifacts من:

```text
service_role
JWT secrets
access tokens
refresh tokens
passwords
bank payload dumps
raw OTPs
```

أي secret:

```text
BLOCKED_SECRET_LEAK
```

---

# 176. Report honesty

إذا Gradle ما زال blocked:

الصياغة الصحيحة:

```text
STATIC/MODEL PASS; BUILD/UNIT/ANDROID RUNTIME NOT RUN DUE ENVIRONMENT BLOCKER
```

الممنوع:

```text
all tests passed
```

إذا لم تعمل tests فعلًا.

---

# 177. Final verdict matrix

## PASS

```text
all required correctness gates pass
server contract verified
runtime tests required for claimed PASS have run
no new waivers
```

## PASS_STATIC_RUNTIME_BLOCKED

```text
all source/static/model gates pass
server contract verified
runtime/build unavailable بسبب environment
no false runtime claims
handoff68 may be authorized conditionally
```

## BLOCKED_SERVER_TOMBSTONE_CONTRACT

```text
client code cannot safely bind unknown server schema/cursor
handoff68 = false
```

## FAIL

أي correctness gate يفشل.

---

# 178. Handoff payload إلى Session 68

يجب أن تستلم68:

```text
AutoDrive-v67-sync-safety-foundation.zip
SESSION_67_FINAL.md
AUTODRIVE_SYNC_VERIFICATION_v67.json
AUTODRIVE_SYNC_VERIFICATION_v67.md
server tombstone contract evidence/migration
Room 14 schema evidence
```

ويكون baseline 68 هو ZIP الناتج من67 فقط، لا v66.

---

# 179. ما يجب أن تبنيه68 فوق67

68 ستأخذ foundation التالية كأمر واقع:

```text
SyncScope exists
Durable cursor exists
Deletion feed works
Atomic cursor apply exists
Generation-safe coordinator exists
Push-before-pull order exists
```

ثم تضيف:

```text
transactional local mutation + Outbox
Outbox owner/entity identity fields
logout isolation
```

إذا اضطر68 لإعادة بناء أي foundation رئيسية من الصفر، فهذه إشارة أن67 لم تُنفذ كما يجب.

---

# 180. الخلاصة النهائية للعقد

جلسة67 تنجح فقط إذا تحول AutoDrive من:

```text
Pull-first
no durable cursor
unused deletion abstraction
non-atomic apply
single-flight hint collapse
```

إلى:

```text
Recover leases
→ Push current Outbox
→ Pull authoritative deletion delta + compatibility positive state
→ Atomic apply + durable scoped cursor
→ Protect/reconcile current pending local intent
→ Drain requested generations without losing hints
```

مع الحفاظ على حدود الخطة:

```text
No Outbox redesign yet.
No Inbox yet.
No Realtime rewrite yet.
No Chat-scale rewrite yet.
No Unified Server Revision yet.
No final anti-entropy/fault-injection closure yet.
```

والقاعدة النهائية:

```text
If deletion, cursor, atomicity, pending-local safety, or generation-drain
is assumed rather than proven, Session 67 is not complete.
```

---

# END OF SESSION_67_FINAL.md
