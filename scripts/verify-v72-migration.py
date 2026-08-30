#!/usr/bin/env python3
from pathlib import Path
import json,re,sys
r=Path(__file__).resolve().parents[1]
db=(r/'core/database/src/main/kotlin/com/autodrive/app/core/database/AutoDriveDatabase.kt').read_text()
ents=(r/'core/database/src/main/kotlin/com/autodrive/app/core/database/entities/SyncBootstrapEntities.kt').read_text()
pay=(r/'core/database/src/main/kotlin/com/autodrive/app/core/database/entities/Entities.kt').read_text()
checks=[]
def ck(n,c): checks.append({'name':n,'pass':bool(c)})
ck('version_18','AUTODRIVE_DATABASE_VERSION = 18' in db)
block=db.split('val MIGRATION_17_18',1)[1].split('val ROOM_V13_INDEXES',1)[0]
ck('append_only_17_18','Migration(17, 18)' in block)
for t in ('sync_bootstrap_state','sync_bootstrap_staging','sync_reconciliation_state'):
    ck('ddl_'+t,f'CREATE TABLE IF NOT EXISTS {t}' in block and f'tableName = "{t}"' in ents)
ck('scoped_bootstrap_pk','primaryKeys = ["user_id", "client_id", "org_id", "stream"]' in ents)
ck('scoped_staging_pk','primaryKeys = ["user_id", "client_id", "org_id", "bootstrap_id", "entity_type", "entity_id"]' in ents)
ck('payment_client_column','ALTER TABLE payments ADD COLUMN client_id TEXT NOT NULL' in block and '@ColumnInfo(name = "client_id") val clientId: String' in pay)
ck('payment_backfill','UPDATE payments SET client_id' in block and 'SELECT client_id FROM invoices' in block)
ck('payment_scope_index','index_payments_client_id' in block and 'Index(value = ["client_id"], name = "index_payments_client_id")' in pay)
ck('no_destructive_drop','DROP TABLE' not in block.upper() and 'DELETE FROM invoices' not in block)
ok=all(x['pass'] for x in checks)
out={'session':72,'verifier':'migration-model','passed':ok,'passedCount':sum(x['pass'] for x in checks),'totalCount':len(checks),'checks':checks}
print(json.dumps(out,sort_keys=True,separators=(',',':')))
sys.exit(0 if ok else 1)
