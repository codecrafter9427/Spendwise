# Stage 4 — Android account linking + online CRUD

Status: **complete** (2026-11-19)

## Delivered

- `INTERNET` + cleartext config for local API (`10.0.2.2`)
- Retrofit SpendWise API client + Supabase Auth REST (sign in / sign up / refresh)
- Room v2 migration: `remoteId` + `serverVersion` on transactions/budgets; `remoteId` on categories
- Stable default category UUID5 mapping shared with backend
- Settings → Account: sign in/up, upload local data, sign out
- Online create/update/delete for transactions and budgets when signed in (local Room remains UI source)

## Configure

Add to `local.properties` (see `local.properties.example`):

```properties
spendwise.api.baseUrl=http://10.0.2.2:8000
spendwise.supabase.url=https://YOUR_PROJECT_REF.supabase.co
spendwise.supabase.anonKey=YOUR_PUBLISHABLE_OR_ANON_KEY
```

Sync Gradle, run backend on host `:8000`, then run the app on an emulator.

## Flow

1. Use the app offline as before.
2. Settings → sign in with the same Supabase account as web.
3. Tap **Upload local data to cloud** once.
4. New edits while signed in also POST/PATCH/DELETE to the API.

## Next (Stage 5)

Durable offline outbox, pull cursor, conflict UI, WorkManager sync.
