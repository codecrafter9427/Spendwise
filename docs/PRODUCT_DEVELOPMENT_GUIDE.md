# SpendWise — Product Development & Deployment Guide

**Purpose:** One document so you understand *what* we built, *why* it was shaped this way, *how* we shipped it live, and *how to repeat* this pattern for future products.

**Audience:** You (builder / founder / engineer learning full-stack product delivery).

**Live stack (as of Stage 8 deploy):**

| Piece | Where it runs |
|-------|----------------|
| Auth + Postgres | Supabase |
| FastAPI backend | Render (`https://spendwise-efpk.onrender.com`) |
| Next.js web | Vercel (`https://spendwise.vercel.app` or your Domains URL) |
| Android app | Device / Play sideload; points at Render + Supabase |

---

## 1. Product in one paragraph

SpendWise is a personal finance tracker. The **Android app works offline** (Room database). When the user signs in, the same identity (Supabase Auth JWT) unlocks a **shared cloud API** so **web and phone** see the same transactions, budgets, and analytics. Money is always **integer paise** (₹1 = 100), never floating-point currency.

---

## 2. The big idea: three layers

Think of every similar product as three layers:

```text
┌─────────────────────────────────────────────────────────┐
│  Clients                                                │
│  Android (offline-first)  ·  Next.js web (online)       │
└─────────────────────┬───────────────────────────────────┘
                      │ HTTPS + Bearer JWT
┌─────────────────────▼───────────────────────────────────┐
│  Your API (FastAPI modular monolith)                    │
│  Auth check → business rules → DB writes → sync/outbox  │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│  Managed platform                                       │
│  Supabase Auth (users/JWT) + Supabase Postgres (data)   │
└─────────────────────────────────────────────────────────┘
```

**Why not put business logic only in Supabase client libraries?**

- Android already had Room + local rules; rewriting everything to RLS-only would fight the offline MVP.
- One API keeps **money rules, sync, rate limits, AI tools, export/delete** in one place you control.
- Web and Android share the same `/api/v1/...` contract.

**Why Supabase Auth instead of rolling your own passwords?**

- Login, JWT, JWKS verification are solved problems.
- Your API only verifies the token (`sub` = user id) and stores app data keyed by that id.

---

## 3. What we developed (by stage)

Delivery was **staged** so each slice was shippable and testable.

| Stage | What shipped | Mental model |
|-------|----------------|--------------|
| **0 / Android MVP** | Compose UI, Room, budgets, analytics, paise | Offline product that works alone |
| **1 Backend foundation** | FastAPI, config, health, Docker/Postgres local | API process + DB connectivity |
| **2 Core API** | Users, categories, transactions, budgets, analytics | CRUD + domain rules behind JWT |
| **3 Web** | Next.js App Router, login, dashboard pages | Browser client of the same API |
| **4 Android cloud** | Sign-in, upload local → cloud, online edits | Link device identity to JWT `sub` |
| **5 Offline sync** | Outbox, soft deletes, push/pull, WorkManager | Eventually consistent multi-device |
| **6 Realtime** | SSE wake / web refresh bridge | Faster “other device changed” |
| **7 AI assistant** | Allowlisted tools + optional LLM | Safe Q&A over *your* verified data |
| **8 Hardening** | Rate limits, headers, export/delete, CI, deploy docs | Production readiness |

**Design constraints we kept:**

- Do **not** rewrite Android from scratch.
- Money as **integer paise**, INR.
- Identity = JWT **`sub`**.
- Soft deletes / versions / outbox for sync.
- No Redis/Kafka/microservices unless clearly needed (modular monolith is enough).

Detailed stage notes live in `docs/STAGE1_*.md` … `docs/STAGE8_*.md`. End-user flows: `docs/USER_MANUAL.md`.

---

## 4. Repository map (what lives where)

```text
SpendWise/
├── app/                    # Android (Kotlin, Compose, Room, Hilt)
├── backend/                # FastAPI + Alembic + Dockerfile
│   ├── app/                # routes, services, models, middleware
│   ├── migrations/         # schema versions
│   ├── entrypoint.sh       # alembic upgrade → uvicorn
│   └── Dockerfile
├── web/                    # Next.js (src/app/...)
├── infra/                  # local docker-compose Postgres
├── docs/                   # stage docs, runbook, this guide
├── render.yaml             # Render Blueprint for API
└── .github/workflows/      # backend CI
```

**Client config (secrets stay out of git):**

- Android: `local.properties` → `spendwise.api.baseUrl`, Supabase URL + anon key  
- Web: `web/.env.local` → `NEXT_PUBLIC_*`  
- Backend: `backend/.env` → `DATABASE_URL`, Supabase JWKS/issuer, `CORS_ORIGINS`

---

## 5. Core domain rules (reuse these ideas)

1. **Money:** store `Long` paise; format for UI only.
2. **IDs:** server/cloud rows use client-stable **UUIDs** for sync; Android may keep local Long PKs with a mapping.
3. **Auth:** API trusts Supabase JWT via JWKS; never trust a client-sent “user id” alone.
4. **Sync:** write locally → outbox → push; pull remote changes → merge by version / soft-delete.
5. **AI:** tools that read *your* APIs/DB summaries; don’t let the model invent balances without a tool result.
6. **Hardening:** body size limits, rate limits, security headers, export + account data delete.

---

## 6. Local development (how you run it on a laptop)

### 6.1 Postgres

```powershell
docker compose -f infra/docker-compose.yml up -d postgres
```

### 6.2 API

```powershell
cd backend
.\.venv\Scripts\activate
# .env from .env.example — Supabase JWT fields + DATABASE_URL to local Postgres
alembic upgrade head
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

- Health: `http://127.0.0.1:8000/health/live`  
- Docs: `http://127.0.0.1:8000/docs`

### 6.3 Web

```powershell
cd web
# .env.local with NEXT_PUBLIC_SUPABASE_* and NEXT_PUBLIC_API_BASE_URL=http://localhost:8000
npm run dev
```

Open `http://localhost:3000`.

### 6.4 Android

- Emulator API base: `http://10.0.2.2:8000`  
- USB phone: `http://127.0.0.1:8000` + `adb reverse tcp:8000 tcp:8000`  
- Same Supabase project as web/backend.

---

## 7. Production architecture (what “hosting” means)

You do **not** host one giant machine. You compose managed free/cheap services:

```text
User browser ──► Vercel (Next.js static/SSR)
                      │
                      │ fetch + Bearer token
                      ▼
                 Render (Docker: FastAPI)
                      │
          ┌───────────┴───────────┐
          ▼                       ▼
   Supabase Auth            Supabase Postgres
   (login / JWT)            (app tables via Alembic)
```

| Concern | Service | Why |
|---------|---------|-----|
| User signup/login | Supabase Auth | Free tier, JWKS, no password storage in your API |
| Database | Supabase Postgres | Managed Postgres; same project as Auth |
| API process | Render Web Service (Docker) | Runs migrations + uvicorn; public HTTPS |
| Website | Vercel | Native Next.js deploys |
| Mobile | APK/AAB pointing at HTTPS API | No `adb reverse` in production |

**Branch used for deploy:** `cursor/stage-8-hardening`

---

## 8. Deployment walkthrough (exactly what we did)

### Step A — Supabase project

1. Create project; save **database password**.
2. Note project ref (example): `wxdrpfyegrcqbufoequs`.
3. Auth URLs later point at Vercel.
4. Copy **anon/public** key for web + Android (never the service-role key in clients).

### Step B — Database URL for Render

Render free is **IPv4**. Supabase **direct** host `db.…supabase.co` is often **IPv6-only** → `Network is unreachable`.

Use **Connect → Session / Shared pooler** (host contains `pooler.supabase.com`):

```text
postgresql+asyncpg://postgres.PROJECT_REF:PASSWORD@aws-….pooler.supabase.com:5432/postgres?ssl=require
```

Rules we learned the hard way:

| Mistake | Symptom | Fix |
|---------|---------|-----|
| Plain `postgresql://` without asyncpg | `No module named 'psycopg'` | Use `+asyncpg` (API also rewrites this) |
| Direct `db.` host from Render | `Network is unreachable` | Pooler host |
| `?pgbouncer=true` | `unexpected keyword argument 'pgbouncer'` | Prisma-only; use `ssl=require` |
| `@` in password unescaped | Wrong host / auth failures | Encode `@` as `%40` |
| `%` in URL via Alembic ConfigParser | `invalid interpolation syntax` | Pass URL directly to engine (fixed in `migrations/env.py`) |
| `DATABASE_URL` missing | Connect to `127.0.0.1:5432` | Set env on Render |

### Step C — Render API

1. Connect GitHub App to repo `codecrafter9427/Spendwise` (or Public Git URL).
2. Web Service, Docker, context `backend`, branch `cursor/stage-8-hardening`.
3. Health check: `/health/live`.
4. Env vars (minimum):

| Key | Role |
|-----|------|
| `DATABASE_URL` | Pooler asyncpg URL + SSL |
| `SUPABASE_URL` | `https://REF.supabase.co` |
| `SUPABASE_JWKS_URL` | `…/auth/v1/.well-known/jwks.json` |
| `SUPABASE_JWT_ISSUER` | `…/auth/v1` |
| `SUPABASE_JWT_AUDIENCE` | `authenticated` |
| `CORS_ORIGINS` | Exact Vercel origin(s), comma-separated |
| `APP_ENV` | `production` |

Do **not** set `JWT_TEST_SECRET` in production.

5. Container entrypoint: `alembic upgrade head` → `uvicorn` on `$PORT`.
6. Success looks like: migrations run, `Application startup complete`, `/health/live` → 200.
7. Live example: `https://spendwise-efpk.onrender.com`

**Note:** Free Render sleeps when idle; first request can be slow or briefly fail — Retry is expected.

### Step D — Vercel web

1. Import same GitHub repo.
2. **Root Directory = `web`** (critical; wrong root → platform 404).
3. Branch: `cursor/stage-8-hardening`.
4. Env:

| Key | Value |
|-----|--------|
| `NEXT_PUBLIC_SUPABASE_URL` | Supabase project URL |
| `NEXT_PUBLIC_SUPABASE_ANON_KEY` | anon key |
| `NEXT_PUBLIC_API_BASE_URL` | `https://spendwise-efpk.onrender.com` |

5. Redeploy after any env change (`NEXT_PUBLIC_*` are build-time).

### Step E — Wire the triangle

1. Render `CORS_ORIGINS` = `https://your-app.vercel.app` (exact match).
2. Supabase Auth → Site URL + redirect allow list for that Vercel URL.
3. Dashboard “Failed to fetch” without CORS = browser blocked, not always a dead API.
4. “SUPABASE_JWKS_URL is not configured” = missing Render auth env vars.

### Step F — Android production pointing

```properties
spendwise.api.baseUrl=https://spendwise-efpk.onrender.com
spendwise.supabase.url=https://YOUR_REF.supabase.co
spendwise.supabase.anonKey=YOUR_ANON_KEY
```

Rebuild; no `adb reverse` for cloud.

---

## 9. Request path (mental model when debugging)

```text
Web page → fetch(NEXT_PUBLIC_API_BASE_URL + /api/v1/...)
        → Authorization: Bearer <Supabase access token>
        → Render FastAPI
        → Verify JWT via JWKS
        → Load/update rows for user_id = token.sub
        → JSON response
```

**Where to look when something fails:**

| Symptom | Likely layer |
|---------|----------------|
| Login page broken | Supabase URL/anon key on Vercel |
| Login OK, “Failed to fetch” | CORS, wrong API base URL, Render asleep |
| API 401 / JWKS errors | Render Supabase JWKS/issuer/audience |
| Migrations fail | `DATABASE_URL` |
| Web 404 “page doesn’t exist” (Vercel chrome) | Wrong deployment / Root Directory not `web` |
| Phone can’t reach local API | Emulator `10.0.2.2` vs USB `adb reverse` |

---

## 10. Security & ops checklist (short)

- Clients get **anon** key only; service role stays on server (we didn’t need it for this design).
- Never commit `.env`, `.env.local`, or `local.properties` secrets.
- Reset DB password if it was pasted into chat/logs; update Render.
- Rate limits + body limits (Stage 8) protect free tiers from abuse.
- Export / delete endpoints support basic data rights flows.
- See `docs/SECURITY_AND_PRIVACY.md` and `docs/RUNBOOK.md`.

---

## 11. How to build the *next* product with the same clarity

Use this template every time:

### A. Product slice

1. **Offline or online-first?** (SpendWise: offline Android first.)
2. **One shared identity?** (SpendWise: Supabase Auth `sub`.)
3. **One source of truth for money/rules?** (SpendWise: FastAPI + Postgres.)

### B. Technical slice

1. Domain model + money/IDs rules written down first.
2. API contract (`/api/v1/...`) before polishing UI.
3. Migrations (Alembic) from day one — schema is versioned.
4. Clients are thin: auth + call API + local cache if needed.

### C. Delivery slice

1. Local Docker Postgres + API + one client.
2. Stage features (auth → CRUD → sync → polish).
3. Host with **managed** pieces: Auth/DB, API container, web host.
4. Env vars matrix written once (local vs prod).
5. Smoke: health → login → one authenticated GET → one write.

### D. Deploy matrix (copy for new apps)

| Env var / setting | Local | Production |
|-------------------|-------|------------|
| Database | localhost Docker | Hosted Postgres (pooler if IPv4) |
| API URL | `localhost:8000` / `10.0.2.2` | `https://api…` |
| CORS | `http://localhost:3000` | `https://app…` |
| Auth issuer/JWKS | Same Supabase project | Same |
| Web API base | localhost | Render URL |

---

## 12. What “done” looked like for SpendWise hosting

- [x] Alembic migrations apply against Supabase pooler from Render  
- [x] `GET /health/live` → 200 on Render  
- [x] Vercel serves SpendWise UI (Root Directory `web`)  
- [x] `CORS_ORIGINS` includes Vercel origin  
- [x] Supabase JWKS/issuer/audience set on Render  
- [x] Signed-in dashboard can call API (after CORS + JWKS)  
- [ ] Optional: Android `local.properties` → production API  
- [ ] Optional: reset leaked DB password; custom domains  

---

## 13. Related docs in this repo

| Doc | Use when |
|-----|----------|
| `docs/USER_MANUAL.md` | How users use the product |
| `docs/DEPLOY_FREE.md` | Short deploy checklist |
| `docs/RUNBOOK.md` | Ops / local / backups |
| `docs/SECURITY_AND_PRIVACY.md` | Threat / privacy notes |
| `docs/STAGE5_OFFLINE_SYNC.md` … `STAGE8_*.md` | Feature-deep dives |
| `docs/ANDROID_BACKEND_MAPPING.md` | Local ↔ cloud field mapping |

---

## 14. Takeaway

SpendWise is not “an Android app” or “a website.” It is a **product system**:

1. **Clients** for different contexts (offline phone vs browser).  
2. **One API** for rules and sync.  
3. **Managed auth + DB** so you don’t operate passwords and Postgres hardware.  
4. **Staged delivery** so each week adds a vertical slice.  
5. **Explicit env wiring** (CORS, JWKS, pooler URL) — most “hosting bugs” were configuration, not business logic.

When you build the next product, draw the three-layer diagram first, write the env matrix second, then code stage 1. Deployment will feel familiar because the shape stays the same even when the domain changes.
