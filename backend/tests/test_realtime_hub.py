from __future__ import annotations

import asyncio
import uuid

import pytest

from app.services.realtime import RealtimeEvent, RealtimeHub


@pytest.mark.asyncio
async def test_realtime_hub_fanout_and_unsubscribe() -> None:
    hub = RealtimeHub()
    user = uuid.uuid4()
    other = uuid.uuid4()

    q1 = await hub.subscribe(user)
    q2 = await hub.subscribe(user)
    q_other = await hub.subscribe(other)

    event = RealtimeEvent(
        type="sync",
        entity_type="transaction",
        entity_id=str(uuid.uuid4()),
        operation="upsert",
        version=1,
        cursor="42",
    )
    await hub.publish(user, event)

    assert q1.get_nowait() == event
    assert q2.get_nowait() == event
    with pytest.raises(asyncio.QueueEmpty):
        q_other.get_nowait()

    await hub.unsubscribe(user, q1)
    await hub.publish(user, event)
    with pytest.raises(asyncio.QueueEmpty):
        q1.get_nowait()
    assert q2.get_nowait() == event
