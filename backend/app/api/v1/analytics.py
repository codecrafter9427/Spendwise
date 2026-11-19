from fastapi import APIRouter, Query

from app.api.deps import CurrentPrincipal, DbSession
from app.schemas.analytics import AnalyticsSummary, CategorySpendResponse
from app.services.analytics import AnalyticsService

router = APIRouter(prefix="/api/v1/analytics", tags=["analytics"])


@router.get("/summary", response_model=AnalyticsSummary)
async def analytics_summary(
    principal: CurrentPrincipal,
    session: DbSession,
    year: int = Query(ge=1970, le=2100),
    month: int = Query(ge=1, le=12),
    currency: str = Query(default="INR", min_length=3, max_length=3),
) -> AnalyticsSummary:
    service = AnalyticsService(session)
    return await service.summary(
        principal.subject,
        year=year,
        month=month,
        currency=currency.upper(),
    )


@router.get("/categories", response_model=CategorySpendResponse)
async def analytics_categories(
    principal: CurrentPrincipal,
    session: DbSession,
    year: int = Query(ge=1970, le=2100),
    month: int = Query(ge=1, le=12),
    currency: str = Query(default="INR", min_length=3, max_length=3),
) -> CategorySpendResponse:
    service = AnalyticsService(session)
    return await service.category_spend(
        principal.subject,
        year=year,
        month=month,
        currency=currency.upper(),
    )
