-- AutoDrive weekly performance v1
-- Server owns week boundaries, fair same-period comparison and goal suggestions.
-- Personal goal remains user-controlled; suggestions never auto-change it.

alter table public.autodrive_users
  add column if not exists weekly_target numeric(18,2) not null default 500000,
  add column if not exists weekly_target_updated_at timestamptz,
  add column if not exists weekly_target_suggestion_snoozed_until timestamptz;

update public.autodrive_users
set weekly_target = 500000
where weekly_target is null;

alter table public.autodrive_users
  alter column weekly_target set default 500000,
  alter column weekly_target set not null;

do $$
begin
  if not exists (
    select 1
    from pg_constraint
    where conname = 'autodrive_users_weekly_target_range'
      and conrelid = 'public.autodrive_users'::regclass
  ) then
    alter table public.autodrive_users
      add constraint autodrive_users_weekly_target_range
      check (weekly_target between 100000 and 5000000);
  end if;
end
$$;

create or replace function public.autodrive_weekly_performance_v1()
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
stable
security definer
set search_path = public, pg_temp
as $$
declare
  v_user_id uuid := auth.uid();
  v_client_id uuid;
  v_target numeric;
  v_snoozed_until timestamptz;
  v_now timestamptz := now();
  v_week_start timestamptz := public.last_friday_9am();
  v_week_end timestamptz;
  v_elapsed interval;
  v_previous_start timestamptz;
  v_previous_end timestamptz;
  v_current numeric := 0;
  v_current_count bigint := 0;
  v_previous numeric := 0;
  v_previous_count bigint := 0;
  v_change numeric;
  v_trend text;
  v_progress numeric := 0;
  v_remaining numeric := 0;
  v_days integer := 0;
  v_daily numeric := 0;
  v_overperform_weeks integer := 0;
  v_suggested numeric;
  v_too_easy boolean := false;
  v_suggestion_visible boolean := false;
begin
  if v_user_id is null then
    raise exception 'AUTH_REQUIRED' using errcode = '42501';
  end if;

  select au.client_id, au.weekly_target, au.weekly_target_suggestion_snoozed_until
    into v_client_id, v_target, v_snoozed_until
  from public.autodrive_users au
  where au.user_id = v_user_id
  limit 1;

  if v_client_id is null then
    raise exception 'AUTODRIVE_USER_NOT_FOUND' using errcode = 'P0001';
  end if;

  v_target := coalesce(v_target, 500000);
  v_week_end := v_week_start + interval '7 days';
  v_elapsed := least(v_now, v_week_end) - v_week_start;
  if v_elapsed < interval '0 seconds' then
    v_elapsed := interval '0 seconds';
  end if;
  v_previous_start := v_week_start - interval '7 days';
  v_previous_end := v_previous_start + v_elapsed;

  select coalesce(sum(cl.amount), 0), count(*)
    into v_current, v_current_count
  from public.commission_ledger cl
  where cl.client_id = v_client_id
    and cl.created_at >= v_week_start
    and cl.created_at < least(v_now, v_week_end);

  select coalesce(sum(cl.amount), 0), count(*)
    into v_previous, v_previous_count
  from public.commission_ledger cl
  where cl.client_id = v_client_id
    and cl.created_at >= v_previous_start
    and cl.created_at < v_previous_end;

  if v_previous > 0 then
    v_change := round(((v_current - v_previous) / v_previous) * 100, 1);
    v_trend := case
      when v_current > v_previous then 'UP'
      when v_current < v_previous then 'DOWN'
      else 'FLAT'
    end;
  elsif v_current > 0 then
    v_change := null;
    v_trend := 'UP_NO_BASELINE';
  else
    v_change := null;
    v_trend := 'NO_BASELINE';
  end if;

  v_progress := case when v_target > 0 then round((v_current / v_target) * 100, 1) else 0 end;
  v_remaining := greatest(v_target - v_current, 0);
  v_days := greatest(0, ceil(extract(epoch from (v_week_end - v_now)) / 86400.0)::integer);
  v_daily := case
    when v_remaining <= 0 then 0
    when v_days <= 0 then v_remaining
    else ceil(v_remaining / v_days)
  end;

  with completed_weeks as (
    select gs.n,
           coalesce(sum(cl.amount), 0)::numeric as total_amount
    from generate_series(1, 3) as gs(n)
    left join public.commission_ledger cl
      on cl.client_id = v_client_id
     and cl.created_at >= v_week_start - (gs.n * interval '7 days')
     and cl.created_at <  v_week_start - ((gs.n - 1) * interval '7 days')
    group by gs.n
  )
  select count(*) filter (where total_amount >= v_target * 1.20)::integer
    into v_overperform_weeks
  from completed_weeks;

  v_too_easy := v_target < 5000000 and v_overperform_weeks >= 2;
  v_suggestion_visible := v_too_easy and (v_snoozed_until is null or v_snoozed_until <= v_now);
  v_suggested := case
    when v_too_easy then least(5000000::numeric, ceil((v_target * 1.20) / 50000) * 50000)
    else null
  end;

  return query
  select
    v_week_start,
    v_week_end,
    v_now,
    v_current,
    v_current_count,
    v_previous,
    v_previous_count,
    v_change,
    v_trend,
    v_target,
    v_progress,
    v_remaining,
    v_days,
    v_daily,
    (v_current >= v_target),
    (v_current >= v_target and v_now < v_week_end - interval '1 day'),
    v_too_easy,
    v_suggestion_visible,
    v_suggested;
end;
$$;

create or replace function public.autodrive_set_weekly_target_v1(p_target numeric)
returns table(weekly_target numeric, updated_at timestamptz)
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_user_id uuid := auth.uid();
begin
  if v_user_id is null then
    raise exception 'AUTH_REQUIRED' using errcode = '42501';
  end if;
  if p_target is null or p_target < 100000 or p_target > 5000000 then
    raise exception 'WEEKLY_TARGET_OUT_OF_RANGE' using errcode = '22003';
  end if;

  update public.autodrive_users au
  set weekly_target = round(p_target, 2),
      weekly_target_updated_at = now(),
      weekly_target_suggestion_snoozed_until = null,
      updated_at = now()
  where au.user_id = v_user_id
  returning au.weekly_target, au.weekly_target_updated_at
  into weekly_target, updated_at;

  if not found then
    raise exception 'AUTODRIVE_USER_NOT_FOUND' using errcode = 'P0001';
  end if;

  return next;
end;
$$;

create or replace function public.autodrive_snooze_weekly_target_suggestion_v1(p_days integer default 14)
returns timestamptz
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_user_id uuid := auth.uid();
  v_until timestamptz;
begin
  if v_user_id is null then
    raise exception 'AUTH_REQUIRED' using errcode = '42501';
  end if;
  if p_days is null or p_days < 1 or p_days > 90 then
    raise exception 'INVALID_SNOOZE_DAYS' using errcode = '22003';
  end if;

  update public.autodrive_users au
  set weekly_target_suggestion_snoozed_until = now() + make_interval(days => p_days),
      updated_at = now()
  where au.user_id = v_user_id
  returning au.weekly_target_suggestion_snoozed_until into v_until;

  if not found then
    raise exception 'AUTODRIVE_USER_NOT_FOUND' using errcode = 'P0001';
  end if;

  return v_until;
end;
$$;

revoke all on function public.autodrive_weekly_performance_v1() from public, anon;
revoke all on function public.autodrive_set_weekly_target_v1(numeric) from public, anon;
revoke all on function public.autodrive_snooze_weekly_target_suggestion_v1(integer) from public, anon;

grant execute on function public.autodrive_weekly_performance_v1() to authenticated;
grant execute on function public.autodrive_set_weekly_target_v1(numeric) to authenticated;
grant execute on function public.autodrive_snooze_weekly_target_suggestion_v1(integer) to authenticated;
