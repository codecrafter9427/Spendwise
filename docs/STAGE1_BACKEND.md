# Stage 1 — Backend foundation

Status: **complete** (2026-11-19)

## Delivered

- `backend/` FastAPI modular monolith (Python 3.12)
- Config via pydantic-settings + placeholder `.env.example`
- Async SQLAlchemy 2 + PostgreSQL (`infra/docker-compose.yml`)
- Alembic migration `20261119_0001` → `app_users`
- Supabase-style JWT verification (JWKS in non-test; HS256 only when `APP_ENV=test`)
- Endpoints: `GET /health/live`, `GET /health/ready`, `GET|PATCH /api/v1/me`
- Idempotent `app_users` provisioning from JWT `sub`
- pytest: auth rejection, profile, isolation (14 passed)

## How to run

See [`backend/README.md`](../backend/README.md).

## Next (Stage 2)

Categories, transactions, budgets, analytics APIs + integration tests.
