-- AutoDrive v71 — authoritative chat recovery cursor + durable media reference
-- + idempotent conversation-create. Append-only; v69 history remains unchanged.

begin;

alter table public.autodrive_command_receipts
    drop constraint if exists autodrive_command_receipts_command_type_check;
alter table public.autodrive_command_receipts
    add constraint autodrive_command_receipts_command_type_check check (command_type = any (array[
        'UPDATE_PROFILE'::text,
        'REQUEST_WITHDRAWAL'::text,
        'SEND_CHAT_MESSAGE'::text,
        'CREATE_CHAT_CONVERSATION'::text,
        'MARK_CHAT_READ'::text,
        'MARK_NOTIFICATION_READ'::text,
        'REGISTER_PUSH_TOKEN'::text,
        'REVOKE_PUSH_TOKEN'::text,
        'CANCEL_PENDING_WITHDRAWALS'::text
    ]));

-- Chat-only compatibility cursor. This is deliberately NOT the Session 72 global data revision.
create sequence if not exists public.autodrive_chat_recovery_seq_v1 as bigint;

alter table public.internal_messages
    add column if not exists chat_recovery_seq bigint;
alter table public.internal_messages
    add column if not exists media_object_path text;

-- Migration transaction owns a strong-enough table lock while the historical sequence is backfilled.
lock table public.internal_messages in share row exclusive mode;

with base as (
    select coalesce(max(chat_recovery_seq), 0)::bigint as n
    from public.internal_messages
), ranked as (
    select m.id,
           row_number() over (order by m.created_at asc, m.id asc)::bigint as rn
    from public.internal_messages m
    where m.chat_recovery_seq is null
)
update public.internal_messages m
set chat_recovery_seq = base.n + ranked.rn
from base, ranked
where m.id = ranked.id;

select setval(
    'public.autodrive_chat_recovery_seq_v1',
    greatest(coalesce(max(chat_recovery_seq), 0), 1),
    coalesce(max(chat_recovery_seq), 0) > 0
)
from public.internal_messages;

alter table public.internal_messages
    alter column chat_recovery_seq set not null;

create unique index if not exists internal_messages_chat_recovery_seq_key
    on public.internal_messages(chat_recovery_seq);

-- Own the recovery sequence on the server even for legacy/direct insert paths. A caller-supplied
-- value cannot backdate/skip behind an accepted cursor, and the ordering identity is immutable.
create or replace function public.autodrive_internal_message_recovery_identity_v1()
returns trigger
language plpgsql
set search_path = public, pg_temp
as $$
begin
    if tg_op = 'INSERT' then
        new.chat_recovery_seq := nextval('public.autodrive_chat_recovery_seq_v1');
        return new;
    end if;

    if new.id is distinct from old.id
       or new.chat_recovery_seq is distinct from old.chat_recovery_seq then
        raise exception 'CHAT_RECOVERY_IDENTITY_IMMUTABLE' using errcode = '22023';
    end if;
    return new;
end;
$$;

drop trigger if exists trg_autodrive_internal_message_recovery_identity_v1
    on public.internal_messages;
create trigger trg_autodrive_internal_message_recovery_identity_v1
before insert or update on public.internal_messages
for each row execute function public.autodrive_internal_message_recovery_identity_v1();

-- Conversation-scoped keyset page over the server-owned monotonically increasing chat sequence.
create or replace function public.autodrive_chat_recovery_page_v1(
    p_conversation_id uuid,
    p_after_seq bigint default 0,
    p_limit integer default 200
) returns setof public.internal_messages
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_scope record;
begin
    if p_conversation_id is null then
        raise exception 'CONVERSATION_ID_REQUIRED' using errcode = '22023';
    end if;
    if p_after_seq is null or p_after_seq < 0 then
        raise exception 'CHAT_CURSOR_INVALID' using errcode = '22023';
    end if;
    if p_limit is null or p_limit < 1 or p_limit > 500 then
        raise exception 'CHAT_PAGE_LIMIT_INVALID' using errcode = '22023';
    end if;

    select * into v_scope from public.autodrive_command_scope_v1();

    if not exists (
        select 1 from public.conversations c
        where c.id = p_conversation_id
          and c.client_id = v_scope.client_id
          and c.org_id = v_scope.org_id
    ) then
        return;
    end if;

    return query
    select m.*
    from public.internal_messages m
    where m.client_id = v_scope.client_id
      and m.org_id = v_scope.org_id
      and m.conversation_id = p_conversation_id
      and m.chat_recovery_seq > p_after_seq
    order by m.chat_recovery_seq asc
    limit p_limit;
end;
$$;

comment on function public.autodrive_chat_recovery_page_v1(uuid,bigint,integer) is
'v71 conversation-scoped chat compatibility keyset. chat_recovery_seq is not a global data revision/change-feed cursor.';

-- Idempotent logical conversation create. Business ownership/subject semantics are delegated to
-- the current authoritative create_new_conversation(uuid,text) function proven by the 2026-08-21 schema.
create or replace function public.autodrive_create_chat_conversation_command_v1(
    p_mutation_id text,
    p_local_conversation_id uuid,
    p_subject text default ''
) returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_scope record;
    v_fingerprint text;
    v_existing jsonb;
    v_created public.conversations;
    v_server_id text;
begin
    perform public.autodrive_command_validate_mutation_v1(p_mutation_id);
    if p_local_conversation_id is null or p_local_conversation_id::text <> p_mutation_id then
        raise exception 'CONVERSATION_MUTATION_ID_MISMATCH' using errcode = '22023';
    end if;

    select * into v_scope from public.autodrive_command_scope_v1();
    perform pg_advisory_xact_lock(hashtextextended(
        'autodrive-chat-create:' || v_scope.user_id::text || ':' || p_local_conversation_id::text,
        0
    ));
    perform pg_advisory_xact_lock(hashtextextended(
        'autodrive-command:' || v_scope.user_id::text || ':' || v_scope.client_id::text || ':' ||
        v_scope.org_id::text || ':' || p_mutation_id,
        0
    ));

    v_fingerprint := public.autodrive_command_fingerprint_v1(jsonb_build_object(
        'local_conversation_id', p_local_conversation_id,
        'subject', coalesce(p_subject, '')
    ));
    v_existing := public.autodrive_command_existing_or_conflict_v1(
        v_scope.user_id, v_scope.client_id, v_scope.org_id,
        p_mutation_id, 'CREATE_CHAT_CONVERSATION', v_fingerprint
    );
    if v_existing is not null then return v_existing; end if;

    select * into v_created
    from public.create_new_conversation(v_scope.client_id, coalesce(p_subject, ''));
    v_server_id := v_created.id::text;

    if v_server_id is null then
        raise exception 'CREATE_CONVERSATION_RESULT_ID_MISSING' using errcode = 'P0001';
    end if;
    if v_created.client_id <> v_scope.client_id or v_created.org_id <> v_scope.org_id then
        raise exception 'CREATE_CONVERSATION_SCOPE_MISMATCH' using errcode = '42501';
    end if;

    return public.autodrive_command_store_receipt_v1(
        v_scope.user_id, v_scope.client_id, v_scope.org_id,
        p_mutation_id, 'CREATE_CHAT_CONVERSATION', 'conversations', p_local_conversation_id::text,
        v_fingerprint, 'APPLIED', v_server_id
    );
end;
$$;

-- V2 keeps the private Storage object path as canonical durable identity. media_url is only a
-- compatibility signed URL for older readers and is intentionally excluded from canonical fingerprint.
create or replace function public.autodrive_send_chat_message_command_v2(
    p_mutation_id text,
    p_message_id uuid,
    p_conversation_id uuid,
    p_type text,
    p_body text,
    p_media_url text default null,
    p_media_mime text default null,
    p_media_duration_ms bigint default null,
    p_media_object_path text default null
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
        'autodrive-command:' || v_scope.user_id::text || ':' || v_scope.client_id::text || ':' ||
        v_scope.org_id::text || ':' || p_mutation_id,
        0
    ));

    if p_media_object_path is null or btrim(p_media_object_path) = ''
       or p_media_object_path not like v_scope.org_id::text || '/%' then
        return public.autodrive_command_store_receipt_v1(
            v_scope.user_id, v_scope.client_id, v_scope.org_id,
            p_mutation_id, 'SEND_CHAT_MESSAGE', 'internal_messages', p_message_id::text,
            public.autodrive_command_fingerprint_v1(jsonb_build_object(
                'message_id', p_message_id, 'conversation_id', p_conversation_id,
                'sender_id', v_scope.user_id, 'type', p_type, 'body', p_body,
                'media_mime', p_media_mime, 'media_duration_ms', p_media_duration_ms,
                'media_object_path', p_media_object_path
            )),
            'REJECTED', null, 'MEDIA_OBJECT_PATH_INVALID'
        );
    end if;

    v_fingerprint := public.autodrive_command_fingerprint_v1(jsonb_build_object(
        'message_id', p_message_id,
        'conversation_id', p_conversation_id,
        'sender_id', v_scope.user_id,
        'type', p_type,
        'body', p_body,
        'media_mime', p_media_mime,
        'media_duration_ms', p_media_duration_ms,
        'media_object_path', p_media_object_path
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
        is_read, conversation_id, type, media_url, media_mime, media_duration_ms, media_object_path
    ) values (
        p_message_id, v_scope.org_id, v_scope.client_id, v_scope.user_id, 'MARKETER', p_body,
        false, p_conversation_id, p_type, p_media_url, p_media_mime, p_media_duration_ms,
        p_media_object_path
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
       or v_row.media_mime is distinct from p_media_mime
       or v_row.media_duration_ms is distinct from p_media_duration_ms
       or v_row.media_object_path is distinct from p_media_object_path then
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

revoke all on function public.autodrive_chat_recovery_page_v1(uuid,bigint,integer)
    from public, anon;
grant execute on function public.autodrive_chat_recovery_page_v1(uuid,bigint,integer)
    to authenticated;

revoke all on function public.autodrive_create_chat_conversation_command_v1(text,uuid,text)
    from public, anon;
grant execute on function public.autodrive_create_chat_conversation_command_v1(text,uuid,text)
    to authenticated;

revoke all on function public.autodrive_send_chat_message_command_v2(text,uuid,uuid,text,text,text,text,bigint,text)
    from public, anon;
grant execute on function public.autodrive_send_chat_message_command_v2(text,uuid,uuid,text,text,text,text,bigint,text)
    to authenticated;

commit;
