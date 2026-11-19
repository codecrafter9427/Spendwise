from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.domain.money import validate_currency, validate_non_negative_amount_minor


class BudgetUpsert(BaseModel):
    amount_minor: int
    currency: str = "INR"
    expected_version: int | None = Field(default=None, ge=1)

    @field_validator("amount_minor")
    @classmethod
    def check_amount(cls, value: int) -> int:
        validate_non_negative_amount_minor(value)
        return value

    @field_validator("currency")
    @classmethod
    def check_currency(cls, value: str) -> str:
        return validate_currency(value)


class BudgetRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    user_id: UUID
    year: int
    month: int
    amount_minor: int
    currency: str
    version: int
    created_at: datetime
    updated_at: datetime
