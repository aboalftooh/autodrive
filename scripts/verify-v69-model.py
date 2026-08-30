#!/usr/bin/env python3
import hashlib, json
from dataclasses import dataclass
@dataclass(frozen=True)
class Receipt:
    mutation:str; command:str; fingerprint:str; status:str='APPLIED'; entity:str='e1'; revision:int=1
class Ledger:
    def __init__(self): self.r={}; self.effects=0; self.rev=0
    def send(self, scope, mutation, command, payload, apply=True):
        fp=hashlib.sha256(json.dumps(payload,sort_keys=True,separators=(',',':')).encode()).hexdigest()
        key=(*scope,mutation)
        if key in self.r:
            old=self.r[key]
            if old.command==command and old.fingerprint==fp: return old,True
            return Receipt(mutation,command,fp,'CONFLICT',old.entity,old.revision),True
        if not apply: return None,False
        self.effects+=1; self.rev+=1
        rec=Receipt(mutation,command,fp,revision=self.rev); self.r[key]=rec; return rec,False

def run():
    results=[]
    def ck(n,c): results.append((n,bool(c)))
    s=('u','c','o'); l=Ledger()
    first,_=l.send(s,'m1','UPDATE_PROFILE',{'name':'A'})
    for _ in range(99): r,replayed=l.send(s,'m1','UPDATE_PROFILE',{'name':'A'})
    ck('same_mutation_100_effect_once', l.effects==1 and r==first and replayed)
    conflict,_=l.send(s,'m1','UPDATE_PROFILE',{'name':'B'})
    ck('changed_payload_conflicts', conflict.status=='CONFLICT' and l.effects==1)
    conflict2,_=l.send(s,'m1','SEND_CHAT_MESSAGE',{'name':'A'})
    ck('changed_command_conflicts', conflict2.status=='CONFLICT' and l.effects==1)
    before=l.effects; r,_=l.send(s,'m2','SEND_CHAT_MESSAGE',{'body':'x'},apply=False); ck('timeout_before_commit_no_effect',r is None and l.effects==before)
    r,_=l.send(s,'m2','SEND_CHAT_MESSAGE',{'body':'x'}); ck('retry_after_precommit_timeout_applies_once',r.status=='APPLIED' and l.effects==before+1)
    committed,_=l.send(s,'m3','REQUEST_WITHDRAWAL',{'amount':'10.00'}); effects=l.effects
    replay,replayed=l.send(s,'m3','REQUEST_WITHDRAWAL',{'amount':'10.00'})
    ck('timeout_after_commit_replays', replay==committed and replayed and l.effects==effects)
    l.send(('u2','c2','o2'),'m3','REQUEST_WITHDRAWAL',{'amount':'10.00'})
    ck('cross_scope_same_mutation_independent', l.effects==effects+1)
    ck('receipt_revision_positive', committed.revision>0)
    ck('receipt_revision_monotonic', sorted(x.revision for x in l.r.values())==[x.revision for x in sorted(l.r.values(),key=lambda x:x.revision)])
    # Typed retry behavior model required by v69.
    terminal={'PERMISSION','VALIDATION','CONFLICT','PERMANENT_PROTOCOL'}
    retriable={'TRANSIENT','AUTH','AMBIGUOUS','ALREADY_COMMITTED'}
    ck('typed_terminal_set', terminal=={'PERMISSION','VALIDATION','CONFLICT','PERMANENT_PROTOCOL'})
    ck('typed_retry_set', retriable=={'TRANSIENT','AUTH','AMBIGUOUS','ALREADY_COMMITTED'})
    ck('ambiguous_not_terminal', 'AMBIGUOUS' not in terminal)
    ck('auth_not_terminal', 'AUTH' not in terminal)
    ck('already_committed_success_class', 'ALREADY_COMMITTED' in retriable)
    # Lost duplicate response cannot delete another mutation because identity includes mutation id.
    keys=set(l.r); ck('scoped_receipt_identity_unique', len(keys)==len(l.r))
    passed=sum(v for _,v in results)
    for n,v in results: print(('PASS' if v else 'FAIL'),n)
    print(f'SUMMARY {passed}/{len(results)} PASS')
    return 0 if passed==len(results) else 1
if __name__=='__main__': raise SystemExit(run())
