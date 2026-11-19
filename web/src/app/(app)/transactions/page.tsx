"use client";

import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";

import { MonthPicker } from "@/components/MonthPicker";
import { StateBlock } from "@/components/StateBlock";
import { api } from "@/lib/api";
import { currentYearMonth, monthBounds, type YearMonth } from "@/lib/dates";
import { formatInr, parseRupeesToMinor } from "@/lib/money";
import { createClient } from "@/lib/supabase/client";
import type { Category, Transaction, TransactionType } from "@/lib/types";
import { ApiError } from "@/lib/types";

export default function TransactionsPage() {
  const [ym, setYm] = useState<YearMonth>(currentYearMonth);
  const [items, setItems] = useState<Transaction[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [typeFilter, setTypeFilter] = useState<"" | TransactionType>("");

  const [amount, setAmount] = useState("");
  const [type, setType] = useState<TransactionType>("expense");
  const [categoryId, setCategoryId] = useState("");
  const [description, setDescription] = useState("");
  const [txDate, setTxDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [saving, setSaving] = useState(false);

  const filteredCategories = useMemo(
    () => categories.filter((c) => c.type === type && !c.archived_at),
    [categories, type],
  );

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const supabase = createClient();
      const { data } = await supabase.auth.getSession();
      const token = data.session?.access_token;
      if (!token) throw new Error("Not signed in");
      const bounds = monthBounds(ym);
      const [txPage, cats] = await Promise.all([
        api.listTransactions(token, {
          limit: 100,
          start_date: bounds.start,
          end_date: bounds.end,
          type: typeFilter || undefined,
          q: query || undefined,
        }),
        api.listCategories(token),
      ]);
      setItems(txPage.items);
      setCategories(cats);
    } catch (err) {
      setError(err instanceof ApiError || err instanceof Error ? err.message : "Failed to load");
    } finally {
      setLoading(false);
    }
  }, [ym, typeFilter, query]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    const first = filteredCategories[0];
    if (first && !filteredCategories.some((c) => c.id === categoryId)) {
      setCategoryId(first.id);
    }
  }, [filteredCategories, categoryId]);

  async function onCreate(event: FormEvent) {
    event.preventDefault();
    const amountMinor = parseRupeesToMinor(amount);
    if (amountMinor == null) {
      setError("Enter a valid amount");
      return;
    }
    if (!categoryId) {
      setError("Select a category");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const supabase = createClient();
      const { data } = await supabase.auth.getSession();
      const token = data.session?.access_token;
      if (!token) throw new Error("Not signed in");
      await api.createTransaction(token, {
        id: crypto.randomUUID(),
        type,
        amount_minor: amountMinor,
        category_id: categoryId,
        description: description.trim() || null,
        transaction_date: txDate,
      });
      setAmount("");
      setDescription("");
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not create transaction");
    } finally {
      setSaving(false);
    }
  }

  async function onDelete(tx: Transaction) {
    if (!window.confirm("Delete this transaction?")) return;
    try {
      const supabase = createClient();
      const { data } = await supabase.auth.getSession();
      const token = data.session?.access_token;
      if (!token) throw new Error("Not signed in");
      await api.deleteTransaction(token, tx.id, tx.version);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not delete transaction");
    }
  }

  const categoryName = useMemo(() => {
    const map = new Map(categories.map((c) => [c.id, c.name]));
    return (id: string) => map.get(id) ?? "Unknown";
  }, [categories]);

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h1>Transactions</h1>
          <p>Create, filter, and review income and expenses.</p>
        </div>
        <MonthPicker value={ym} onChange={setYm} />
      </div>

      <div className="panel">
        <form className="stack" onSubmit={onCreate}>
          <div className="row">
            <div className="field">
              <label htmlFor="amount">Amount (₹)</label>
              <input id="amount" value={amount} onChange={(e) => setAmount(e.target.value)} required />
            </div>
            <div className="field">
              <label htmlFor="type">Type</label>
              <select
                id="type"
                value={type}
                onChange={(e) => setType(e.target.value as TransactionType)}
              >
                <option value="expense">Expense</option>
                <option value="income">Income</option>
              </select>
            </div>
            <div className="field">
              <label htmlFor="category">Category</label>
              <select
                id="category"
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value)}
                required
              >
                {filteredCategories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="date">Date</label>
              <input
                id="date"
                type="date"
                value={txDate}
                onChange={(e) => setTxDate(e.target.value)}
                required
              />
            </div>
          </div>
          <div className="field">
            <label htmlFor="description">Description</label>
            <input
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              maxLength={120}
            />
          </div>
          <button className="btn" type="submit" disabled={saving}>
            {saving ? "Saving…" : "Add transaction"}
          </button>
        </form>
      </div>

      <div className="panel stack">
        <div className="row">
          <div className="field">
            <label htmlFor="search">Search</label>
            <input
              id="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Description"
            />
          </div>
          <div className="field">
            <label htmlFor="filterType">Type filter</label>
            <select
              id="filterType"
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value as "" | TransactionType)}
            >
              <option value="">All</option>
              <option value="expense">Expense</option>
              <option value="income">Income</option>
            </select>
          </div>
        </div>

        {loading ? <StateBlock kind="loading" message="Loading transactions…" /> : null}
        {error ? <StateBlock kind="error" message={error} actionLabel="Retry" onAction={load} /> : null}
        {!loading && items.length === 0 ? (
          <StateBlock kind="empty" message="No transactions match these filters." />
        ) : null}
        {!loading && items.length > 0 ? (
          <table className="table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Type</th>
                <th>Category</th>
                <th>Description</th>
                <th>Amount</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {items.map((tx) => (
                <tr key={tx.id}>
                  <td>{tx.transaction_date}</td>
                  <td>
                    <span className={`badge ${tx.type}`}>{tx.type}</span>
                  </td>
                  <td>{categoryName(tx.category_id)}</td>
                  <td>{tx.description || "—"}</td>
                  <td>{formatInr(tx.amount_minor)}</td>
                  <td>
                    <button type="button" className="btn danger" onClick={() => void onDelete(tx)}>
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
      </div>
    </div>
  );
}
