from __future__ import annotations

from uuid import UUID, uuid4

from fastapi import HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.budget import MonthlyBudget
from app.repositories.budget import BudgetRepository
from app.schemas.budget import BudgetUpsert
from app.services.sync_log import SyncLogService
from app.services.sync_payloads import budget_payload
from app.services.users import UserService


class BudgetService:
    def __init__(self, session: AsyncSession) -> None:
        self._repo = BudgetRepository(session)
        self._users = UserService(session)
        self._sync = SyncLogService(session)

    async def list_budgets(self, user_id: UUID) -> list[MonthlyBudget]:
        await self._users.get_or_provision(user_id)
        return await self._repo.list_for_user(user_id)

    async def upsert(
        self,
        user_id: UUID,
        year: int,
        month: int,
        payload: BudgetUpsert,
    ) -> MonthlyBudget:
        if month < 1 or month > 12:
            raise HTTPException(
                status_code=422,
                detail={"code": "INVALID_MONTH", "message": "month must be between 1 and 12"},
            )
        await self._users.get_or_provision(user_id)
        existing = await self._repo.get_for_period(
            user_id, year, month, currency=payload.currency
        )
        if existing is None:
            budget = MonthlyBudget(
                id=uuid4(),
                user_id=user_id,
                year=year,
                month=month,
                amount_minor=payload.amount_minor,
                currency=payload.currency,
                version=1,
            )
            saved = await self._repo.add(budget)
        else:
            if (
                payload.expected_version is not None
                and existing.version != payload.expected_version
            ):
                raise HTTPException(
                    status_code=status.HTTP_409_CONFLICT,
                    detail={
                        "code": "VERSION_CONFLICT",
                        "message": "Stale budget version",
                        "details": {"current_version": existing.version},
                    },
                )
            existing.amount_minor = payload.amount_minor
            existing.currency = payload.currency
            existing.version += 1
            saved = await self._repo.save(existing)

        await self._sync.record(
            user_id=user_id,
            entity_type="budget",
            entity_id=saved.id,
            operation="upsert",
            version=saved.version,
            payload=budget_payload(saved),
        )
        return saved
