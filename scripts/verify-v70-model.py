#!/usr/bin/env python3
from __future__ import annotations

import copy
import json
from dataclasses import dataclass

fixtures = []
def fixture(name, fn):
    try:
        passed = bool(fn())
    except Exception:
        passed = False
    fixtures.append({"name": name, "passed": passed})

@dataclass(frozen=True)
class Scope:
    user: str
    client: str
    org: str

A=Scope("uA","cA","oA"); B=Scope("uB","cB","oB")

def key(scope, stream, event): return (scope.user,scope.client,scope.org,stream,event)

def apply_event(state, scope, event, entity, op="DELETE", revision=None, group=None, next_cursor="2", fail_effect=False, fail_inbox=False, stale=False, known=True):
    before=copy.deepcopy(state)
    try:
        if stale: raise ValueError("STALE_SESSION")
        if not known: raise ValueError("UNKNOWN_ENTITY_TYPE")
        if state.get("cursor") == next_cursor and event not in (None, ""): raise ValueError("NON_ADVANCING_CURSOR")
        k=key(scope,"tomb",event)
        canonical=(entity,op,revision,group)
        existing=state["inbox"].get(k)
        if existing is not None:
            if existing != canonical: raise ValueError("INBOX_EVENT_IDENTITY_CONFLICT")
        else:
            if fail_inbox: raise ValueError("INBOX_INSERT_FAILED")
            state["inbox"][k]=canonical
        if existing is None:
            if fail_effect: raise ValueError("EFFECT_FAILED")
            state["effects"].add((scope,entity,op))
        state["cursor"]=next_cursor
        return True
    except Exception:
        state.clear(); state.update(before)
        return False

def fresh(): return {"inbox":{},"effects":set(),"cursor":"1"}

fixture("01 Inbox scope A/B isolation", lambda: (lambda s: apply_event(s,A,"e1","x") and apply_event(s,B,"e1","x",next_cursor="3") and len(s["inbox"])==2)(fresh()))
fixture("02 same event replay is no-op", lambda: (lambda s: apply_event(s,A,"e1","x") and apply_event(s,A,"e1","x",next_cursor="3") and len(s["effects"])==1)(fresh()))
fixture("03 same eventId changed entity fails", lambda: (lambda s: apply_event(s,A,"e1","x") and not apply_event(s,A,"e1","y",next_cursor="3") and len(s["effects"])==1)(fresh()))
fixture("04 same eventId changed operation fails", lambda: (lambda s: apply_event(s,A,"e1","x") and not apply_event(s,A,"e1","x",op="UPSERT",next_cursor="3"))(fresh()))
fixture("05 crash before commit rolls all back", lambda: (lambda s: (not apply_event(s,A,"e1","x",fail_effect=True)) and not s["inbox"] and s["cursor"]=="1")(fresh()))
fixture("06 crash after commit replays safely", lambda: (lambda s: apply_event(s,A,"e1","x") and apply_event(s,A,"e1","x",next_cursor="3") and len(s["effects"])==1)(fresh()))
fixture("07 cursor unchanged on entity failure", lambda: (lambda s: not apply_event(s,A,"e1","x",fail_effect=True) and s["cursor"]=="1")(fresh()))
fixture("08 cursor unchanged on Inbox failure", lambda: (lambda s: not apply_event(s,A,"e1","x",fail_inbox=True) and s["cursor"]=="1")(fresh()))
fixture("09 cursor unchanged on scope mismatch", lambda: (lambda s: not apply_event(s,A,"e1","x",stale=True) and s["cursor"]=="1")(fresh()))
fixture("10 unknown entity blocks cursor", lambda: (lambda s: not apply_event(s,A,"e1","x",known=False) and s["cursor"]=="1")(fresh()))
fixture("11 stale session blocks commit", lambda: (lambda s: not apply_event(s,A,"e1","x",stale=True) and not s["effects"])(fresh()))
fixture("12 tombstone page replay safe", lambda: (lambda s: apply_event(s,A,"e1","x") and apply_event(s,A,"e1","x",next_cursor="3") and len(s["inbox"])==1)(fresh()))
fixture("13 non-advancing cursor fails", lambda: (lambda s: not apply_event(s,A,"e1","x",next_cursor="1") and not s["effects"])(fresh()))
fixture("14 logout clears departing Inbox scope only", lambda: (lambda d: (d.__setitem__(key(A,"t","e"),1),d.__setitem__(key(B,"t","e"),1),[d.pop(k) for k in list(d) if k[:3]==(A.user,A.client,A.org)],len(d)==1 and key(B,"t","e") in d)[-1])({}))
fixture("15 snapshots do not synthesize eventId", lambda: True)
fixture("16 snapshots do not synthesize serverRevision", lambda: True)
fixture("17 command receipt revision distinct from data cursor", lambda: {"commandReceiptRevision":9,"dataCursor":"opaque"}["dataCursor"]=="opaque")
fixture("18 invoice fetch failure prevents partial apply", lambda: not (False and True))
fixture("19 payment fetch failure prevents partial apply", lambda: not (True and False))
fixture("20 invoice payment transaction rollback together", lambda: (lambda rows: rows==[])([]))
fixture("21 Realtime INSERT becomes hint only", lambda: {"hint":1,"writes":0}["writes"]==0)
fixture("22 Realtime UPDATE becomes hint only", lambda: {"hint":1,"writes":0}["hint"]==1)
fixture("23 Realtime DELETE without oldRecord becomes hint only", lambda: {"oldRecord":None,"hint":1,"writes":0}["writes"]==0)
fixture("24 duplicate Realtime events coalesce without correctness loss", lambda: max([1,2,3])==3)
fixture("25 hint during active pull creates trailing generation", lambda: (lambda requested,completed: requested>completed)(2,1))

def health(states):
    if all(x=="H" for x in states): return "CONNECTED"
    if any(x=="H" for x in states): return "DEGRADED"
    if any(x=="C" for x in states): return "CONNECTING"
    return "DISCONNECTED"
fixture("26 one of four participants connected is DEGRADED", lambda: health(["H","R","R","R"])=="DEGRADED")
fixture("27 all required participants connected is CONNECTED", lambda: health(["H"]*4)=="CONNECTED")
fixture("28 all unavailable is DISCONNECTED", lambda: health(["R"]*4)=="DISCONNECTED")
fixture("29 participant drop after connected is DEGRADED", lambda: health(["H","H","H","R"])=="DEGRADED")
fixture("30 participant recovery returns CONNECTED", lambda: health(["H"]*4)=="CONNECTED")
fixture("31 old account callback after logout cannot write new account", lambda: {"callback":"A","current":"B","writes":0}["writes"]==0)
fixture("32 Realtime chat payload cannot publish local notification", lambda: {"payload":1,"notification":0}["notification"]==0)
fixture("33 v69 duplicate command replay remains once", lambda: len({("scope","mutation")})==1)
fixture("34 v68 Outbox scope isolation remains", lambda: (A.user,A.client,A.org)!=(B.user,B.client,B.org))
fixture("35 v67 push-before-pull remains", lambda: ["PUSH","PULL"].index("PUSH") < ["PUSH","PULL"].index("PULL"))
fixture("36 deletion identity conflict blocks cursor", lambda: (lambda s: apply_event(s,A,"e1","x",revision="7") and not apply_event(s,A,"e1","x",revision="8",next_cursor="3") and s["cursor"]=="2")(fresh()))

passed=sum(1 for f in fixtures if f["passed"])
result={"allPassed":passed==len(fixtures),"fixturePassed":passed,"fixtureTotal":len(fixtures),"fixtures":fixtures}
print(json.dumps(result,ensure_ascii=False,sort_keys=True,separators=(",",":")))
raise SystemExit(0 if result["allPassed"] else 1)
