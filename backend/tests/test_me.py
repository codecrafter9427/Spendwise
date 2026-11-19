import uuid

import pytest
from httpx import AsyncClient
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.models.user import AppUser
from tests.conftest import auth_headers


@pytest.mark.asyncio
async def test_get_me_provisions_profile(client: AsyncClient) -> None:
    subject = uuid.uuid4()
    response = await client.get("/api/v1/me", headers=auth_headers(subject))
    assert response.status_code == 200
    data = response.json()
    assert data["id"] == str(subject)
    assert data["base_currency"] == "INR"
    assert data["timezone"] == "UTC"
    assert data["display_name"] is None
    assert "created_at" in data
    assert "updated_at" in data


@pytest.mark.asyncio
async def test_get_me_is_idempotent(client: AsyncClient) -> None:
    subject = uuid.uuid4()
    first = await client.get("/api/v1/me", headers=auth_headers(subject))
    second = await client.get("/api/v1/me", headers=auth_headers(subject))
    assert first.status_code == 200
    assert second.status_code == 200
    assert first.json()["id"] == second.json()["id"]
    assert first.json()["created_at"] == second.json()["created_at"]


@pytest.mark.asyncio
async def test_patch_me_updates_allowed_fields(client: AsyncClient) -> None:
    subject = uuid.uuid4()
    await client.get("/api/v1/me", headers=auth_headers(subject))
    response = await client.patch(
        "/api/v1/me",
        headers=auth_headers(subject),
        json={
            "display_name": "  Dins  ",
            "timezone": "Asia/Kolkata",
            "base_currency": "inr",
        },
    )
    assert response.status_code == 200
    data = response.json()
    assert data["display_name"] == "Dins"
    assert data["timezone"] == "Asia/Kolkata"
    assert data["base_currency"] == "INR"


@pytest.mark.asyncio
async def test_patch_me_rejects_unsupported_currency(client: AsyncClient) -> None:
    subject = uuid.uuid4()
    response = await client.patch(
        "/api/v1/me",
        headers=auth_headers(subject),
        json={"base_currency": "USD"},
    )
    assert response.status_code == 422
    assert response.json()["error"]["code"] == "VALIDATION_ERROR"


@pytest.mark.asyncio
async def test_user_isolation_profiles_are_separate(
    client: AsyncClient,
    session_factory: async_sessionmaker[AsyncSession],
) -> None:
    user_a = uuid.uuid4()
    user_b = uuid.uuid4()

    patch_a = await client.patch(
        "/api/v1/me",
        headers=auth_headers(user_a),
        json={"display_name": "Alice"},
    )
    patch_b = await client.patch(
        "/api/v1/me",
        headers=auth_headers(user_b),
        json={"display_name": "Bob"},
    )
    assert patch_a.status_code == 200
    assert patch_b.status_code == 200

    get_a = await client.get("/api/v1/me", headers=auth_headers(user_a))
    get_b = await client.get("/api/v1/me", headers=auth_headers(user_b))
    assert get_a.json()["id"] == str(user_a)
    assert get_a.json()["display_name"] == "Alice"
    assert get_b.json()["id"] == str(user_b)
    assert get_b.json()["display_name"] == "Bob"

    # Token for A never mutates B even if a foreign id is smuggled in JSON.
    smuggle = await client.patch(
        "/api/v1/me",
        headers=auth_headers(user_a),
        json={"display_name": "Alice Updated", "id": str(user_b), "user_id": str(user_b)},
    )
    assert smuggle.status_code == 200
    assert smuggle.json()["id"] == str(user_a)
    assert smuggle.json()["display_name"] == "Alice Updated"

    get_b_after = await client.get("/api/v1/me", headers=auth_headers(user_b))
    assert get_b_after.json()["display_name"] == "Bob"

    async with session_factory() as session:
        rows = (await session.execute(select(AppUser))).scalars().all()
        by_id = {row.id: row for row in rows}
        assert by_id[user_a].display_name == "Alice Updated"
        assert by_id[user_b].display_name == "Bob"
