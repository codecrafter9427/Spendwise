"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";

import { MonthPicker } from "@/components/MonthPicker";
import { StateBlock } from "@/components/StateBlock";
import { api } from "@/lib/api";
import { currentYearMonth, type YearMonth } from "@/lib/dates";
import { formatInr, parseRupeesToMinor } from "@/lib/money";
import { createClient } from "@/lib/supabase/client";
import type { AnalyticsSummary, Transaction } from "@/lib/types";
import { ApiError } from "@/lib/types";
import { monthBounds } from "@/lib/dates";

export default function DashboardPage() {
  const [ym, setYm] = useState<YearMonth>(currentYearMonth);
  const [summary, setSummary] = useState<AnalyticsSummary | null>(null);
  const [recent, setRecent] = useState<Transaction[]>([]);
  const [budgetInput, setBudgetInput] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [savingBudget, setSavingBudget] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const supabase = createClient();
      const { data } = await supabase.auth.getSession();
      const token = data.session?.access_token;
      if (!token) throw new Error("Not signed in");

      const bounds = monthBounds(ym);
      const [summaryData, txPage] = await Promise.all([
        api.analyticsSummary(token, ym.year, ym.month),
        api.listTransactions(token, {
          limit: 8,
          start_date: bounds.start,
          end_date: bounds.end,
        }),
      ]);
      setSummary(summaryData);
      setRecent(txPage.items);
      if (summaryData.budget_amount_minor != null) {
        setBudgetInput((summaryData.budget_amount_minor / 100).toFixed(2));
      } else {
        setBudgetInput("");
      }
    } catch (err) {
      setError(err instanceof ApiError || err instanceof Error ? err.message : "Failed to load");
    } finally {
      setLoading(false);
    }
  }, [ym]);

  useEffect(() => {
    void load();
  }, [load]);

  async function saveBudget(event: FormEvent) {
    event.preventDefault();
    const minor = parseRupeesToMinor(budgetInput);
    if (minor == null) {
      setError("Enter a valid budget amount");
      return;
    }
    setSavingBudget(true);
    setError(null);
    try {
      const supabase = createClient();
      const { data } = await supabase.auth.getSession();
      const token = data.session?.access_token;
      if (!token) throw new Error("Not signed in");
      await api.upsertBudget(token, ym.year, ym.month, { amount_minor: minor });
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save budget");
    } finally {
      setSavingBudget(false);
    }
  }

  const utilization = summary?.budget_utilization_percent ?? 0;

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h1>Dashboard</h1>
          <p>Monthly income, spending, and budget at a glance.</p>
        </div>
        <MonthPicker value={ym} onChange={setYm} />
      </div>

      {loading ? <StateBlock kind="loading" message="Loading dashboard…" /> : null}
      {error ? <StateBlock kind="error" message={error} actionLabel="Retry" onAction={load} /> : null}

      {!loading && summary ? (
        <>
          <div className="grid metrics">
            <div className="panel">
              <div className="metric-label">Income</div>
              <div className="metric-value income">
                {formatInr(summary.total_income_minor)}
              </div>
            </div>
            <div className="panel">
              <div className="metric-label">Expenses</div>
              <div className="metric-value expense">
                {formatInr(summary.total_expense_minor)}
              </div>
            </div>
            <div className="panel">
              <div className="metric-label">Net</div>
              <div className="metric-value">{formatInr(summary.net_minor, true)}</div>
            </div>
            <div className="panel">
              <div className="metric-label">Budget remaining</div>
              <div className="metric-value">
                {summary.budget_remaining_minor == null
                  ? "Not set"
                  : formatInr(summary.budget_remaining_minor)}
              </div>
            </div>
          </div>

          <div className="panel stack">
            <div className="row" style={{ justifyContent: "space-between" }}>
              <strong>Monthly budget</strong>
              <span className="badge">
                {summary.budget_utilization_percent == null
                  ? "Unset"
                  : `${summary.budget_utilization_percent.toFixed(0)}% used`}
              </span>
            </div>
            <div className="bar">
              <span style={{ width: `${Math.min(utilization, 100)}%` }} />
            </div>
            <form className="row" onSubmit={saveBudget}>
              <div className="field">
                <label htmlFor="budget">Budget (₹)</label>
                <input
                  id="budget"
                  value={budgetInput}
                  onChange={(e) => setBudgetInput(e.target.value)}
                  placeholder="25000.00"
                />
              </div>
              <button className="btn" type="submit" disabled={savingBudget}>
                {savingBudget ? "Saving…" : "Save budget"}
              </button>
            </form>
          </div>

          <div className="panel">
            <div className="page-header" style={{ marginBottom: 8 }}>
              <h1 style={{ fontSize: "1.4rem" }}>Recent transactions</h1>
            </div>
            {recent.length === 0 ? (
              <StateBlock kind="empty" message="No transactions this month yet." />
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Type</th>
                    <th>Description</th>
                    <th>Amount</th>
                  </tr>
                </thead>
                <tbody>
                  {recent.map((tx) => (
                    <tr key={tx.id}>
                      <td>{tx.transaction_date}</td>
                      <td>
                        <span className={`badge ${tx.type}`}>{tx.type}</span>
                      </td>
                      <td>{tx.description || "—"}</td>
                      <td>{formatInr(tx.amount_minor)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      ) : null}
    </div>
  );
}
