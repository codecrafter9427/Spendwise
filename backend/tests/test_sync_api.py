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
async def test_sync_push_pull_and_idempotency(client: AsyncClient) -> None:
    user = uuid.uuid4()
    tx_id = uuid.uuid4()
    op_id = str(uuid.uuid4())

    push = await client.post(
        "/api/v1/sync/push",
        headers=auth_headers(user),
        json={
            "operations": [
                {
                    "operation_id": op_id,
                    "entity_type": "transaction",
                    "entity_id": str(tx_id),
                    "operation": "upsert",
                    "payload": {
                        "type": "expense",
                        "amount_minor": 1500,
                        "category_id": str(_expense_category_id()),
                        "description": "Sync coffee",
                        "transaction_date": "2026-11-19",
                    },
                }
            ]
        },
    )
    assert push.status_code == 200
    assert push.json()["results"][0]["status"] == "accepted"
    assert push.json()["results"][0]["version"] == 1

    replay = await client.post(
        "/api/v1/sync/push",
        headers=auth_headers(user),
        json={
            "operations": [
                {
                    "operation_id": op_id,
                    "entity_type": "transaction",
                    "entity_id": str(tx_id),
                    "operation": "upsert",
                    "payload": {
                        "type": "expense",
                        "amount_minor": 1500,
                        "category_id": str(_expense_category_id()),
                        "description": "Sync coffee",
                        "transaction_date": "2026-11-19",
                    },
                }
            ]
        },
    )
    assert replay.status_code == 200
    assert replay.json()["results"][0]["status"] == "duplicate"

    mismatch = await client.post(
        "/api/v1/sync/push",
        headers=auth_headers(user),
        json={
            "operations": [
                {
                    "operation_id": op_id,
                    "entity_type": "transaction",
                    "entity_id": str(tx_id),
                    "operation": "upsert",
                    "payload": {
                        "type": "expense",
                        "amount_minor": 9999,
                        "category_id": str(_expense_category_id()),
                        "transaction_date": "2026-11-19",
                    },
                }
            ]
        },
    )
    assert mismatch.status_code == 200
    assert mismatch.json()["results"][0]["status"] == "conflict"

    pull = await client.get("/api/v1/sync/pull", headers=auth_headers(user))
    assert pull.status_code == 200
    changes = pull.json()["changes"]
    assert len(changes) >= 1
    assert changes[0]["entity_type"] == "transaction"
    assert changes[0]["entity_id"] == str(tx_id)
    assert pull.json()["next_cursor"] is not None

    other = uuid.uuid4()
    isolated = await client.get("/api/v1/sync/pull", headers=auth_headers(other))
    assert isolated.status_code == 200
    assert isolated.json()["changes"] == []
