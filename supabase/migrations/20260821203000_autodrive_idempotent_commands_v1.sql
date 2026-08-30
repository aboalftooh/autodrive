-- AutoDrive v69 — unified idempotent server command contract.
-- Source authority: current PostgreSQL 17 schema dump dated 2026-08-20.
-- Append-only migration. No historical migration is modified.

begin;

create schema if not exists extensions;
create extension if not exists pgcrypto with schema extensions;

create sequence if not exists public.autodrive_command_receipt_revision_seq;

create table if not exists public.autodrive_command_receipts (
    user_id uuid not null,
    client_id uuid not null,
    org_id uuid not null,
    mutation_id text not null,
    command_type text not null,
    entity_type text not null,
    entity_id text,
    request_fingerprint text not null,
    result_status text not null,
    server_entity_id text,
    receipt_revision bigint not null default nextval('public.autodrive_command_receipt_revision_seq'::regclass),
    revision_kind text not null default 'COMMAND_RECEIPT',
    error_code text,
    server_created_at_ms bigint,
    result_count integer,
    created_at timestamptz not null default now(),
    primary key (user_id, client_id, org_id, mutation_id),
    constraint autodrive_command_receipts_command_type_check check (command_type = any (array[
        'UPDATE_PROFILE'::text,
        'REQUEST_WITHDRAWAL'::text,
        'SEND_CHAT_MESSAGE'::text,
        'MARK_CHAT_READ'::text,
        'MARK_NOTIFICATION_READ'::text,
        'REGISTER_PUSH_TOKEN'::text,
        'REVOKE_PUSH_TOKEN'::text,
        'CANCEL_PENDING_WITHDRAWALS'::text
    ])),
    constraint autodrive_command_receipts_result_status_check check (result_status = any (array[
        'APPLIED'::text, 'REJECTED'::text, 'CONFLICT'::text
    ])),
    constraint autodrive_command_receipts_revision_kind_check check (revision_kind = 'COMMAND_RECEIPT'),
    constraint autodrive_command_receipts_mutation_id_check check (char_length(btrim(mutation_id)) between 1 and 128),
    constraint autodrive_command_receipts_fingerprint_check check (request_fingerprint ~ '^[0-9a-f]{64}$')
);

comment on table public.autodrive_command_receipts is
'AutoDrive durable command receipts. No automatic cleanup is installed: receipts are retained indefinitely until a future migration proves a safe offline/retry horizon.';
comment on column public.autodrive_command_receipts.receipt_revision is
'Command-receipt sequence only; MUST NOT be used as the global sync/change-feed cursor.';
comment on column public.autodrive_command_receipts.request_fingerprint is
'Server-computed SHA-256 over canonical command input. Raw sensitive command payloads are never stored.';

create index if not exists autodrive_command_receipts_created_idx
    on public.autodrive_command_receipts(created_at);
create index if not exists autodrive_command_receipts_scope_revision_idx
    on public.autodrive_command_receipts(user_id, client_id, org_id, receipt_revision);

alter table public.autodrive_command_receipts enable row level security;
revoke all on table public.autodrive_command_receipts from public, anon, authenticated;
revoke all on sequence public.autodrive_command_receipt_revision_seq from public, anon, authenticated;

create or replace function public.autodrive_command_receipt_json_v1(
    p_receipt public.autodrive_command_receipts,
    p_replayed boolean
) returns jsonb
language sql
stable
set search_path = public, pg_temp
as $$
    select jsonb_strip_nulls(jsonb_build_object(
        'mutation_id', p_receipt.mutation_id,
        'command_type', p_receipt.command_type,
        'result_status', p_receipt.result_status,
        'server_entity_id', p_receipt.server_entity_id,
        'server_revision', p_receipt.receipt_revision,
        'revision_kind', p_receipt.revision_kind,
        'replayed', p_replayed,
        'error_code', p_receipt.error_code,
        'server_created_at_ms', p_receipt.server_created_at_ms,
        'result_count', p_receipt.result_count
    ));
$$;

create or replace function public.autodrive_command_scope_v1()
returns table(user_id uuid, client_id uuid, org_id uuid)
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_user_id uuid := auth.uid();
begin
    if v_user_id is null then
        raise exception 'AUTH_REQUIRED' using errcode = '28000';
    end if;

    return query
    select au.user_id, au.client_id, au.org_id
    from public.autodrive_users au
    where au.user_id = v_user_id
    limit 1;

    if not found then
        raise exception 'USER_NOT_REGISTERED' using errcode = 'P0001';
    end if;
end;
$$;

create or replace function public.autodrive_command_fingerprint_v1(p_payload jsonb)
returns text
language sql
immutable
strict
set search_path = pg_catalog, extensions
as $$
    select encode(extensions.digest(convert_to(p_payload::text, 'UTF8'), 'sha256'), 'hex');
$$;

create or replace function public.autodrive_command_validate_mutation_v1(p_mutation_id text)
returns void
language plpgsql
immutable
set search_path = pg_catalog
as $$
begin
    if p_mutation_id is null or btrim(p_mutation_id) = '' or char_length(p_mutation_id) > 128 then
        raise exception 'INVALID_MUTATION_ID' using errcode = '22023';
    end if;
end;
$$;

create or replace function public.autodrive_command_existing_or_conflict_v1(
    p_user_id uuid,
    p_client_id uuid,
    p_org_id uuid,
    p_mutation_id text,
    p_command_type text,
    p_request_fingerprint text
) returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_receipt public.autodrive_command_receipts%rowtype;
begin
    select * into v_receipt
    from public.autodrive_command_receipts r
    where r.user_id = p_user_id
      and r.client_id = p_client_id
      and r.org_id = p_org_id
      and r.mutation_id = p_mutation_id;

    if not found then
        return null;
    end if;

    if v_receipt.command_type = p_command_type
       and v_receipt.request_fingerprint = p_request_fingerprint then
        return public.autodrive_command_receipt_json_v1(v_receipt, true);
    end if;

    return jsonb_strip_nulls(jsonb_build_object(
        'mutation_id', p_mutation_id,
        'command_type', p_command_type,
        'result_status', 'CONFLICT',
        'server_entity_id', v_receipt.server_entity_id,
        'server_revision', v_receipt.receipt_revision,
        'revision_kind', 'COMMAND_RECEIPT',
        'replayed', true,
        'error_code', 'MUTATION_ID_REUSE_CONFLICT'
    ));
end;
$$;

create or replace function public.autodrive_command_store_receipt_v1(
    p_user_id uuid,
    p_client_id uuid,
    p_org_id uuid,
    p_mutation_id text,
    p_command_type text,
    p_entity_type text,
    p_entity_id text,
    p_request_fingerprint text,
    p_result_status text,
    p_server_entity_id text default null,
    p_error_code text default null,
    p_server_created_at_ms bigint default null,
    p_result_count integer default null
) returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_receipt public.autodrive_command_receipts%rowtype;
begin
    insert into public.autodrive_command_receipts(
        user_id, client_id, org_id, mutation_id, command_type,
        entity_type, entity_id, request_fingerprint, result_status,
        server_entity_id, error_code, server_created_at_ms, result_count
    ) values (
        p_user_id, p_client_id, p_org_id, p_mutation_id, p_command_type,
        p_entity_type, p_entity_id, p_request_fingerprint, p_result_status,
        p_server_entity_id, p_error_code, p_server_created_at_ms, p_result_count
    )
    returning * into v_receipt;

    return public.autodrive_command_receipt_json_v1(v_receipt, false);
end;
$$;

-- UPDATE_PROFILE -------------------------------------------------------------
create or replace function public.autodrive_update_profile_command_v1(
    p_mutation_id text,
    p_full_name text default null,
    p_phone text default null,
    p_bank_name text default null,
    p_bank_account text default null,
    p_workshop_name text default null,
    p_specialty text default null,
    p_workers_count integer default null,
    p_address text default null
) returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_scope record;
    v_fingerprint text;
    v_existing jsonb;
    v_entity_id uuid;
begin
    perform public.autodrive_command_validate_mutation_v1(p_mutation_id);
    select * into v_scope from public.autodrive_command_scope_v1();
    perform pg_advisory_xact_lock(hashtextextended(
        'autodrive-command:' || v_scope.user_id::text || ':' || v_scope.client_id::text || ':' || v_scope.org_id::text || ':' || p_mutation_id,
        0
    ));

    v_fingerprint := public.autodrive_command_fingerprint_v1(jsonb_build_object(
        'full_name', p_full_name, 'phone', p_phone,
        'bank_name', p_bank_name, 'bank_account', p_bank_account,
        'workshop_name', p_workshop_name, 'specialty', p_specialty,
        'workers_count', p_workers_count, 'address', p_address
    ));
    v_existing := public.autodrive_command_existing_or_conflict_v1(
        v_scope.user_id, v_scope.client_id, v_scope.org_id,
        p_mutation_id, 'UPDATE_PROFILE', v_fingerprint
    );
    if v_existing is not null then return v_existing; end if;

    update public.autodrive_users
    set full_name = coalesce(p_full_name, full_name),
        phone = coalesce(p_phone, phone),
        bank_name = coalesce(p_bank_name, bank_name),
        bank_account = coalesce(p_bank_account, bank_account),
        workshop_name = coalesce(p_workshop_name, workshop_name),
        specialty = coalesce(p_specialty, specialty),
        workers_count = coalesce(p_workers_count, workers_count),
        address = coalesce(p_address, address)
    where user_id = v_scope.user_id
      and client_id = v_scope.client_id
      and org_id = v_scope.org_id
    returning user_id into v_entity_id;

    if v_entity_id is null then
        return public.autodrive_command_store_receipt_v1(
            v_scope.user_id, v_scope.client_id, v_scope.org_id,
            p_mutation_id, 'UPDATE_PROFILE', 'autodrive_users', v_scope.user_id::text,
            v_fingerprint, 'REJECTED', null, 'TARGET_NOT_FOUND_OR_FORBIDDEN'
        );
    end if;

    return public.autodrive_command_store_receipt_v1(
        v_scope.user_id, v_scope.client_id, v_scope.org_id,
        p_mutation_id, 'UPDATE_PROFILE', 'autodrive_users', v_scope.user_id::text,
        v_fingerprint, 'APPLIED', v_scope.user_id::text
    );
end;
$$;

-- REQUEST_WITHDRAWAL ---------------------------------------------------------
create or replace function public.autodrive_request_withdrawal_command_v1(
    p_mutation_id text,
    p_amount numeric,
    p_note text default ''
) returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_scope record;
    v_fingerprint text;
    v_existing jsonb;
    v_request_id uuid;
    v_existing_request public.withdrawal_requests%rowtype;
    v_bank_name text;
    v_bank_account text;
    v_balance numeric;
    v_pending_total numeric;
    v_available numeric;
begin
    perform public.autodrive_command_validate_mutation_v1(p_mutation_id);
    select * into v_scope from public.autodrive_command_scope_v1();

    -- The first lock protects the globally-unique legacy client_request_id. The second serializes
    -- withdrawal eligibility for one marketer so two different mutations cannot race the pending check.
    perform pg_advisory_xact_lock(hashtextextended('autodrive-withdrawal:' || p_mutation_id, 0));
    perform pg_advisory_xact_lock(hashtextextended('autodrive-withdrawal-client:' || v_scope.client_id::text, 0));
    perform pg_advisory_xact_lock(hashtextextended(
        'autodrive-command:' || v_scope.user_id::text || ':' || v_scope.client_id::text || ':' || v_scope.org_id::text || ':' || p_mutation_id,
        0
    ));

    v_fingerprint := public.autodrive_command_fingerprint_v1(jsonb_build_object(
        'amount', p_amount,
        'note', coalesce(p_note, '')
    ));
    v_existing := public.autodrive_command_existing_or_conflict_v1(
        v_scope.user_id, v_scope.client_id, v_scope.org_id,
        p_mutation_id, 'REQUEST_WITHDRAWAL', v_fingerprint
    );
    if v_existing is not null then return v_existing; end if;

    -- Upgrade compatibility: a v68/legacy caller may already have committed the target row before
    -- v69 receipts existed. The target row is authoritative evidence for this exact mutation.
    select * into v_existing_request
    from public.withdrawal_requests wr
    where wr.client_request_id = p_mutation_id;
    if found then
        if v_existing_request.client_id <> v_scope.client_id
           or v_existing_request.org_id <> v_scope.org_id
           or v_existing_request.amount <> p_amount
           or coalesce(v_existing_request.note, '') <> coalesce(p_note, '') then
            return jsonb_build_object(
                'mutation_id', p_mutation_id,
                'command_type', 'REQUEST_WITHDRAWAL',
                'result_status', 'CONFLICT',
                'server_entity_id', null,
                'server_revision', 0,
                'revision_kind', 'COMMAND_RECEIPT',
                'replayed', true,
                'error_code', 'MUTATION_ID_REUSE_CONFLICT'
            );
        end if;
        return public.autodrive_command_store_receipt_v1(
            v_scope.user_id, v_scope.client_id, v_scope.org_id,
            p_mutation_id, 'REQUEST_WITHDRAWAL', 'withdrawal_requests', p_mutation_id,
            v_fingerprint, 'APPLIED', v_existing_request.id::text
        );
    end if;

    -- These checks deliberately mirror the authoritative 2026-08-20 request_withdrawal contract,
    -- but return typed durable receipts instead of interpreting exception message text.
    if p_amount is null or p_amount <= 0 then
        return public.autodrive_command_store_receipt_v1(
            v_scope.user_id, v_scope.client_id, v_scope.org_id,
            p_mutation_id, 'REQUEST_WITHDRAWAL', 'withdrawal_requests', p_mutation_id,
            v_fingerprint, 'REJECTED', null, 'INVALID_AMOUNT'
        );
    end if;

    select au.bank_name, au.bank_account
    into v_bank_name, v_bank_account
    from public.autodrive_users au
    where au.user_id = v_scope.user_id
      and au.client_id = v_scope.client_id
      and au.org_id = v_scope.org_id
    for update;

    if v_bank_name is null or btrim(v_bank_name) = ''
       or v_bank_account is null or btrim(v_bank_account) = '' then
        return public.autodrive_command_store_receipt_v1(
            v_scope.user_id, v_scope.client_id, v_scope.org_id,
            p_mutation_id, 'REQUEST_WITHDRAWAL', 'withdrawal_requests', p_mutation_id,
            v_fingerprint, 'REJECTED', null, 'BANK_DETAILS_MISSING'
        );
    end if;

    if exists (
        select 1 from public.withdrawal_requests wr
        where wr.client_id = v_scope.client_id and wr.status = 'PENDING'
    ) then
        return public.autodrive_command_store_receipt_v1(
            v_scope.user_id, v_scope.client_id, v_scope.org_id,
            p_mutation_id, 'REQUEST_WITHDRAWAL', 'withdrawal_requests', p_mutation_id,
            v_fingerprint, 'REJECTED', null, 'PENDING_REQUEST_EXISTS'
        );
    end if;

    select coalesce(mb.balance, 0)
    into v_balance
    from public.marketer_balance mb
    where mb.client_id = v_scope.client_id
    for update;
    v_balance := coalesce(v_balance, 0);

    select coalesce(sum(wr.amount), 0)
    into v_pending_total
    from public.withdrawal_requests wr
    where wr.client_id = v_scope.client_id
      and wr.status in ('PENDING', 'APPROVED');
    v_available := v_balance - v_pending_total;

    if p_amount > v_available then
        return public.autodrive_command_store_receipt_v1(
            v_scope.user_id, v_scope.client_id, v_scope.org_id,
            p_mutation_id, 'REQUEST_WITHDRAWAL', 'withdrawal_requests', p_mutation_id,
            v_fingerprint, 'REJECTED', null, 'INSUFFICIENT_BALANCE'
        );
    end if;

    begin
        insert into public.withdrawal_requests(
            client_id, org_id, amount, status,
            bank_name, bank_account, note, client_request_id
        ) values (
            v_scope.client_id, v_scope.org_id, p_amount, 'PENDING',
            v_bank_name, v_bank_account, nullif(p_note, ''), p_mutation_id
        ) returning id into v_request_id;
    exception when unique_violation then
        -- Structured SQLSTATE handling only. A legacy concurrent caller can win the client_request_id
        -- race; reconcile the canonical row without parsing constraint-message text.
        select wr.id into v_request_id
        from public.withdrawal_requests wr
        where wr.client_request_id = p_mutation_id
          and wr.client_id = v_scope.client_id
          and wr.org_id = v_scope.org_id
          and wr.amount = p_amount
          and coalesce(wr.note, '') = coalesce(p_note, '');
        if v_request_id is null then raise; end if;
    end;

    return public.autodrive_command_store_receipt_v1(
        v_scope.user_id, v_scope.client_id, v_scope.org_id,
        p_mutation_id, 'REQUEST_WITHDRAWAL', 'withdrawal_requests', p_mutation_id,
        v_fingerprint, 'APPLIED', v_request_id::text
    );
end;
$$;

-- SEND_CHAT_MESSAGE ----------------------------------------------------------
create or replace function public.autodrive_send_chat_message_command_v1(
    p_mutation_id text,
    p_message_id uuid,
    p_conversation_id uuid,
    p_type text,
    p_body text,
    p_media_url text default null,
    p_media_mime text default null,
    p_media_duration_ms bigint default null
) returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_scope record;
    v_fingerprint text;
    v_existing jsonb;
    v_row public.internal_messages%rowtype;
    v_created_ms bigint;
begin
    perform public.autodrive_command_validate_mutation_v1(p_mutation_id);
    if p_message_id::text <> p_mutation_id then
        raise exception 'MESSAGE_MUTATION_ID_MISMATCH' using errcode = '22023';
    end if;
    select * into v_scope from public.autodrive_command_scope_v1();
    perform pg_advisory_xact_lock(hashtextextended('autodrive-chat:' || p_message_id::text, 0));
    perform pg_advisory_xact_lock(hashtextextended(
        'autodrive-command:' || v_scope.user_id::text || ':' || v_scope.client_id::text || ':' || v_scope.org_id::text || ':' || p_mutation_id,
        0
    ));

    v_fingerprint := public.autodrive_command_fingerprint_v1(jsonb_build_object(
        'message_id', p_message_id,
        'conversation_id', p_conversation_id,
        'sender_id', v_scope.user_id,
        'type', p_type,
        'body', p_body,
        'media_url', p_media_url,
        'media_mime', p_media_mime,
        'media_duration_ms', p_media_duration_ms
    ));
    v_existing := public.autodrive_command_existing_or_conflict_v1(
        v_scope.user_id, v_scope.client_id, v_scope.org_id,
        p_mutation_id, 'SEND_CHAT_MESSAGE', v_fingerprint
    );
    if v_existing is not null then return v_existing; end if;

    if not exists (
        select 1 from public.conversations c
        where c.id = p_conversation_id
          and c.client_id = v_scope.client_id
          and c.org_id = v_scope.org_id
    ) then
        return public.autodrive_command_store_receipt_v1(
            v_scope.user_id, v_scope.client_id, v_scope.org_id,
            p_mutation_id, 'SEND_CHAT_MESSAGE', 'internal_messages', p_message_id::text,
            v_fingerprint, 'REJECTED', null, 'TARGET_NOT_FOUND_OR_FORBIDDEN'
        );
    end if;

    insert into public.internal_messages(
        id, org_id, client_id, sender_id, sender_type, body,
        is_read, conversation_id, type, media_url, media_mime, media_duration_ms
    ) values (
        p_message_id, v_scope.org_id, v_scope.client_id, v_scope.user_id, 'MARKETER', p_body,
        false, p_conversation_id, p_type, p_media_url, p_media_mime, p_media_duration_ms
    )
    on conflict (id) do nothing;

    select * into v_row from public.internal_messages m where m.id = p_message_id;
    if not found
       or v_row.org_id <> v_scope.org_id
       or v_row.client_id <> v_scope.client_id
       or v_row.sender_id <> v_scope.user_id
       or v_row.sender_type <> 'MARKETER'
       or v_row.conversation_id is distinct from p_conversation_id
       or v_row.type <> p_type
       or v_row.body <> p_body
       or v_row.media_url is distinct from p_media_url
       or v_row.media_mime is distinct from p_media_mime
       or v_row.media_duration_ms is distinct from p_media_duration_ms then
        return jsonb_build_object(
            'mutation_id', p_mutation_id,
            'command_type', 'SEND_CHAT_MESSAGE',
            'result_status', 'CONFLICT',
            'server_entity_id', null,
            'server_revision', 0,
            'revision_kind', 'COMMAND_RECEIPT',
            'replayed', true,
            'error_code', 'MUTATION_ID_REUSE_CONFLICT'
        );
    end if;

    v_created_ms := floor(extract(epoch from v_row.created_at) * 1000)::bigint;
    return public.autodrive_command_store_receipt_v1(
        v_scope.user_id, v_scope.client_id, v_scope.org_id,
        p_mutation_id, 'SEND_CHAT_MESSAGE', 'internal_messages', p_message_id::text,
        v_fingerprint, 'APPLIED', p_message_id::text, null, v_created_ms
    );
end;
$$;

-- MARK_CHAT_READ -------------------------------------------------------------
create or replace function public.autodrive_mark_chat_read_command_v1(
    p_mutation_id text,
    p_conversation_id uuid
) returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_scope record;
    v_fingerprint text;
    v_existing jsonb;
begin
    perform public.autodrive_command_validate_mutation_v1(p_mutation_id);
    select * into v_scope from public.autodrive_command_scope_v1();
    perform pg_advisory_xact_lock(hashtextextended(
        'autodrive-command:' || v_scope.user_id::text || ':' || v_scope.client_id::text || ':' || v_scope.org_id::text || ':' || p_mutation_id,
        0
    ));
    v_fingerprint := public.autodrive_command_fingerprint_v1(jsonb_build_object('conversation_id', p_conversation_id));
    v_existing := public.autodrive_command_existing_or_conflict_v1(
        v_scope.user_id, v_scope.client_id, v_scope.org_id,
        p_mutation_id, 'MARK_CHAT_READ', v_fingerprint
    );
    if v_existing is not null then return v_existing; end if;

    if not exists (
        select 1 from public.conversations c
        where c.id = p_conversation_id
          and c.client_id = v_scope.client_id
          and c.org_id = v_scope.org_id
    ) then
        return public.autodrive_command_store_receipt_v1(
            v_scope.user_id, v_scope.client_id, v_scope.org_id,
            p_mutation_id, 'MARK_CHAT_READ', 'internal_messages', p_conversation_id::text,
            v_fingerprint, 'REJECTED', null, 'TARGET_NOT_FOUND_OR_FORBIDDEN'
        );
    end if;

    update public.internal_messages
    set is_read = true
    where conversation_id = p_conversation_id
      and client_id = v_scope.client_id
      and org_id = v_scope.org_id
      and sender_type = 'ADMIN'
      and is_read = false;

    return public.autodrive_command_store_receipt_v1(
        v_scope.user_id, v_scope.client_id, v_scope.org_id,
        p_mutation_id, 'MARK_CHAT_READ', 'internal_messages', p_conversation_id::text,
        v_fingerprint, 'APPLIED', p_conversation_id::text
    );
end;
$$;

-- MARK_NOTIFICATION_READ -----------------------------------------------------
create or replace function public.autodrive_mark_notification_read_command_v1(
    p_mutation_id text,
    p_notification_id uuid
) returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_scope record;
    v_fingerprint text;
    v_existing jsonb;
begin
    perform public.autodrive_command_validate_mutation_v1(p_mutation_id);
    select * into v_scope from public.autodrive_command_scope_v1();
    perform pg_advisory_xact_lock(hashtextextended(
        'autodrive-command:' || v_scope.user_id::text || ':' || v_scope.client_id::text || ':' || v_scope.org_id::text || ':' || p_mutation_id,
        0
    ));
    v_fingerprint := public.autodrive_command_fingerprint_v1(jsonb_build_object('notification_id', p_notification_id));
    v_existing := public.autodrive_command_existing_or_conflict_v1(
        v_scope.user_id, v_scope.client_id, v_scope.org_id,
        p_mutation_id, 'MARK_NOTIFICATION_READ', v_fingerprint
    );
    if v_existing is not null then return v_existing; end if;

    if not exists (
        select 1 from public.notifications n
        where n.id = p_notification_id
          and n.org_id = v_scope.org_id
          and (n.client_id is null or n.client_id = v_scope.client_id)
          and (n.user_id = v_scope.user_id or n.target_user_id = v_scope.user_id)
    ) then
        return public.autodrive_command_store_receipt_v1(
            v_scope.user_id, v_scope.client_id, v_scope.org_id,
            p_mutation_id, 'MARK_NOTIFICATION_READ', 'notifications', p_notification_id::text,
            v_fingerprint, 'REJECTED', null, 'TARGET_NOT_FOUND_OR_FORBIDDEN'
        );
    end if;

    update public.notifications
    set is_read = true
    where id = p_notification_id
      and org_id = v_scope.org_id
      and (client_id is null or client_id = v_scope.client_id)
      and (user_id = v_scope.user_id or target_user_id = v_scope.user_id);

    return public.autodrive_command_store_receipt_v1(
        v_scope.user_id, v_scope.client_id, v_scope.org_id,
        p_mutation_id, 'MARK_NOTIFICATION_READ', 'notifications', p_notification_id::text,
        v_fingerprint, 'APPLIED', p_notification_id::text
    );
end;
$$;

-- REGISTER_PUSH_TOKEN --------------------------------------------------------
create or replace function public.autodrive_register_push_token_command_v1(
    p_mutation_id text,
    p_token text,
    p_platform text default 'android'
) returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_scope record;
    v_fingerprint text;
    v_existing jsonb;
    v_token_id uuid;
begin
    perform public.autodrive_command_validate_mutation_v1(p_mutation_id);
    select * into v_scope from public.autodrive_command_scope_v1();
    perform pg_advisory_xact_lock(hashtextextended(
        'autodrive-command:' || v_scope.user_id::text || ':' || v_scope.client_id::text || ':' || v_scope.org_id::text || ':' || p_mutation_id,
        0
    ));
    v_fingerprint := public.autodrive_command_fingerprint_v1(jsonb_build_object('token', p_token, 'platform', p_platform));
    v_existing := public.autodrive_command_existing_or_conflict_v1(
        v_scope.user_id, v_scope.client_id, v_scope.org_id,
        p_mutation_id, 'REGISTER_PUSH_TOKEN', v_fingerprint
    );
    if v_existing is not null then return v_existing; end if;

    if p_token is null or btrim(p_token) = '' or p_platform not in ('android', 'ios') then
        return public.autodrive_command_store_receipt_v1(
            v_scope.user_id, v_scope.client_id, v_scope.org_id,
            p_mutation_id, 'REGISTER_PUSH_TOKEN', 'push_tokens', v_scope.user_id::text,
            v_fingerprint, 'REJECTED', null, 'INVALID_TOKEN_OR_PLATFORM'
        );
    end if;

    insert into public.push_tokens(user_id, client_id, org_id, token, platform)
    values (v_scope.user_id, v_scope.client_id, v_scope.org_id, p_token, p_platform)
    on conflict (user_id) do update
    set client_id = excluded.client_id,
        org_id = excluded.org_id,
        token = excluded.token,
        platform = excluded.platform
    returning id into v_token_id;

    return public.autodrive_command_store_receipt_v1(
        v_scope.user_id, v_scope.client_id, v_scope.org_id,
        p_mutation_id, 'REGISTER_PUSH_TOKEN', 'push_tokens', v_scope.user_id::text,
        v_fingerprint, 'APPLIED', v_token_id::text
    );
end;
$$;

-- REVOKE_PUSH_TOKEN ----------------------------------------------------------
create or replace function public.autodrive_revoke_push_token_command_v1(
    p_mutation_id text
) returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_scope record;
    v_fingerprint text;
    v_existing jsonb;
begin
    perform public.autodrive_command_validate_mutation_v1(p_mutation_id);
    select * into v_scope from public.autodrive_command_scope_v1();
    perform pg_advisory_xact_lock(hashtextextended(
        'autodrive-command:' || v_scope.user_id::text || ':' || v_scope.client_id::text || ':' || v_scope.org_id::text || ':' || p_mutation_id,
        0
    ));
    v_fingerprint := public.autodrive_command_fingerprint_v1(jsonb_build_object('revoke', true));
    v_existing := public.autodrive_command_existing_or_conflict_v1(
        v_scope.user_id, v_scope.client_id, v_scope.org_id,
        p_mutation_id, 'REVOKE_PUSH_TOKEN', v_fingerprint
    );
    if v_existing is not null then return v_existing; end if;

    delete from public.push_tokens where user_id = v_scope.user_id;

    return public.autodrive_command_store_receipt_v1(
        v_scope.user_id, v_scope.client_id, v_scope.org_id,
        p_mutation_id, 'REVOKE_PUSH_TOKEN', 'push_tokens', v_scope.user_id::text,
        v_fingerprint, 'APPLIED', v_scope.user_id::text
    );
end;
$$;

-- CANCEL_PENDING_WITHDRAWALS -------------------------------------------------
create or replace function public.autodrive_cancel_pending_withdrawals_command_v1(
    p_mutation_id text
) returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_scope record;
    v_fingerprint text;
    v_existing jsonb;
    v_deleted integer;
begin
    perform public.autodrive_command_validate_mutation_v1(p_mutation_id);
    select * into v_scope from public.autodrive_command_scope_v1();
    perform pg_advisory_xact_lock(hashtextextended(
        'autodrive-command:' || v_scope.user_id::text || ':' || v_scope.client_id::text || ':' || v_scope.org_id::text || ':' || p_mutation_id,
        0
    ));
    v_fingerprint := public.autodrive_command_fingerprint_v1(jsonb_build_object('cancel_pending', true));
    v_existing := public.autodrive_command_existing_or_conflict_v1(
        v_scope.user_id, v_scope.client_id, v_scope.org_id,
        p_mutation_id, 'CANCEL_PENDING_WITHDRAWALS', v_fingerprint
    );
    if v_existing is not null then return v_existing; end if;

    v_deleted := public.cancel_pending_withdrawals();

    return public.autodrive_command_store_receipt_v1(
        v_scope.user_id, v_scope.client_id, v_scope.org_id,
        p_mutation_id, 'CANCEL_PENDING_WITHDRAWALS', 'withdrawal_requests', v_scope.client_id::text,
        v_fingerprint, 'APPLIED', v_scope.client_id::text, null, null, v_deleted
    );
end;
$$;

-- Scoped receipt reconciliation. No direct receipt-table SELECT is granted to Android.
create or replace function public.autodrive_get_command_receipt_v1(p_mutation_id text)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_scope record;
    v_receipt public.autodrive_command_receipts%rowtype;
begin
    perform public.autodrive_command_validate_mutation_v1(p_mutation_id);
    select * into v_scope from public.autodrive_command_scope_v1();
    select * into v_receipt
    from public.autodrive_command_receipts r
    where r.user_id = v_scope.user_id
      and r.client_id = v_scope.client_id
      and r.org_id = v_scope.org_id
      and r.mutation_id = p_mutation_id;
    if not found then return null; end if;
    return public.autodrive_command_receipt_json_v1(v_receipt, true);
end;
$$;

-- Helper functions are internal to the SECURITY DEFINER command surface.
revoke all on function public.autodrive_command_receipt_json_v1(public.autodrive_command_receipts, boolean) from public, anon, authenticated;
revoke all on function public.autodrive_command_scope_v1() from public, anon, authenticated;
revoke all on function public.autodrive_command_fingerprint_v1(jsonb) from public, anon, authenticated;
revoke all on function public.autodrive_command_validate_mutation_v1(text) from public, anon, authenticated;
revoke all on function public.autodrive_command_existing_or_conflict_v1(uuid, uuid, uuid, text, text, text) from public, anon, authenticated;
revoke all on function public.autodrive_command_store_receipt_v1(uuid, uuid, uuid, text, text, text, text, text, text, text, text, bigint, integer) from public, anon, authenticated;

revoke all on function public.autodrive_update_profile_command_v1(text, text, text, text, text, text, text, integer, text) from public, anon;
revoke all on function public.autodrive_request_withdrawal_command_v1(text, numeric, text) from public, anon;
revoke all on function public.autodrive_send_chat_message_command_v1(text, uuid, uuid, text, text, text, text, bigint) from public, anon;
revoke all on function public.autodrive_mark_chat_read_command_v1(text, uuid) from public, anon;
revoke all on function public.autodrive_mark_notification_read_command_v1(text, uuid) from public, anon;
revoke all on function public.autodrive_register_push_token_command_v1(text, text, text) from public, anon;
revoke all on function public.autodrive_revoke_push_token_command_v1(text) from public, anon;
revoke all on function public.autodrive_cancel_pending_withdrawals_command_v1(text) from public, anon;
revoke all on function public.autodrive_get_command_receipt_v1(text) from public, anon;

grant execute on function public.autodrive_update_profile_command_v1(text, text, text, text, text, text, text, integer, text) to authenticated;
grant execute on function public.autodrive_request_withdrawal_command_v1(text, numeric, text) to authenticated;
grant execute on function public.autodrive_send_chat_message_command_v1(text, uuid, uuid, text, text, text, text, bigint) to authenticated;
grant execute on function public.autodrive_mark_chat_read_command_v1(text, uuid) to authenticated;
grant execute on function public.autodrive_mark_notification_read_command_v1(text, uuid) to authenticated;
grant execute on function public.autodrive_register_push_token_command_v1(text, text, text) to authenticated;
grant execute on function public.autodrive_revoke_push_token_command_v1(text) to authenticated;
grant execute on function public.autodrive_cancel_pending_withdrawals_command_v1(text) to authenticated;
grant execute on function public.autodrive_get_command_receipt_v1(text) to authenticated;

commit;
