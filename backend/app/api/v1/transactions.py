from datetime import date
from uuid import UUID

from fastapi import APIRouter, Query

from app.api.deps import CurrentPrincipal, DbSession
from app.domain.enums import TransactionType
from app.schemas.common import CursorPage
from app.schemas.transaction import TransactionCreate, TransactionRead, TransactionUpdate
from app.services.transactions import TransactionService

router = APIRouter(prefix="/api/v1/transactions", tags=["transactions"])


@router.get("", response_model=CursorPage[TransactionRead])
async def list_transactions(
    principal: CurrentPrincipal,
    session: DbSession,
    limit: int = Query(default=50, ge=1, le=100),
    cursor: str | None = Query(default=None),
    start_date: date | None = Query(default=None),
    end_date: date | None = Query(default=None),
    type: TransactionType | None = Query(default=None),
    category_id: UUID | None = Query(default=None),
    q: str | None = Query(default=None),
) -> CursorPage[TransactionRead]:
    service = TransactionService(session)
    rows, next_cursor = await service.list_page(
        principal.subject,
        limit=limit,
        cursor=cursor,
        start_date=start_date,
        end_date=end_date,
        type_filter=type.value if type else None,
        category_id=category_id,
        query=q,
    )
    return CursorPage(
        items=[TransactionRead.model_validate(row) for row in rows],
        next_cursor=next_cursor,
        limit=limit,
    )


@router.post("", response_model=TransactionRead, status_code=201)
async def create_transaction(
    payload: TransactionCreate,
    principal: CurrentPrincipal,
    session: DbSession,
) -> TransactionRead:
    service = TransactionService(session)
    row = await service.create(principal.subject, payload)
    return TransactionRead.model_validate(row)


@router.get("/{transaction_id}", response_model=TransactionRead)
async def get_transaction(
    transaction_id: UUID,
    principal: CurrentPrincipal,
    session: DbSession,
) -> TransactionRead:
    service = TransactionService(session)
    row = await service.get(principal.subject, transaction_id)
    return TransactionRead.model_validate(row)


@router.patch("/{transaction_id}", response_model=TransactionRead)
async def update_transaction(
    transaction_id: UUID,
    payload: TransactionUpdate,
    principal: CurrentPrincipal,
    session: DbSession,
) -> TransactionRead:
    service = TransactionService(session)
    row = await service.update(principal.subject, transaction_id, payload)
    return TransactionRead.model_validate(row)


@router.delete("/{transaction_id}", response_model=TransactionRead)
async def delete_transaction(
    transaction_id: UUID,
    principal: CurrentPrincipal,
    session: DbSession,
    expected_version: int = Query(ge=1),
) -> TransactionRead:
    service = TransactionService(session)
    row = await service.soft_delete(
        principal.subject,
        transaction_id,
        expected_version,
    )
    return TransactionRead.model_validate(row)
