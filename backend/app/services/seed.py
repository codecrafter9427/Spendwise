from __future__ import annotations

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.domain.default_categories import DEFAULT_CATEGORIES
from app.models.category import Category


async def ensure_default_categories(session: AsyncSession) -> None:
    """Idempotent seed used by tests (Alembic migration seeds production DBs)."""
    existing = await session.execute(
        select(Category.id).where(Category.is_default.is_(True)).limit(1)
    )
    if existing.scalar_one_or_none() is not None:
        return
    for seed in DEFAULT_CATEGORIES:
        session.add(
            Category(
                id=seed.id,
                user_id=None,
                name=seed.name,
                type=seed.type.value,
                icon_key=seed.icon_key,
                is_default=True,
            )
        )
    await session.flush()
