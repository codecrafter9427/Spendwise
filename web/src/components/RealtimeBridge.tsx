"use client";

import { useEffect } from "react";

import { connectRealtime, emitRealtimeRefresh } from "@/lib/realtime";
import { createClient } from "@/lib/supabase/client";

export function RealtimeBridge() {
  useEffect(() => {
    let stop: (() => void) | undefined;
    let cancelled = false;

    async function start() {
      const supabase = createClient();
      const { data } = await supabase.auth.getSession();
      const token = data.session?.access_token;
      if (!token || cancelled) return;

      stop = connectRealtime(token, () => {
        emitRealtimeRefresh();
      });
    }

    void start();

    const onFocus = () => emitRealtimeRefresh();
    const onVisibility = () => {
      if (document.visibilityState === "visible") emitRealtimeRefresh();
    };
    window.addEventListener("focus", onFocus);
    document.addEventListener("visibilitychange", onVisibility);

    return () => {
      cancelled = true;
      stop?.();
      window.removeEventListener("focus", onFocus);
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, []);

  return null;
}
