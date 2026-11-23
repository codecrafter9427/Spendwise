# SpendWise User Manual

**Version:** Phase 2 (Stages 0–8)  
**Currency:** Indian Rupee (INR), stored as paise (₹1 = 100 paise)  
**Platforms:** Android app · Web app · Shared cloud backend

SpendWise helps you track income and expenses, set a monthly budget, review analytics, and keep phone and web in sync when you sign in.

---

## 1. What you can do

| Feature | Android | Web |
|--------|---------|-----|
| Add / edit / delete transactions | Yes | Yes |
| Monthly budget | Yes | Yes |
| Dashboard / month overview | Yes | Yes |
| Analytics | Yes | Yes |
| Works offline | Yes (local database) | No (needs network) |
| Sign in & sync with cloud | Yes | Yes |
| AI spending assistant | No (web only) | Yes |
| Export / delete cloud data | No (web only) | Yes |
| Live refresh when data changes elsewhere | Sync when online / foreground | Auto-refresh when signed in |

Use the **same email account** on phone and web so both see the same cloud data.

---

## 2. Accounts and sign-in

SpendWise uses **Supabase Auth** (email + password).

1. Create an account once (from **Web → Login** or **Android → Settings → Account**).
2. Sign in with the same email on every device you want linked.
3. Sign out anytime; **local Android data stays on the phone** after sign-out.

**Tips**

- Prefer one account for personal use so sync stays simple.
- If sign-in fails, check email/password and that the app/web are pointed at the same Supabase project.

---

## 3. Android app

### 3.1 Main areas

Typical bottom/side navigation includes:

- **Home / Dashboard** — month income, expenses, balance, budget remaining, recent transactions  
- **Transactions** — list, add, edit, delete  
- **Budgets** — set the monthly spending budget  
- **Analytics** — trends and category breakdown  
- **Settings** — theme, account, sync  

### 3.2 Recording a transaction

1. Open **Transactions** (or add from the dashboard shortcut if available).
2. Choose **Income** or **Expense**.
3. Enter amount in rupees (the app stores it as integer paise).
4. Pick a **category**, optional **description**, and **date**.
5. Save.

Edits and deletes work the same way from the transaction list or detail screen.

### 3.3 Monthly budget

1. Open the budget screen (or set budget from the dashboard).
2. Choose the month if needed.
3. Enter the budget amount in rupees and save.

Dashboard and analytics use this to show remaining budget and utilization.

### 3.4 Offline use

- You can use Android **without** signing in; everything is stored on the device.
- When signed in, changes are saved locally first, then synced to the cloud when the network is available.

### 3.5 Link the phone to the cloud

1. Open **Settings → Account**.
2. Sign in (or sign up).
3. Tap **Upload local data to cloud** once if you already have offline transactions/budgets you want online.
4. Use **Sync now** to push pending changes and pull updates from the server.
5. Watch **Pending sync** / **Conflicts** counts in Settings.

**While signed in and online**, the app also syncs in the background (WorkManager) and when you bring the app to the foreground.

### 3.6 Conflicts

If the same record was changed on the server (for example on the web) after your phone based an edit on an older version, sync may show a **conflict**. The app keeps the conflicted outbox item instead of silently overwriting. Resolve by reviewing the record and syncing again after aligning the data (advanced conflict UI may be limited).

### 3.7 Connecting a physical phone to a local backend (developers)

If the API runs on your PC:

| Setup | `spendwise.api.baseUrl` | Extra step |
|-------|-------------------------|------------|
| Android emulator | `http://10.0.2.2:8000` | None |
| Physical phone (USB) | `http://127.0.0.1:8000` | `adb reverse tcp:8000 tcp:8000` |
| Physical phone (Wi‑Fi) | `http://YOUR_PC_LAN_IP:8000` | Same Wi‑Fi; API must listen on `0.0.0.0` |

Also set Supabase URL and anon/publishable key in `local.properties`. Rebuild the app after changing those values.

---

## 4. Web app

Open the web app in a browser (local default: http://localhost:3000).

### 4.1 Navigation

- **Dashboard** — month summary, budget, recent transactions  
- **Transactions** — filter, create, delete  
- **Analytics** — totals and category spend  
- **Assistant** — AI Q&A (after you enable it)  
- **Settings** — profile, AI consent, export/delete, sign out  

### 4.2 Dashboard

1. Pick the month with the month picker.
2. Review income, expenses, net, and budget remaining.
3. Set or update the monthly budget from the dashboard form when available.
4. Recent transactions for that month appear below.

When another device updates cloud data, the page can **refresh automatically** (realtime wake + refetch). Focusing the browser tab also refreshes.

### 4.3 Transactions

1. Choose month / filters as needed.
2. Create a transaction: type, amount, category, date, optional description.
3. Delete from the list when you no longer need a row.

Amounts are entered in rupees and stored as paise on the server.

### 4.4 Analytics

1. Select the month.
2. View totals and category breakdown for expenses.

### 4.5 AI Assistant (web only)

1. Go to **Settings**.
2. Enable **AI assistant** and save.
3. Open **Assistant**.
4. Select the month context and ask questions such as:
   - “What is my budget status?”
   - “How much did I spend by category?”
   - “What were my largest expenses?”
   - “Compare this month to last month.”

**Rules**

- Answers are based on **your SpendWise records** for the selected period.  
- The assistant is **read-only** (cannot add/edit/delete money data).  
- It will **not** give investment, tax, or credit advice.  
- Without an optional server LLM key, answers are still built from verified finance tools (deterministic mode).

### 4.6 Export and delete cloud data (web only)

In **Settings → Data controls**:

- **Export my data** — downloads a JSON file with profile, categories, transactions, and budgets.  
- **Delete cloud account data** — permanently removes your server-side finance data (and related sync rows). Confirm carefully; this cannot be undone. You will be signed out afterward.

Default shared categories remain on the server for other users; only your personal data is removed.

---

## 5. How sync works (phone ↔ web)

```
Android (offline cache) ── push/pull ──► Cloud API + database ◄── Web
```

1. **Android** keeps a local database so the UI stays fast offline.  
2. Signed-in changes go into an **outbox**, then **push** to the cloud.  
3. **Pull** brings down changes made on web or other devices.  
4. **Web** always reads/writes the cloud API.  
5. **Realtime** only *wakes* clients to refresh sooner; the API remains the source of truth.

If the phone never signs in or never syncs, web will not show phone-only offline data.

---

## 6. Money and categories

- Primary currency is **INR**.  
- Internally amounts are **integer paise** (no floating-point money).  
- Default categories (Food, Salary, etc.) are shared across Android and backend with stable IDs.  
- Soft-deleted transactions disappear from lists but remain in sync history as needed.

---

## 7. Privacy summary

- Auth tokens come from Supabase; the web/Android clients call the SpendWise API with your access token.  
- The browser does **not** talk to Postgres directly.  
- AI (when enabled) uses allowlisted read tools scoped to your user.  
- You can export or delete cloud data from web Settings.  
- Do not share your password or publish anon keys in public places beyond normal client config.

---

## 8. Troubleshooting

| Problem | What to try |
|---------|-------------|
| Web won’t load data | Confirm you’re signed in; API is running; CORS allows your web origin |
| Android “failed to connect” | Check API URL; for USB phone run `adb reverse tcp:8000 tcp:8000`; API with `--host 0.0.0.0` |
| Sync says HTTP 500 | Run DB migrations (`alembic upgrade head`) |
| Web missing phone transactions | Sign in on phone → Sync now / Upload local data; same account as web |
| Phone missing web transactions | Sign in → Sync now (pull) |
| AI says consent required | Web Settings → enable AI → Save |
| AI refuses a question | Ask about your spending/budget/categories only—not investment advice |
| Pending sync stuck | Check network/sign-in; open Settings → Sync now; resolve conflicts if shown |

---

## 9. Starting the full stack locally (operators)

These steps are for running SpendWise on a development machine.

### 9.1 Backend

```powershell
docker compose -f infra/docker-compose.yml up -d postgres

cd backend
.\.venv\Scripts\activate
# Configure .env from .env.example (Supabase JWT settings, CORS)
alembic upgrade head
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

- Health: http://127.0.0.1:8000/health/live  
- API docs: http://127.0.0.1:8000/docs  

### 9.2 Web

```powershell
cd web
# .env.local: NEXT_PUBLIC_SUPABASE_URL, NEXT_PUBLIC_SUPABASE_ANON_KEY, NEXT_PUBLIC_API_BASE_URL=http://localhost:8000
npm install
npm run dev
```

Open http://localhost:3000

### 9.3 Android

1. Fill `local.properties` (see `local.properties.example`).  
2. Sync Gradle and Run from Android Studio.  
3. For a USB device: `adb reverse tcp:8000 tcp:8000`.

### 9.4 Optional AI LLM

In `backend/.env`, set `AI_PROVIDER_API_KEY` (and model/base URL if needed). Without it, the assistant still answers using verified tools only.

---

## 10. Feature map by release stage

| Stage | What it added for users |
|-------|-------------------------|
| Android MVP | Offline expense tracking on phone |
| Backend + Web | Cloud API, sign-in, dashboard/transactions/budgets/analytics on web |
| Android cloud | Sign-in, upload, online sync of edits |
| Offline sync | Outbox, soft deletes, Sync now, pull from server |
| Realtime | Faster auto-refresh on web; phone sync wake in foreground |
| AI | Web Assistant for spending Q&A |
| Hardening | Safer API limits, export & delete account data, ops docs |

---

## 11. Support checklist before asking for help

1. Are phone and web using the **same email**?  
2. Did you tap **Sync now** (or wait for automatic sync) after changes?  
3. Is the API healthy (`/health/live`)?  
4. For a physical phone, is `adb reverse` still active?  
5. Exact error text from Settings / the web page  

---

*This manual describes the SpendWise Phase 2 product as implemented through Stage 8. Operator details also live in `docs/RUNBOOK.md` and `docs/SECURITY_AND_PRIVACY.md`.*
