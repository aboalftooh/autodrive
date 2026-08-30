-- Keep one current OTP per normalized phone. Existing records are retained;
-- the application marks prior unused records as used before inserting a replacement.
CREATE UNIQUE INDEX IF NOT EXISTS phone_otps_one_unused_per_phone
ON public.phone_otps (phone)
WHERE used = false;
