# Free deploy guide (Supabase + Render + Vercel)

Branch: `cursor/stage-8-hardening`

## 1. Supabase Postgres URL

In the project dashboard click **Connect** (top bar) → **Session pooler** → copy URI.

Prefer this form (asyncpg + SSL). Plain `postgresql://` is also accepted — the API rewrites it to asyncpg:

```text
postgresql+asyncpg://USER:PASSWORD@HOST:5432/postgres?ssl=require
```

URL-encode special characters in the password. For Supabase hosts, include `ssl=require`.

Also keep:

```text
SUPABASE_URL=https://YOUR_REF.supabase.co
SUPABASE_JWKS_URL=https://YOUR_REF.supabase.co/auth/v1/.well-known/jwks.json
SUPABASE_JWT_ISSUER=https://YOUR_REF.supabase.co/auth/v1
SUPABASE_JWT_AUDIENCE=authenticated
```

Do **not** set `JWT_TEST_SECRET` in production.

## 2. Render API

Option A — Blueprint: New → Blueprint → this repo → `render.yaml`.

Option B — Manual Web Service:

- Branch: `cursor/stage-8-hardening`
- Runtime: Docker
- Dockerfile path: `backend/Dockerfile`
- Docker context: `backend`
- Plan: Free
- Health check: `/health/live`

Env (required):

| Key | Example |
|-----|---------|
| `DATABASE_URL` | `postgresql+asyncpg://...?...ssl=require` |
| `SUPABASE_URL` | `https://xxx.supabase.co` |
| `SUPABASE_JWKS_URL` | `https://xxx.supabase.co/auth/v1/.well-known/jwks.json` |
| `SUPABASE_JWT_ISSUER` | `https://xxx.supabase.co/auth/v1` |
| `SUPABASE_JWT_AUDIENCE` | `authenticated` |
| `CORS_ORIGINS` | your Vercel URL (update after step 3) |
| `APP_ENV` | `production` |

The container runs `alembic upgrade head` then uvicorn on `$PORT`.

Smoke test: `https://YOUR-API.onrender.com/health/live`

## 3. Vercel web

- Root directory: `web`
- Branch: `cursor/stage-8-hardening`
- Env:
  - `NEXT_PUBLIC_SUPABASE_URL`
  - `NEXT_PUBLIC_SUPABASE_ANON_KEY`
  - `NEXT_PUBLIC_API_BASE_URL` = `https://YOUR-API.onrender.com`

## 4. Wire auth + CORS

1. Render: set `CORS_ORIGINS` to `https://YOUR-APP.vercel.app` (optionally add `,http://localhost:3000`).
2. Supabase Auth URL config:
   - Site URL = Vercel URL
   - Redirect allow list includes `https://YOUR-APP.vercel.app/**`

## 5. Android

```properties
spendwise.api.baseUrl=https://YOUR-API.onrender.com
spendwise.supabase.url=https://YOUR_REF.supabase.co
spendwise.supabase.anonKey=YOUR_ANON_KEY
```

Rebuild. No `adb reverse` for the live API.

## Notes

- Render free services sleep when idle; first request can be slow.
- If DB connect fails, try Supabase “Session mode” pooler host vs direct `db.` host.
