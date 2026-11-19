import type { Metadata } from "next";

import "./globals.css";

export const metadata: Metadata = {
  title: "SpendWise",
  description: "Personal finance tracker for Android and web",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
