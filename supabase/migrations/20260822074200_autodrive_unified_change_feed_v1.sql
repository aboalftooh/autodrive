-- AutoDrive v72 — canonical unified DATA change feed, safe bootstrap and anti-entropy.
-- Append-only. COMMAND_RECEIPT and chat_recovery_seq are deliberately unrelated to this revision.

create sequence if not exists public.autodrive_data_revision_seq_v1 as bigint start with 1 increment by 1;

create table if not exists public.autodrive_sync_change_log_v1 (
    revision bigint primary key default nextval('public.autodrive_data_revision_seq_v1'),
    event_id uuid not null default gen_random_uuid() unique,
    entity_type text not null,
    entity_id uuid not null,
    operation text not null check (operation in ('UPSERT','DELETE')),
    transaction_group_id text not null,
    user_id uuid not null,
    client_id uuid not null,
    org_id uuid not null,
    payload jsonb,
    occurred_at timestamptz not null default now(),
    contract_version integer not null default 2 check (contract_version = 2)
);
create index if not exists autodrive_sync_change_log_v1_scope_revision_idx
    on public.autodrive_sync_change_log_v1(user_id, client_id, org_id, revision);
create index if not exists autodrive_sync_change_log_v1_group_revision_idx
    on public.autodrive_sync_change_log_v1(user_id, client_id, org_id, transaction_group_id, revision);

comment on table public.autodrive_sync_change_log_v1 is
'AutoDrive canonical immutable DATA_CHANGE ledger. Revision gaps are valid. Not command receipt or chat recovery ordering.';
revoke all on public.autodrive_sync_change_log_v1 from anon, authenticated;
revoke all on sequence public.autodrive_data_revision_seq_v1 from anon, authenticated;

create table if not exists public.autodrive_sync_retention_v1 (
    singleton boolean primary key default true check (singleton),
    minimum_available_revision bigint not null default 0 check (minimum_available_revision >= 0),
    retention_contract_version integer not null default 1,
    updated_at timestamptz not null default now()
);
insert into public.autodrive_sync_retention_v1(singleton) values (true) on conflict (singleton) do nothing;
revoke all on public.autodrive_sync_retention_v1 from anon, authenticated;

create table if not exists public.autodrive_sync_bootstrap_sessions_v1 (
    bootstrap_id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    client_id uuid not null,
    org_id uuid not null,
    baseline_revision bigint not null,
    contract_version integer not null default 2,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null default (now() + interval '1 hour')
);
create index if not exists autodrive_sync_bootstrap_sessions_v1_scope_idx
    on public.autodrive_sync_bootstrap_sessions_v1(user_id, client_id, org_id, expires_at);

create table if not exists public.autodrive_sync_bootstrap_rows_v1 (
    bootstrap_id uuid not null references public.autodrive_sync_bootstrap_sessions_v1(bootstrap_id) on delete cascade,
    entity_type text not null,
    entity_id uuid not null,
    payload jsonb not null,
    digest text not null,
    primary key(bootstrap_id, entity_type, entity_id)
);
revoke all on public.autodrive_sync_bootstrap_sessions_v1 from anon, authenticated;
revoke all on public.autodrive_sync_bootstrap_rows_v1 from anon, authenticated;

create or replace function public.autodrive_sync_num_v1(p_value numeric)
returns text language sql immutable as $$
    select case
        when p_value is null then null
        when position('.' in p_value::text) > 0 then regexp_replace(regexp_replace(p_value::text, '0+$', ''), '\.$', '')
        else p_value::text
    end
$$;

create or replace function public.autodrive_sync_epoch_ms_v1(p_value text)
returns text language sql immutable as $$
    select case when p_value is null or btrim(p_value) = '' then null
                else floor(extract(epoch from p_value::timestamptz) * 1000)::bigint::text end
$$;

create or replace function public.autodrive_sync_hash_parts_v1(p_parts text[])
returns text language sql immutable set search_path = public, extensions as $$
    select encode(extensions.digest(convert_to(coalesce(string_agg(
        case when x is null then '-1:' else octet_length(x)::text || ':' || x end,
        '' order by ord), ''), 'UTF8'), 'sha256'), 'hex')
    from unnest(p_parts) with ordinality as p(x, ord)
$$;

create or replace function public.autodrive_sync_sha256_text_v1(p_value text)
returns text language sql immutable set search_path = public, extensions as $$
    select encode(extensions.digest(convert_to(coalesce(p_value,''), 'UTF8'), 'sha256'), 'hex')
$$;

-- Allowlisted payload; never copies tokens/secrets or arbitrary table columns.
create or replace function public.autodrive_sync_payload_v1(p_entity_type text, r jsonb)
returns jsonb language plpgsql immutable set search_path = public as $$
begin
    case p_entity_type
      when 'autodrive_users' then return jsonb_build_object(
        'id',r->'id','user_id',r->'user_id','client_id',r->'client_id','org_id',r->'org_id',
        'account_type',r->'account_type','full_name',r->'full_name','phone',r->'phone',
        'bank_name',r->'bank_name','bank_account',r->'bank_account','workshop_name',r->'workshop_name',
        'specialty',r->'specialty','workers_count',r->'workers_count','address',r->'address',
        'onboarding_completed',r->'onboarding_completed','created_at',r->'created_at','updated_at',r->'updated_at');
      when 'invoices' then return jsonb_build_object(
        'id',r->'id','client_id',r->'client_id','commission',r->'commission','status',r->'status',
        'category',r->'category','total_amount',r->'total_amount','invoice_number',r->'invoice_number','created_at',r->'created_at');
      when 'payments' then return jsonb_build_object(
        'id',r->'id','client_id',r->'client_id','invoice_id',r->'invoice_id','amount',r->'amount','created_at',r->'created_at');
      when 'commission_payments' then return jsonb_build_object(
        'id',r->'id','client_id',r->'client_id','total_amount',r->'total_amount',
        'transaction_ref',r->'transaction_ref','invoice_ids',r->'invoice_ids','paid_at',r->'paid_at');
      when 'marketer_balance' then return jsonb_build_object(
        'id',r->'id','client_id',r->'client_id','org_id',r->'org_id','balance',r->'balance','updated_at',r->'updated_at');
      when 'balance_transactions' then return jsonb_build_object(
        'id',r->'id','client_id',r->'client_id','org_id',r->'org_id','type',r->'type','amount',r->'amount',
        'balance_before',r->'balance_before','balance_after',r->'balance_after','reference_type',r->'reference_type',
        'reference_id',r->'reference_id','note',r->'note','created_at',r->'created_at');
      when 'withdrawal_requests' then return jsonb_build_object(
        'id',r->'id','client_id',r->'client_id','org_id',r->'org_id','amount',r->'amount','status',r->'status',
        'bank_name',r->'bank_name','bank_account',r->'bank_account','transaction_ref',r->'transaction_ref',
        'note',r->'note','admin_note',r->'admin_note','requested_at',r->'requested_at','processed_at',r->'processed_at',
        'processed_by',r->'processed_by','client_request_id',r->'client_request_id');
      when 'notifications' then return jsonb_build_object(
        'id',r->'id','user_id',r->'user_id','client_id',r->'client_id','type',r->'type','title',r->'title',
        'body',r->'body','data',r->'data','is_read',r->'is_read','created_at',r->'created_at');
      when 'conversations' then return jsonb_build_object(
        'id',r->'id','org_id',r->'org_id','client_id',r->'client_id','subject',r->'subject',
        'last_message',r->'last_message','last_message_at',r->'last_message_at','marketer_unread',r->'marketer_unread','created_at',r->'created_at');
      when 'internal_messages' then return jsonb_build_object(
        'id',r->'id','org_id',r->'org_id','client_id',r->'client_id','sender_id',r->'sender_id','sender_type',r->'sender_type',
        'type',r->'type','body',r->'body','media_url',r->'media_url','media_mime',r->'media_mime',
        'media_duration_ms',r->'media_duration_ms','media_object_path',r->'media_object_path',
        'is_read',r->'is_read','created_at',r->'created_at','conversation_id',r->'conversation_id');
      else raise exception 'UNSUPPORTED_CHANGE_ENTITY:%', p_entity_type using errcode='22023';
    end case;
end $$;

create or replace function public.autodrive_sync_row_digest_v1(p_entity_type text, r jsonb)
returns text language plpgsql immutable set search_path = public, extensions as $$
declare parts text[];
begin
  case p_entity_type
    when 'autodrive_users' then parts := array[
      r->>'id',r->>'user_id',r->>'client_id',r->>'org_id',r->>'account_type',r->>'full_name',r->>'phone',
      r->>'bank_name',r->>'bank_account',r->>'workshop_name',r->>'specialty',r->>'workers_count',r->>'address',
      public.autodrive_sync_epoch_ms_v1(r->>'created_at'),public.autodrive_sync_epoch_ms_v1(r->>'updated_at')];
    when 'invoices' then parts := array[
      r->>'id',r->>'client_id',public.autodrive_sync_num_v1((r->>'commission')::numeric),r->>'status',r->>'category',
      public.autodrive_sync_num_v1((r->>'total_amount')::numeric),r->>'invoice_number',public.autodrive_sync_epoch_ms_v1(r->>'created_at')];
    when 'payments' then parts := array[
      r->>'id',r->>'client_id',r->>'invoice_id',public.autodrive_sync_num_v1((r->>'amount')::numeric),public.autodrive_sync_epoch_ms_v1(r->>'created_at')];
    when 'commission_payments' then parts := array[
      r->>'id',r->>'client_id',public.autodrive_sync_num_v1((r->>'total_amount')::numeric),r->>'transaction_ref',r->>'invoice_ids',
      public.autodrive_sync_epoch_ms_v1(r->>'paid_at')];
    when 'marketer_balance' then parts := array[
      r->>'id',r->>'client_id',public.autodrive_sync_num_v1((r->>'balance')::numeric),public.autodrive_sync_epoch_ms_v1(r->>'updated_at')];
    when 'balance_transactions' then parts := array[
      r->>'id',r->>'client_id',r->>'type',public.autodrive_sync_num_v1((r->>'amount')::numeric),
      coalesce(nullif(r->>'note',''),r->>'reference_type'),public.autodrive_sync_epoch_ms_v1(r->>'created_at')];
    when 'withdrawal_requests' then parts := array[
      r->>'id',r->>'client_id',public.autodrive_sync_num_v1((r->>'amount')::numeric),r->>'status',r->>'bank_name',r->>'bank_account',
      r->>'transaction_ref',r->>'note',public.autodrive_sync_epoch_ms_v1(r->>'requested_at'),public.autodrive_sync_epoch_ms_v1(r->>'processed_at')];
    when 'notifications' then parts := array[
      r->>'id',r->>'user_id',r->>'client_id',r->>'type',r->>'title',r->>'body',lower(r->>'is_read'),public.autodrive_sync_epoch_ms_v1(r->>'created_at')];
    when 'conversations' then parts := array[
      r->>'id',r->>'client_id',coalesce(r->>'subject',''),coalesce(r->>'last_message',''),
      coalesce(public.autodrive_sync_epoch_ms_v1(r->>'last_message_at'),'0'),coalesce(r->>'marketer_unread','0'),
      public.autodrive_sync_epoch_ms_v1(r->>'created_at')];
    when 'internal_messages' then parts := array[
      r->>'id',r->>'conversation_id',r->>'sender_id',r->>'sender_type',r->>'body',coalesce(r->>'type','TEXT'),
      lower(r->>'is_read'),public.autodrive_sync_epoch_ms_v1(r->>'created_at'),r->>'media_url',r->>'media_mime',
      r->>'media_duration_ms',r->>'media_object_path'];
    else raise exception 'UNSUPPORTED_CHANGE_ENTITY:%', p_entity_type using errcode='22023';
  end case;
  return public.autodrive_sync_hash_parts_v1(parts);
end $$;

create or replace function public.autodrive_sync_current_rows_v1(p_user uuid, p_client uuid, p_org uuid)
returns table(entity_type text, entity_id uuid, payload jsonb, digest text)
language sql stable security definer set search_path = public, extensions as $$
  select 'autodrive_users', x.id, public.autodrive_sync_payload_v1('autodrive_users',to_jsonb(x)), public.autodrive_sync_row_digest_v1('autodrive_users',to_jsonb(x))
    from public.autodrive_users x where x.user_id=p_user and x.client_id=p_client and x.org_id=p_org
  union all
  select 'invoices',x.id,public.autodrive_sync_payload_v1('invoices',to_jsonb(x)),public.autodrive_sync_row_digest_v1('invoices',to_jsonb(x))
    from public.invoices x where x.organization_id=p_org and x.client_id=p_client and x.category='SALE' and x.commission>0
  union all
  select 'payments',x.id,public.autodrive_sync_payload_v1('payments',to_jsonb(x)),public.autodrive_sync_row_digest_v1('payments',to_jsonb(x))
    from public.payments x where x.organization_id=p_org and x.client_id=p_client
  union all
  select 'commission_payments',x.id,public.autodrive_sync_payload_v1('commission_payments',to_jsonb(x)),public.autodrive_sync_row_digest_v1('commission_payments',to_jsonb(x))
    from public.commission_payments x where x.organization_id=p_org and x.client_id=p_client
  union all
  select 'marketer_balance',x.id,public.autodrive_sync_payload_v1('marketer_balance',to_jsonb(x)),public.autodrive_sync_row_digest_v1('marketer_balance',to_jsonb(x))
    from public.marketer_balance x where x.org_id=p_org and x.client_id=p_client
  union all
  select 'balance_transactions',x.id,public.autodrive_sync_payload_v1('balance_transactions',to_jsonb(x)),public.autodrive_sync_row_digest_v1('balance_transactions',to_jsonb(x))
    from public.balance_transactions x where x.org_id=p_org and x.client_id=p_client
  union all
  select 'withdrawal_requests',x.id,public.autodrive_sync_payload_v1('withdrawal_requests',to_jsonb(x)),public.autodrive_sync_row_digest_v1('withdrawal_requests',to_jsonb(x))
    from public.withdrawal_requests x where x.org_id=p_org and x.client_id=p_client
  union all
  select 'notifications',x.id,public.autodrive_sync_payload_v1('notifications',to_jsonb(x)),public.autodrive_sync_row_digest_v1('notifications',to_jsonb(x))
    from public.notifications x where x.org_id=p_org and x.client_id=p_client and x.user_id=p_user
  union all
  select 'conversations',x.id,public.autodrive_sync_payload_v1('conversations',to_jsonb(x)),public.autodrive_sync_row_digest_v1('conversations',to_jsonb(x))
    from public.conversations x where x.org_id=p_org and x.client_id=p_client
  union all
  select 'internal_messages',x.id,public.autodrive_sync_payload_v1('internal_messages',to_jsonb(x)),public.autodrive_sync_row_digest_v1('internal_messages',to_jsonb(x))
    from public.internal_messages x where x.org_id=p_org and x.client_id=p_client
$$;
revoke all on function public.autodrive_sync_current_rows_v1(uuid,uuid,uuid) from public, anon, authenticated;

create or replace function public.autodrive_sync_capture_v1()
returns trigger language plpgsql security definer set search_path = public, pg_temp, extensions as $$
declare
  r jsonb := case when tg_op='DELETE' then to_jsonb(old) else to_jsonb(new) end;
  oldr jsonb := case when tg_op='INSERT' then null else to_jsonb(old) end;
  v_type text := tg_table_name;
  v_org uuid; v_client uuid; v_user uuid; v_operation text;
  old_visible boolean := true; new_visible boolean := true;
begin
  -- Shared mutation lock: bootstrap/manifest take the exclusive counterpart without serialising normal writers.
  perform pg_advisory_xact_lock_shared(hashtextextended('autodrive-sync-v1',0));

  if v_type='autodrive_users' then
    v_org := (r->>'org_id')::uuid; v_client := (r->>'client_id')::uuid; v_user := (r->>'user_id')::uuid;
  elsif v_type in ('invoices','payments','commission_payments') then
    v_org := (r->>'organization_id')::uuid; v_client := nullif(r->>'client_id','')::uuid;
  elsif v_type in ('marketer_balance','balance_transactions','withdrawal_requests','conversations','internal_messages') then
    v_org := (r->>'org_id')::uuid; v_client := nullif(r->>'client_id','')::uuid;
  elsif v_type='notifications' then
    v_org := (r->>'org_id')::uuid; v_client := nullif(r->>'client_id','')::uuid; v_user := nullif(r->>'user_id','')::uuid;
  else
    raise exception 'UNSUPPORTED_CHANGE_ENTITY:%', v_type;
  end if;

  if v_type='invoices' then
    old_visible := oldr is not null and nullif(oldr->>'client_id','') is not null and oldr->>'category'='SALE' and coalesce((oldr->>'commission')::numeric,0)>0;
    new_visible := tg_op<>'DELETE' and nullif(to_jsonb(new)->>'client_id','') is not null and to_jsonb(new)->>'category'='SALE' and coalesce((to_jsonb(new)->>'commission')::numeric,0)>0;
    if not old_visible and not new_visible then if tg_op='DELETE' then return old; else return new; end if; end if;
    if old_visible and not new_visible then
      r := oldr; v_org := (oldr->>'organization_id')::uuid; v_client := (oldr->>'client_id')::uuid; v_operation := 'DELETE';
    else v_operation := 'UPSERT'; end if;
  else
    v_operation := case when tg_op='DELETE' then 'DELETE' else 'UPSERT' end;
  end if;

  if v_client is null or v_org is null then if tg_op='DELETE' then return old; else return new; end if; end if;
  if v_user is null then
    select au.user_id into v_user from public.autodrive_users au
     where au.client_id=v_client and au.org_id=v_org order by au.created_at desc nulls last limit 1;
  end if;
  if v_user is null then if tg_op='DELETE' then return old; else return new; end if; end if;

  insert into public.autodrive_sync_change_log_v1(
    entity_type,entity_id,operation,transaction_group_id,user_id,client_id,org_id,payload,contract_version
  ) values (
    v_type,(r->>'id')::uuid,v_operation,pg_current_xact_id()::text,v_user,v_client,v_org,
    case when v_operation='DELETE' then null else public.autodrive_sync_payload_v1(v_type,r) end,2
  );
  if tg_op='DELETE' then return old; else return new; end if;
end $$;

-- One trigger per in-scope table captures every writer path (RPC, trigger, admin/job or direct authorised DML).
do $$
declare t text;
begin
  foreach t in array array['autodrive_users','invoices','payments','commission_payments','marketer_balance',
    'balance_transactions','withdrawal_requests','notifications','conversations','internal_messages']
  loop
    execute format('drop trigger if exists autodrive_sync_capture_v1 on public.%I',t);
    execute format('create trigger autodrive_sync_capture_v1 after insert or update or delete on public.%I for each row execute function public.autodrive_sync_capture_v1()',t);
  end loop;
end $$;

create or replace function public.autodrive_sync_changes_v1(p_after_revision bigint, p_page_limit integer default 200)
returns jsonb language plpgsql security definer set search_path = public, pg_temp, extensions as $$
declare
  uid uuid := auth.uid(); cid uuid; oid uuid; minrev bigint; head bigint; cutoff bigint; nextrev bigint; more boolean; ev jsonb;
begin
  if uid is null then raise exception 'AUTH_REQUIRED' using errcode='28000'; end if;
  if p_after_revision is null or p_after_revision < 0 or p_page_limit not between 1 and 1000 then raise exception 'INVALID_SYNC_ARGUMENT' using errcode='22023'; end if;
  select client_id,org_id into cid,oid from public.autodrive_users where user_id=uid limit 1;
  if cid is null or oid is null then raise exception 'USER_NOT_REGISTERED' using errcode='42501'; end if;
  select minimum_available_revision into minrev from public.autodrive_sync_retention_v1 where singleton=true;
  select coalesce(max(revision),0) into head from public.autodrive_sync_change_log_v1;
  if p_after_revision < minrev then
    return jsonb_build_object('status','CURSOR_EXPIRED','contract_version',2,'head_revision',head,
      'minimum_available_revision',minrev,'events','[]'::jsonb,'next_revision',p_after_revision,'has_more',false);
  end if;

  select max(revision) into cutoff from (
    select revision from public.autodrive_sync_change_log_v1
     where user_id=uid and client_id=cid and org_id=oid and revision>p_after_revision
     order by revision limit p_page_limit
  ) q;
  if cutoff is null then
    return jsonb_build_object('status','OK','contract_version',2,'head_revision',head,'minimum_available_revision',minrev,
      'events','[]'::jsonb,'next_revision',head,'has_more',false);
  end if;
  select max(x.revision) into cutoff from public.autodrive_sync_change_log_v1 x
   where x.user_id=uid and x.client_id=cid and x.org_id=oid
     and x.transaction_group_id=(select transaction_group_id from public.autodrive_sync_change_log_v1 where revision=cutoff);
  select exists(select 1 from public.autodrive_sync_change_log_v1 x
    where x.user_id=uid and x.client_id=cid and x.org_id=oid and x.revision>cutoff) into more;
  nextrev := case when more then cutoff else head end;
  select coalesce(jsonb_agg(jsonb_build_object(
      'event_id',x.event_id::text,'revision',x.revision,'entity_type',x.entity_type,'entity_id',x.entity_id::text,
      'operation',x.operation,'transaction_group_id',x.transaction_group_id,'occurred_at',x.occurred_at,
      'contract_version',x.contract_version,'user_id',x.user_id::text,'client_id',x.client_id::text,'org_id',x.org_id::text,
      'payload',x.payload) order by x.revision),'[]'::jsonb) into ev
    from public.autodrive_sync_change_log_v1 x
   where x.user_id=uid and x.client_id=cid and x.org_id=oid and x.revision>p_after_revision and x.revision<=cutoff;
  return jsonb_build_object('status','OK','contract_version',2,'head_revision',head,'minimum_available_revision',minrev,
    'events',ev,'next_revision',nextrev,'has_more',more);
end $$;

create or replace function public.autodrive_sync_bootstrap_begin_v1(p_contract_version integer default 2)
returns jsonb language plpgsql security definer set search_path = public, pg_temp, extensions as $$
declare uid uuid:=auth.uid(); cid uuid; oid uuid; bid uuid; base bigint; exp timestamptz;
begin
  if uid is null then raise exception 'AUTH_REQUIRED' using errcode='28000'; end if;
  if p_contract_version<>2 then return jsonb_build_object('status','UNSUPPORTED_VERSION','bootstrap_id','','baseline_revision',0,'contract_version',2,'expires_at',now()); end if;
  select client_id,org_id into cid,oid from public.autodrive_users where user_id=uid limit 1;
  if cid is null or oid is null then raise exception 'USER_NOT_REGISTERED' using errcode='42501'; end if;
  perform pg_advisory_xact_lock(hashtextextended('autodrive-sync-v1',0));
  select coalesce(max(revision),0) into base from public.autodrive_sync_change_log_v1;
  insert into public.autodrive_sync_bootstrap_sessions_v1(user_id,client_id,org_id,baseline_revision)
    values(uid,cid,oid,base) returning bootstrap_id,expires_at into bid,exp;
  insert into public.autodrive_sync_bootstrap_rows_v1(bootstrap_id,entity_type,entity_id,payload,digest)
    select bid,entity_type,entity_id,payload,digest from public.autodrive_sync_current_rows_v1(uid,cid,oid);
  return jsonb_build_object('status','OK','bootstrap_id',bid::text,'baseline_revision',base,'contract_version',2,'expires_at',exp);
end $$;

create or replace function public.autodrive_sync_bootstrap_page_v1(p_bootstrap_id uuid, p_after_token text default null, p_page_limit integer default 500)
returns jsonb language plpgsql security definer set search_path = public, pg_temp, extensions as $$
declare uid uuid:=auth.uid(); s public.autodrive_sync_bootstrap_sessions_v1%rowtype; rows_json jsonb; next_token text; more boolean;
begin
  if uid is null then raise exception 'AUTH_REQUIRED' using errcode='28000'; end if;
  if p_page_limit not between 1 and 1000 then raise exception 'INVALID_SYNC_ARGUMENT' using errcode='22023'; end if;
  select * into s from public.autodrive_sync_bootstrap_sessions_v1 where bootstrap_id=p_bootstrap_id and user_id=uid;
  if not found then raise exception 'BOOTSTRAP_NOT_FOUND' using errcode='42501'; end if;
  if s.expires_at<=now() then
    return jsonb_build_object('status','BOOTSTRAP_EXPIRED','bootstrap_id',p_bootstrap_id::text,'contract_version',2,
      'rows','[]'::jsonb,'next_page_token',null,'has_more',false);
  end if;
  with page as (
    select *, entity_type||'|'||entity_id::text token from public.autodrive_sync_bootstrap_rows_v1
     where bootstrap_id=p_bootstrap_id and (p_after_token is null or entity_type||'|'||entity_id::text>p_after_token)
     order by entity_type,entity_id limit p_page_limit
  ) select coalesce(jsonb_agg(jsonb_build_object('entity_type',entity_type,'entity_id',entity_id::text,'payload',payload,'digest',digest)
      order by entity_type,entity_id),'[]'::jsonb), max(token) into rows_json,next_token from page;
  select exists(select 1 from public.autodrive_sync_bootstrap_rows_v1
    where bootstrap_id=p_bootstrap_id and next_token is not null and entity_type||'|'||entity_id::text>next_token) into more;
  return jsonb_build_object('status','OK','bootstrap_id',p_bootstrap_id::text,'contract_version',2,'rows',rows_json,
    'next_page_token',case when more then next_token else null end,'has_more',more);
end $$;

create or replace function public.autodrive_sync_manifest_v1(p_contract_version integer default 1)
returns jsonb language plpgsql security definer set search_path = public, pg_temp, extensions as $$
declare uid uuid:=auth.uid(); cid uuid; oid uuid; rev bigint; parts jsonb;
begin
  if uid is null then raise exception 'AUTH_REQUIRED' using errcode='28000'; end if;
  if p_contract_version<>1 then return jsonb_build_object('status','UNSUPPORTED_VERSION','contract_version',1,'manifest_revision',0,'partitions','[]'::jsonb); end if;
  select client_id,org_id into cid,oid from public.autodrive_users where user_id=uid limit 1;
  if cid is null or oid is null then raise exception 'USER_NOT_REGISTERED' using errcode='42501'; end if;
  perform pg_advisory_xact_lock(hashtextextended('autodrive-sync-v1',0));
  select coalesce(max(revision),0) into rev from public.autodrive_sync_change_log_v1;
  with r as (
    select *, left(public.autodrive_sync_sha256_text_v1(entity_id::text),2) partition
      from public.autodrive_sync_current_rows_v1(uid,cid,oid)
  ), p as (
    select entity_type,partition,count(*)::int count,
      public.autodrive_sync_sha256_text_v1(string_agg(entity_id::text||E'\t'||digest||E'\n','' order by entity_id)) digest
    from r group by entity_type,partition
  ) select coalesce(jsonb_agg(jsonb_build_object('entity_type',entity_type,'partition',partition,'count',count,'digest',digest)
      order by entity_type,partition),'[]'::jsonb) into parts from p;
  return jsonb_build_object('status','OK','contract_version',1,'manifest_revision',rev,'partitions',parts);
end $$;

create or replace function public.autodrive_sync_partition_v1(p_manifest_revision bigint, p_entity_type text, p_partition text)
returns jsonb language plpgsql security definer set search_path = public, pg_temp, extensions as $$
declare uid uuid:=auth.uid(); cid uuid; oid uuid; rev bigint; rows_json jsonb;
begin
  if uid is null then raise exception 'AUTH_REQUIRED' using errcode='28000'; end if;
  if p_entity_type not in ('autodrive_users','invoices','payments','commission_payments','marketer_balance','balance_transactions','withdrawal_requests','notifications','conversations','internal_messages')
     or p_partition !~ '^[0-9a-f]{2}$' then raise exception 'INVALID_PARTITION_ARGUMENT' using errcode='22023'; end if;
  select client_id,org_id into cid,oid from public.autodrive_users where user_id=uid limit 1;
  if cid is null or oid is null then raise exception 'USER_NOT_REGISTERED' using errcode='42501'; end if;
  perform pg_advisory_xact_lock(hashtextextended('autodrive-sync-v1',0));
  select coalesce(max(revision),0) into rev from public.autodrive_sync_change_log_v1;
  if rev<>p_manifest_revision then
    return jsonb_build_object('status','STALE_MANIFEST','contract_version',1,'manifest_revision',rev,
      'entity_type',p_entity_type,'partition',p_partition,'rows','[]'::jsonb);
  end if;
  select coalesce(jsonb_agg(jsonb_build_object('entity_id',entity_id::text,'digest',digest,'payload',payload) order by entity_id),'[]'::jsonb)
    into rows_json from public.autodrive_sync_current_rows_v1(uid,cid,oid)
   where entity_type=p_entity_type and left(public.autodrive_sync_sha256_text_v1(entity_id::text),2)=p_partition;
  return jsonb_build_object('status','OK','contract_version',1,'manifest_revision',rev,'entity_type',p_entity_type,
    'partition',p_partition,'rows',rows_json);
end $$;

-- Server-owned retention operation. It is intentionally not executable by authenticated clients.
create or replace function public.autodrive_sync_prune_changes_v1(p_through_revision bigint)
returns bigint language plpgsql security definer set search_path = public, pg_temp as $$
declare n bigint;
begin
  if p_through_revision is null or p_through_revision<0 then raise exception 'INVALID_REVISION'; end if;
  delete from public.autodrive_sync_change_log_v1 where revision<=p_through_revision;
  get diagnostics n = row_count;
  update public.autodrive_sync_retention_v1
     set minimum_available_revision=greatest(minimum_available_revision,p_through_revision),updated_at=now()
   where singleton=true;
  return n;
end $$;

-- No direct ledger/bootstrap table access from API roles; only authenticated scoped RPCs.
revoke all on function public.autodrive_sync_capture_v1() from public, anon, authenticated;
revoke all on function public.autodrive_sync_prune_changes_v1(bigint) from public, anon, authenticated;
revoke all on function public.autodrive_sync_payload_v1(text,jsonb) from public, anon, authenticated;
revoke all on function public.autodrive_sync_row_digest_v1(text,jsonb) from public, anon, authenticated;
revoke all on function public.autodrive_sync_hash_parts_v1(text[]) from public, anon, authenticated;
revoke all on function public.autodrive_sync_sha256_text_v1(text) from public, anon, authenticated;
revoke all on function public.autodrive_sync_num_v1(numeric) from public, anon, authenticated;
revoke all on function public.autodrive_sync_epoch_ms_v1(text) from public, anon, authenticated;

grant execute on function public.autodrive_sync_changes_v1(bigint,integer) to authenticated;
grant execute on function public.autodrive_sync_bootstrap_begin_v1(integer) to authenticated;
grant execute on function public.autodrive_sync_bootstrap_page_v1(uuid,text,integer) to authenticated;
grant execute on function public.autodrive_sync_manifest_v1(integer) to authenticated;
grant execute on function public.autodrive_sync_partition_v1(bigint,text,text) to authenticated;

comment on function public.autodrive_sync_changes_v1(bigint,integer) is
'Scoped AutoDrive DATA_CHANGE feed. auth.uid() derives user/client/org. Groups are never split; revision gaps are valid.';
comment on function public.autodrive_sync_bootstrap_begin_v1(integer) is
'Materializes a stable scoped bootstrap under an exclusive sync snapshot lock and binds it to baseline DATA revision.';
comment on function public.autodrive_sync_manifest_v1(integer) is
'Versioned SHA-256 anti-entropy manifest over canonical server projection; local-only fields are excluded.';
