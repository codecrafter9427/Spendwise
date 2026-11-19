from uuid import UUID

from fastapi import APIRouter, Query

from app.api.deps import CurrentPrincipal, DbSession
from app.domain.enums import TransactionType
from app.schemas.category import CategoryCreate, CategoryRead, CategoryUpdate
from app.services.categories import CategoryService

router = APIRouter(prefix="/api/v1/categories", tags=["categories"])


@router.get("", response_model=list[CategoryRead])
async def list_categories(
    principal: CurrentPrincipal,
    session: DbSession,
    type: TransactionType | None = Query(default=None),
    include_archived: bool = Query(default=False),
) -> list[CategoryRead]:
    service = CategoryService(session)
    rows = await service.list_categories(
        principal.subject,
        type_filter=type.value if type else None,
        include_archived=include_archived,
    )
    return [CategoryRead.model_validate(row) for row in rows]


@router.post("", response_model=CategoryRead, status_code=201)
async def create_category(
    payload: CategoryCreate,
    principal: CurrentPrincipal,
    session: DbSession,
) -> CategoryRead:
    service = CategoryService(session)
    row = await service.create(principal.subject, payload)
    return CategoryRead.model_validate(row)


@router.patch("/{category_id}", response_model=CategoryRead)
async def update_category(
    category_id: UUID,
    payload: CategoryUpdate,
    principal: CurrentPrincipal,
    session: DbSession,
) -> CategoryRead:
    service = CategoryService(session)
    row = await service.update(principal.subject, category_id, payload)
    return CategoryRead.model_validate(row)
