from __future__ import annotations

import base64
import json
from datetime import date, datetime
from uuid import UUID

from sqlalchemy import Select, and_, func, or_, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.transaction import Transaction


class TransactionRepository:
    def __init__(self, session: AsyncSession) -> None:
        self._session = session

    async def get_owned(
        self,
        transaction_id: UUID,
        user_id: UUID,
        *,
        include_deleted: bool = False,
    ) -> Transaction | None:
        stmt = select(Transaction).where(
            Transaction.id == transaction_id,
            Transaction.user_id == user_id,
        )
        if not include_deleted:
            stmt = stmt.where(Transaction.deleted_at.is_(None))
        result = await self._session.execute(stmt)
        return result.scalar_one_or_none()

    async def add(self, transaction: Transaction) -> Transaction:
        self._session.add(transaction)
        await self._session.flush()
        await self._session.refresh(transaction)
        return transaction

    async def save(self, transaction: Transaction) -> Transaction:
        await self._session.flush()
        await self._session.refresh(transaction)
        return transaction

    def _filtered_stmt(
        self,
        user_id: UUID,
        *,
        start_date: date | None,
        end_date: date | None,
        type_filter: str | None,
        category_id: UUID | None,
        query: str | None,
        include_deleted: bool,
    ) -> Select[tuple[Transaction]]:
        stmt = select(Transaction).where(Transaction.user_id == user_id)
        if not include_deleted:
            stmt = stmt.where(Transaction.deleted_at.is_(None))
        if start_date is not None:
            stmt = stmt.where(Transaction.transaction_date >= start_date)
        if end_date is not None:
            stmt = stmt.where(Transaction.transaction_date <= end_date)
        if type_filter is not None:
            stmt = stmt.where(Transaction.type == type_filter)
        if category_id is not None:
            stmt = stmt.where(Transaction.category_id == category_id)
        if query:
            pattern = f"%{query.strip()}%"
            stmt = stmt.where(Transaction.description.ilike(pattern))
        return stmt

    async def list_page(
        self,
        user_id: UUID,
        *,
        limit: int,
        cursor: str | None,
        start_date: date | None,
        end_date: date | None,
        type_filter: str | None,
        category_id: UUID | None,
        query: str | None,
    ) -> tuple[list[Transaction], str | None]:
        stmt = self._filtered_stmt(
            user_id,
            start_date=start_date,
            end_date=end_date,
            type_filter=type_filter,
            category_id=category_id,
            query=query,
            include_deleted=False,
        )
        stmt = stmt.order_by(
            Transaction.transaction_date.desc(),
            Transaction.created_at.desc(),
            Transaction.id.desc(),
        )
        if cursor:
            cursor_date, cursor_created, cursor_id = _decode_cursor(cursor)
            stmt = stmt.where(
                or_(
                    Transaction.transaction_date < cursor_date,
                    and_(
                        Transaction.transaction_date == cursor_date,
                        Transaction.created_at < cursor_created,
                    ),
                    and_(
                        Transaction.transaction_date == cursor_date,
                        Transaction.created_at == cursor_created,
                        Transaction.id < cursor_id,
                    ),
                )
            )
        stmt = stmt.limit(limit + 1)
        rows = list((await self._session.execute(stmt)).scalars().all())
        next_cursor = None
        if len(rows) > limit:
            rows = rows[:limit]
            last = rows[-1]
            next_cursor = _encode_cursor(last.transaction_date, last.created_at, last.id)
        return rows, next_cursor

    async def sum_by_type(
        self,
        user_id: UUID,
        *,
        start_date: date,
        end_date: date,
        type_filter: str,
        currency: str = "INR",
    ) -> int:
        stmt = select(func.coalesce(func.sum(Transaction.amount_minor), 0)).where(
            Transaction.user_id == user_id,
            Transaction.deleted_at.is_(None),
            Transaction.transaction_date >= start_date,
            Transaction.transaction_date <= end_date,
            Transaction.type == type_filter,
            Transaction.currency == currency,
        )
        result = await self._session.execute(stmt)
        return int(result.scalar_one())

    async def expense_totals_by_category(
        self,
        user_id: UUID,
        *,
        start_date: date,
        end_date: date,
        currency: str = "INR",
    ) -> list[tuple[UUID, int]]:
        stmt = (
            select(Transaction.category_id, func.sum(Transaction.amount_minor))
            .where(
                Transaction.user_id == user_id,
                Transaction.deleted_at.is_(None),
                Transaction.transaction_date >= start_date,
                Transaction.transaction_date <= end_date,
                Transaction.type == "expense",
                Transaction.currency == currency,
            )
            .group_by(Transaction.category_id)
            .order_by(func.sum(Transaction.amount_minor).desc())
        )
        result = await self._session.execute(stmt)
        return [(row[0], int(row[1])) for row in result.all()]


def _encode_cursor(transaction_date: date, created_at: datetime, entity_id: UUID) -> str:
    payload = {
        "d": transaction_date.isoformat(),
        "c": created_at.isoformat(),
        "i": str(entity_id),
    }
    raw = json.dumps(payload, separators=(",", ":")).encode("utf-8")
    return base64.urlsafe_b64encode(raw).decode("ascii")


def _decode_cursor(cursor: str) -> tuple[date, datetime, UUID]:
    try:
        raw = base64.urlsafe_b64decode(cursor.encode("ascii"))
        payload = json.loads(raw.decode("utf-8"))
        return (
            date.fromisoformat(payload["d"]),
            datetime.fromisoformat(payload["c"]),
            UUID(payload["i"]),
        )
    except Exception as exc:  # noqa: BLE001
        raise ValueError("Invalid cursor") from exc
