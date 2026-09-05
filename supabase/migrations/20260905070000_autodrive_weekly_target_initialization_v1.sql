-- One-time, race-safe migration path from the pre-server local weekly target.
-- The first upgraded device may initialize the account target; after that the
-- canonical server value always wins on every device.

create or replace function public.autodrive_weekly_performance_v1(p_legacy_target numeric)
returns table(
  week_start timestamptz,
  week_end timestamptz,
  as_of timestamptz,
  current_amount numeric,
  current_count bigint,
  previous_same_period_amount numeric,
  previous_same_period_count bigint,
  change_percent numeric,
  trend text,
  weekly_target numeric,
  progress_percent numeric,
  remaining_to_target numeric,
  days_remaining integer,
  required_daily_average numeric,
  target_achieved boolean,
  target_achieved_early boolean,
  target_is_too_easy boolean,
  target_suggestion_visible boolean,
  suggested_target numeric
)
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_user_id uuid := auth.uid();
  v_target numeric;
begin
  if v_user_id is null then
    raise exception 'AUTH_REQUIRED' using errcode = '42501';
  end if;

  v_target := coalesce(p_legacy_target, 500000);
  if v_target < 100000 or v_target > 5000000 then
    v_target := 500000;
  end if;

  update public.autodrive_users au
  set weekly_target = round(v_target, 2),
      weekly_target_updated_at = now(),
      updated_at = now()
  where au.user_id = v_user_id
    and au.weekly_target_updated_at is null;

  return query
  select * from public.autodrive_weekly_performance_v1();
end;
$$;

revoke all on function public.autodrive_weekly_performance_v1(numeric) from public, anon;
grant execute on function public.autodrive_weekly_performance_v1(numeric) to authenticated;
