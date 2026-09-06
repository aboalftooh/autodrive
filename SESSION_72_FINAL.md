# SESSION_72_FINAL.md

## AutoDrive Sync Modernization — Session 72

### Unified Server Change Feed + Global Data Revision + Safe Bootstrap + Cursor Expiry + Anti-Entropy

**نوع المستند:** عقد تنفيذ مستقل وصارم للجلسة السادسة من مسار تحديث مزامنة AutoDrive المضغوط v67→v73  
**الجلسة:** 72  
**الحالة:** `PLAN ONLY — READY AS CONTRACT; EXECUTION GATED BY v71 HANDOFF / PREDECESSOR CHAIN + AUTHORITATIVE SERVER SCHEMA/RUNTIME EVIDENCE`  
**تاريخ الصياغة:** 2026-08-21  
**مصدر الكود المفحوص:** `AutoDrive-v71-chat-recovery-durable-media-source-of-truth.zip`  
**SHA-256 للمصدر المفحوص:** `c1367830c0b7332c15d9e1a71476a8242537a61e39deca5c11e7a2d89768cfd2`  
**Archive entries:** `1359`  
**Production Kotlin files:** `269`  
**Test Kotlin files:** `46`  
**Room الحالي:** `17`  
**Room المستهدف في 72:** `18` — migration محلية append-only واحدة فقط إذا نُفذت durable bootstrap staging/reconciliation state كما يفرض هذا العقد  
**مرجع التنفيذ السابق داخل ZIP:** `AUTODRIVE_SYNC_VERIFICATION_v71.md/.json` + `SESSION_71_FINAL.md`  
**v71 final verdict:** `IMPLEMENTED_STATIC_MODEL_RUNTIME_BLOCKED_PREDECESSOR_OVERRIDDEN`  
**v71 static:** `59/59 PASS` deterministic  
**v71 model:** `38/38 PASS` deterministic  
**v71 migration model:** `9/9 PASS` deterministic  
**v71 CHAT_10K_VERIFIED:** `true`  
**v71 COMPILED:** `false`  
**v71 UNIT_TESTED:** `false`  
**v71 ANDROID_MIGRATION_TESTED:** `false`  
**v71 SERVER_CHAT_RUNTIME_VERIFIED:** `false`  
**v71 MEDIA_STORAGE_RUNTIME_VERIFIED:** `false`  
**v71 predecessorGateSatisfied:** `false`  
**v71 handoff72Authorized:** `false`  
**v67 inherited server tombstone blocker:** `OPEN`  
**production UI drift in v71:** `0`  
**new v71 waivers:** `0`  

---

# 0. الحكم التنفيذي المختصر

Session 72 ليست تحسينًا تجميليًا لـ`LegacyRemotePuller`، وليست زيادة `LIMIT`، وليست تغيير أسماء cursors.

هي نقطة التحول من عدة مسارات inbound مستقلة إلى بروتوكول واحد حتمي:

```text
SERVER MUTATION
  ↓ same database transaction
Unified Change Log
  ↓
Global monotonic DATA revision
  ↓
Scoped paged Change Feed
  ↓
Android durable global cursor
  ↓
Scoped Inbox dedupe
  ↓
Room transaction {
    apply complete transaction-group
    record Inbox
    advance cursor
}
  ↓
Room remains UI Source of Truth
```

وعند انتهاء retention:

```text
CURSOR_EXPIRED
  ↓
Safe server snapshot at baseline revision R
  ↓
Durable local bootstrap staging
  ↓
Room transaction {
    install snapshot
    preserve pending local intent
    set global cursor = R
}
  ↓
resume deltas > R
```

ثم:

```text
Periodic Anti-Entropy
  ↓
server manifest / partition digest
  ↓
local canonical projection digest
  ↓
match -> converged evidence
mismatch -> targeted repair
persistent mismatch -> scoped rebootstrap
```

القواعد المطلقة:

```text
COMMAND_RECEIPT revision != DATA_CHANGE revision.
```

```text
chat_recovery_seq != global data revision.
```

```text
updated_at/device time MUST NOT be correctness cursors.
```

```text
Revision gaps are valid; ordering is monotonic, not contiguous.
```

```text
A transaction group MUST NOT be split across apply boundaries.
```

```text
Cursor advancement MUST commit with applied data + Inbox.
```

```text
Realtime remains HINT ONLY.
```

```text
Legacy snapshots MUST NOT remain a second incremental authority after canonical cutover.
```

```text
No full wipe is the default anti-entropy repair strategy.
```

```text
No predecessor blocker may be hidden behind PASS.
```

---

# 1. لماذا Session 72 الحالية تجمع 76 + 77 + 78 من الخطة الأصلية

الخطة الأصلية v67→v80 فصلت:

```text
76 = Unified Server Change Feed
77 = Safe Bootstrap + Cursor Expiry
78 = Reconciliation / Anti-Entropy
79 = Observability
80 = Fault Injection
```

لكن المسار المضغوط v67→v73 المثبت في عقود 70 و71 نقل:

```text
69 = Idempotent commands
70 = Durable Inbox + Atomic Apply + Realtime Hint-Only
71 = Chat scale/recovery + durable media + conversation ambiguity
72 = Unified Change Feed + global revision + safe bootstrap + anti-entropy
73 = Observability + full fault-injection closure
```

لذلك:

```text
SESSION_72_SCOPE =
    UNIFIED_SERVER_CHANGE_FEED
  + GLOBAL_DATA_REVISION
  + TRANSACTION_GROUP_PAGING
  + CANONICAL_ANDROID_DELTA_APPLY
  + SAFE_BOOTSTRAP
  + CURSOR_EXPIRY
  + ANTI_ENTROPY
  + TARGETED_REPAIR
```

ولا تشمل 72 إعادة تنفيذ:

```text
v67 generation safety
v68 transactional Outbox
v69 command receipts
v70 Inbox / Realtime hint-only
v71 chat 10k/media/conversation create
v73 full observability
v73 full fault-injection suite
```

---

# 2. بوابة البداية — v71 Handoff Gate

قبل أي mutation يجب قراءة:

```text
AUTODRIVE_SYNC_VERIFICATION_v71.json
AUTODRIVE_SYNC_VERIFICATION_v71.md
SESSION_71_FINAL.md
AUTODRIVE_SYNC_VERIFICATION_v70.md
SESSION_70_FINAL.md
```

والتحقق من:

```text
finalVerdict
handoff72Authorized
predecessorGateSatisfied
Room version
v71 static/model/migration results
CHAT_10K_VERIFIED
newV71WaiverCount
Realtime direct/transitive Room write counters
v69 command receipt revision kind
```

الحالة الحالية المثبتة:

```text
v71 finalVerdict             = IMPLEMENTED_STATIC_MODEL_RUNTIME_BLOCKED_PREDECESSOR_OVERRIDDEN
handoff72Authorized          = false
predecessorGateSatisfied     = false
Room                         = 17
v71 static                   = 59/59 PASS
v71 model                    = 38/38 PASS
v71 migration model          = 9/9 PASS
CHAT_10K_VERIFIED            = true
COMPILED                     = false
UNIT_TESTED                  = false
ANDROID_MIGRATION_TESTED     = false
SERVER_CHAT_RUNTIME_VERIFIED = false
MEDIA_STORAGE_RUNTIME_VERIFIED = false
newV71WaiverCount            = 0
productionUiFilesChanged     = 0
```

إذًا افتراضيًا:

```text
SESSION_72_EXECUTION_GATE = BLOCKED_PREDECESSOR_HANDOFF
```

يجوز التنفيذ فقط إذا:

```text
A) أُغلقت predecessor chain رسميًا وأصبح handoff72Authorized=true
```

أو:

```text
B) أصدر المستخدم Override صريحًا لتنفيذ 72 فوق السلسلة المحجوبة
```

في B:

```text
IMPLEMENTATION MAY PROCEED
FULL_RELEASE_PASS IS FORBIDDEN
handoff73Authorized MUST remain false
```

إلا إذا وفرت 72 نفسها evidence موثوقًا يغلق blocker الموروث فعليًا وفق قسم Supersession أدناه.

---

# 3. Baseline Gate — هوية v71

قبل أي تعديل يجب تثبيت:

```text
ZIP SHA-256       = c1367830c0b7332c15d9e1a71476a8242537a61e39deca5c11e7a2d89768cfd2
archive entries   = 1359
production Kotlin = 269
test Kotlin       = 46
Room              = 17
```

أي اختلاف غير موثق:

```text
BLOCKED_INPUT_DRIFT
```

إذا Room ليست 17:

```text
BLOCKED_ROOM_BASELINE_DRIFT
```

إذا Realtime عاد يكتب business state إلى Room مباشرة:

```text
BLOCKED_REALTIME_REGRESSION
```

---

# 4. Authority Order

عند التعارض، ترتيب السلطة:

1. `AutoDrive-v71-chat-recovery-durable-media-source-of-truth.zip` بالـSHA المثبت.
2. `AUTODRIVE_SYNC_VERIFICATION_v71.json/.md` لحقيقة التنفيذ الحالية.
3. `SESSION_71_FINAL.md` لحدود 72 وما لا يجوز إعادته.
4. `AUTODRIVE_SYNC_VERIFICATION_v70.md` و`SESSION_70_FINAL.md` لعقد Inbox/Realtime.
5. `SESSION_69_FINAL.md` لعقد command receipts وفصل `COMMAND_RECEIPT` عن data revision.
6. الخطة الأصلية `AUTODRIVE_SYNC_MODERNIZATION_REPAIR_PLAN_v67-v80.md` فقط لبنود Unified Change Feed / Bootstrap / Anti-Entropy حيث لا تتعارض مع الضغط.
7. authoritative current server schema / live introspection / deployed migration evidence.
8. كود v71 الفعلي.
9. هذا العقد.

ممنوع استيراد semantics خاصة بـVerto/Optimal/Max إلى AutoDrive.

---

# 5. بصمات الملفات الحرجة قبل التنفيذ

يجب تسجيل هذه البصمات قبل mutation:

```text
SyncManager.kt
  e1f985aa77c61a25fc50307f55a6b990d1c75a98c11a72a18e264de8ca37c437

LegacyRemotePuller.kt
  213cc4eff00601d954fa7117d60d966db337b8a3b53a96f808b4b90463d3a556

DeletionSynchronizer.kt
  d8bac53a9eac96a375d8be6554546f0170ac8982a6538d8b57d346dd306a934b

RemoteSyncSemantics.kt
  8759fe5fd2318bd93dde8b7f6306a2a0a0c7f3c99b90031f361551c562c3317c

ChatRecoverySynchronizer.kt
  7aafa6d770a04eae30ee83a7bd3d6e586476517b722350943c752a511f4936f2

PendingLocalMutationGuard.kt
  f39244dd2d2eb27c94fb4e2e08e5faaa6878c823b504a14839d1b073a7a7c8d6

OutboxSynchronizer.kt
  bccfa6a1288d644d6bfb0736174c3840a349e4f927e90cebea96781061e390fd

LocalDataCleaner.kt
  504aebba4af30a0d773bd9b1ce19404d58cee4440fe895822f92921a05b47547

AutoDriveDatabase.kt
  59d905ae23e895ca178df34311fc62656093734f2e52e09d2b69d33c4fb52ab8

SyncCursorEntity.kt
  62b31a3377f4a4693cbd2fbc63c37c22ec2d9ff705780749cc8944f9f57e03dd

SyncInboxEntity.kt
  3ec702c3d79e09adb7ae514cf5faa90fffcfccd83b6c4b82c8b65fbc919881ce

SyncCursorDao.kt
  9040dc6a136b37b26bdfe5fc8ac06ae952917df97896072cc3f2c07dfdcb2f07

SyncInboxDao.kt
  2d8991826d669fd93a5759f61a785a133d8a84bc8f4c5f64a44d4f3f18060e29

v69 idempotent command SQL
  6663381c4bf177c7cc22c75fb4c1eee1683290894307ec9ade85e4fe7620c01e

v71 chat recovery SQL
  e945ca54902b28e592250e3763a5584e84cd2ac08f35d53c03f8b918151ec641
```

أي drift يجب تسجيله في verification قبل المتابعة.

---

# 6. الحقيقة الحالية — لا يوجد Unified Change Feed

المشروع الحالي لا يحتوي migration أو production adapter باسم/معنى:

```text
unified change log
canonical data revision feed
global change feed
```

الموجود حاليًا ثلاثة مسارات inbound مختلفة:

```text
LegacyRemotePuller       -> positive snapshots
DeletionSynchronizer     -> tombstone feed abstraction
ChatRecoverySynchronizer -> chat_recovery_seq compatibility cursor
```

هذه ليست بعد بروتوكولًا موحدًا.

---

# 7. الفجوة المثبتة — DeletionFeed ما زال fail-closed

`DeletionFeedModule` يربط:

```text
DeletionFeed -> BlockedServerDeletionFeed
```

و`BlockedServerDeletionFeed.changesSince()` يرمي:

```text
SERVER_TOMBSTONE_CONTRACT_UNAVAILABLE
```

إذًا:

```text
SERVER_TOMBSTONE_RUNTIME/CONTRACT remains OPEN
```

ولا يجوز وصف deletion sync بأنها live verified في v71.

---

# 8. الفجوة المثبتة — LegacyRemotePuller = SNAPSHOT_COMPAT

`LegacyRemotePuller` يصرح صراحة:

```text
Compatibility positive-row pulls
snapshots, not canonical event streams
no eventId/serverRevision synthesized
absence is never deletion
```

هذا صحيح كحماية مؤقتة، لكنه لا يحقق 72.

بعد cutover الصحيح:

```text
LegacyRemotePuller MUST NOT remain steady-state incremental correctness authority.
```

---

# 9. الفجوة المثبتة — bounded snapshots

في v71 توجد bounded snapshot pulls منها:

```text
balance_transactions LIMIT 50
withdrawal_requests  LIMIT 20
notifications        LIMIT 50
```

هذه حدود عرض/compatibility وليست guarantees للـcompleteness.

لا يجوز في 72 علاجها بـ:

```text
LIMIT 500
LIMIT 5000
```

المطلوب change-feed pagination.

---

# 10. الفجوة المثبتة — global cursor غير موجود

`sync_cursors` موجود ومحدد بالـ:

```text
user_id
client_id
org_id
stream
```

لكن الـstream الفعلي الخاص بالحذف هو:

```text
core-tombstones-v1
```

ولا يوجد في v71 stream canonical مثل:

```text
autodrive-global-change-v1
```

يجب إنشاؤه فقط بعد server contract حقيقي.

---

# 11. الفجوة المثبتة — Chat cursor ليس global revision

v71 أضاف:

```text
chat_recovery_seq
```

والـSQL نفسه يصفه بأنه:

```text
conversation-scoped chat compatibility keyset
not a global data revision/change-feed cursor
```

في 72 ممنوع:

```text
rename chat_recovery_seq -> globalRevision
```

أو استعماله لتقدم data cursor.

---

# 12. الفجوة المثبتة — command receipt revision ليست data revision

v69 أنشأ:

```text
autodrive_command_receipt_revision_seq
receipt_revision
revision_kind = COMMAND_RECEIPT
```

هذا ledger خاص بنتيجة commands.

ممنوع:

```text
receipt.serverRevision -> sync_cursors(global-change-v1)
```

72 يجب أن تنشئ data-change revision مستقلة.

---

# 13. الفجوة المثبتة — RECONCILE الحالية no-op تقريبًا

`SyncManager` الحالي يصل إلى:

```text
onPhase(SyncPhase.RECONCILE)
completed += 1
```

بدون server manifest/digest أو targeted repair.

إذًا:

```text
ANTI_ENTROPY_PRESENT = false
```

---

# 14. الفجوة المثبتة — Bootstrap contract غير موجود

لا يوجد production protocol موثق ينفذ:

```text
CURSOR_EXPIRED
→ consistent snapshot
→ baseline revision
→ local install preserving pending writes
→ cursor commit
→ delta resume
```

إذًا:

```text
SAFE_BOOTSTRAP_PRESENT = false
```

---

# 15. ما هو موجود ويجب البناء فوقه

v72 يجب أن تعيد استخدام:

```text
v67 scoped durable cursor foundation
v67 generation-safe coordinator
v68 scoped transactional Outbox
v69 idempotent command receipts
v70 scoped durable Inbox
v70 atomic inbound apply principle
v70 Realtime hint-only
v71 chat recovery compatibility path
v71 durable media queue
v71 create->send dependency
```

ولا تعيد اختراعها.

---

# 16. Server Authority Evidence Gate

قبل كتابة SQL النهائي يجب توفير evidence authoritative حديث يغطي الجداول التي ستدخل feed.

الدليل المقبول:

```text
A) current schema.sql / pg_dump --schema-only
B) live introspection موثق
C) migrations الأصلية التي تعرف الجداول/السياسات الحالية بالكامل
```

يجب أن يغطي على الأقل schema الفعلي لـ:

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
internal_messages
```

وكذلك:

```text
RLS policies
FKs
PK types
scope columns
write functions/triggers
existing tombstone objects if any
```

إذا لم يتوفر:

```text
BLOCKED_AUTHORITATIVE_SERVER_SCHEMA
```

ممنوع اختراع column names أو RLS semantics.

---

# 17. Server Change Feed — الهدف

المطلوب إنشاء بروتوكول Server-owned واحد يجيب:

```text
What changed after data revision R for this authenticated scope?
```

وليس:

```text
Which rows have updated_at after device timestamp T?
```

---

# 18. Data revision sequence

يجب وجود sequence مستقلة لـdata changes.

مثال تسمية فقط:

```text
autodrive_data_revision_seq_v1
```

القواعد:

```text
revision > 0
revision monotonic
revision unique per event
revision gaps allowed
revision never derived from client time
revision never reused from command receipt sequence
```

لا يوجد شرط:

```text
nextRevision == previousRevision + 1
```

---

# 19. Revision gaps — قاعدة إلزامية

PostgreSQL sequences قد تترك gaps بسبب rollback/cache/crash.

لذلك validation الصحيح:

```text
nextRevision > previousRevision
```

وليس:

```text
nextRevision == previousRevision + 1
```

أي verifier يفرض contiguity:

```text
INVALID_V72_VERIFIER
```

---

# 20. Canonical change event contract

كل event يجب أن يحتوي contractually على الأقل:

```text
eventId
revision
entityType
entityId
operation
transactionGroupId
occurredAt
contractVersion
scope identity/visibility derived by server
```

ولـUPSERT يجب أن يصل Android إلى canonical data كافية لتطبيق الحدث deterministically.

يمكن تنفيذ ذلك بـ:

```text
immutable event payload
```

أو protocol آخر مثبت يضمن نفس النتيجة.

ممنوع الاعتماد على ambiguous "fetch current row later" دون تعريف superseded/deleted semantics.

---

# 21. Supported operations

الحد الأدنى:

```text
UPSERT
DELETE
```

إذا أضيفت operations أخرى:

```text
CREATE
UPDATE
```

فيجب إما normalise إلى UPSERT أو تعريف apply semantics صريحة.

Android لا يستنتج DELETE من غياب row.

---

# 22. Event identity

`eventId` يجب أن يكون:

```text
server-generated
stable
unique
replayable
```

إعادة نفس eventId مع identity مختلفة:

```text
INBOX_EVENT_IDENTITY_CONFLICT
```

ولا يجوز silent overwrite.

---

# 23. Transaction group identity

كل تغييرات server transaction الواحدة المترابطة يجب أن تشترك في:

```text
transactionGroupId
```

الهوية يجب أن:

```text
تُولد server-side
تكون ثابتة لكل events في transaction
لا تعتمد على device
لا تعتمد على command receipt revision
```

نوعها الفعلي يحدد من authoritative server design.

---

# 24. Transaction group atomicity

إذا transaction أنتجت:

```text
Invoice revision 120
Payment revision 121
```

بنفس group:

```text
G42
```

Android يجب أن يطبق المجموعة كاملة داخل Room transaction واحدة.

ممنوع:

```text
commit invoice
advance cursor=120
crash
payment later
```

إذا group واحدة.

---

# 25. Page boundary rule

Server feed MUST NOT split transaction group بين صفحتين applyable.

المسموح:

```text
page size is a target, not permission to split group
```

إذا آخر group يتجاوز الحد:

```text
include whole group
```

أو return typed error:

```text
TRANSACTION_GROUP_TOO_LARGE
```

لكن لا split صامت.

---

# 26. Change Log write atomicity

كل mutation authoritative للكيان + change event يجب أن تكون:

```text
SAME POSTGRES TRANSACTION
```

إذا row commit وchange event لم commit:

```text
SYNC_PROTOCOL_INVALID
```

إذا event commit والrow mutation rollback:

```text
SYNC_PROTOCOL_INVALID
```

---

# 27. Coverage strategy

يفضل common trigger/helper بحيث كل writer path ينتج event تلقائيًا.

لكن القبول ليس مبنيًا على شكل implementation.

القبول مبني على:

```text
100% write-path coverage for in-scope entities
```

ويجب إثباته بـinventory machine-readable.

---

# 28. Required entity coverage inventory

يجب إنتاج:

```text
AUTODRIVE_SYNC_CHANGE_FEED_INVENTORY_v72.json
```

لكل entity:

```text
entityType
serverTable
primaryKeyType
scopeDerivation
writers[]
changeCaptureMechanism
upsertCovered
deleteCovered
transactionGroupCovered
payloadStrategy
runtimeVerified
```

الحد الأدنى المتوقع من الكود الحالي:

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
internal_messages
```

أي entity غير مغطى يجب أن يكون deferred صراحة، لا silently omitted.

---

# 29. Scope derivation on server

الـclient لا يرسل `orgId/clientId/userId` ويُصدق حرفيًا كسلطة.

Server RPCs يجب أن derive/validate scope من authenticated principal والعلاقات الحالية.

قاعدة:

```text
AUTHENTICATED_IDENTITY -> SERVER_DERIVED_SCOPE
```

وليس:

```text
CLIENT_JSON_SCOPE -> TRUSTED_SCOPE
```

---

# 30. Change Feed RLS / security

ممنوع grant مباشر على raw change-log table للـAndroid.

يفضل:

```text
raw table: no SELECT for authenticated
scoped RPC: authenticated execute only
```

إذا RPC `SECURITY DEFINER`:

```text
search_path pinned
scope derived server-side
no dynamic SQL from client identifiers
```

---

# 31. Secrets in change log

ممنوع نسخ:

```text
access tokens
refresh tokens
OTP
password hashes
service_role
push token raw value where not required
signed storage secrets
```

إلى event payload.

إذا entity تحتوي حقول حساسة، payload strategy يجب أن تكون allowlisted لا `to_jsonb(row)` بلا مراجعة.

---

# 32. Media payload rule

`internal_messages` change event يمكن أن يحتوي metadata/reference مثل:

```text
media_object_path
media_mime
media_duration_ms
```

لكن ممنوع:

```text
binary media bytes/base64 in change log
```

v71 durable media transfer remains separate.

---

# 33. Change log retention

يجب تعريف retention صريح.

العقد لا يفرض 90 يومًا إذا authoritative server policy مختلفة.

لكن server يجب أن يستطيع إرجاع:

```text
headRevision
minimumAvailableRevision
retentionContractVersion
```

حتى يكتشف Android cursor expiry deterministically.

---

# 34. CURSOR_EXPIRED classification

إذا:

```text
localRevision < minimumAvailableRevision
```

والـserver لم يعد يضمن replay كاملًا:

```text
CURSOR_EXPIRED
```

يجب أن يكون typed protocol result، لا parsing رسالة نصية.

---

# 35. Canonical feed RPC

ينبغي وجود API واحدة canonical للتغييرات، مثال تسمية:

```text
autodrive_sync_changes_v1(after_revision, page_limit)
```

الاسم غير authority؛ semantics هي authority.

الـresponse يجب أن يحمل:

```text
contractVersion
headRevision
minimumAvailableRevision
events[]
nextRevision
hasMore
```

مع group-boundary guarantees.

---

# 36. Cursor semantics

بعد v72 steady-state:

```text
stream = autodrive-global-change-v1
cursorToken = canonical decimal data revision
```

يمكن إبقاء `cursorToken` String في Room لتجنب schema churn، لكن parsing يجب أن يكون strict.

ممنوع:

```text
opaque timestamp
updated_at
chat_recovery_seq
command receipt revision
```

كقيمة لهذا stream.

---

# 37. Cursor contractVersion

`sync_cursors.contractVersion` للـglobal stream يجب أن يميز البروتوكول الجديد.

المستهدف:

```text
contractVersion >= 2
```

ولا يجوز silently interpreting old tombstone cursor as global revision.

---

# 38. Android feed boundary

أنشئ abstraction إنتاجية واحدة، مثال:

```text
UnifiedChangeFeed
```

وظيفتها فقط:

```text
fetch page from server
validate transport contract
return canonical page model
```

لا تكتب Room بنفسها.

---

# 39. Android canonical synchronizer

أنشئ owner واحد، مثال:

```text
UnifiedChangeSynchronizer
```

وهو المسؤول عن:

```text
read scoped cursor
fetch page outside Room
validate page
partition complete transaction groups
apply group(s)
record Inbox
advance cursor atomically
repeat bounded pages
surface CURSOR_EXPIRED
```

---

# 40. Network I/O rule

ممنوع أي:

```text
Supabase select/rpc/storage
```

داخل:

```text
Room.withTransaction
```

الترتيب:

```text
NETWORK FETCH
→ validate immutable page
→ Room transaction apply
```

---

# 41. Page validation

قبل أي local mutation يجب التحقق من:

```text
contract version supported
scope matches exact current scope
eventId nonblank
entityId nonblank
entityType supported
operation supported
revision > cursor
revisions strictly increasing within page
duplicate event identity consistent
transaction groups complete
nextRevision >= last returned revision
headRevision >= nextRevision
```

مع السماح بالـrevision gaps.

---

# 42. Scope validation

كل page/event يجب أن يكون usable فقط لنفس:

```text
userId
clientId
orgId
```

إذا server response يحمل scope مختلفة:

```text
REMOTE_SCOPE_MISMATCH
```

ويمنع apply/cursor advancement.

---

# 43. Session switch barrier

بعد network fetch وقبل Room apply:

```text
SyncScope.from(sessionReader.currentSession()) == capturedScope
```

وإلا:

```text
STALE_SYNC_SCOPE
```

لا writes ولا cursor advance.

---

# 44. Inbox stream transition

v70 `sync_inbox` يُعاد استخدامه مع stream جديد:

```text
autodrive-global-change-v1
```

لكل event:

```text
serverRevision = decimal data revision
revisionKind = DATA_CHANGE
transactionGroupId = canonical server value
```

ممنوع synthetic events أثناء bootstrap snapshot.

---

# 45. Duplicate page semantics

إذا page نفسها وصلت N مرات:

```text
same eventId
same identity
same revision
```

النتيجة المنطقية مرة واحدة.

Inbox dedupe يجب أن يمنع duplicate effect.

---

# 46. Event identity conflict

إذا eventId موجود لكن:

```text
entityType differs
entityId differs
operation differs
revision differs
transactionGroupId differs
```

النتيجة:

```text
INBOX_EVENT_IDENTITY_CONFLICT
```

ولا cursor advance.

---

# 47. Atomic group apply

كل complete transaction group:

```text
Room.withTransaction {
    validate current scope
    apply every event
    write/mark Inbox for every event
    set cursor to group's highest revision
}
```

إما كله commit أو كله rollback.

---

# 48. Multiple groups per page

يمكن تطبيق عدة groups في transaction واحدة أو transactions منفصلة.

لكن cursor لا يتجاوز group غير مطبقة.

إذا group G2 فشلت بعد نجاح G1:

```text
cursor may remain at end(G1)
```

ولا يجوز:

```text
cursor = page.nextRevision
```

إذا G2 لم تطبق.

---

# 49. Upsert appliers

يجب إنشاء mapping صريح لكل `entityType`.

ممنوع generic reflection غير مُختبر يكتب أي payload عشوائيًا.

كل adapter يثبت:

```text
scope
PK identity
required fields
server-owned fields
local-only fields
pending-local merge behavior
```

---

# 50. Delete appliers

DELETE يجب أن يأتي من نفس feed.

ممنوع:

```text
absence in snapshot -> delete
Realtime oldRecord -> delete authority
```

entityId + authenticated scope هما أساس delete apply.

---

# 51. Pending local mutation protection

الترتيب يبقى:

```text
RECOVER LEASES
→ PUSH OUTBOX
→ PULL CHANGE FEED
```

لكن قد تبقى operation pending بعد push attempt.

لذلك remote apply لا يحق له سحق local intent.

كل entity adapter يجب أن يحدد:

```text
APPLY_SAFE
MERGE_PRESERVE_PENDING
DEFER/CONFLICT
```

بشكل typed.

---

# 52. No cursor leap over unresolved local conflict

إذا event لا يمكن دمجه بأمان بسبب pending local intent:

```text
DO NOT ADVANCE CURSOR PAST THAT GROUP
```

إلا إذا الحدث نفسه تم حفظه durably في recovery structure تسمح بإعادة apply لاحقًا دون فقد.

لا يوجد في v71 durable raw event payload ledger لهذا الغرض، لذلك default الآمن:

```text
fail group + keep cursor
```

---

# 53. LegacyRemotePuller after canonical cutover

بعد نجاح bootstrap/global cursor:

```text
steady-state incremental pull MUST use UnifiedChangeSynchronizer
```

`LegacyRemotePuller` يمكن أن يبقى فقط:

```text
bootstrap compatibility adapter
explicit fallback diagnostic path
migration-era code behind fail-closed capability gate
```

لكن لا يُستدعى بالتوازي مع global feed كـcorrectness authority.

---

# 54. DeletionSynchronizer after canonical cutover

بعد feed موحد يحتوي UPSERT + DELETE:

```text
separate DeletionSynchronizer MUST NOT advance an independent correctness cursor in parallel
```

يمكن:

```text
retire it
or adapt it as legacy verification-only path
```

لكن canonical deletion authority تصبح unified feed.

---

# 55. Tombstone blocker supersession rule

72 يمكن أن تُغلق عمليًا blocker الموروث فقط إذا ثبت runtime أن:

```text
all in-scope DELETE writers emit canonical feed events
feed replay works from deployed server
delete events are scoped correctly
retention/minRevision contract works
Android applies DELETE + Inbox + cursor atomically
```

حينها verification يمكن أن يسجل:

```text
V67_TOMBSTONE_BLOCKER_SUPERSEDED_BY_V72_UNIFIED_CHANGE_FEED = true
```

لكن ليس قبل runtime evidence.

---

# 56. Chat recovery after global feed

`ChatRecoverySynchronizer` يبقى valid كـ:

```text
targeted repair / bootstrap helper / chat anti-entropy recovery
```

لكن steady-state global ordering لا تستخدم:

```text
chat_recovery_seq
```

كـglobal cursor.

---

# 57. Chat event coverage

global feed يجب أن يغطي metadata changes لـ:

```text
conversations
internal_messages
```

بما فيها DELETE إذا يدعمها السيرفر.

لكن v71 media bytes تبقى خارج feed.

---

# 58. Billing transaction group gate

يجب إثبات سيناريو مترابط فعلي مثل:

```text
invoice mutation + payment mutation
```

إذا حدثا في server transaction واحدة، يجب أن:

```text
share transactionGroupId
arrive in one apply group
commit together in Room
```

هذا هو replacement الصحيح لحماية v70 snapshot fetch-together.

---

# 59. Bootstrap trigger conditions

Safe Bootstrap يجب أن يعمل عند:

```text
no canonical global cursor on first canonical sync
CURSOR_EXPIRED
explicit protocol version reset
anti-entropy escalation requiring rebootstrap
```

ولا يعمل افتراضيًا عند كل app start.

---

# 60. Bootstrap is not full wipe

ممنوع:

```text
clear all Room
then download
```

كآلية correctness الأساسية.

ذلك يسبب:

```text
UI empty window
pending local intent loss risk
cross-scope hazards
```

المطلوب staged safe install.

---

# 61. Server bootstrap consistency problem

لا يكفي:

```text
fetch snapshots
then SELECT max(revision)
```

لأن mutations بين الخطوتين قد تضيع.

يجب أن تكون snapshot وbaseline revision مرتبطة contractually في نفس server consistency point.

---

# 62. Bootstrap server contract

مطلوب server API يمنح:

```text
bootstrapId
baselineRevision
contractVersion
snapshot consistency guarantee
expiry/TTL
```

ثم يسمح بقراءة snapshot pages stable لنفس `bootstrapId`.

---

# 63. Bootstrap materialization

لأن HTTP/RPC calls لا تشترك تلقائيًا في transaction واحدة طويلة، server يجب أن يثبت طريقة snapshot مستقرة عبر الصفحات.

حل مقبول:

```text
begin RPC transaction
  capture baseline revision R
  materialize scoped canonical snapshot rows under bootstrapId
commit

subsequent page RPCs read immutable bootstrap rows
```

أو أي design مكافئ مثبت.

ممنوع افتراض consistent multi-call snapshot بلا دليل.

---

# 64. Bootstrap baseline revision

`baselineRevision` هو:

```text
last DATA_CHANGE revision included/covered by snapshot consistency point
```

ليس:

```text
command receipt revision
chat recovery sequence
device timestamp
```

---

# 65. Bootstrap local durability

لمنع process death بعد تحميل بعض الصفحات، v72 تستهدف Room 18 مع durable staging.

المطلوب على الأقل:

```text
sync_bootstrap_state
sync_bootstrap_staging
```

ويمكن إضافة state ثالثة للـreconciliation إذا لزم.

---

# 66. sync_bootstrap_state minimum contract

الحقول المنطقية المطلوبة:

```text
userId
clientId
orgId
stream
bootstrapId
baselineRevision
status
contractVersion
startedAtLocal
updatedAtLocal
```

وأي page tokens المطلوبة للاستئناف إذا contract server يستخدمها.

PK يجب أن تكون scoped بالكامل.

---

# 67. sync_bootstrap_staging minimum contract

لكل snapshot row staged:

```text
userId
clientId
orgId
bootstrapId
entityType
entityId
canonicalPayload
canonicalDigest/version if contract uses it
```

الـPK تمنع duplicate row داخل bootstrap نفسها.

---

# 68. Bootstrap staging security

staging تحتوي بيانات sync فعلية، لذا:

```text
no auth tokens
no service-role secrets
no storage signing secrets
```

والتنظيف exact-scope عند logout.

---

# 69. Bootstrap page apply

Network snapshot page:

```text
fetch outside Room
validate scope/identity/bootstrapId
persist into staging transaction
```

لا تُحدث canonical Room entities مباشرة أثناء page download.

---

# 70. Bootstrap final install

بعد اكتمال snapshot pages:

```text
Room.withTransaction {
    require current scope
    validate bootstrap state COMPLETE_TO_INSTALL
    install staged canonical snapshot
    preserve pending local mutations/outbox-owned state
    remove stale server-owned rows proven absent from snapshot
    set global cursor = baselineRevision
    mark bootstrap installed / clear staging
}
```

هذه هي نقطة cutover الذرية.

---

# 71. Bootstrap pending-local preservation

إذا local entity لديها active Outbox/pending state:

```text
bootstrap MUST NOT blindly overwrite/delete it
```

الاستراتيجية يجب أن تكون entity-specific:

```text
preserve local intent
install safe server fields
or defer conflict
```

وبعد bootstrap:

```text
pending Outbox remains deliverable
```

---

# 72. Bootstrap stale-row deletion

غياب row من **consistent complete bootstrap snapshot** يمكن أن يكون deletion evidence فقط بعد snapshot completeness proven.

وهذا مختلف تمامًا عن bounded LegacyRemotePuller absence.

ممنوع delete-on-absence قبل:

```text
all bootstrap pages complete
bootstrapId stable
scope validated
```

---

# 73. Bootstrap crash before final install

إذا process مات بعد staging 90%:

```text
canonical Room remains old state
cursor remains old/expired
staging resumes or restarts safely
```

لا partial canonical install.

---

# 74. Bootstrap crash during final Room transaction

النتيجة يجب أن تكون:

```text
old canonical state + old cursor
```

أو:

```text
new canonical state + baseline cursor
```

ولا توجد حالة نصفية.

---

# 75. Bootstrap crash after final commit

عند restart:

```text
global cursor = baselineRevision
bootstrap recognized installed
next pull starts > baselineRevision
```

ولا يعيد wipe.

---

# 76. Bootstrap snapshot/delta no-gap proof

اختبار إلزامي:

```text
begin bootstrap at R=100
server mutation commits revision 101 during page downloads
snapshot represents state through R=100
final local cursor=100
next delta pull returns 101
```

إذا 101 يمكن أن تضيع:

```text
BOOTSTRAP_PROTOCOL_FAIL
```

---

# 77. Bootstrap snapshot/delta duplicate safety

إذا implementation snapshot قد تتضمن state ناتجة عن revision 101 لكن baseline=100، delta 101 قد يعاد تطبيقها.

يجب أن تكون النتيجة idempotent.

الأفضل أن contract يمنع هذا ambiguity أصلًا عبر consistent materialization.

---

# 78. Bootstrap retention

server bootstrap artifact يجب أن يملك TTL معلن.

إذا انتهت bootstrapId قبل اكتمال العميل:

```text
BOOTSTRAP_EXPIRED
```

ويبدأ bootstrap جديدة safely.

---

# 79. Bootstrap cleanup

بعد نجاح install أو إلغاء الحساب:

```text
local staging cleared for exact scope
```

وعلى server:

```text
expired bootstrap artifacts cleaned by policy
```

ولا تعتمد correctness على client cleanup.

---

# 80. Room migration 17 -> 18

إذا نفذ durable staging كما يفرض العقد:

```text
AUTODRIVE_DATABASE_VERSION = 18
```

مع:

```text
MIGRATION_17_18 only
```

ممنوع تعديل migrations التاريخية:

```text
13->14
14->15
15->16
16->17
```

---

# 81. Room 18 schema export

يجب توليد:

```text
core/database/schemas/.../18.json
```

عبر Room tooling إذا Gradle متاح.

إذا build bootstrap blocked:

```text
schema export NOT VERIFIED
```

ولا يُكتب يدويًا كدليل نهائي.

---

# 82. Migration runtime test

`DatabaseMigrationTest` يجب أن يغطي:

```text
17 -> 18
```

ويثبت:

```text
existing v71 rows preserved
new staging/state tables present
indexes present
scope keys correct
```

إذا instrumentation لا تعمل:

```text
ANDROID_MIGRATION_TESTED=false
```

---

# 83. LocalDataCleaner update

Logout exact scope يجب أن يحذف:

```text
sync_bootstrap_state
sync_bootstrap_staging
sync_reconciliation_state if added
```

إضافة إلى v71 state الحالية.

ممنوع حذف rows لحساب آخر.

---

# 84. Anti-Entropy الهدف

المزامنة incremental وحدها لا تكتشف:

```text
missed historical event
local corruption
buggy old client apply
manual server repair
partial legacy-era divergence
```

72 يجب أن تملك بروتوكول كشف مستقل.

---

# 85. Reconciliation server manifest

مطلوب endpoint server-side scoped يعيد على الأقل:

```text
contractVersion
manifestRevision
entityType
count
partition scheme
partition digests
```

والـmanifest يجب أن يمثل server truth عند revision معروفة.

---

# 86. Digest canonicalization

لا يجوز hashing لـraw JSON غير canonical.

لكل entity type يجب تعريف:

```text
field allowlist
null representation
number normalization
timestamp normalization
sorting
encoding
hash algorithm
contractVersion
```

المستهدف:

```text
SHA-256
```

أو equivalent cryptographic digest موثق.

---

# 87. Local-only fields excluded

ممنوع إدخال fields محلية بحتة في convergence digest مثل:

```text
syncStatus
readSynced
local retry timestamps
leaseUntil
local media path
local diagnostics timestamps
```

وإلا ينتج mismatch دائم.

---

# 88. Pending Outbox and digest

إذا partition تحتوي active local mutations غير committed server-side، لا يجوز إعلان mismatch نهائي مباشرة.

النتيجة يجب أن تكون typed مثل:

```text
RECONCILIATION_DEFERRED_PENDING_LOCAL
```

أو استخدام canonical server-projection digest منفصلة عن local speculative overlay.

---

# 89. Partitioning

لمنع full inventory ضخم، manifest يجب أن يدعم deterministic partitions.

أمثلة مقبولة:

```text
stable hash prefix of entityId
server-defined fixed buckets
```

لكن الخوارزمية يجب أن تكون versioned ومتطابقة server/client.

---

# 90. Root manifest mismatch

إذا:

```text
server root digest != local root digest
```

لا تذهب مباشرة إلى full wipe.

الترتيب:

```text
identify mismatched entity type
identify mismatched partition
fetch partition inventory
compute missing/extra/different IDs
targeted repair
re-check
```

---

# 91. Targeted repair

مطلوب API يسمح بجلب canonical current truth لـIDs محددة أو partition محددة ضمن scope.

الrepair يجب أن ينتج:

```text
UPSERT exact server rows
DELETE local server-owned rows proven absent
```

مع pending-local guard.

---

# 92. Targeted repair atomicity

repair batch يجب أن:

```text
validate scope
apply inside Room transaction
not mutate global cursor unless protocol explicitly ties repair to revision
```

Anti-entropy repair ليست ذريعة للقفز بالcursor.

---

# 93. Rebootstrap escalation

إذا targeted repair بعد bounded retries لا يحقق convergence:

```text
REBOOTSTRAP_REQUIRED
```

ثم Safe Bootstrap لنفس scope/stream.

لكن:

```text
full database wipe != rebootstrap
```

---

# 94. Reconciliation cadence

72 يجب أن تمنع run مكلف مع كل UI event.

مطلوب policy مثل:

```text
periodic due check
forced after bootstrap
forced after suspicious protocol failure
forced after cursor recovery
```

القيمة الزمنية exact ليست correctness invariant؛ يجب أن تكون قابلة للضبط.

---

# 95. Durable reconciliation state

يمكن إضافة table scoped مثل:

```text
sync_reconciliation_state
```

لتخزين الحد الأدنى:

```text
lastCheckedRevision
lastResult
contractVersion
nextDueAtLocal
```

هذه state correctness/scheduling وليست observability overhaul.

تفاصيل metrics الشاملة مؤجلة إلى 73.

---

# 96. SyncManager order after 72

التدفق المستهدف:

```text
AUTH
→ RECOVER EXPIRED OUTBOX LEASES
→ PUSH OUTBOX
→ ENSURE CANONICAL CURSOR / BOOTSTRAP IF NEEDED
→ PULL UNIFIED CHANGE FEED
→ APPLY GROUPS ATOMICALLY
→ RECONCILE IF DUE
→ COMPLETE
```

Realtime يبقى خارج authority:

```text
Realtime -> requestSync hint
```

---

# 97. CURSOR_EXPIRED flow in SyncManager

إذا feed يعيد `CURSOR_EXPIRED`:

```text
stop incremental apply immediately
run SafeBootstrapSynchronizer
on successful bootstrap install:
    resume unified delta pull > baselineRevision
```

ممنوع fallback تلقائي إلى bounded LegacyRemotePuller ثم اعتبار sync ناجحًا.

---

# 98. First canonical sync

إذا لا يوجد global cursor:

```text
bootstrap required
```

ولا يبدأ من:

```text
revision 0
```

إلا إذا server contract يثبت أن كامل history منذ 0 ما زال محتفظًا ويمكن replayه ضمن حدود آمنة.

الافتراضي الصحيح: bootstrap.

---

# 99. Head revision semantics

`headRevision` server value يستخدم لتحديد نهاية available feed/reconciliation reference.

لا يُكتب كlocal cursor قبل تطبيق كل events حتى ذلك الرأس.

ممنوع:

```text
cursor = headRevision on fetch start
```

---

# 100. Max pages per cycle

يجب أن يبقى هناك bounded work per sync cycle.

لكن الوصول للحد لا يعني:

```text
SUCCESS + cursor=head
```

بل:

```text
PARTIAL / more work scheduled
```

مع حفظ آخر group مطبقة فقط.

---

# 101. Large transaction group

اختبار إلزامي:

```text
group size > nominal page limit
```

Server إما:

```text
returns whole group
```

أو typed failure.

Android لا يطبق half group.

---

# 102. Event ordering across entity types

global revision هي ordering authority بين:

```text
profile
billing
balance
withdrawal
notification
chat
```

ولا توجد separate clocks لكل entity كـcorrectness ordering بعد cutover.

---

# 103. Stream scope

رغم اسم "global"، cursor ليست global لكل المستخدمين.

هي:

```text
global across in-scope entity types
BUT scoped to exact authenticated principal/client/org
```

ممنوع global unscoped cursor.

---

# 104. Principal visibility changes

إذا صلاحيات المستخدم تغيرت بحيث data set المرئية تتغير، server contract يجب أن يعرّف:

```text
scope/version invalidation
```

وقد يتطلب:

```text
CURSOR_EXPIRED / REBOOTSTRAP_REQUIRED
```

ممنوع بقاء rows لم تعد visible دون repair.

---

# 105. Cross-account leakage test

سيناريو إلزامي:

```text
A fetches page
logout A
login B
A response returns
```

النتيجة:

```text
0 writes to B
0 cursor advance for B
0 bootstrap staging under B
```

---

# 106. Cross-org leakage test

page لـorg X لا تطبق تحت org Y حتى إذا userId متطابق.

المطلوب:

```text
REMOTE_SCOPE_MISMATCH
```

---

# 107. Realtime regression gate

بعد 72:

```text
direct Room writes reachable from Realtime = 0
transitive Room writes reachable from Realtime = 0
payload business apply = 0
oldRecord delete authority = 0
```

أي regression:

```text
FAIL_V70_REGRESSION
```

---

# 108. v71 Chat regression gate

يجب استمرار:

```text
10k chat recovery passes
one authoritative ChatRecoverySynchronizer
stable media object identity
media durable intent before upload
idempotent conversation create
create->send dependency
```

72 لا تعيدها.

---

# 109. v69 command regression gate

يجب استمرار:

```text
revisionKind = COMMAND_RECEIPT
same mutation -> same logical effect
receipt reconciliation
```

ولا تستخدم command receipt revision كdata ordering.

---

# 110. v68 Outbox regression gate

يجب استمرار exact scope:

```text
userId
clientId
orgId
```

مع:

```text
claim/finalize/delete scoped
lease separate from retry
entity + outbox atomicity
```

---

# 111. v67 generation regression gate

يجب استمرار:

```text
requestedGeneration
completedGeneration
trailing sync generation
hint during push not lost
hint during pull not lost
```

72 لا تغير algorithm إلا integration الضرورية.

---

# 112. Pending local guard regression

`PendingLocalMutationGuard` الحالي يجب ألا يُتجاوز عبر generic feed applier.

كل entity adapter يجب أن يستعمل guard أو replacement أقوى مثبت.

---

# 113. Billing snapshot transition

v70 كان يجلب invoices + payments قبل apply واحد لتقليل half-state.

بعد 72 steady-state، canonical transaction-group هو authority الأعلى.

`pullBillingSnapshot()` لا يبقى correctness mechanism الدائم.

---

# 114. Notifications transition

bounded latest-50 snapshot يمكن أن يبقى UI warmup فقط إذا كان منفصلًا عن correctness.

لكن canonical feed يجب أن يجعل missed notification event recoverable طالما cursor valid.

---

# 115. Balance transactions transition

`LIMIT 50` لا يبقى completeness boundary.

historical/server changes يجب أن تأتي عبر unified delta أو bootstrap/repair.

---

# 116. Withdrawals transition

`LIMIT 20` لا يبقى completeness boundary.

pending local withdrawal protection يبقى من v68/v69.

---

# 117. Profile transition

profile snapshot single-row pull لا يبقى ordering authority.

update profile server command يجب أن ينتج data-change event مستقل عن command receipt.

---

# 118. Command → Change Feed relation

لنفس mutation الناجحة قد يوجد:

```text
command receipt revision C
```

و:

```text
data change revision D
```

العلاقة:

```text
C proves command outcome
D orders replicated state
```

لا يلزم:

```text
C == D
```

ولا يجوز افتراضه.

---

# 119. Timeout-after-commit convergence

سيناريو:

```text
command commits
response lost
Outbox reconciles receipt
later unified feed returns data event
```

النتيجة:

```text
one logical server effect
one local canonical converged state
```

بدون duplicate apply harmful effect.

---

# 120. Server write-path audit

قبل PASS يجب مسح جميع paths التي تكتب in-scope tables:

```text
RPCs
triggers
admin mutations
scheduled jobs
manual helper functions
legacy endpoints
```

أي path لا ينتج feed event:

```text
CHANGE_FEED_COVERAGE_GAP
```

---

# 121. Historical migration integrity

ممنوع تعديل:

```text
20260821203000_autodrive_idempotent_commands_v1.sql
20260821224500_autodrive_chat_recovery_commands_v1.sql
```

ويجب الحفاظ على SHA:

```text
v69 = 6663381c4bf177c7cc22c75fb4c1eee1683290894307ec9ade85e4fe7620c01e
v71 = e945ca54902b28e592250e3763a5584e84cd2ac08f35d53c03f8b918151ec641
```

72 تضيف migration جديدة append-only.

---

# 122. Server migration naming

مثال مسموح:

```text
supabase/migrations/<timestamp>_autodrive_unified_change_feed_v1.sql
```

المهم:

```text
new file
append-only
reversible by forward repair migration, not editing history
```

---

# 123. Server migration idempotent DDL

حيث يمكن، استخدم:

```text
create table if not exists
create index if not exists
create or replace function
```

لكن لا تخفي incompatible old object تحت `if not exists`.

يجب fail closed عند contract conflict.

---

# 124. Change Log indexes

يجب أن يدعم server query بكفاءة:

```text
scope + revision
revision/event identity
retention cleanup
```

الـindex exact يعتمد على schema/visibility الحقيقية.

---

# 125. Change Log immutability

بعد insert event:

```text
revision/event identity/entity identity/operation/transaction group
```

لا تعدل.

يفضل منع UPDATE/DELETE للمستخدمين العاديين على ledger.

retention cleanup يتم بمسار server-owned مضبوط.

---

# 126. Event payload schema version

إذا payload inline:

```text
payloadVersion
```

يجب أن يكون واضحًا.

Android يجب أن:

```text
reject unsupported required version
```

ولا best-effort parse مع cursor advance.

---

# 127. Unknown entity type

إذا feed أعاد entityType غير مدعومة:

```text
UNSUPPORTED_CHANGE_ENTITY
```

ولا cursor advance past event.

لا silently skip.

---

# 128. Unknown operation

إذا operation غير مدعومة:

```text
UNSUPPORTED_CHANGE_OPERATION
```

ولا cursor advance.

---

# 129. Malformed payload

إذا payload ناقصة/invalid:

```text
REMOTE_CHANGE_PAYLOAD_INVALID
```

ولا partial apply.

---

# 130. Revision overflow / parsing

Android يجب أن parse server bigint بأمان إلى `Long` إذا contract يضمن signed 64-bit positive range.

إذا خارج range:

```text
REVISION_OUT_OF_RANGE
```

ممنوع fallback إلى timestamp/String lexical ordering.

---

# 131. Cursor write location

global cursor لا تُكتب في:

```text
DataStore
SharedPreferences
in-memory only
```

إذا `sync_cursors` هي authority المحلية الحالية.

يجب أن commit داخل Room transaction مع apply.

---

# 132. Inbox receivedAt/appliedAt

`System.currentTimeMillis()` مسموح فقط كـdiagnostic local timestamps:

```text
receivedAt
appliedAt
updatedAt local metadata
```

ولا تستخدم في ordering/cursor/completeness.

---

# 133. Bootstrap local timestamps

نفس القاعدة:

```text
startedAtLocal/updatedAtLocal
```

للتشخيص/TTL المحلي فقط.

Server snapshot consistency لا تعتمد عليها.

---

# 134. Anti-entropy local timestamps

`nextDueAtLocal` scheduling فقط.

لا تثبت convergence بساعة الجهاز.

convergence evidence يأتي من manifest/digest at server revision.

---

# 135. No correctness dependency on Realtime

اختبار إلزامي:

```text
Realtime disabled بالكامل
server receives changes
periodic/manual sync runs
client converges through change feed
```

إذا لا:

```text
FAIL_REALTIME_CORRECTNESS_DEPENDENCY
```

---

# 136. No correctness dependency on FCM

FCM يمكن أن يوقظ sync فقط.

فقد FCM لا يفقد data إذا sync لاحقًا يعمل.

---

# 137. Anti-entropy without Realtime

اختبار:

```text
drop one simulated event from client apply path
advance not allowed normally
inject legacy corruption fixture
run anti-entropy
mismatch detected
repair converges
```

هذا يثبت أن anti-entropy مستقلة عن Realtime.

---

# 138. Corrupted local row test

غيّر field محلية في fixture بدون Outbox.

النتيجة:

```text
manifest mismatch
partition mismatch
entity mismatch
repair restores canonical server truth
```

---

# 139. Missing local row test

احذف row local يدويًا في model fixture.

النتيجة:

```text
anti-entropy detects missing id
fetches canonical row
upserts safely
```

---

# 140. Extra local row test

أضف server-owned row محلية غير موجودة server-side ولا Pending.

النتيجة:

```text
anti-entropy detects extra id
proves absence through scoped inventory
removes row safely
```

---

# 141. Pending extra local row test

إذا extra local row مرتبطة active Outbox:

```text
MUST NOT delete as corruption
```

بل:

```text
defer/reconcile pending mutation
```

---

# 142. Server revision gap test

Page revisions:

```text
100, 101, 105, 109
```

يجب أن:

```text
PASS ordering validation
```

إذا strict increasing.

---

# 143. Server revision regression test

Page revisions:

```text
100, 105, 103
```

يجب أن:

```text
FAIL REMOTE_REVISION_NOT_MONOTONIC
```

ولا apply.

---

# 144. Duplicate revision test

إذا eventين مختلفين بنفس revision في contract الذي يفرض unique revision:

```text
FAIL DUPLICATE_DATA_REVISION
```

إلا إذا server contract اختار revision per transaction وليس per event؛ في هذه الحالة يجب تعديل النموذج صراحة قبل التنفيذ، لا silently.

هذا العقد الافتراضي يطلب revision unique per event.

---

# 145. Transaction group split test

Server fixture يعيد:

```text
page1: G1 event A
page2: G1 event B
```

النتيجة:

```text
FAIL SERVER_GROUP_BOUNDARY_CONTRACT
```

ولا commit A وحده.

---

# 146. Transaction group replay test

نفس group تعاد كاملة.

Inbox dedupe يجعل:

```text
logical result unchanged
cursor deterministic
```

---

# 147. Process death before group commit model

simulate:

```text
apply event1
throw before transaction commit
```

النتيجة:

```text
0 entity changes
0 Inbox applied markers
cursor unchanged
```

---

# 148. Process death after group commit model

بعد commit ثم قبل next network call:

```text
entities applied
Inbox applied
cursor at end(group)
```

restart resumes بعد cursor.

---

# 149. Page replay after crash

إذا response نفسها أعيدت بسبب network ambiguity:

```text
already applied events dedupe
unapplied groups continue
```

ولا duplicate logical effects.

---

# 150. CURSOR_EXPIRED model

fixture:

```text
local cursor=50
server minRevision=100
```

النتيجة:

```text
no delta apply
bootstrap initiated
```

---

# 151. Bootstrap with Pending Outbox model

fixture:

```text
local profile edit Pending
server snapshot has old profile
```

بعد bootstrap:

```text
pending local intent still exists
Outbox still exists
server snapshot does not erase intent
```

ثم push success + feed event converges.

---

# 152. Bootstrap deletion resurrection model

server snapshot لا يحتوي entity قديمة تم حذفها أثناء offline الطويل.

بعد complete bootstrap:

```text
stale local server-owned entity removed
```

ولا resurrect.

---

# 153. Bootstrap cross-scope model

staging لـA لا يمكن قراءتها/تثبيتها لـB.

PK/query paths كلها exact-scope.

---

# 154. Bootstrap restart model

download 3 pages، restart، ثم resume.

يجب ألا:

```text
wipe canonical data
lose staged pages
advance cursor prematurely
```

---

# 155. Anti-entropy targeted repair model

server partition count/digest mismatch واحد فقط.

النتيجة:

```text
repair only affected partition/entities
```

ولا full rebootstrap.

---

# 156. Anti-entropy escalation model

إذا targeted repair يفشل deterministic bounded attempts:

```text
REBOOTSTRAP_REQUIRED
```

ثم safe bootstrap.

---

# 157. Anti-entropy clean model

إذا digests/counts match:

```text
no entity writes
no cursor change
```

فقط reconciliation state قد تتحدث.

---

# 158. No synthetic Inbox during bootstrap

Bootstrap snapshot rows ليست change events.

ممنوع اختراع:

```text
eventId = hash(entity)
serverRevision = baselineRevision لكل row
```

ثم تسجيلها كأحداث حقيقية.

Bootstrap لها state مستقلة.

---

# 159. No synthetic transaction group during bootstrap

لا تُنشئ fake transactionGroupId للsnapshot rows.

Atomic bootstrap install يأتي من bootstrap transaction المحلية، لا من event group مزيف.

---

# 160. No receipt revision contamination

Static verifier يجب أن يبحث عن:

```text
receipt.serverRevision
COMMAND_RECEIPT
```

ويضمن عدم تمريرها إلى:

```text
global sync cursor
UnifiedChangeSynchronizer
data change Inbox revision
bootstrap baselineRevision
```

---

# 161. No chat cursor contamination

Static verifier يجب أن يضمن عدم استخدام:

```text
chat_recovery_seq
lastServerSequence
```

كـ:

```text
global change cursor
bootstrap baseline
anti-entropy manifestRevision
```

---

# 162. updated_at correctness scan

Static scan يجب أن يثبت أن:

```text
updated_at
created_at
System.currentTimeMillis()
```

لا تُستخدم كcanonical delta cursor.

وجودها للعرض/diagnostics مسموح.

---

# 163. Legacy bounded pull scan

بعد canonical cutover، verifier يجب أن يثبت أن bounded snapshot calls لا تُستدعى من steady-state incremental path.

وجودها في compatibility/bootstrap code وحده لا يفشل تلقائيًا.

---

# 164. Canonical mode capability gate

يفضل وجود typed capability/state مثل:

```text
UNIFIED_V1_AVAILABLE
UNIFIED_V1_REQUIRED
```

بعد bootstrap/cursor creation.

ممنوع silent fallback من canonical mode إلى legacy incremental بدون explicit failure truth.

---

# 165. Server contract version mismatch

إذا server يعيد version غير مدعومة:

```text
SYNC_PROTOCOL_VERSION_UNSUPPORTED
```

ولا apply/cursor advance.

---

# 166. Bootstrap contract version mismatch

نفس القاعدة:

```text
BOOTSTRAP_PROTOCOL_VERSION_UNSUPPORTED
```

ولا install.

---

# 167. Manifest contract version mismatch

نفس القاعدة:

```text
RECONCILIATION_PROTOCOL_VERSION_UNSUPPORTED
```

ولا false-clean result.

---

# 168. Required new production classes — conceptual

التسمية قابلة للاختلاف، لكن responsibilities يجب أن توجد:

```text
UnifiedChangeFeed
UnifiedChangeSynchronizer
ChangeEventValidator
ChangeEventApplier/registry
SafeBootstrapSynchronizer
BootstrapSnapshotSource
AntiEntropyReconciler
ReconciliationManifestSource
```

لا يلزم ملف لكل اسم إذا التصميم الأنظف يدمج بعضها.

---

# 169. Required database additions — conceptual

المستهدف:

```text
SyncBootstrapStateEntity
SyncBootstrapStagingEntity
optional SyncReconciliationStateEntity
matching DAOs
MIGRATION_17_18
```

يجب أن تكون كلها scoped.

---

# 170. Required network DTOs — conceptual

يجب فصل:

```text
ChangeFeedPageDto
ChangeEventDto
BootstrapBeginDto
BootstrapPageDto
ReconciliationManifestDto
PartitionInventoryDto
```

عن Room entities.

ممنوع network DTO = Room entity directly إذا يخلط local-only fields.

---

# 171. DI ownership

الـDI يجب أن يجعل:

```text
SyncManager -> canonical synchronizer
```

بعد cutover.

ولا يبقى:

```text
SyncManager -> LegacyRemotePuller + DeletionSynchronizer
```

كـparallel correctness owners.

---

# 172. SyncPhase handling

يمكن الحفاظ على phases UI الحالية لتجنب drift.

لكن التنفيذ الداخلي يجب أن يعكس protocol الجديد.

لا يشترط redesign enum إذا لا حاجة.

يمكن استخدام:

```text
existing domain phases for progress mapping
```

مع تفاصيل diagnostics الداخلية.

---

# 173. Production UI gate

```text
productionUiFilesChanged = 0
```

إلا compile blocker موثق يفرض تعديل API reference بسيط دون UX change.

ممنوع redesign screens في 72.

---

# 174. Business rules gate

ممنوع تغيير:

```text
commission formulas
withdrawal eligibility
balance calculations
chat UX semantics
pricing/business logic
```

72 infrastructure-only.

---

# 175. Media behavior gate

ممنوع إعادة تصميم upload queue v71.

التغيير الوحيد المقبول:

```text
feed/anti-entropy can reconcile message metadata references
```

ولا يغير transfer ownership.

---

# 176. Conversation create gate

لا تغير idempotent create semantics v71.

فقط ensure successful server mutation produces canonical data event.

---

# 177. Read receipt gate

Read receipt Outbox remains.

server read mutation يجب أن ينتج data event إذا يغير replicated state.

---

# 178. Notification read gate

نفس القاعدة:

```text
command receipt proves command
change feed propagates state
```

---

# 179. Withdrawal gate

Withdrawal command receipt + data event يجب أن يبقيا distinct.

Anti-entropy لا تعيد تنفيذ withdrawal command.

---

# 180. Server RLS negative tests

إلزامي:

```text
anon cannot call feed/bootstrap/manifest
user A cannot request org B
user A cannot read B's events
client-supplied forged scope rejected
raw change-log direct select denied
```

---

# 181. Server delete coverage tests

لكل in-scope entity يدعم DELETE:

```text
delete row
assert one canonical DELETE event visible to correct scope
assert not visible to wrong scope
```

إذا table لا تدعم deletion business-wise، سجل `NOT_APPLICABLE` بدليل.

---

# 182. Server upsert coverage tests

لكل in-scope entity:

```text
insert/update
assert canonical UPSERT event
assert revision monotonic
assert event identity stable
```

---

# 183. Server transaction group test

داخل transaction واحدة غيّر entityين مرتبطين.

assert:

```text
same transactionGroupId
different eventId
different revision
group not split by feed pagination
```

---

# 184. Server rollback test

ابدأ transaction تغير row ثم rollback.

assert:

```text
no committed row change
no committed change event
```

sequence gap مسموح.

---

# 185. Server revision gap test

تعمد rollback بعد nextval إن أمكن.

assert feed يتعامل مع gap دون CURSOR_GAP failure.

---

# 186. Server retention/cursor expiry test

باستخدام fixture أو controlled minRevision:

assert:

```text
old cursor -> CURSOR_EXPIRED
not empty success
```

---

# 187. Bootstrap server consistency test

بين begin وpage reads نفذ mutation جديدة.

assert:

```text
snapshot remains pinned to bootstrap baseline semantics
new mutation appears later in delta > baseline
```

---

# 188. Bootstrap scope test

bootstrapId لـA لا يمكن استخدامه بواسطة B.

النتيجة typed unauthorized/not found دون leak.

---

# 189. Bootstrap expiry test

expired bootstrapId:

```text
BOOTSTRAP_EXPIRED
```

ولا returns partial stale pages.

---

# 190. Manifest stability test

لنفس server state/revision:

```text
manifest digest deterministic across repeated calls
```

---

# 191. Manifest sensitivity test

غيّر canonical field واحدة.

assert affected partition/root digest يتغير.

غيّر local-only concept غير موجود server-side: لا ينطبق.

---

# 192. Partition inventory test

م mismatch partition returns deterministic sorted IDs/digests.

pagination إن وجدت يجب أن تكون stable.

---

# 193. Targeted repair server auth test

لا يمكن طلب arbitrary IDs خارج scope والحصول على data leak.

IDs خارج scope:

```text
omitted/typed forbidden حسب contract
```

بدون كشف sensitive metadata غير ضروري.

---

# 194. Required static verifier

أضف:

```text
scripts/verify-v72-static.py
```

ويتحقق على الأقل من:

```text
baseline drift
Room version/migration
new change feed classes
canonical stream
no command receipt cursor contamination
no chat cursor contamination
no Realtime Room writes regression
legacy steady-state authority removed/gated
bootstrap staging scoped
logout cleanup scoped
historical migrations unchanged
UI drift zero
new waivers zero
```

---

# 195. Required model verifier

أضف:

```text
scripts/verify-v72-model.py
```

يغطي على الأقل:

```text
revision gaps
duplicate page
identity conflict
transaction group atomicity
group boundary
cursor rollback
CURSOR_EXPIRED
bootstrap baseline no-gap
pending local bootstrap
restart bootstrap
anti-entropy missing/extra/different
targeted repair
rebootstrap escalation
cross-scope stale callback
```

ويُشغل مرتين deterministically.

---

# 196. Required migration model verifier

أضف:

```text
scripts/verify-v72-migration.py
```

ويتحقق من DDL 17->18 static/model semantics إذا Gradle instrumentation غير متاح.

هذا لا يعوض Android migration test.

---

# 197. Required server contract SQL verifier

أضف:

```text
tools/verify_v72_unified_sync_server_contract.sql
```

ويفحص deployed schema/runtime حيث أمكن:

```text
objects exist
permissions
RLS
revision separation
change capture coverage
transaction groups
feed pagination
cursor expiry
bootstrap
manifest
```

---

# 198. Required inventory artifacts

يجب إنتاج:

```text
AUTODRIVE_SYNC_CHANGE_FEED_INVENTORY_v72.json
AUTODRIVE_SYNC_BOOTSTRAP_INVENTORY_v72.json
AUTODRIVE_SYNC_ANTI_ENTROPY_INVENTORY_v72.json
```

كلها machine-readable.

---

# 199. Required verification artifacts

يجب إنتاج:

```text
AUTODRIVE_SYNC_VERIFICATION_v72.json
AUTODRIVE_SYNC_VERIFICATION_v72.md
```

وبصمات SHA-256 لهما.

---

# 200. Verification JSON — baseline fields

يحتوي على الأقل:

```text
session
sourceArchive
sourceSha256
archiveEntries
productionKotlinFilesBefore
productionKotlinFilesAfter
testKotlinFilesBefore
testKotlinFilesAfter
roomVersionBefore
roomVersionAfter
v71FinalVerdict
v71Handoff72Authorized
userExecutionOverrideAccepted
predecessorGateSatisfied
```

---

# 201. Verification JSON — feed fields

```text
unifiedFeedImplemented
canonicalStreamName
dataRevisionContractVersion
changeFeedEntityCount
upsertCoverageCount
deleteCoverageCount
changeFeedRuntimeVerified
revisionGapTestPassed
transactionGroupTestPassed
legacyIncrementalAuthorityCount
separateDeletionAuthorityCount
commandReceiptCursorContaminationCount
chatCursorContaminationCount
```

---

# 202. Verification JSON — bootstrap fields

```text
safeBootstrapImplemented
bootstrapServerContractVerified
bootstrapLocalStagingImplemented
bootstrapNoGapModelPassed
bootstrapPendingLocalModelPassed
bootstrapCrossScopeModelPassed
cursorExpiredHandled
bootstrapRuntimeVerified
```

---

# 203. Verification JSON — anti-entropy fields

```text
antiEntropyImplemented
manifestContractVersion
manifestRuntimeVerified
targetedRepairImplemented
rebootstrapEscalationImplemented
localCorruptionDetectedModel
missingRowDetectedModel
extraRowDetectedModel
pendingLocalProtectedModel
```

---

# 204. Verification JSON — regression fields

```text
v67Model
v68Model
v69Model
v70Model
v71Model
v71MigrationModel
chat10kVerified
realtimeDirectRoomWriteCount
realtimeTransitiveRoomWriteCount
productionUiFilesChanged
historicalRoomMigrationMutationCount
historicalServerMigrationMutationCount
newV72WaiverCount
```

---

# 205. Verification JSON — runtime truth

```text
COMPILED
UNIT_TESTED
ANDROID_MIGRATION_TESTED
SERVER_CHANGE_FEED_RUNTIME_VERIFIED
SERVER_BOOTSTRAP_RUNTIME_VERIFIED
SERVER_ANTI_ENTROPY_RUNTIME_VERIFIED
REALTIME_RUNTIME_TESTED
```

لا infer لأي قيمة `true`.

---

# 206. Diff inventory

يجب تسجيل:

```text
changedExistingFiles
addedFiles
deletedFiles
productionFilesTouched
testFilesTouched
serverMigrationsAdded
historicalMigrationsMutated
unexpectedProductionMutations
```

---

# 207. No new waiver rule

```text
newV72WaiverCount = 0
```

أي waiver جديد:

```text
FAIL_SCOPE_INTEGRITY
```

إلا إذا المستخدم وافق عليه صراحة في نفس التنفيذ، ويظل موثقًا ولا يتحول إلى PASS كامل تلقائيًا.

---

# 208. Production file scope المسموح

مسموح عند الحاجة:

```text
core/sync/**
core/network/**
core/database/**
core/session/** فقط لربط scope إذا لزم
app DI/migration registration/tests
feature/*/data adapters فقط إذا feed mapping لا يمكن عزله core-side
scripts/**
tools/**
supabase/migrations/** new v72 file only
```

---

# 209. Forbidden scope

ممنوع:

```text
Compose redesign
navigation redesign
new user-facing reconciliation screen
commission logic changes
withdrawal business rule changes
media UX changes
chat UI pagination changes
analytics overhaul
full observability redesign
full v73 fault-injection framework
```

---

# 210. Build gate

يجب محاولة Gradle فعليًا.

الحد الأدنى:

```text
./gradlew --version --console=plain
```

ثم tasks المناسبة إذا bootstrap نجح.

إذا فشل بـ:

```text
UnknownHostException: services.gradle.org
```

يسجل:

```text
COMPILED=false
UNIT_TESTED=false
ANDROID_MIGRATION_TESTED=false
```

ولا يدعى PASS runtime.

---

# 211. Compile task

إذا Gradle متاح، شغّل على الأقل compile مناسب للموديولات المتغيرة، ويفضل app debug compile/build حسب المشروع.

الفشل compile:

```text
FAIL_COMPILE
```

حتى لو static/model PASS.

---

# 212. Unit tests

إذا Gradle متاح، شغّل unit tests المتأثرة بـ:

```text
core:sync
core:database
core:network
app architecture tests
```

حسب task graph الفعلي.

---

# 213. Android migration tests

إذا emulator/device/instrumentation غير متاح:

```text
ANDROID_MIGRATION_TESTED=false
```

ولا static migration model يغيرها إلى true.

---

# 214. Server runtime gate

وجود SQL source لا يكفي.

`SERVER_CHANGE_FEED_RUNTIME_VERIFIED=true` فقط إذا:

```text
migration deployed to authoritative test/live target intended by user
contract verifier executed
positive + negative tests pass
```

---

# 215. Bootstrap runtime gate

`SERVER_BOOTSTRAP_RUNTIME_VERIFIED=true` فقط إذا:

```text
begin/page/expiry/scope tests executed against deployed server
snapshot-delta no-gap verified
```

---

# 216. Anti-entropy runtime gate

`SERVER_ANTI_ENTROPY_RUNTIME_VERIFIED=true` فقط إذا:

```text
manifest deterministic
mismatch detected
partition inventory works
targeted repair authorization works
```

على server deployed.

---

# 217. Historical Room migration hashes

يجب حفظ block/file evidence أن migrations السابقة لم تتغير.

الحد الأدنى:

```text
historicalRoomMigrationMutationCount = 0
```

---

# 218. Historical server migration hashes

يجب الحفاظ على:

```text
v69 SQL SHA = 6663381c4bf177c7cc22c75fb4c1eee1683290894307ec9ade85e4fe7620c01e
v71 SQL SHA = e945ca54902b28e592250e3763a5584e84cd2ac08f35d53c03f8b918151ec641
```

---

# 219. Required server migration count

المتوقع:

```text
newServerMigrationCount = 1
```

يمكن أكثر فقط إذا authoritative schema يفرض فصلًا واضحًا وموثقًا.

ممنوع تعديل migrations القديمة بدل إضافة الجديدة.

---

# 220. Required Room migration count

المتوقع:

```text
newRoomMigrationCount = 1
17 -> 18
```

إذا المنفذ أثبت design آمن بدون persistent bootstrap staging، يجب توثيق reasoning قوي؛ لكن هذا العقد الافتراضي يعتبر durable staging requirement، لذلك عدم وجود migration يحتاج blocker/contract amendment لا تجاهل.

---

# 221. Determinism gate

شغل:

```text
verify-v72-static.py twice
verify-v72-model.py twice
verify-v72-migration.py twice
```

المخرجات الأساسية يجب أن تكون byte-identical أو deterministic logically مع timestamps مستثناة بشكل موثق.

---

# 222. Secret scan

افحص التغييرات/التغليف ضد:

```text
service_role
JWT secret
access token
refresh token
password
OTP
raw push token
private storage token
bank data dump
raw change-feed payload dump
```

أي secret حقيقي:

```text
BLOCKED_SECRET_LEAK
```

---

# 223. Generated junk policy

ممنوع تضمين:

```text
.gradle/
build/
IDE caches
local.properties real secrets
keystores
runtime DB copies
Supabase credentials
```

في source-of-truth ZIP.

---

# 224. Source package naming

المخرج المقترح:

```text
AutoDrive-v72-unified-change-feed-bootstrap-anti-entropy.zip
```

أو اسم مكافئ واضح.

يجب أن يحتوي داخل root:

```text
SESSION_72_FINAL.md
AUTODRIVE_SYNC_VERIFICATION_v72.md/.json
inventories
verification scripts
new migration(s)
```

---

# 225. Package SHA

بعد التغليف:

```text
sha256sum output zip
```

ويُسجل في verification النهائي.

---

# 226. Required final truth table

التقرير النهائي يجب أن يعرض صراحة:

```text
IMPLEMENTED
STATIC_VERIFIED
MODEL_VERIFIED
MIGRATION_MODEL_VERIFIED
COMPILED
UNIT_TESTED
ANDROID_MIGRATION_TESTED
SERVER_CHANGE_FEED_RUNTIME_VERIFIED
SERVER_BOOTSTRAP_RUNTIME_VERIFIED
SERVER_ANTI_ENTROPY_RUNTIME_VERIFIED
PREDECESSOR_GATE_SATISFIED
V67_TOMBSTONE_BLOCKER_SUPERSEDED
handoff73Authorized
```

---

# 227. Verdict ladder

الـverdict يستخدم أقصى truth مثبتة فقط.

أمثلة:

```text
BLOCKED_INPUT_DRIFT
BLOCKED_PREDECESSOR_HANDOFF
BLOCKED_AUTHORITATIVE_SERVER_SCHEMA
IMPLEMENTED_STATIC_MODEL_RUNTIME_BLOCKED_PREDECESSOR_OVERRIDDEN
IMPLEMENTED_STATIC_MODEL_SERVER_RUNTIME_PENDING
IMPLEMENTED_COMPILED_SERVER_RUNTIME_PENDING
IMPLEMENTED_RUNTIME_VERIFIED_PREDECESSOR_OPEN
FULL_PASS
```

---

# 228. FULL_PASS conditions

`FULL_PASS` ممنوع إلا إذا كلها true:

```text
predecessorGateSatisfied
COMPILED
UNIT_TESTED
ANDROID_MIGRATION_TESTED
SERVER_CHANGE_FEED_RUNTIME_VERIFIED
SERVER_BOOTSTRAP_RUNTIME_VERIFIED
SERVER_ANTI_ENTROPY_RUNTIME_VERIFIED
change-feed coverage complete
revision separation proven
transaction-group boundary proven
cursor expiry/bootstrap proven
anti-entropy targeted repair proven
all inherited regression gates pass
newV72WaiverCount = 0
productionUiFilesChanged = 0
historical migrations untouched
```

---

# 229. Predecessor override verdict ceiling

إذا التنفيذ تم تحت override بينما predecessor ما زال open:

```text
FULL_PASS = forbidden
handoff73Authorized = false
```

حتى لو static/model/compile/server v72 نجحت، إلا إذا v72 runtime evidence نفسه supersedes ويغلق blocker الموروث رسميًا كما في قسم 55.

---

# 230. Handoff إلى Session 73

`handoff73Authorized=true` فقط إذا:

```text
predecessorGateSatisfied=true
unified change feed canonical=true
global cursor active=true
legacy incremental authority disabled/gated
safe bootstrap verified=true
anti-entropy verified=true
no critical blocker open
v67-v71 regressions pass
newV72WaiverCount=0
```

وإلا:

```text
handoff73Authorized=false
```

---

# 231. ما يجب أن تستلمه Session 73

```text
AutoDrive-v72-*.zip
SESSION_72_FINAL.md
AUTODRIVE_SYNC_VERIFICATION_v72.json/.md
AUTODRIVE_SYNC_CHANGE_FEED_INVENTORY_v72.json
AUTODRIVE_SYNC_BOOTSTRAP_INVENTORY_v72.json
AUTODRIVE_SYNC_ANTI_ENTROPY_INVENTORY_v72.json
new v72 server migration
server verification SQL
v72 static/model/migration verifiers
runtime blocker logs if any
```

---

# 232. ما لا تعيده Session 73

73 لا تعيد بناء:

```text
change feed
global revision
bootstrap
anti-entropy
```

إلا إصلاح regression/blocker يمنع observability/fault injection.

73 تملك:

```text
full observability
correlation IDs/metrics
fault-injection closure
end-to-end convergence proof
```

---

# 233. Implementation order — إلزامي

```text
1. Freeze baseline + hashes.
2. Read v71 verification/contract and v70/v69 invariants.
3. Acquire authoritative current server schema/runtime evidence.
4. Build write-path/change-feed coverage inventory BEFORE SQL mutation.
5. Define DATA revision + event + group + retention contract.
6. Implement append-only server migration.
7. Implement server feed/bootstrap/manifest contract verifiers.
8. Add Room 17->18 bootstrap/reconciliation state.
9. Add network DTOs/adapters.
10. Implement UnifiedChangeFeed + validator.
11. Implement entity applier registry with pending-local protection.
12. Implement atomic UnifiedChangeSynchronizer.
13. Implement SafeBootstrapSynchronizer + durable staging.
14. Implement AntiEntropyReconciler + targeted repair.
15. Wire SyncManager canonical flow.
16. Gate/retire legacy incremental + separate deletion authority.
17. Update logout exact-scope cleanup.
18. Add static/model/migration/server tests.
19. Run inherited v67-v71 regressions.
20. Attempt Gradle compile/tests/migration tests.
21. Run server runtime tests if authoritative server access/evidence available.
22. Generate inventories + verification truth.
23. Package source-of-truth ZIP + SHA.
```

ممنوع القفز إلى 9 قبل حسم 3–5.

---

# 234. Pre-implementation questions — يجب الإجابة من evidence لا التخمين

```text
Q1  ما schema الحالي الفعلي لكل in-scope server table؟
Q2  كيف يشتق server user/client/org scope لكل table؟
Q3  هل sync_tombstones موجود live؟ وما retention/columns الفعلية؟
Q4  ما كل write paths الحالية لكل entity؟
Q5  هل delete مسموح لكل entity أم بعضها append-only؟
Q6  كيف سنولد transactionGroupId ثابت داخل transaction؟
Q7  هل event revision per-event أم per-transaction؟ هذا العقد يفترض per-event.
Q8  كيف نضمن page boundary لا تقسم group؟
Q9  ما retention الفعلي للchange log؟
Q10 كيف يعلن server minimumAvailableRevision؟
Q11 كيف يثبت bootstrap snapshot عبر multi-call pagination؟
Q12 ما snapshot entities المطلوبة لأول bootstrap؟
Q13 كيف تُحمى pending local rows أثناء install؟
Q14 ما canonical hash fields لكل entity للanti-entropy؟
Q15 ما partition scheme؟
Q16 ما targeted repair API؟
Q17 هل server runtime يمكن اختباره الآن أم source-only؟
Q18 هل v72 unified delete feed يمكنه رسميًا supersede v67 blocker؟
```

أي سؤال مؤثر بلا evidence:

```text
BLOCKED_UNRESOLVED_PROTOCOL_DECISION
```

ولا يُحل بالتخمين.

---

# 235. Acceptance matrix — Change Feed

PASS فقط إذا:

```text
[ ] data revision distinct from command receipt revision
[ ] revisions monotonic, gaps allowed
[ ] canonical event identity
[ ] UPSERT + DELETE same feed
[ ] exact scope isolation
[ ] complete write-path coverage inventory
[ ] transaction group identity
[ ] no group split across page boundary
[ ] durable scoped global cursor
[ ] Inbox + apply + cursor atomic
[ ] duplicate replay idempotent
[ ] unsupported/malformed event fails closed
[ ] legacy incremental authority disabled/gated after cutover
```

---

# 236. Acceptance matrix — Bootstrap

```text
[ ] CURSOR_EXPIRED typed
[ ] consistent server snapshot
[ ] baseline data revision bound to snapshot
[ ] durable local staging
[ ] exact-scope staging
[ ] pending Outbox preserved
[ ] stale server-owned rows removed only after complete snapshot
[ ] final install + baseline cursor atomic
[ ] restart safe
[ ] snapshot/delta no-gap test
[ ] bootstrap expiry handled
[ ] no full wipe default
```

---

# 237. Acceptance matrix — Anti-Entropy

```text
[ ] server manifest deterministic
[ ] canonical digest versioned
[ ] local-only fields excluded
[ ] pending-local partitions handled safely
[ ] deterministic partitions
[ ] missing row detection
[ ] extra row detection
[ ] changed row detection
[ ] targeted repair
[ ] post-repair recheck
[ ] rebootstrap escalation
[ ] no default full wipe
```

---

# 238. Acceptance matrix — Regressions

```text
[ ] v67 generation safety
[ ] v68 atomic/scoped Outbox
[ ] v69 idempotent receipts
[ ] v70 Inbox atomicity
[ ] v70 Realtime hint-only
[ ] v71 chat 10k
[ ] v71 durable media
[ ] v71 conversation idempotency/dependency
[ ] logout exact-scope isolation
[ ] zero production UI drift
[ ] zero historical migration mutation
[ ] zero new waivers
```

---

# 239. Expected architecture after 72

```text
LOCAL WRITE
  ↓
Room transaction(Entity + scoped Outbox)
  ↓
Idempotent Server Command
  ↓
Command Receipt (COMMAND_RECEIPT revision)
  ↓
Server business mutation
  ↓ SAME SERVER TX
Unified Change Log (DATA_CHANGE revision)
  ↓
Scoped Unified Change Feed
  ↓
Durable Global Data Cursor
  ↓
Scoped Inbox + transaction-group atomic apply
  ↓
Room Source of Truth
  ↓
UI

Realtime = Hint Only
ChatRecovery = targeted compatibility/repair
Bootstrap = safe cursor recovery
Anti-Entropy = divergence detection + repair
```

---

# 240. Invariant — one canonical incremental authority

```text
If steady-state correctness still requires LegacyRemotePuller snapshots,
DeletionSynchronizer cursor, and global feed simultaneously,
Session 72 is not complete.
```

---

# 241. Invariant — revision separation

```text
If COMMAND_RECEIPT revision can advance DATA_CHANGE cursor,
Session 72 is invalid.
```

---

# 242. Invariant — chat cursor separation

```text
If chat_recovery_seq is used as global data revision,
Session 72 is invalid.
```

---

# 243. Invariant — monotonic not contiguous

```text
If client rejects a valid server revision gap solely because +1 is missing,
Session 72 is invalid.
```

---

# 244. Invariant — group atomicity

```text
If one server transaction group can become partially visible in Room,
Session 72 is not complete.
```

---

# 245. Invariant — cursor atomicity

```text
If cursor can advance beyond data/Inbox not committed locally,
Session 72 is not complete.
```

---

# 246. Invariant — bootstrap no-gap

```text
If a mutation committed between snapshot start and delta resume can be lost,
Session 72 is not complete.
```

---

# 247. Invariant — pending intent preservation

```text
If bootstrap/repair can delete or overwrite active local Outbox intent,
Session 72 is invalid.
```

---

# 248. Invariant — anti-entropy independent detection

```text
If silent local divergence cannot be detected without a new Realtime event,
Session 72 is not complete.
```

---

# 249. Invariant — no default wipe

```text
If first repair action for digest mismatch is clear-all Room,
Session 72 is invalid.
```

---

# 250. Invariant — no synthetic event truth

```text
If bootstrap snapshot rows receive fabricated eventId/revision/group
only to satisfy Inbox APIs, Session 72 is invalid.
```

---

# 251. Invariant — exact scope

```text
If any cursor/bootstrap staging/manifest result can cross user/client/org scope,
Session 72 is invalid.
```

---

# 252. Invariant — Realtime remains acceleration only

```text
Disabling Realtime MUST NOT prevent eventual convergence
through scheduled/manual canonical sync.
```

---

# 253. Invariant — retention honesty

```text
If server can silently return empty success for an expired cursor,
Session 72 is invalid.
```

---

# 254. Invariant — write-path completeness

```text
If an in-scope server mutation can commit without a canonical change event,
Session 72 is not complete.
```

---

# 255. Invariant — immutable event identity

```text
If same eventId can later describe a different entity/revision/operation,
Session 72 is invalid.
```

---

# 256. Invariant — bootstrap install atomicity

```text
If canonical Room can expose half the new bootstrap snapshot with baseline cursor committed,
Session 72 is invalid.
```

---

# 257. Invariant — targeted repair first

```text
If one partition mismatch forces full account rebootstrap without targeted attempt,
Session 72 is incomplete unless the server explicitly cannot localize divergence
and that limitation is documented as blocker rather than PASS.
```

---

# 258. Invariant — runtime truth

```text
Static SQL existence != deployed runtime verification.
```

```text
Model test PASS != Android instrumentation PASS.
```

```text
Source compile smoke != Gradle Android compile PASS.
```

---

# 259. Current contract state

بناءً على v71 المفحوص:

```text
CONTRACT_READY = true
SOURCE_INSPECTED = true
PLAN_MAPPING_RESOLVED = true

SourceZipSha = c1367830c0b7332c15d9e1a71476a8242537a61e39deca5c11e7a2d89768cfd2
RoomBaseline = 17
UnifiedChangeFeedPresent = false
GlobalDataRevisionPresent = false
SafeBootstrapPresent = false
AntiEntropyPresent = false
LegacyRemotePullerSteadyStatePresent = true
BlockedServerDeletionFeedBound = true
ChatRecoveryCompatibilityCursorPresent = true
CommandReceiptRevisionPresent = true
ScopedInboxPresent = true
ScopedCursorFoundationPresent = true
RealtimeHintOnly = true

v71FinalVerdict = IMPLEMENTED_STATIC_MODEL_RUNTIME_BLOCKED_PREDECESSOR_OVERRIDDEN
handoff72Authorized = false
predecessorGateSatisfied = false
```

إذًا:

```text
EXECUTION_WITHOUT_OVERRIDE = BLOCKED_PREDECESSOR_HANDOFF
```

وكذلك:

```text
FINAL_SERVER_SQL WITHOUT AUTHORITATIVE SCHEMA EVIDENCE = BLOCKED_AUTHORITATIVE_SERVER_SCHEMA
```

---

# 260. الخلاصة النهائية للعقد

Session 72 تنجح فقط إذا تحول AutoDrive من:

```text
positive snapshots
+ blocked/independent tombstone abstraction
+ chat compatibility cursor
+ no safe bootstrap
+ no anti-entropy
```

إلى:

```text
one scoped Unified Change Feed
+ independent monotonic DATA revision
+ transaction-group-safe pagination
+ durable global cursor
+ Inbox + apply + cursor atomicity
+ safe CURSOR_EXPIRED bootstrap
+ snapshot/baseline no-gap proof
+ pending-local-preserving install
+ deterministic anti-entropy manifest
+ targeted repair
+ scoped rebootstrap escalation
```

مع الحفاظ الكامل على:

```text
v67 generation safety
v68 transactional scoped Outbox
v69 idempotent command receipts
v70 durable Inbox + Realtime hint-only
v71 chat recovery/media/conversation guarantees
```

والقاعدة النهائية:

```text
A sync system is not deterministic because it polls more often.
It is deterministic only when every committed server change has a recoverable ordered identity,
every client cursor advances atomically with apply,
expired history has a safe bootstrap path,
and silent divergence can be detected and repaired independently.
```

---

# END OF SESSION_72_FINAL.md
