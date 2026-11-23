from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, field_validator


class UserProfileRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    display_name: str | None
    base_currency: str
    timezone: str
    ai_enabled: bool = False
    created_at: datetime
    updated_at: datetime


class UserProfileUpdate(BaseModel):
    display_name: str | None = Field(default=None, max_length=120)
    base_currency: str | None = Field(default=None, min_length=3, max_length=3)
    timezone: str | None = Field(default=None, min_length=1, max_length=64)
    ai_enabled: bool | None = None

    @field_validator("base_currency")
    @classmethod
    def currency_uppercase(cls, value: str | None) -> str | None:
        if value is None:
            return None
        normalized = value.upper()
        if normalized != "INR":
            raise ValueError("Only INR is supported in Stage 1")
        return normalized

    @field_validator("display_name")
    @classmethod
    def strip_display_name(cls, value: str | None) -> str | None:
        if value is None:
            return None
        trimmed = value.strip()
        return trimmed or None
