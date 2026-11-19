from fastapi import APIRouter

from app.api.deps import CurrentPrincipal, Users
from app.schemas.user import UserProfileRead, UserProfileUpdate

router = APIRouter(prefix="/api/v1", tags=["me"])


@router.get("/me", response_model=UserProfileRead)
async def get_me(principal: CurrentPrincipal, users: Users) -> UserProfileRead:
    user = await users.get_or_provision(principal.subject)
    return UserProfileRead.model_validate(user)


@router.patch("/me", response_model=UserProfileRead)
async def patch_me(
    payload: UserProfileUpdate,
    principal: CurrentPrincipal,
    users: Users,
) -> UserProfileRead:
    user = await users.update_profile(principal.subject, payload)
    return UserProfileRead.model_validate(user)
