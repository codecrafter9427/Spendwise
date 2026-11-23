import type {
  AiChatResponse,
  AnalyticsSummary,
  Budget,
  Category,
  CategorySpendResponse,
  CursorPage,
  Transaction,
  TransactionType,
  UserProfile,
} from "@/lib/types";
import { ApiError } from "@/lib/types";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8000";

type RequestOptions = {
  method?: string;
  token: string;
  body?: unknown;
  query?: Record<string, string | number | undefined | null>;
};

async function apiRequest<T>(path: string, options: RequestOptions): Promise<T> {
  const url = new URL(path, API_BASE);
  if (options.query) {
    for (const [key, value] of Object.entries(options.query)) {
      if (value !== undefined && value !== null && value !== "") {
        url.searchParams.set(key, String(value));
      }
    }
  }

  const response = await fetch(url.toString(), {
    method: options.method ?? "GET",
    headers: {
      Authorization: `Bearer ${options.token}`,
      "Content-Type": "application/json",
    },
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    cache: "no-store",
  });

  if (!response.ok) {
    let code = "HTTP_ERROR";
    let message = `Request failed (${response.status})`;
    try {
      const payload = await response.json();
      code = payload?.error?.code ?? code;
      message = payload?.error?.message ?? message;
    } catch {
      // ignore non-JSON errors
    }
    throw new ApiError(response.status, code, message);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export const api = {
  getMe: (token: string) => apiRequest<UserProfile>("/api/v1/me", { token }),
  patchMe: (
    token: string,
    body: {
      display_name?: string | null;
      timezone?: string;
      base_currency?: string;
      ai_enabled?: boolean;
    },
  ) => apiRequest<UserProfile>("/api/v1/me", { method: "PATCH", token, body }),

  aiChat: (
    token: string,
    body: { message: string; year?: number; month?: number },
  ) => apiRequest<AiChatResponse>("/api/v1/ai/chat", { method: "POST", token, body }),

  listCategories: (token: string, type?: TransactionType) =>
    apiRequest<Category[]>("/api/v1/categories", {
      token,
      query: { type },
    }),

  listTransactions: (
    token: string,
    query: {
      limit?: number;
      cursor?: string | null;
      start_date?: string;
      end_date?: string;
      type?: TransactionType;
      category_id?: string;
      q?: string;
    },
  ) =>
    apiRequest<CursorPage<Transaction>>("/api/v1/transactions", {
      token,
      query,
    }),

  createTransaction: (
    token: string,
    body: {
      id: string;
      type: TransactionType;
      amount_minor: number;
      currency?: string;
      category_id: string;
      description?: string | null;
      transaction_date: string;
    },
  ) =>
    apiRequest<Transaction>("/api/v1/transactions", {
      method: "POST",
      token,
      body,
    }),

  updateTransaction: (
    token: string,
    id: string,
    body: {
      expected_version: number;
      description?: string | null;
      amount_minor?: number;
      category_id?: string;
      type?: TransactionType;
      transaction_date?: string;
    },
  ) =>
    apiRequest<Transaction>(`/api/v1/transactions/${id}`, {
      method: "PATCH",
      token,
      body,
    }),

  deleteTransaction: (token: string, id: string, expectedVersion: number) =>
    apiRequest<Transaction>(`/api/v1/transactions/${id}`, {
      method: "DELETE",
      token,
      query: { expected_version: expectedVersion },
    }),

  listBudgets: (token: string) => apiRequest<Budget[]>("/api/v1/budgets", { token }),

  upsertBudget: (
    token: string,
    year: number,
    month: number,
    body: { amount_minor: number; currency?: string; expected_version?: number },
  ) =>
    apiRequest<Budget>(`/api/v1/budgets/${year}/${month}`, {
      method: "PUT",
      token,
      body,
    }),

  analyticsSummary: (token: string, year: number, month: number) =>
    apiRequest<AnalyticsSummary>("/api/v1/analytics/summary", {
      token,
      query: { year, month },
    }),

  analyticsCategories: (token: string, year: number, month: number) =>
    apiRequest<CategorySpendResponse>("/api/v1/analytics/categories", {
      token,
      query: { year, month },
    }),
};
