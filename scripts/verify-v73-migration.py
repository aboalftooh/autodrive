#!/usr/bin/env python3
from pathlib import Path
import argparse, hashlib, json, re, sqlite3, sys
EXPECTED={
"MIGRATION_13_14":"e2f85ee75230d98abe9cb5a360d4b274089b26a465e877cd374cbfbf73a88052",
"MIGRATION_14_15":"10ccb9ad1f41e15b15671254525adeebfc50a38ba14be94bcf9e47d9de34f1ad",
"MIGRATION_15_16":"0dd0d0bf18b233d1fd4213ded840f981a3c7ee42a72d806682b5cb4b16df2d43",
"MIGRATION_16_17":"6d096b57d58b399753a9e495d98765e21ae2e57f764a7e4bf422f2a086b56862",
"MIGRATION_17_18":"9b03b4058ca6646db3009e8fddcb0a3f84b9a6ec2655c9b60c99940c27000cf1"}
def h(s):return hashlib.sha256(s.encode()).hexdigest()
def blocks(s):
 out={}
 for label in EXPECTED:
  a=s.index('val '+label); m=re.search(r'^        \}$',s[a:],re.M)
  if not m: raise ValueError('cannot close '+label)
  out[label]=h(s[a:a+m.end()].strip())
 return out

def main():
 ap=argparse.ArgumentParser();ap.add_argument('--root',default='.');ap.add_argument('--output');a=ap.parse_args();root=Path(a.root)
 s=(root/'core/database/src/main/kotlin/com/autodrive/app/core/database/AutoDriveDatabase.kt').read_text(); checks=[]
 def ck(n,c,d=''):checks.append({'name':n,'passed':bool(c),'detail':d if not c else ''})
 ck('version_19','AUTODRIVE_DATABASE_VERSION = 19' in s)
 ck('only_append_18_19','Migration(18, 19)' in s and 'MIGRATION_19_20' not in s)
 ck('historical_hashes',blocks(s)==EXPECTED,json.dumps(blocks(s),sort_keys=True))
 mig=s[s.index('val MIGRATION_18_19'):s.index('val ROOM_V13_INDEXES')]
 m=re.search(r'CREATE TABLE IF NOT EXISTS sync_observability_state \((.*?)\n\s*\)\n\s*"""',mig,re.S)
 ck('create_table_extractable',m is not None)
 cols=[]; defaults={}; pk=[]
 if m:
  sql='CREATE TABLE sync_observability_state ('+m.group(1)+'\n)'
  con=sqlite3.connect(':memory:'); con.execute(sql)
  info=con.execute('pragma table_info(sync_observability_state)').fetchall(); con.close()
  cols=[r[1] for r in info]; defaults={r[1]:r[4] for r in info}; pk=[r[1] for r in sorted(info,key=lambda x:x[5]) if r[5]>0]
 req=['user_id','client_id','org_id','stream','contract_version','last_sync_run_id','last_server_head_observed_at','bootstrap_count','cursor_expiry_count','reconciliation_mismatch_count','hint_received_count','hint_dropped_count','updated_at_local']
 ck('required_columns',all(x in cols for x in req),','.join(x for x in req if x not in cols))
 ck('exact_scope_pk',pk==['user_id','client_id','org_id','stream'],str(pk))
 ck('counters_zero_default',all(defaults.get(x) in ('0','0.0') for x in ['bootstrap_count','cursor_expiry_count','reconciliation_mismatch_count','reconciliation_repair_count','rebootstrap_count','hint_received_count','hint_trailing_run_count','hint_dropped_count']),str(defaults))
 ck('no_synthetic_history',all(defaults.get(x) is None for x in ['last_successful_bootstrap_at','last_reconciliation_at','last_sync_completed_at_local','last_success_at_local']),str(defaults))
 ck('no_destructive_fallback','fallbackToDestructiveMigration' not in s)
 result={'session':73,'verifier':'migration','totalCount':len(checks),'passedCount':sum(x['passed'] for x in checks),'passed':all(x['passed'] for x in checks),'assertions':checks,'columns':cols,'primaryKey':pk}
 text=json.dumps(result,indent=2,sort_keys=True)+'\n'; Path(a.output).write_text(text) if a.output else print(text,end=''); sys.exit(0 if result['passed'] else 1)
if __name__=='__main__':main()
