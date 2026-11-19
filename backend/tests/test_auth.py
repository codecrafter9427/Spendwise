import uuid
from datetime import UTC, datetime, timedelta

import jwt
import pytest
from httpx import AsyncClient

from tests.conftest import JWT_AUDIENCE, JWT_ISSUER, JWT_SECRET, auth_headers, make_access_token


@pytest.mark.asyncio
async def test_me_requires_bearer_token(client: AsyncClient) -> None:
    response = await client.get("/api/v1/me")
    assert response.status_code == 401
    body = response.json()
    assert body["error"]["code"] == "MISSING_TOKEN"


@pytest.mark.asyncio
async def test_me_rejects_malformed_token(client: AsyncClient) -> None:
    response = await client.get(
        "/api/v1/me",
        headers={"Authorization": "Bearer not-a-jwt"},
    )
    assert response.status_code == 401
    assert response.json()["error"]["code"] == "INVALID_TOKEN"


@pytest.mark.asyncio
async def test_me_rejects_expired_token(client: AsyncClient) -> None:
    subject = uuid.uuid4()
    now = datetime.now(UTC)
    token = jwt.encode(
        {
            "sub": str(subject),
            "iss": JWT_ISSUER,
            "aud": JWT_AUDIENCE,
            "iat": int((now - timedelta(hours=2)).timestamp()),
            "exp": int((now - timedelta(hours=1)).timestamp()),
        },
        JWT_SECRET,
        algorithm="HS256",
    )
    response = await client.get("/api/v1/me", headers={"Authorization": f"Bearer {token}"})
    assert response.status_code == 401
    assert response.json()["error"]["code"] == "TOKEN_EXPIRED"


@pytest.mark.asyncio
async def test_me_rejects_wrong_issuer(client: AsyncClient) -> None:
    subject = uuid.uuid4()
    token = make_access_token(subject, issuer="https://evil.example/auth/v1")
    response = await client.get("/api/v1/me", headers={"Authorization": f"Bearer {token}"})
    assert response.status_code == 401
    assert response.json()["error"]["code"] == "INVALID_ISSUER"


@pytest.mark.asyncio
async def test_me_rejects_wrong_audience(client: AsyncClient) -> None:
    subject = uuid.uuid4()
    token = make_access_token(subject, audience="anon")
    response = await client.get("/api/v1/me", headers={"Authorization": f"Bearer {token}"})
    assert response.status_code == 401
    assert response.json()["error"]["code"] == "INVALID_AUDIENCE"


@pytest.mark.asyncio
async def test_me_rejects_wrong_signature(client: AsyncClient) -> None:
    subject = uuid.uuid4()
    token = make_access_token(subject, secret="wrong-secret-not-the-test-secret!!")
    response = await client.get("/api/v1/me", headers={"Authorization": f"Bearer {token}"})
    assert response.status_code == 401
    assert response.json()["error"]["code"] == "INVALID_TOKEN"


@pytest.mark.asyncio
async def test_valid_token_maps_to_subject(client: AsyncClient) -> None:
    subject = uuid.uuid4()
    response = await client.get("/api/v1/me", headers=auth_headers(subject))
    assert response.status_code == 200
    assert response.json()["id"] == str(subject)
