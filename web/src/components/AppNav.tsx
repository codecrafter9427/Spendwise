"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const links = [
  { href: "/dashboard", label: "Dashboard" },
  { href: "/transactions", label: "Transactions" },
  { href: "/analytics", label: "Analytics" },
  { href: "/settings", label: "Settings" },
];

export function AppNav() {
  const pathname = usePathname();

  return (
    <aside className="sidebar">
      <div className="brand">SpendWise</div>
      <nav className="nav">
        {links.map((link) => (
          <Link
            key={link.href}
            href={link.href}
            className={pathname.startsWith(link.href) ? "active" : undefined}
          >
            {link.label}
          </Link>
        ))}
      </nav>
    </aside>
  );
}
