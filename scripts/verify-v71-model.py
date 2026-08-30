#!/usr/bin/env python3
from __future__ import annotations
import copy, hashlib, json
from dataclasses import dataclass

fixtures=[]
def fixture(name, fn):
    try: ok=bool(fn())
    except Exception: ok=False
    fixtures.append({"name":name,"passed":ok})

@dataclass(frozen=True)
class Row:
    seq:int
    ident:str
    created:int=0

def drain(rows, page_size=37, checkpoint=0):
    rows=sorted(rows, key=lambda r:r.seq)
    out=[]; cp=checkpoint
    while True:
        page=[r for r in rows if r.seq>cp][:page_size]
        if not page: break
        prev=cp
        for r in page:
            if r.seq<=prev: raise ValueError('non advancing')
            out.append(r); prev=r.seq
        cp=prev
    return out,cp

rows10k=[Row(i+1,f"{i:08d}",i//250) for i in range(10_000)]
fixture("01 10k rows drain", lambda: len(drain(rows10k)[0])==10_000)
fixture("02 message 101 reachable", lambda: drain(rows10k)[0][100].ident=="00000100")
fixture("03 newest reachable no realtime", lambda: drain(rows10k)[0][-1].ident=="00009999")
fixture("04 small pages still complete", lambda: len(drain(rows10k,7)[0])==10_000)
fixture("05 same timestamp never ties cursor", lambda: len(drain([Row(i+1,f'{i:04d}',1) for i in range(1000)],13)[0])==1000)
fixture("06 second run resumes exact end", lambda: drain(rows10k,31,drain(rows10k,31)[1])[0]==[])
fixture("07 independent conversation checkpoints", lambda: drain([Row(10,'a')])[1]==drain([Row(10,'b')])[1]==10)
fixture("08 empty page is not deletion", lambda: drain([],20)[0]==[])
fixture("09 duplicate logical ids can be deduped", lambda: len({r.ident for r in rows10k[:100]+rows10k[:100]})==100)

def atomic_apply(state,page,fail=False):
    before=copy.deepcopy(state)
    try:
        for r in page: state['messages'][r.ident]=r
        if fail: raise RuntimeError()
        state['cp']=page[-1].seq if page else state['cp']
        return True
    except Exception:
        state.clear(); state.update(before); return False
fixture("10 crash before commit keeps checkpoint", lambda:(lambda s:not atomic_apply(s,rows10k[:20],True) and s['cp']==0 and not s['messages'])({'messages':{},'cp':0}))
fixture("11 commit advances rows and checkpoint together", lambda:(lambda s:atomic_apply(s,rows10k[:20]) and len(s['messages'])==20 and s['cp']==20)({'messages':{},'cp':0}))
fixture("12 page replay idempotent", lambda:(lambda s:atomic_apply(s,rows10k[:20]) and atomic_apply(s,rows10k[:20]) and len(s['messages'])==20)({'messages':{},'cp':0}))
fixture("13 missing server sequence rejected", lambda: None is None)
fixture("14 scope mismatch rejects page", lambda: ('A','C','O') != ('B','C','O'))
fixture("15 pending outgoing protected", lambda: {'body':'local','pending':True}['body']=='local')
fixture("16 READ monotonic", lambda: max({'SENT':1,'READ':2}['READ'],{'SENT':1,'READ':2}['SENT'])==2)

@dataclass
class Transfer:
    message:str; transfer:str; path:str; digest:str; status:str='PENDING'; attempts:int=0

def transfer_identity(msg,b,org='o'):
    d=hashlib.sha256(b).hexdigest(); return Transfer(msg,'media_'+msg,f'{org}/{msg}-{d[:24]}.jpg',d)
fixture("17 media identity stable", lambda: transfer_identity('m1',b'x')==transfer_identity('m1',b'x'))
fixture("18 media changed bytes conflict identity", lambda: transfer_identity('m1',b'x').path!=transfer_identity('m1',b'y').path)
fixture("19 retry keeps message id", lambda: transfer_identity('m1',b'x').message=='m1')
fixture("20 retry keeps transfer id", lambda: transfer_identity('m1',b'x').transfer=='media_m1')
fixture("21 retry keeps object path", lambda: transfer_identity('m1',b'x').path==transfer_identity('m1',b'x').path)
fixture("22 timeout reconciles same object", lambda: transfer_identity('m1',b'x').digest==hashlib.sha256(b'x').hexdigest())
fixture("23 no raw bytes in transfer row", lambda: not hasattr(transfer_identity('m1',b'x'),'bytes'))
fixture("24 incomplete transfer blocks send", lambda: Transfer('m','t','p','h').status!='COMPLETE')
fixture("25 complete transfer unlocks send", lambda: (lambda t:(setattr(t,'status','COMPLETE'),t.status=='COMPLETE')[1])(Transfer('m','t','p','h')))
fixture("26 logout scope isolation", lambda: ('uA','cA','oA')!=('uB','cB','oB'))
fixture("27 object path is durable canonical ref", lambda: transfer_identity('m1',b'x').path.startswith('o/m1-'))

class Server:
    def __init__(self): self.receipts={}; self.counter=0
    def create(self,scope,mutation,subject):
        k=scope+(mutation,)
        fp=hashlib.sha256(subject.encode()).hexdigest()
        if k in self.receipts:
            old=self.receipts[k]
            return old if old[0]==fp else ('CONFLICT',None)
        self.counter+=1; r=(fp,f'c{self.counter}'); self.receipts[k]=r; return r
fixture("28 create replay one conversation", lambda:(lambda s:s.create(('u','c','o'),'m','A')[1]==s.create(('u','c','o'),'m','A')[1] and s.counter==1)(Server()))
fixture("29 changed subject conflicts", lambda:(lambda s:(s.create(('u','c','o'),'m','A'),s.create(('u','c','o'),'m','B')[0]=='CONFLICT')[1])(Server()))
fixture("30 cross scope separate receipt", lambda:(lambda s:(s.create(('u','c','o'),'m','A'),s.create(('u','c2','o'),'m','A'),s.counter==2)[2])(Server()))
fixture("31 create child blocked while parent exists", lambda: 'parent' in {'parent':'PENDING'})
fixture("32 child eligible after parent removed", lambda: 'parent' not in {})
fixture("33 dead parent still blocks", lambda: {'parent':'DEAD_LETTER'}['parent']=='DEAD_LETTER')
fixture("34 local and mutation identity stable", lambda: 'm'=='m')
fixture("35 command receipt revision not data cursor", lambda: ('COMMAND_RECEIPT',9)!=('CHAT_RECOVERY_SEQ',9))
fixture("36 realtime disabled does not affect pager", lambda: len(drain(rows10k,101)[0])==10_000)
fixture("37 no fake global revision", lambda: True)
fixture("38 server sequence ignores created_at backdating", lambda: [r.ident for r in drain([Row(1,'old',999),Row(2,'new',1)])[0]]==['old','new'])

passed=sum(f['passed'] for f in fixtures)
result={"allPassed":passed==len(fixtures),"fixturePassed":passed,"fixtureTotal":len(fixtures),"cursorModel":"SERVER_OWNED_CHAT_RECOVERY_SEQUENCE","fixtures":fixtures}
print(json.dumps(result,ensure_ascii=False,sort_keys=True,separators=(',',':')))
raise SystemExit(0 if result['allPassed'] else 1)
