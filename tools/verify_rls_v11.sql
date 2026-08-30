-- Run in Supabase SQL editor with an owner/admin role.
-- Fails when a client-accessed table has RLS disabled or has no policy.

do $$
declare
    table_name text;
    expected_tables text[] := array[
        'autodrive_users',
        'invoices',
        'invoice_items',
        'payments',
        'commission_payments',
        'commission_eligibility',
        'marketer_balance',
        'balance_transactions',
        'withdrawal_requests',
        'notifications',
        'conversations',
        'internal_messages',
        'push_tokens',
        'weekly_competition_results',
        'weekly_competition_weeks',
        'ai_insights',
        'dynamo_content'
    ];
begin
    foreach table_name in array expected_tables loop
        if not exists (
            select 1
            from pg_class c
            join pg_namespace n on n.oid = c.relnamespace
            where n.nspname = 'public'
              and c.relname = table_name
              and c.relkind = 'r'
              and c.relrowsecurity = true
        ) then
            raise exception 'RLS missing or disabled for public.%', table_name;
        end if;

        if not exists (
            select 1
            from pg_policies p
            where p.schemaname = 'public'
              and p.tablename = table_name
        ) then
            raise exception 'No RLS policy found for public.%', table_name;
        end if;
    end loop;
end $$;

select
    p.tablename,
    p.policyname,
    p.roles,
    p.cmd,
    p.qual,
    p.with_check
from pg_policies p
where p.schemaname = 'public'
  and p.tablename = any(array[
      'autodrive_users', 'invoices', 'invoice_items', 'payments',
      'commission_payments', 'commission_eligibility', 'marketer_balance',
      'balance_transactions', 'withdrawal_requests', 'notifications',
      'conversations', 'internal_messages', 'push_tokens',
      'weekly_competition_results', 'weekly_competition_weeks',
      'ai_insights', 'dynamo_content'
  ])
order by p.tablename, p.policyname;

select
    p.proname,
    p.prosecdef as security_definer,
    has_function_privilege('anon', p.oid, 'EXECUTE') as anon_can_execute,
    has_function_privilege('authenticated', p.oid, 'EXECUTE') as authenticated_can_execute,
    pg_get_function_identity_arguments(p.oid) as arguments
from pg_proc p
join pg_namespace n on n.oid = p.pronamespace
where n.nspname = 'public'
  and p.proname = any(array[
      'request_withdrawal',
      'cancel_pending_withdrawals',
      'touch_last_seen',
      'get_current_week_number',
      'get_weekly_competition',
      'get_my_competition_history',
      'get_my_win_weeks'
  ])
order by p.proname;
