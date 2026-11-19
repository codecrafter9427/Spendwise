const formatter = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export function formatInr(amountMinor: number, withSign = false): string {
  const rupees = Math.abs(amountMinor) / 100;
  const formatted = formatter.format(rupees);
  if (withSign && amountMinor > 0) return `+${formatted}`;
  if (amountMinor < 0) return `-${formatted}`;
  return formatted;
}

export function parseRupeesToMinor(input: string): number | null {
  const trimmed = input.trim();
  if (!/^\d+(\.\d{0,2})?$/.test(trimmed)) return null;
  const [rupees, paise = ""] = trimmed.split(".");
  const minor = Number(rupees) * 100 + Number((paise + "00").slice(0, 2));
  if (!Number.isFinite(minor) || minor <= 0) return null;
  return minor;
}
