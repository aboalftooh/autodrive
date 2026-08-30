#!/usr/bin/env python3
from pathlib import Path
import json
R=Path(__file__).resolve().parents[1]
db=(R/'core/database/src/main/kotlin/com/autodrive/app/core/database/AutoDriveDatabase.kt').read_text()
ent=(R/'core/database/src/main/kotlin/com/autodrive/app/core/database/entities/Entities.kt').read_text()
chat=(R/'core/database/src/main/kotlin/com/autodrive/app/core/database/entities/ChatSyncEntities.kt').read_text()
checks={
'room17':'AUTODRIVE_DATABASE_VERSION = 17' in db,
'migration16_17':'MIGRATION_16_17 = object : Migration(16, 17)' in db,
'registered':'MIGRATION_16_17,' in db,
'dependency_column':'depends_on_mutation_id' in db and 'dependsOnMutationId' in ent,
'checkpoint_table':'CREATE TABLE IF NOT EXISTS chat_recovery_checkpoints' in db and 'last_server_sequence INTEGER NOT NULL DEFAULT 0' in db and 'lastServerSequence: Long = 0L' in chat,
'media_table':'CREATE TABLE IF NOT EXISTS chat_media_transfers' in db and 'tableName = "chat_media_transfers"' in chat,
'media_object_path':'ALTER TABLE chat_messages ADD COLUMN media_object_path TEXT' in db,
'no_sync_inbox_mutation':db[db.index('val MIGRATION_16_17'):db.index('val ROOM_V13_INDEXES')].count('sync_inbox')==0,
'old_15_16_present':'val MIGRATION_15_16 = object : Migration(15, 16)' in db,
}
print(json.dumps({'allPassed':all(checks.values()),'passedCount':sum(checks.values()),'checkCount':len(checks),'checks':checks},sort_keys=True,separators=(',',':')))
raise SystemExit(0 if all(checks.values()) else 1)
