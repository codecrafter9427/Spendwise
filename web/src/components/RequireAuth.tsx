import { redirect } from "next/navigation";

import { createClient } from "@/lib/supabase/server";

export async function requireAccessToken(): Promise<string> {
  const supabase = await createClient();
  const { data } = await supabase.auth.getSession();
  const token = data.session?.access_token;
  if (!token) {
    redirect("/login");
  }
  return token;
}
