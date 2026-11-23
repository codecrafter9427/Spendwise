from __future__ import annotations

import re
from datetime import date
from typing import Any
from uuid import UUID

import httpx
from fastapi import HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import Settings, get_settings
from app.schemas.ai import AiChatResponse, AiToolCall
from app.services.ai_tools import AiToolService
from app.services.users import UserService

DISCLAIMER = (
    "Figures come from your SpendWise records for the stated period. "
    "This is not investment, tax, credit, or regulated financial advice."
)

_BLOCKED = re.compile(
    r"\b(invest|investment|stock|mutual fund|crypto|bitcoin|tax advice|loan advice|"
    r"credit score|insurance advice|guaranteed return)\b",
    re.IGNORECASE,
)


class AiAssistantService:
    def __init__(self, session: AsyncSession, settings: Settings | None = None) -> None:
        self._session = session
        self._settings = settings or get_settings()
        self._users = UserService(session)
        self._tools = AiToolService(session)

    async def chat(
        self,
        user_id: UUID,
        *,
        message: str,
        year: int | None = None,
        month: int | None = None,
    ) -> AiChatResponse:
        if not self._settings.ai_feature_enabled:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail={"code": "AI_DISABLED", "message": "AI assistant is disabled on this server"},
            )

        user = await self._users.get_or_provision(user_id)
        if not user.ai_enabled:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail={
                    "code": "AI_CONSENT_REQUIRED",
                    "message": "Enable AI assistant in Settings to use this feature",
                },
            )

        text = message.strip()
        if _BLOCKED.search(text):
            raise HTTPException(
                status_code=getattr(status, "HTTP_422_UNPROCESSABLE_CONTENT", 422),
                detail={
                    "code": "UNSUPPORTED_REQUEST",
                    "message": "SpendWise AI only answers questions about your recorded spending, "
                    "income, budgets, and categories. It cannot give investment or tax advice.",
                },
            )

        today = date.today()
        period_year = year or today.year
        period_month = month or today.month
        tool_name, tool_args = self._route(text, period_year, period_month)
        result = await self._tools.run(user_id, tool_name, tool_args)
        tools_used = [AiToolCall(name=tool_name, arguments=tool_args, result=result)]

        if self._settings.ai_provider_api_key:
            answer = await self._llm_explain(text, tools_used)
            mode = "llm"
        else:
            answer = self._deterministic_answer(tool_name, result, period_year, period_month)
            mode = "deterministic"

        return AiChatResponse(
            answer=answer,
            mode=mode,
            period={"year": period_year, "month": period_month},
            tools_used=tools_used,
            disclaimer=DISCLAIMER,
        )

    def _route(self, message: str, year: int, month: int) -> tuple[str, dict[str, Any]]:
        lower = message.lower()
        args: dict[str, Any] = {"year": year, "month": month}

        if "compar" in lower or "vs last" in lower or "versus" in lower:
            prev_year, prev_month = (year - 1, 12) if month == 1 else (year, month - 1)
            return "compare_periods", {
                **args,
                "previous_year": prev_year,
                "previous_month": prev_month,
            }
        if "budget" in lower:
            return "budget_status", args
        if "largest" in lower or "biggest" in lower or "top expense" in lower:
            return "largest_expenses", {**args, "limit": 5}
        if "categor" in lower:
            return "category_spend", args
        return "monthly_summary", args

    def _deterministic_answer(
        self,
        tool_name: str,
        result: dict[str, Any],
        year: int,
        month: int,
    ) -> str:
        period = f"{year}-{month:02d}"
        if tool_name == "monthly_summary":
            return (
                f"For {period}, income was {result['total_income']}, "
                f"expenses {result['total_expense']}, net {result['net']}. "
                f"{DISCLAIMER}"
            )
        if tool_name == "budget_status":
            if result["budget_amount"] is None:
                return f"No budget is set for {period}. Spent {result['spent']}. {DISCLAIMER}"
            util = result.get("utilization_percent")
            util_txt = f"{util:.1f}%" if util is not None else "n/a"
            return (
                f"Budget for {period}: {result['budget_amount']}. "
                f"Spent {result['spent']}, remaining {result['remaining']} "
                f"({util_txt} used). {DISCLAIMER}"
            )
        if tool_name == "category_spend":
            cats = result.get("categories") or []
            if not cats:
                return f"No expense categories recorded for {period}. {DISCLAIMER}"
            top = ", ".join(f"{c['name']} {c['amount']}" for c in cats[:5])
            return f"Category spend for {period} (total {result['total_expense']}): {top}. {DISCLAIMER}"
        if tool_name == "largest_expenses":
            items = result.get("items") or []
            if not items:
                return f"No expenses found for {period}. {DISCLAIMER}"
            lines = ", ".join(
                f"{i['amount']} ({i['category']}"
                + (f", {i['description']}" if i.get("description") else "")
                + ")"
                for i in items
            )
            return f"Largest expenses in {period}: {lines}. {DISCLAIMER}"
        if tool_name == "compare_periods":
            cur = result["current"]
            prev = result["previous"]
            return (
                f"{cur['year']}-{cur['month']:02d} expenses {cur['total_expense']} vs "
                f"{prev['year']}-{prev['month']:02d} {prev['total_expense']}. {DISCLAIMER}"
            )
        return f"Here are the verified tool results for {period}. {DISCLAIMER}"

    async def _llm_explain(self, question: str, tools_used: list[AiToolCall]) -> str:
        key = self._settings.ai_provider_api_key
        assert key is not None
        payload = {
            "model": self._settings.ai_model,
            "temperature": 0.2,
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "You explain SpendWise finance tool results only. "
                        "Use only the provided tool JSON numbers. Do not invent totals. "
                        "Do not give investment/tax/credit advice. Be concise."
                    ),
                },
                {
                    "role": "user",
                    "content": (
                        f"Question: {question}\n\n"
                        f"Verified tool results: {[t.model_dump() for t in tools_used]}"
                    ),
                },
            ],
        }
        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.post(
                    f"{self._settings.ai_provider_base_url.rstrip('/')}/chat/completions",
                    headers={"Authorization": f"Bearer {key}"},
                    json=payload,
                )
                response.raise_for_status()
                data = response.json()
                content = data["choices"][0]["message"]["content"]
                return f"{content.strip()}\n\n{DISCLAIMER}"
        except Exception:
            # Fall back so the product still works if the provider is down.
            first = tools_used[0]
            return self._deterministic_answer(
                first.name,
                first.result,
                int(first.arguments["year"]),
                int(first.arguments["month"]),
            )
