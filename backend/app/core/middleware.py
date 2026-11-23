from __future__ import annotations

import logging
import time
import uuid
from collections import defaultdict, deque
from typing import Callable

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import JSONResponse, Response

from app.core.config import Settings

logger = logging.getLogger("spendwise.request")


class RequestContextMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        request_id = request.headers.get("X-Request-ID") or str(uuid.uuid4())
        request.state.request_id = request_id
        started = time.perf_counter()
        response = await call_next(request)
        elapsed_ms = (time.perf_counter() - started) * 1000
        response.headers["X-Request-ID"] = request_id
        logger.info(
            "request_id=%s method=%s path=%s status=%s duration_ms=%.1f",
            request_id,
            request.method,
            request.url.path,
            response.status_code,
            elapsed_ms,
        )
        return response


class SecurityHeadersMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        response = await call_next(request)
        response.headers.setdefault("X-Content-Type-Options", "nosniff")
        response.headers.setdefault("X-Frame-Options", "DENY")
        response.headers.setdefault("Referrer-Policy", "no-referrer")
        response.headers.setdefault(
            "Permissions-Policy",
            "geolocation=(), microphone=(), camera=()",
        )
        response.headers.setdefault("Cache-Control", "no-store")
        return response


class BodySizeLimitMiddleware(BaseHTTPMiddleware):
    def __init__(self, app, max_body_bytes: int) -> None:
        super().__init__(app)
        self._max_body_bytes = max_body_bytes

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        content_length = request.headers.get("content-length")
        if content_length is not None:
            try:
                if int(content_length) > self._max_body_bytes:
                    return JSONResponse(
                        status_code=413,
                        content={
                            "error": {
                                "code": "PAYLOAD_TOO_LARGE",
                                "message": "Request body exceeds configured limit",
                                "details": {"max_bytes": self._max_body_bytes},
                            }
                        },
                    )
            except ValueError:
                pass
        return await call_next(request)


class RateLimitMiddleware(BaseHTTPMiddleware):
    """In-process sliding-window limiter for writes and AI. Not shared across replicas."""

    def __init__(self, app, settings: Settings) -> None:
        super().__init__(app)
        self._settings = settings
        self._writes: dict[str, deque[float]] = defaultdict(deque)
        self._ai: dict[str, deque[float]] = defaultdict(deque)

    def _client_key(self, request: Request) -> str:
        forwarded = request.headers.get("X-Forwarded-For")
        if forwarded:
            return forwarded.split(",")[0].strip()
        if request.client:
            return request.client.host
        return "unknown"

    def _hit(self, bucket: dict[str, deque[float]], key: str, limit: int) -> bool:
        now = time.monotonic()
        window = float(self._settings.rate_limit_window_seconds)
        q = bucket[key]
        while q and now - q[0] > window:
            q.popleft()
        if len(q) >= limit:
            return False
        q.append(now)
        return True

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        if not self._settings.rate_limit_enabled:
            return await call_next(request)
        if self._settings.is_test:
            return await call_next(request)

        path = request.url.path
        method = request.method.upper()
        key = self._client_key(request)

        if path.startswith("/api/v1/ai/"):
            if not self._hit(self._ai, key, self._settings.rate_limit_ai_per_window):
                return JSONResponse(
                    status_code=429,
                    content={
                        "error": {
                            "code": "RATE_LIMITED",
                            "message": "Too many AI requests; try again shortly",
                            "details": None,
                        }
                    },
                    headers={"Retry-After": str(self._settings.rate_limit_window_seconds)},
                )
        elif method in {"POST", "PUT", "PATCH", "DELETE"} and path.startswith("/api/v1/"):
            if not self._hit(self._writes, key, self._settings.rate_limit_write_per_window):
                return JSONResponse(
                    status_code=429,
                    content={
                        "error": {
                            "code": "RATE_LIMITED",
                            "message": "Too many write requests; try again shortly",
                            "details": None,
                        }
                    },
                    headers={"Retry-After": str(self._settings.rate_limit_window_seconds)},
                )

        return await call_next(request)
