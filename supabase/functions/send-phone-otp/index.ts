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

// This implementation is intentionally identical to verify-phone-otp.
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
    action: 'send',
    success,
    reason,
  })
}

function normalizeAppHash(raw: unknown): string | null {
  const value = String(raw ?? '').trim()
  return /^[A-Za-z0-9+/_-]{11}$/.test(value) ? value : null
}

function secureOtp(): string {
  const limit = Math.floor(0x1_0000_0000 / 900000) * 900000
  const bucket = new Uint32Array(1)
  do crypto.getRandomValues(bucket); while (bucket[0] >= limit)
  return String(100000 + (bucket[0] % 900000))
}

async function sendSms(phone: string, otp: string, requestAppHash: string | null): Promise<{ ok: true } | { ok: false; message: string }> {
  const apiKey = Deno.env.get('BRQSMS_API_KEY')
  const sender = Deno.env.get('BRQSMS_SENDER') ?? 'BrqOTP'
  // Prefer the hash computed from the signature of the installed APK. This survives debug/release
  // signing changes and keeps SMS Retriever aligned with the actual client binary.
  const appHash = requestAppHash ?? normalizeAppHash(Deno.env.get('ANDROID_SMS_RETRIEVER_HASH'))
  if (!apiKey) return { ok: false, message: 'إعدادات مزود الرسائل غير مكتملة' }

  const response = await fetch('https://dash.brqsms.com/api/v3/sms/send', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${apiKey}`,
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      recipient: phone,
      sender_id: sender,
      message: appHash
        ? `<#> AutoDrive verification code: ${otp}\n${appHash}`
        : `AutoDrive verification code: ${otp}`,
    }),
  })
  const text = await response.text()
  let body: Record<string, unknown> = {}
  try { body = JSON.parse(text) } catch { /* provider returned non-JSON */ }
  const providerStatus = String(body.status ?? body.code ?? '').toLowerCase()
  if (response.ok && providerStatus !== 'error') return { ok: true }
  const message = typeof body.message === 'string' && body.message.trim()
    ? body.message
    : 'فشل إرسال الرسالة من مزود SMS'
  return { ok: false, message }
}

Deno.serve(async (req: Request) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders })

  try {
    const body = await req.json()
    const normalizedPhone = normalizeSudanesePhone(body?.phone)
    if (!/^249[0-9]{9}$/.test(normalizedPhone)) {
      return json({ error: 'رقم الهاتف غير صالح', code: 'invalid_phone' }, 400)
    }
    const requestAppHash = body?.app_hash == null ? null : normalizeAppHash(body.app_hash)
    if (body?.app_hash != null && requestAppHash == null) {
      return json({ error: 'بصمة التطبيق غير صالحة', code: 'invalid_app_hash' }, 400)
    }

    const supabase = createClient(
      Deno.env.get('SUPABASE_URL')!,
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!,
    )
    const phoneHash = await sha256Hex(normalizedPhone)
    const ipHash = await sha256Hex(clientIp(req))
    const now = new Date()
    const oneMinuteAgo = new Date(now.getTime() - 60_000).toISOString()
    const tenMinutesAgo = new Date(now.getTime() - 10 * 60_000).toISOString()

    const { data: recent, error: recentError } = await supabase
      .from('phone_otps')
      .select('id')
      .eq('phone', normalizedPhone)
      .eq('used', false)
      .gte('created_at', oneMinuteAgo)
      .limit(1)
    if (recentError) return json({ error: 'تعذر إرسال رمز التحقق', code: 'otp_lookup_failed' }, 500)
    if (recent && recent.length > 0) {
      await logAttempt(supabase, phoneHash, ipHash, false, 'phone_rate_limited')
      return json({ error: 'يرجى الانتظار قبل طلب رمز جديد', code: 'rate_limited' }, 429)
    }

    const { count: recentIpAttempts } = await supabase
      .from('phone_otp_attempt_log')
      .select('id', { count: 'exact', head: true })
      .eq('action', 'send')
      .eq('ip_hash', ipHash)
      .gte('created_at', tenMinutesAgo)
    if ((recentIpAttempts ?? 0) >= 10) {
      await logAttempt(supabase, phoneHash, ipHash, false, 'ip_rate_limited')
      return json({ error: 'تعذر إرسال رمز التحقق', code: 'rate_limited' }, 429)
    }

    // The partial unique index plus this update makes replacement safe under concurrent sends.
    const { error: cancelError } = await supabase
      .from('phone_otps')
      .update({ used: true })
      .eq('phone', normalizedPhone)
      .eq('used', false)
    if (cancelError) return json({ error: 'تعذر إنشاء رمز التحقق', code: 'otp_replace_failed' }, 500)

    const otp = secureOtp()
    const { data: inserted, error: insertError } = await supabase
      .from('phone_otps')
      .insert({
        phone: normalizedPhone,
        otp_hash: await sha256Hex(normalizeOtp(otp)),
        expires_at: new Date(now.getTime() + 5 * 60_000).toISOString(),
      })
      .select('id')
      .single()
    if (insertError || !inserted?.id) {
      await logAttempt(supabase, phoneHash, ipHash, false, 'otp_create_failed')
      return json({ error: 'تعذر إنشاء رمز التحقق', code: 'otp_create_failed' }, 500)
    }

    const sms = await sendSms(normalizedPhone, otp, requestAppHash)
    if (!sms.ok) {
      await supabase.from('phone_otps').update({ used: true }).eq('id', inserted.id)
      await logAttempt(supabase, phoneHash, ipHash, false, 'provider_unavailable')
      return json({ error: sms.message, code: 'provider_unavailable' }, 502)
    }
    await logAttempt(supabase, phoneHash, ipHash, true, 'sent')
    return json({ success: true })
  } catch {
    return json({ error: 'تعذر إرسال رمز التحقق', code: 'internal_error' }, 500)
  }
})
