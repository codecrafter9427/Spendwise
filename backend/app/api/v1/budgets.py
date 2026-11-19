from fastapi import APIRouter, Path

from app.api.deps import CurrentPrincipal, DbSession
from app.schemas.budget import BudgetRead, BudgetUpsert
from app.services.budgets import BudgetService

router = APIRouter(prefix="/api/v1/budgets", tags=["budgets"])


@router.get("", response_model=list[BudgetRead])
async def list_budgets(
    principal: CurrentPrincipal,
    session: DbSession,
) -> list[BudgetRead]:
    service = BudgetService(session)
    rows = await service.list_budgets(principal.subject)
    return [BudgetRead.model_validate(row) for row in rows]


@router.put("/{year}/{month}", response_model=BudgetRead)
async def upsert_budget(
    payload: BudgetUpsert,
    principal: CurrentPrincipal,
    session: DbSession,
    year: int = Path(ge=1970, le=2100),
    month: int = Path(ge=1, le=12),
) -> BudgetRead:
    service = BudgetService(session)
    row = await service.upsert(principal.subject, year, month, payload)
    return BudgetRead.model_validate(row)
