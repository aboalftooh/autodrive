create table if not exists public.autodrive_feature_flags (
    feature_key text primary key,
    state text not null
        check (state in ('DISABLED', 'LOCKED', 'ACTIVE')),
    updated_at timestamptz not null default now()
);

create or replace function public.set_autodrive_feature_flag_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists trg_autodrive_feature_flags_updated_at
on public.autodrive_feature_flags;

create trigger trg_autodrive_feature_flags_updated_at
before update on public.autodrive_feature_flags
for each row
execute function public.set_autodrive_feature_flag_updated_at();

alter table public.autodrive_feature_flags enable row level security;

revoke all on table public.autodrive_feature_flags from anon, authenticated;
grant select on table public.autodrive_feature_flags to anon, authenticated;

drop policy if exists autodrive_feature_flags_public_read
on public.autodrive_feature_flags;

create policy autodrive_feature_flags_public_read
on public.autodrive_feature_flags
for select
to anon, authenticated
using (true);

insert into public.autodrive_feature_flags(feature_key, state)
values ('weekly_competition', 'DISABLED')
on conflict (feature_key) do nothing;
