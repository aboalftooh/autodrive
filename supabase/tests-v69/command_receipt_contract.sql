-- AutoDrive v69 executable PostgreSQL contract test. Run after applying the v69 migration.
-- It tests the receipt ledger without requiring production business fixtures.
begin;
do $$
declare
  u uuid := '00000000-0000-0000-0000-000000000069';
  c uuid := '00000000-0000-0000-0000-000000000169';
  o uuid := '00000000-0000-0000-0000-000000000269';
  m text := 'v69-test-mutation';
  f1 text := public.autodrive_command_fingerprint_v1('{"x":1}'::jsonb);
  f2 text := public.autodrive_command_fingerprint_v1('{"x":2}'::jsonb);
  r1 jsonb; r2 jsonb; rc jsonb;
begin
  r1 := public.autodrive_command_store_receipt_v1(u,c,o,m,'UPDATE_PROFILE','autodrive_users',u::text,f1,'APPLIED',u::text);
  r2 := public.autodrive_command_existing_or_conflict_v1(u,c,o,m,'UPDATE_PROFILE',f1);
  rc := public.autodrive_command_existing_or_conflict_v1(u,c,o,m,'UPDATE_PROFILE',f2);
  if r1->>'result_status' <> 'APPLIED' then raise exception 'v69 test: first receipt not APPLIED'; end if;
  if coalesce((r2->>'replayed')::boolean,false) is not true then raise exception 'v69 test: replay flag false'; end if;
  if r2->>'server_revision' <> r1->>'server_revision' then raise exception 'v69 test: replay revision changed'; end if;
  if rc->>'result_status' <> 'CONFLICT' or rc->>'error_code' <> 'MUTATION_ID_REUSE_CONFLICT' then raise exception 'v69 test: changed fingerprint did not conflict'; end if;
  if (select count(*) from public.autodrive_command_receipts where user_id=u and client_id=c and org_id=o and mutation_id=m) <> 1 then raise exception 'v69 test: duplicate receipt row'; end if;
end $$;
rollback;
