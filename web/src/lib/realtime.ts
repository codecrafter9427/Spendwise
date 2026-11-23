const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8000";

export const REALTIME_EVENT = "spendwise:realtime";

export type RealtimeSyncPayload = {
  type: string;
  entity_type?: string;
  entity_id?: string;
  operation?: string;
  version?: number;
  cursor?: string;
};

/**
 * Authenticated SSE via fetch streaming (EventSource cannot set Authorization).
 * Best-effort: reconnect with backoff; callers should still pull on focus.
 */
export function connectRealtime(
  token: string,
  onSync: (payload: RealtimeSyncPayload) => void,
): () => void {
  const controller = new AbortController();
  let closed = false;
  let attempt = 0;

  const run = async () => {
    while (!closed) {
      try {
        const response = await fetch(`${API_BASE}/api/v1/realtime/events`, {
          headers: {
            Authorization: `Bearer ${token}`,
            Accept: "text/event-stream",
          },
          signal: controller.signal,
          cache: "no-store",
        });
        if (!response.ok || !response.body) {
          throw new Error(`Realtime HTTP ${response.status}`);
        }
        attempt = 0;
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";
        while (!closed) {
          const { value, done } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });
          const parts = buffer.split("\n\n");
          buffer = parts.pop() ?? "";
          for (const part of parts) {
            const lines = part.split("\n");
            const eventLine = lines.find((line) => line.startsWith("event:"));
            const dataLine = lines.find((line) => line.startsWith("data:"));
            if (!eventLine || !dataLine) continue;
            const eventName = eventLine.slice("event:".length).trim();
            if (eventName !== "sync") continue;
            try {
              const payload = JSON.parse(dataLine.slice("data:".length).trim()) as RealtimeSyncPayload;
              onSync(payload);
            } catch {
              // ignore malformed frames
            }
          }
        }
      } catch (error) {
        if (closed || controller.signal.aborted) return;
        attempt += 1;
        const delay = Math.min(30_000, 1000 * 2 ** Math.min(attempt, 4));
        await new Promise((resolve) => setTimeout(resolve, delay));
      }
    }
  };

  void run();

  return () => {
    closed = true;
    controller.abort();
  };
}

export function emitRealtimeRefresh(): void {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new CustomEvent(REALTIME_EVENT));
  }
}
