from __future__ import annotations

from typing import Any
from uuid import UUID

from sqlalchemy.ext.asyncio import AsyncSession

from app.models.sync import SyncChange


class SyncLogService:
    def __init__(self, session: AsyncSession) -> None:
        self._session = session

    async def record(
        self,
        *,
        user_id: UUID,
        entity_type: str,
        entity_id: UUID,
        operation: str,
        version: int,
        payload: dict[str, Any] | None = None,
    ) -> SyncChange:
        change = SyncChange(
            user_id=user_id,
            entity_type=entity_type,
            entity_id=entity_id,
            operation=operation,
            version=version,
            payload=payload,
        )
        self._session.add(change)
        await self._session.flush()
        return change
