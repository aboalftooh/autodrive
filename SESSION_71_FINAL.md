# SESSION_71_FINAL.md

## AutoDrive Sync Modernization — Session 71

### Chat Recovery at Scale + Durable Media Transfer + Idempotent Conversation Creation

**نوع المستند:** عقد تنفيذ مستقل وصارم للجلسة الخامسة من مسار تحديث مزامنة AutoDrive المضغوط v67→v73  
**الجلسة:** 71  
**الحالة:** `PLAN ONLY — READY AS CONTRACT; EXECUTION GATED BY v70 HANDOFF / PREDECESSOR CHAIN + AUTHORITATIVE CHAT/SERVER/STORAGE EVIDENCE`  
**تاريخ الصياغة:** 2026-08-21  
**مصدر الكود المفحوص:** `AutoDrive-v70-durable-inbox-realtime-hints.zip`  
**SHA-256 للمصدر المفحوص:** `ed96aad130545287aefacc0f8d80ebd5f00960baac038815a3dd59a7c1739b78`  
**Archive entries:** `1325`  
**Production Kotlin files:** `263`  
**Test Kotlin files:** `45`  
**Room الحالي:** `16`  
**Room المستهدف في 71:** `17` — migration محلية واحدة فقط عند الحاجة لتنفيذ durable chat recovery/media state  
**مرجع التنفيذ السابق داخل ZIP:** `SESSION_70_FINAL.md`  
**SHA-256 لمرجع 70:** `0136f2ca5566893ae526dabc00c003fdbe646a0c86192f76b6266378d0cab4f2`  
**تقرير تحقق v70 JSON:** `AUTODRIVE_SYNC_VERIFICATION_v70.json`  
**SHA-256:** `a22d81f5cc885f69c32eaec9e82c2869db5959097d88e8be03a63956f7a29be9`  
**تقرير تحقق v70 MD:** `AUTODRIVE_SYNC_VERIFICATION_v70.md`  
**SHA-256:** `8489f4d23e03e18a1cdc0550f1fd8e58fe17357b333d4857fb72871564b55191`  
**v70 final verdict:** `IMPLEMENTED_STATIC_BUT_PREDECESSOR_BLOCKED`  
**v70 handoff71Authorized:** `false`  
**v70 static:** `78/78 PASS` deterministic  
**v70 model:** `36/36 PASS` deterministic  
**v70 migration model:** `16/16 PASS`  
**v67 inherited model:** `22/22 PASS`  
**v68 inherited model fixtures:** `36/36 PASS`  
**v69 model:** `15/15 PASS`  
**v70 Android compile:** `false — Gradle bootstrap blocked by UnknownHostException: services.gradle.org`  
**v70 unit tests:** `false`  
**v70 Android migration tests:** `false`  
**v70 Realtime runtime:** `false`  
**v67 inherited blocker:** `SERVER_TOMBSTONE_RUNTIME/CONTRACT CHAIN OPEN`  
**v69 server command migration SHA-256:** `6663381c4bf177c7cc22c75fb4c1eee1683290894307ec9ade85e4fe7620c01e`  
**production UI drift in v70:** `0`  
**new v70 waivers:** `0`

---

# 0. الحكم التنفيذي المختصر

Session 71 لا تعيد Inbox ولا Realtime ولا Idempotent Chat Send.

هذه الأجزاء أصبحت موجودة بالفعل في v70/v69:

```text
Room16 scoped Inbox
+ atomic deletion/inbox/cursor apply
+ Realtime hint-only
+ generation-safe Sync
+ scoped transactional Outbox
+ idempotent SEND_CHAT_MESSAGE receipt
+ durable MARK_CHAT_READ receipt
```

المتبقي المثبت فعليًا في كود v70 هو ثلاث فجوات Chat مستقلة:

```text
A) Recovery/scale
   internal_messages ORDER BY created_at ASC LIMIT 100
   موجودة في مسارين مختلفين
   => بعد 100 رسالة لا يوجد ضمان للوصول إلى الأحدث.

B) Media durability
   media upload يحدث قبل Room transaction وقبل تثبيت Outbox
   object path عشوائي في كل upload
   => crash/timeout يمكن أن يترك upload يتيمًا أو يفقد نية الإرسال.

C) Conversation creation ambiguity
   create_new_conversation RPC مباشر بلا mutationId/receipt
   => timeout-after-commit ثم retry قد ينشئ محادثة ثانية.
```

الحالة المستهدفة في 71:

```text
Authoritative Chat Recovery
        ↓
conversation-scoped keyset pages
        ↓
validate full scope + strict cursor tuple
        ↓
Room transaction {
    safe message merge
    conversation summary reconciliation
    advance exact chat checkpoint
}
```

وللوسائط:

```text
User selects media
        ↓
local durable staging only
        ↓
Room transaction {
    ChatMessage(PENDING)
    SEND_CHAT_MESSAGE intent
    DurableMediaTransfer
}
        ↓
network upload OUTSIDE Room
        ↓
reconcile stable object identity
        ↓
Room transaction {
    persist canonical durable media reference
    finalize immutable send payload before first send
    mark transfer COMPLETE
}
        ↓
existing v69 idempotent SEND_CHAT_MESSAGE
        ↓
canonical command receipt
```

ولإنشاء المحادثة:

```text
client-generated stable conversationId/mutationId
        ↓
Room transaction {
    local Conversation
    CREATE_CHAT_CONVERSATION Outbox
}
        ↓
idempotent typed server command
        ↓
durable command receipt
        ↓
messages targeting that conversation remain dependency-blocked
until conversation create receipt is committed
```

القواعد المطلقة:

```text
LIMIT 100 MUST NOT be a correctness boundary.
```

```text
No network media upload may happen before durable local intent exists.
```

```text
Media retry MUST reuse the same messageId, transfer identity, and remote object identity.
```

```text
Conversation retry MUST reuse the same mutationId and MUST NOT create a second logical conversation.
```

```text
Realtime remains HINT ONLY throughout Session 71.
```

```text
No chat compatibility cursor may masquerade as Session 72 global server revision.
```

```text
No synthetic eventId/serverRevision may be created merely to satisfy v70 Inbox semantics.
```

```text
No predecessor blocker may be hidden behind PASS.
```

---

# 1. لماذا Session 71 الحالية = Chat Repair

الخطة الأصلية v67→v80 كانت ترتب:

```text
71 = Durable Inbox + Atomic Apply
72 = Realtime Hint-Only
73 = Chat Sync Repair
```

لكن المسار المضغوط v67→v73 نقل:

```text
69 = Idempotent commands
70 = Durable Inbox + Atomic Apply + Realtime Hint-Only
71 = Chat scale/recovery + durable media + conversation ambiguity
72 = Unified change feed/global revision/bootstrap/anti-entropy
73 = observability/fault injection
```

ويؤكد `SESSION_70_FINAL.md` صراحة أن 71 تملك:

```text
Chat 10k pagination/recovery
create_new_conversation timeout duplication
durable media transfer queue
```

لذلك:

```text
SESSION_71_SCOPE =
    CHAT_RECOVERY_AT_SCALE
  + PER_CONVERSATION_KEYSET_PAGING
  + SAFE_CHAT_REMOTE_MERGE
  + DURABLE_MEDIA_UPLOAD_QUEUE
  + IDEMPOTENT_CONVERSATION_CREATION
  + CREATE→SEND_DEPENDENCY_ORDERING
```

وأي إعادة تنفيذ لـInbox/Realtime تعتبر:

```text
BLOCKED_DUPLICATE_SCOPE
```

---

# 2. بوابة البداية — v70 Handoff Gate

قبل أي mutation يجب قراءة:

```text
AUTODRIVE_SYNC_VERIFICATION_v70.json
AUTODRIVE_SYNC_VERIFICATION_v70.md
SESSION_70_FINAL.md
AUTODRIVE_SYNC_VERIFICATION_v69.md
SESSION_69_FINAL.md
```

والتحقق من:

```text
finalVerdict
handoff71Authorized
predecessorGateSatisfied
Room version
v70 static/model/migration results
Realtime direct/transitive Room write counters
newV70WaiverCount
v69 command receipt contract presence
```

الحالة الحالية المثبتة:

```text
finalVerdict             = IMPLEMENTED_STATIC_BUT_PREDECESSOR_BLOCKED
handoff71Authorized      = false
predecessorGateSatisfied = false
Room                     = 16
v70 static               = 78/78 PASS
v70 model                = 36/36 PASS
v70 migration model      = 16/16 PASS
Realtime direct writes   = 0
Realtime transitive writes = 0
newV70WaiverCount        = 0
```

إذًا افتراضيًا:

```text
SESSION_71_EXECUTION_GATE = BLOCKED_PREDECESSOR_HANDOFF
```

يسمح بالتنفيذ فقط إذا:

```text
A) أغلقت السلسلة السابقة رسميًا وأصبح handoff71Authorized=true
```

أو:

```text
B) أصدر المستخدم Override صريح لتنفيذ 71 فوق predecessor blocker
```

لكن تحت B:

```text
FULL_RELEASE_PASS = FORBIDDEN
handoff72Authorized = false
```

حتى تُغلق السلسلة رسميًا.

---

# 3. Baseline Gate — هوية v70

قبل mutation يجب تثبيت:

```text
ZIP SHA-256             = ed96aad130545287aefacc0f8d80ebd5f00960baac038815a3dd59a7c1739b78
archive entries         = 1325
production Kotlin       = 263
test Kotlin             = 45
Room                    = 16
v70 production UI drift = 0
v70 unexpected prod     = 0
v70 new waivers         = 0
```

أي اختلاف غير موثق:

```text
BLOCKED_INPUT_DRIFT
```

إذا Room ليست 16:

```text
BLOCKED_ROOM_BASELINE_DRIFT
```

إذا Realtime عاد يكتب Room مباشرة:

```text
BLOCKED_REALTIME_REGRESSION
```

---

# 4. ترتيب السلطات — Authority Order

عند التعارض:

1. `AutoDrive-v70-durable-inbox-realtime-hints.zip` بالـSHA المثبت.
2. `AUTODRIVE_SYNC_VERIFICATION_v70.json/.md` لحقيقة ما نُفذ في 70 وما بقي blocked.
3. `SESSION_70_FINAL.md` خصوصًا handoff وحدود 71/72/73.
4. `AUTODRIVE_SYNC_VERIFICATION_v69.md` و`SESSION_69_FINAL.md` لعقد command receipts وما تم تأجيله إلى 71.
5. `SESSION_67_FINAL.md` لتثبيت إعادة ترقيم الخطة المضغوطة.
6. `AUTODRIVE_SYNC_MODERNIZATION_REPAIR_PLAN_v67-v80.md` **لبند Chat Sync Repair الأصلي فقط** حيث لا يتعارض مع إعادة الترقيم.
7. authoritative current server schema / live introspection / storage policy evidence.
8. كود v70 الفعلي.
9. هذا العقد.

ممنوع استيراد semantics من Verto أو Optimal إلى AutoDrive.

---

# 5. بصمات الملفات الحرجة قبل التنفيذ

يجب تسجيل:

```text
LegacyRemotePuller.kt
  a09a57bfdc647c35009f797bf57de816beedf271ee351167ff52f017ce0332f4

ChatRepositoryImpl.kt
  c21c5fc458e59df5f1b2fc98d2124b2d6947f3bb4a383dae897bc7f30f0a79de

ChatMediaManager.kt
  b6ed481c3d4dd767cc3b4e2eb545d340a4f16df3bb63a54cf93667f2b27fcbfa

RetryFailedMessagesWorker.kt
  a5e60fe12530499d4d63e01c77a217b68d295d3eab3876999fb75a47774b5fae

AutoDriveDatabase.kt
  c7928ee79121887b79b0603b02ffa9cd584567139d14fc98963b1c3452819af1

Entities.kt
  5235b58a2445d0bba9ac08abbfe11b80740ab2f460fb7cfc7ade11d67e837d75

ChatMessageDao.kt
  32f7920ad23f576d09dfacf03a6e14ce12a735ab9baa1a6d659a926a67314bbd

ConversationDao.kt
  c897464c5708e90fdc47585e250e58ccaa938199870578b9a1bfb4f8dd0f50d5

OutboxContracts.kt
  cf32c39eefbb966f004a5b65a56ace51c7fa559042c61e13349c98b51492853a

v69 idempotent server SQL
  6663381c4bf177c7cc22c75fb4c1eee1683290894307ec9ade85e4fe7620c01e
```

أي drift في هذه الملفات قبل التنفيذ يجب توثيقه قبل مواصلة 71.

---

# 6. الفجوة المثبتة الأولى — LIMIT 100

في v70 يوجد مساران يقرآن `internal_messages` بالمنطق:

```text
client_id = current client
sender_type = ADMIN
ORDER BY created_at ASC
LIMIT 100
```

المساران هما:

```text
core/sync/.../LegacyRemotePuller.pullChat()
feature/chat/.../ChatRepositoryImpl.syncMessages()
```

المشكلة:

```text
100 oldest rows are returned
→ checkpoint does not exist
→ next run asks for the same oldest 100
→ message 101+ may never become local truth
```

هذا:

```text
BLOCKED_CHAT_RECOVERY_WINDOW
```

ولا يجوز علاجه بـ:

```text
LIMIT 500
LIMIT 1000
LIMIT 10000
```

زيادة الحد ليست pagination.

---

# 7. الفجوة المثبتة الثانية — مساران مستقلان للمزامنة

حالياً يوجد منطق Chat pull في:

```text
LegacyRemotePuller.pullChat()
ChatRepositoryImpl.syncMessages()
```

ولهما apply semantics مختلفة.

المطلوب:

```text
ONE authoritative chat recovery implementation
```

يفضل component إنتاجي واحد، مثل:

```text
ChatRecoverySynchronizer
ChatRecoveryPuller
```

على أن:

```text
LegacyRemotePuller delegates to it
ChatRepositoryImpl.syncMessages delegates to it
```

ولا يبقى مساران يطبقان remote chat rows إلى Room بشكل مستقل.

---

# 8. الفجوة المثبتة الثالثة — media upload قبل durability

`ChatRepositoryImpl.sendMessage()` يستدعي:

```text
mediaManager.prepareOutgoing(...)
```

قبل:

```text
db.withTransaction {
    insert ChatMessage
    insert PendingOperation
}
```

و`prepareOutgoing()` في v70 ينفذ network upload.

إذًا يوجد window:

```text
upload succeeds
→ process dies / Room transaction never commits
→ remote storage object exists
→ no durable message/outbox owns it
```

كما يوجد:

```text
o network
→ upload fails
→ local message intent is never durably recorded
```

هذا مخالف لهدف Offline-First.

---

# 9. الفجوة المثبتة الرابعة — random media object identity

v70 ينشئ storage path بالشكل:

```text
$orgId/${UUID.randomUUID()}.$extension
```

كل retry يمكن أن ينتج object جديدًا.

في 71 يجب أن تصبح هوية remote media deterministic بالنسبة للمحاولة المنطقية نفسها.

قاعدة:

```text
same messageId + same content
→ same transfer identity
→ same object identity
```

---

# 10. الفجوة المثبتة الخامسة — signed URL لمدة 7 أيام

v70 يستخدم:

```text
createSignedUrl(path, 7.days)
```

ثم يمرر `mediaUrl` إلى command الرسالة.

إذا كانت هذه القيمة نفسها هي المرجع الدائم المخزن في `internal_messages`:

```text
message survives
but media reference expires
```

وهذا غير مقبول كـdurable media contract.

قبل اعتماد wire contract يجب إثبات أحد التالي:

```text
A) media_url هو URL عام غير منتهي الصلاحية
B) server stores a durable object key/path and clients mint signed URLs on read
C) توجد آلية server-side durable resolver موثقة
```

إذا لم يتوفر دليل:

```text
BLOCKED_MEDIA_REFERENCE_CONTRACT
```

ممنوع اعتبار signed URL مدته 7 أيام مرجعًا دائمًا لمجرد نجاح upload.

---

# 11. الفجوة المثبتة السادسة — create_new_conversation ambiguity

v70 ينفذ:

```text
rpc("create_new_conversation", ...)
```

مباشرة بلا:

```text
mutationId
durable Outbox
canonical receipt
replay reconciliation
```

السيناريو:

```text
server creates conversation
→ response lost
→ Android reports error
→ user retries
→ second conversation may be created
```

هذا هو deferred risk المثبت من Session 69.

---

# 12. Server Chat Contract Evidence Gate

قبل كتابة أي SQL في 71 يجب توفير دليل authoritative حديث يغطي:

```text
public.conversations table definition
public.internal_messages table definition
create_new_conversation current function body/signature
get_or_create_conversation current function body/signature
RLS policies on conversations/internal_messages
function grants/revokes
authenticated ownership semantics
created_at default/nullability/immutability
message id type/uniqueness
conversation id type/uniqueness
storage buckets chat-images/chat-audio
storage policies / public-vs-private semantics
```

الدليل المقبول:

```text
A) current schema.sql / pg_dump schema-only
B) original migrations defining these objects
C) live introspection documented from PostgreSQL/Supabase
D) authoritative RPC/storage contract that hides table details
```

بدون ذلك:

```text
SERVER_CHAT_SCHEMA_EVIDENCE = INCOMPLETE
```

ويجوز تنفيذ local abstractions/tests فقط، لكن:

```text
FULL_SESSION_71_PASS = BLOCKED_SERVER_CHAT_CONTRACT
```

---

# 13. ممنوع تعديل migration v69 التاريخية

ممنوع تعديل:

```text
supabase/migrations/20260821203000_autodrive_idempotent_commands_v1.sql
```

إذا احتاج 71 server changes:

```text
ADD ONE NEW APPEND-ONLY MIGRATION
```

مع اسم زمني جديد.

أي mutation للمigration القديمة:

```text
BLOCKED_HISTORICAL_SERVER_MIGRATION_MUTATION
```

---

# 14. Chat Recovery Protocol — المطلوب

لكل conversation:

```text
resolve exact SyncScope
fetch page OUTSIDE Room transaction
validate all rows
validate strict total ordering
Room.withTransaction {
    revalidate current scope
    merge page idempotently
    reconcile conversation summary
    advance exact conversation checkpoint
}
repeat until page exhausted
```

لا cursor advance قبل commit.

---

# 15. نوع pagination المسموح

الافتراضي في 71:

```text
KEYSET PAGINATION
```

وليس:

```text
OFFSET pagination
page numbers
ASC LIMIT 100 terminal window
local timestamp cutoff
```

المفتاح المقبول إذا أثبته server contract:

```text
(created_at, id)
```

بترتيب:

```text
ORDER BY created_at ASC, id ASC
```

والصفحة التالية:

```text
(created_at, id) > (lastCreatedAt, lastId)
```

إذا Supabase/PostgREST client لا يمكنه التعبير عن tuple predicate بأمان:

```text
DO NOT emulate it incorrectly.
```

بدلاً من ذلك يجب إضافة RPC server-side موثق يعيد صفحة + cursor ثابت.

---

# 16. شرط created_at/id قبل استخدام tuple cursor

لا يعتمد `(created_at,id)` كـcursor إلا إذا ثبت:

```text
created_at server-owned
created_at non-null
created_at immutable for message lifetime
id unique and immutable
rows can be ordered by created_at then id deterministically
new message cannot intentionally backdate behind accepted cursor under supported contract
```

إذا لم يثبت ذلك:

```text
BLOCKED_CHAT_CURSOR_SEMANTICS
```

ولا يتم اختراع ضمان من Android.

---

# 17. ممنوع device clock في Chat cursor

ممنوع:

```text
System.currentTimeMillis()
```

كقيمة fallback لـ:

```text
chat page cursor
remote message ordering authority
resume token
```

إذا `created_at` غير صالح:

```text
FAIL THE PAGE
DO NOT ADVANCE CHECKPOINT
```

يجوز استخدام local time فقط لـdiagnostics/local timestamps غير الحاكمة.

---

# 18. Chat Recovery Checkpoint

لأن `sync_cursors` في v67 موثق كـopaque server cursor، لا يجوز كسر عقده بإدخال tuple محلية غير موثقة داخله.

الافتراضي في 71 إذا استُخدمت tuple keyset مباشرة:

```text
new dedicated durable table:
chat_recovery_checkpoints
```

المفتاح:

```text
user_id
client_id
org_id
conversation_id
```

القيم:

```text
last_created_at_server TEXT
last_message_id TEXT
contract_version
updated_at_local
```

`updated_at_local` تشخيص فقط وليس ordering authority.

إذا استُخدم server RPC يعيد opaque cursor حقيقي، يجوز استخدام `sync_cursors` عبر stream مستقل موثق.

---

# 19. Room 16 → 17

Session 71 تتوقع migration واحدة:

```text
MIGRATION_16_17
```

يجب أن تكون append-only.

الحد الأدنى المتوقع إذا نفذ التصميم المحلي الكامل:

```text
chat_recovery_checkpoints
chat_media_transfers
pending_operations.depends_on_mutation_id NULLABLE
```

لا تغير أي migration تاريخية.

---

# 20. chat_recovery_checkpoints schema contract

يجب أن تكون scoped بالكامل:

```text
PRIMARY KEY (
  user_id,
  client_id,
  org_id,
  conversation_id
)
```

ولا يوجد global chat cursor.

يجب أن يسمح logout بحذف exact scope فقط.

---

# 21. Durable Media Transfer entity

أضف durable queue منفصلة عن command receipt ledger.

الحقول الدنيا:

```text
transfer_id
user_id
client_id
org_id
message_id
direction
local_path
media_mime
size_bytes
content_sha256
bucket
object_path
remote_reference
status
attempt_count
next_retry_at
lease_until
last_error_code
created_at
updated_at
```

الـqueue لا تخزن raw bytes في Room.

---

# 22. Media transfer scope

كل transfer MUST be owned by:

```text
userId + clientId + orgId + messageId
```

ممنوع:

```text
transfer row بلا owner scope
worker يقرأ كل الحسابات
logout يحذف global queue
```

---

# 23. Media transfer identity

يفضل:

```text
transferId = stable id derived/generated once with messageId
```

ويجب حفظه قبل أول network attempt.

لا UUID جديد في كل retry.

---

# 24. Stable object path

remote storage object path يجب أن يكون deterministic للمحاولة المنطقية.

مثال مقبول من حيث المبدأ:

```text
<scope>/<messageId>-<contentHash>.<ext>
```

لكن شكل المسار الفعلي يجب أن يطابق storage policy authoritative.

القواعد:

```text
same transfer retry -> same path
changed bytes under same message mutation -> CONFLICT
```

---

# 25. Upload preflight

قبل durable enqueue يجب التحقق محليًا من:

```text
source readable
supported type
supported MIME
size <= configured contract limit
content hash computed
stable local staging file created
```

v70 يطبق 15 MB على download فقط؛ 71 يجب ألا تسمح upload غير محدود بلا دليل.

إذا حد السيرفر مختلف، server evidence يحكم.

---

# 26. No network before durable intent

`prepareOutgoing()` لا يجوز أن يبقى function يقوم بالـupload قبل transaction.

يفصل إلى semantics واضحة، مثل:

```text
stageOutgoingMedia()  // local only
transferWorker.upload() // network later
```

القانون:

```text
NO NETWORK I/O before ChatMessage + Outbox intent + Transfer commit.
```

---

# 27. Initial media send transaction

لـIMAGE/VOICE يجب أن يثبت commit واحد على الأقل:

```text
Room.withTransaction {
    insert ChatMessage(status=PENDING)
    insert SEND_CHAT_MESSAGE pending operation
    insert ChatMediaTransfer(status=PENDING)
}
```

إذا فشل transaction:

```text
none of the three may survive logically
```

الـlocal staging file قد يوجد ويُنظف لاحقًا؛ لكنه لا يمثل intent وحده.

---

# 28. Outbox payload before upload

لا يجوز إرسال media message قبل اكتمال transfer.

يوجد خياران مقبولان فقط إذا أثبتا invariant:

```text
A) SEND_CHAT_MESSAGE outbox موجود من البداية لكنه dependency-blocked
   ثم payload النهائي يُثبت قبل أول claim فقط.

B) durable transfer operation itself is the canonical send-intent carrier
   بشرط عدم كسر v68 invariant ووجود explicit handoff إلى send outbox.
```

الافتراضي في هذا العقد هو A لأنه يحافظ على v68 Outbox invariant.

---

# 29. Pending operation dependency

أضف nullable field:

```text
depends_on_mutation_id
```

للاستخدام عند:

```text
message depends on CREATE_CHAT_CONVERSATION
```

ولـmedia يمكن استخدام transfer readiness gate منفصلة حسب messageId.

جميع العمليات القديمة:

```text
depends_on_mutation_id = NULL
```

ولا تتغير semantics الخاصة بها.

---

# 30. Outbox due eligibility

`getDue()` لا يعيد child operation إذا dependency نفسها ما زالت موجودة في exact scope.

ممنوع إرسال:

```text
SEND_CHAT_MESSAGE
```

قبل نجاح:

```text
CREATE_CHAT_CONVERSATION
```

للمحادثة المحلية الجديدة.

---

# 31. Media readiness gate

`SEND_CHAT_MESSAGE` لرسالة media غير eligible إذا وجد transfer غير COMPLETE لنفس:

```text
scope + messageId
```

ويجب وجود guard ثاني في sender نفسه fail-closed.

أي race بين DAO eligibility وsender يجب ألا يسمح بإرسال media reference غير نهائية.

---

# 32. Outbox payload immutability boundary

قبل أول server send يجوز finalizing media reference فقط إذا:

```text
operation.status = PENDING
attempt_count = 0
operation was never claimed/sent
```

بعد أول claim/network attempt:

```text
canonical request fingerprint inputs MUST be immutable
```

ممنوع تغيير `mediaUrl/mediaReference` بعد إرسال mutation نفسها مرة واحدة.

---

# 33. Transfer completion transaction

بعد upload/reconciliation الناجح:

```text
Room.withTransaction {
    revalidate exact SyncScope
    verify transfer still owns message
    verify message mutation unchanged
    persist canonical durable remote reference
    finalize pending SEND_CHAT_MESSAGE payload if never attempted
    mark transfer COMPLETE
}
```

ثم فقط يصبح Outbox eligible.

---

# 34. Crash after object upload before Room commit

هذا scenario إلزامي.

بعد restart:

```text
same transfer row
same messageId
same content hash
same object path
```

الworker يعيد reconcile نفس object.

ممنوع إنشاء object جديد أو message جديد.

---

# 35. Already-existing storage object

إذا retry اكتشف object path موجودًا:

لا يعتبر نجاحًا أعمى.

يجب إثبات توافقه مع transfer contract، مثل:

```text
same deterministic path derived from same messageId/content hash
compatible content metadata
```

إذا تعارض:

```text
MEDIA_OBJECT_CONFLICT
```

ولا يعمل overwrite صامت.

---

# 36. Incoming media downloads

`cachePendingAdminMedia()` هو cache optimization وليس correctness authority.

في 71:

```text
message row convergence MUST NOT depend on successful media download
```

فشل download:

```text
must not roll back chat message page cursor
must not erase message
must not mark server state failed
```

يمكن retry للcache بشكل منفصل.

---

# 37. Expiring URL gate

إذا أثبت server/storage evidence أن media private:

يجب تخزين durable object reference وليس signed URL منتهي الصلاحية كحقيقة نهائية.

إذا wire schema يحتاج field جديد:

```text
new append-only server migration only
```

ولا تغير semantics مشتركة مع Verto بلا evidence.

إذا يتطلب الحل producer/consumer change خارج AutoDrive ولم يقدم:

```text
BLOCKED_SHARED_MEDIA_CONTRACT
```

---

# 38. Conversation creation — local identity

Session 71 تعتمد stable local identity:

```text
conversationId generated once
mutationId generated once
```

يفضل إذا server schema يسمح:

```text
conversationId == mutationId
```

لتقليل reconciliation ambiguity.

لكن لا يفرض ذلك إذا authoritative schema يمنعه؛ المهم deterministic relation محفوظة.

---

# 39. CREATE_CHAT_CONVERSATION Outbox operation

أضف operation جديد:

```text
CREATE_CHAT_CONVERSATION
```

والـentity type:

```text
conversations
```

payload الأدنى:

```text
conversationId
subject
```

ownership لا يؤخذ من payload كسلطة.

السيرفر يشتق:

```text
auth.uid()
client/org scope
```

من الجلسة authoritative.

---

# 40. createNewConversation local transaction

بدل direct RPC:

```text
Room.withTransaction {
    verify current scope
    insert local Conversation using stable conversationId
    insert CREATE_CHAT_CONVERSATION Outbox using stable mutationId
}
```

ثم:

```text
request normal sync/outbox flush
```

لا يلزم انتظار الشبكة لإثبات أن النية حفظت محليًا.

---

# 41. Idempotent server create command

إذا server schema evidence يسمح، أضف typed RPC جديد، مثل:

```text
autodrive_create_chat_conversation_command_v1
```

مع v69 receipt discipline نفسها:

```text
stable mutationId
server-derived scope
request fingerprint
advisory/transaction lock
same mutation same result
same mutation different canonical request -> CONFLICT
APPLIED/REJECTED/CONFLICT typed receipt
```

لا تعيد اختراع receipt ledger.

استخدم ledger v69 نفسه.

---

# 42. Server create command business semantics

يجب أن يطابق existing `create_new_conversation` semantics الفعلية بعد introspection:

```text
required columns
defaults
subject normalization
client/org ownership
marketer association
created_at behavior
RLS/grants
```

ممنوع اختراع أعمدة أو defaults.

---

# 43. timeout-after-commit create conversation

السيناريو المطلوب:

```text
mutation M
server creates C and stores receipt
response lost
Android preserves Outbox M
retry M
server returns canonical receipt for C
no second C
```

هذا gate إلزامي.

---

# 44. mutation reuse conflict

```text
same mutationId
same conversationId
subject = A
```

ثم retry مع:

```text
subject = B
```

يجب أن ينتج:

```text
CONFLICT
```

وليس تعديل/إنشاء محادثة ثانية.

---

# 45. get_or_create_conversation inventory

يجب فحص `get_or_create_conversation` أيضًا.

إذا ثبت أنه server-idempotent تحت exact scope:

```text
may remain as a proven compatibility path
```

إذا لم يثبت:

```text
must not be labeled safe
```

ويجب تحويله إلى typed/durable behavior أو تسجيل blocker واضح.

ممنوع تخمين idempotency من اسم RPC.

---

# 46. Create → Send dependency

المستخدم قد ينشئ محادثة جديدة ثم يرسل رسالة قبل وصول create للسيرفر.

يجب أن تكون النتيجة:

```text
Conversation create Outbox = parent
Message send Outbox = child
child.dependsOnMutationId = parent.mutationId
```

الchild لا يُرسل حتى اختفاء parent بعد canonical APPLIED receipt.

---

# 47. Parent failure semantics

إذا parent create بقي:

```text
PENDING / IN_PROGRESS / DEAD_LETTER
```

الchild لا يرسل.

لا تحول `TARGET_NOT_FOUND` إلى retry spam على message بينما create لم ينجح.

Recovery UX الشامل للdead letters يبقى خارج 71 وفق الخطة اللاحقة، لكن dependency يجب ألا تضيع.

---

# 48. Existing server conversations

الرسائل في محادثات موجودة فعلاً على السيرفر:

```text
depends_on_mutation_id = NULL
```

ولا تضف dependency وهمية.

---

# 49. Chat page must include all relevant messages

Recovery correctness لا يجوز أن يعتمد على:

```text
sender_type = ADMIN only
```

إذا server/RLS contract يسمح بقراءة marketer messages لنفس scope، يجب pagination على كلا النوعين:

```text
ADMIN
MARKETER
```

حتى جهاز جديد أو جهاز ثانٍ يستطيع إعادة بناء التاريخ المرسل أيضًا.

إذا server policy يمنع ذلك:

```text
BLOCKED_CHAT_MULTI_DEVICE_HISTORY_CONTRACT
```

ولا يدعى 10k full convergence.

---

# 50. Message identity validation

لكل remote row:

```text
id nonblank
conversation_id exact requested conversation
client_id == scope.clientId
org_id == scope.orgId
sender_id valid for sender type under contract
created_at strict parseable server value
```

أي mismatch:

```text
FAIL PAGE
NO CHECKPOINT ADVANCE
```

---

# 51. Safe remote message merge

`insertOrIgnore()` وحدها غير كافية للتقارب إذا remote row موجودة محليًا وتغير server-owned state مثل `is_read`.

يجب بناء merge صريح.

لـremote existing row:

```text
immutable identity fields must match
conversationId must match
messageId must match
sender identity/type must match
```

ثم يسمح بتحديث server-owned fields فقط حسب contract.

---

# 52. Pending local message protection

إذا توجد local MARKETER message ولها active SEND_CHAT_MESSAGE Outbox:

```text
remote snapshot MUST NOT overwrite local unsent intent
```

خصوصًا:

```text
body
type
media localPath
canonical mutation identity
```

إذا server row بنفس id ظهرت، تستخدم كreconciliation evidence فقط وفق v69 receipt semantics ولا تغير fingerprint inputs عشوائيًا.

---

# 53. Read-state merge

لـMARKETER message موجودة على السيرفر:

```text
remote is_read=true
```

يجوز أن يرفع local presentation state إلى:

```text
READ
```

إذا لا توجد local pending semantics تمنع ذلك.

لا يخفض:

```text
READ -> SENT
```

بسبب snapshot أقدم.

---

# 54. Admin read intent protection

إذا يوجد active:

```text
MARK_CHAT_READ
```

للمحادثة، local unread يجب ألا يعود >0 بسبب snapshot أقدم قبل تنفيذ receipt.

يحافظ 71 على gate الموجود في v70 ولا يزيله.

---

# 55. Conversation summary reconciliation

بعد apply page:

```text
lastMessage
lastMessageAt
unreadCount
```

تُحسب من authoritative remote data + protected local intents بطريقة deterministic.

ممنوع page قديمة أن تخفض `lastMessageAt`.

---

# 56. Empty remote page

صفحة فارغة تعني:

```text
no newer rows after this checkpoint
```

ولا تعني:

```text
delete local messages
```

الحذف يبقى عبر tombstone/change-feed semantics.

---

# 57. No synthetic Inbox event

Chat compatibility page ليست canonical event feed.

لذلك ممنوع إنشاء:

```text
fake eventId
fake serverRevision
fake transactionGroupId
```

فقط لكي تمر عبر `sync_inbox`.

يمكن تطبيق page + dedicated checkpoint atomically بدون Inbox event إذا feed لا يوفر event identity.

---

# 58. Session 72 boundary — mutable old rows

إذا current server chat snapshot لا يملك revision/update feed موحدًا، فإن keyset by creation tuple يضمن **append recovery** وليس proof كامل لكل future mutation على صف قديم.

لا تدعي أكثر من ذلك.

الآتي يبقى لـ72:

```text
unified change feed
global monotonic revision
canonical upsert/delete event history
old-row mutation completeness
bootstrap/rebootstrap
anti-entropy
```

Session 71 يجب أن توثق هذا الحد صراحة.

---

# 59. Message edit/delete inventory

فحص v70 لا يظهر production Chat edit/delete command paths.

إذًا 71:

```text
MUST NOT invent new edit/delete UI or business feature
```

إذا ظهرت paths أثناء التنفيذ بسبب source drift، يجب inventory وتصنيفها قبل المتابعة.

---

# 60. Realtime invariant

بعد 71 يجب أن يبقى:

```text
Realtime event
→ hint
→ SyncCoordinator
→ authoritative pull/recovery
→ Room
```

ولا يجوز:

```text
Realtime payload → ChatMessageDao
Realtime payload → ConversationDao
Realtime payload → media download/publish side effect
```

---

# 61. Realtime counters after 71

يجب أن تظل:

```text
realtimeDirectRoomWriteCount = 0
realtimeTransitiveRoomWriteCount = 0
realtimeOldRecordDeleteAuthorityCount = 0
realtimePayloadBusinessApplyCount = 0
realtimePayloadUserVisibleSideEffectCount = 0
```

أي regression:

```text
FAIL_STATIC
```

---

# 62. Network I/O boundary

كل network operations التالية خارج Room transaction:

```text
chat page fetch
conversation list fetch
storage upload
storage metadata/existence reconciliation
signed/public URL resolution
server command call
incoming media download
```

Room transaction قصيرة ومحلية فقط.

---

# 63. Logout / scope switch during Chat recovery

بعد كل network response وقبل Room apply:

```text
SyncScope.from(currentSession) == capturedScope
```

وإلا:

```text
STALE_SYNC_SCOPE
NO APPLY
NO CHECKPOINT ADVANCE
```

---

# 64. Logout / scope switch during media upload

الworker يلتقط exact scope من transfer row.

بعد network upload وقبل Room completion:

```text
revalidate current session == transfer scope
```

إذا تغير الحساب:

```text
NO OLD-SCOPE LOCAL COMPLETION
```

والremote object cleanup/reconciliation يتم وفق policy آمنة، لا عبر كتابة بيانات للحساب الجديد.

---

# 65. LocalDataCleaner

يجب توسيع exact-scope cleanup ليشمل:

```text
chat_recovery_checkpoints
chat_media_transfers
```

مع الحفاظ على:

```text
pending_operations
sync_cursors
sync_inbox
```

ممنوع global delete لطوابير chat الجديدة.

---

# 66. Local staged files on logout

حذف rows من Room لا يكفي إذا بقيت files حساسة لحساب سابق.

يجب inventory local staged media ownership.

الملفات التي تخص departing scope ولا تحتاجها عملية ناجحة قائمة يجب تنظيفها وفق policy موثقة.

لا تحذف shared/current-account media بالخطأ.

---

# 67. RetryFailedMessagesWorker

الworker الحالي يفحص فقط:

```text
chat_messages.status = FAILED
```

ثم `retrySend()`.

هذا لا يمثل durable media transfer queue.

في 71 يجب:

```text
introduce ChatMediaTransferWorker/processor
```

أو إعادة تصميم العامل الحالي بوضوح، بشرط:

```text
no duplicate responsibility
no direct resend with new messageId
no bypass of Outbox
```

---

# 68. WorkManager semantics

Media worker يجب أن يكون:

```text
network constrained
restart-safe
idempotent
scope-safe
lease-aware or claim-safe
```

الـWorkManager ليس source of truth.

Room transfer queue هي source of durable transfer intent.

---

# 69. Media transfer leases

لمنع worker مزدوج:

```text
PENDING -> IN_PROGRESS with leaseUntil
```

وعند process death:

```text
expired lease -> PENDING
```

لا تستخدم `nextRetryAt` كبديل للlease.

---

# 70. Media retry taxonomy

ممنوع parsing نص رسالة الخطأ لاتخاذ قرار correctness.

يفضل تصنيف:

```text
TRANSIENT_NETWORK
AUTH
PERMISSION
VALIDATION
OBJECT_CONFLICT
LOCAL_FILE_MISSING
PERMANENT_PROTOCOL
AMBIGUOUS_UPLOAD
```

يمكن UI mapper استخدام text للعرض، لكن retry policy لا تعتمد عليه.

---

# 71. Ambiguous upload outcome

إذا upload request timeout ولا يعرف هل object تم إنشاؤه:

```text
DO NOT allocate a new path
```

بل:

```text
reconcile same deterministic object identity
```

ثم إما:

```text
COMPLETE same transfer
or retry same transfer/path
or typed conflict
```

---

# 72. Media retry must not recreate message

عند user retry أو worker retry:

```text
messageId unchanged
conversationId unchanged
SEND_CHAT_MESSAGE mutationId unchanged
transferId unchanged
object path unchanged
```

الاختبار إلزامي.

---

# 73. Text messages regression guard

TEXT send path لا يحتاج media queue.

يجب أن يبقى:

```text
ChatMessage + SEND_CHAT_MESSAGE Outbox
inside one Room transaction
```

ولا يُبطأ بtransfer dependency غير موجودة.

---

# 74. v69 receipt semantics must remain

بعد نجاح media transfer، الإرسال لا يتم direct insert.

يستمر عبر:

```text
IdempotentServerCommandGateway.sendChatMessage()
```

والreceipt يجب أن تمر نفس validation:

```text
mutationId
commandType
revisionKind=COMMAND_RECEIPT
serverEntityId
resultStatus
```

---

# 75. COMMAND_RECEIPT revision boundary

حتى بعد إضافة create conversation command:

```text
serverRevision from command receipt
```

لا يصبح:

```text
chat recovery cursor
Inbox data revision
global change-feed revision
```

هذه revisions ذات semantic type مختلف.

---

# 76. Outbox contract version safety

ممنوع رفع global `OUTBOX_CONTRACT_VERSION` بطريقة تجعل pending rows v68/v69 غير قابلة للمعالجة.

إذا احتاج operation جديد version جديد:

```text
support old persisted versions explicitly
```

أو استخدم version الحالي للعملية الجديدة إذا schema متوافق ولا توجد rows تاريخية لها.

---

# 77. Room migration safety

`MIGRATION_16_17` يجب أن تثبت:

```text
all existing 16 tables/data preserved
sync_inbox preserved exactly
pending_operations rows preserved
new depends_on_mutation_id defaults NULL
new chat tables created
indexes created
no destructive migration
```

---

# 78. Migration test — 16→17

Android migration test إلزامي عند توفر runtime:

```text
create DB16 fixture
insert representative existing Outbox/Inbox/chat rows
migrate 16→17
verify old rows unchanged
verify new tables/column/indexes
verify schema validates Room17
```

بدون instrumentation runtime:

```text
ANDROID_MIGRATION_TESTED=false
```

ولا يدعى PASS runtime.

---

# 79. Required Chat Recovery unit/model tests

على الأقل:

```text
1. 10,000 messages arrive across pages.
2. message 101+ is reachable.
3. newest row is reached without Realtime.
4. duplicate page replay adds no duplicate logical message.
5. crash before page commit keeps checkpoint old.
6. crash after commit resumes after committed tuple.
7. malformed created_at fails page and preserves checkpoint.
8. tuple ordering handles same created_at with different ids.
9. scope mismatch fails whole page.
10. local pending outgoing message is not overwritten.
11. remote read state does not regress READ->SENT.
12. empty page is not deletion.
13. two conversations have independent checkpoints.
14. account B cannot read/update account A checkpoint.
15. feature syncMessages and global sync use same recovery implementation.
```

---

# 80. 10k acceptance definition

لا يكفي:

```text
DAO can store 10k rows
```

يجب إثبات:

```text
server/fake pager emits >100 rows
client drains every page
last row is applied
checkpoint reaches exact last tuple
second run does not redownload from zero unless explicitly bootstrap
```

---

# 81. Required media tests

إلزاميًا:

```text
1. Offline media send persists message+outbox+transfer.
2. No network call happens before durable transaction.
3. Upload retry reuses transfer/message/object identity.
4. Timeout after storage commit reconciles same object.
5. Process death after upload before Room completion is recoverable.
6. Transfer COMPLETE makes same Outbox eligible.
7. Outbox cannot send before transfer completion.
8. Payload cannot mutate after first claim/send.
9. Local file missing yields typed failure, not new message.
10. Oversized file rejected deterministically.
11. MIME/extension contract validated.
12. Logout during upload cannot update new session.
13. Exact-scope cleanup removes only departing queue.
14. Incoming media download failure does not block message convergence.
15. media retry never creates second chat message.
```

---

# 82. Required create-conversation tests

إلزاميًا:

```text
1. local Conversation + create Outbox commit atomically.
2. crash before transaction commit leaves neither.
3. same mutation replay returns same conversation.
4. timeout-after-server-commit + retry creates one conversation.
5. same mutation different subject conflicts.
6. create server ownership derived from auth scope.
7. cross-client/org conversation id rejected.
8. child message blocked while create parent active.
9. child becomes eligible only after parent APPLIED finalization.
10. parent DEAD_LETTER does not cause child direct send.
11. retry does not allocate new conversationId/mutationId.
12. get_or_create idempotency is proven or explicitly blocked.
```

---

# 83. Required server SQL tests if new create RPC exists

يجب توفير executable SQL tests لـ:

```text
same mutation same payload -> same receipt
same mutation changed subject -> CONFLICT
same conversation id cross-scope -> REJECTED/blocked
receipt ownership = auth-derived scope
no direct receipt ledger authenticated access
correct grants/revokes
timeout/replay simulation where feasible
one logical conversation effect
```

لا يكفي static regex على SQL لإعلان server runtime PASS.

---

# 84. Required storage runtime tests

لـFULL media PASS يجب اختبار فعلي أو بيئة موثوقة تمثل production policy:

```text
upload
retry same object identity
already-exists reconciliation
signed/public reference behavior
logout/auth rejection
read after configured durability horizon where contract permits
```

إذا لم تتوفر:

```text
MEDIA_STORAGE_RUNTIME_TESTED=false
```

---

# 85. Static gate — LIMIT 100

بعد 71:

```text
chatRecoveryTerminalLimit100Count = 0
```

وجود `limit(100)` في unrelated UI query ليس failure تلقائيًا؛ gate يطابق authoritative Chat recovery paths تحديدًا.

---

# 86. Static gate — duplicate Chat pull implementations

بعد 71:

```text
independentChatRemoteApplyPathCount = 1
```

`LegacyRemotePuller` و`ChatRepositoryImpl` يجوز أن يستدعيا نفس component، لكن لا يملكان logic مختلفًا للتطبيق.

---

# 87. Static gate — media pre-durability network

بعد 71:

```text
mediaUploadBeforeDurableIntentCount = 0
```

و`ChatRepositoryImpl.sendMessage` لا يستدعي network upload قبل transaction.

---

# 88. Static gate — random object path on retry

بعد 71:

```text
randomUuidStoragePathPerRetryCount = 0
```

بالنسبة للمسار canonical outbound media.

---

# 89. Static gate — direct create_new_conversation

بعد 71 يجب ألا يبقى production path risky بالشكل:

```text
UI/repository -> direct create_new_conversation RPC -> assume one response = truth
```

العداد:

```text
unsafeDirectCreateConversationCount = 0
```

---

# 90. Static gate — direct Chat send

يجب أن يبقى:

```text
directChatInsertBypassCommandCount = 0
```

كل marketer send عبر v69 command Outbox.

---

# 91. Static gate — Realtime

يجب إعادة تشغيل v70 Realtime verifier أو equivalent semantic checks.

أي direct/transitive Room write = FAIL.

---

# 92. Static gate — historical migrations

بعد 71:

```text
historicalRoomMigrationMutationCount = 0
historicalServerMigrationMutationCount = 0
```

يسمح فقط:

```text
new Room MIGRATION_16_17
new append-only server migration if justified
```

---

# 93. Static gate — UI drift

Session 71 ليست Design System/UI session.

```text
productionUiFilesChanged = 0
```

إلا إذا كان compile-only adapter لا يغير behavior، ويجب توثيقه صراحة.

لا إعادة تصميم ChatScreen.

---

# 94. Files allowed — core/database

متوقع السماح بتعديل:

```text
core/database/.../AutoDriveDatabase.kt
core/database/.../entities/Entities.kt or dedicated new entity files
core/database/.../dao/PendingOperationDao.kt
core/database/.../dao/ChatMessageDao.kt
core/database/.../dao/ConversationDao.kt
```

ومتوقع السماح بإنشاء:

```text
ChatMediaTransferEntity.kt
ChatMediaTransferDao.kt
ChatRecoveryCheckpointEntity.kt
ChatRecoveryCheckpointDao.kt
```

حسب بنية module الحالية.

---

# 95. Files allowed — core/sync

متوقع السماح بتعديل:

```text
LegacyRemotePuller.kt
OutboxSynchronizer.kt
OutboxContracts.kt
IdempotentServerCommandGateway.kt
LocalDataCleaner.kt
```

ومتوقع السماح بإنشاء:

```text
ChatRecoverySynchronizer.kt
```

أو اسم مكافئ واضح.

لا تغير DefaultSyncCoordinator generation semantics بلا سبب.

---

# 96. Files allowed — feature/chat

متوقع السماح بتعديل:

```text
ChatRepositoryImpl.kt
ChatMediaManager.kt
RetryFailedMessagesWorker.kt
ChatFeatureModule.kt إذا احتاج DI
ChatEntityMapper.kt إذا أزيل device-time fallback من recovery mapping
```

ومتوقع السماح بإنشاء:

```text
ChatMediaTransferWorker.kt
ChatMediaTransferProcessor.kt
```

حسب فصل المسؤوليات.

---

# 97. Files allowed — core/network/server

يسمح فقط عند evidence:

```text
chat DTOs / RPC params needed for verified paging or create command
one new append-only Supabase migration
one new SQL contract test file
```

ممنوع أي broad server refactor.

---

# 98. Files allowed — tests/tools

أضف/حدّث:

```text
DatabaseMigrationTest.kt
ChatRepositoryImplTest.kt
new ChatRecovery tests
new MediaTransfer tests
Outbox architecture tests
Room architecture tests
v71 static verifier
v71 model verifier
v71 migration verifier
SQL server tests if applicable
```

---

# 99. Files forbidden by default

ممنوع تعديل:

```text
Design System
Home UI
Balance UI
Commission UI
Notifications UI
Auth UI
Navigation semantics
Verto project
Optimal project
historical migrations
unrelated business rules
```

أي توسع:

```text
BLOCKED_SCOPE_DRIFT
```

---

# 100. ChatRepositoryImpl responsibility

الملف الحالي قريب من responsibility limit الموثق.

Session 71 لا تحشر pagination/transfer engine داخله.

المطلوب:

```text
repository orchestrates/delegates
specialized components own recovery/transfer
```

ويجب بقاء architecture test للresponsibility split خضراء أو تحديثه بعقد أكثر صرامة لا بإزالة الحماية.

---

# 101. No UI pagination scope creep

Session 71 تثبت sync convergence لـ10k.

لا يلزم تحويل Chat UI إلى Paging 3 ما لم يثبت memory/performance test أن ذلك شرط correctness/runtime.

إذا احتاج لاحقًا:

```text
DEFER_UI_PAGING
```

ولا توسع 71 تلقائيًا.

---

# 102. Conversation list snapshot semantics

قائمة `conversations` الحالية snapshot compatibility.

في 71:

```text
absence from snapshot is NOT deletion
```

التحديثات تُupsert/reconcile.

الحذف يبقى عبر tombstone/change feed.

---

# 103. Chat checkpoint atomicity

كل page cursor advance يجب أن يكون داخل نفس transaction التي تطبق rows للصفحة.

```text
apply rows + checkpoint = one commit
```

إذا crash:

```text
both commit
or neither commits
```

---

# 104. Page replay idempotency

إعادة نفس page N مرة يجب أن تؤدي لنفس local logical state.

لا duplicate messages.

لا unread inflation.

لا lastMessage regression.

---

# 105. Same-timestamp boundary test

يجب اختبار أكثر من `pageSize` rows لها نفس `created_at` إن كان server يسمح.

الـtie-breaker `id` يجب أن يمنع:

```text
skip
duplicate loop
infinite page
```

إذا server ordering لا يضمن ذلك:

```text
BLOCKED_CHAT_CURSOR_SEMANTICS
```

---

# 106. Loop termination

الpager يجب أن يملك invariant:

```text
next checkpoint strictly > previous checkpoint
```

إذا page غير فارغة ولا يتقدم checkpoint:

```text
FAIL_PROTOCOL
```

لا loop لا نهائية.

---

# 107. Page size

يجوز اختيار page size ثابت معقول، مثل 100–500، لكن:

```text
page size is performance tuning only
```

وليس completeness boundary.

اختبار 10k يجب أن يمر مع page size صغير بما يكفي لإثبات multi-page drain.

---

# 108. Chat sync invocation

`ChatViewModel.init()` يمكن أن يبقى يطلب `repository.syncMessages(clientId)`.

لكن هذا الاستدعاء يجب أن يمر عبر نفس authoritative recovery component.

لا direct duplicate network apply في repository.

---

# 109. RecentActivity integration

`RecentActivityViewModel` يستخدم `repository.syncMessages(clientId)` أيضًا.

71 يجب الحفاظ على behavior، لكن التنفيذ يصبح unified recovery.

لا تنشئ مسارًا خاصًا للتقارير.

---

# 110. Create conversation UI behavior

`NewChatViewModel` لا يحتاج إعادة تصميم.

عند نجاح local durable transaction:

```text
Result.Success(localConversation)
```

مقبول إذا تم توثيق أن `SYNCED` server confirmation لاحق.

لا تدعِ server commit فورًا.

---

# 111. Local conversation collision

إذا stable conversationId موجود محليًا:

```text
same logical create -> reconcile
changed canonical payload -> conflict
```

ممنوع REPLACE صامت لمحادثة مختلفة بنفس identity.

---

# 112. Server conversation collision

server command إذا وجد نفس conversationId:

يجب إثبات:

```text
same scope + same canonical subject/semantics -> replay/repair receipt
```

وإلا:

```text
CONFLICT
```

لا overwrite.

---

# 113. Conversation finalization

بعد APPLIED create receipt:

```text
validate mutationId/commandType/serverEntityId
```

ثم داخل Room transaction:

```text
revalidate scope
finalize exact local conversation if needed
remove exact create Outbox row
```

لا حذف child message operations.

اختفاؤها من dependency query يجعلها eligible طبيعيًا.

---

# 114. Create receipt serverRevision

حتى لو create receipt يحمل serverRevision من command ledger:

```text
revisionKind MUST remain COMMAND_RECEIPT
```

ولا يستخدم في ChatRecoveryCheckpoint.

---

# 115. Media server command fingerprint

قبل send يجب أن تكون القيم canonical ومستقرة:

```text
messageId
conversationId
type
body
media durable reference
media MIME
media duration
```

نفس mutation retry لا يولد signed URL جديدًا يغير fingerprint.

---

# 116. Media duration

`mediaDurationMs` تُحسب من local staged file مرة واحدة قبل durable enqueue وتُحفظ.

لا يعاد حسابها بطريقة قد تغير payload بعد أول attempt.

---

# 117. LocalPath semantics

`local_path` هو local cache/staging reference فقط.

ممنوع إرساله كserver media reference.

بعد upload/send:

```text
localPath may remain for offline playback
```

وفق storage cleanup policy.

---

# 118. Remote reference resolution

`resolveMediaUrl(localPath, remoteReference)` يجوز أن يفضل local file إن موجودًا.

إذا remoteReference أصبح object key بدل URL:

يجب adapter واضح لإنتاج playable URL دون تغيير domain semantics عشوائيًا.

هذا يتطلب authoritative storage contract قبل التنفيذ.

---

# 119. No raw token in durable diagnostics

إذا signed URLs أو auth-bearing references تحتوي secrets قصيرة العمر:

ممنوع logging الكامل لها.

الـdiagnostics تستخدم:

```text
messageId
transferId
bucket
object key hash/redacted path
error code
```

لا credential leakage.

---

# 120. No raw media in command receipt ledger

v69 receipt ledger يجب ألا يخزن:

```text
file bytes
local paths
raw auth tokens
```

71 لا تغير هذه القاعدة.

---

# 121. Existing push-before-pull invariant

لا تغير ترتيب Sync v67:

```text
recover leases
push Outbox
pull authoritative data
trailing generation if hinted
```

لكن عند وجود media-dependent send غير eligible:

```text
Outbox flush must skip it safely
then pull/recovery continues
```

لا deadlock sync كله.

---

# 122. Media worker vs SyncManager

Media transfer network يمكن أن يُدار عبر WorkManager/processor منفصل.

لا تجعل SyncManager ينفذ file upload داخل main Room transaction.

يمكن Sync trigger worker أو worker trigger sync بعد COMPLETE.

---

# 123. Generation safety

إذا media completion أو conversation create receipt يطلب sync while sync active:

يجب المرور عبر existing generation-safe request mechanism.

لا coroutine bypass جديد.

---

# 124. Pending local guard compatibility

Chat remote recovery يجب ألا يزيل/يدوس local pending intents التي v68/v69 تحميها.

أي merge جديد يراجع active Outbox قبل overwrite server-facing state.

---

# 125. v70 Inbox compatibility

Session 71 لا تغير:

```text
sync_inbox primary key
Inbox event dedupe
DeletionSynchronizer atomicity
v70 Realtime hint-only
```

إذا Room17 migration لمست `sync_inbox` schema:

```text
FAIL_SCOPE
```

إلا migration technical necessity موثقة، وهي غير متوقعة.

---

# 126. v69 command SQL compatibility

لا تغير signatures الحالية لـ:

```text
autodrive_send_chat_message_command_v1
autodrive_mark_chat_read_command_v1
```

إلا إذا evidence يثبت ضرورة breaking change ومع migration versioned جديد.

الأفضل:

```text
preserve existing RPCs
add create conversation RPC separately
```

---

# 127. Shared media schema compatibility

إذا احتاج durable media reference تغيير `internal_messages` schema:

قبل التنفيذ يجب inventory كل consumers/producers المعروفين.

ممنوع:

```text
rename/remove media_url silently
change URL->path semantics invisibly
```

يمكن إضافة field/versioned contract إذا authoritative evidence يدعم.

---

# 128. No full chat wipe

لإصلاح cursor/10k ممنوع:

```text
delete all chat and resync every time
```

initial bootstrap يمكن أن يبدأ من zero checkpoint، لكن ليس full wipe.

---

# 129. No correctness by Realtime

اختبار 10k يجب تشغيله مع:

```text
Realtime disabled/missing
```

ويجب أن يصل آخر message عبر pull recovery وحده.

---

# 130. No correctness by FCM

FCM/notification hints ليست مطلوبة لتقارب Chat.

APP_START/manual/network recovery path يجب أن يكون كافيًا للوصول للتاريخ الجديد.

---

# 131. Test — duplicate page

نفس page مرتين:

```text
message count unchanged
checkpoint unchanged after second identical replay
conversation unread/lastMessage deterministic
```

---

# 132. Test — crash before checkpoint

simulate:

```text
page fetched
Room transaction aborted before commit
```

ثم rerun:

```text
same page reapplied safely
no data loss
```

---

# 133. Test — crash after page commit

simulate:

```text
page transaction commits
process dies before next fetch
```

ثم rerun:

```text
resume from committed checkpoint
```

---

# 134. Test — local pending vs remote same id

وجود local message:

```text
status=PENDING
active SEND_CHAT_MESSAGE
```

ثم remote page تحمل same id.

يجب:

```text
identity validate
no body/media mutation drift
no duplicate
receipt/outbox remains authority for finalization
```

---

# 135. Test — multi-device historical marketer messages

إذا server policy يسمح:

```text
device A sends
server commits
device B has no local row
Realtime disabled
device B recovery pull
```

يجب أن تظهر الرسالة على B.

إذا لا يمكن بسبب RLS:

```text
record blocker, no false PASS
```

---

# 136. Test — read status

remote MARKETER message:

```text
is_read=false -> SENT
later server snapshot/change evidence is_read=true -> READ
```

لا regression reverse.

إذا completeness لتحديث الصف القديم تعتمد على Session72، يوثق ذلك بدل overclaim.

---

# 137. Test — conversation dependency

```text
create C pending
send M to C
flush
```

المتوقع:

```text
CREATE_CHAT_CONVERSATION attempted first
SEND_CHAT_MESSAGE not attempted before parent success
```

حتى لو message row أقدم/أحدث timestamps متقاربة.

---

# 138. Test — media dependency

```text
media transfer pending
SEND_CHAT_MESSAGE outbox exists
flush
```

المتوقع:

```text
SEND_CHAT_MESSAGE skipped
```

بعد COMPLETE:

```text
same Outbox becomes eligible
```

---

# 139. Test — media payload fingerprint stability

بعد transfer completion وقبل first send ثبت payload hash.

بعد ambiguous command timeout/retry:

```text
exact payload hash unchanged
```

وإلا FAIL.

---

# 140. Verification artifacts المطلوبة

71 يجب أن تنتج:

```text
AUTODRIVE_SYNC_VERIFICATION_v71.json
AUTODRIVE_SYNC_VERIFICATION_v71.md
AUTODRIVE_CHAT_RECOVERY_INVENTORY_v71.json
AUTODRIVE_CHAT_MEDIA_TRANSFER_INVENTORY_v71.json
AUTODRIVE_CHAT_CONVERSATION_COMMAND_INVENTORY_v71.json
```

مع SHA-256 لكل artifact.

---

# 141. Verification JSON — baseline

يجب أن يحتوي:

```text
session = 71
baseline archive/SHA
Room before/after
production/test file counts
v70 verdict/handoff
predecessor gate
server chat evidence status
storage reference evidence status
```

---

# 142. Verification JSON — chat recovery counters

على الأقل:

```text
chatTerminalLimit100Count
independentChatRemoteApplyPathCount
chatCheckpointUnscopedCount
chatCursorDeviceClockFallbackCount
chatPageCursorAdvanceOutsideTransactionCount
chatPageScopeMismatchSilentlyAppliedCount
chatPendingLocalOverwriteCount
chatSyntheticEventIdCount
chatSyntheticServerRevisionCount
chatRecovery10kFixtureFailures
```

كلها `0` عند PASS حيث ينطبق.

---

# 143. Verification JSON — media counters

على الأقل:

```text
mediaUploadBeforeDurableIntentCount
randomMediaObjectIdentityRetryCount
mediaTransferUnscopedCount
mediaNetworkInsideRoomTransactionCount
mediaPayloadMutatedAfterFirstAttemptCount
mediaRetryNewMessageIdentityCount
mediaSignedUrlClaimedDurableWithoutEvidenceCount
mediaScopeSwitchApplyCount
```

كلها `0` عند PASS.

---

# 144. Verification JSON — conversation counters

على الأقل:

```text
unsafeDirectCreateConversationCount
createConversationWithoutMutationIdCount
createConversationWithoutDurableReceiptCount
createConversationScopeTrustFromClientCount
createSendDependencyBypassCount
conversationMutationReuseSilentlyAcceptedCount
```

كلها `0` عند PASS.

---

# 145. Diff inventory

التقرير يسجل:

```text
production files touched
test files touched
Room migrations added
server migrations added
historical migrations modified
UI files touched
unexpected production files
new waivers
```

أي unexpected production mutation:

```text
FAIL_STATIC
```

---

# 146. No new waiver rule

Session 71 لا تضيف waiver لإخفاء:

```text
LIMIT 100
non-durable media upload
direct create conversation
scope mismatch
Realtime regression
migration failure
```

```text
newV71WaiverCount = 0
```

---

# 147. Build gate

عند توفر Gradle:

```text
./gradlew --version
./gradlew :app:compileDebugKotlin --console=plain
./gradlew testDebugUnitTest --console=plain
```

وإن كانت module-specific tasks أو المشروع يحتاج task أدق، يسجل الأمر الفعلي.

ممنوع إعلان:

```text
COMPILED=true
UNIT_TESTED=true
```

دون تنفيذ ناجح حقيقي.

---

# 148. Android migration runtime gate

يجب تشغيل:

```text
DatabaseMigrationTest 16→17
```

على instrumentation/device/emulator صالح.

إذا تعذر:

```text
ANDROID_MIGRATION_TESTED=false
```

مع blocker حقيقي.

---

# 149. Server runtime gate

إذا أضيف create conversation RPC/server change:

لا يكفي SQL file.

يجب، لـFULL PASS:

```text
migration deployed/applied in verification environment
contract SQL tests run
same mutation replay tested
cross-scope rejection tested
```

بدون ذلك:

```text
SERVER_CHAT_RUNTIME_VERIFIED=false
```

---

# 150. Storage runtime gate

لـFULL media PASS:

```text
MEDIA_STORAGE_RUNTIME_VERIFIED=true
```

يتطلب اختبار upload/retry/reference semantics ضد policy موثقة.

إذا environment يمنع:

```text
IMPLEMENTED_STATIC_MODEL_BUT_MEDIA_RUNTIME_BLOCKED
```

أو verdict مكافئ صريح.

---

# 151. Inherited regression gates

بعد 71 شغل/حافظ على:

```text
v67 model: 22/22
v68 model fixtures: 36/36
v68 migration model
v69 model: 15/15
v69 command-contract semantic checks
v70 model: 36/36
v70 migration semantic checks
v70 Realtime semantic checks
new v71 static/model/migration checks
```

الـlegacy checks التي تفترض Room 15/16 حرفيًا قد تفشل بنيويًا بعد Room17؛ يوثق سببها ولا تُستخدم لإخفاء semantic regression.

---

# 152. Room version assertion updates

أي architecture test يحتوي حرفيًا:

```text
AUTODRIVE_DATABASE_VERSION = 16
```

يحدث إلى 17 فقط لأنه current version assertion.

لا تحذف الاختبار لتجاوز failure.

---

# 153. Historical Room block hashes

يجب إثبات أن migrations السابقة، خصوصًا:

```text
13→14
14→15
15→16
```

لم تتغير.

71 تضيف فقط:

```text
16→17
```

---

# 154. Server v69 hash preservation

يجب أن يبقى:

```text
20260821203000_autodrive_idempotent_commands_v1.sql
SHA-256 = 6663381c4bf177c7cc22c75fb4c1eee1683290894307ec9ade85e4fe7620c01e
```

إذا تغير:

```text
FAIL_HISTORICAL_SERVER_MIGRATION
```

---

# 155. Required final truth table

التقرير النهائي يسجل صراحة:

```text
IMPLEMENTED
STATIC_VERIFIED
MODEL_VERIFIED
COMPILED
UNIT_TESTED
ANDROID_MIGRATION_TESTED
SERVER_CHAT_RUNTIME_VERIFIED
MEDIA_STORAGE_RUNTIME_VERIFIED
CHAT_10K_VERIFIED
PREDECESSOR_GATE_SATISFIED
```

لا تختصرها في PASS عام غامض.

---

# 156. Verdict ladder

### الحالة A — لا تنفيذ

```text
PLAN_ONLY
```

### الحالة B — تنفيذ صحيح محليًا لكن predecessor blocked

```text
IMPLEMENTED_STATIC_BUT_PREDECESSOR_BLOCKED
```

### الحالة C — server chat evidence ناقص

```text
IMPLEMENTED_PARTIAL_BLOCKED_SERVER_CHAT_CONTRACT
```

### الحالة D — media durable reference غير مثبت

```text
IMPLEMENTED_PARTIAL_BLOCKED_MEDIA_REFERENCE_CONTRACT
```

### الحالة E — build/runtime blocked فقط

```text
IMPLEMENTED_STATIC_MODEL_RUNTIME_BLOCKED
```

### الحالة F — Full PASS

يجوز فقط إذا كل required gates خضراء، including predecessor chain.

---

# 157. Full PASS conditions

```text
predecessorGateSatisfied = true
Room17 migration accepted
chat 10k recovery passes
no terminal LIMIT100 correctness path
single authoritative chat recovery implementation
per-conversation checkpoint scoped/atomic
no device clock cursor
pending local messages protected
durable media queue present
no upload before durable intent
stable transfer/object identity
stable durable media reference contract proven
media retry does not create new message
conversation create idempotent durable command proven
create→send dependency enforced
v69 send/read receipts preserved
v70 Inbox/Reatime semantics preserved
build + unit + migration runtime pass
required server/storage runtime gates pass
newV71WaiverCount = 0
```

---

# 158. ما ليست عليه Session 71

ممنوع سحب:

```text
Unified global change feed                       -> 72
global monotonic server revision                 -> 72
full CURSOR_EXPIRED bootstrap/rebootstrap        -> 72
anti-entropy manifests/digests                   -> 72
cross-entity global transaction groups from feed -> 72
final sync observability dashboard/metrics       -> 73
full fault-injection campaign                    -> 73
dead-letter recovery UX                          -> later/final closure
```

---

# 159. لا تعيد بناء 67–70

71 لا تعيد:

```text
v67 generation coordinator
v67 push-before-pull
v68 scoped Outbox foundation
v68 local mutation atomicity
v69 command receipt ledger
v69 typed retry taxonomy
v69 SEND_CHAT_MESSAGE command
v69 MARK_CHAT_READ command
v70 scoped Inbox
v70 Realtime hint-only
v70 aggregate Realtime health
```

---

# 160. Handoff إلى Session 72

`handoff72Authorized = true` فقط إذا:

```text
predecessorGateSatisfied = true
v71 full acceptance gates pass
server/storage required runtime truth is green
no v67-v70 regression
newV71WaiverCount = 0
```

إذا 71 نُفذت تحت override والسلسلة ما زالت blocked:

```text
handoff72Authorized = false
```

حتى لو implementation المحلي صحيح.

---

# 161. ما يجب أن تستلمه 72

```text
AutoDrive-v71-*.zip
SESSION_71_FINAL.md
AUTODRIVE_SYNC_VERIFICATION_v71.json
AUTODRIVE_SYNC_VERIFICATION_v71.md
AUTODRIVE_CHAT_RECOVERY_INVENTORY_v71.json
AUTODRIVE_CHAT_MEDIA_TRANSFER_INVENTORY_v71.json
AUTODRIVE_CHAT_CONVERSATION_COMMAND_INVENTORY_v71.json
Room17 migration/schema evidence
new server migration/test evidence if created
v71 verifiers/tests
```

---

# 162. ما لا تعيده 72

72 لا تعيد:

```text
10k Chat keyset recovery foundation
durable media upload queue
stable media retry identity
idempotent create-conversation command
create→send dependency
```

بل تبني فوقها:

```text
Unified Change Feed
Global Data Revision
Safe Bootstrap/Rebootstrap
Anti-Entropy
```

وقد تستبدل compatibility chat checkpoint بglobal feed cursor عندما يصبح server contract authoritative.

---

# 163. Implementation order — إلزامي

```text
1. Verify v70 SHA/baseline.
2. Read v70 verification/handoff and record predecessor state.
3. Inventory all Chat network/apply/write/media/create paths again.
4. Obtain authoritative chat/server/storage evidence.
5. Freeze historical Room/server migration hashes.
6. Design one ChatRecoverySynchronizer.
7. Define verified keyset/server cursor semantics.
8. Add Room17 checkpoint/media/dependency schema.
9. Add migration + migration model/test first.
10. Centralize LegacyRemotePuller + ChatRepository sync through one recovery path.
11. Remove terminal ASC LIMIT100 correctness behavior.
12. Implement strict page validation + atomic apply/checkpoint.
13. Implement safe remote message merge and pending-intent guards.
14. Stage outbound media locally without network.
15. Atomically persist message + Outbox + media transfer.
16. Implement durable transfer processor/worker + stable object identity.
17. Prove durable media reference semantics; do not persist expiring signed URL as permanent truth without contract.
18. Gate media SEND_CHAT_MESSAGE until transfer COMPLETE.
19. Inventory/create idempotent conversation server command from authoritative schema.
20. Convert createNewConversation to local Conversation + Outbox.
21. Enforce create→send dependency.
22. Inventory/prove or repair get_or_create_conversation semantics.
23. Extend exact-scope logout cleanup to checkpoints/transfers/files.
24. Run v71 static/model/migration tests twice for determinism.
25. Run inherited semantic regressions.
26. Attempt Gradle compile/unit tests.
27. Run Android migration instrumentation when available.
28. Run server/storage runtime tests when available.
29. Generate inventories + verification JSON/MD.
30. Package only after truth is documented.
```

---

# 164. Pre-implementation questions — يجب الإجابة من evidence لا من التخمين

```text
Q1  ما تعريف public.conversations الحالي؟
Q2  ما body/signature الحالية لـcreate_new_conversation؟
Q3  هل get_or_create_conversation idempotent فعليًا؟ وكيف؟
Q4  هل conversations.id يقبل client-generated UUID؟
Q5  ما ownership columns الفعلية للمحادثة؟
Q6  هل internal_messages.created_at server-owned/non-null/immutable؟
Q7  هل internal_messages.id unique immutable UUID؟
Q8  هل PostgREST path يستطيع keyset predicate (created_at,id) دون skip؟
Q9  هل RLS يسمح بقراءة MARKETER + ADMIN messages لنفس client/org؟
Q10 هل media buckets public أم private؟
Q11 ما المرجع الدائم المعتمد للmedia: URL أم object path أم resolver؟
Q12 هل 7-day signed URL تُخزن حاليًا كحقيقة دائمة؟
Q13 ما upload size/MIME limits server-side؟
Q14 هل storage object overwrite مسموح؟
Q15 كيف يثبت retry أن existing object يخص نفس content؟
Q16 هل new local conversation يمكن أن ترسل message قبل server create حاليًا؟
Q17 هل أي code path آخر ينفذ direct internal_messages insert؟
Q18 هل أي code path آخر ينفذ create_new_conversation direct؟
Q19 هل أي Chat edit/delete path ظهر في source drift؟
Q20 هل Room17 migration تحفظ كل v70 durable state؟
```

أي سؤال حاسم بلا evidence:

```text
BLOCKED_UNPROVEN_CHAT_CONTRACT
```

---

# 165. Acceptance matrix

| Gate | Required result |
|---|---|
| Baseline SHA | exact match |
| Room baseline | 16 |
| Room target | 17 if local durable structures implemented |
| Historical Room migrations | unchanged |
| v69 server migration | unchanged SHA |
| Chat recovery implementations | one authoritative path |
| terminal LIMIT100 | 0 |
| 10k recovery | PASS |
| per-conversation checkpoint | scoped + atomic |
| device clock cursor | 0 |
| pending local overwrite | 0 |
| media upload before durability | 0 |
| transfer identity changes on retry | 0 |
| expiring URL claimed durable without evidence | 0 |
| unsafe direct create conversation | 0 |
| create→send dependency bypass | 0 |
| Realtime direct/transitive Room writes | 0 |
| production UI drift | 0 |
| new waivers | 0 |
| build/unit/runtime claims | truthful only |

---

# 166. Required architectural end-state after 71

```text
                 ┌──────────────────────────┐
                 │ Realtime / FCM / UI Hint │
                 └─────────────┬────────────┘
                               │
                               ▼
                     Generation-safe Sync
                               │
                ┌──────────────┴──────────────┐
                │                             │
                ▼                             ▼
        Scoped transactional Outbox      ChatRecoverySynchronizer
                │                             │
                ▼                             ▼
        Idempotent server commands       keyset/page pull
                │                             │
                ▼                             ▼
        Canonical command receipt        Room atomic page+checkpoint
                │                             │
                └──────────────┬──────────────┘
                               ▼
                              Room
                               │
                               ▼
                               UI
```

ولـmedia:

```text
Local staged media
      ↓
Message + Outbox + Transfer atomic commit
      ↓
Durable transfer worker
      ↓
stable remote object identity/reference
      ↓
Outbox becomes eligible
      ↓
v69 idempotent SEND_CHAT_MESSAGE
```

---

# 167. Invariant — recovery completeness

إذا 10,000 server messages موجودة وRealtime متوقفة، ثم client ينفذ recovery ضمن supported server contract:

```text
all 10,000 must become locally reachable
```

وليس أول 100 فقط.

---

# 168. Invariant — recovery crash safety

```text
message page apply + checkpoint advance
```

إما كلاهما commit أو كلاهما rollback.

---

# 169. Invariant — media intent durability

بعد نجاح user action محليًا لرسالة media:

```text
process death cannot erase the logical send intent
```

حتى لو لم تبدأ الشبكة بعد.

---

# 170. Invariant — media retry identity

```text
retry(media message M) != create(message M2)
```

بل:

```text
retry same M, same transfer, same object identity, same send mutation
```

---

# 171. Invariant — conversation exactly-once logical effect

```text
retry(create conversation C, mutation M) N times
```

ينتج:

```text
one logical conversation C
one canonical receipt M
```

---

# 172. Invariant — dependency ordering

إذا message تعتمد على conversation create:

```text
SEND_CHAT_MESSAGE cannot cross server boundary before CREATE_CHAT_CONVERSATION is APPLIED
```

---

# 173. Invariant — no false global revision

Chat recovery checkpoint في 71 هو compatibility mechanism.

ممنوع تسميته:

```text
global server revision
canonical change-feed cursor
```

إلا بعد Session72 contract.

---

# 174. Invariant — no false media durability

نجاح upload مرة واحدة لا يساوي durable media إذا المرجع ينتهي بعد 7 أيام.

يجب إثبات lifecycle الكامل للمرجع.

---

# 175. Invariant — no old-account completion

worker أو page response بدأ تحت A ثم عاد بعد login B:

```text
must not mutate B Room state
must not advance B checkpoint
must not finalize A operation as B
```

---

# 176. Invariant — Realtime remains acceleration only

تعطيل Realtime:

```text
must not break eventual Chat recovery
```

---

# 177. Deferred truth after 71

حتى لو 71 نفذت بنجاح، يبقى:

```text
72 Unified Change Feed + global revision + safe bootstrap + anti-entropy
73 observability + full fault-injection closure
```

كما يبقى inherited predecessor/server tombstone blocker حتى إغلاقه رسميًا.

---

# 178. الحالة الحالية للعقد

بناءً على v70 المفحوص:

```text
CONTRACT_READY = true
SOURCE_INSPECTED = true
PLAN_MAPPING_RESOLVED = true

RoomBaseline = 16
TerminalChatLimit100Present = true
IndependentChatRemoteApplyPaths = 2
DurableChatRecoveryCheckpointPresent = false
DurableMediaTransferQueuePresent = false
MediaUploadBeforeOutboxCommitPresent = true
RandomMediaObjectPathPresent = true
SevenDaySignedUrlPersistenceRiskPresent = true
DirectCreateNewConversationRpcPresent = true
CreateConversationMutationReceiptPresent = false
CreateSendDependencyPresent = false
RealtimeDirectRoomWrites = 0
RealtimeHintOnly = true

v70FinalVerdict = IMPLEMENTED_STATIC_BUT_PREDECESSOR_BLOCKED
handoff71Authorized = false
predecessorGateSatisfied = false
```

إذًا:

```text
EXECUTION_WITHOUT_OVERRIDE = BLOCKED_PREDECESSOR_HANDOFF
```

كما أن server/storage changes لا يجوز تنفيذها دون authoritative evidence.

---

# 179. الخلاصة النهائية للعقد

Session 71 تنجح فقط إذا تحول Chat من:

```text
bounded snapshot + best-effort media + ambiguous conversation create
```

إلى:

```text
Deterministic paged recovery
+ scoped atomic checkpoint
+ safe remote merge
+ durable media transfer intent
+ stable media retry identity
+ durable media reference contract
+ idempotent conversation creation
+ create→send dependency ordering
```

مع الحفاظ الكامل على:

```text
v67 generation/push-before-pull
v68 transactional scoped Outbox
v69 idempotent command receipts/typed retry
v70 durable Inbox/Realtime hint-only
```

والقاعدة النهائية:

```text
Chat recovery must not depend on receiving the right Realtime event,
media delivery must not depend on surviving one upload attempt,
and conversation creation must not depend on receiving one RPC response.
```

---

# END OF SESSION_71_FINAL.md
