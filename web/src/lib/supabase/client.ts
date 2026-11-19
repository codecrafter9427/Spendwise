import { createBrowserClient } from "@supabase/ssr";

import { getSupabaseEnv, supabaseConfigErrorMessage } from "@/lib/supabase/config";

export function createClient() {
  const env = getSupabaseEnv();
  if (!env) {
    throw new Error(supabaseConfigErrorMessage());
  }
  return createBrowserClient(env.url, env.anonKey);
}
