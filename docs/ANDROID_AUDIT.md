# SpendWise Android Audit (Phase 2 — Stage 0)

**Audit date:** 2026-11-18  
**Repository root:** `C:\Users\DinsLav\AndroidStudioProjects\SpendWise`  
**Package / applicationId:** `com.dins.spendwise`  
**App version:** `1.0.0` (versionCode 1)  
**Purpose:** Document the real Android MVP before backend/API mapping. No assumptions beyond verified code.

---

## 1. Project shape

| Item | Value |
|------|--------|
| Modules | Single Gradle module: `app` (Android application) |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Repository; lightweight domain models/use cases |
| DI | Dagger Hilt (`@HiltAndroidApp`, `@HiltViewModel`, `AppModule`) |
| Local DB | Room 2.8.4 — database name `spendwise.db`, schema version **1** |
| Preferences | DataStore Preferences (`spendwise_settings`) — theme only |
| Navigation | Navigation Compose (`SpendWiseNavHost`, `Routes`) |
| Network | **None** — no Retrofit/OkHttp; manifest has **no** `INTERNET` permission |
| Auth | **None** |
| Background work | **None** — no WorkManager |

### Toolchain (verified)

| Tool | Version source |
|------|----------------|
| AGP | 9.4.1 (`gradle/libs.versions.toml`) |
| Kotlin | 2.2.10 |
| Compose BOM | 2026.02.01 |
| Hilt | 2.59 |
| Room | 2.8.4 |
| Navigation Compose | 2.9.0 |
| DataStore | 1.1.7 |
| minSdk / targetSdk / compileSdk | 26 / 37 / 37 |
| Java compatibility | 11 |

### Layout (package tree)

```
com.dins.spendwise/
  SpendWiseApplication.kt
  MainActivity.kt
  core/navigation/          Routes, SpendWiseNavHost
  core/designsystem/        Shared Compose components, CategoryIcons
  core/utils/               Money, DateUtils
  data/local/entity|dao|database/
  data/mapper/              Entity ↔ domain
  data/repository/          *RepositoryImpl
  domain/model/
  domain/repository/        Interfaces
  domain/usecase/           Validator, filters, finance calculations
  di/                       DatabaseModule, RepositoryModule
  feature/dashboard|transaction|budget|analytics|settings/
  ui/theme/
```

There is **no** `data/remote` package and **no** monorepo `android/` / `backend/` / `web/` split yet. Android lives at the repo root.

---

## 2. Navigation and screens

Routes: [`app/src/main/java/com/dins/spendwise/core/navigation/Routes.kt`](../app/src/main/java/com/dins/spendwise/core/navigation/Routes.kt)

| Route | Screen | Role |
|-------|--------|------|
| `home` | Dashboard | Month nav, income/expense/net, budget progress, recent txs |
| `transactions` | Transaction list | Search + month/type/category filters |
| `analytics` | Analytics | Category expense donut + list |
| `settings` | Settings | Theme, INR note, About, Privacy, version |
| `add_transaction` | Form | Create |
| `edit_transaction/{id}` | Form | Edit |
| `transaction_details/{id}` | Details | View / edit / delete |

Bottom bar: Home, Transactions, Analytics, Settings. FAB add on Home and Transactions. Budget edit is a dialog hosted from the nav shell (`BudgetDialog` / `BudgetViewModel`).

---

## 3. Dependency injection

[`app/src/main/java/com/dins/spendwise/di/AppModule.kt`](../app/src/main/java/com/dins/spendwise/di/AppModule.kt)

- **DatabaseModule:** provides `CoroutineScope`, Room `SpendWiseDatabase` (destructive migration fallback), DAOs, DataStore.
- **RepositoryModule:** `@Binds` for `TransactionRepository`, `CategoryRepository`, `BudgetRepository`, `SettingsRepository`.

Room builder uses:

```kotlin
.fallbackToDestructiveMigration(dropAllTables = true)
```

`exportSchema = false` on `@Database` — no checked-in schema JSON.

---

## 4. Local database schema

Database: [`SpendWiseDatabase.kt`](../app/src/main/java/com/dins/spendwise/data/local/database/SpendWiseDatabase.kt)  
Entities: `CategoryEntity`, `TransactionEntity`, `MonthlyBudgetEntity`.

### 4.1 `categories`

| Column | Kotlin type | Notes |
|--------|-------------|--------|
| `id` | `Long` PK (not autoGenerate) | Fixed seed IDs |
| `name` | `String` | Display name |
| `type` | `String` | `INCOME` or `EXPENSE` (`TransactionType.name`) |
| `icon` | `String` | Material icon key string |
| `isDefault` | `Boolean` | Always `true` for seeds |

Index: `type`.  
DAO: observe all / by type, getById, insertAll (IGNORE), count. **No update/delete/create custom category APIs.**

### 4.2 `transactions`

| Column | Kotlin type | Notes |
|--------|-------------|--------|
| `id` | `Long` PK autoGenerate | Local integer identity |
| `type` | `String` | `INCOME` / `EXPENSE` |
| `amountMinor` | `Long` | Paise; must be > 0 |
| `categoryId` | `Long` | FK → `categories.id` RESTRICT |
| `description` | `String?` | Optional; max 120 trimmed chars at validation |
| `transactionDate` | `Long` | **Epoch day** (`LocalDate.toEpochDay()`) |
| `createdAt` | `Long` | Epoch **millis** |
| `updatedAt` | `Long` | Epoch **millis** |

Indexes: `transactionDate`, `categoryId`, `type`, `(transactionDate, type)`.  
Deletes: Room `@Delete` — **hard delete**, no tombstone.

### 4.3 `monthly_budgets`

| Column | Kotlin type | Notes |
|--------|-------------|--------|
| `id` | `Long` PK autoGenerate | |
| `year` | `Int` | Calendar year |
| `month` | `Int` | 1–12 (`YearMonth.monthValue`) |
| `amountMinor` | `Long` | Budget in paise |
| `createdAt` / `updatedAt` | `Long` | Epoch millis |

Unique index: `(year, month)` — single-user local assumption (one budget per month globally on device).

### 4.4 Seeded categories

Source: [`CategorySeeder.kt`](../app/src/main/java/com/dins/spendwise/data/local/database/CategorySeeder.kt)

**Expense (ids 1–12):** Food & Dining, Transportation, Shopping, Bills & Utilities, Health, Entertainment, Education, Travel, Groceries, Housing, Personal Care, Other.

**Income (ids 101–106):** Salary, Freelance, Business, Investment Returns, Gift, Other Income.

Seeded on DB create and re-seeded if `categories` count is 0 on open.

---

## 5. Money, dates, and types

### Money

[`Money.kt`](../app/src/main/java/com/dins/spendwise/core/utils/Money.kt)

- Storage and arithmetic: **integer paise** (`Long`), never float for storage.
- Display: INR via `NumberFormat` + `Currency.getInstance("INR")`.
- Max: `MAX_AMOUNT_MINOR = 99_999_999_999L` (₹99,99,99,999.99).
- Parse: up to 2 decimal places; amount must be **> 0**.

No per-row currency column; currency is display-level INR only.

### Dates

- User-facing transaction date: `LocalDate` in domain; persisted as epoch day.
- Month filters: `YearMonth` → inclusive epoch-day range via [`DateUtils`](../app/src/main/java/com/dins/spendwise/core/utils/DateUtils.kt).
- Audit timestamps: `System.currentTimeMillis()` (UTC epoch millis; no explicit zone stored).

### Enums

- `TransactionType`: `INCOME`, `EXPENSE` — stored as `.name` strings.
- `BudgetStatus`: `UNSET`, `WITHIN`, `APPROACHING`, `NEAR_LIMIT`, `EXCEEDED`.
- `ThemePreference`: `SYSTEM`, `LIGHT`, `DARK` (DataStore only).

---

## 6. Business rules (domain)

### Validation — `TransactionValidator`

[`TransactionValidator.kt`](../app/src/main/java/com/dins/spendwise/domain/usecase/TransactionValidator.kt)

- Amount required, valid money parse, > 0, ≤ max.
- Category required; category type must match transaction type.
- Description optional; trimmed; max length **120**; empty → stored `null`.
- Date required (`dateEpochDay != null`).

### Calculations — `FinanceCalculations`

[`FinanceCalculations.kt`](../app/src/main/java/com/dins/spendwise/domain/usecase/FinanceCalculations.kt)

- Monthly summary: sum income / expense by `YearMonth`; net = income − expense.
- Budget progress: utilization % from expense ÷ budget; statuses:
  - `< 70%` WITHIN
  - `< 90%` APPROACHING
  - `≤ 100%` NEAR_LIMIT
  - `> 100%` EXCEEDED
  - unset / budget ≤ 0 → UNSET
- Category expense shares: expense-only for month; percent of total expense; sorted descending by amount.

### Filters — `TransactionFilters`

[`TransactionFilters.kt`](../app/src/main/java/com/dins/spendwise/domain/usecase/TransactionFilters.kt)

- Optional filters: month, type, categoryId, free-text query (category name or description, case-insensitive).
- Sort: `transactionDate` DESC, then `createdAt` DESC.

### Repository behaviors

- Transactions: Flow observe + insert/update/hard delete ([`TransactionRepositoryImpl`](../app/src/main/java/com/dins/spendwise/data/repository/TransactionRepositoryImpl.kt)).
- Categories: read-only observe/get ([`CategoryRepository`](../app/src/main/java/com/dins/spendwise/domain/repository/CategoryRepository.kt)).
- Budgets: upsert by year/month ([`BudgetRepositoryImpl`](../app/src/main/java/com/dins/spendwise/data/repository/BudgetRepositoryImpl.kt)).
- Settings: theme preference only.

---

## 7. Manifest / privacy posture (MVP)

[`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml): no `INTERNET`, no biometric, no SMS. `allowBackup=true` with backup XML rules. Settings privacy copy states local-only storage and no accounts (MVP).

---

## 8. Gaps vs Phase 2 product (PRD)

| Capability | Android today |
|------------|---------------|
| User identity / Supabase Auth | Absent |
| Server sync / outbox / versions | Absent |
| Soft deletes (`deleted_at`) | Hard delete only |
| UUID entity IDs | Local `Long` only |
| Currency column / multi-currency | INR display only |
| Custom / user-owned categories | Seeded defaults only |
| Conflict detection | N/A (single local writer) |
| Real Room migrations | Destructive fallback |
| Export / account deletion | Not implemented |
| Network client | Not present |

These are expected Stage 4–5 Android changes; **not** altered in Stage 0.

---

## 9. Baseline tests (Stage 0)

### Unit tests — executed

Command:

```bat
gradlew.bat :app:testDebugUnitTest --console=plain
```

**Result (2026-11-18):** `BUILD SUCCESSFUL` — exit code **0**.

| Suite | Tests | Failures | Errors |
|-------|------:|---------:|-------:|
| `MoneyTest` | 4 | 0 | 0 |
| `FinanceCalculationsTest` | 3 | 0 | 0 |
| `TransactionFiltersTest` | 2 | 0 | 0 |
| `TransactionValidatorTest` | 3 | 0 | 0 |
| `ExampleUnitTest` | 1 | 0 | 0 |
| **Total** | **13** | **0** | **0** |

### Instrumented tests — not run in this audit

Require a device/emulator:

```bat
gradlew.bat :app:connectedDebugAndroidTest
```

Present: `SpendWiseDatabaseTest`, `ExampleInstrumentedTest`. Recorded as **not executed** in Stage 0 (no connected device in this run).

---

## 10. Stage 0 conclusion

The Android MVP is a coherent offline Compose app with integer-paise money, epoch-day dates, seeded categories, and hard-delete CRUD. It is **compatible in monetary semantics** with the Phase 2 PRD (minor units / INR) but **not yet sync-ready** (Long IDs, no version/tombstones/outbox/auth/network).

Next document: [`ANDROID_BACKEND_MAPPING.md`](ANDROID_BACKEND_MAPPING.md).  
**Do not implement backend until both audit docs are reviewed.**
