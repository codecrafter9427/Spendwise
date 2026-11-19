from collections.abc import AsyncGenerator
from typing import Annotated

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import Settings, get_settings
from app.core.security import AuthenticatedPrincipal, TokenVerifier
from app.db.session import get_db_session
from app.services.users import UserService

bearer_scheme = HTTPBearer(auto_error=False)


def get_token_verifier(settings: Annotated[Settings, Depends(get_settings)]) -> TokenVerifier:
    return TokenVerifier(settings)


async def get_current_principal(
    credentials: Annotated[HTTPAuthorizationCredentials | None, Depends(bearer_scheme)],
    verifier: Annotated[TokenVerifier, Depends(get_token_verifier)],
) -> AuthenticatedPrincipal:
    if credentials is None or credentials.scheme.lower() != "bearer":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={"code": "MISSING_TOKEN", "message": "Authorization Bearer token is required"},
        )
    return verifier.verify_access_token(credentials.credentials)


async def get_db(
    session: Annotated[AsyncSession, Depends(get_db_session)],
) -> AsyncGenerator[AsyncSession, None]:
    yield session


def get_user_service(
    session: Annotated[AsyncSession, Depends(get_db)],
) -> UserService:
    return UserService(session)


CurrentPrincipal = Annotated[AuthenticatedPrincipal, Depends(get_current_principal)]
DbSession = Annotated[AsyncSession, Depends(get_db)]
Users = Annotated[UserService, Depends(get_user_service)]
SettingsDep = Annotated[Settings, Depends(get_settings)]
