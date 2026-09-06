create or replace function public.autodrive_complete_onboarding_v1(
  p_full_name text,
  p_bank_name text,
  p_bank_account text,
  p_workshop_name text default null,
  p_specialty text default null,
  p_workers_count integer default null,
  p_address text default null
)
returns boolean
language plpgsql
security definer
set search_path to 'pg_catalog', 'public'
as $function$
declare
  v_uid uuid := auth.uid();
  v_type text;
  v_name text := btrim(coalesce(p_full_name,''));
  v_bank text := btrim(coalesce(p_bank_name,''));
  v_account text := btrim(coalesce(p_bank_account,''));
  v_workshop text := nullif(btrim(coalesce(p_workshop_name,'')), '');
  v_specialty text := nullif(btrim(coalesce(p_specialty,'')), '');
  v_address text := nullif(btrim(coalesce(p_address,'')), '');
begin
  if v_uid is null then raise exception 'NOT_AUTHENTICATED' using errcode='42501'; end if;
  if length(v_name) < 2 or length(v_name) > 120 then raise exception 'INVALID_FULL_NAME' using errcode='22023'; end if;
  if v_bank = '' or length(v_bank) > 120 then raise exception 'INVALID_BANK_NAME' using errcode='22023'; end if;
  if v_account = '' or length(v_account) > 160 then raise exception 'INVALID_BANK_ACCOUNT' using errcode='22023'; end if;
  if p_workers_count is not null and (p_workers_count < 0 or p_workers_count > 100000) then raise exception 'INVALID_WORKERS_COUNT' using errcode='22023'; end if;

  select au.account_type::text into v_type
  from public.autodrive_users au
  where au.user_id = v_uid
  for update;
  if not found then raise exception 'AUTODRIVE_MEMBERSHIP_NOT_FOUND' using errcode='P0002'; end if;

  if v_type = 'WORKSHOP_OWNER' and (v_workshop is null or v_specialty is null or v_address is null) then
    raise exception 'WORKSHOP_DETAILS_REQUIRED' using errcode='22023';
  end if;

  update public.autodrive_users
  set full_name = v_name,
      bank_name = v_bank,
      bank_account = v_account,
      workshop_name = case when v_type='WORKSHOP_OWNER' then v_workshop else null end,
      specialty = case when v_type='WORKSHOP_OWNER' then v_specialty else null end,
      workers_count = case when v_type='WORKSHOP_OWNER' then p_workers_count else null end,
      address = case when v_type='WORKSHOP_OWNER' then v_address else null end,
      onboarding_completed = true,
      updated_at = now()
  where user_id = v_uid;

  return true;
end;
$function$;

revoke all on function public.autodrive_complete_onboarding_v1(text,text,text,text,text,integer,text) from public, anon;
grant execute on function public.autodrive_complete_onboarding_v1(text,text,text,text,text,integer,text) to authenticated;
