from __future__ import annotations

import uuid

import pytest
from httpx import AsyncClient

from app.services.realtime import RealtimeEvent, realtime_hub
from tests.conftest import auth_headers


@pytest.mark.asyncio
async def test_realtime_requires_auth(client: AsyncClient) -> None:
    response = await client.get("/api/v1/realtime/events")
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_realtime_ready_event(client: AsyncClient) -> None:
    user = uuid.uuid4()
    headers = auth_headers(user)
    # once=1 closes after ready so the ASGI stream can finish cleanly in tests.
    response = await client.get("/api/v1/realtime/events?once=true", headers=headers)
    assert response.status_code == 200
    body = response.text
    assert "event: ready" in body
    assert str(user) in body


@pytest.mark.asyncio
async def test_realtime_hub_delivers_sync_event_to_subscriber() -> None:
    user = uuid.uuid4()
    queue = await realtime_hub.subscribe(user)
    event = RealtimeEvent(
        type="sync",
        entity_type="budget",
        entity_id=str(uuid.uuid4()),
        operation="upsert",
        version=2,
        cursor="9",
    )
    await realtime_hub.publish(user, event)
    assert queue.get_nowait() == event
    await realtime_hub.unsubscribe(user, queue)
