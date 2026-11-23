# Stage 6 — Realtime wake signals

Status: **complete** (2026-11-23)

## Approach

Push/pull remains the source of truth. Realtime only **wakes** clients to pull sooner.

- Backend: authenticated SSE at `GET /api/v1/realtime/events`
- On each `SyncChange` write, publish a minimal event: `entity_type`, `entity_id`, `operation`, `version`, `cursor`
- In-process fan-out (`RealtimeHub`) — fine for single API process; missed events repaired by pull
- Web: fetch-stream SSE + refresh dashboard/transactions/analytics; also refresh on window focus
- Android: foreground SSE via OkHttp (process lifecycle); pull-on-resume; WorkManager still runs sync

## Not used

Supabase Realtime was skipped because SpendWise app data lives in the FastAPI Postgres, not the Supabase project DB.

## Verify

```bash
cd backend
.\.venv\Scripts\python.exe -m pytest tests/test_realtime_api.py tests/test_sync_api.py -q
```

Manual: open web + Android signed in as the same user; create a transaction on web; Android should sync without tapping **Sync now** (while app is foreground).

## Next (Stage 7)

AI assistant with allowlisted FastAPI tools only.
