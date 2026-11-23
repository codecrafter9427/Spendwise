from __future__ import annotations

from datetime import UTC, datetime
from typing import Any
from uuid import UUID

from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.budget import MonthlyBudget
from app.models.category import Category
from app.models.sync import IdempotencyRecord, SyncChange
from app.models.transaction import Transaction
from app.models.user import AppUser
from app.services.users import UserService


class AccountService:
    def __init__(self, session: AsyncSession) -> None:
        self._session = session
        self._users = UserService(session)

    async def export(self, user_id: UUID) -> dict[str, Any]:
        user = await self._users.get_or_provision(user_id)
        categories = (
            await self._session.execute(
                select(Category).where(
                    (Category.user_id == user_id) | (Category.user_id.is_(None))
                )
            )
        ).scalars().all()
        transactions = (
            await self._session.execute(
                select(Transaction).where(Transaction.user_id == user_id)
            )
        ).scalars().all()
        budgets = (
            await self._session.execute(
                select(MonthlyBudget).where(MonthlyBudget.user_id == user_id)
            )
        ).scalars().all()

        return {
            "exported_at": datetime.now(UTC).isoformat(),
            "profile": {
                "id": str(user.id),
                "display_name": user.display_name,
                "base_currency": user.base_currency,
                "timezone": user.timezone,
                "ai_enabled": user.ai_enabled,
            },
            "categories": [
                {
                    "id": str(c.id),
                    "user_id": str(c.user_id) if c.user_id else None,
                    "name": c.name,
                    "type": c.type,
                    "icon_key": c.icon_key,
                    "is_default": c.is_default,
                    "archived_at": c.archived_at.isoformat() if c.archived_at else None,
                }
                for c in categories
            ],
            "transactions": [
                {
                    "id": str(t.id),
                    "type": t.type,
                    "amount_minor": t.amount_minor,
                    "currency": t.currency,
                    "category_id": str(t.category_id),
                    "description": t.description,
                    "transaction_date": t.transaction_date.isoformat(),
                    "version": t.version,
                    "deleted_at": t.deleted_at.isoformat() if t.deleted_at else None,
                    "created_at": t.created_at.isoformat() if t.created_at else None,
                    "updated_at": t.updated_at.isoformat() if t.updated_at else None,
                }
                for t in transactions
            ],
            "budgets": [
                {
                    "id": str(b.id),
                    "year": b.year,
                    "month": b.month,
                    "amount_minor": b.amount_minor,
                    "currency": b.currency,
                    "version": b.version,
                }
                for b in budgets
            ],
        }

    async def delete_account(self, user_id: UUID) -> dict[str, int]:
        await self._users.get_or_provision(user_id)

        tx_result = await self._session.execute(
            delete(Transaction).where(Transaction.user_id == user_id)
        )
        budget_result = await self._session.execute(
            delete(MonthlyBudget).where(MonthlyBudget.user_id == user_id)
        )
        cat_result = await self._session.execute(
            delete(Category).where(Category.user_id == user_id)
        )
        sync_result = await self._session.execute(
            delete(SyncChange).where(SyncChange.user_id == user_id)
        )
        idem_result = await self._session.execute(
            delete(IdempotencyRecord).where(IdempotencyRecord.user_id == user_id)
        )
        user_result = await self._session.execute(delete(AppUser).where(AppUser.id == user_id))
        await self._session.flush()

        return {
            "transactions": tx_result.rowcount or 0,
            "budgets": budget_result.rowcount or 0,
            "categories": cat_result.rowcount or 0,
            "sync_changes": sync_result.rowcount or 0,
            "idempotency_records": idem_result.rowcount or 0,
            "users": user_result.rowcount or 0,
        }
