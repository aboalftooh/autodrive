-- Production contract hardening for the marketer app.
-- The view still returns only rows owned by the authenticated user because its
-- underlying tables enforce the existing client RLS policies.
ALTER VIEW public.commission_eligibility SET (security_invoker = true);

-- request_withdrawal already handles this index name explicitly when a race
-- occurs, so make the database invariant match the RPC's error handling.
CREATE UNIQUE INDEX IF NOT EXISTS idx_one_pending_withdrawal_per_client
    ON public.withdrawal_requests (client_id)
    WHERE status = 'PENDING';
