"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";

import { MonthPicker } from "@/components/MonthPicker";
import { StateBlock } from "@/components/StateBlock";
import { api } from "@/lib/api";
import { currentYearMonth, type YearMonth } from "@/lib/dates";
import { createClient } from "@/lib/supabase/client";
import type { AiChatResponse } from "@/lib/types";
import { ApiError } from "@/lib/types";

type ChatTurn = {
  role: "user" | "assistant";
  text: string;
  meta?: AiChatResponse;
};

export default function AssistantPage() {
  const [ym, setYm] = useState<YearMonth>(currentYearMonth);
  const [aiEnabled, setAiEnabled] = useState(false);
  const [input, setInput] = useState("");
  const [turns, setTurns] = useState<ChatTurn[]>([]);
  const [loading, setLoading] = useState(true);
  const [asking, setAsking] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadProfile = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const supabase = createClient();
      const { data } = await supabase.auth.getSession();
      const token = data.session?.access_token;
      if (!token) throw new Error("Not signed in");
      const me = await api.getMe(token);
      setAiEnabled(Boolean(me.ai_enabled));
    } catch (err) {
      setError(err instanceof ApiError || err instanceof Error ? err.message : "Failed to load");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadProfile();
  }, [loadProfile]);

  async function onAsk(event: FormEvent) {
    event.preventDefault();
    const message = input.trim();
    if (!message) return;
    setAsking(true);
    setError(null);
    setTurns((prev) => [...prev, { role: "user", text: message }]);
    setInput("");
    try {
      const supabase = createClient();
      const { data } = await supabase.auth.getSession();
      const token = data.session?.access_token;
      if (!token) throw new Error("Not signed in");
      const response = await api.aiChat(token, {
        message,
        year: ym.year,
        month: ym.month,
      });
      setTurns((prev) => [
        ...prev,
        { role: "assistant", text: response.answer, meta: response },
      ]);
    } catch (err) {
      setError(err instanceof ApiError || err instanceof Error ? err.message : "Ask failed");
    } finally {
      setAsking(false);
    }
  }

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h1>Assistant</h1>
          <p>Read-only Q&amp;A grounded in your SpendWise records.</p>
        </div>
        <MonthPicker value={ym} onChange={setYm} />
      </div>

      {loading ? <StateBlock kind="loading" message="Loading assistant…" /> : null}
      {error ? <StateBlock kind="error" message={error} /> : null}

      {!loading && !aiEnabled ? (
        <StateBlock
          kind="empty"
          message="Enable the AI assistant under Settings to ask spending questions. It cannot change your data."
        />
      ) : null}

      {!loading && aiEnabled ? (
        <>
          <div className="panel stack">
            <p className="muted">
              Ask about monthly totals, budgets, categories, largest expenses, or compare periods.
              Optional LLM explanation uses a server key; otherwise answers are built from verified tools only.
            </p>
            <form className="row" onSubmit={onAsk}>
              <input
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder="e.g. What is my budget status?"
                disabled={asking}
                style={{ flex: 1 }}
              />
              <button type="submit" className="btn" disabled={asking || !input.trim()}>
                {asking ? "Thinking…" : "Ask"}
              </button>
            </form>
          </div>

          <div className="stack">
            {turns.length === 0 ? (
              <StateBlock kind="empty" message="No questions yet." />
            ) : (
              turns.map((turn, index) => (
                <div key={`${turn.role}-${index}`} className="panel">
                  <strong>{turn.role === "user" ? "You" : "SpendWise"}</strong>
                  <p style={{ whiteSpace: "pre-wrap" }}>{turn.text}</p>
                  {turn.meta ? (
                    <p className="muted">
                      Mode: {turn.meta.mode} · Tools:{" "}
                      {turn.meta.tools_used.map((t) => t.name).join(", ")} ·{" "}
                      {turn.meta.period.year}-{String(turn.meta.period.month).padStart(2, "0")}
                    </p>
                  ) : null}
                </div>
              ))
            )}
          </div>
        </>
      ) : null}
    </div>
  );
}
