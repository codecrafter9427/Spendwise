from fastapi import APIRouter, Query

from app.api.deps import CurrentPrincipal, DbSession
from app.schemas.sync import SyncPullResponse, SyncPushRequest, SyncPushResponse
from app.services.sync import SyncService

router = APIRouter(prefix="/api/v1/sync", tags=["sync"])


@router.get("/pull", response_model=SyncPullResponse)
async def sync_pull(
    principal: CurrentPrincipal,
    session: DbSession,
    cursor: str | None = Query(default=None),
    limit: int = Query(default=100, ge=1, le=200),
) -> SyncPullResponse:
    service = SyncService(session)
    return await service.pull(principal.subject, cursor=cursor, limit=limit)


@router.post("/push", response_model=SyncPushResponse)
async def sync_push(
    payload: SyncPushRequest,
    principal: CurrentPrincipal,
    session: DbSession,
) -> SyncPushResponse:
    service = SyncService(session)
    return await service.push(principal.subject, payload)
