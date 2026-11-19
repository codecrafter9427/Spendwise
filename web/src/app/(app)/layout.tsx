import { AppNav } from "@/components/AppNav";

export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="shell">
      <AppNav />
      <main className="main">{children}</main>
    </div>
  );
}
