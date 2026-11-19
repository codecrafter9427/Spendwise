from __future__ import annotations

from uuid import UUID

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.budget import MonthlyBudget


class BudgetRepository:
    def __init__(self, session: AsyncSession) -> None:
        self._session = session

    async def list_for_user(self, user_id: UUID) -> list[MonthlyBudget]:
        result = await self._session.execute(
            select(MonthlyBudget)
            .where(MonthlyBudget.user_id == user_id)
            .order_by(MonthlyBudget.year.desc(), MonthlyBudget.month.desc())
        )
        return list(result.scalars().all())

    async def get_for_period(
        self,
        user_id: UUID,
        year: int,
        month: int,
        currency: str = "INR",
    ) -> MonthlyBudget | None:
        result = await self._session.execute(
            select(MonthlyBudget).where(
                MonthlyBudget.user_id == user_id,
                MonthlyBudget.year == year,
                MonthlyBudget.month == month,
                MonthlyBudget.currency == currency,
            )
        )
        return result.scalar_one_or_none()

    async def add(self, budget: MonthlyBudget) -> MonthlyBudget:
        self._session.add(budget)
        await self._session.flush()
        await self._session.refresh(budget)
        return budget

    async def save(self, budget: MonthlyBudget) -> MonthlyBudget:
        await self._session.flush()
        await self._session.refresh(budget)
        return budget
