import { AppNav } from "@/components/AppNav";
import { RealtimeBridge } from "@/components/RealtimeBridge";

export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="shell">
      <RealtimeBridge />
      <AppNav />
      <main className="main">{children}</main>
    </div>
  );
}
