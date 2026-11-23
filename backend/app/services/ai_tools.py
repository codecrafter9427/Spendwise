from __future__ import annotations

from calendar import monthrange
from datetime import date
from typing import Any
from uuid import UUID

from sqlalchemy.ext.asyncio import AsyncSession

from app.repositories.category import CategoryRepository
from app.repositories.transaction import TransactionRepository
from app.services.analytics import AnalyticsService


def _paise_to_rupees(amount_minor: int) -> str:
    return f"₹{amount_minor / 100:,.2f}"


class AiToolService:
    """Allowlisted, user-scoped read tools. No arbitrary SQL. No mutations."""

    ALLOWED = frozenset(
        {
            "monthly_summary",
            "category_spend",
            "largest_expenses",
            "budget_status",
            "compare_periods",
        }
    )

    def __init__(self, session: AsyncSession) -> None:
        self._session = session
        self._analytics = AnalyticsService(session)
        self._transactions = TransactionRepository(session)
        self._categories = CategoryRepository(session)

    async def run(
        self,
        user_id: UUID,
        name: str,
        arguments: dict[str, Any],
    ) -> dict[str, Any]:
        if name not in self.ALLOWED:
            raise ValueError(f"Tool not allowed: {name}")
        handler = getattr(self, name)
        return await handler(user_id, **arguments)

    async def monthly_summary(
        self,
        user_id: UUID,
        *,
        year: int,
        month: int,
    ) -> dict[str, Any]:
        summary = await self._analytics.summary(user_id, year=year, month=month)
        return {
            "year": year,
            "month": month,
            "currency": summary.currency,
            "total_income_minor": summary.total_income_minor,
            "total_expense_minor": summary.total_expense_minor,
            "net_minor": summary.net_minor,
            "total_income": _paise_to_rupees(summary.total_income_minor),
            "total_expense": _paise_to_rupees(summary.total_expense_minor),
            "net": _paise_to_rupees(summary.net_minor),
        }

    async def category_spend(
        self,
        user_id: UUID,
        *,
        year: int,
        month: int,
    ) -> dict[str, Any]:
        data = await self._analytics.category_spend(user_id, year=year, month=month)
        return {
            "year": year,
            "month": month,
            "total_expense_minor": data.total_expense_minor,
            "total_expense": _paise_to_rupees(data.total_expense_minor),
            "categories": [
                {
                    "name": item.category_name,
                    "amount_minor": item.amount_minor,
                    "amount": _paise_to_rupees(item.amount_minor),
                    "percent": round(item.percent_of_total, 2),
                }
                for item in data.items[:10]
            ],
        }

    async def largest_expenses(
        self,
        user_id: UUID,
        *,
        year: int,
        month: int,
        limit: int = 5,
    ) -> dict[str, Any]:
        limit = max(1, min(int(limit), 10))
        start = date(year, month, 1)
        end = date(year, month, monthrange(year, month)[1])
        rows = await self._transactions.largest_expenses(
            user_id,
            start_date=start,
            end_date=end,
            limit=limit,
        )
        categories = {
            c.id: c.name
            for c in await self._categories.list_for_user(user_id, include_archived=True)
        }
        return {
            "year": year,
            "month": month,
            "items": [
                {
                    "amount_minor": row.amount_minor,
                    "amount": _paise_to_rupees(row.amount_minor),
                    "description": row.description,
                    "category": categories.get(row.category_id, "Unknown"),
                    "date": row.transaction_date.isoformat(),
                }
                for row in rows
            ],
        }

    async def budget_status(
        self,
        user_id: UUID,
        *,
        year: int,
        month: int,
    ) -> dict[str, Any]:
        summary = await self._analytics.summary(user_id, year=year, month=month)
        return {
            "year": year,
            "month": month,
            "budget_amount_minor": summary.budget_amount_minor,
            "budget_amount": (
                _paise_to_rupees(summary.budget_amount_minor)
                if summary.budget_amount_minor is not None
                else None
            ),
            "spent_minor": summary.total_expense_minor,
            "spent": _paise_to_rupees(summary.total_expense_minor),
            "remaining_minor": summary.budget_remaining_minor,
            "remaining": (
                _paise_to_rupees(summary.budget_remaining_minor)
                if summary.budget_remaining_minor is not None
                else None
            ),
            "utilization_percent": summary.budget_utilization_percent,
        }

    async def compare_periods(
        self,
        user_id: UUID,
        *,
        year: int,
        month: int,
        previous_year: int,
        previous_month: int,
    ) -> dict[str, Any]:
        current = await self.monthly_summary(user_id, year=year, month=month)
        previous = await self.monthly_summary(
            user_id,
            year=previous_year,
            month=previous_month,
        )
        return {
            "current": current,
            "previous": previous,
            "expense_delta_minor": current["total_expense_minor"] - previous["total_expense_minor"],
            "income_delta_minor": current["total_income_minor"] - previous["total_income_minor"],
        }
