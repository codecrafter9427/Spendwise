from typing import Any

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException


def error_envelope(
    *,
    code: str,
    message: str,
    details: Any = None,
    status_code: int,
) -> JSONResponse:
    body: dict[str, Any] = {
        "error": {
            "code": code,
            "message": message,
            "details": details,
        }
    }
    return JSONResponse(status_code=status_code, content=body)


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(StarletteHTTPException)
    async def http_exception_handler(
        _request: Request,
        exc: StarletteHTTPException,
    ) -> JSONResponse:
        detail = exc.detail
        if isinstance(detail, dict) and "code" in detail and "message" in detail:
            code = str(detail["code"])
            message = str(detail["message"])
            details = detail.get("details")
        else:
            code = {
                401: "UNAUTHORIZED",
                403: "FORBIDDEN",
                404: "NOT_FOUND",
                409: "CONFLICT",
                422: "VALIDATION_ERROR",
                429: "RATE_LIMITED",
            }.get(exc.status_code, "HTTP_ERROR")
            message = str(detail)
            details = None
        return error_envelope(
            code=code,
            message=message,
            details=details,
            status_code=exc.status_code,
        )

    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(
        _request: Request,
        exc: RequestValidationError,
    ) -> JSONResponse:
        return error_envelope(
            code="VALIDATION_ERROR",
            message="Request validation failed",
            details=_jsonable_validation_errors(exc.errors()),
            status_code=422,
        )


def _jsonable_validation_errors(errors: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Pydantic may put exception objects in error ctx; make them JSON-safe."""
    cleaned: list[dict[str, Any]] = []
    for item in errors:
        entry = dict(item)
        ctx = entry.get("ctx")
        if isinstance(ctx, dict):
            entry["ctx"] = {
                key: (str(value) if isinstance(value, BaseException) else value)
                for key, value in ctx.items()
            }
        cleaned.append(entry)
    return cleaned
