#!/usr/bin/env python3
import hashlib, json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
s=(ROOT/'core/database/src/main/kotlin/com/autodrive/app/core/database/AutoDriveDatabase.kt').read_text()
checks=[]
def add(name,ok): checks.append({'name':name,'passed':bool(ok)})
add('room_version_at_least_16', any(f'AUTODRIVE_DATABASE_VERSION = {v}' in s for v in range(16, 100)))
add('migration_15_16','val MIGRATION_15_16 = object : Migration(15, 16)' in s)
for col in ['user_id TEXT NOT NULL','client_id TEXT NOT NULL','org_id TEXT NOT NULL','stream TEXT NOT NULL','event_id TEXT NOT NULL','server_revision TEXT,','transaction_group_id TEXT,','received_at INTEGER NOT NULL','applied_at INTEGER,']:
    add('column_'+col.split()[0],col in s)
add('scoped_primary_key','PRIMARY KEY(user_id, client_id, org_id, stream, event_id)' in s)
add('no_business_drop','DROP TABLE invoices' not in s[s.index('val MIGRATION_15_16'):])
add('migration_registered','MIGRATION_15_16,' in s)
m13=s[s.index('        val MIGRATION_13_14'):s.index('        /**\n         * v68')]
m14=s[s.index('        val MIGRATION_14_15'):s.index('        /** v70: durable scoped inbound event ledger.')]
add('m13_14_unchanged',hashlib.sha256(m13.encode()).hexdigest()=='079d3c00ea43a453db6acaea822c730ef4bb2f4b4eb22b3b80bfffd422c70a8f')
add('m14_15_unchanged',hashlib.sha256(m14.encode()).hexdigest()=='c151c823af07f1145e416137060932bf8fcc4bfa449f4b0d0f8e728c5dbfa981')
passed=sum(x['passed'] for x in checks)
out={'allPassed':passed==len(checks),'passedCount':passed,'checkCount':len(checks),'instrumentationRun':False,'instrumentationReason':'Gradle bootstrap blocked by UnknownHostException: services.gradle.org','checks':checks}
print(json.dumps(out,sort_keys=True,separators=(',',':')))
raise SystemExit(0 if out['allPassed'] else 1)
