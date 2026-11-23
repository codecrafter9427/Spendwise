from __future__ import annotations

import asyncio
from collections import defaultdict
from dataclasses import asdict, dataclass
from typing import Any
from uuid import UUID


@dataclass(frozen=True, slots=True)
class RealtimeEvent:
    """Minimal wake payload; clients must pull authoritative data via sync/API."""

    type: str
    entity_type: str
    entity_id: str
    operation: str
    version: int
    cursor: str

    def as_dict(self) -> dict[str, Any]:
        return asdict(self)


class RealtimeHub:
    """In-process fan-out for Stage 6. Best-effort; missed events are repaired by pull."""

    def __init__(self) -> None:
        self._subscribers: dict[UUID, set[asyncio.Queue[RealtimeEvent]]] = defaultdict(set)
        self._lock = asyncio.Lock()

    async def subscribe(self, user_id: UUID) -> asyncio.Queue[RealtimeEvent]:
        queue: asyncio.Queue[RealtimeEvent] = asyncio.Queue(maxsize=64)
        async with self._lock:
            self._subscribers[user_id].add(queue)
        return queue

    async def unsubscribe(self, user_id: UUID, queue: asyncio.Queue[RealtimeEvent]) -> None:
        async with self._lock:
            subscribers = self._subscribers.get(user_id)
            if not subscribers:
                return
            subscribers.discard(queue)
            if not subscribers:
                self._subscribers.pop(user_id, None)

    async def publish(self, user_id: UUID, event: RealtimeEvent) -> None:
        async with self._lock:
            subscribers = list(self._subscribers.get(user_id, set()))
        for queue in subscribers:
            try:
                queue.put_nowait(event)
            except asyncio.QueueFull:
                # Drop oldest wake if client is slow; pull will catch up.
                try:
                    _ = queue.get_nowait()
                except asyncio.QueueEmpty:
                    pass
                try:
                    queue.put_nowait(event)
                except asyncio.QueueFull:
                    pass


realtime_hub = RealtimeHub()
