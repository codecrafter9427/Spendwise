from __future__ import annotations

from uuid import UUID

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.user import AppUser


class UserRepository:
    def __init__(self, session: AsyncSession) -> None:
        self._session = session

    async def get_by_id(self, user_id: UUID) -> AppUser | None:
        result = await self._session.execute(select(AppUser).where(AppUser.id == user_id))
        return result.scalar_one_or_none()

    async def create(
        self,
        *,
        user_id: UUID,
        display_name: str | None = None,
        base_currency: str = "INR",
        timezone: str = "UTC",
    ) -> AppUser:
        user = AppUser(
            id=user_id,
            display_name=display_name,
            base_currency=base_currency,
            timezone=timezone,
        )
        self._session.add(user)
        await self._session.flush()
        await self._session.refresh(user)
        return user

    async def save(self, user: AppUser) -> AppUser:
        await self._session.flush()
        await self._session.refresh(user)
        return user
