# Stage 8 — Production hardening

Status: **complete** (2026-11-23)

## Delivered

- Security response headers + `X-Request-ID` request logging
- Request body size limit
- In-process rate limits for writes and AI endpoints
- Account **export** (`GET /api/v1/account/export`) and **delete** (`DELETE /api/v1/account`)
- Web Settings actions for export download and account deletion
- Docs: `SECURITY_AND_PRIVACY.md`, `RUNBOOK.md`
- Backup helper script + GitHub Actions backend CI

## Verify

```powershell
cd backend
.\.venv\Scripts\python.exe -m pytest -q
```

## Remaining ops (deploy environment)

- Terminate TLS at the edge
- Store secrets outside git
- Schedule `pg_dump` backups and practice restore
- For multi-replica rate limits, introduce a shared limiter when needed
