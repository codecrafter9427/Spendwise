# Stage 2 — Core finance API

Status: **complete** (2026-11-19)

## Delivered

- Tables: `categories`, `transactions`, `monthly_budgets` (Alembic `20261119_0002`)
- Seeded 18 default categories (stable UUID5 ids aligned with Android seeder names/icons)
- Endpoints under `/api/v1`:
  - Categories: `GET`, `POST`, `PATCH /{id}`
  - Transactions: `GET` (cursor page), `POST`, `GET|PATCH|DELETE /{id}`
  - Budgets: `GET`, `PUT /{year}/{month}`
  - Analytics: `GET /summary`, `GET /categories`
- Invariants: integer paise, INR-only, soft delete, optimistic `version` → `409 VERSION_CONFLICT`
- Types at API boundary: lowercase `income` / `expense`
- Isolation and money/validation coverage in pytest

## Next (Stage 3)

Responsive Next.js web client against this API (PRD §15).
