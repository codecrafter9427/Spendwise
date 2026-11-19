export type YearMonth = { year: number; month: number };

export function currentYearMonth(): YearMonth {
  const now = new Date();
  return { year: now.getFullYear(), month: now.getMonth() + 1 };
}

export function shiftYearMonth(ym: YearMonth, delta: number): YearMonth {
  const date = new Date(ym.year, ym.month - 1 + delta, 1);
  return { year: date.getFullYear(), month: date.getMonth() + 1 };
}

export function formatMonthYear(ym: YearMonth): string {
  return new Date(ym.year, ym.month - 1, 1).toLocaleDateString("en-IN", {
    month: "long",
    year: "numeric",
  });
}

export function toIsoDate(value: Date): string {
  return value.toISOString().slice(0, 10);
}

export function monthBounds(ym: YearMonth): { start: string; end: string } {
  const start = new Date(ym.year, ym.month - 1, 1);
  const end = new Date(ym.year, ym.month, 0);
  return { start: toIsoDate(start), end: toIsoDate(end) };
}
