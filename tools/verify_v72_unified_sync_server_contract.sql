-- Run after deploying the v72 migration on the authoritative Supabase target.
-- This script is intentionally fail-closed and does not mutate business data.
\set ON_ERROR_STOP on

DO $$
DECLARE missing text;
BEGIN
  SELECT string_agg(x, ', ') INTO missing
  FROM unnest(ARRAY[
    'autodrive_sync_change_log_v1','autodrive_sync_retention_v1',
    'autodrive_sync_bootstrap_sessions_v1','autodrive_sync_bootstrap_rows_v1'
  ]) x WHERE to_regclass('public.'||x) IS NULL;
  IF missing IS NOT NULL THEN RAISE EXCEPTION 'V72_MISSING_TABLES:%',missing; END IF;
  IF to_regclass('public.autodrive_data_revision_seq_v1') IS NULL THEN RAISE EXCEPTION 'V72_DATA_REVISION_SEQUENCE_MISSING'; END IF;
END $$;

DO $$
DECLARE fn text;
BEGIN
  FOREACH fn IN ARRAY ARRAY[
    'autodrive_sync_changes_v1','autodrive_sync_bootstrap_begin_v1','autodrive_sync_bootstrap_page_v1',
    'autodrive_sync_manifest_v1','autodrive_sync_partition_v1','autodrive_sync_capture_v1'
  ] LOOP
    IF NOT EXISTS (SELECT 1 FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname='public' AND p.proname=fn)
      THEN RAISE EXCEPTION 'V72_MISSING_FUNCTION:%',fn; END IF;
  END LOOP;
END $$;

-- All ten in-scope tables must have the canonical capture trigger.
DO $$
DECLARE t text;
BEGIN
  FOREACH t IN ARRAY ARRAY['autodrive_users','invoices','payments','commission_payments','marketer_balance',
    'balance_transactions','withdrawal_requests','notifications','conversations','internal_messages'] LOOP
    IF NOT EXISTS (
      SELECT 1 FROM pg_trigger g JOIN pg_class c ON c.oid=g.tgrelid JOIN pg_namespace n ON n.oid=c.relnamespace
      WHERE n.nspname='public' AND c.relname=t AND g.tgname='autodrive_sync_capture_v1' AND NOT g.tgisinternal
    ) THEN RAISE EXCEPTION 'V72_CHANGE_CAPTURE_GAP:%',t; END IF;
  END LOOP;
END $$;

-- Authenticated must not have raw ledger/table SELECT.
DO $$
BEGIN
  IF has_table_privilege('authenticated','public.autodrive_sync_change_log_v1','SELECT') THEN
    RAISE EXCEPTION 'V72_RAW_LEDGER_EXPOSED';
  END IF;
  IF has_table_privilege('authenticated','public.autodrive_sync_bootstrap_rows_v1','SELECT') THEN
    RAISE EXCEPTION 'V72_RAW_BOOTSTRAP_EXPOSED';
  END IF;
END $$;

-- Revision sequences must be distinct objects.
DO $$
BEGIN
  IF to_regclass('public.autodrive_command_receipt_revision_seq') IS NOT NULL
     AND to_regclass('public.autodrive_command_receipt_revision_seq') = to_regclass('public.autodrive_data_revision_seq_v1') THEN
    RAISE EXCEPTION 'V72_RECEIPT_DATA_REVISION_COLLISION';
  END IF;
END $$;

SELECT 'V72_SERVER_SCHEMA_CONTRACT_STATIC_PASS' AS result;
-- Positive/negative authenticated RPC, transaction-group, rollback, expiry, bootstrap no-gap,
-- manifest determinism and targeted-repair tests require test identities/fixtures on the deployed target.
