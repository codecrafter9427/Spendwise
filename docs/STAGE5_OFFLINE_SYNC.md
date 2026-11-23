# Stage 5 — Offline sync (push/pull + outbox)

Status: **complete** (2026-11-19)

## Backend

- Tables: `sync_changes` (per-user sequenced changelog), `idempotency_records` (operation_id + request hash)
- Alembic: `20261119_0003_sync_tables`, `20261119_0004_backfill_sync_changes`
- Android also hydrates from `GET /transactions` + `GET /budgets` so pre-changelog web rows appear on first sync
- Domain mutations (transactions/budgets) append `SyncChange` rows
- `POST /api/v1/sync/push` — bounded batch of upsert/delete ops; per-item `accepted` / `duplicate` / `conflict` / `rejected`
- `GET /api/v1/sync/pull?cursor=&limit=` — opaque integer cursor over `sync_changes.sequence`
- pytest: `tests/test_sync_api.py` (push, idempotent replay, payload mismatch, pull isolation)

## Android

- Room v3: `transactions.deletedAt` soft delete; `outbox_operations` table
- Mutations write Room first, enqueue outbox (`operationId`, payload, `baseVersion`), schedule WorkManager
- `SyncWorker` + `SyncEngine`: push pending → pull pages → advance cursor only after apply
- Conflicts stay in outbox as `CONFLICT` (no silent last-write-wins)
- Settings: pending/conflict counts, **Sync now**
- Sign-in / import still wake a sync pass

## Verify

```bash
cd backend
.\.venv\Scripts\python.exe -m pytest -q

# Android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

## Next (Stage 6)

Realtime wake signals so clients pull sooner; keep push/pull as source of truth.
