# Security and privacy

## Authentication and authorization

- Clients authenticate with Supabase-issued JWTs.
- FastAPI verifies JWT via JWKS (HS256 only in `APP_ENV=test`).
- Identity is always JWT `sub`. Clients cannot supply another user id.
- Every finance, sync, analytics, AI, export, and delete path is user-scoped.

## Transport and browser

- Production must terminate TLS at the edge (load balancer / reverse proxy).
- CORS is an allowlist (`CORS_ORIGINS`); it is not authorization.
- API responses include `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, and `Permissions-Policy`.
- Request bodies are size-capped (`MAX_REQUEST_BODY_BYTES`, default 1 MiB).

## Rate limiting

- In-process sliding windows protect write methods and `/api/v1/ai/*`.
- Disabled automatically when `APP_ENV=test`.
- Multi-instance deployments should move limits to a shared store (Stage 8 documents this; Redis is not required yet).

## AI

- Opt-in per user (`ai_enabled`) and server kill switch (`AI_FEATURE_ENABLED`).
- Allowlisted read tools only; no mutations; no arbitrary SQL.
- Rejects investment/tax/credit advice prompts.
- Optional LLM provider never receives DB credentials.

## Data lifecycle

- Soft deletes for transactions (`deleted_at`) support sync.
- `GET /api/v1/account/export` returns a JSON snapshot of the caller's data.
- `DELETE /api/v1/account` removes the caller's transactions, budgets, custom categories, sync/idempotency rows, and profile.
- Default shared categories remain.

## Secrets

- Never commit `.env`, `local.properties` secrets, or service-role keys.
- Store `AI_PROVIDER_API_KEY` and DB credentials in the host secret manager / CI secrets.
