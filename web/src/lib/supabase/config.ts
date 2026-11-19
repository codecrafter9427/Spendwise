export function getSupabaseEnv(): { url: string; anonKey: string } | null {
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL?.trim() ?? "";
  const anonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY?.trim() ?? "";

  if (!url || !anonKey) {
    return null;
  }

  // Reject copied placeholders from .env.example
  if (
    url.includes("<") ||
    url.includes(">") ||
    anonKey.includes("<") ||
    anonKey.includes(">") ||
    url.includes("project-ref") ||
    anonKey.includes("supabase-anon-key")
  ) {
    return null;
  }

  try {
    const parsed = new URL(url);
    if (parsed.protocol !== "https:" && parsed.protocol !== "http:") {
      return null;
    }
  } catch {
    return null;
  }

  return { url, anonKey };
}

export function supabaseConfigErrorMessage(): string {
  return "Supabase is not configured. Edit web/.env.local with your real project URL and anon key from Supabase → Project Settings → API, then restart npm run dev.";
}
