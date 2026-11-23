# SpendWise runbook

## Local stack

```powershell
# Postgres
docker compose -f infra/docker-compose.yml up -d postgres

# API
cd backend
.\.venv\Scripts\activate
alembic upgrade head
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

# Web
cd web
npm run dev
```

Health: `GET /health/live`, `GET /health/ready`

## Migrations

```powershell
cd backend
alembic upgrade head
alembic current
```

## Backups (Postgres)

Logical dump (example):

```powershell
docker exec spendwise-postgres pg_dump -U spendwise -d spendwise -Fc -f /tmp/spendwise.dump
docker cp spendwise-postgres:/tmp/spendwise.dump .\backups\spendwise-$(Get-Date -Format yyyyMMddHHmm).dump
```

Restore drill:

```powershell
# create empty DB, then:
docker cp .\backups\spendwise-YYYYMMDDHHMM.dump spendwise-postgres:/tmp/restore.dump
docker exec spendwise-postgres pg_restore -U spendwise -d spendwise --clean --if-exists /tmp/restore.dump
```

Script helper: `infra/backup_postgres.ps1`

## Rate limits / payload limits

| Setting | Default |
|---------|---------|
| `RATE_LIMIT_ENABLED` | true (off in `APP_ENV=test`) |
| `RATE_LIMIT_WRITE_PER_WINDOW` | 120 / 60s |
| `RATE_LIMIT_AI_PER_WINDOW` | 20 / 60s |
| `MAX_REQUEST_BODY_BYTES` | 1048576 |

## Incident notes

- Prefer rotating Supabase JWT signing keys via Supabase dashboard; API picks up JWKS.
- If AI provider fails, deterministic tool answers still work when consent is enabled.
- Account delete is immediate and irreversible for server-side finance rows.

## CI

GitHub Actions workflow `.github/workflows/backend-ci.yml` runs pytest against Postgres 16.
