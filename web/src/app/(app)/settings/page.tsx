"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import { StateBlock } from "@/components/StateBlock";
import { api } from "@/lib/api";
import { createClient } from "@/lib/supabase/client";
import type { UserProfile } from "@/lib/types";
import { ApiError } from "@/lib/types";

export default function SettingsPage() {
  const router = useRouter();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [displayName, setDisplayName] = useState("");
  const [timezone, setTimezone] = useState("UTC");
  const [aiEnabled, setAiEnabled] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [busyAccount, setBusyAccount] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const supabase = createClient();
        const { data } = await supabase.auth.getSession();
        const token = data.session?.access_token;
        if (!token) throw new Error("Not signed in");
        const me = await api.getMe(token);
        setProfile(me);
        setDisplayName(me.display_name ?? "");
        setTimezone(me.timezone || "UTC");
        setAiEnabled(Boolean(me.ai_enabled));
      } catch (err) {
        setError(err instanceof ApiError || err instanceof Error ? err.message : "Failed to load");
      } finally {
        setLoading(false);
      }
    }
    void load();
  }, []);

  async function onSave(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const supabase = createClient();
      const { data } = await supabase.auth.getSession();
      const token = data.session?.access_token;
      if (!token) throw new Error("Not signed in");
      const updated = await api.patchMe(token, {
        display_name: displayName.trim() || null,
        timezone,
        base_currency: "INR",
        ai_enabled: aiEnabled,
      });
      setProfile(updated);
      setAiEnabled(Boolean(updated.ai_enabled));
      setMessage("Profile saved.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save profile");
    } finally {
      setSaving(false);
    }
  }

  async function onSignOut() {
    const supabase = createClient();
    await supabase.auth.signOut();
    router.replace("/login");
    router.refresh();
  }

  async function onExport() {
    setBusyAccount(true);
    setError(null);
    setMessage(null);
    try {
      const supabase = createClient();
      const { data } = await supabase.auth.getSession();
      const token = data.session?.access_token;
      if (!token) throw new Error("Not signed in");
      const payload = await api.exportAccount(token);
      const blob = new Blob([JSON.stringify(payload, null, 2)], {
        type: "application/json",
      });
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `spendwise-export-${new Date().toISOString().slice(0, 10)}.json`;
      anchor.click();
      URL.revokeObjectURL(url);
      setMessage("Export downloaded.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Export failed");
    } finally {
      setBusyAccount(false);
    }
  }

  async function onDeleteAccount() {
    const confirmed = window.confirm(
      "Delete all SpendWise cloud data for this account? This cannot be undone.",
    );
    if (!confirmed) return;
    setBusyAccount(true);
    setError(null);
    setMessage(null);
    try {
      const supabase = createClient();
      const { data } = await supabase.auth.getSession();
      const token = data.session?.access_token;
      if (!token) throw new Error("Not signed in");
      await api.deleteAccount(token);
      await supabase.auth.signOut();
      router.replace("/login");
      router.refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Delete failed");
      setBusyAccount(false);
    }
  }

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h1>Settings</h1>
          <p>Profile, currency, and account controls.</p>
        </div>
        <button type="button" className="btn secondary" onClick={() => void onSignOut()}>
          Sign out
        </button>
      </div>

      {loading ? <StateBlock kind="loading" message="Loading settings…" /> : null}
      {error ? <StateBlock kind="error" message={error} /> : null}
      {message ? <StateBlock kind="empty" message={message} /> : null}

      {!loading && profile ? (
        <div className="panel">
          <form className="stack" onSubmit={onSave}>
            <div className="field">
              <label>User ID</label>
              <input value={profile.id} readOnly />
            </div>
            <div className="field">
              <label htmlFor="displayName">Display name</label>
              <input
                id="displayName"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                maxLength={120}
              />
            </div>
            <div className="field">
              <label htmlFor="timezone">Timezone</label>
              <input
                id="timezone"
                value={timezone}
                onChange={(e) => setTimezone(e.target.value)}
                placeholder="Asia/Kolkata"
              />
            </div>
            <div className="field">
              <label>Base currency</label>
              <input value="INR" readOnly />
            </div>
            <label className="row" style={{ gap: "0.5rem", alignItems: "center" }}>
              <input
                type="checkbox"
                checked={aiEnabled}
                onChange={(e) => setAiEnabled(e.target.checked)}
              />
              Enable AI assistant (read-only; uses your finance records)
            </label>
            <button className="btn" type="submit" disabled={saving}>
              {saving ? "Saving…" : "Save profile"}
            </button>
          </form>
        </div>
      ) : null}

      <div className="panel stack">
        <strong>Data controls</strong>
        <p style={{ margin: 0, color: "var(--muted)" }}>
          Download a JSON export of your cloud profile, transactions, budgets, and categories, or
          permanently delete server-side finance data for this account.
        </p>
        <div className="row" style={{ gap: "0.75rem", flexWrap: "wrap" }}>
          <button
            type="button"
            className="btn secondary"
            disabled={busyAccount}
            onClick={() => void onExport()}
          >
            {busyAccount ? "Working…" : "Export my data"}
          </button>
          <button
            type="button"
            className="btn secondary"
            disabled={busyAccount}
            onClick={() => void onDeleteAccount()}
          >
            Delete cloud account data
          </button>
        </div>
      </div>

      <div className="panel stack">
        <strong>Privacy</strong>
        <p style={{ margin: 0, color: "var(--muted)" }}>
          SpendWise web talks only to the SpendWise API with your Supabase access token. Financial
          data is not queried directly from the browser database. The AI assistant cannot create,
          edit, or delete records and will not give investment or tax advice.
        </p>
      </div>
    </div>
  );
}
