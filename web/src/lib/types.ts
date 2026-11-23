export type TransactionType = "income" | "expense";

export type UserProfile = {
  id: string;
  display_name: string | null;
  base_currency: string;
  timezone: string;
  ai_enabled: boolean;
  created_at: string;
  updated_at: string;
};

export type AiChatResponse = {
  answer: string;
  mode: "deterministic" | "llm";
  period: { year: number; month: number };
  tools_used: Array<{
    name: string;
    arguments: Record<string, unknown>;
    result: Record<string, unknown>;
  }>;
  disclaimer: string;
};

export type Category = {
  id: string;
  user_id: string | null;
  name: string;
  type: TransactionType;
  icon_key: string;
  is_default: boolean;
  archived_at: string | null;
  created_at: string;
  updated_at: string;
};

export type Transaction = {
  id: string;
  user_id: string;
  type: TransactionType;
  amount_minor: number;
  currency: string;
  category_id: string;
  description: string | null;
  transaction_date: string;
  version: number;
  deleted_at: string | null;
  created_at: string;
  updated_at: string;
};

export type CursorPage<T> = {
  items: T[];
  next_cursor: string | null;
  limit: number;
};

export type Budget = {
  id: string;
  user_id: string;
  year: number;
  month: number;
  amount_minor: number;
  currency: string;
  version: number;
  created_at: string;
  updated_at: string;
};

export type AnalyticsSummary = {
  start_date: string;
  end_date: string;
  currency: string;
  total_income_minor: number;
  total_expense_minor: number;
  net_minor: number;
  budget_amount_minor: number | null;
  budget_remaining_minor: number | null;
  budget_utilization_percent: number | null;
};

export type CategorySpendResponse = {
  start_date: string;
  end_date: string;
  currency: string;
  total_expense_minor: number;
  items: Array<{
    category_id: string;
    category_name: string;
    type: TransactionType;
    amount_minor: number;
    percent_of_total: number;
  }>;
};

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}
