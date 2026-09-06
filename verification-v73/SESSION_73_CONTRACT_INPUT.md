# SESSION_73_FINAL.md

## AutoDrive Sync Modernization — Session 73

### Full Sync Observability + Correlation + Fault-Injection Closure + End-to-End Convergence Proof

**نوع المستند:** عقد تنفيذ مستقل وصارم للجلسة السابعة والأخيرة من مسار تحديث مزامنة AutoDrive المضغوط v67→v73  
**الجلسة:** 73  
**الحالة:** `PLAN ONLY — READY AS CONTRACT; EXECUTION GATED BY v72 HANDOFF / PREDECESSOR CHAIN / RUNTIME EVIDENCE`  
**تاريخ الصياغة:** 2026-08-22  
**مصدر الكود المفحوص:** `AutoDrive-v72-unified-change-feed-bootstrap-anti-entropy.zip`  
**SHA-256 للمصدر المفحوص:** `da46384ddf0941578e4a14a205e25aa825463df32636c00ca640c18b27bd8e27`  
**Archive entries:** `964`  
**Production Kotlin files:** `278`  
**Test Kotlin files:** `46`  
**Room الحالي:** `18`  
**Room المستهدف في 73:** `19` — migration محلية append-only واحدة فقط لسجل observability scoped durable؛ لا يوجد سبب مقبول لتغيير schema أخرى في هذه الجلسة  
**v72 final verdict:** `IMPLEMENTED_STATIC_MODEL_RUNTIME_BLOCKED_PREDECESSOR_OVERRIDDEN`  
**v72 static:** `31/31 PASS` deterministic  
**v72 model:** `21/21 PASS` deterministic  
**v72 migration model:** `11/11 PASS` deterministic  
**v72 COMPILED:** `false`  
**v72 UNIT_TESTED:** `false`  
**v72 ANDROID_MIGRATION_TESTED:** `false`  
**v72 SERVER_CHANGE_FEED_RUNTIME_VERIFIED:** `false`  
**v72 SERVER_BOOTSTRAP_RUNTIME_VERIFIED:** `false`  
**v72 SERVER_ANTI_ENTROPY_RUNTIME_VERIFIED:** `false`  
**v72 predecessorGateSatisfied:** `false`  
**v72 handoff73Authorized:** `false`  
**v72 V67_TOMBSTONE_BLOCKER_SUPERSEDED:** `false`  
**production UI drift in v72:** `0`  
**new v72 waivers:** `0`

---

# 0. الحكم التنفيذي المختصر

Session 73 ليست إضافة Logs أكثر، وليست شاشة Debug، وليست إعادة تنفيذ Session 72.

هي جلسة الإغلاق التي تجعل بروتوكول المزامنة:

```text
observable
+ diagnosable
+ fault-testable
+ convergence-provable
```

المطلوب النهائي:

```text
SYNC REQUEST
  ↓
unique syncRunId
  ↓
exact scoped run context
  ↓
AUTH → PUSH → CHANGE FEED → BOOTSTRAP if needed → RECONCILE → REALTIME restart
  ↓
structured metrics + typed failures + correlation
  ↓
SyncHealthSnapshot
  ↓
Fault Injection Matrix
  ↓
End-to-End Convergence Proof
```

القواعد المطلقة:

```text
Observability MUST NOT become a second correctness authority.
```

```text
Metrics MUST describe truth; UNKNOWN is valid, fabricated freshness is not.
```

```text
Fault injection MUST be deterministic and disabled in production.
```

```text
No PASS from happy-path-only tests.
```

```text
No raw token/OTP/password/Authorization/business payload/financial body in diagnostics.
```

```text
No raw user/client/org identity in remote logs; use a non-reversible diagnostic scope fingerprint.
```

```text
No fault hook may alter production behavior when the injector is NoOp.
```

```text
No predecessor blocker may be hidden behind Session 73 PASS.
```

---

# 1. لماذا Session 73 الحالية تجمع 79 + 80 من الخطة الأصلية

الخطة الأصلية v67→v80 فصلت:

```text
79 = Sync Observability
80 = Sync Fault-Injection Test Suite
```

لكن المسار المضغوط المثبت في عقود 70–72 أصبح:

```text
69 = Idempotent commands
70 = Durable Inbox + Atomic Apply + Realtime Hint-Only
71 = Chat scale/recovery + durable media + conversation ambiguity
72 = Unified Change Feed + Global DATA Revision + Safe Bootstrap + Anti-Entropy
73 = Observability + Full Fault Injection + End-to-End Convergence Proof
```

إذًا:

```text
SESSION_73_SCOPE =
    FULL_SYNC_OBSERVABILITY
  + CORRELATION_CONTEXT
  + DURABLE_SCOPED_HEALTH_STATE
  + PRIVACY_SAFE_DIAGNOSTICS
  + FAULT_INJECTION_SEAMS
  + 20-SCENARIO_FAULT_MATRIX
  + MULTI_DEVICE_CONVERGENCE_PROOF
  + FINAL_REGRESSION_CLOSURE
```

Session 73 لا تعيد بناء:

```text
v67 generation safety
v68 transactional Outbox
v69 idempotent command receipts
v70 Inbox / atomic inbound apply / Realtime hint-only
v71 chat 10k recovery / durable media / conversation create dependency
v72 change feed / DATA revision / bootstrap / anti-entropy
```

إلا إذا ظهر regression أو blocker يمنع الاختبار نفسه.

---

# 2. بوابة البداية — v72 Handoff Gate

قبل أي mutation يجب قراءة:

```text
AUTODRIVE_SYNC_VERIFICATION_v72.json
AUTODRIVE_SYNC_VERIFICATION_v72.md
SESSION_72_FINAL.md
AUTODRIVE_SYNC_CHANGE_FEED_INVENTORY_v72.json
AUTODRIVE_SYNC_BOOTSTRAP_INVENTORY_v72.json
AUTODRIVE_SYNC_ANTI_ENTROPY_INVENTORY_v72.json
```

والتحقق من:

```text
finalVerdict
handoff73Authorized
predecessorGateSatisfied
Room version
static/model/migration results
unifiedFeedImplemented
safeBootstrapImplemented
antiEntropyImplemented
legacyIncrementalAuthorityCount
separateDeletionAuthorityCount
realtimeDirectRoomWriteCount
realtimeTransitiveRoomWriteCount
newV72WaiverCount
runtime flags
```

الحالة الحالية المثبتة:

```text
v72 finalVerdict                         = IMPLEMENTED_STATIC_MODEL_RUNTIME_BLOCKED_PREDECESSOR_OVERRIDDEN
handoff73Authorized                      = false
predecessorGateSatisfied                 = false
Room                                     = 18
STATIC_VERIFIED                          = true
MODEL_VERIFIED                           = true
MIGRATION_MODEL_VERIFIED                 = true
COMPILED                                 = false
UNIT_TESTED                              = false
ANDROID_MIGRATION_TESTED                 = false
SERVER_CHANGE_FEED_RUNTIME_VERIFIED      = false
SERVER_BOOTSTRAP_RUNTIME_VERIFIED        = false
SERVER_ANTI_ENTROPY_RUNTIME_VERIFIED     = false
V67_TOMBSTONE_BLOCKER_SUPERSEDED         = false
legacyIncrementalAuthorityCount          = 0
separateDeletionAuthorityCount           = 0
realtimeDirectRoomWriteCount             = 0
realtimeTransitiveRoomWriteCount         = 0
newV72WaiverCount                        = 0
```

إذًا افتراضيًا:

```text
SESSION_73_EXECUTION_GATE = BLOCKED_PREDECESSOR_HANDOFF
```

يجوز التنفيذ فقط إذا:

```text
A) أُغلقت predecessor chain رسميًا وأصبح handoff73Authorized=true
```

أو:

```text
B) أصدر المستخدم Override صريحًا لتنفيذ 73 فوق السلسلة المحجوبة
```

في B:

```text
IMPLEMENTATION MAY PROCEED
STATIC/MODEL/FAULT evidence MAY be produced
FULL_RELEASE_PASS IS FORBIDDEN
SYNC_MODERNIZATION_CLOSED MUST remain false
```

حتى يغلق blocker/runtime truth فعليًا.

---

# 3. Baseline Gate — هوية v72

قبل أي تعديل يجب تثبيت:

```text
ZIP SHA-256       = da46384ddf0941578e4a14a205e25aa825463df32636c00ca640c18b27bd8e27
archive entries   = 964
production Kotlin = 278
test Kotlin       = 46
Room              = 18
```

أي اختلاف غير موثق:

```text
BLOCKED_INPUT_DRIFT
```

إذا Room ليست 18:

```text
BLOCKED_ROOM_BASELINE_DRIFT
```

إذا `SyncManager` عاد يعتمد `LegacyRemotePuller` أو `DeletionSynchronizer` كauthority steady-state:

```text
BLOCKED_V72_AUTHORITY_REGRESSION
```

إذا Realtime عاد يكتب business state إلى Room:

```text
BLOCKED_REALTIME_REGRESSION
```

---

# 4. Authority Order

عند التعارض، ترتيب السلطة:

1. `AutoDrive-v72-unified-change-feed-bootstrap-anti-entropy.zip` بالـSHA المثبت أعلاه.
2. `AUTODRIVE_SYNC_VERIFICATION_v72.json/.md` لحقيقة ما نُفذ وما بقي runtime-blocked.
3. `SESSION_72_FINAL.md` لحدود 73 وما لا يجوز إعادته.
4. `AUTODRIVE_SYNC_CHANGE_FEED_INVENTORY_v72.json`.
5. `AUTODRIVE_SYNC_BOOTSTRAP_INVENTORY_v72.json`.
6. `AUTODRIVE_SYNC_ANTI_ENTROPY_INVENTORY_v72.json`.
7. `SESSION_71_FINAL.md` لعقد Chat 10k/media/conversation.
8. `SESSION_70_FINAL.md` لعقد Inbox/Realtime hint-only.
9. `SESSION_69_FINAL.md` لعقد command receipts/typed retry.
10. الخطة الأصلية `AUTODRIVE_SYNC_MODERNIZATION_REPAIR_PLAN_v67-v80.md` لبنود Observability/Fault Injection حيث لا تتعارض مع الضغط.
11. authoritative deployed server/runtime evidence عند توفره.
12. الكود الفعلي في v72.
13. هذا العقد.

ممنوع استيراد semantics خاصة بـVerto/Optimal/Max إلى AutoDrive.

---

# 5. بصمات Authority الحرجة قبل التنفيذ

يجب تسجيل:

```text
AUTODRIVE_SYNC_VERIFICATION_v72.json
  712da328b72757b10532df512e8715388caaa7572c891fd33ba318db829cc0a4

AUTODRIVE_SYNC_VERIFICATION_v72.md
  41e145f7e680c2ecce36f34e6f712bafdbbe907e450199d1a5887b57f119fc9c

SESSION_72_FINAL.md
  8590f7b72a1b7e362c9633e2374dfd2b0483eff1c6000f066e651595f87c2617

AUTODRIVE_SYNC_CHANGE_FEED_INVENTORY_v72.json
  d627447f461e2cdf3c6551b496a53600dc036587e4a3a464b5d9e737517a588c

AUTODRIVE_SYNC_BOOTSTRAP_INVENTORY_v72.json
  a951372627670987ec0516853baafd84240c31fe59727367d6e4da43db99a182

AUTODRIVE_SYNC_ANTI_ENTROPY_INVENTORY_v72.json
  b4168f6c500b7d66fe412e06a9c4057ab50704e1577388ff6103ca48b6e8e75f

AppLogger.kt
  6973530b5445d50e4a4927f54723638c03b34896f841113b3f904b6f6f402d13

DiagnosticEvent.kt
  a959cf70c47e61e1e6386863ac4cc78326b9d95761c0e22b46fa0cb06970ff00

SensitiveDataRedactor.kt
  9f3dcd5e12cb7ad5400b86ca24c89783dca85697c4b4cc8b92ce90af0500b855

FirebaseCrashlyticsReporter.kt
  11f65246d13dc3d72e9d5240dd11105a3f5bfa9a523cfe515b4e70ba149ba63f

SyncDiagnostics.kt
  bf8dfa4044bf00e637b9fd7fb1946c9810ff6cf043009c53ba3da8fe0ccca972

DefaultSyncCoordinator.kt
  c153691154ce5662524ffe0981d81f3b5d27d5aa20e146a8d7996a22ee849f5b

SyncManager.kt
  a3d0f607661dd8e376436ae93f485cc2ca694d8ae475678d86e2e30aa31a927c

UnifiedChangeSynchronizer.kt
  5404701f836f84a11be3478703cbea69248a9905288ca95b859c7e3590870812

SafeBootstrapSynchronizer.kt
  d91644e1741fb08eb0ae218e33e74dbabc3fd4e541ece0e784a1026a06e59e53

AntiEntropyReconciler.kt
  dcf905de612ec27f246f96ba42cd2a1da0e5b945c6aceb1c409bd09ad0ff367b

OutboxSynchronizer.kt
  bccfa6a1288d644d6bfb0736174c3840a349e4f927e90cebea96781061e390fd

RealtimeManager.kt
  0b1d95304958668fb7208fa6deaccc905d4f24831aeda16493cecf8b9cd66e56

v72 unified server migration
  eb1b5dcae5ebff8b7f2cac7563d851af50441c3c25e956851407d3bb0fe105e7

v72 server verifier
  830abe134d8a0d2b2529ef67c61f93914e855cd7d5f2467383f06a2354b52ffb
```

أي drift يجب توثيقه قبل mutation.

---

# 6. الحقيقة الحالية — Observability موجودة لكنها غير كافية لإغلاق الخطة

الموجود فعليًا:

```text
core/observability/AppLogger.kt
core/observability/DiagnosticEvent.kt
core/observability/SensitiveDataRedactor.kt
core/observability/FirebaseCrashlyticsReporter.kt
core/sync/diagnostics/SyncDiagnostics.kt
```

`SyncDiagnostics` الحالي يسجل:

```text
syncStarted
phaseFinished
syncFinished
outboxState(pending/inProgress/deadLetter)
realtimeState
```

هذا مفيد، لكنه لا يحقق Session 79 الأصلية كاملة.

---

# 7. الفجوة المثبتة — لا يوجد syncRunId

`DefaultSyncCoordinator.execute()` الحالي يبدأ run بالوقت والسبب فقط:

```text
diagnostics.syncStarted(reason, startedAt)
```

ولا يولد:

```text
syncRunId
```

ولا تمر هوية run إلى:

```text
SyncManager
OutboxSynchronizer
UnifiedChangeSynchronizer
SafeBootstrapSynchronizer
AntiEntropyReconciler
Realtime diagnostics
```

النتيجة:

```text
عدة events يمكن أن تكون صحيحة منفردة لكن لا يمكن ربطها بدورة واحدة دون تخمين.
```

---

# 8. الفجوة المثبتة — head/cursor/lag موجودة في data path لكن لا تُقاس

`UnifiedChangeResult` يحتوي:

```text
cursorRevision
headRevision
hasMore
```

لكن `SyncDiagnostics` لا يستقبلها.

إذًا لا توجد metric موحدة تجيب:

```text
local cursor = ?
server head  = ?
revision lag = ?
```

رغم أن البيانات موجودة أثناء التنفيذ.

---

# 9. الفجوة المثبتة — Outbox metrics ناقصة

الحالي يسجل فقط:

```text
pending_count
in_progress_count
dead_letter_count
```

الخطة تتطلب أيضًا:

```text
oldest outbox age
retry count
conflicts
```

وDAO الحالي لا يملك query موحدة لهذه health snapshot.

---

# 10. الفجوة المثبتة — Bootstrap history غير محفوظ بعد النجاح

`SafeBootstrapSynchronizer` بعد install:

```text
delete staging
delete bootstrap state
```

ويكتب:

```text
sync_reconciliation_state.lastResult = BOOTSTRAP_INSTALLED
```

لكن reconciliation لاحق يمكن أن يستبدل `lastResult`.

لذلك لا توجد durable fact مستقلة ودائمة لـ:

```text
last successful bootstrap
bootstrap count
cursor expiry count
last bootstrap duration
```

---

# 11. الفجوة المثبتة — Reconciliation تحفظ scheduling state لا metrics history

`SyncReconciliationStateEntity` نفسها تصف أنها:

```text
Minimal durable reconciliation scheduling/result state.
Metrics remain a Session 73 concern.
```

الموجود:

```text
lastCheckedRevision
lastResult
nextDueAtLocal
updatedAtLocal
```

المفقود لإغلاق observability:

```text
last successful reconciliation
mismatch count
repair count
rebootstrap escalation count
```

---

# 12. الفجوة المثبتة — لا يوجد dropped/replayed hint accounting

`DefaultSyncCoordinator` يملك generation safety:

```text
requestedGeneration
completedGeneration
```

لكنه لا يثبت metrics لـ:

```text
hint received
hint coalesced
hint replayed by trailing generation
hint dropped
```

Session 73 يجب أن تثبت:

```text
dropped correctness-relevant hints = 0
```

ولا يكفي استنتاج ذلك من عدم وجود crash.

---

# 13. الفجوة المثبتة — لا توجد Fault-Injection Suite كاملة

الموجود اختبارات ومعماريات جزئية، منها:

```text
DefaultSyncCoordinatorTest
SyncStepExecutorTest
PendingOperationProcessorTest
OutboxRetryPolicyTest
RealtimeEventPolicyTest
ChatSyncV71ArchitectureTest
OutboxArchitectureTest
RealtimeArchitectureTest
SyncBoundaryArchitectureTest
v67-v72 static/model verifiers
```

لكن لا يوجد artifact واحد يثبت السيناريوهات العشرين المطلوبة كحملة fault injection متكاملة.

إذًا:

```text
FULL_FAULT_INJECTION_PRESENT = false
```

---

# 14. ما يجب أن تبني عليه Session 73

يجب إعادة استخدام:

```text
v67 generation-safe coordinator
v68 scoped transactional Outbox
v69 stable mutationId + typed retry + command receipt
v70 scoped Inbox + atomic apply + Realtime hint-only
v71 chat 10k recovery + media queue + conversation dependency
v72 global DATA_CHANGE feed + safe bootstrap + anti-entropy
existing AppLogger + SensitiveDataRedactor + CrashlyticsReporter
```

ممنوع إنشاء parallel sync stack للاختبارات.

الاختبارات يجب أن تضرب نفس production components قدر الإمكان عبر fakes/test seams فقط.

---

# 15. القرار المحلي — Room 18→19

Session 73 تحتاج state تشخيصية scoped ودائمة لأن:

```text
last bootstrap facts are otherwise overwritten/lost
cursor expiry count does not exist
last server head is otherwise ephemeral
dropped/replayed hint counters do not exist
last typed failure needs durable diagnostic truth
```

لذلك:

```text
current Room = 18
target Room  = 19
```

بـmigration واحدة فقط:

```text
MIGRATION_18_19
```

ممنوع:

```text
fallbackToDestructiveMigration()
DROP user data
schema jump > 19
historical migration edits
unrelated entity redesign
```

---

# 16. Canonical `sync_observability_state` schema

الحد الأدنى الدلالي:

```text
user_id                         TEXT NOT NULL
client_id                       TEXT NOT NULL
org_id                          TEXT NOT NULL
stream                          TEXT NOT NULL
contract_version                INTEGER NOT NULL
last_sync_run_id                TEXT
last_sync_status                TEXT
last_sync_started_at_local      INTEGER
last_sync_completed_at_local    INTEGER
last_success_at_local           INTEGER
last_failure_phase              TEXT
last_failure_code               TEXT
last_local_cursor_revision      TEXT
last_server_head_revision       TEXT
last_successful_bootstrap_at    INTEGER
last_bootstrap_duration_ms      INTEGER
bootstrap_count                 INTEGER NOT NULL
cursor_expiry_count             INTEGER NOT NULL
last_reconciliation_at          INTEGER
last_reconciliation_result      TEXT
reconciliation_mismatch_count   INTEGER NOT NULL
reconciliation_repair_count     INTEGER NOT NULL
rebootstrap_count               INTEGER NOT NULL
hint_received_count             INTEGER NOT NULL
hint_trailing_run_count         INTEGER NOT NULL
hint_dropped_count              INTEGER NOT NULL
last_realtime_state             TEXT
updated_at_local                INTEGER NOT NULL
```

المفتاح الأساسي:

```text
(user_id, client_id, org_id, stream)
```

هذه table:

```text
OBSERVABILITY ONLY
```

ولا يجوز أن تؤثر على:

```text
whether a mutation is sent
whether a server event is applied
whether a cursor advances
whether a bootstrap installs
whether reconciliation repairs
```

فشل كتابة metric:

```text
MUST NOT corrupt correctness state
```

لكن يجب أن يكون observable كdiagnostic degradation إن أمكن.

---

# 17. ما لا نخزنه في observability table

ممنوع تخزين:

```text
raw Authorization
access token
refresh token
OTP
password
full server payload
full chat message
full invoice/payment body
bank details
raw exception body containing business data
raw media path if user-sensitive
```

لا حاجة لتكرار:

```text
pending_count
in_progress_count
dead_letter_count
oldest outbox age
```

داخل table؛ هذه تُحسب مباشرة من Outbox الحالية لتجنب stale duplicated truth.

---

# 18. `SyncRunContext`

يجب إضافة type صريح، مثال دلالي:

```text
SyncRunContext {
  syncRunId
  reason
  requestedGeneration
  startedAtLocal
  scopeFingerprint
}
```

`syncRunId`:

```text
UUID random
one logical execute() = one syncRunId
non-empty
never reused
not derived from device clock
```

إذا generation جديدة تتطلب دورة engine جديدة:

```text
new syncRunId
```

ولا يعاد استخدام runId للدورة السابقة.

---

# 19. Correlation contract

كل diagnostic event متعلق بالدورة يجب أن يحمل حيث ينطبق:

```text
sync_run_id
phase
reason
scope_fingerprint
```

Outbox operation event يضيف:

```text
mutation_id
operation_type
failure_category
stable_error_code
```

Inbound event diagnostic يضيف عند الحاجة:

```text
event_id
revision
entity_type
operation
```

ممنوع logging لـ:

```text
canonical payload
entity business content
```

الـIDs opaque فقط.

---

# 20. Scope fingerprint

الخطة الأصلية تطلب correlation على user/client/org scope، لكن remote diagnostics لا تحتاج كشف الهوية الخام.

المطلوب:

```text
scope_fingerprint = SHA-256(versioned canonical scope string + app-install diagnostic salt)
```

ويُعرض منه prefix ثابت الطول، مثال:

```text
16-24 hex chars
```

القواعد:

```text
same scope + same install -> stable fingerprint
other scope -> different fingerprint
cannot recover raw IDs from emitted value
raw user_id/client_id/org_id not sent to Crashlytics fields
```

داخل Room تبقى المفاتيح الخام لأنها already local application state.

---

# 21. `DiagnosticEvent` evolution

يجوز توسيع `DiagnosticEvent` بدل إنشاء logger موازٍ.

يجب أن يدعم على الأقل:

```text
level
tag
name/message
fields
throwableType
stackTrace
syncRunId optional
```

لكن لا يجوز أن يصبح stack trace وسيلة لتهريب sensitive messages.

`SensitiveDataRedactor` يجب أن يظل قبل reporter boundary.

---

# 22. SensitiveDataRedactor — متطلبات 73

أضف حماية صريحة لأي keys خام مثل:

```text
user_id
client_id
org_id
email
authorization
access_token
refresh_token
phone
otp
password
secret
api_key
payload
message_body
content
note
amount
balance
commission
invoice_total
bank/account
```

ويجب وجود tests تثبت:

```text
key-based redaction
text-based redaction
nested error message sanitization
no raw scope IDs in emitted Crashlytics event
safe operational metrics remain visible
```

---

# 23. Crashlytics contract

`FirebaseCrashlyticsReporter` الحالي يضع custom keys عالميًا داخل instance.

Session 73 يجب أن تمنع attribution المضلل بين runs المتداخلة أو المتعاقبة.

المطلوب:

```text
كل log event يحمل sync_run_id داخل النص/fields sanitized
custom keys لا تُعامل كledger تاريخي
لا تعتمد correctness أو test PASS على آخر custom key global فقط
```

يمكن إبقاء Crashlytics كsink، لكن source-of-truth التشخيصي المحلي هو:

```text
SyncHealthSnapshot + durable scoped observability state + structured test evidence
```

---

# 24. Metrics المطلوبة — الأصلية + تعريفات دقيقة

Session 73 يجب أن تكشف على الأقل:

```text
local cursor
server head revision
revision lag
oldest outbox age
pending count
retry count
dead letters
conflicts
failed participants
dropped/replayed hints
last successful bootstrap
last reconciliation
cursor expiry count
```

تعريفات 73:

```text
local_cursor_revision
  = current canonical `autodrive-global-change-v1` cursor for exact scope

server_head_revision
  = latest authoritative head observed from canonical feed/manifest for exact scope
  = UNKNOWN if never observed

revision_lag
  = max(server_head_revision - local_cursor_revision, 0)
  = UNKNOWN if either side unknown

oldest_outbox_age_ms
  = now - MIN(created_at) among exact-scope active PENDING/IN_PROGRESS/DEAD_LETTER rows
  = 0 if none

pending_count
  = exact-scope PENDING count

retry_count
  = sum(attempt_count) for exact-scope active rows, plus per-run retry events where needed

dead_letter_count
  = exact-scope DEAD_LETTER count

conflict_count
  = exact-scope active rows whose typed/stable failure category is CONFLICT, or durable conflict event count if normalized separately

failed_participants
  = required Realtime participants not healthy

hint_received_count
  = accepted realtime/manual/network requests relevant to coordinator generation accounting

hint_trailing_run_count
  = hints arriving while run active that cause a later serviced generation

hint_dropped_count
  = correctness-relevant accepted hints that never become serviced generation
  = MUST remain 0 in acceptance suite

last_successful_bootstrap_at
  = durable timestamp of latest completed bootstrap install

last_reconciliation_at
  = durable timestamp of latest completed reconciliation attempt/result

cursor_expiry_count
  = durable count incremented only when canonical feed returns typed CURSOR_EXPIRED
```

---

# 25. Outbox DAO additions

يجوز إضافة read-only aggregate queries:

```text
oldestActiveCreatedAt(scope)
sumActiveAttemptCount(scope)
countByLastErrorCode/category-compatible mapping(scope)
```

ممنوع تعديل send/claim semantics لمجرد metrics.

أي query غير scoped بالكامل:

```text
BLOCKED_CROSS_SCOPE_METRIC_QUERY
```

---

# 26. `SyncHealthSnapshot`

يجب تقديم API typed واحد يجيب عن صحة المزامنة، مثال دلالي:

```text
SyncHealthSnapshot {
  scopeFingerprint
  syncStatus
  currentPhase
  localCursorRevision
  serverHeadRevision
  revisionLag
  pendingCount
  oldestOutboxAgeMs
  retryCount
  deadLetterCount
  conflictCount
  failedRealtimeParticipants
  lastSuccessfulBootstrapAt
  lastReconciliationAt
  lastReconciliationResult
  cursorExpiryCount
  hintReceivedCount
  hintTrailingRunCount
  hintDroppedCount
  lastFailurePhase
  lastFailureCode
  freshness
}
```

الـsnapshot لا يغير state.

---

# 27. Health answers المطلوبة

بعد 73 يجب أن يمكن الإجابة مباشرة دون فتح DB يدويًا:

```text
هل الجهاز متزامن؟
كم متأخر؟
ما العملية العالقة؟
لماذا؟
هل المشكلة Push أم Pull/Change Feed أم Bootstrap أم Reconciliation أم Realtime؟
هل هناك Dead Letter؟
هل هناك conflict؟
هل cursor انتهت صلاحيته سابقًا؟
هل reconciliation اكتشفت divergence؟
```

ممنوع جواب ثنائي كاذب:

```text
SYNCED
```

إذا `serverHeadRevision` غير معروف حديثًا.

الحالة الصحيحة قد تكون:

```text
UNKNOWN_REMOTE_HEAD
```

---

# 28. Freshness truth

كل remote-derived metric يجب أن يحمل freshness/time:

```text
last_server_head_observed_at
last_reconciliation_at
last_realtime_state_at
```

لا يجوز استخدام head قديم على أنه current head بلا إشارة freshness.

---

# 29. Failure taxonomy

يجب توحيد diagnostics حول typed stable codes، لا parsing النص.

الفئات الدنيا:

```text
AUTH
NETWORK
OUTBOX_TRANSIENT
OUTBOX_AMBIGUOUS
OUTBOX_CONFLICT
OUTBOX_VALIDATION
OUTBOX_PERMISSION
OUTBOX_PROTOCOL
CHANGE_FEED_PROTOCOL
CURSOR_EXPIRED
BOOTSTRAP_EXPIRED
BOOTSTRAP_PROTOCOL
RECONCILIATION_MISMATCH
RECONCILIATION_PROTOCOL
REBOOTSTRAP_REQUIRED
REALTIME_DEGRADED
STALE_SCOPE
UNKNOWN
```

`lastFailureCode` يجب أن يكون stable code sanitized.

---

# 30. Phase metrics

كل phase يجب أن تنتج:

```text
sync_run_id
phase
duration_ms
success
failure_code optional
```

الـphases الحالية يجب ألا يعاد تعريف correctness order بسبب observability.

---

# 31. Canonical change-feed metrics

بعد كل feed cycle:

```text
applied_event_count
page_count optional
group_count optional
local_cursor_revision
server_head_revision
revision_lag
has_more
cursor_expired boolean/event
```

لا تسجل payload.

---

# 32. Bootstrap metrics

المطلوب:

```text
bootstrap_started
bootstrap_completed
bootstrap_failed
bootstrap_expired
staged_row_count
page_count
duration_ms
baseline_revision
```

عند success فقط:

```text
last_successful_bootstrap_at = now
bootstrap_count += 1
```

عند CURSOR_EXPIRED:

```text
cursor_expiry_count += 1
```

مرة واحدة لكل typed expiry observation، لا لكل retry الداخلي لنفس response.

---

# 33. Reconciliation metrics

المطلوب:

```text
reconciliation_started
reconciliation_result
mismatch_partition_count
repaired_row_count
rebootstrap_required
last_reconciliation_at
```

إذا clean:

```text
mismatch_count does not increment
```

إذا mismatch حقيقي:

```text
reconciliation_mismatch_count += 1
```

إذا targeted repair نجح:

```text
reconciliation_repair_count += 1
```

إذا escalation إلى bootstrap:

```text
rebootstrap_count += 1
```

---

# 34. Realtime metrics

يجب المحافظة على:

```text
Realtime = HINT ONLY
```

المراقبة تضيف فقط:

```text
participant state
aggregate state
reconnect delay
authorized hint count
trailing generation count
```

ممنوع:

```text
Realtime metric callback -> business Room write
```

---

# 35. Server-side observability — حدود 73

Session 73 لا تحتاج server schema جديدة فقط لإضافة logs.

يجب أولًا استخدام الحقيقة الموجودة من:

```text
change feed head_revision
manifest revision
command receipt mutationId/revisionKind
eventId/data revision
```

إذا أضيف server correlation support فهو:

```text
append-only / backwards-compatible
no change to correctness semantics
no raw auth token logging
```

ولا يجوز تعديل v72 data revision sequence أو feed ordering فقط لخدمة metrics.

---

# 36. لا SLOs مخترعة

الخطة الحالية تطلب القياس والتشخيص، لكنها لا تعطي thresholds تشغيلية رقمية.

لذلك 73:

```text
MUST collect deterministic metrics
MUST NOT invent production SLO threshold and claim it authoritative
```

يمكن إنتاج baseline فقط إذا runtime evidence متاح.

---

# 37. Fault injection architecture

المطلوب test seam واحد واضح، مثال:

```text
interface SyncFaultInjector {
  suspend fun hit(point: SyncFaultPoint, context: FaultContext)
}
```

Production binding:

```text
NoOpSyncFaultInjector
```

Test binding:

```text
DeterministicSyncFaultInjector
```

القواعد:

```text
release path cannot enable faults from remote/user input
fault points are inert under NoOp
fault injector holds no business authority
fault injector receives opaque/minimal context only
```

---

# 38. Fault points — الحد الأدنى

يجب أن توجد seams عند حدود correctness الحرجة فقط، وليس داخل كل سطر:

```text
OUTBOX_AFTER_LOCAL_COMMIT_BEFORE_SEND
OUTBOX_AFTER_SERVER_COMMIT_BEFORE_RESPONSE   // fake/server harness
OUTBOX_BEFORE_FINALIZE_LOCAL_RECEIPT
CHANGE_FEED_AFTER_FETCH_BEFORE_APPLY
CHANGE_GROUP_AFTER_ENTITY_APPLY_BEFORE_CURSOR_COMMIT
CHANGE_PAGE_AFTER_COMMIT_BEFORE_NEXT_FETCH
BOOTSTRAP_AFTER_BEGIN
BOOTSTRAP_AFTER_STAGE_PAGE_COMMIT
BOOTSTRAP_BEFORE_INSTALL_COMMIT
BOOTSTRAP_AFTER_INSTALL_BEFORE_DELTA_RESUME
RECONCILE_AFTER_MANIFEST
RECONCILE_AFTER_TARGETED_REPAIR_BEFORE_RECHECK
COORDINATOR_DURING_PUSH
COORDINATOR_DURING_PULL
LOGOUT_DURING_ACTIVE_SYNC
WORKER_AFTER_LEASE_CLAIM
```

لا يلزم أن تكون الأسماء مطابقة حرفيًا، لكن coverage semantic إلزامي.

---

# 39. ممنوع fault flags في production configuration

ممنوع:

```text
BuildConfig.ENABLE_FAULTS=true in release
remote config can trigger fault
hidden UI can trigger crash hooks in production
server payload can choose fault point
```

أي قابلية كهذه:

```text
BLOCKED_PRODUCTION_FAULT_SURFACE
```

---

# 40. Determinism

كل fault test يجب أن يحدد:

```text
scenarioId
seed if randomness exists
initial server state
initial device state
fault point
fault occurrence count
recovery actions
expected final digest
```

إذا نفس seed ينتج نتائج مختلفة:

```text
FAULT_SUITE_NON_DETERMINISTIC
```

---

# 41. Test harness — server model

يجب إنشاء fake/model server يدعم على الأقل:

```text
idempotent commands by mutationId
COMMAND_RECEIPT revision separate from DATA_CHANGE revision
monotonic DATA revisions with gaps allowed
transaction groups
scoped change feed
cursor expiry
bootstrap snapshot + baseline revision
manifest/partition anti-entropy
retention simulation
network fault injection
response duplication
commit-then-timeout ambiguity
```

ممنوع تبسيط fake بحيث يخالف server contract ثم استخدام PASS منه كدليل.

---

# 42. Test harness — devices

يجب دعم:

```text
Device A local DB/state
Device B local DB/state
same authenticated logical user/scope when scenario requires
separate account/scope when isolation scenario requires
independent cursors/inboxes/outboxes
shared fake server
```

الهدف:

```text
multi-device convergence proof
```

لا يكفي compare object واحد في memory.

---

# 43. Canonical convergence digest

نهاية كل scenario مناسب يجب مقارنة:

```text
server canonical projection digest
Device A canonical Room projection digest
Device B canonical Room projection digest
```

مع استثناء local-only fields نفسها المستخدمة في v72 anti-entropy.

PASS:

```text
logical canonical digests equal
```

أو اختلاف متوقع موثق بسبب pending local intent لم يُحسم بعد.

---

# 44. قاعدة PASS العامة للـFault Suite

لا يكفي نجاح happy path.

PASS فقط إذا ثبت عبر السيناريوهات المناسبة:

```text
no lost writes
no resurrected deletes
no duplicate logical effects
no cross-account leakage
no cursor advancement past uncommitted apply
no stale callback writes after scope switch
pending local intent preserved
Realtime loss does not prevent eventual convergence
retry of ambiguous commit is idempotent
deterministic eventual convergence
```

---

# 45. السيناريو 1 — Process death بعد Room write وقبل commit

الاختبار:

```text
begin local mutation transaction
write entity
inject failure before transaction commit
simulate restart/reopen
```

PASS إذا:

```text
entity mutation rolled back
matching Outbox row rolled back
no half state
```

إذا entity موجودة بلا Outbox أو العكس:

```text
FAIL_ATOMIC_LOCAL_MUTATION
```

---

# 46. السيناريو 2 — Process death / timeout بعد server commit وقبل response

الاختبار:

```text
send mutationId M
server commits effect + receipt
transport fails before client receives response
retry same mutationId M
```

PASS إذا:

```text
one logical server effect
same canonical receipt/reconciliation outcome
Outbox eventually finalizes once
```

ممنوع إنشاء effect ثانٍ.

---

# 47. السيناريو 3 — Network timeout أثناء Push

اختبر:

```text
timeout before server commit
timeout with ambiguous outcome
```

PASS إذا:

```text
operation remains durable
retry classification typed
same mutationId reused
no silent drop
```

---

# 48. السيناريو 4 — Duplicate server response

أعد نفس receipt/result أكثر من مرة.

PASS إذا:

```text
no duplicate finalize effect
no duplicate local business mutation
no corruption of Outbox state
```

---

# 49. السيناريو 5 — Realtime event مفقود

احذف hint كاملًا.

ثم شغل scheduled/manual canonical sync.

PASS إذا:

```text
change feed catches data
final canonical state converges
```

إذا المعلومة لا تصل دون Realtime:

```text
FAIL_REALTIME_AUTHORITY_REGRESSION
```

---

# 50. السيناريو 6 — Realtime event مكرر

أرسل نفس hint عدة مرات.

PASS إذا:

```text
no duplicate business apply
coalescing/generations bounded
Inbox/change feed dedupe remains correct
```

---

# 51. السيناريو 7 — Hint أثناء Pull

أوقف الدورة عند change-feed phase ثم أرسل hint.

PASS إذا:

```text
requestedGeneration increments
current run may finish
trailing generation runs
completedGeneration catches requestedGeneration
hintDroppedCount = 0
```

---

# 52. السيناريو 8 — Hint أثناء Push

نفس اختبار 7 لكن أثناء Outbox push.

PASS بنفس invariants.

---

# 53. السيناريو 9 — Device offline أطول من retention

fake server يجعل cursor أقدم من minimumAvailableRevision.

PASS إذا:

```text
canonical feed returns CURSOR_EXPIRED
cursorExpiryCount increments
incremental apply stops
safe bootstrap installs snapshot
pending local intent preserved
delta resumes after baseline
no resurrected delete
```

---

# 54. السيناريو 10 — Cursor page يعاد مرتين

أعد نفس page/events.

PASS إذا:

```text
Inbox dedupe prevents second logical effect
cursor remains monotonic
transaction groups remain atomic
```

---

# 55. السيناريو 11 — Logout أثناء active sync

أوقف sync في push أو pull ثم نفذ logout barrier.

PASS إذا:

```text
new work blocked
active owner quiesces/cancels safely
departing scope is cleared per policy
stale callback cannot write after switch
```

---

# 56. السيناريو 12 — Login بحساب ثانٍ فور logout

التسلسل:

```text
A active
logout A
login B immediately
late callback from A arrives
```

PASS إذا:

```text
B never sees A Outbox/Inbox/Cursor/bootstrap/reconciliation/observability state
A late callback fails closed
```

---

# 57. السيناريو 13 — نفس المستخدم على جهازين يعدلان نفس Entity

Device A وB ينشئان mutations مستقلة.

يجب استخدام server conflict/ordering contract الفعلي.

PASS إذا:

```text
no duplicate effect
no permanent split-brain after all retries/change-feed/reconcile
both devices converge to authoritative server result
```

إذا conflict policy غير محددة authoritative:

```text
BLOCKED_CONFLICT_POLICY_RUNTIME_TRUTH
```

ولا يجوز اختراع winner semantics داخل test.

---

# 58. السيناريو 14 — 10k chat messages

يجب إعادة تشغيل proof v71 على 10,000+ رسالة.

اختبر:

```text
Realtime disabled
multiple recovery pages
app restart between pages
new messages while recovering
media metadata present on subset
```

PASS إذا:

```text
no ASC LIMIT100 terminal correctness
no missing tail
deterministic order
final conversation state converges
```

`CHAT_10K_VERIFIED` يجب أن يبقى true في v73 evidence.

---

# 59. السيناريو 15 — Primary-key reconciliation after timeout

اختبر command أنشأ/ثبت server entity id لكن response ضاع.

PASS إذا:

```text
retry/reconciliation resolves same server entity
local mapping finalizes once
child dependencies remain valid
```

خصوصًا conversation create→send.

---

# 60. السيناريو 16 — Dead Letter recovery

اختر failure terminal typed.

PASS إذا:

```text
DEAD_LETTER reason visible and typed
reactivate/retry path keeps same logical mutation identity when policy permits
successful recovery does not duplicate effect
```

إذا operation غير قابلة لإعادة المحاولة policy-wise:

```text
discard/reconcile path must be explicit in evidence
```

لا يجوز reset عشوائي للهوية.

---

# 61. السيناريو 17 — Invoice/Payment transaction group

server يرسل Invoice + Payment في group واحدة.

inject failure بعد apply أول entity وقبل cursor commit.

PASS إذا:

```text
Room transaction rolls back entire group
replay applies both together
no half-visible state
cursor not advanced past partial group
```

---

# 62. السيناريو 18 — Server revision gap

server revisions مثال:

```text
100, 103, 109
```

PASS إذا:

```text
client accepts monotonic gaps
no contiguous +1 assumption
cursor advances according to valid page contract
```

---

# 63. السيناريو 19 — Bootstrap مع Pending local mutations

قبل bootstrap أنشئ active Outbox intent.

PASS إذا:

```text
bootstrap does not overwrite/delete pending local intent
canonical install preserves local pending state policy
subsequent push/reconcile converges
```

---

# 64. السيناريو 20 — App restart أثناء lease

claim operation ثم simulate process death/restart قبل completion.

PASS إذا:

```text
lease remains durable
operation not sent concurrently before expiry
expired lease recovers
same mutationId retried
no duplicate logical effect
```

---

# 65. Fault scenario evidence artifact

يجب إنتاج machine-readable:

```text
AUTODRIVE_SYNC_FAULT_MATRIX_v73.json
```

لكل scenario:

```text
id
name
seed
faultPoint
preconditions
executed
passed
assertions
finalServerDigest
finalDeviceADigest
finalDeviceBDigest if applicable
failureCode if failed
runtimeClass: UNIT|MODEL|ANDROID|SERVER_LIVE
```

ممنوع تحويل NOT_RUN إلى PASS.

---

# 66. End-to-End Convergence Proof artifact

يجب إنتاج:

```text
AUTODRIVE_SYNC_CONVERGENCE_PROOF_v73.json
```

ويحتوي على الأقل:

```text
scenarioCount
multiDeviceScenarioCount
crossScopeScenarioCount
allRequiredScenariosExecuted
noLostWrites
noResurrectedDeletes
noDuplicateEffects
noCrossAccountLeakage
deterministicEventualConvergence
serverRuntimeIncluded
androidRuntimeIncluded
```

إذا server/android runtime غير متاح:

```text
serverRuntimeIncluded=false
androidRuntimeIncluded=false
```

ولا يعلن full closure.

---

# 67. Observability inventory artifact

يجب إنتاج:

```text
AUTODRIVE_SYNC_OBSERVABILITY_INVENTORY_v73.json
```

يتضمن لكل metric:

```text
name
source
scope
persistence
freshness
emission points
privacy classification
runtime verified?
```

ويغطي كل metrics الأصلية المطلوبة.

---

# 68. Correlation inventory

داخل observability inventory يجب إثبات coverage لـ:

```text
syncRunId
mutationId
eventId
scopeFingerprint
```

وأنها تصل للأحداث ذات الصلة دون raw payloads.

---

# 69. Runtime observability verifier

إذا يمكن تشغيل التطبيق/اختبارات instrumented:

يجب تنفيذ دورة فيها:

```text
one success
one retryable outbox failure
one duplicate hint
one CURSOR_EXPIRED/bootstrap path
one reconciliation mismatch/repair
one Realtime degraded participant
```

ثم التحقق أن `SyncHealthSnapshot` يصف النتائج بدقة.

---

# 70. Server runtime gate

v72 لم يثبت runtime للسيرفر.

لذلك Session 73 لا يجوز أن تعتبر fake server proof بديلًا عن deployed verification.

FULL closure تحتاج authoritative target يثبت على الأقل:

```text
change feed RPC live
bootstrap RPC live
manifest/partition RPC live
RLS/scope isolation live
retention/CURSOR_EXPIRED behavior live
idempotent command receipt behavior live
```

إذا غير متاح:

```text
SERVER_RUNTIME_VERIFIED=false
```

---

# 71. Android runtime gate

يجب محاولة:

```text
compile
unit tests
Room migration 18→19 instrumentation
fault instrumentation where applicable
```

إذا Gradle bootstrap ما زال يفشل بـnetwork:

```text
COMPILED=false
UNIT_TESTED=false
ANDROID_MIGRATION_TESTED=false
ANDROID_FAULT_RUNTIME_TESTED=false
```

ولا تستبدلها static proof.

---

# 72. Migration 18→19 contract

`MIGRATION_18_19` يجب أن:

```text
CREATE sync_observability_state
preserve all existing tables/data
add scoped PK/indexes only as needed
initialize counters to 0
not fabricate historical success timestamps
```

أي historical metrics غير معروفة عند migration:

```text
NULL / UNKNOWN
```

ممنوع تخمينها من current time.

---

# 73. Logout observability cleanup

عند logout exact scope:

```text
sync_observability_state for departing scope must be deleted/cleared with other scoped state per policy
```

لكن عملية cleanup نفسها لا يجوز أن تمس scope آخر.

يجب إضافة cross-account test.

---

# 74. Metrics failure isolation

إذا writing observability state يفشل:

```text
correctness transaction MUST NOT be widened to include nonessential metric write if that can cause business rollback solely due telemetry
```

القاعدة:

```text
correctness first
telemetry second
```

استثناء: metric مدمجة أصلًا في نفس durable state لحقيقة correctness لا تُفصل عشوائيًا.

---

# 75. No observability-induced network dependency

إذا Crashlytics/network telemetry غير متاحة:

```text
sync correctness must continue
```

Reporter failure يجب أن يبقى fail-safe كما `AppLogger` الحالي.

---

# 76. No raw exception leakage

`AppLogger` الحالي يستخدم sanitized throwable wrapper.

Session 73 يجب أن تحافظ على:

```text
throwable type/stack allowed
raw throwable message not forwarded
```

وتضيف regression test على كل new diagnostic path.

---

# 77. Realtime participant truth

`failedParticipants` يجب أن يعتمد required participant set الفعلي، لا أول subscriber.

إذا participant واحد down:

```text
aggregate must not say healthy/CONNECTED if v70 contract says DEGRADED
```

الـhealth snapshot يجب أن يعكس ذلك.

---

# 78. `hintDroppedCount` semantics

لا يجوز زيادة dropped بسبب deliberate coalescing الصحيح.

التفريق:

```text
coalesced hint = accepted and represented by serviced generation
true dropped hint = accepted request with generation never serviced and no cancellation/logout policy explanation
```

الهدف النهائي:

```text
true dropped hints = 0
```

---

# 79. Cancellation semantics

Logout/cancellation قد يمنع generation من الإكمال intentionally.

هذا لا يسجل dropped correctness hint إذا scope itself was invalidated.

يجب أن تكون reason typed:

```text
CANCELLED_SCOPE_INVALIDATED
```

ولا تُحسب false positive.

---

# 80. Observability does not redefine SyncStatus

ممنوع تغيير SUCCESS/PARTIAL_SUCCESS/FAILED semantics لمجرد dashboard.

إذا احتاج health state richer truth، أضف:

```text
SyncHealthState
```

مستقلة عن public `SyncStatus` بدل كسر contract القديم.

---

# 81. Suggested health states

دلاليًا يجوز:

```text
HEALTHY
LAGGING
OUTBOX_BACKLOG
DEAD_LETTER
CONFLICT
REMOTE_HEAD_UNKNOWN
REALTIME_DEGRADED
BOOTSTRAP_REQUIRED
RECONCILIATION_REQUIRED
FAILED
```

لكن التسمية النهائية من code style؛ semantics أهم.

---

# 82. Current UI policy

Session 73 ليست UI redesign.

ممنوع تعديل production UI إلا إذا هناك شاشة diagnostics موجودة أصلًا وتحتاج wiring صغير موثق.

Default:

```text
productionUiFilesChanged = 0
```

أي UI drift غير مطلوب:

```text
BLOCKED_SCOPE_DRIFT
```

---

# 83. Static verifier v73

أنشئ:

```text
scripts/verify-v73-static.py
```

ويتحقق على الأقل من:

```text
Room=19
MIGRATION_18_19 exists
historical migrations unchanged
sync_observability_state exact scope
SyncRunContext exists
syncRunId generated per run
required metrics present
SensitiveDataRedactor covers raw scope keys
fault injector production binding is NoOp
no production fault enable switch
Realtime direct/transitive Room writes remain 0
legacy incremental authority remains 0
separate deletion authority remains 0
v72 change feed/bootstrap/anti-entropy files remain wired
no raw Log.* outside allowed logger policy
no new waiver
```

---

# 84. Static verifier determinism

شغّل v73 static verifier مرتين.

مطلوب:

```text
same assertion count
same PASS/FAIL order
same result JSON
```

باستثناء timestamps الممنوعة أصلًا داخل deterministic result.

---

# 85. Model verifier v73

أنشئ:

```text
scripts/verify-v73-model.py
```

ويغطي على الأقل:

```text
correlation propagation model
metric derivation model
scope fingerprint stability/separation
cursor lag with gaps
unknown remote head semantics
hint generation accounting
bootstrap counters
reconciliation counters
fault matrix deterministic runner
multi-device convergence model
cross-account isolation model
```

---

# 86. Migration verifier v73

أنشئ:

```text
scripts/verify-v73-migration.py
```

ويثبت:

```text
18→19 only
new table schema exact
existing v13→18 migration hashes unchanged
no destructive fallback
no historical table mutation outside intended migration
```

---

# 87. Fault matrix runner

يجوز أن يكون:

```text
JUnit/Kotlin tests
+ deterministic model runner
```

لكن artifact النهائي يجب أن يربط كل scenario بنتيجته.

إذا بعض السيناريوهات لا يمكن إثباتها إلا Android/runtime:

```text
MODEL_PASS != ANDROID_PASS
```

ويجب فصل flags.

---

# 88. Required Android instrumentation focus

الأعلى أولوية instrumentation:

```text
Room rollback under injected exception
MIGRATION_18_19
logout/scope persistence cleanup
lease persistence/recovery
bootstrap staging/install crash boundaries
transaction-group atomic apply
```

لأن model وحده لا يثبت behavior الفعلي لـRoom/Android process lifecycle.

---

# 89. Required server live focus

الأعلى أولوية live server:

```text
timeout-after-commit idempotency
revision gaps
cursor expiry/retention
transaction group pagination
bootstrap baseline no-gap
manifest mismatch/repair scope
RLS cross-account isolation
```

---

# 90. Regression verifiers — إلزامي

بعد v73 يجب إعادة تشغيل ما أمكن من:

```text
v67 model/static
v68 model/static
v69 model/static
v70 model/static
v71 model/static/migration
v72 static/model/migration
```

الverifiers القديمة التي hardcode Room version قد تفشل لأسباب superseded فقط.

يجب تصنيف كل failure:

```text
GENUINE_REGRESSION
or
OBSOLETE_BASELINE_ASSERTION
```

ولا يجوز تجاهله بصمت.

---

# 91. v72 regression invariants

يجب إثبات بقاء:

```text
canonical stream = autodrive-global-change-v1
DATA_CHANGE revision distinct from COMMAND_RECEIPT
revision gaps allowed
transaction group atomicity
cursor + Inbox + apply atomicity
safe bootstrap
pending-local preservation
anti-entropy targeted repair first
no default full wipe
legacy incremental authority count = 0
separate deletion authority count = 0
```

---

# 92. v71 regression invariants

يجب إثبات:

```text
CHAT_10K_VERIFIED=true
durable media retry identity
create conversation idempotency
create→send dependency
no media retry duplicate message
```

---

# 93. v70 regression invariants

يجب إثبات:

```text
scoped Inbox
duplicate replay safe
Realtime hint-only
oldRecord not required for DELETE
aggregate Realtime health truthful
zero direct/transitive Realtime business writes
```

---

# 94. v69 regression invariants

يجب إثبات:

```text
stable mutationId
idempotent retry
COMMAND_RECEIPT revision kind
no command receipt cursor contamination
typed retry taxonomy
```

---

# 95. v68 regression invariants

يجب إثبات:

```text
Entity + Outbox atomic local write
exact scope Outbox
leaseUntil separate from nextRetryAt
logout cannot replay other account operation
```

---

# 96. v67 regression invariants

يجب إثبات:

```text
requestedGeneration/completedGeneration safety
push-before-pull ordering
trailing generation on concurrent hint
no unbounded loop
```

ومع ذلك inherited tombstone server blocker لا يُغلق إلا evidence رسمي.

---

# 97. Build gate

يجب محاولة:

```text
./gradlew :app:compileDebugKotlin
```

أو equivalent مناسب للمشروع.

إذا wrapper distribution غير متاح:

```text
COMPILED=false
BUILD_BLOCKER=environment/network
```

لا تعدل Gradle wrapper history كshortcut غير متعلق بالجلسة.

---

# 98. Unit test gate

شغّل suites الجديدة + sync regressions.

مطلوب truth منفصل:

```text
UNIT_TESTED=true/false
UNIT_TESTS_PASSED=true/false
```

إذا لم يبدأ Gradle أصلًا:

```text
UNIT_TESTED=false
```

وليس FAIL منطقي للكود.

---

# 99. Android migration gate

مطلوب:

```text
MIGRATION_18_19 actual Room instrumentation
```

الـPython migration model لا يساوي Android migration runtime.

---

# 100. Fault execution classes

كل scenario يصنف:

```text
STATIC
MODEL
UNIT
ANDROID_RUNTIME
SERVER_LIVE
END_TO_END
```

Session 73 full closure تتطلب مستوى proof المناسب لكل invariant، لا أقل مستوى متاح فقط.

---

# 101. Verdict taxonomy

التقرير النهائي يجب أن يستخدم verdict صريحًا، مثل:

```text
IMPLEMENTED_STATIC_MODEL_RUNTIME_BLOCKED_PREDECESSOR_OVERRIDDEN
IMPLEMENTED_STATIC_MODEL_FAULT_PASS_RUNTIME_BLOCKED
IMPLEMENTED_RUNTIME_PARTIAL_SERVER_BLOCKED
FULL_PASS
```

`FULL_PASS` ممنوع إذا أي من التالي false:

```text
predecessorGateSatisfied
COMPILED
UNIT_TESTED + passed
ANDROID_MIGRATION_TESTED + passed
required fault scenarios executed at required levels
SERVER_RUNTIME_VERIFIED
NO_CRITICAL_BLOCKER
NO_NEW_WAIVER
END_TO_END_CONVERGENCE_VERIFIED
```

---

# 102. `SYNC_MODERNIZATION_CLOSED`

بما أن 73 آخر الجلسات المضغوطة، لا يوجد handoff74 في هذا المسار.

بدلًا منه:

```text
SYNC_MODERNIZATION_CLOSED=true
```

فقط إذا:

```text
predecessorGateSatisfied=true
v72 runtime server contract verified=true
v73 observability acceptance=true
all 20 fault scenarios executed/covered at required proof levels=true
no lost writes=true
no resurrected deletes=true
no duplicate effects=true
no cross-account leakage=true
deterministic eventual convergence=true
compile/unit/migration runtime gates=true
newV73WaiverCount=0
criticalBlockerCount=0
```

وإلا:

```text
SYNC_MODERNIZATION_CLOSED=false
```

---

# 103. No blocker laundering

إذا 73 نفذت فوق override الحالي:

```text
predecessorGateSatisfied=false
```

إذًا حتى لو:

```text
static/model/fault harness = PASS
```

يبقى:

```text
FULL_PASS = forbidden
SYNC_MODERNIZATION_CLOSED = false
```

---

# 104. Server blocker supersession rule

v73 لا تقول إن v67 tombstone blocker superseded فقط لأن v72 source migration موجودة.

Supersession تحتاج runtime evidence أن canonical change feed:

```text
covers DELETE for all in-scope entities
is deployed
is RLS-correct
is retention-aware
is consumed by client
passes CURSOR_EXPIRED recovery
```

عندها فقط يمكن تسجيل:

```text
V67_TOMBSTONE_BLOCKER_SUPERSEDED=true
```

---

# 105. Production security gate

ابحث في source/artifacts عن:

```text
service_role
JWT secret
access token
refresh token
password
OTP
raw push token
bank/account dump
raw chat payload
raw invoice/payment payload
```

أي secret حقيقي:

```text
BLOCKED_SECRET_LEAK
```

---

# 106. Generated junk policy

ممنوع التغليف النهائي مع:

```text
.gradle/
build/
IDE caches
local.properties real secrets
keystores
runtime DB copies with user data
Supabase credentials
test crash dumps containing raw user data
```

---

# 107. Allowed production changes — المبدأ

التعديل الإنتاجي يجب أن يتركز في:

```text
core/observability
core/sync/diagnostics
core/sync coordinator/manager seams needed for correlation
read-only DAO metrics
Room observability entity/DAO/migration
minimal fault injection interface with NoOp production binding
minimal hooks at correctness boundaries
logout cleanup for new scoped observability row
```

ويمنع الانتشار غير المبرر إلى feature UI.

---

# 108. Expected new production files

أسماء تقريبية مقبولة:

```text
core/observability/.../DiagnosticCorrelation.kt
core/sync/.../diagnostics/SyncHealthSnapshot.kt
core/sync/.../diagnostics/SyncObservabilityStore.kt
core/sync/.../fault/SyncFaultInjector.kt
core/database/.../entities/SyncObservabilityStateEntity.kt
core/database/.../dao/SyncObservabilityDao.kt
```

التسمية يمكن أن تختلف، semantics لا.

---

# 109. Expected modified production files

المتوقع غالبًا:

```text
AutoDriveDatabase.kt
LocalDataCleaner.kt
DefaultSyncCoordinator.kt
SyncManager.kt
SyncDiagnostics.kt
OutboxSynchronizer.kt / PendingOperationProcessor.kt
UnifiedChangeSynchronizer.kt
SafeBootstrapSynchronizer.kt
AntiEntropyReconciler.kt
RealtimeManager.kt
AppLogger.kt
DiagnosticEvent.kt
SensitiveDataRedactor.kt
DI modules
PendingOperationDao.kt
```

أي ملف إضافي يجب تفسيره في diff inventory.

---

# 110. Prohibited scope drift

ممنوع في 73:

```text
redesign invoices
redesign commission
new chat feature
new navigation
new auth flow
new business rules
new server conflict policy without authority
change DATA revision semantics
change transaction group semantics
replace Room as local source of truth
replace Realtime hint-only model
```

---

# 111. Implementation order — إلزامي

```text
1. Verify v72 ZIP SHA and baseline counts.
2. Read v72 verification + handoff + inventories.
3. Record predecessor/runtime blocker truth.
4. Freeze critical file/migration hashes.
5. Inventory all current diagnostics emission points.
6. Map every required metric to an authoritative source.
7. Define SyncRunContext + scope fingerprint privacy contract.
8. Define sync_observability_state schema and MIGRATION_18_19.
9. Add migration model/test first.
10. Add read-only Outbox metric queries.
11. Extend SyncDiagnostics with typed correlation/metrics.
12. Wire coordinator runId once per logical execute.
13. Wire SyncManager/change feed/bootstrap/reconcile/outbox/realtime metrics.
14. Extend redaction tests before enabling new remote fields.
15. Build SyncHealthSnapshot API.
16. Add NoOp production fault injector + deterministic test injector.
17. Add minimal fault points at correctness boundaries.
18. Build deterministic fake server/device harness.
19. Implement all 20 fault scenarios.
20. Add two-device convergence proof.
21. Add cross-account isolation proof.
22. Re-run 10k chat proof.
23. Produce observability/fault/convergence inventories.
24. Run v73 static verifier twice.
25. Run v73 model verifier twice.
26. Run v73 migration verifier twice.
27. Run inherited v67-v72 regressions.
28. Attempt Gradle compile/unit tests.
29. Attempt Android migration/fault instrumentation.
30. Run authoritative server live suite if access exists.
31. Generate verification JSON/MD with truthful flags.
32. Package clean source-of-truth ZIP.
33. Extract final ZIP fresh and rerun deterministic verifiers.
34. Generate SHA-256 sidecars.
```

ممنوع القفز إلى fault PASS قبل 5–17.

---

# 112. Pre-implementation questions — تُجاب من evidence لا التخمين

المنفذ يجب أن يسجل إجابات فعلية:

```text
Q1  هل baseline SHA مطابق؟
Q2  هل Room=18؟
Q3  ما كل diagnostic emission points الحالية؟
Q4  أين تُولد كل sync request/generation؟
Q5  ما المصدر authoritative لكل metric؟
Q6  كيف سنحفظ last bootstrap/cursor expiry دون خلط scopes؟
Q7  هل raw user/client/org يظهر الآن في أي logger field/message؟
Q8  هل Crashlytics enabled في build target المستخدم؟
Q9  كيف نميز UNKNOWN head من head=0؟
Q10 كيف نحسب retry/conflict دون parsing message text؟
Q11 ما required Realtime participant set؟
Q12 كيف نثبت coalesced hint مقابل truly dropped hint؟
Q13 ما fault points التي تتطلب Android instrumentation؟
Q14 ما scenarios التي تتطلب live server بدل fake؟
Q15 ما server conflict policy الفعلية للسيناريو multi-device؟
Q16 هل runtime Supabase target متاح؟
Q17 هل Gradle network blocker مغلق؟
Q18 هل Room 18→19 test assets/schemas متاحة؟
Q19 هل historical migration hashes ثابتة؟
Q20 هل أي new waiver مطلوب؟
```

أي سؤال مؤثر بلا evidence:

```text
BLOCKED_UNRESOLVED_PROTOCOL_DECISION
```

---

# 113. Acceptance matrix — Observability

PASS فقط إذا:

```text
[ ] unique syncRunId per run
[ ] correlation reaches phases
[ ] mutationId correlation on Outbox events
[ ] eventId/revision correlation on inbound events where emitted
[ ] privacy-safe scope fingerprint
[ ] no raw scope IDs in remote diagnostics
[ ] local cursor metric
[ ] server head metric with freshness
[ ] revision lag metric
[ ] oldest outbox age
[ ] pending count
[ ] retry count
[ ] dead letter count
[ ] conflict count
[ ] failed Realtime participants
[ ] hint received/trailing/dropped accounting
[ ] last successful bootstrap
[ ] last reconciliation
[ ] cursor expiry count
[ ] typed last failure
[ ] one SyncHealthSnapshot API
[ ] observability failure cannot break correctness
```

---

# 114. Acceptance matrix — Privacy

```text
[ ] token redaction
[ ] Authorization redaction
[ ] OTP/password/secret redaction
[ ] phone/account/bank redaction
[ ] amount/balance/commission redaction
[ ] payload/content/note redaction
[ ] raw user/client/org redaction in remote events
[ ] raw throwable message not forwarded
[ ] safe metrics remain visible
[ ] no secrets in generated JSON/MD/log artifacts
```

---

# 115. Acceptance matrix — Fault Injection

```text
[ ] all 20 scenarios represented
[ ] deterministic seeds/results
[ ] no production-enabled fault switch
[ ] no lost writes
[ ] no resurrected deletes
[ ] no duplicate effects
[ ] no cross-account leakage
[ ] group atomicity under injected failure
[ ] cursor atomicity under injected failure
[ ] idempotent retry after commit ambiguity
[ ] Realtime loss harmless to eventual convergence
[ ] CURSOR_EXPIRED bootstrap safe
[ ] pending local intent preserved
[ ] lease recovery safe
[ ] 10k chat proof retained
[ ] revision gaps accepted
[ ] multi-device convergence proven
```

---

# 116. Acceptance matrix — Runtime

```text
[ ] compile executed and passed
[ ] unit tests executed and passed
[ ] Room 18→19 Android migration executed and passed
[ ] Android fault tests executed where required
[ ] server live change-feed tests passed
[ ] server live bootstrap tests passed
[ ] server live anti-entropy tests passed
[ ] server live idempotency tests passed
[ ] live scope isolation passed
[ ] no critical runtime blocker
```

إذا أي منها NOT_RUN:

```text
FULL_PASS=false
```

---

# 117. Acceptance matrix — Regression

```text
[ ] v67 generation safety
[ ] v68 atomic/scoped Outbox
[ ] v69 idempotent command receipts
[ ] v70 Inbox atomicity
[ ] v70 Realtime hint-only
[ ] v71 chat 10k
[ ] v71 durable media
[ ] v71 conversation create→send dependency
[ ] v72 unified change feed
[ ] v72 safe bootstrap
[ ] v72 anti-entropy
[ ] logout exact-scope isolation
[ ] zero unexpected UI drift
[ ] zero historical migration mutation
[ ] zero new waivers
```

---

# 118. Expected architecture after 73

```text
LOCAL MUTATION
  ↓
Room transaction(Entity + scoped Outbox)
  ↓
Idempotent Server Command
  ↓
COMMAND_RECEIPT
  ↓
Server business mutation
  ↓ same server transaction
DATA_CHANGE event
  ↓
Unified scoped Change Feed
  ↓
Durable global cursor
  ↓
Inbox + transaction-group atomic apply
  ↓
Room Source of Truth
  ↓
UI

Realtime = Hint Only
Bootstrap = Safe Cursor Recovery
Anti-Entropy = Divergence Detection + Targeted Repair

Around the protocol:
  SyncRunContext
  ↓
  structured privacy-safe diagnostics
  ↓
  durable scoped observability state
  ↓
  SyncHealthSnapshot

Validation:
  Deterministic Fault Injector (tests only)
  ↓
  20-scenario matrix
  ↓
  multi-device convergence proof
```

---

# 119. Invariant — observability is not authority

```text
If deleting/losing observability state can change what business data syncs,
Session 73 is invalid.
```

---

# 120. Invariant — no fake freshness

```text
If a stale/unknown server head is reported as current without freshness truth,
Session 73 is invalid.
```

---

# 121. Invariant — correlation completeness

```text
If phase failures cannot be tied to one syncRunId,
Session 73 is incomplete.
```

---

# 122. Invariant — privacy

```text
If remote diagnostics contain raw token/OTP/password/raw business payload/raw scope identity,
Session 73 is invalid.
```

---

# 123. Invariant — deterministic fault suite

```text
If the same scenario seed can produce materially different expected outcomes,
Session 73 fault evidence is invalid.
```

---

# 124. Invariant — production fault safety

```text
If production can activate fault injection through user/server/remote config,
Session 73 is invalid.
```

---

# 125. Invariant — happy path insufficient

```text
If FULL_PASS is based on ordinary success tests without the mandatory failure matrix,
Session 73 is invalid.
```

---

# 126. Invariant — no lost writes

```text
After recovery to quiescence, every accepted local intent must be either
canonically applied, explicitly typed-terminal, or explicitly user/policy resolved.
Silent disappearance is forbidden.
```

---

# 127. Invariant — no resurrected deletes

```text
A deleted server entity must not reappear solely because a device missed Realtime,
replayed an old page, or returned after retention.
```

---

# 128. Invariant — no duplicate effects

```text
Retry/duplicate response/replayed page may repeat transport,
but must not repeat the logical business effect.
```

---

# 129. Invariant — no cross-account leakage

```text
No Outbox/Inbox/Cursor/Bootstrap/Reconciliation/Observability state from scope A
may become visible or actionable in scope B.
```

---

# 130. Invariant — eventual convergence

```text
With network restored, no new writes, and retryable work allowed to finish,
all participating devices must converge to the same canonical logical server state.
```

---

# 131. Invariant — transaction group atomicity under fault

```text
Injected failure inside a server transaction group apply must never expose half the group.
```

---

# 132. Invariant — cursor atomicity under fault

```text
Injected failure before local transaction commit must never leave cursor ahead of committed data/Inbox.
```

---

# 133. Invariant — ambiguous commit recovery

```text
Commit-then-timeout must reuse mutationId and reconcile/replay receipt,
never invent a new mutation identity.
```

---

# 134. Invariant — Realtime remains optional for correctness

```text
Dropping all Realtime hints must slow convergence only;
it must not make convergence impossible.
```

---

# 135. Invariant — bootstrap pending intent

```text
Cursor expiry recovery must not destroy active local Outbox intent.
```

---

# 136. Invariant — revision gaps

```text
Valid monotonic DATA revision gaps must not be treated as data loss by the client.
```

---

# 137. Invariant — observability counters scoped

```text
Counters from A must not continue accumulating into B after account switch.
```

---

# 138. Invariant — zero silent hint drop

```text
For valid active scope, accepted sync requests must either be serviced by current/trailing generation
or explicitly cancelled by a typed scope/lifecycle reason.
```

---

# 139. Invariant — no metric-driven business rollback

```text
A telemetry sink failure must not roll back an otherwise valid business sync transaction.
```

---

# 140. Invariant — source-of-truth preservation

```text
Room remains the UI source of truth; observability state is diagnostic metadata only.
```

---

# 141. Invariant — v72 canonical authority preserved

```text
Session 73 must not reintroduce LegacyRemotePuller or separate deletion cursor as steady-state authority.
```

---

# 142. Invariant — command/data revision separation preserved

```text
No diagnostic convenience may merge COMMAND_RECEIPT and DATA_CHANGE revisions.
```

---

# 143. Invariant — no synthetic historical metrics

```text
Migration 18→19 must not claim historical bootstrap/reconciliation success at migration time.
Unknown history remains unknown.
```

---

# 144. Required final verification JSON

أنشئ:

```text
AUTODRIVE_SYNC_VERIFICATION_v73.json
```

الحد الأدنى:

```text
session
sourceArchive
sourceSha256
roomVersionBefore
roomVersionAfter
implemented
staticVerified
modelVerified
migrationModelVerified
compiled
unitTested
androidMigrationTested
androidFaultRuntimeTested
serverRuntimeVerified
observabilityImplemented
correlationVerified
privacyVerified
faultScenarioCount
faultScenarioPassCount
requiredFaultScenariosExecuted
multiDeviceConvergenceVerified
crossAccountIsolationVerified
chat10kVerified
noLostWrites
noResurrectedDeletes
noDuplicateEffects
noCrossAccountLeakage
deterministicEventualConvergence
predecessorGateSatisfied
v67TombstoneBlockerSuperseded
newV73WaiverCount
criticalBlockerCount
syncModernizationClosed
finalVerdict
```

---

# 145. Required final verification Markdown

أنشئ:

```text
AUTODRIVE_SYNC_VERIFICATION_v73.md
```

ويجب أن يلخص الحقيقة دون تسويق:

```text
Implemented
Static/model evidence
Runtime evidence
Fault matrix summary
Convergence summary
Privacy summary
Regression summary
Open blockers
Final closure flag
```

---

# 146. Required final artifacts

عند إتمام التنفيذ يجب وجود:

```text
SESSION_73_FINAL.md
AUTODRIVE_SYNC_VERIFICATION_v73.json
AUTODRIVE_SYNC_VERIFICATION_v73.md
AUTODRIVE_SYNC_OBSERVABILITY_INVENTORY_v73.json
AUTODRIVE_SYNC_FAULT_MATRIX_v73.json
AUTODRIVE_SYNC_CONVERGENCE_PROOF_v73.json
scripts/verify-v73-static.py
scripts/verify-v73-model.py
scripts/verify-v73-migration.py
verification-v73/*
source-of-truth ZIP
SHA-256 sidecars
```

وإذا أضيف Room19:

```text
18.json / 19.json Room schema evidence where project export permits
migration instrumentation evidence
```

---

# 147. Diff inventory

التقرير يجب أن يذكر:

```text
changedExistingFiles
addedFiles
deletedFiles
productionUiFilesChanged
historicalRoomMigrationsMutated
historicalServerMigrationsMutated
unexpectedProductionMutations
newWaivers
```

أي حذف production غير مبرر:

```text
BLOCKED_UNEXPECTED_DELETE
```

---

# 148. Historical migration integrity

يجب مقارنة hashes قبل/بعد لـ:

```text
MIGRATION_13_14
MIGRATION_14_15
MIGRATION_15_16
MIGRATION_16_17
MIGRATION_17_18
v69 server migration
v71 server migration
v72 server migration
```

73 تضيف فقط:

```text
MIGRATION_18_19
```

إلا إذا server observability migration جديدة مبررة، وهي يجب أن تكون append-only.

---

# 149. Packaging gate

قبل التغليف:

```text
run v73 static twice
run v73 model twice
run v73 migration model twice
run inherited regressions
attempt runtime gates
scan secrets
scan generated junk
```

ثم:

```text
package clean ZIP
extract fresh
rerun deterministic verifiers against extracted ZIP
```

---

# 150. Final ZIP naming

الاسم المقترح:

```text
AutoDrive-v73-observability-fault-injection-convergence.zip
```

الاسم ليس invariant؛ المحتوى والـSHA هما authority.

---

# 151. Final source-of-truth sidecars

أنشئ:

```text
AutoDrive-v73-*.zip.sha256
SESSION_73_FINAL.md.sha256
AUTODRIVE_SYNC_VERIFICATION_v73.json.sha256
AUTODRIVE_SYNC_VERIFICATION_v73.md.sha256
```

لا تحاول تضمين SHA للZIP نفسه داخل ZIP كحقيقة circular.

---

# 152. Stop conditions

توقف التنفيذ عن claim النجاح إذا ظهر:

```text
BLOCKED_INPUT_DRIFT
BLOCKED_ROOM_BASELINE_DRIFT
BLOCKED_PREDECESSOR_HANDOFF without user override
BLOCKED_SECRET_LEAK
BLOCKED_PRODUCTION_FAULT_SURFACE
BLOCKED_CROSS_SCOPE_METRIC_QUERY
BLOCKED_REALTIME_REGRESSION
BLOCKED_V72_AUTHORITY_REGRESSION
BLOCKED_UNRESOLVED_PROTOCOL_DECISION
FAULT_SUITE_NON_DETERMINISTIC
GENUINE_REGRESSION
```

يجوز متابعة التحليل/التوثيق، لكن لا يجوز claim PASS.

---

# 153. ما يعتبر نجاحًا جزئيًا مشروعًا

إذا البيئة تمنع Gradle أو server runtime لكن التنفيذ source/model صحيح:

يجوز verdict مثل:

```text
IMPLEMENTED_STATIC_MODEL_FAULT_PASS_RUNTIME_BLOCKED_PREDECESSOR_OVERRIDDEN
```

بشرط:

```text
all NOT_RUN flags explicit
no runtime inferred
syncModernizationClosed=false
```

---

# 154. ما لا يعتبر نجاحًا

غير مقبول:

```text
"all tests pass" إذا Gradle لم يعمل
"server verified" من قراءة SQL فقط
"fault injection passed" من static grep فقط
"converged" من جهاز واحد فقط في multi-device scenario
"no cross-account leakage" دون تبديل فعلي/scoped model
"cursor expiry safe" دون bootstrap recovery test
```

---

# 155. الحد الأدنى للـFinal Closure

حتى يقال إن مسار v67→v73 أغلق فعليًا:

```text
predecessor chain green
runtime server contract green
client compile/tests green
Room migration green
20 fault scenarios covered at appropriate proof level
multi-device convergence green
cross-account isolation green
privacy-safe observability green
zero new waivers
zero critical blockers
```

---

# 156. Final completion statement template

إذا FULL PASS حقيقي فقط:

```text
SESSION_73_COMPLETED=true
SYNC_MODERNIZATION_CLOSED=true
FINAL_VERDICT=FULL_PASS
```

إذا runtime/predecessor blocked:

```text
SESSION_73_IMPLEMENTED=true|false according to evidence
SYNC_MODERNIZATION_CLOSED=false
FINAL_VERDICT=<truthful blocked verdict>
```

---

# 157. الخلاصة التنفيذية للمنفذ

نفذ 73 كإغلاق إثبات، لا كميزة جديدة:

```text
1) اربط كل run بـ syncRunId.
2) اجعل metrics المطلوبة قابلة للقراءة من exact scope.
3) أضف Room19 observability state دون جعلها authority.
4) امنع أي تسريب sensitive diagnostics.
5) أضف fault injector NoOp في production وdeterministic في tests.
6) نفذ السيناريوهات العشرين كاملة.
7) أثبت convergence على جهازين + server model/live حيث أمكن.
8) أعد تشغيل regressions 67→72.
9) لا تحول NOT_RUN إلى PASS.
10) لا تغلق المسار إذا predecessor/server/runtime ما زالت محجوبة.
```

هذه هي بوابة الإغلاق الصحيحة للجلسة 73.
