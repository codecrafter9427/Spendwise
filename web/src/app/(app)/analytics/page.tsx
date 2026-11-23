"use client";

import { useCallback, useEffect, useState } from "react";

import { MonthPicker } from "@/components/MonthPicker";
import { StateBlock } from "@/components/StateBlock";
import { api } from "@/lib/api";
import { currentYearMonth, type YearMonth } from "@/lib/dates";
import { formatInr } from "@/lib/money";
import { createClient } from "@/lib/supabase/client";
import type { AnalyticsSummary, CategorySpendResponse } from "@/lib/types";
import { ApiError } from "@/lib/types";
import { useRealtimeRefresh } from "@/lib/useRealtimeRefresh";

export default function AnalyticsPage() {
  const [ym, setYm] = useState<YearMonth>(currentYearMonth);
  const [summary, setSummary] = useState<AnalyticsSummary | null>(null);
  const [categories, setCategories] = useState<CategorySpendResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const supabase = createClient();
      const { data } = await supabase.auth.getSession();
      const token = data.session?.access_token;
      if (!token) throw new Error("Not signed in");
      const [summaryData, categoryData] = await Promise.all([
        api.analyticsSummary(token, ym.year, ym.month),
        api.analyticsCategories(token, ym.year, ym.month),
      ]);
      setSummary(summaryData);
      setCategories(categoryData);
    } catch (err) {
      setError(err instanceof ApiError || err instanceof Error ? err.message : "Failed to load");
    } finally {
      setLoading(false);
    }
  }, [ym]);

  useEffect(() => {
    void load();
  }, [load]);

  useRealtimeRefresh(() => {
    void load();
  });

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h1>Analytics</h1>
          <p>Monthly totals and category breakdown.</p>
        </div>
        <MonthPicker value={ym} onChange={setYm} />
      </div>

      {loading ? <StateBlock kind="loading" message="Loading analytics…" /> : null}
      {error ? <StateBlock kind="error" message={error} actionLabel="Retry" onAction={load} /> : null}

      {!loading && summary ? (
        <div className="grid metrics">
          <div className="panel">
            <div className="metric-label">Income</div>
            <div className="metric-value income">{formatInr(summary.total_income_minor)}</div>
          </div>
          <div className="panel">
            <div className="metric-label">Expenses</div>
            <div className="metric-value expense">{formatInr(summary.total_expense_minor)}</div>
          </div>
          <div className="panel">
            <div className="metric-label">Net</div>
            <div className="metric-value">{formatInr(summary.net_minor, true)}</div>
          </div>
          <div className="panel">
            <div className="metric-label">Budget used</div>
            <div className="metric-value">
              {summary.budget_utilization_percent == null
                ? "—"
                : `${summary.budget_utilization_percent.toFixed(1)}%`}
            </div>
          </div>
        </div>
      ) : null}

      {!loading && categories ? (
        <div className="panel">
          <div className="page-header" style={{ marginBottom: 8 }}>
            <h1 style={{ fontSize: "1.4rem" }}>Expense by category</h1>
          </div>
          {categories.items.length === 0 ? (
            <StateBlock kind="empty" message="No expenses in this month." />
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Category</th>
                  <th>Amount</th>
                  <th>Share</th>
                  <th>Bar</th>
                </tr>
              </thead>
              <tbody>
                {categories.items.map((item) => (
                  <tr key={item.category_id}>
                    <td>{item.category_name}</td>
                    <td>{formatInr(item.amount_minor)}</td>
                    <td>{item.percent_of_total.toFixed(1)}%</td>
                    <td style={{ minWidth: 120 }}>
                      <div className="bar">
                        <span style={{ width: `${Math.min(item.percent_of_total, 100)}%` }} />
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      ) : null}
    </div>
  );
}
