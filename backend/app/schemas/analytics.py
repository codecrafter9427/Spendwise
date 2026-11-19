from datetime import date
from uuid import UUID

from pydantic import BaseModel

from app.domain.enums import TransactionType


class AnalyticsSummary(BaseModel):
    start_date: date
    end_date: date
    currency: str
    total_income_minor: int
    total_expense_minor: int
    net_minor: int
    budget_amount_minor: int | None = None
    budget_remaining_minor: int | None = None
    budget_utilization_percent: float | None = None


class CategorySpendItem(BaseModel):
    category_id: UUID
    category_name: str
    type: TransactionType
    amount_minor: int
    percent_of_total: float


class CategorySpendResponse(BaseModel):
    start_date: date
    end_date: date
    currency: str
    total_expense_minor: int
    items: list[CategorySpendItem]
