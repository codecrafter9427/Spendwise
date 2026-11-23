from __future__ import annotations

import uuid
from datetime import date

import pytest
from httpx import AsyncClient

from app.domain.default_categories import default_category_by_android_id
from tests.conftest import auth_headers


def _expense_category_id() -> uuid.UUID:
    seed = default_category_by_android_id(1)
    assert seed is not None
    return seed.id


@pytest.mark.asyncio
async def test_ai_requires_consent_and_answers_from_tools(client: AsyncClient) -> None:
    user = uuid.uuid4()
    headers = auth_headers(user)

    denied = await client.post(
        "/api/v1/ai/chat",
        headers=headers,
        json={"message": "How much did I spend this month?", "year": 2026, "month": 11},
    )
    assert denied.status_code == 403
    assert denied.json()["error"]["code"] == "AI_CONSENT_REQUIRED"

    enabled = await client.patch(
        "/api/v1/me",
        headers=headers,
        json={"ai_enabled": True},
    )
    assert enabled.status_code == 200
    assert enabled.json()["ai_enabled"] is True

    await client.post(
        "/api/v1/transactions",
        headers=headers,
        json={
            "id": str(uuid.uuid4()),
            "type": "expense",
            "amount_minor": 12_500,
            "category_id": str(_expense_category_id()),
            "description": "Lunch",
            "transaction_date": "2026-11-10",
        },
    )
    await client.put(
        "/api/v1/budgets/2026/11",
        headers=headers,
        json={"amount_minor": 50_000_00},
    )

    chat = await client.post(
        "/api/v1/ai/chat",
        headers=headers,
        json={"message": "What is my budget status?", "year": 2026, "month": 11},
    )
    assert chat.status_code == 200
    body = chat.json()
    assert body["mode"] == "deterministic"
    assert body["tools_used"][0]["name"] == "budget_status"
    assert "₹" in body["answer"] or "budget" in body["answer"].lower()
    assert "not investment" in body["disclaimer"].lower() or "advice" in body["disclaimer"].lower()

    blocked = await client.post(
        "/api/v1/ai/chat",
        headers=headers,
        json={"message": "Which stocks should I invest in?"},
    )
    assert blocked.status_code == 422
    assert blocked.json()["error"]["code"] == "UNSUPPORTED_REQUEST"


@pytest.mark.asyncio
async def test_ai_isolation(client: AsyncClient) -> None:
    owner = uuid.uuid4()
    other = uuid.uuid4()
    await client.patch("/api/v1/me", headers=auth_headers(owner), json={"ai_enabled": True})
    await client.patch("/api/v1/me", headers=auth_headers(other), json={"ai_enabled": True})

    await client.post(
        "/api/v1/transactions",
        headers=auth_headers(owner),
        json={
            "id": str(uuid.uuid4()),
            "type": "expense",
            "amount_minor": 99_00,
            "category_id": str(_expense_category_id()),
            "transaction_date": date.today().isoformat(),
        },
    )

    today = date.today()
    other_chat = await client.post(
        "/api/v1/ai/chat",
        headers=auth_headers(other),
        json={"message": "Monthly summary", "year": today.year, "month": today.month},
    )
    assert other_chat.status_code == 200
    result = other_chat.json()["tools_used"][0]["result"]
    assert result["total_expense_minor"] == 0
