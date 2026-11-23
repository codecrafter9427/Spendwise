# Stage 7 — AI assistant (read-only tools)

Status: **complete** (2026-11-23)

## Guardrails

- LLM never gets DB credentials or arbitrary SQL
- Allowlisted tools only: `monthly_summary`, `category_spend`, `largest_expenses`, `budget_status`, `compare_periods`
- All tools are user-scoped via JWT `sub`
- No create/update/delete of finance data
- Rejects investment/tax/credit-style prompts
- Requires per-user consent: `app_users.ai_enabled`
- Server kill switch: `AI_FEATURE_ENABLED`

## Modes

1. **Deterministic** (default): route question → run tool → format verified numbers  
2. **LLM** (optional): if `AI_PROVIDER_API_KEY` is set, explain tool JSON via OpenAI-compatible API; falls back to deterministic on provider failure

## API

- `POST /api/v1/ai/chat` `{ message, year?, month? }`
- `PATCH /api/v1/me` `{ ai_enabled: true }`

## Web

- Settings: enable AI consent
- Assistant page: ask spending questions for a selected month

## Migrate

```bash
cd backend
alembic upgrade head
```

## Next (Stage 8)

Production hardening: rate limits, headers, backups, deletion/export.
