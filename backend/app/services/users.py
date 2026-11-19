from __future__ import annotations

from uuid import UUID

from sqlalchemy.dialects.postgresql import insert
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.user import AppUser
from app.repositories.user import UserRepository
from app.schemas.user import UserProfileUpdate


class UserService:
    def __init__(self, session: AsyncSession) -> None:
        self._repo = UserRepository(session)
        self._session = session

    async def get_or_provision(self, user_id: UUID) -> AppUser:
        existing = await self._repo.get_by_id(user_id)
        if existing is not None:
            return existing

        stmt = (
            insert(AppUser)
            .values(
                id=user_id,
                display_name=None,
                base_currency="INR",
                timezone="UTC",
            )
            .on_conflict_do_nothing(index_elements=[AppUser.id])
        )
        await self._session.execute(stmt)
        await self._session.flush()

        user = await self._repo.get_by_id(user_id)
        if user is None:
            raise RuntimeError("Failed to provision app_users row")
        return user

    async def update_profile(self, user_id: UUID, payload: UserProfileUpdate) -> AppUser:
        user = await self.get_or_provision(user_id)
        data = payload.model_dump(exclude_unset=True)
        # Never accept client-supplied user id; identity is only from JWT.
        data.pop("id", None)
        data.pop("user_id", None)
        for key, value in data.items():
            setattr(user, key, value)
        return await self._repo.save(user)
