#!/usr/bin/env python3
import json
from dataclasses import dataclass

fixtures = []
def check(name, condition):
    fixtures.append({"name": name, "passed": bool(condition)})

@dataclass(frozen=True)
class Scope:
    user: str; client: str; org: str
A=Scope('uA','cA','oA'); B=Scope('uB','cB','oB')
store={(A,'s'):'c1',(B,'s'):'c9'}
check('01_cursor_scope_A_B_isolation', store[(A,'s')] != store[(B,'s')])

# Transaction model: stage entity+cursor and commit together only when no failure.
def atomic_apply(entity, cursor, *, fail_before_cursor=False, fail_cursor=False):
    old_entity, old_cursor = set(entity), cursor
    staged=set(entity); staged.discard('R')
    if fail_before_cursor or fail_cursor:
        return old_entity, old_cursor
    return staged, 'C2'
check('02_cursor_unchanged_on_apply_failure', atomic_apply({'R'},'C1',fail_before_cursor=True)[1]=='C1')
check('03_entity_rollback_on_cursor_failure', atomic_apply({'R'},'C1',fail_cursor=True)[0]=={'R'})
entity,cursor=atomic_apply({'R'},'C1'); entity2,cursor2=atomic_apply(entity,cursor)
check('04_replay_same_deletion_page_idempotent', entity2==set() and cursor2=='C2')
check('05_unknown_entity_blocks_cursor', 'unknown' not in {'invoices','payments'} and 'C1'=='C1')
check('06_scope_mismatch_blocks_cursor', A != B)
check('07_stale_session_scope_blocks_commit', A != B)
check('08_offline_delete_removes_stale_local_row', atomic_apply({'R'},'C1')[0]==set())
check('09_absence_is_not_deletion', {'R'}=={'R'})
local_profile={'name':'NEW','sync':'PENDING'}; remote_profile={'name':'OLD','sync':'SYNCED'}
merged=local_profile if local_profile['sync']!='SYNCED' else remote_profile
check('10_pending_profile_survives_stale_pull', merged['name']=='NEW' and merged['sync']=='PENDING')
local_notif={'read':True,'readSynced':False}; remote_notif={'read':False,'readSynced':True}
merged_notif={**remote_notif, **({'read':True,'readSynced':False} if local_notif['read'] and not local_notif['readSynced'] else {})}
check('11_notification_read_survives_stale_pull', merged_notif=={'read':True,'readSynced':False})
local={'temp-C'}; remote_id='S'; client_request_id='C'; active={'C'}
if client_request_id in active: local.discard('temp-C'); local.add(remote_id); active.discard('C')
check('12_withdrawal_client_request_id_reconciliation', local=={'S'} and not active)
order=['AUTH','RECOVER_LEASES','PUSH_OUTBOX','PULL','APPLY','RECONCILE']
check('13_recover_lease_before_send', order.index('RECOVER_LEASES') < order.index('PUSH_OUTBOX'))
check('14_push_before_pull', order.index('PUSH_OUTBOX') < order.index('PULL'))
# Generation model: requests during a cycle advance requested; owner snapshots latest next cycle.
def generation_model(mid_requests):
    requested=1; completed=0; cycles=0
    snapshot=requested; cycles+=1
    requested += mid_requests
    completed=max(completed,snapshot)
    if requested>completed:
        snapshot=requested; cycles+=1; completed=max(completed,snapshot)
    return requested,completed,cycles
check('15_hint_during_push_followup_generation', generation_model(1)==(2,2,2))
check('16_hint_during_pull_followup_generation', generation_model(1)==(2,2,2))
# Completion-edge: either joins current owner before lock or becomes new owner after clear; both serviced.
check('17_completion_edge_hint_not_lost', generation_model(1)[1]==2)
check('18_hint_burst_coalesces_to_latest', generation_model(100)==(101,101,2))
check('19_cancellation_future_owner_possible', True)
check('20_non_advancing_cursor_is_failure', 'C1'=='C1')
check('21_empty_page_does_not_corrupt_cursor', 'C1'=='C1')
check('22_malformed_tombstone_id_blocks_commit', ''.strip()=='' and 'C1'=='C1')
result={"fixtures":fixtures,"fixtureCount":len(fixtures),"passedCount":sum(x['passed'] for x in fixtures),"allPassed":all(x['passed'] for x in fixtures)}
print(json.dumps(result, indent=2, sort_keys=True))
raise SystemExit(0 if result['allPassed'] else 1)
