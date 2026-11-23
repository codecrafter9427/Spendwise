from datetime import datetime
from typing import Any, Literal
from uuid import UUID

from pydantic import BaseModel, Field


class SyncChangeRead(BaseModel):
    sequence: int
    entity_type: str
    entity_id: UUID
    operation: str
    version: int
    payload: dict[str, Any] | None
    changed_at: datetime


class SyncPullResponse(BaseModel):
    changes: list[SyncChangeRead]
    next_cursor: str | None
    has_more: bool


class SyncPushOperation(BaseModel):
    operation_id: str = Field(min_length=1, max_length=128)
    entity_type: Literal["transaction", "budget"]
    entity_id: UUID
    operation: Literal["upsert", "delete"]
    base_version: int | None = Field(default=None, ge=1)
    payload: dict[str, Any] | None = None


class SyncPushRequest(BaseModel):
    operations: list[SyncPushOperation] = Field(min_length=1, max_length=100)


class SyncPushItemResult(BaseModel):
    operation_id: str
    status: Literal["accepted", "duplicate", "rejected", "conflict"]
    entity_id: UUID | None = None
    version: int | None = None
    error_code: str | None = None
    error_message: str | None = None


class SyncPushResponse(BaseModel):
    results: list[SyncPushItemResult]
