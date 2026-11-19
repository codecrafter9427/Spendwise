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


def _income_category_id() -> uuid.UUID:
    seed = default_category_by_android_id(101)
    assert seed is not None
    return seed.id


@pytest.mark.asyncio
async def test_list_default_categories(client: AsyncClient) -> None:
    user = uuid.uuid4()
    response = await client.get("/api/v1/categories", headers=auth_headers(user))
    assert response.status_code == 200
    rows = response.json()
    assert len(rows) == 18
    assert all(row["is_default"] for row in rows)
    assert all(row["user_id"] is None for row in rows)


@pytest.mark.asyncio
async def test_create_custom_category_and_forbid_default_edit(
    client: AsyncClient,
) -> None:
    user = uuid.uuid4()
    created = await client.post(
        "/api/v1/categories",
        headers=auth_headers(user),
        json={"name": "Pets", "type": "expense", "icon_key": "pets"},
    )
    assert created.status_code == 201
    custom_id = created.json()["id"]

    patched = await client.patch(
        f"/api/v1/categories/{custom_id}",
        headers=auth_headers(user),
        json={"name": "Pet Care"},
    )
    assert patched.status_code == 200
    assert patched.json()["name"] == "Pet Care"

    default_id = str(_expense_category_id())
    forbidden = await client.patch(
        f"/api/v1/categories/{default_id}",
        headers=auth_headers(user),
        json={"name": "Hack"},
    )
    assert forbidden.status_code == 403
    assert forbidden.json()["error"]["code"] == "DEFAULT_CATEGORY_IMMUTABLE"


@pytest.mark.asyncio
async def test_transaction_crud_version_and_soft_delete(client: AsyncClient) -> None:
    user = uuid.uuid4()
    tx_id = uuid.uuid4()
    create = await client.post(
        "/api/v1/transactions",
        headers=auth_headers(user),
        json={
            "id": str(tx_id),
            "type": "expense",
            "amount_minor": 12500,
            "currency": "INR",
            "category_id": str(_expense_category_id()),
            "description": "Groceries",
            "transaction_date": "2026-11-14",
        },
    )
    assert create.status_code == 201
    assert create.json()["amount_minor"] == 12500
    assert create.json()["version"] == 1

    replay = await client.post(
        "/api/v1/transactions",
        headers=auth_headers(user),
        json={
            "id": str(tx_id),
            "type": "expense",
            "amount_minor": 12500,
            "currency": "INR",
            "category_id": str(_expense_category_id()),
            "description": "Groceries",
            "transaction_date": "2026-11-14",
        },
    )
    assert replay.status_code == 201
    assert replay.json()["id"] == str(tx_id)

    stale = await client.patch(
        f"/api/v1/transactions/{tx_id}",
        headers=auth_headers(user),
        json={"expected_version": 99, "description": "Nope"},
    )
    assert stale.status_code == 409
    assert stale.json()["error"]["code"] == "VERSION_CONFLICT"

    updated = await client.patch(
        f"/api/v1/transactions/{tx_id}",
        headers=auth_headers(user),
        json={"expected_version": 1, "description": "Weekly groceries"},
    )
    assert updated.status_code == 200
    assert updated.json()["version"] == 2
    assert updated.json()["description"] == "Weekly groceries"

    deleted = await client.delete(
        f"/api/v1/transactions/{tx_id}",
        headers=auth_headers(user),
        params={"expected_version": 2},
    )
    assert deleted.status_code == 200
    assert deleted.json()["deleted_at"] is not None

    missing = await client.get(
        f"/api/v1/transactions/{tx_id}",
        headers=auth_headers(user),
    )
    assert missing.status_code == 404


@pytest.mark.asyncio
async def test_transaction_rejects_category_type_mismatch_and_floatish_rules(
    client: AsyncClient,
) -> None:
    user = uuid.uuid4()
    mismatch = await client.post(
        "/api/v1/transactions",
        headers=auth_headers(user),
        json={
            "id": str(uuid.uuid4()),
            "type": "expense",
            "amount_minor": 100,
            "category_id": str(_income_category_id()),
            "transaction_date": "2026-11-01",
        },
    )
    assert mismatch.status_code == 422
    assert mismatch.json()["error"]["code"] == "CATEGORY_TYPE_MISMATCH"

    zero = await client.post(
        "/api/v1/transactions",
        headers=auth_headers(user),
        json={
            "id": str(uuid.uuid4()),
            "type": "expense",
            "amount_minor": 0,
            "category_id": str(_expense_category_id()),
            "transaction_date": "2026-11-01",
        },
    )
    assert zero.status_code == 422


@pytest.mark.asyncio
async def test_user_isolation_for_transactions_and_budgets(client: AsyncClient) -> None:
    user_a = uuid.uuid4()
    user_b = uuid.uuid4()
    tx_id = uuid.uuid4()

    created = await client.post(
        "/api/v1/transactions",
        headers=auth_headers(user_a),
        json={
            "id": str(tx_id),
            "type": "expense",
            "amount_minor": 5000,
            "category_id": str(_expense_category_id()),
            "transaction_date": "2026-11-10",
        },
    )
    assert created.status_code == 201

    other_get = await client.get(
        f"/api/v1/transactions/{tx_id}",
        headers=auth_headers(user_b),
    )
    assert other_get.status_code == 404

    await client.put(
        "/api/v1/budgets/2026/11",
        headers=auth_headers(user_a),
        json={"amount_minor": 500000},
    )
    await client.put(
        "/api/v1/budgets/2026/11",
        headers=auth_headers(user_b),
        json={"amount_minor": 100000},
    )
    budgets_a = await client.get("/api/v1/budgets", headers=auth_headers(user_a))
    budgets_b = await client.get("/api/v1/budgets", headers=auth_headers(user_b))
    assert budgets_a.json()[0]["amount_minor"] == 500000
    assert budgets_b.json()[0]["amount_minor"] == 100000


@pytest.mark.asyncio
async def test_budget_and_analytics_summary(client: AsyncClient) -> None:
    user = uuid.uuid4()
    await client.put(
        "/api/v1/budgets/2026/11",
        headers=auth_headers(user),
        json={"amount_minor": 100000},
    )
    await client.post(
        "/api/v1/transactions",
        headers=auth_headers(user),
        json={
            "id": str(uuid.uuid4()),
            "type": "income",
            "amount_minor": 200000,
            "category_id": str(_income_category_id()),
            "transaction_date": "2026-11-01",
        },
    )
    await client.post(
        "/api/v1/transactions",
        headers=auth_headers(user),
        json={
            "id": str(uuid.uuid4()),
            "type": "expense",
            "amount_minor": 25000,
            "category_id": str(_expense_category_id()),
            "description": "Lunch",
            "transaction_date": "2026-11-05",
        },
    )

    summary = await client.get(
        "/api/v1/analytics/summary",
        headers=auth_headers(user),
        params={"year": 2026, "month": 11},
    )
    assert summary.status_code == 200
    body = summary.json()
    assert body["total_income_minor"] == 200000
    assert body["total_expense_minor"] == 25000
    assert body["net_minor"] == 175000
    assert body["budget_amount_minor"] == 100000
    assert body["budget_remaining_minor"] == 75000
    assert body["start_date"] == "2026-11-01"
    assert body["end_date"] == "2026-11-30"

    categories = await client.get(
        "/api/v1/analytics/categories",
        headers=auth_headers(user),
        params={"year": 2026, "month": 11},
    )
    assert categories.status_code == 200
    cat_body = categories.json()
    assert cat_body["total_expense_minor"] == 25000
    assert len(cat_body["items"]) == 1
    assert cat_body["items"][0]["category_name"] == "Food & Dining"
    assert cat_body["items"][0]["percent_of_total"] == 100.0


@pytest.mark.asyncio
async def test_transaction_list_filter_and_pagination(client: AsyncClient) -> None:
    user = uuid.uuid4()
    for day, amount in [(1, 1000), (2, 2000), (3, 3000)]:
        await client.post(
            "/api/v1/transactions",
            headers=auth_headers(user),
            json={
                "id": str(uuid.uuid4()),
                "type": "expense",
                "amount_minor": amount,
                "category_id": str(_expense_category_id()),
                "description": f"Day {day}",
                "transaction_date": date(2026, 11, day).isoformat(),
            },
        )

    page1 = await client.get(
        "/api/v1/transactions",
        headers=auth_headers(user),
        params={"limit": 2, "start_date": "2026-11-01", "end_date": "2026-11-30"},
    )
    assert page1.status_code == 200
    assert len(page1.json()["items"]) == 2
    assert page1.json()["next_cursor"] is not None

    page2 = await client.get(
        "/api/v1/transactions",
        headers=auth_headers(user),
        params={
            "limit": 2,
            "cursor": page1.json()["next_cursor"],
            "start_date": "2026-11-01",
            "end_date": "2026-11-30",
        },
    )
    assert page2.status_code == 200
    assert len(page2.json()["items"]) == 1
    assert page2.json()["next_cursor"] is None
