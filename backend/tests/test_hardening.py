from __future__ import annotations

import uuid

import pytest
from httpx import AsyncClient

from app.domain.default_categories import default_category_by_android_id
from tests.conftest import auth_headers


def _expense_category_id() -> uuid.UUID:
    seed = default_category_by_android_id(1)
    assert seed is not None
    return seed.id


@pytest.mark.asyncio
async def test_security_headers_present(client: AsyncClient) -> None:
    response = await client.get("/health/live")
    assert response.status_code == 200
    assert response.headers.get("X-Content-Type-Options") == "nosniff"
    assert response.headers.get("X-Frame-Options") == "DENY"
    assert response.headers.get("X-Request-ID")


@pytest.mark.asyncio
async def test_payload_too_large_rejected(client: AsyncClient) -> None:
    # Default MAX_REQUEST_BODY_BYTES is 1 MiB; httpx sets Content-Length from body size.
    oversized = b'{"display_name":"' + (b"a" * 1_100_000) + b'"}'
    response = await client.post(
        "/api/v1/me",
        headers=auth_headers(uuid.uuid4()),
        content=oversized,
    )
    assert response.status_code == 413
    assert response.json()["error"]["code"] == "PAYLOAD_TOO_LARGE"


@pytest.mark.asyncio
async def test_export_and_delete_account(client: AsyncClient) -> None:
    user = uuid.uuid4()
    headers = auth_headers(user)

    await client.post(
        "/api/v1/transactions",
        headers=headers,
        json={
            "id": str(uuid.uuid4()),
            "type": "expense",
            "amount_minor": 4500,
            "category_id": str(_expense_category_id()),
            "description": "Export me",
            "transaction_date": "2026-11-23",
        },
    )
    await client.put(
        "/api/v1/budgets/2026/11",
        headers=headers,
        json={"amount_minor": 10_000_00},
    )

    exported = await client.get("/api/v1/account/export", headers=headers)
    assert exported.status_code == 200
    body = exported.json()
    assert body["profile"]["id"] == str(user)
    assert len(body["transactions"]) >= 1
    assert len(body["budgets"]) >= 1

    deleted = await client.delete("/api/v1/account", headers=headers)
    assert deleted.status_code == 200
    assert deleted.json()["status"] == "deleted"
    assert deleted.json()["deleted"]["transactions"] >= 1

    me = await client.get("/api/v1/me", headers=headers)
    assert me.status_code == 200
    assert me.json()["id"] == str(user)

    exported_again = await client.get("/api/v1/account/export", headers=headers)
    assert exported_again.status_code == 200
    assert exported_again.json()["transactions"] == []
