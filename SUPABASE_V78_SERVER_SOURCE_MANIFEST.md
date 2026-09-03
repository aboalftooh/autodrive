# Supabase v78 server source manifest

Date: 2026-09-03
Purpose: bind the live v77/v78 authentication cutover to the exact server-source files packaged in `AutoDrive-v78.zip`.

## Edge Functions

- `supabase/functions/autodrive-registration/index.ts` — `364f867895b829f81ff2b3737791642ba560c8ad6a536566741d2fba71985fe6`
- `supabase/functions/autodrive-send-otp/index.ts` — `fcbe47b05c18aaa2c781b6f9ae9a5606331a30442c756994cccff7a765847dbf`
- `supabase/functions/autodrive-verify-otp/index.ts` — `fcbe47b05c18aaa2c781b6f9ae9a5606331a30442c756994cccff7a765847dbf`
- `supabase/functions/send-phone-otp/index.ts` — `015a377d7f1fc3b0a0765ba9f46c331f28d959243ad5632523f5291a6d6a0230`
- `supabase/functions/verify-phone-otp/index.ts` — `8866c15f8f62dacec20f2024d29e386fcdf7bff5b736b6f167d81754ca633663`

## Migrations

- `supabase/migrations/20260903065255_autodrive_v77_invite_code_auth_cutover.sql` — `8cd69113915aeb4c6f7f42d8f1d56e3cb8aaf9d2a58c5679d24d29f1eeb8bfff`
- `supabase/migrations/20260903070029_autodrive_v77_drop_join_request_runtime.sql` — `04ac495127466fbf9b452877cc82eb53960640177659c033d7ab6402b44c6516`
- `supabase/migrations/20260903070456_autodrive_v77_fix_join_code_activation.sql` — `21375804b4b40acf6527d7b78e775db5957081116048ff4a1f926caeed70a048`
- `supabase/migrations/20260903071800_autodrive_v77_clean_phone_entry_contract.sql` — `0e3367ff9499c6bb6bbd1ae84b04d7e017696cba508fd7f5e5c6d6baa9da6284`
- `supabase/migrations/20260903072900_autodrive_v77_fix_balance_conflict.sql` — `eab0bd2328dd3f128d3e5b53e3ef2f41cebd1518c8241e9ebdc484a9ddaa965e`

## Package

`AutoDrive-v78.zip` SHA-256: `5a5cc92f50ecfc895545bad5cb4b80640b75b3d17cf6fbfad7709b3dc5e08acc`

The package contains the full server files above. The hashes allow later sessions to prove whether a local/server-source file matches this closeout snapshot exactly.
