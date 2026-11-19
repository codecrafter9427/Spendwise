"use client";

import { formatMonthYear, shiftYearMonth, type YearMonth } from "@/lib/dates";

type Props = {
  value: YearMonth;
  onChange: (value: YearMonth) => void;
};

export function MonthPicker({ value, onChange }: Props) {
  return (
    <div className="row">
      <button
        type="button"
        className="btn secondary"
        onClick={() => onChange(shiftYearMonth(value, -1))}
      >
        Prev
      </button>
      <strong>{formatMonthYear(value)}</strong>
      <button
        type="button"
        className="btn secondary"
        onClick={() => onChange(shiftYearMonth(value, 1))}
      >
        Next
      </button>
    </div>
  );
}
