"use client";

import { useEffect } from "react";

import { REALTIME_EVENT } from "@/lib/realtime";

/** Re-run loader when SSE wake or window focus asks for a refresh. */
export function useRealtimeRefresh(onRefresh: () => void): void {
  useEffect(() => {
    const handler = () => onRefresh();
    window.addEventListener(REALTIME_EVENT, handler);
    return () => window.removeEventListener(REALTIME_EVENT, handler);
  }, [onRefresh]);
}
