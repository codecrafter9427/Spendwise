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
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
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
      });
      setProfile(updated);
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
            <button className="btn" type="submit" disabled={saving}>
              {saving ? "Saving…" : "Save profile"}
            </button>
          </form>
        </div>
      ) : null}

      <div className="panel stack">
        <strong>Privacy</strong>
        <p style={{ margin: 0, color: "var(--muted)" }}>
          SpendWise web talks only to the SpendWise API with your Supabase access token. Financial
          data is not queried directly from the browser database. Export and account deletion land
          in a later hardening stage.
        </p>
      </div>
    </div>
  );
}
