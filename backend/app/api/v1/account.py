from fastapi import APIRouter

from app.api.deps import CurrentPrincipal, DbSession
from app.services.account import AccountService

router = APIRouter(prefix="/api/v1/account", tags=["account"])


@router.get("/export")
async def export_account(
    principal: CurrentPrincipal,
    session: DbSession,
) -> dict:
    service = AccountService(session)
    return await service.export(principal.subject)


@router.delete("")
async def delete_account(
    principal: CurrentPrincipal,
    session: DbSession,
) -> dict:
    service = AccountService(session)
    deleted = await service.delete_account(principal.subject)
    return {"status": "deleted", "deleted": deleted}
