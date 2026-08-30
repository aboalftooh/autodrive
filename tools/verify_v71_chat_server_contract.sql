-- Session 71 runtime verification. Run after applying the v71 migration in a disposable
-- verification environment. Authenticated replay/cross-scope fixtures are supplied by deployment.

begin;

do $$
begin
  if to_regprocedure('public.autodrive_chat_recovery_page_v1(uuid,bigint,integer)') is null then
    raise exception 'V71_CHAT_RECOVERY_RPC_MISSING';
  end if;
  if to_regprocedure('public.autodrive_create_chat_conversation_command_v1(text,uuid,text)') is null then
    raise exception 'V71_CREATE_CONVERSATION_RPC_MISSING';
  end if;
  if to_regprocedure('public.autodrive_send_chat_message_command_v2(text,uuid,uuid,text,text,text,text,bigint,text)') is null then
    raise exception 'V71_SEND_CHAT_V2_RPC_MISSING';
  end if;
end $$;

do $$
declare
  v_def text;
  v_nullable text;
begin
  select pg_get_constraintdef(oid) into v_def
  from pg_constraint
  where conrelid='public.autodrive_command_receipts'::regclass
    and conname='autodrive_command_receipts_command_type_check';
  if position('CREATE_CHAT_CONVERSATION' in coalesce(v_def,'')) = 0 then
    raise exception 'V71_RECEIPT_COMMAND_TYPE_NOT_REGISTERED';
  end if;

  select is_nullable into v_nullable from information_schema.columns
  where table_schema='public' and table_name='internal_messages' and column_name='chat_recovery_seq';
  if v_nullable is distinct from 'NO' then raise exception 'V71_CHAT_RECOVERY_SEQ_NOT_NOT_NULL'; end if;

  if not exists (
    select 1 from pg_indexes
    where schemaname='public' and tablename='internal_messages'
      and indexname='internal_messages_chat_recovery_seq_key'
  ) then raise exception 'V71_CHAT_RECOVERY_SEQ_UNIQUE_INDEX_MISSING'; end if;

  if not exists (
    select 1 from information_schema.columns
    where table_schema='public' and table_name='internal_messages' and column_name='media_object_path'
  ) then raise exception 'V71_MEDIA_OBJECT_PATH_MISSING'; end if;
end $$;

-- Runtime replay, auth-derived scope, storage object reconciliation, and cross-scope negative tests
-- require real principals and are intentionally not faked in this static transaction.
rollback;
