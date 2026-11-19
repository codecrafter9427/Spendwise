from __future__ import annotations

from uuid import UUID

from sqlalchemy import or_, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.category import Category


class CategoryRepository:
    def __init__(self, session: AsyncSession) -> None:
        self._session = session

    async def list_for_user(
        self,
        user_id: UUID,
        *,
        type_filter: str | None = None,
        include_archived: bool = False,
    ) -> list[Category]:
        stmt = select(Category).where(
            or_(Category.user_id.is_(None), Category.user_id == user_id)
        )
        if type_filter:
            stmt = stmt.where(Category.type == type_filter)
        if not include_archived:
            stmt = stmt.where(Category.archived_at.is_(None))
        stmt = stmt.order_by(Category.is_default.desc(), Category.name.asc())
        result = await self._session.execute(stmt)
        return list(result.scalars().all())

    async def get_accessible(self, category_id: UUID, user_id: UUID) -> Category | None:
        result = await self._session.execute(
            select(Category).where(
                Category.id == category_id,
                or_(Category.user_id.is_(None), Category.user_id == user_id),
            )
        )
        return result.scalar_one_or_none()

    async def add(self, category: Category) -> Category:
        self._session.add(category)
        await self._session.flush()
        await self._session.refresh(category)
        return category

    async def save(self, category: Category) -> Category:
        await self._session.flush()
        await self._session.refresh(category)
        return category
