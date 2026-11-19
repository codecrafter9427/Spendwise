from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.domain.enums import TransactionType


class CategoryCreate(BaseModel):
    name: str = Field(min_length=1, max_length=80)
    type: TransactionType
    icon_key: str = Field(default="more_horiz", min_length=1, max_length=64)

    @field_validator("name")
    @classmethod
    def strip_name(cls, value: str) -> str:
        trimmed = value.strip()
        if not trimmed:
            raise ValueError("name is required")
        return trimmed


class CategoryUpdate(BaseModel):
    name: str | None = Field(default=None, min_length=1, max_length=80)
    icon_key: str | None = Field(default=None, min_length=1, max_length=64)
    archived: bool | None = None

    @field_validator("name")
    @classmethod
    def strip_name(cls, value: str | None) -> str | None:
        if value is None:
            return None
        trimmed = value.strip()
        if not trimmed:
            raise ValueError("name is required")
        return trimmed


class CategoryRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    user_id: UUID | None
    name: str
    type: TransactionType
    icon_key: str
    is_default: bool
    archived_at: datetime | None
    created_at: datetime
    updated_at: datetime
