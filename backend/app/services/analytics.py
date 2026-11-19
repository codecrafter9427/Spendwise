from __future__ import annotations

from calendar import monthrange
from datetime import date
from uuid import UUID

from fastapi import HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.domain.enums import TransactionType
from app.repositories.budget import BudgetRepository
from app.repositories.category import CategoryRepository
from app.repositories.transaction import TransactionRepository
from app.schemas.analytics import AnalyticsSummary, CategorySpendItem, CategorySpendResponse
from app.services.users import UserService


class AnalyticsService:
    def __init__(self, session: AsyncSession) -> None:
        self._transactions = TransactionRepository(session)
        self._budgets = BudgetRepository(session)
        self._categories = CategoryRepository(session)
        self._users = UserService(session)

    @staticmethod
    def _period(year: int, month: int) -> tuple[date, date]:
        if month < 1 or month > 12:
            raise HTTPException(
                status_code=422,
                detail={"code": "INVALID_MONTH", "message": "month must be between 1 and 12"},
            )
        start = date(year, month, 1)
        end = date(year, month, monthrange(year, month)[1])
        return start, end

    async def summary(
        self,
        user_id: UUID,
        *,
        year: int,
        month: int,
        currency: str = "INR",
    ) -> AnalyticsSummary:
        await self._users.get_or_provision(user_id)
        start, end = self._period(year, month)
        income = await self._transactions.sum_by_type(
            user_id,
            start_date=start,
            end_date=end,
            type_filter=TransactionType.INCOME.value,
            currency=currency,
        )
        expense = await self._transactions.sum_by_type(
            user_id,
            start_date=start,
            end_date=end,
            type_filter=TransactionType.EXPENSE.value,
            currency=currency,
        )
        budget = await self._budgets.get_for_period(user_id, year, month, currency=currency)
        budget_amount = budget.amount_minor if budget else None
        remaining = None
        utilization = None
        if budget_amount is not None and budget_amount > 0:
            remaining = budget_amount - expense
            utilization = (expense / budget_amount) * 100.0

        return AnalyticsSummary(
            start_date=start,
            end_date=end,
            currency=currency,
            total_income_minor=income,
            total_expense_minor=expense,
            net_minor=income - expense,
            budget_amount_minor=budget_amount,
            budget_remaining_minor=remaining,
            budget_utilization_percent=utilization,
        )

    async def category_spend(
        self,
        user_id: UUID,
        *,
        year: int,
        month: int,
        currency: str = "INR",
    ) -> CategorySpendResponse:
        await self._users.get_or_provision(user_id)
        start, end = self._period(year, month)
        rows = await self._transactions.expense_totals_by_category(
            user_id,
            start_date=start,
            end_date=end,
            currency=currency,
        )
        total = sum(amount for _, amount in rows)
        categories = {
            item.id: item
            for item in await self._categories.list_for_user(user_id, include_archived=True)
        }
        items: list[CategorySpendItem] = []
        for category_id, amount in rows:
            category = categories.get(category_id)
            items.append(
                CategorySpendItem(
                    category_id=category_id,
                    category_name=category.name if category else "Unknown",
                    type=TransactionType.EXPENSE,
                    amount_minor=amount,
                    percent_of_total=(amount / total * 100.0) if total else 0.0,
                )
            )
        return CategorySpendResponse(
            start_date=start,
            end_date=end,
            currency=currency,
            total_expense_minor=total,
            items=items,
        )
