from __future__ import annotations

import asyncio
import json
from collections.abc import AsyncIterator

from fastapi import APIRouter, Query
from fastapi.responses import StreamingResponse

from app.api.deps import CurrentPrincipal
from app.db.session import get_session_factory
from app.services.realtime import realtime_hub
from app.services.users import UserService

router = APIRouter(prefix="/api/v1/realtime", tags=["realtime"])


@router.get("/events")
async def realtime_events(
    principal: CurrentPrincipal,
    once: bool = Query(
        default=False,
        description="If true, send ready and close (test/helper only).",
    ),
) -> StreamingResponse:
    # Provision in a short-lived session so SSE does not hold a DB connection open.
    factory = get_session_factory()
    async with factory() as session:
        await UserService(session).get_or_provision(principal.subject)
        await session.commit()

    user_id = principal.subject

    async def event_stream() -> AsyncIterator[str]:
        queue = await realtime_hub.subscribe(user_id)
        try:
            ready = {"type": "ready", "user_id": str(user_id)}
            yield f"event: ready\ndata: {json.dumps(ready)}\n\n"
            if once:
                return
            while True:
                try:
                    event = await asyncio.wait_for(queue.get(), timeout=20)
                    payload = json.dumps(event.as_dict())
                    yield f"event: sync\ndata: {payload}\n\n"
                except asyncio.TimeoutError:
                    yield ": keepalive\n\n"
        finally:
            await realtime_hub.unsubscribe(user_id, queue)

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )
