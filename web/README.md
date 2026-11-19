# SpendWise Web (Stage 3)

Responsive Next.js client for the SpendWise FastAPI backend. Auth is Supabase; finance data is loaded only through `/api/v1`.

## Prerequisites

1. Backend running on `http://localhost:8000` (Postgres migrated).
2. A Supabase project with Email auth enabled.
3. Backend `.env` JWT settings pointing at the same Supabase project.

## Setup

```bat
cd web
copy .env.example .env.local
```

Fill:

```env
NEXT_PUBLIC_SUPABASE_URL=https://<project-ref>.supabase.co
NEXT_PUBLIC_SUPABASE_ANON_KEY=<anon-key>
NEXT_PUBLIC_API_BASE_URL=http://localhost:8000
```

Install and run:

```bat
npm install
npm run dev
```

Open http://localhost:3000

## Screens

- Login / sign-up
- Dashboard (month summary, budget, recent txs)
- Transactions (create, filter, delete)
- Analytics (totals + category breakdown)
- Settings (profile + sign out)

## Notes

- Do not put the Supabase service-role key in this app.
- CORS on the API must allow `http://localhost:3000` (default in backend `.env.example`).
