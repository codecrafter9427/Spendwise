from typing import Any, Literal

from pydantic import BaseModel, Field


class AiChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=1000)
    year: int | None = Field(default=None, ge=1970, le=2100)
    month: int | None = Field(default=None, ge=1, le=12)


class AiToolCall(BaseModel):
    name: str
    arguments: dict[str, Any]
    result: dict[str, Any]


class AiChatResponse(BaseModel):
    answer: str
    mode: Literal["deterministic", "llm"]
    period: dict[str, int]
    tools_used: list[AiToolCall]
    disclaimer: str
