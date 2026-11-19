from __future__ import annotations

import logging
import time
from dataclasses import dataclass
from typing import Any
from uuid import UUID

import httpx
import jwt
from fastapi import HTTPException, status
from jwt import PyJWKClient

from app.core.config import Settings

logger = logging.getLogger(__name__)


@dataclass(frozen=True, slots=True)
class AuthenticatedPrincipal:
    """Identity derived solely from a verified access token."""

    subject: UUID
    claims: dict[str, Any]


class TokenVerifier:
    """Verifies Supabase access JWTs via JWKS (or HS256 in APP_ENV=test)."""

    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._jwk_client: PyJWKClient | None = None

    def _get_jwk_client(self) -> PyJWKClient:
        if self._jwk_client is None:
            if not self._settings.supabase_jwks_url:
                raise HTTPException(
                    status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                    detail={
                        "code": "AUTH_MISCONFIGURED",
                        "message": "SUPABASE_JWKS_URL is not configured",
                    },
                )
            self._jwk_client = PyJWKClient(
                self._settings.supabase_jwks_url,
                cache_keys=True,
                lifespan=3600,
            )
        return self._jwk_client

    def verify_access_token(self, token: str) -> AuthenticatedPrincipal:
        try:
            if self._settings.is_test and self._settings.jwt_test_secret:
                payload = jwt.decode(
                    token,
                    self._settings.jwt_test_secret,
                    algorithms=["HS256"],
                    audience=self._settings.supabase_jwt_audience,
                    issuer=self._settings.supabase_jwt_issuer,
                    options={"require": ["exp", "sub", "iss", "aud"]},
                )
            else:
                signing_key = self._get_jwk_client().get_signing_key_from_jwt(token)
                payload = jwt.decode(
                    token,
                    signing_key.key,
                    algorithms=["RS256", "ES256"],
                    audience=self._settings.supabase_jwt_audience,
                    issuer=self._settings.supabase_jwt_issuer,
                    options={"require": ["exp", "sub", "iss", "aud"]},
                )
        except jwt.ExpiredSignatureError as exc:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail={"code": "TOKEN_EXPIRED", "message": "Access token has expired"},
            ) from exc
        except jwt.InvalidAudienceError as exc:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail={"code": "INVALID_AUDIENCE", "message": "Access token audience is invalid"},
            ) from exc
        except jwt.InvalidIssuerError as exc:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail={"code": "INVALID_ISSUER", "message": "Access token issuer is invalid"},
            ) from exc
        except jwt.PyJWTError as exc:
            logger.info("JWT verification failed: %s", type(exc).__name__)
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail={"code": "INVALID_TOKEN", "message": "Access token is invalid"},
            ) from exc

        raw_sub = payload.get("sub")
        try:
            subject = UUID(str(raw_sub))
        except (TypeError, ValueError) as exc:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail={"code": "INVALID_SUBJECT", "message": "Access token subject is invalid"},
            ) from exc

        return AuthenticatedPrincipal(subject=subject, claims=payload)


async def jwks_reachable(settings: Settings, timeout_seconds: float = 2.0) -> bool:
    """Optional readiness helper; does not gate /health/ready in Stage 1."""
    if settings.is_test:
        return True
    if not settings.supabase_jwks_url:
        return False
    try:
        async with httpx.AsyncClient(timeout=timeout_seconds) as client:
            response = await client.get(settings.supabase_jwks_url)
            return response.status_code == 200
    except httpx.HTTPError:
        return False


def now_epoch() -> int:
    return int(time.time())
