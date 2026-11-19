# SpendWise Android ↔ Backend Mapping (Phase 2 — Stage 0)

**Companion to:** [`ANDROID_AUDIT.md`](ANDROID_AUDIT.md)  
**PRD source:** SpendWise Backend Architecture & Cursor-Ready PRD (Phase 2)  
**Scope:** Mapping and import/sync implications only. No schema or backend code changes in Stage 0.

---

## 1. Repository layout recommendation

| PRD suggestion | Actual today | Decision for later stages |
|----------------|--------------|---------------------------|
| `spendwise/android/` | Android project at **repo root** (`app/`, `gradle/`, etc.) | **Do not relocate** the existing Android tree in early stages. Add `backend/`, `web/`, `docs/`, `infra/` beside the current root layout. |

Stage 0 already creates `docs/` at the repository root.

---

## 2. Identity and tenancy

| Concept | Android today | Server (PRD) | Mapping |
|---------|---------------|--------------|---------|
| User | Implicit single device DB | `app_users.id` = Supabase Auth subject UUID | Provision `app_users` on first authenticated API use (idempotent). |
| Ownership | N/A | Every row scoped by `user_id` from JWT `sub` | Never accept client-supplied `user_id` as authority. |
| Profile fields | Theme in DataStore only | `display_name`, `base_currency`, `timezone` | Default `base_currency = INR`, timezone from device/profile at account link. Theme stays local unless product later syncs preferences. |

---

## 3. Money and currency

| Android | Server | Mapping |
|---------|--------|---------|
| `amountMinor: Long` (paise) | `amount_minor BIGINT` | **Direct 1:1.** No float migration required. |
| Display INR hardcoded | `currency CHAR(3)` ISO 4217 | Persist `INR` on all imported/created rows initially. |
| `Money.MAX_AMOUNT_MINOR` | Server validation bounds | Mirror max (or document shared limit) in FastAPI validators. |
| Amount must be > 0 | `amount_minor > 0` | Same invariant; type (income/expense) decides sign in summaries, not storage. |

API responses should return integer minor units + currency metadata (never float JSON numbers for money).

---

## 4. Identifiers

### 4.1 Problem

Android uses **local `Long` primary keys**. Server requires **client-generated UUIDs** for stable multi-device identity and idempotent creates.

### 4.2 Strategy (Stage 4 import / Stage 5 sync)

1. **Before first upload**, assign a stable UUID per local row and persist it on device (new column e.g. `remoteId` / `serverId`, or dual-key table). Keep existing `Long` PK for local FKs and UI navigation during transition.
2. **Default categories:** do not invent per-device UUIDs that diverge. Prefer server-seeded default category UUIDs mapped by stable natural key: `(type, name)` or `(type, icon)` — see §5.
3. **Transactions / budgets:** generate UUID v4 (or ULID) locally; use that UUID as create `id` in `POST /api/v1/transactions` and sync push payloads.
4. Retain local integer PK until all UI/repos are UUID-aware if needed; mapping table `local_id ↔ uuid` is acceptable interim.

**Do not** rename Room tables or destructively wipe user data merely to match server naming.

---

## 5. Categories

### 5.1 Field map

| Android (`categories`) | Server (`categories`) | Notes |
|------------------------|----------------------|-------|
| `id: Long` | `id: UUID` | Defaults: map via seed catalog (§5.2). Customs (later): client UUID. |
| — | `user_id` nullable | Defaults: `user_id` null (system); customs: owner UUID. |
| `name` | `name` | Direct |
| `type` (`INCOME`/`EXPENSE`) | `type` (`income`/`expense` or enum) | **Normalize casing** at API boundary; Android stores `TransactionType.name` uppercase. Recommend server accept uppercase or document lowercase conversion in clients. |
| `icon` | `icon_key` | Rename at boundary only; values are Material icon key strings. |
| `isDefault` | `is_default` | Direct |
| — | `archived_at` | Not present locally; treat missing as active. |
| — | timestamps | Set on server; local has no category timestamps. |

### 5.2 Default category seed map (stable natural keys)

Use this catalog when linking Android seed IDs → server default UUIDs (server assigns UUIDs once; Android stores mapping after first sync/import):

| Local ID | Name | Type | icon |
|---------:|------|------|------|
| 1 | Food & Dining | EXPENSE | restaurant |
| 2 | Transportation | EXPENSE | directions_car |
| 3 | Shopping | EXPENSE | shopping_bag |
| 4 | Bills & Utilities | EXPENSE | receipt |
| 5 | Health | EXPENSE | favorite |
| 6 | Entertainment | EXPENSE | sports_esports |
| 7 | Education | EXPENSE | school |
| 8 | Travel | EXPENSE | flight |
| 9 | Groceries | EXPENSE | shopping_basket |
| 10 | Housing | EXPENSE | home |
| 11 | Personal Care | EXPENSE | person |
| 12 | Other | EXPENSE | more_horiz |
| 101 | Salary | INCOME | work |
| 102 | Freelance | INCOME | laptop |
| 103 | Business | INCOME | business |
| 104 | Investment Returns | INCOME | trending_up |
| 105 | Gift | INCOME | card_giftcard |
| 106 | Other Income | INCOME | account_balance_wallet |

**Import rule:** resolve `category_id` by matching `(type, name)` (or icon) against server defaults after login; fail the batch item if unmatched rather than inventing a wrong category.

Custom categories are a **later** product feature (PRD allows `POST /categories`); Android currently cannot create them.

---

## 6. Transactions

### 6.1 Field map

| Android (`transactions`) | Server (`transactions`) | Mapping |
|--------------------------|-------------------------|---------|
| `id: Long` | `id: UUID` | Generate/persist UUID before upload (§4). |
| — | `user_id` | From JWT subject. |
| `type` | `type` | Same semantics; normalize case at boundary. |
| `amountMinor` | `amount_minor` | Direct. |
| — | `currency` | Default `INR`. |
| `categoryId: Long` | `category_id: UUID` | Via category mapping table. |
| `description` | `description` | Direct; max 120 on Android — enforce same or document server max. |
| `transactionDate` (epoch day) | `transaction_date: DATE` | `LocalDate.ofEpochDay(n).toString()` → `YYYY-MM-DD`. **Preserve user date**; never replace with server receipt date. |
| `createdAt` (epoch millis) | `created_at` timestamptz | Convert millis → UTC instant. Prefer preserving original client `created_at` on import if API allows; otherwise document server-assigned create time for imports. |
| `updatedAt` (epoch millis) | `updated_at` timestamptz | Same. |
| — | `version: INT` | Start at 1 on create; required for PATCH/DELETE expected_version. |
| Hard `@Delete` | `deleted_at` soft delete | Stage 4/5: switch local delete to soft delete + outbox; until then import only live rows. |

### 6.2 API alignment (reference)

- `POST /api/v1/transactions` with client UUID + `Idempotency-Key`.
- `PATCH` / `DELETE` with expected version → `409 VERSION_CONFLICT` on stale.
- List: cursor pagination + filters (month/type/category/search) mirroring Android filters.

### 6.3 Validation parity

| Rule | Android | Server should |
|------|---------|---------------|
| Amount > 0, ≤ max, 2 dp input | `Money` + validator | Same |
| Category matches type | Validator | Same |
| Description ≤ 120 | Validator | Same (or higher with documented Android trim) |
| Date required | Validator | Same |

---

## 7. Monthly budgets

| Android (`monthly_budgets`) | Server (`monthly_budgets`) | Mapping |
|-----------------------------|----------------------------|---------|
| `id: Long` | `id: UUID` | Generate UUID for sync identity. |
| — | `user_id` | JWT subject. |
| `year`, `month` | `year`, `month` | Direct (`month` 1–12). |
| `amountMinor` | `amount_minor` | Direct; amount ≥ 0 on server (Android budget UI should stay non-negative). |
| — | `currency` | `INR`. |
| Unique `(year, month)` | Unique `(user_id, year, month, currency)` | Add tenancy + currency. |
| `createdAt` / `updatedAt` | timestamps + `version` | Add version for conflict checks. |
| Upsert via insert-or-update | `PUT /api/v1/budgets/{year}/{month}` | Natural key upsert; map local row after ACK. |

---

## 8. Analytics

| Android | Server | Mapping |
|---------|--------|---------|
| Client-side `FinanceCalculations` over local Flows | `GET /api/v1/analytics/summary`, `.../categories` | Same formulas: income/expense/net by period; expense category totals and percentages. |
| Budget thresholds 70 / 90 / 100 | Can stay client-side or document as shared constants | Prefer shared constants in API docs so web matches Android. |

Web and Android must not diverge on period boundaries (calendar month in local user date, not UTC day-shift of timestamps).

---

## 9. Sync model implications (Stages 4–5)

Android has **no** sync columns today. Required local additions (later — not Stage 0):

| Concern | Planned local concept |
|---------|----------------------|
| Sync state | `SYNCED` / `PENDING_CREATE` / `PENDING_UPDATE` / `PENDING_DELETE` / `CONFLICT` |
| Outbox | Durable queue: operation id, entity type/id, payload, base version, attempts, timestamp |
| Soft delete | `deletedAt` instead of Room `@Delete` for synced entities |
| Versions | `serverVersion` (and/or `baseVersion` for pending updates) |
| Pull cursor | Opaque per-user cursor stored in DataStore or a meta table |
| Auth session | Supabase access/refresh tokens; never service-role key on device |

Push/pull remains the source of truth recovery path; realtime (Stage 6) only wakes clients to pull.

### Conflict policy

- Updates send `expected_version`; stale → `409`, surface keep-mine / use-server (UI may be deferred but conflict must not be discarded).
- No silent last-write-wins on financial records.

---

## 10. First sign-in import flow

Aligned with PRD §7.2:

1. **Backup** local Room DB (or export snapshot) before linking.
2. **Consent UI:** ask whether to upload existing local records.
3. Assign UUIDs locally; build category ID map to server defaults.
4. Upload **bounded batches** with idempotency keys; report imported / skipped / failed counts.
5. Retries must not duplicate (client UUID + idempotency records).
6. **Do not delete** local rows after upload; verify server reads match before marking import complete.

Suggested order: categories mapping → transactions → monthly budgets.

---

## 11. Supporting server tables (no Android equivalent yet)

| Table | Role when Android catches up |
|-------|------------------------------|
| `sync_changes` | Per-user change feed for pull cursor |
| `idempotency_records` | Safe retry of writes |
| `ai_action_audit` | Later Stage 7 — no Android work now |

---

## 12. Type / casing convention (decision for Stage 1+)

Android persists enum names as **uppercase** (`INCOME`, `EXPENSE`).  
PRD examples use **lowercase** (`expense`).  

**Chosen approach for API design:** FastAPI accepts and returns **lowercase** string enums (`income` | `expense`); Android mapping layer converts at the network boundary. Seed docs and OpenAPI use lowercase. Document this in future `API_SPECIFICATION.md`.

---

## 13. Risks and open follow-ups (for Stage 1 review)

1. **ID migration** is the highest-risk Android change; plan dual keys before online mode.
2. **Hard → soft delete** changes DAO semantics and analytics queries (must exclude `deleted_at IS NOT NULL`).
3. **Destructive Room migrations** must be replaced with real migrations before Play production updates that alter schema.
4. **Created_at preservation on import** — confirm whether API allows client-supplied create timestamps for import-only paths.
5. **Instrumented DAO tests** not run in Stage 0; run before Stage 4 schema work.

---

## 14. Stage gate

Stage 0 deliverables:

- [`ANDROID_AUDIT.md`](ANDROID_AUDIT.md) — current app reality + baseline tests  
- This mapping document — Android ↔ PostgreSQL/API bridge  

**Stop here.** Do not scaffold FastAPI, Supabase clients, or Android network/sync code until these documents are reviewed and approved.
