import { Suspense } from "react";

import { StateBlock } from "@/components/StateBlock";
import { LoginForm } from "@/components/LoginForm";

export default function LoginPage() {
  return (
    <div className="login-shell">
      <div className="login-card">
        <h1>SpendWise</h1>
        <p>Sign in to manage your finances across web and Android.</p>
        <Suspense fallback={<StateBlock kind="loading" message="Loading…" />}>
          <LoginForm />
        </Suspense>
      </div>
    </div>
  );
}
