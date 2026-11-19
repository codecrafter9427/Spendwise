from __future__ import annotations

from datetime import UTC, datetime
from uuid import UUID, uuid4

from fastapi import HTTPException, status
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.category import Category
from app.repositories.category import CategoryRepository
from app.schemas.category import CategoryCreate, CategoryUpdate
from app.services.users import UserService


class CategoryService:
    def __init__(self, session: AsyncSession) -> None:
        self._session = session
        self._repo = CategoryRepository(session)
        self._users = UserService(session)

    async def list_categories(
        self,
        user_id: UUID,
        *,
        type_filter: str | None = None,
        include_archived: bool = False,
    ) -> list[Category]:
        await self._users.get_or_provision(user_id)
        return await self._repo.list_for_user(
            user_id,
            type_filter=type_filter,
            include_archived=include_archived,
        )

    async def create(self, user_id: UUID, payload: CategoryCreate) -> Category:
        await self._users.get_or_provision(user_id)
        category = Category(
            id=uuid4(),
            user_id=user_id,
            name=payload.name,
            type=payload.type.value,
            icon_key=payload.icon_key,
            is_default=False,
        )
        try:
            return await self._repo.add(category)
        except IntegrityError as exc:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail={
                    "code": "CATEGORY_EXISTS",
                    "message": "A category with this name and type already exists",
                },
            ) from exc

    async def update(
        self,
        user_id: UUID,
        category_id: UUID,
        payload: CategoryUpdate,
    ) -> Category:
        await self._users.get_or_provision(user_id)
        category = await self._repo.get_accessible(category_id, user_id)
        if category is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail={"code": "CATEGORY_NOT_FOUND", "message": "Category not found"},
            )
        if category.is_default or category.user_id is None:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail={
                    "code": "DEFAULT_CATEGORY_IMMUTABLE",
                    "message": "Default categories cannot be modified",
                },
            )
        if category.user_id != user_id:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail={"code": "FORBIDDEN", "message": "Category is not owned by user"},
            )

        data = payload.model_dump(exclude_unset=True)
        if "name" in data:
            category.name = data["name"]
        if "icon_key" in data and data["icon_key"] is not None:
            category.icon_key = data["icon_key"]
        if "archived" in data and data["archived"] is not None:
            category.archived_at = datetime.now(UTC) if data["archived"] else None

        try:
            return await self._repo.save(category)
        except IntegrityError as exc:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail={
                    "code": "CATEGORY_EXISTS",
                    "message": "A category with this name and type already exists",
                },
            ) from exc
