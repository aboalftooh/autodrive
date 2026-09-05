#!/usr/bin/env python3
import sqlite3
import sys

PROFILE='UPDATE_PROFILE'
WITHDRAWAL='REQUEST_WITHDRAWAL_RPC'

def baseline(conn):
    conn.executescript('''
    CREATE TABLE autodrive_users (
      id TEXT PRIMARY KEY, user_id TEXT NOT NULL, client_id TEXT NOT NULL, org_id TEXT NOT NULL
    );
    CREATE TABLE withdrawal_requests (
      id TEXT PRIMARY KEY, user_id TEXT NOT NULL, client_id TEXT NOT NULL
    );
    CREATE TABLE pending_operations (
      id TEXT PRIMARY KEY, table_name TEXT NOT NULL, operation TEXT NOT NULL, payload TEXT NOT NULL,
      created_at INTEGER NOT NULL, status TEXT NOT NULL, attempt_count INTEGER NOT NULL,
      next_retry_at INTEGER NOT NULL, last_error_code TEXT, last_error_message TEXT,
      payload_version INTEGER NOT NULL, idempotency_key TEXT NOT NULL
    );
    ''')

def unresolved_count(conn):
    return conn.execute('''
      SELECT COUNT(*) FROM pending_operations p
      WHERE NOT (
        p.operation='UPDATE_PROFILE' AND EXISTS(
          SELECT 1 FROM autodrive_users u WHERE p.idempotency_key='profile:' || u.user_id
        )
      )
      AND NOT (
        p.operation='REQUEST_WITHDRAWAL_RPC' AND EXISTS(
          SELECT 1 FROM withdrawal_requests w
          JOIN autodrive_users u ON u.user_id=w.user_id AND u.client_id=w.client_id
          WHERE w.id=p.idempotency_key
        )
      )
    ''').fetchone()[0]

def migrate(conn):
    if unresolved_count(conn):
        raise RuntimeError('MIGRATION_UNSCOPED_OUTBOX_ROW')
    conn.executescript('''
      CREATE TABLE pending_operations_v15 (
        id TEXT NOT NULL PRIMARY KEY, mutation_id TEXT NOT NULL, user_id TEXT NOT NULL,
        client_id TEXT NOT NULL, org_id TEXT NOT NULL, entity_type TEXT NOT NULL,
        entity_id TEXT NOT NULL, operation TEXT NOT NULL, payload TEXT NOT NULL,
        contract_version INTEGER NOT NULL, created_at INTEGER NOT NULL, status TEXT NOT NULL,
        attempt_count INTEGER NOT NULL, next_retry_at INTEGER NOT NULL, lease_until INTEGER NOT NULL,
        last_error_code TEXT, last_error_message TEXT
      );
      INSERT INTO pending_operations_v15
      SELECT p.id,p.id,u.user_id,u.client_id,u.org_id,'autodrive_users',u.user_id,'UPDATE_PROFILE',
             p.payload,p.payload_version,p.created_at,p.status,p.attempt_count,
             CASE WHEN p.status='IN_PROGRESS' THEN 0 ELSE p.next_retry_at END,
             CASE WHEN p.status='IN_PROGRESS' THEN p.next_retry_at ELSE 0 END,
             p.last_error_code,p.last_error_message
      FROM pending_operations p JOIN autodrive_users u ON p.idempotency_key='profile:' || u.user_id
      WHERE p.operation='UPDATE_PROFILE';
      INSERT INTO pending_operations_v15
      SELECT p.id,p.idempotency_key,w.user_id,w.client_id,u.org_id,'withdrawal_requests',p.idempotency_key,
             'REQUEST_WITHDRAWAL_RPC',p.payload,p.payload_version,p.created_at,p.status,p.attempt_count,
             CASE WHEN p.status='IN_PROGRESS' THEN 0 ELSE p.next_retry_at END,
             CASE WHEN p.status='IN_PROGRESS' THEN p.next_retry_at ELSE 0 END,
             p.last_error_code,p.last_error_message
      FROM pending_operations p
      JOIN withdrawal_requests w ON w.id=p.idempotency_key
      JOIN autodrive_users u ON u.user_id=w.user_id AND u.client_id=w.client_id
      WHERE p.operation='REQUEST_WITHDRAWAL_RPC';
    ''')
    old=conn.execute('SELECT COUNT(*) FROM pending_operations').fetchone()[0]
    new=conn.execute('SELECT COUNT(*) FROM pending_operations_v15').fetchone()[0]
    if old != new: raise RuntimeError('MIGRATION_OUTBOX_ROW_COUNT_MISMATCH')


def main():
    c=sqlite3.connect(':memory:'); baseline(c)
    c.execute("INSERT INTO autodrive_users VALUES ('urow','user-a','client-a','org-a')")
    c.execute("INSERT INTO withdrawal_requests VALUES ('req-a','user-a','client-a')")
    c.execute("INSERT INTO pending_operations VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
              ('p1','autodrive_users',PROFILE,'{}',10,'PENDING',2,123,None,None,1,'profile:user-a'))
    c.execute("INSERT INTO pending_operations VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
              ('p2','withdrawal_requests',WITHDRAWAL,'{}',11,'IN_PROGRESS',1,999,'E','m',1,'req-a'))
    assert unresolved_count(c)==0
    migrate(c)
    rows={r[0]:r for r in c.execute('SELECT * FROM pending_operations_v15')}
    p=rows['p1']; w=rows['p2']
    # id,mutation,user,client,org,entity_type,entity_id,operation,payload,contract,created,status,attempt,retry,lease,...
    assert p[1]=='p1' and p[2:5]==('user-a','client-a','org-a') and p[6]=='user-a'
    assert p[13]==123 and p[14]==0
    assert w[1]=='req-a' and w[2:5]==('user-a','client-a','org-a') and w[6]=='req-a'
    assert w[13]==0 and w[14]==999

    bad=sqlite3.connect(':memory:'); baseline(bad)
    bad.execute("INSERT INTO pending_operations VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                ('x','x','UNKNOWN','{}',1,'PENDING',0,0,None,None,1,'x'))
    assert unresolved_count(bad)==1
    try:
        migrate(bad)
    except RuntimeError as e:
        assert str(e)=='MIGRATION_UNSCOPED_OUTBOX_ROW'
    else:
        raise AssertionError('unknown legacy row became executable')

    print('{"migrationModelPassed":true,"legacyMapped":2,"unknownOwnerFailClosed":true,"leaseSeparated":true}')

if __name__=='__main__':
    try: main()
    except Exception as e:
        print('{"migrationModelPassed":false,"error":"%s"}' % str(e).replace('"','\\"'))
        sys.exit(1)
