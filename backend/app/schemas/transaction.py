from datetime import date, datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.domain.enums import TransactionType
from app.domain.money import (
    MAX_DESCRIPTION_LENGTH,
    validate_currency,
    validate_positive_amount_minor,
)


class TransactionCreate(BaseModel):
    id: UUID
    type: TransactionType
    amount_minor: int
    currency: str = "INR"
    category_id: UUID
    description: str | None = Field(default=None, max_length=MAX_DESCRIPTION_LENGTH)
    transaction_date: date

    @field_validator("amount_minor")
    @classmethod
    def check_amount(cls, value: int) -> int:
        validate_positive_amount_minor(value)
        return value

    @field_validator("currency")
    @classmethod
    def check_currency(cls, value: str) -> str:
        return validate_currency(value)

    @field_validator("description")
    @classmethod
    def normalize_description(cls, value: str | None) -> str | None:
        if value is None:
            return None
        trimmed = value.strip()
        return trimmed or None


class TransactionUpdate(BaseModel):
    expected_version: int = Field(ge=1)
    type: TransactionType | None = None
    amount_minor: int | None = None
    currency: str | None = None
    category_id: UUID | None = None
    description: str | None = Field(default=None, max_length=MAX_DESCRIPTION_LENGTH)
    transaction_date: date | None = None

    @field_validator("amount_minor")
    @classmethod
    def check_amount(cls, value: int | None) -> int | None:
        if value is None:
            return None
        validate_positive_amount_minor(value)
        return value

    @field_validator("currency")
    @classmethod
    def check_currency(cls, value: str | None) -> str | None:
        if value is None:
            return None
        return validate_currency(value)

    @field_validator("description")
    @classmethod
    def normalize_description(cls, value: str | None) -> str | None:
        if value is None:
            return None
        trimmed = value.strip()
        return trimmed or None


class TransactionDelete(BaseModel):
    expected_version: int = Field(ge=1)


class TransactionRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    user_id: UUID
    type: TransactionType
    amount_minor: int
    currency: str
    category_id: UUID
    description: str | None
    transaction_date: date
    version: int
    deleted_at: datetime | None
    created_at: datetime
    updated_at: datetime
