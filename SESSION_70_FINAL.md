# SESSION_70_FINAL.md

## AutoDrive Sync Modernization — Session 70

### Durable Scoped Inbox + Atomic Inbound Apply + Realtime Hint-Only

**نوع المستند:** عقد تنفيذ مستقل وصارم للجلسة الرابعة من مسار تحديث مزامنة AutoDrive المضغوط v67→v73  
**الجلسة:** 70  
**الحالة:** `PLAN ONLY — READY AS CONTRACT; EXECUTION GATED BY v69 HANDOFF / PREDECESSOR CHAIN`  
**تاريخ الصياغة:** 2026-08-21  
**مصدر الكود المفحوص:** `AutoDrive-v69-idempotent-command-contract.zip`  
**SHA-256 للمصدر المفحوص:** `45193784dadacf5d78501265468a48f51803ee5577e5a8c27bfe6db6561f6fa9`  
**Archive entries:** `1302`  
**Production Kotlin files:** `260`  
**Test Kotlin files:** `45`  
**Room الحالي:** `15`  
**Room المستهدف في 70:** `16`  
**مرجع التنفيذ السابق داخل ZIP:** `SESSION_69_FINAL.md`  
**SHA-256 لمرجع 69 داخل ZIP:** `24872b8021331d15135dab6c27a80d945df5ed326918c82b3e2ff952aea132`  
**تقرير تحقق v69 JSON داخل ZIP:** `AUTODRIVE_SYNC_VERIFICATION_v69.json`  
**SHA-256:** `04bd7ce9b8b4d4b204b008286ceefffe87588c59b3c3281d9ad97ace8db0e87c`  
**تقرير تحقق v69 MD المرفق/داخل ZIP:** `AUTODRIVE_SYNC_VERIFICATION_v69.md`  
**SHA-256:** `2ca90aaa4b2c2a7133b37bbc06287842885c102938f8989787fd26c0bdfdfe7c`  
**v69 final verdict:** `IMPLEMENTED_STATIC_BUT_PREDECESSOR_BLOCKED`  
**v69 handoff70Authorized:** `false`  
**v69 static:** `93/93 PASS` deterministic ×2  
**v69 model:** `15/15 PASS`  
**inherited v67 model:** `22/22 PASS`  
**inherited v68 model:** `36/36 PASS`  
**v69 server runtime:** `NOT RUN`  
**v69 Android compile:** `false — Gradle bootstrap blocked by UnknownHostException: services.gradle.org`  
**v67 inherited blocker:** `BLOCKED_SERVER_TOMBSTONE_CONTRACT`  
**production UI drift in v69:** `0`  
**new v69 waivers:** `0`

---

# 0. الحكم التنفيذي المختصر

Session 70 في **الخطة المضغوطة** ليست إعادة تنفيذ Idempotent Commands.

69 نفذت بالفعل:

```text
Scoped transactional Outbox
        ↓
stable mutationId
        ↓
typed idempotent server command
        ↓
durable canonical command receipt
        ↓
typed retry/reconciliation
```

70 تغلق الجهة المقابلة، أي **مسار الاستقبال**، وتزيل سلطة Realtime على Room:

```text
Authoritative pull/event batch
        ↓
validate scope + event identity
        ↓
Room.withTransaction {
    dedupe by durable Inbox
    apply entity/tombstone
    record Inbox outcome
    advance exact stream cursor
}
        ↓
Room remains the only UI source
```

وفي Realtime:

```text
Realtime event
        ↓
HINT ONLY
        ↓
SyncCoordinator.requestSync(REALTIME_HINT)
        ↓
existing generation-safe drain
        ↓
authoritative pull
        ↓
atomic Inbox/apply
        ↓
Room
```

القواعد المطلقة:

```text
Realtime MUST NOT insert/update/delete Room.
```

```text
A Realtime payload MUST NOT be treated as authoritative business state.
```

```text
A DELETE oldRecord MUST NOT be required for correctness.
```

```text
An inbound event already applied MUST NOT be applied a second time.
```

```text
Entity/tombstone + Inbox + cursor advancement MUST commit or roll back together
for cursor-bearing event streams.
```

```text
v69 command-receipt revision MUST NEVER become an inbound sync cursor.
```

```text
No eventId or serverRevision may be fabricated for legacy snapshot pulls.
```

```text
Network I/O MUST stay outside Room transactions.
```

```text
No predecessor blocker may be hidden by a PASS label.
```

---

# 1. لماذا Session 70 الحالية = Inbox + Realtime Hint-Only

الخطة الأصلية v67→v80 كانت ترتب:

```text
70 = Idempotent Command Contract
71 = Durable Inbox + Atomic Apply
72 = Realtime Hint-Only
```

لكن عقد Session 67 المضغوط v67→v73 نقل:

```text
Idempotent server commands + typed retry -> 69
Durable Inbox                            -> 70
Remove Realtime direct Room writes       -> 70
Chat 10k + media queue                   -> 71
Unified Change Feed / global revision    -> 72
Bootstrap / anti-entropy                 -> 72
Observability / fault injection          -> 73
```

ثم Session 69 نفسها سلّمت 70 صراحة:

```text
Durable Inbox
Atomic Apply
Realtime direct-write removal / hint-only transition
```

لذلك:

```text
SESSION_70_SCOPE =
    DURABLE_SCOPED_INBOX
  + ATOMIC_INBOUND_APPLY
  + REALTIME_HINT_ONLY
  + AGGREGATE_REALTIME_HEALTH
```

وأي إعادة بناء للـIdempotent Command Contract تعتبر:

```text
BLOCKED_DUPLICATE_SCOPE
```

---

# 2. بوابة البداية — v69 Handoff Gate

قبل أي mutation يجب قراءة:

```text
AUTODRIVE_SYNC_VERIFICATION_v69.json
AUTODRIVE_SYNC_VERIFICATION_v69.md
SESSION_69_FINAL.md
```

والتحقق من:

```text
finalVerdict
handoff70Authorized
predecessorGateSatisfied
Room version
v69 static/model results
newV69WaiverCount
newServerMigrationCount
typedRetryTaxonomyPresent
commandReceiptContractPresent
```

الحالة الحالية المثبتة:

```text
finalVerdict             = IMPLEMENTED_STATIC_BUT_PREDECESSOR_BLOCKED
handoff70Authorized      = false
predecessorGateSatisfied = false
Room                     = 15
new Room migrations      = 0
new server migrations    = 1
production UI drift      = 0
new waivers              = 0
```

إذًا افتراضيًا:

```text
SESSION_70_EXECUTION_GATE = BLOCKED_PREDECESSOR_HANDOFF
```

يجوز تنفيذ 70 فقط إذا:

```text
A) أُغلق blocker الموروث رسميًا وأصبح handoff70Authorized=true
```

أو:

```text
B) أصدر المستخدم Override صريحًا لتنفيذ 70 فوق السلسلة المحجوبة
```

في الحالة B:

```text
IMPLEMENTATION MAY PROCEED
FINAL RELEASE PASS IS FORBIDDEN
inherited blocker MUST remain visible
```

والـverdict الأقصى عند نجاح static/model تحت override:

```text
IMPLEMENTED_STATIC_BUT_PREDECESSOR_BLOCKED
```

---

# 3. Authority Order

عند التعارض:

1. `AutoDrive-v69-idempotent-command-contract.zip` بالـSHA المثبت أعلاه.
2. `AUTODRIVE_SYNC_VERIFICATION_v69.json/.md` داخل المصدر.
3. `SESSION_69_FINAL.md` داخل المصدر، خصوصًا Handoff 70.
4. `SESSION_67_FINAL.md` لتحديد خريطة الخطة المضغوطة v67→v73.
5. `AUTODRIVE_SYNC_MODERNIZATION_REPAIR_PLAN_v67-v80.md` لبنود Inbox وRealtime الأصلية فقط، مع إعادة الترقيم المضغوط.
6. authoritative server schema/evidence المستخدم في 69 حيث يلزم فقط لفهم الحدود الحالية.
7. كود v69 الفعلي.
8. هذا العقد.

ممنوع:

```text
import Verto/Optimal SQL
invent server change-feed columns
invent revision semantics
reuse command receipt revision as data cursor
```

---

# 4. Baseline identity gate

قبل أي تعديل يجب إثبات:

```text
ZIP SHA-256                 = 45193784dadacf5d78501265468a48f51803ee5577e5a8c27bfe6db6561f6fa9
archive entries             = 1302
production Kotlin           = 260
test Kotlin                 = 45
Room                        = 15
server migration files      = 4
v69 UI drift                = 0
v69 waivers                 = 0
```

إذا اختلف المصدر دون توثيق:

```text
BLOCKED_INPUT_DRIFT
```

إذا Room ليست 15:

```text
BLOCKED_ROOM_BASELINE_DRIFT
```

إذا تغيرت migration v69 التاريخية:

```text
BLOCKED_HISTORICAL_MIGRATION_DRIFT
```

---

# 5. Critical-file fingerprints

يجب تثبيت هذه البصمات قبل mutation:

```text
core/database/.../AutoDriveDatabase.kt
75ee82632d32b389ac6efd065e8592c7461037287f69a3e447b5d3a3ea29878f

core/database/.../entities/SyncCursorEntity.kt
62b31a3377f4a4693cbd2fbc63c37c22ec2d9ff705780749cc8944f9f57e03dd

core/database/.../dao/SyncCursorDao.kt
9040dc6a136b37b26bdfe5fc8ac06ae952917df97896072cc3f2c07dfdcb2f07

core/sync/.../data/RemoteSyncSemantics.kt
3d2dd88e8b5d07d028cd6ea998f4e6b0e581638ff4e7e37b89e154c487e7fcce

core/sync/.../data/DeletionSynchronizer.kt
6aa926bf143e61331b9f2a46275a6f990233cd9747aa8a7560acbff72861b28c

core/sync/.../data/LegacyRemotePuller.kt
359be663828c95a17138b08503747e66cceadbd1aa379bef2e58e8a36b08df98

core/sync/.../data/SyncManager.kt
e1f985aa77c61a25fc50307f55a6b990d1c75a98c11a72a18e264de8ca37c437

core/sync/.../data/DefaultSyncCoordinator.kt
c153691154ce5662524ffe0981d81f3b5d27d5aa20e146a8d7996a22ee849f5b

core/sync/.../realtime/RealtimeManager.kt
41342bfca594a06858f43ade619a6344553c5c089ae04ebfef9d8fab8601a2b6

core/sync/.../domain/RealtimeConnectionObserver.kt
1646b72cc63b0073a83c7953086eca1460de4277f0f1ab4575bafca2f9ca86b1

core/sync/.../data/LocalDataCleaner.kt
7e1ffa09fb6bf6257bef8101fe87ce87823c5180dc88a01e0c476ff3e4fcf919

core/sync/.../data/BillingTargetedRefresher.kt
a333650246b62ee618da8494c31e439263cd96c6dccd79c322378b376e2fa820

feature/commission/.../BillingRealtimeParticipant.kt
1c0369c8a019d3ca1682fd6f481c7438c3484d64a58f8d86a4f932bd5ff62b51

feature/balance/.../BalanceRealtimeParticipant.kt
ffa2095b3b82ed83483ce79588f3a790489be9ee2ea4a24e7e74551463d7a7a1

feature/notifications/.../NotificationsRealtimeParticipant.kt
34b0f75c6efb45bd94f4435f5de96c7786b173d3b02c5b5cc78664ab9d6aae28

feature/chat/.../ChatRealtimeParticipant.kt
53b5ed5e2b041a1050a7f35eb4ad174d561319c95525ac54cf2f9f8563f314fb
```

---

# 6. حقيقة v69 — لا يوجد Durable Inbox

`AutoDriveDatabase` الحالي:

```text
AUTODRIVE_DATABASE_VERSION = 15
```

ويملك:

```text
SyncCursorEntity
SyncCursorDao
PendingOperationEntity
```

ولا يملك:

```text
SyncInboxEntity
SyncInboxDao
sync_inbox table
```

إذًا:

```text
DURABLE_INBOX_PRESENT = false
```

70 تحتاج:

```text
Room 15 -> 16
```

---

# 7. حقيقة v69 — deletion path ذرّي جزئيًا لكنه بلا Inbox

`DeletionSynchronizer` الحالي يفعل:

```text
fetch page outside transaction
validate page
Room.withTransaction {
    revalidate scope
    apply deletions
    advance sync cursor
}
```

وهذا foundation جيد يجب الحفاظ عليه.

لكن `DeletionEnvelope` الحالي يحتوي:

```text
eventId
entityType
entityId
scope
```

ولا يحتوي:

```text
serverRevision
transactionGroupId
```

كما أن production `DeletionFeed` ما زال fail-closed بسبب blocker الموروث.

الحكم:

```text
DELETION_ATOMIC_CURSOR_APPLY = PRESENT
DELETION_DURABLE_EVENT_DEDUPE = ABSENT
DELETION_SERVER_REVISION = ABSENT/UNPROVEN
SERVER_TOMBSTONE_RUNTIME = BLOCKED
```

---

# 8. حقيقة v69 — Legacy positive pulls ليست event stream

`LegacyRemotePuller` يسحب snapshots/rows من جداول متعددة ثم يكتب Room مباشرة.

أمثلة مثبتة:

```text
invoices       -> upsertAll
payments       -> upsertAll
commissions    -> upsertAll
balance        -> replace/upsert
transactions   -> recent limit 50
withdrawals    -> recent limit 20
notifications  -> recent limit 50
chat messages  -> ASC limit 100
```

هذه الاستجابات الحالية لا توفر في الكود:

```text
eventId
global serverRevision
transactionGroupId
resume cursor per positive stream
```

إذًا ممنوع في 70:

```text
hash row payload -> fake eventId
updated_at -> fake serverRevision
System.currentTimeMillis() -> fake serverRevision
v69 command receipt revision -> inbound data revision
```

الحكم:

```text
LEGACY_POSITIVE_PULL = SNAPSHOT_COMPATIBILITY_PATH
NOT_A_CANONICAL_EVENT_FEED
```

---

# 9. حقيقة v69 — Realtime يكتب إلى Room مباشرة

المشاركون الإنتاجيون المسجلون حاليًا:

```text
billing
balance
chat
notifications
```

ويملكون direct Room mutation paths.

### Billing

```text
commissionPaymentDao.upsert
commissionPaymentDao.deleteById
BillingTargetedRefresher -> invoice/payment Room writes
```

### Balance

```text
marketerBalanceDao.upsert/delete
balanceTransactionDao.upsert/delete
withdrawalRequestDao.upsert/delete
```

### Notifications

```text
notificationDao.upsert/delete
```

### Chat

```text
conversationDao.upsert/update
chatMessageDao.insert/insertOrIgnore/delete
LocalNotificationPublisher.publishChatMessage from Realtime payload
```

إذًا:

```text
REALTIME_HINT_ONLY = false
REALTIME_IS_SECOND_ROOM_WRITER = true
```

70 يجب أن يجعل العدد النهائي:

```text
Realtime-reachable Room mutation paths = 0
```

---

# 10. حقيقة v69 — DELETE oldRecord authority

المشاركون الحاليون يفكون `action.oldRecord` في DELETE في:

```text
BillingRealtimeParticipant
BalanceRealtimeParticipant
NotificationsRealtimeParticipant
ChatRealtimeParticipant
```

ثم يستخدمون محتواه للحذف/التحديث.

بعد 70:

```text
oldRecord may be absent/incomplete and correctness still holds
```

لأن DELETE يصبح فقط:

```text
signal -> sync request
```

وليس:

```text
payload -> local delete
```

---

# 11. حقيقة v69 — Realtime health غير صادق تجميعيًا

الحالة الحالية:

```text
DISCONNECTED
CONNECTING
CONNECTED
```

ولا يوجد:

```text
DEGRADED
```

`RealtimeManager` ينتظر:

```text
subscribed.receive()
```

أي أول participant ناجح، ثم يعلن:

```text
CONNECTED
```

حتى لو بقية المشاركين في retry/failure.

إذًا:

```text
FIRST_CHANNEL_CONNECTED == GLOBAL_CONNECTED
```

وهو مخالف لنطاق 70.

---

# 12. الهدف الدقيق لـ70

عند النهاية يجب أن يثبت الكود:

```text
1. Room = 16.
2. canonical scoped sync_inbox exists.
3. migration 15→16 append-only and non-destructive.
4. Inbox identity is scoped by userId/clientId/orgId/stream/eventId.
5. inbound event replay is deduped durably.
6. same eventId with different canonical identity fails closed.
7. entity/tombstone + Inbox + cursor are atomic for cursor-bearing event streams.
8. cursor never advances after partial/failed apply.
9. stale session scope cannot commit inbound data.
10. network I/O remains outside Room transactions.
11. legacy snapshot pulls never fabricate event identity/revision.
12. v69 command receipt revision never contaminates sync cursors/Inbox data revision.
13. billing invoice+payment local apply is crash-atomic where fetched as one compatibility batch.
14. every Realtime participant is hint-only.
15. no Realtime-reachable helper mutates Room.
16. DELETE oldRecord is not correctness authority.
17. Realtime payload cannot directly trigger user-visible business effects.
18. duplicate/lost Realtime events do not affect eventual correctness.
19. aggregate Realtime health supports DEGRADED.
20. CONNECTED means the defined required participant set is healthy, not merely first subscriber.
21. logout clears Inbox only for departing scope inside existing cleanup transaction.
22. v67 generation/push-before-pull foundations remain unchanged.
23. v68 transactional Outbox/logout isolation remains unchanged.
24. v69 idempotent command/receipt/typed retry semantics remain unchanged.
25. no UI drift.
26. no historical SQL mutation.
27. no new waiver.
```

---

# 13. ما ليست عليه70

ممنوع سحب النطاقات التالية:

```text
Chat 10k pagination/recovery                    -> 71
create_new_conversation timeout duplication     -> 71
durable media transfer queue                    -> 71
Unified Server Change Feed                      -> 72
global monotonic data revision                  -> 72
CURSOR_EXPIRED full bootstrap/rebootstrap       -> 72
anti-entropy manifest/digest                    -> 72
final sync observability                        -> 73
full fault-injection campaign                   -> 73
dead-letter recovery UX                         -> later/final closure
```

كما أن 70 لا تعيد بناء:

```text
v69 command receipt ledger
v69 mutation fingerprinting
v69 typed retry taxonomy
v69 ambiguous outcome reconciliation
v68 Outbox scope/lease
v67 generation coordinator
```

أي توسع غير ضروري:

```text
BLOCKED_SCOPE_DRIFT
```

---

# 14. Room 15→16 policy

70 تحتاج migration محلية واحدة فقط:

```text
MIGRATION_15_16
```

المتوقع:

```text
current Room = 15
target Room  = 16
new Room migrations = 1
```

ممنوع:

```text
fallbackToDestructiveMigration
DROP business tables
recreate existing tables destructively
rewrite MIGRATION_13_14
rewrite MIGRATION_14_15
```

---

# 15. Canonical Sync Inbox schema

الاسم المفضل:

```text
sync_inbox
```

الأسماء الدقيقة قابلة للتعديل، لكن semantics إلزامية.

الحد الأدنى:

```text
user_id
client_id
org_id
stream
event_id
server_revision nullable
revision_kind
entity_type
entity_id
operation
transaction_group_id nullable
received_at
applied_at nullable
contract_version
```

---

# 16. Inbox primary identity

الـprimary/unique authority يجب أن تمنع event collision بين scopes:

```text
(user_id, client_id, org_id, stream, event_id)
```

ممنوع:

```text
PRIMARY KEY(event_id) only
```

إلا إذا ثبت server-global event UUID + ownership invariant رسميًا، وهو غير مثبت حاليًا.

---

# 17. eventId contract

`eventId` يجب أن يكون:

```text
server-provided
nonblank
stable across replay
stable across pagination retry
immutable for one server event
```

ممنوع توليده من:

```text
row id only
updated_at
local clock
random UUID on Android
hash(payload) as authority
Realtime callback id
command mutationId
```

إذا المصدر لا يوفر eventId:

```text
SOURCE_CLASSIFICATION = SNAPSHOT_COMPAT
DO_NOT_INSERT_FAKE_INBOX_EVENT
```

---

# 18. serverRevision policy

`server_revision` في Inbox:

```text
nullable until an authoritative data revision exists
```

ويجب وجود discriminator دلالي مثل:

```text
revision_kind = DATA_CHANGE | NONE
```

أو equivalent.

ممنوع قطعًا:

```text
COMMAND_RECEIPT revision -> DATA_CHANGE revision
```

وممنوع:

```text
command receipt sequence -> sync_cursors.cursor_token
```

Session72 وحدها تملك قرار unified/global data revision.

---

# 19. operation policy

الحد الأدنى:

```text
UPSERT
DELETE
```

يمكن إضافة:

```text
STATE
```

فقط إذا كان له استعمال inbound مثبت.

ممنوع استنتاج DELETE من:

```text
absence from snapshot
```

---

# 20. transactionGroupId policy

الحقل:

```text
transaction_group_id nullable
```

للاستعداد دون اختراع semantics.

إذا stream موثوق يوفر group:

```text
all known members of one group must apply atomically
cursor must not advance past a partial group
```

إذا current server لا يوفر group:

```text
transactionGroupId = null
```

ولا يختلق Android group id من ترتيب rows.

---

# 21. receivedAt / appliedAt

هذه timestamps تشخيصية فقط.

ممنوع استخدامها في:

```text
ordering authority
dedupe authority
cursor authority
conflict authority
```

الـcorrectness يعتمد على:

```text
scope + stream + eventId + authoritative cursor/revision when available
```

---

# 22. Inbox payload policy

70 لا تحتاج تخزين raw server payload افتراضيًا.

يفضل:

```text
metadata-only Inbox ledger
```

ممنوع تخزين raw:

```text
chat body
push token
bank data
profile payload
JWT/session values
```

إلا إذا ثبت احتياج correctness وجرى تعريف تشفير/retention منفصل، وهذا خارج 70.

---

# 23. Inbox retention policy

ممنوع pruning عشوائي في 70.

السبب:

```text
retention horizon + cursor replay horizon غير مثبتين كعقد موحد بعد
```

إذًا:

```text
AUTO_PRUNE = false by default
```

يجوز فقط cleanup scoped عند logout وفق policy الحالية.

Session72/73 يمكنها إضافة retention بعد إثبات revision/cursor horizon.

---

# 24. SyncInboxDao contract

كل API production يجب أن يكون exact-scope.

الحد الأدنى:

```text
get(scope, stream, eventId)
insert/markApplied(...)
countForScope(...)
deleteForScope(...)
```

ويفضل query للتشخيص:

```text
countUnappliedForScope
```

لكن لا يلزم UI.

ممنوع:

```text
getByEventId(eventId) without scope
deleteAll() from normal sync
global cursor/inbox cleanup
```

---

# 25. Event replay rule

إذا event موجود ومطبق بنفس canonical metadata:

```text
REPLAY = NO-OP EFFECT
```

ثم يجوز للـcursor الوصول لنفس/التالي وفق batch contract.

إذا event موجود لكنه:

```text
entityType differs
entityId differs
operation differs
serverRevision conflicts
transactionGroupId conflicts
```

فالحكم:

```text
INBOX_EVENT_IDENTITY_CONFLICT
```

ولا effect ولا cursor advance.

---

# 26. Atomic inbound apply boundary

لأي cursor-bearing event batch:

```text
NETWORK:
    fetch batch
    validate shape/scope/progression

ROOM TRANSACTION:
    revalidate current SyncScope
    for each event/group:
        load Inbox identity
        if already-applied and identical:
            skip effect
        else:
            apply authoritative entity/tombstone
            record Inbox applied metadata
    advance exact stream cursor

COMMIT
```

أي exception:

```text
ROLLBACK entity changes
ROLLBACK Inbox rows
ROLLBACK cursor
```

---

# 27. Cursor advancement invariant

قاعدة مطلقة:

```text
cursorAfter >/!= cursorBefore only after full local transaction success
```

بحسب opaque cursor contract الفعلي.

ممنوع:

```text
advance cursor before apply
advance cursor outside transaction
advance cursor after unknown entity
advance cursor after scope mismatch
advance cursor after incomplete transaction group
```

---

# 28. DeletionSynchronizer conversion

يجب الحفاظ على foundation الحالية:

```text
fetch outside transaction
validate
scope recheck
atomic deletion + cursor
non-advancing cursor guard
bounded pages
```

وإضافة Inbox dedupe داخل **نفس transaction**.

المسار المستهدف:

```text
DeletionEnvelope(eventId,...)
        ↓
Inbox identity lookup
        ↓
if unseen:
    apply registered deletion
    record Inbox
else:
    verify identical replay
        ↓
advance cursor with batch atomically
```

---

# 29. Tombstone blocker truth

70 لا يجوز أن تدعي live tombstone success طالما blocker v67 لم يُغلق.

الحالة الموروثة:

```text
DeletionFeed production contract = formally blocked
```

لذلك التنفيذ تحت override يمكنه إثبات:

```text
local Inbox architecture
model replay semantics
atomic Room transaction structure
fail-closed adapter preservation
```

لكن لا يجوز أن يدعي:

```text
live server tombstone page consumed
live eventId/revision semantics proven
live RLS proven
```

بدون runtime evidence حقيقي.

---

# 30. Legacy positive pulls — classification

الـLegacyRemotePuller يبقى compatibility path إلى أن تأتي Session72 بالـChange Feed.

يجب تصنيف كل phase:

```text
EVENT_STREAM
SNAPSHOT_COMPAT
COMMAND_RECEIPT
REALTIME_HINT
```

الحالة الحالية المتوقعة:

```text
deletions, when server contract valid -> EVENT_STREAM
positive legacy PostgREST rows        -> SNAPSHOT_COMPAT
v69 command receipts                  -> COMMAND_RECEIPT
Realtime callbacks                    -> REALTIME_HINT
```

ولا يجوز خلط هذه الأنواع.

---

# 31. Snapshot compatibility apply

عدم وجود eventId لا يعني السماح بالكتابة المتناثرة.

70 يجب أن تجعل كل phase snapshot:

```text
fetch network rows
normalize/validate
Room.withTransaction {
    revalidate scope
    protect pending-local intent
    apply exact phase
}
```

ممنوع network I/O داخل transaction.

---

# 32. Invoice + Payment local atomicity

الخطة تطلب ألا تظهر Invoice + Payment في حالة نصف مطبقة.

بسبب عدم وجود authoritative server transaction group حاليًا، 70 لا يجوز ادعاء server-snapshot atomicity.

المطلوب القابل للإثبات الآن:

```text
fetch invoice set
fetch payment set
NO Room writes yet
Room.withTransaction {
    revalidate scope
    apply invoice rows
    apply payment rows
}
```

إذا fetch لأي مجموعة يفشل:

```text
do not partially apply the pair
```

هذا يثبت:

```text
LOCAL_CRASH_ATOMICITY = true
```

ولا يثبت بعد:

```text
SERVER_TRANSACTION_GROUP_SNAPSHOT_CONSISTENCY = true
```

الأخيرة مؤجلة إلى Session72 unified feed.

---

# 33. Other snapshot phases

كل phase يجب أن يكون local-transactional حيث توجد عدة writes ذات علاقة.

خصوصًا:

```text
balance replacement
notifications merge with pending local read
chat conversation/message compatibility apply
withdrawal pending-state merge
```

مع الحفاظ على pending-local guards الحالية.

---

# 34. No delete by absence

Session70 لا تغير المبدأ:

```text
absence from positive snapshot != deletion
```

الحذف الموثوق يأتي فقط من:

```text
authoritative deletion/event stream
```

حتى بعد تحويل Realtime إلى hint-only.

---

# 35. Realtime contract بعد70

كل participant:

```text
subscribe
validate/coarsely filter signal if useful
coalesce/debounce safely
requestSync(REALTIME_HINT)
```

ثم ينتهي دوره.

ممنوع:

```text
AutoDriveDatabase injection for state mutation
DAO write
repository write causing Room mutation
targeted refresher that writes Room
payload-to-domain upsert
payload-to-domain delete
payload-to-local notification business side effect
```

---

# 36. Realtime hint dispatcher

يجوز:

```text
SyncCoordinator.requestSync(SyncReason.REALTIME_HINT)
```

مباشرة أو عبر abstraction صغيرة مثل:

```text
RealtimeHintDispatcher
```

إذا أُضيف abstraction، يجب أن يكون:

```text
stateless for business data
generation-safe via existing SyncCoordinator
scope-resolving through current session at authoritative pull
```

ولا ينشئ queue correctness مستقلة.

---

# 37. Realtime targeted sync

يجوز الاحتفاظ بمعلومة target كـoptimization فقط إذا:

```text
it never bypasses canonical pull/apply path
it cannot advance a cursor past unseen changes
it never mutates Room from Realtime callback
```

إذا لا يوجد targeted cursor-safe pull حاليًا:

```text
fallback to requestSync(REALTIME_HINT)
```

هو الخيار الصحيح.

---

# 38. BillingTargetedRefresher

الحالة الحالية:

```text
Realtime -> BillingTargetedRefresher -> PostgREST -> Room write
```

وهذا غير مسموح بعد70.

الحلول المقبولة:

```text
A) remove its Realtime use and route signal to SyncCoordinator
B) refactor it into a pure fetch/hint boundary whose apply goes through canonical inbound apply transaction
```

غير المقبول:

```text
Realtime handler has no DB,
but helper called by it writes DB
```

Static verifier يجب أن يفحص **transitive helper authority**، لا imports المباشرة فقط.

---

# 39. Realtime payload rule

بعد70 payload يستخدم فقط حيث يلزم:

```text
safe coarse ownership/filter validation
coalescing key
diagnostic non-sensitive context
```

ولا يستخدم كـ:

```text
authoritative entity
authoritative delete row
financial amount authority
notification read/write authority
chat message source-of-truth
```

---

# 40. DELETE without oldRecord

Model gate إلزامي:

```text
DELETE signal with empty/unusable oldRecord
    -> schedules authoritative sync
    -> no local delete from payload
    -> no crash
```

إذا correctness يتطلب decode `oldRecord`:

```text
FAIL_REALTIME_HINT_ONLY
```

---

# 41. Realtime chat notification side effect

الحالي يستطيع:

```text
Realtime chat payload
-> LocalNotificationPublisher.publishChatMessage(...)
```

بعد70:

```text
Realtime payload is not sufficient authority for user-visible business notification.
```

المسموح:

```text
FCM path وفق عقده المستقل
or authoritative post-pull state transition with explicit dedupe policy
```

70 لا تبني notification subsystem جديدًا؛ فقط تزيل سلطة Realtime payload.

---

# 42. RealtimeEventPolicy

يجوز إبقاؤه لأغراض:

```text
filtering load
scope sanity
drop obviously unrelated hints
```

لكن ممنوع اعتباره correctness authority بدل authoritative pull.

إذا policy لم تستطع قراءة DELETE payload:

```text
request scoped authoritative sync
```

أفضل من حذف محلي من oldRecord.

---

# 43. Generation-safe hints

70 يجب أن تستخدم foundation v67 الحالية:

```text
requestedGeneration
completedGeneration
activeSync shared flight
completion-edge mutex
```

أي hint أثناء:

```text
Push
Pull
Inbox apply
Deletion apply
Reconcile
```

يجب أن ينتج generation لاحقة دون فقد.

ممنوع إنشاء single-flight منفصل داخل Realtime يبتلع hint.

---

# 44. Hint coalescing

يسمح debounce/coalescing لتخفيف الضوضاء إذا:

```text
at least one sync generation remains requested
no final hint can be lost at completion edge
```

ممنوع:

```text
drop event because sync is already running
```

---

# 45. Realtime health model

يجب إضافة:

```text
DEGRADED
```

إلى aggregate state أو equivalent صريح.

الحد الأدنى:

```text
DISCONNECTED
CONNECTING
DEGRADED
CONNECTED
```

---

# 46. Participant health truth

يجب تتبع حالة كل participant على الأقل دلاليًا:

```text
CONNECTED
DEGRADED/RETRYING
DISCONNECTED
```

يمكن internal map ولا يلزم UI جديد.

---

# 47. Aggregate health semantics

القواعد:

```text
CONNECTED:
    all required participants for current session are subscribed/healthy

DEGRADED:
    at least one required participant unavailable/retrying
    while one or more remain connected

DISCONNECTED:
    no usable participants / realtime stopped / no valid session

CONNECTING:
    initial connection attempt before stable aggregate outcome
```

إذا توجد optional stream/table:

```text
classification MUST be explicit
```

ولا يجوز أن يجعل optional غير الموجود global `CONNECTED` كذبًا أو global permanent failureًا.

---

# 48. First subscriber bug

المسار الحالي:

```text
subscribed.receive()
setConnectionState(CONNECTED)
```

يجب ألا يبقى authority للحالة النهائية.

Model fixture:

```text
4 participants configured
1 subscribed
3 retrying
=> DEGRADED
NOT CONNECTED
```

---

# 49. Realtime failure isolation

يجب الحفاظ على:

```text
one participant failure does not cancel all healthy participants
```

لكن health يصبح صادقًا.

أي participant يستعيد الاتصال:

```text
aggregate state recomputed
```

---

# 50. Realtime correctness independence

اختبارات النموذج يجب تثبت:

```text
Realtime fully disabled
+ normal recovery/app/network sync available
=> eventual authoritative convergence path remains
```

لا يلزم runtime convergence الكامل في70 إذا server feed blocked، لكن المعمارية لا تعتمد على Realtime.

---

# 51. LocalDataCleaner / logout

Room16 يضيف Inbox؛ لذلك cleanup الحالي يجب أن يصبح داخل نفس transaction:

```text
delete account business rows
delete pending_operations for scope
delete sync_cursors for scope
delete sync_inbox for scope
```

ممنوع:

```text
delete all inbox rows for every account
```

إلا full reset صريح منفصل.

---

# 52. Cross-account callback safety

Realtime hint قد يصل أثناء logout.

بعد70:

```text
old callback cannot write Room directly
```

والـsync path الحالي يجب أن يعيد التحقق من session/scope قبل apply.

Model:

```text
A realtime callback from account A
logout A
login B
old callback fires
=> no A entity/inbox/cursor write into B
```

---

# 53. v69 command protocol preservation

هذه migration تاريخية immutable:

```text
supabase/migrations/20260821203000_autodrive_idempotent_commands_v1.sql
SHA-256:
6663381c4bf177c7cc22c75fb4c1eee1683290894307ec9ade85e4fe7620c01e
```

70 لا تعدلها.

كما لا تغير semantics:

```text
mutationId
request fingerprint
receipt replay
typed resultStatus
typed retry taxonomy
ambiguous outcome reconciliation
```

---

# 54. Server migration policy في70

المتوقع افتراضيًا:

```text
new server migration count = 0
```

لأن:

```text
Unified Change Feed / global data revision -> Session72
```

إذا احتاج executor server SQL فقط ليجعل Realtime hint-only:

```text
STOP
prove why client-side transition cannot be completed without it
```

لا يجوز سحب Session72.

---

# 55. No command-receipt / Inbox conflation

الـcommand receipt هو:

```text
proof of outbound command outcome
```

الـInbox هو:

```text
dedupe ledger for inbound authoritative change events
```

ممنوع:

```text
insert outbound command receipt into sync_inbox as if it were server change
```

إلا إذا Session72 change feed أعاد نفس mutation كـdata event له eventId/revision مستقلان.

---

# 56. Allowed production scope

يجوز تعديل:

```text
core/database/src/main/kotlin/com/autodrive/app/core/database/**
core/sync/src/main/kotlin/com/autodrive/app/core/sync/data/**
core/sync/src/main/kotlin/com/autodrive/app/core/sync/realtime/**
core/sync/src/main/kotlin/com/autodrive/app/core/sync/domain/**
```

وللـRealtime participants:

```text
feature/commission/**/data/realtime/**
feature/balance/**/data/realtime/**
feature/notifications/**/data/realtime/**
feature/chat/**/data/realtime/**
```

ومسموح minimal DAO/query changes اللازمة للـatomic snapshot apply.

DI:

```text
app/.../di/RealtimeModule.kt
sync/database DI only as required
```

---

# 57. Expected database files

المتوقع على الأقل:

```text
SyncInboxEntity.kt                    CREATE
SyncInboxDao.kt                       CREATE
AutoDriveDatabase.kt                  MODIFY
MIGRATION_15_16                       ADD in canonical migration location
LocalDataCleaner.kt                   MODIFY
```

إذا schema export موجود:

```text
Room 16 schema evidence
```

يولد فقط عبر tooling حقيقي؛ لا يُكتب يدويًا كإثبات runtime.

---

# 58. Expected sync files

مرشح للتعديل:

```text
RemoteSyncSemantics.kt
DeletionSynchronizer.kt
LegacyRemotePuller.kt
SyncManager.kt only if orchestration boundary needs adaptation
DefaultSyncCoordinator.kt preferably no semantic rewrite
RealtimeManager.kt
RealtimeConnectionObserver.kt
RealtimeParticipant.kt only if health callback contract changes
BillingTargetedRefresher.kt
```

---

# 59. Expected feature realtime files

```text
BillingRealtimeParticipant.kt
BalanceRealtimeParticipant.kt
NotificationsRealtimeParticipant.kt
ChatRealtimeParticipant.kt
```

بعد70 لا ينبغي أن تحتاج أي منها:

```text
AutoDriveDatabase
domain mapper toEntity for Realtime apply
DAO
LocalNotificationPublisher from Realtime payload
```

إلا read-only non-authoritative استعمال مثبت ومبرر، والأفضل إزالته.

---

# 60. Forbidden production drift

ممنوع تعديل:

```text
Compose/UI screens
design system
commission formulas
withdrawal eligibility
balance business math
profile business rules
auth semantics beyond compile-safe sync integration
media upload architecture
chat pagination strategy
chat create-conversation protocol
server receipt schema from69
global change-feed schema
bootstrap UX
dead-letter UX
```

أي تغير:

```text
BLOCKED_UNRELATED_MUTATION
```

---

# 61. No UI work

70 ليست جلسة UI.

المتوقع:

```text
productionUiFilesChanged = 0
```

حتى Realtime `DEGRADED` لا يلزم عرض UI جديد في هذه الجلسة.

إذا component موجود ويستهلك enum exhaustively، يسمح compile adaptation فقط دون redesign.

---

# 62. No historical migration mutation

يجب:

```text
historicalMigrationMutationCount = 0
```

ويشمل:

```text
Room migrations 13→14 / 14→15
all existing Supabase migrations
```

70 تضيف فقط:

```text
Room 15→16
```

---

# 63. Migration 15→16 minimum structural checks

يجب إثبات:

```text
CREATE TABLE IF NOT EXISTS sync_inbox ...
NOT NULL scope columns
scoped primary key
server_revision nullable
transaction_group_id nullable
received_at present
applied_at nullable/present
no destructive business-table operation
```

ويجب migration model يفحص:

```text
15 schema -> 16 expected columns/PK/indexes
```

---

# 64. Migration default policy

للجدول الجديد لا توجد legacy rows.

إذًا لا حاجة backfill.

ممنوع:

```text
populate fake event ids
derive fake revisions
copy pending_operations into Inbox
```

---

# 65. Index policy

الحد الأدنى المفيد:

```text
PK(scope + stream + eventId)
```

ويجوز index على:

```text
(scope + stream + appliedAt)
(scope + stream + serverRevision) when not null/useful
```

لا تضف indexes بلا query مثبتة.

---

# 66. Transaction clock policy

`receivedAt/appliedAt` يمكنهما استخدام local clock كdiagnostics.

ممنوع استخدام الساعة في:

```text
ordering
cursor
dedupe
completion proof
```

---

# 67. Error taxonomy for inbound apply

لا تعيد استخدام Outbox errors قسرًا إذا semantics مختلفة.

الحد الأدنى الدلالي:

```text
INBOX_EVENT_IDENTITY_CONFLICT
SCOPE_MISMATCH
UNKNOWN_ENTITY_TYPE
NON_ADVANCING_CURSOR
PARTIAL_TRANSACTION_GROUP
MALFORMED_EVENT
STALE_SESSION
```

يمكن أن تكون exceptions/types داخلية.

ممنوع swallow ثم cursor advance.

---

# 68. Unknown entity rule

إذا event-bearing stream يرسل entityType غير مسجل:

```text
FAIL CLOSED
NO Inbox applied marker
NO cursor advance
```

ممنوع:

```text
ignore unknown event and advance cursor
```

---

# 69. Scope mismatch rule

أي event يحمل scope غير المطابق للـcaptured SyncScope:

```text
reject batch/event
rollback
do not advance cursor
```

Realtime payload scope mismatch لا يطبق شيئًا؛ يمكن drop hint أو schedule safe scoped pull حسب source.

---

# 70. Batch replay rule

إعادة نفس page:

```text
same eventIds
same canonical metadata
```

النتيجة:

```text
no duplicate entity effect
no duplicate delete side effect
Inbox remains one logical event per scoped identity
cursor converges deterministically
```

---

# 71. Crash-before-commit rule

نموذج:

```text
entity change started
Inbox insert started
cursor update pending
process/exception before commit
```

المطلوب:

```text
all rolled back
page can be replayed
```

---

# 72. Crash-after-commit rule

نموذج:

```text
transaction committed
process dies before caller observes success
```

عند restart/re-fetch:

```text
Inbox says applied
identity matches
effect is not duplicated
cursor can safely recover/confirm
```

---

# 73. Pending-local protection

70 لا تلغي guards الموروثة.

يجب أن تبقى:

```text
pending Profile intent protected
notification local read pending protected
withdrawal/outbox reconciliation protected
pending balance transaction behavior preserved
```

أي snapshot compatibility apply يجب أن يستدعي guards داخل transaction الصحيح.

---

# 74. Outbox ordering preservation

pipeline الحالي:

```text
AUTH
RECOVER_LEASES
PUSH_OUTBOX
LEGACY POSITIVE PULL
DELETION DELTA
RECONCILE
REALTIME
```

70 يمكن إدخال Inbox apply داخل inbound stages.

ممنوع إعادة الترتيب إلى:

```text
Pull before Push
```

بدون عقد جديد.

---

# 75. Realtime restart preservation

بعد sync، Realtime يمكن restart كحالياً.

لكن:

```text
restart success != data correctness
```

وحالته الصحية لا تغير نجاح authoritative pull.

---

# 76. Static source inventory قبل mutation

يجب إنتاج artifact:

```text
AUTODRIVE_INBOUND_APPLY_INVENTORY_v70.json
```

يحصر كل path يكتب Room من remote/server data:

```text
source file
trigger/source type
target DAO/table
has eventId?
has serverRevision?
has cursor?
scope dimensions
atomic apply?
classification
v70 disposition
```

---

# 77. Realtime write inventory

يجب إنتاج:

```text
AUTODRIVE_REALTIME_WRITE_INVENTORY_v70.json
```

ويحصر على الأقل:

```text
BillingRealtimeParticipant
BillingTargetedRefresher
BalanceRealtimeParticipant
NotificationsRealtimeParticipant
ChatRealtimeParticipant
```

مع:

```text
direct Room writes before
transitive Room writes before
payload delete authority before
user-visible payload side effects before
state after70
```

---

# 78. Required verifier

أنشئ verifier deterministic مثل:

```text
scripts/verify-v70-static.py
```

ويجب تشغيله مرتين على working tree، ثم على final extracted ZIP.

---

# 79. Static verifier — Room checks

يفحص:

```text
AUTODRIVE_DATABASE_VERSION == 16
SyncInboxEntity present
SyncInboxDao present
MIGRATION_15_16 present
MIGRATION_13_14 unchanged
MIGRATION_14_15 unchanged
no destructive fallback
Inbox PK fully scoped
LocalDataCleaner exact-scope Inbox cleanup
```

---

# 80. Static verifier — Inbox checks

يفحص:

```text
eventId required
serverRevision not synthesized
command receipt revision not imported as data cursor
no raw payload column by default
all Inbox DAO production queries scope-aware
atomic apply references Room transaction
cursor update co-located with event apply for event-bearing stream
```

---

# 81. Static verifier — Realtime checks

يفحص participant source + reachable helpers لمنع:

```text
AutoDriveDatabase write authority
*Dao().upsert
*Dao().insert
*Dao().delete
*Dao().update
BillingTargetedRefresher Room writes reachable from Realtime
oldRecord-based local delete
Realtime payload -> LocalNotificationPublisher
```

الهدف:

```text
realtimeDirectRoomWriteCount = 0
realtimeTransitiveRoomWriteCount = 0
```

---

# 82. Static verifier — health checks

يفحص:

```text
DEGRADED exists
first subscriber cannot be sole global CONNECTED gate
participant states tracked/recomputed
all-required healthy condition for CONNECTED
```

---

# 83. Static verifier — regression checks

يجب ألا تختفي دلائل:

```text
requestedGeneration
completedGeneration
push-before-pull
Room.withTransaction for Outbox mutation
scope fields in PendingOperation
v69 typed retry taxonomy
v69 canonical receipt handling
```

---

# 84. Model verifier

أنشئ/وسع verifier model مستقل مثل:

```text
scripts/verify-v70-model.py
```

لا يكتفي بالبحث النصي؛ يجب تمثيل transitions.

---

# 85. Minimum model fixtures

الحد الأدنى 28 fixture:

```text
01 Inbox scope A/B isolation
02 same event replay is no-op
03 same eventId + changed entity fails
04 same eventId + changed operation fails
05 crash before apply transaction commit rolls all back
06 crash after commit replays safely
07 cursor unchanged when entity apply fails
08 cursor unchanged when Inbox insert fails
09 cursor unchanged on scope mismatch
10 unknown entity blocks cursor
11 stale session blocks commit
12 tombstone page replay is safe
13 non-advancing cursor fails
14 logout clears only departing Inbox scope
15 no synthetic eventId for snapshot pull
16 no synthetic serverRevision for snapshot pull
17 command receipt revision never becomes sync cursor
18 invoice fetch failure prevents partial invoice/payment Room apply
19 payment fetch failure prevents partial invoice/payment Room apply
20 invoice+payment local transaction rollback together
21 Realtime INSERT becomes hint only
22 Realtime UPDATE becomes hint only
23 Realtime DELETE without oldRecord becomes hint only
24 duplicate Realtime events coalesce without lost final generation
25 hint during active pull creates trailing generation
26 one of four participants connected => DEGRADED
27 all required participants connected => CONNECTED
28 all unavailable/stopped => DISCONNECTED
```

يفضل أيضًا:

```text
29 participant drops after CONNECTED => DEGRADED
30 participant recovers => CONNECTED
31 old account callback after logout cannot write new account
32 Realtime payload cannot create local chat notification directly
33 v69 command duplicate replay semantics unchanged
34 v68 Outbox scope isolation unchanged
35 v67 push-before-pull unchanged
36 deletion event identity conflict blocks cursor
```

---

# 86. Kotlin unit tests

حيث البيئة تسمح، أضف/عدل اختبارات:

```text
RealtimeArchitectureTest
RealtimeEventPolicyTest
DefaultSyncCoordinatorTest
DeletionSynchronizer tests
Room migration test 15→16
LocalDataCleaner scope test
LegacyRemotePuller atomic-apply tests
```

---

# 87. RealtimeArchitectureTest الجديد

الاختبار القديم الذي يتوقع DELETE handler محلي لا يصلح كمعيار بعد70.

يجب أن يثبت:

```text
participants do not mutate Room
INSERT/UPDATE/DELETE all schedule hints
oldRecord is not delete authority
helpers reachable from Realtime do not write Room
aggregate health is not first-subscriber based
```

---

# 88. Database migration test

إذا instrumentation متاح:

```text
create Room15 schema
insert representative existing rows
migrate 15→16
verify existing data unchanged
verify sync_inbox schema
verify scoped PK
```

إذا tooling غير متاح:

```text
NOT RUN
```

ولا تُنشئ schema evidence مزيفة.

---

# 89. Build policy

يجب محاولة compile/build المناسب مرة واحدة على الأقل إذا البيئة تسمح.

بسبب v69 baseline:

```text
Gradle distribution bootstrap may still fail on services.gradle.org
```

إذا فشل بيئيًا:

```text
COMPILED = false
reason = exact environmental blocker
```

لا تحول ذلك إلى source failure إذا static/model كلها ناجحة.

---

# 90. Server runtime policy

70 افتراضيًا لا تضيف SQL server.

لكن inherited server verification يبقى:

```text
v69 server SQL tests = NOT RUN
v67 tombstone runtime = BLOCKED
```

لا يجوز إعلان live inbound correctness دون server evidence.

---

# 91. Required truth table

تقرير 70 يجب أن يفصل:

```text
IMPLEMENTED
STATIC_VERIFIED
MODEL_VERIFIED
COMPILED
UNIT_TESTED
ANDROID_MIGRATION_TESTED
SERVER_TOMBSTONE_RUNTIME_VERIFIED
REALTIME_RUNTIME_TESTED
PREDECESSOR_GATE_SATISFIED
```

لا تستخدم `PASS` لتغطية `NOT_RUN`.

---

# 92. Acceptance counters — must be zero

```text
inputDriftCount
newV70WaiverCount
historicalMigrationMutationCount
unexpectedProductionMutationCount
productionUiFilesChanged
inboxUnscopedAccessCount
inboxSyntheticEventIdCount
inboxSyntheticServerRevisionCount
commandReceiptRevisionCursorContaminationCount
cursorAdvanceOutsideApplyTransactionCount
inboxAppliedOutsideAtomicTransactionCount
eventIdentityConflictSilentlyAcceptedCount
scopeMismatchSilentlyAppliedCount
unknownEntitySilentlyIgnoredCount
snapshotCompatClaimedAsEventCount
realtimeDirectRoomWriteCount
realtimeTransitiveRoomWriteCount
realtimeOldRecordDeleteAuthorityCount
realtimePayloadBusinessApplyCount
realtimePayloadUserVisibleSideEffectCount
firstSubscriberGlobalConnectedCount
lostHintFixtureFailures
atomicInboxFixtureFailures
scopeIsolationFixtureFailures
```

---

# 93. Acceptance values — exact/positive

```text
Room version                         = 16
new Room migrations                  = 1
new server migrations                = 0 expected
canonical Inbox table count          = 1
Inbox identity dimensions            = user/client/org/stream/eventId
registered Realtime participants     = 4 baseline participants handled
Realtime aggregate DEGRADED          = present
Realtime Room writer count           = 0
generation-safe coordinator          = preserved
push-before-pull                      = preserved
v68 transactional Outbox             = preserved
v69 command receipt protocol         = preserved
v69 typed retry taxonomy              = preserved
new waivers                           = 0
UI drift                              = 0
```

---

# 94. Snapshot compatibility truth fields

Verification JSON يجب أن يحتوي:

```text
legacyPositivePullUsesCanonicalEventFeed = false
syntheticEventIdentityIntroduced = false
legacySnapshotApplyTransactional = true/false
invoicePaymentLocalAtomicApplyVerified = true/false
serverTransactionGroupSnapshotConsistencyVerified = false unless genuine evidence exists
```

هذه ليست failures تلقائيًا إذا كانت مؤجلة وفق العقد؛ لكنها تمنع ادعاء ما لم يُثبت.

---

# 95. Inbox truth fields

```text
inboxPresent
inboxScoped
inboxReplayDedupeVerified
inboxIdentityConflictVerified
inboxCursorAtomicityVerified
inboxDeletionPathWired
inboxPositiveEventFeedCoverage
inboxServerRevisionAuthorityVerified
```

`inboxPositiveEventFeedCoverage` لا يجوز جعله `true` على legacy snapshots.

---

# 96. Realtime truth fields

```text
realtimeHintOnlyVerified
realtimeDirectRoomWriteCount
realtimeTransitiveRoomWriteCount
realtimeOldRecordAuthorityCount
realtimePayloadSideEffectCount
participantHealthTracked
aggregateDegradedStatePresent
connectedRequiresHealthyRequiredSet
realtimeDisabledCorrectnessIndependent
```

---

# 97. Predecessor truth fields

```text
predecessorGateSatisfied
v67TombstoneBlockerOpen
v69ServerRuntimeRun
v69AndroidCompileRun
handoff70AuthorizedAtStart
userExecutionOverride
```

لا تغير قيم البداية لتجميل النتيجة.

---

# 98. PASS الكامل

`PASS` يتطلب جميع ما يلي:

```text
predecessorGateSatisfied = true
handoff70AuthorizedAtStart or formally reauthorized = true
Room = 16
migration 15→16 verified
all static/model gates pass
canonical scoped Inbox present
event replay dedupe proven
entity/Inbox/cursor atomicity proven for event-bearing streams
zero synthetic event/revision authority
Realtime hint-only verified
zero Realtime-reachable Room writes
oldRecord not correctness authority
aggregate health truthful
v67/v68/v69 regressions pass
compile/unit/migration/runtime gates required by release policy actually run and pass
newV70WaiverCount = 0
```

---

# 99. PASS_STATIC_RUNTIME_BLOCKED

يجوز فقط إذا:

```text
predecessor gate is satisfied
source/static/model fully complete
no unresolved correctness ambiguity inside70 scope
Room migration structurally verified
runtime/build blocked only by environment
new waivers = 0
```

ويجب قول:

```text
COMPILED=false
ANDROID_MIGRATION_TESTED=false
REALTIME_RUNTIME_TESTED=false
```

حسب الواقع.

---

# 100. IMPLEMENTED_STATIC_BUT_PREDECESSOR_BLOCKED

إذا نُفذت 70 تحت user override مع بقاء chain blocker:

```text
finalVerdict = IMPLEMENTED_STATIC_BUT_PREDECESSOR_BLOCKED
handoff71Authorized = false
```

حتى لو:

```text
static/model PASS
Room16 structurally correct
Realtime source writes = 0
```

هذا يمنع سلسلة PASS وهمية.

---

# 101. BLOCKED_INBOUND_EVENT_CONTRACT

إذا executor حاول اعتبار legacy snapshots event feed واحتاج:

```text
eventId
serverRevision
transactionGroupId
```

ولا توجد authoritative server evidence:

```text
DO NOT INVENT
```

وسجّل:

```text
BLOCKED_INBOUND_EVENT_CONTRACT
```

لذلك الجزء event-bearing يبقى فقط على المصادر التي توفر identity موثوقة.

---

# 102. Scope completion interpretation

70 تنجح معماريًا دون سحب Session72 إذا حققت:

```text
Inbox machinery + atomic event apply ready
current event-bearing stream(s) use it where contract exists
legacy snapshots explicitly classified and transactionally applied locally
Realtime becomes pure hint
no fabricated global revision
```

ولا يلزمها إنشاء Unified Change Feed.

---

# 103. Invoice/payment acceptance interpretation

المطلوب في70:

```text
no local half-commit caused by crash between invoice and payment apply
```

أما:

```text
one exact server transaction group reconstructed from a revision feed
```

فهذا Session72.

يجب أن يذكر verification الفرق صراحة.

---

# 104. Chat boundary

70 لا تصلح:

```text
ASC LIMIT 100
conversation-scale pagination
durable media transfer
create_new_conversation ambiguity
```

فقط:

```text
Realtime Chat stops writing Room
authoritative sync remains source of local chat state
```

بقية Chat -> Session71.

---

# 105. Notification boundary

70 تزيل:

```text
Realtime notification upsert/delete
```

لكن تحافظ على:

```text
pending local read protection
v69 MARK_NOTIFICATION_READ command receipt
```

ولا تعيد تصميم FCM.

---

# 106. Balance boundary

70 تزيل Realtime direct mutations لـ:

```text
marketer_balance
balance_transactions
withdrawal_requests
```

لكن لا تغير:

```text
withdrawal eligibility
request_withdrawal RPC
cancel_pending_withdrawals command
v69 financial receipt semantics
```

---

# 107. Billing boundary

70 تزيل Realtime direct/bypass writes لـ:

```text
invoices
payments
commission_payments
```

وتحافظ على business mappings.

لا تغير commission calculation.

---

# 108. Profile boundary

لا يوجد سبب لتغيير Profile command protocol.

إذا snapshot pull يحتاج transactional guard adaptation:

```text
minimal data-layer change only
```

ولا UI/domain redesign.

---

# 109. Logging / privacy

يجوز logging:

```text
participant key
sync phase
safe event type
redacted scope identifiers
error code
```

ممنوع:

```text
raw chat body
raw push token
bank info
JWT
full profile payload
full Realtime record dumps
```

---

# 110. Determinism

Static/model verifier يجب تشغيله:

```text
run #1
run #2
```

والنتيجة semantic-identical.

أي nondeterminism:

```text
FAIL_VERIFIER_DETERMINISM
```

---

# 111. Required implementation order

```text
01 verify v69 ZIP SHA and extract clean tree
02 verify v69 reports/handoff/predecessor truth
03 freeze critical-file fingerprints
04 inventory every remote->Room write path
05 inventory all Realtime participants + transitive helpers
06 classify each source: EVENT_STREAM / SNAPSHOT_COMPAT / COMMAND_RECEIPT / REALTIME_HINT
07 design scoped SyncInbox entity/DAO without fake revision
08 add Room 15→16 migration
09 add exact-scope Inbox cleanup
10 implement atomic Inbox apply primitive
11 integrate DeletionSynchronizer with Inbox dedupe
12 refactor snapshot apply boundaries into Room transactions
13 make invoice+payment local apply one transaction after both fetches
14 remove Realtime DB authority from Billing
15 remove BillingTargetedRefresher Realtime bypass
16 remove Realtime DB authority from Balance
17 remove Realtime DB authority from Notifications
18 remove Realtime DB + local-notification payload authority from Chat
19 route all Realtime events to SyncReason.REALTIME_HINT
20 preserve generation-safe drain
21 add participant health tracking + DEGRADED
22 replace first-subscriber CONNECTED semantics
23 update architecture/unit/model tests
24 add migration model/instrumentation tests where possible
25 run inherited v67/v68/v69 regressions
26 run v70 static verifier twice
27 attempt Gradle compile/tests when environment permits
28 produce v70 verification JSON/MD + inventories
29 package final ZIP
30 unzip-test final archive
31 extract clean final ZIP
32 rerun static/model verifier on extracted ZIP
33 compare semantic verifier outputs
34 generate SHA-256 artifacts
```

---

# 112. Pre-implementation questions the executor must answer from code

قبل mutation، يسجل في inventory:

```text
Q1: كل server-originated Room writer الحالي أين؟
Q2: أي path يحمل stable server eventId فعلًا؟
Q3: أي path يحمل authoritative server revision فعلًا؟
Q4: أي path يملك resumable cursor؟
Q5: أي path snapshot compatibility فقط؟
Q6: أي Realtime participant يكتب Room مباشرة؟
Q7: أي helper يستدعيه Realtime ويكتب Room؟
Q8: أي payload يسبب user-visible side effect؟
Q9: أين oldRecord مستخدم كحقيقة DELETE؟
Q10: ما participant set الفعلي للحالة الحالية؟
Q11: ما تعريف required vs optional participant؟
Q12: هل أي test قديم يفرض direct Realtime apply ويحتاج تحديث؟
Q13: هل Room15 schema export متاح فعليًا؟
Q14: هل migration instrumentation يمكن تشغيله؟
Q15: هل authoritative tombstone server contract أصبح متاحًا أم ما زال blocked؟
```

أي جواب مجهول لا يجوز تخمينه.

---

# 113. Expected regression suite

يجب تشغيل ما أمكن من:

```text
v67 model 22/22
v68 model 36/36
v68 migration model
v69 model 15/15
v69 static command verifier
v70 model
v70 static
```

إذا scripts تعتمد على paths تغيرت، يجوز adaptation compile-safe لكن لا تخفض التغطية.

---

# 114. No waiver policy

المطلوب:

```text
newV70WaiverCount = 0
```

ممنوع إضافة exception ledger لإخفاء direct Realtime writes.

إذا تعذر إزالة path:

```text
BLOCKED_REALTIME_AUTHORITY_REMAINS
```

---

# 115. Required verification artifacts

يجب أن ينتج التنفيذ:

```text
AUTODRIVE_SYNC_VERIFICATION_v70.json
AUTODRIVE_SYNC_VERIFICATION_v70.md
AUTODRIVE_INBOUND_APPLY_INVENTORY_v70.json
AUTODRIVE_REALTIME_WRITE_INVENTORY_v70.json
```

ومعها:

```text
v70 static verifier
v70 model verifier
migration model/test evidence
```

---

# 116. Verification MD minimum contents

يشرح:

```text
baseline identity
predecessor gate truth
Room 15→16
Inbox schema and scope
event vs snapshot classification
atomic apply path
cursor rules
Realtime write inventory before/after
oldRecord removal
participant health semantics
legacy compatibility limitations
regression results
build/runtime truth
remaining 71–73 work
final verdict
handoff71 status
```

---

# 117. Verification JSON minimum fields

يجب أن يكون machine-readable ويشمل counters من الأقسام السابقة.

لا يكفي summary نصي.

---

# 118. Diff inventory

يجب تسجيل:

```text
production files touched
tests added/changed
Room migrations added
server migrations added
UI files touched
historical migrations modified
unexpected files touched
```

أي unexpected:

```text
FAIL_SCOPE_INTEGRITY
```

---

# 119. Packaging target

اسم archive المستهدف:

```text
AutoDrive-v70-durable-inbox-realtime-hints.zip
```

---

# 120. Package minimum contents

```text
modified source tree
SESSION_70_FINAL.md
AUTODRIVE_SYNC_VERIFICATION_v70.json
AUTODRIVE_SYNC_VERIFICATION_v70.md
AUTODRIVE_INBOUND_APPLY_INVENTORY_v70.json
AUTODRIVE_REALTIME_WRITE_INVENTORY_v70.json
v70 verifier(s)
Room 16 schema evidence if genuinely generated
migration test/model artifacts
```

---

# 121. Archive integrity

يجب:

```text
unzip -t AutoDrive-v70-durable-inbox-realtime-hints.zip
```

ثم extract clean وإعادة verifier.

Packaging drift:

```text
FAIL_PACKAGING_DRIFT
```

---

# 122. Output SHA-256

يجب إنتاج:

```text
AutoDrive-v70-durable-inbox-realtime-hints.zip.sha256
SESSION_70_FINAL.md.sha256
```

ويفضل reports SHA أيضًا.

---

# 123. Secret scan

قبل packaging افحص:

```text
service_role
JWT secret
access token
refresh token
password
OTP
raw push token
bank data dump
raw chat payload dump
```

أي secret حقيقي:

```text
BLOCKED_SECRET_LEAK
```

---

# 124. Generated junk policy

ممنوع تضمين:

```text
.gradle/
build/
IDE caches
local.properties real secrets
keystores
runtime DB copies with user data
Supabase credentials
```

---

# 125. Expected architecture after70

```text
LOCAL WRITE
Room transaction(Entity + scoped Outbox)
        ↓
v69 idempotent command + canonical receipt
        ↓
SERVER

INBOUND:
authoritative server event/snapshot pull
        ↓
classify source
        ├── EVENT_STREAM
        │      ↓
        │  scoped Inbox dedupe
        │      ↓
        │  entity/tombstone + Inbox + cursor
        │  IN ONE ROOM TRANSACTION
        │
        └── SNAPSHOT_COMPAT
               ↓
           scoped local Room transaction
           no fake event/revision
        ↓
Room = UI source of truth

REALTIME:
event
  ↓
hint only
  ↓
generation-safe SyncCoordinator
  ↓
authoritative pull/apply
```

---

# 126. What 70 changes conceptually

قبل70:

```text
Remote pull -> Room
Realtime payload -> Room
Deletion delta -> atomic Room+cursor
no durable Inbox
first Realtime subscriber -> global CONNECTED
```

بعد70:

```text
Inbound event -> durable scoped dedupe -> atomic apply
Realtime payload -> no business write
Realtime -> sync hint only
legacy snapshots explicitly non-event compatibility
aggregate Realtime health truthful
```

---

# 127. أهم invariant

```text
If a Realtime callback can still cause a business row to be inserted,
updated, or deleted without passing through authoritative pull/apply,
Session 70 is not complete.
```

---

# 128. invariant الثاني

```text
If an applied server event can be replayed after process death
and produce a second logical effect because no durable Inbox identity exists,
Session 70 is not complete.
```

---

# 129. invariant الثالث

```text
If cursor advancement can commit while entity/Inbox apply rolls back,
Session 70 is not complete.
```

---

# 130. invariant الرابع

```text
If Android invents eventId/serverRevision for a snapshot source
to make the Inbox look complete, Session 70 is invalid.
```

---

# 131. invariant الخامس

```text
If one healthy Realtime participant is enough to report global CONNECTED
while other required participants are down, Session 70 is not complete.
```

---

# 132. invariant السادس

```text
If v69 command receipt revision is used as a data sync cursor,
Session 70 is not complete.
```

---

# 133. Deferred truth after70

حتى لو 70 نُفذت بنجاح، يبقى:

```text
71 Chat scale/recovery + durable media + create conversation ambiguity
72 Unified Change Feed + global revision + bootstrap + anti-entropy
73 observability + full fault injection
```

كما يبقى inherited server tombstone blocker حتى يُغلق رسميًا.

---

# 134. Handoff إلى71

`handoff71Authorized = true` فقط إذا:

```text
predecessorGateSatisfied = true
Room16 migration accepted
Inbox scoped/dedupe/atomicity gates pass
Realtime hint-only complete
zero direct/transitive Realtime Room writes
aggregate health truthful
v67/v68/v69 regressions pass
newV70WaiverCount = 0
```

إذا 70 نُفذت تحت override والسلسلة ما زالت blocked:

```text
handoff71Authorized = false
```

حتى لو التنفيذ المحلي صحيح.

---

# 135. ما يجب أن تستلمه71

```text
AutoDrive-v70-durable-inbox-realtime-hints.zip
SESSION_70_FINAL.md
AUTODRIVE_SYNC_VERIFICATION_v70.json
AUTODRIVE_SYNC_VERIFICATION_v70.md
AUTODRIVE_INBOUND_APPLY_INVENTORY_v70.json
AUTODRIVE_REALTIME_WRITE_INVENTORY_v70.json
Room16 migration/schema evidence where genuine
v70 verifiers/tests
```

---

# 136. ما لا تعيده71

71 يجب ألا تعيد بناء:

```text
durable scoped Inbox
Realtime hint-only conversion
aggregate Realtime health foundation
v69 server command identity
v68 Outbox atomicity
v67 generation logic
```

بل تبني فوقها Chat recovery/media.

---

# 137. الحالة الحالية للعقد

بناءً على المصدر المفحوص:

```text
CONTRACT_READY = true
SOURCE_INSPECTED = true
PLAN_MAPPING_RESOLVED = true

RoomBaseline = 15
InboxPresent = false
RealtimeDirectWritesPresent = true
RealtimeOldRecordDeleteAuthorityPresent = true
RealtimeAggregateDegradedStatePresent = false
FirstSubscriberGlobalConnectedPresent = true

v69FinalVerdict = IMPLEMENTED_STATIC_BUT_PREDECESSOR_BLOCKED
handoff70Authorized = false
predecessorGateSatisfied = false
```

إذًا:

```text
EXECUTION_WITHOUT_OVERRIDE = BLOCKED_PREDECESSOR_HANDOFF
```

ولا يمنع ذلك استخدام هذا المستند كعقد التنفيذ عند صدور override صريح.

---

# 138. الخلاصة النهائية للعقد

70 ليست مجرد إضافة جدول `sync_inbox`.

تنجح فقط إذا أصبح inbound protocol:

```text
Stable scoped event identity
+ durable dedupe
+ atomic entity/Inbox/cursor apply
+ no fabricated revision
+ snapshot compatibility honesty
+ Realtime hint-only
+ generation-safe trailing sync
+ truthful participant health
```

مع الحفاظ الكامل على:

```text
v67 durable cursor / push-before-pull / generation safety
v68 atomic scoped Outbox / logout isolation
v69 idempotent commands / durable receipts / typed retry
```

والقاعدة النهائية:

```text
Realtime tells AutoDrive that something changed.
Only the authoritative sync path is allowed to decide what the local truth becomes.
```

---

# END OF SESSION_70_FINAL.md
