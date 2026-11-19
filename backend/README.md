# SpendWise Backend (Stage 1)

FastAPI modular monolith: config, PostgreSQL, Alembic, Supabase JWT verification, and `/api/v1/me`.

## Requirements

- Python 3.12+
- Docker (for local PostgreSQL)

## Setup

```bash
cd backend
python -m venv .venv

# Windows
.venv\Scripts\activate

pip install -e ".[dev]"
copy .env.example .env
```

Start Postgres:

```bash
docker compose -f ../infra/docker-compose.yml up -d postgres
```

Run migrations:

```bash
alembic upgrade head
```

Start API:

```bash
uvicorn app.main:app --reload --port 8000
```

## Tests

Tests use a disposable Postgres database (`spendwise_test`) on the same Docker Postgres instance, plus HS256 test JWTs (`APP_ENV=test`).

```bash
pytest
```

## Endpoints (Stage 1–2)

| Method | Path | Auth |
|--------|------|------|
| GET | `/health/live` | No |
| GET | `/health/ready` | No |
| GET/PATCH | `/api/v1/me` | Bearer JWT |
| GET/POST | `/api/v1/categories` | Bearer JWT |
| PATCH | `/api/v1/categories/{id}` | Bearer JWT |
| GET/POST | `/api/v1/transactions` | Bearer JWT |
| GET/PATCH/DELETE | `/api/v1/transactions/{id}` | Bearer JWT |
| GET | `/api/v1/budgets` | Bearer JWT |
| PUT | `/api/v1/budgets/{year}/{month}` | Bearer JWT |
| GET | `/api/v1/analytics/summary` | Bearer JWT |
| GET | `/api/v1/analytics/categories` | Bearer JWT |

OpenAPI: http://localhost:8000/docs
