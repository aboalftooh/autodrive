import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

function digit(value: string): string {
  const code = value.codePointAt(0) ?? -1
  if (code >= 0x0660 && code <= 0x0669) return String(code - 0x0660)
  if (code >= 0x06f0 && code <= 0x06f9) return String(code - 0x06f0)
  return value
}

// This implementation is intentionally identical to send-phone-otp.
function normalizeSudanesePhone(raw: unknown): string {
  const digits = Array.from(String(raw ?? '').normalize('NFKC'))
    .map(digit)
    .filter((value) => /[0-9]/.test(value))
    .join('')

  if (digits.startsWith('00249')) return digits.slice(2)
  if (digits.startsWith('249')) return digits
  if (digits.startsWith('0')) return `249${digits.slice(1)}`
  if (digits.length === 9) return `249${digits}`
  return digits
}

function normalizeOtp(raw: unknown): string {
  return Array.from(String(raw ?? '').normalize('NFKC'))
    .map(digit)
    .join('')
    .replace(/\s+/gu, '')
}

function json(body: Record<string, unknown>, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, 'Content-Type': 'application/json' },
  })
}

async function sha256Hex(value: string): Promise<string> {
  const bytes = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value))
  return Array.from(new Uint8Array(bytes)).map((byte) => byte.toString(16).padStart(2, '0')).join('')
}

function clientIp(req: Request): string {
  return req.headers.get('cf-connecting-ip')
    ?? req.headers.get('x-forwarded-for')?.split(',')[0]?.trim()
    ?? 'unknown'
}

async function logAttempt(
  supabase: ReturnType<typeof createClient>,
  phoneHash: string,
  ipHash: string,
  success: boolean,
  reason: string,
) {
  await supabase.from('phone_otp_attempt_log').insert({
    phone_hash: phoneHash,
    ip_hash: ipHash,
    action: 'verify',
    success,
    reason,
  })
}

Deno.serve(async (req: Request) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders })

  try {
    const body = await req.json()
    const normalizedPhone = normalizeSudanesePhone(body?.phone)
    const normalizedOtp = normalizeOtp(body?.otp)
    if (!/^249[0-9]{9}$/.test(normalizedPhone) || !/^[0-9]{6}$/.test(normalizedOtp)) {
      return json({ error: 'رقم الهاتف والرمز مطلوبان', code: 'invalid_request' }, 400)
    }

    const supabase = createClient(
      Deno.env.get('SUPABASE_URL')!,
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!,
    )
    const phoneHash = await sha256Hex(normalizedPhone)
    const ipHash = await sha256Hex(clientIp(req))
    const tenMinutesAgo = new Date(Date.now() - 10 * 60_000).toISOString()
    const { count: recentFailures } = await supabase
      .from('phone_otp_attempt_log')
      .select('id', { count: 'exact', head: true })
      .eq('action', 'verify')
      .eq('success', false)
      .gte('created_at', tenMinutesAgo)
      .or(`phone_hash.eq.${phoneHash},ip_hash.eq.${ipHash}`)
    if ((recentFailures ?? 0) >= 10) {
      await logAttempt(supabase, phoneHash, ipHash, false, 'rate_limited')
      return json({ error: 'تعذر التحقق من الرمز', code: 'otp_invalid_or_expired' }, 429)
    }

    const now = new Date().toISOString()
    const providedHash = await sha256Hex(normalizedOtp)
    const { data: records, error: fetchError } = await supabase
      .from('phone_otps')
      .select('id, otp_hash, attempts')
      .eq('phone', normalizedPhone)
      .eq('used', false)
      .gt('expires_at', now)
      .order('created_at', { ascending: false })
      .order('id', { ascending: false })
      .limit(1)
    if (fetchError || !records || records.length === 0) {
      await logAttempt(supabase, phoneHash, ipHash, false, 'invalid_or_expired')
      return json({ error: 'رمز التحقق غير صحيح أو منتهي', code: 'otp_invalid_or_expired' }, 400)
    }

    const record = records[0]
    if ((record.attempts ?? 0) >= 5) {
      await supabase.from('phone_otps').update({ used: true }).eq('id', record.id).eq('used', false)
      await logAttempt(supabase, phoneHash, ipHash, false, 'attempts_exceeded')
      return json({ error: 'تم تجاوز محاولات التحقق', code: 'otp_attempts_exceeded' }, 429)
    }

    const { error: attemptError } = await supabase
      .from('phone_otps')
      .update({ attempts: (record.attempts ?? 0) + 1 })
      .eq('id', record.id)
      .eq('used', false)
    if (attemptError) {
      await logAttempt(supabase, phoneHash, ipHash, false, 'attempt_update_failed')
      return json({ error: 'تعذر التحقق من الرمز', code: 'otp_invalid_or_expired' }, 500)
    }

    if (String(record.otp_hash).toLowerCase() !== providedHash) {
      await logAttempt(supabase, phoneHash, ipHash, false, 'mismatch')
      return json({ error: 'رمز التحقق غير صحيح', code: 'otp_mismatch' }, 400)
    }

    // Create the Supabase session before consuming the OTP. A session failure therefore
    // does not burn a valid code; the final conditional update makes successful use one-time.
    let userEmail: string
    const { data: autodriveUser } = await supabase
      .from('autodrive_users')
      .select('user_id')
      .eq('phone', normalizedPhone)
      .maybeSingle()
    if (autodriveUser?.user_id) {
      const { data: authUser, error: authError } = await supabase.auth.admin.getUserById(autodriveUser.user_id)
      if (authError || !authUser?.user?.email) {
        console.error('[verify-phone-otp] getUserById failed')
        return json({ error: 'تعذر إنشاء جلسة الدخول', code: 'auth_lookup_failed' }, 500)
      }
      userEmail = authUser.user.email
    } else {
      userEmail = `phone_${normalizedPhone}@phone.autodrive`
    }

    const { data: link, error: linkError } = await supabase.auth.admin.generateLink({
      type: 'magiclink',
      email: userEmail,
      options: { redirectTo: 'autodrive://auth' },
    })
    if (linkError || !link?.properties?.hashed_token) {
      console.error('[verify-phone-otp] generateLink failed')
      return json({ error: 'تعذر إنشاء جلسة الدخول', code: 'session_create_failed' }, 500)
    }

    const tokenResponse = await fetch(`${Deno.env.get('SUPABASE_URL')}/auth/v1/verify`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        apikey: Deno.env.get('SUPABASE_ANON_KEY')!,
        Authorization: `Bearer ${Deno.env.get('SUPABASE_ANON_KEY')!}`,
      },
      body: JSON.stringify({
        type: 'magiclink',
        token_hash: link.properties.hashed_token,
      }),
    })
    const tokenBody = await tokenResponse.text()
    if (!tokenResponse.ok) {
      console.error('[verify-phone-otp] token exchange failed:', tokenResponse.status, tokenBody.slice(0, 500))
      return json({ error: 'تعذر إنشاء جلسة الدخول', code: 'token_exchange_failed' }, 500)
    }
    const session = JSON.parse(tokenBody)
    if (!session.access_token || !session.refresh_token) {
      return json({ error: 'تعذر إنشاء جلسة الدخول', code: 'session_create_failed' }, 500)
    }

    const { data: consumed, error: consumeError } = await supabase
      .from('phone_otps')
      .update({ used: true })
      .eq('id', record.id)
      .eq('used', false)
      .select('id')
    if (consumeError || !consumed || consumed.length !== 1) {
      await logAttempt(supabase, phoneHash, ipHash, false, 'already_consumed')
      return json({ error: 'رمز التحقق غير صحيح أو مستخدم', code: 'otp_invalid_or_expired' }, 400)
    }

    await logAttempt(supabase, phoneHash, ipHash, true, 'verified')
    return json({
      access_token: session.access_token,
      refresh_token: session.refresh_token,
      expires_in: session.expires_in ?? 3600,
      token_type: session.token_type ?? 'bearer',
      user_id: session.user?.id ?? '',
    })
  } catch {
    console.error('[verify-phone-otp] unexpected error')
    return json({ error: 'تعذر التحقق من الرمز', code: 'internal_error' }, 500)
  }
})
