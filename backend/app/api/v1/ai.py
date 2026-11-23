from fastapi import APIRouter

from app.api.deps import CurrentPrincipal, DbSession, SettingsDep
from app.schemas.ai import AiChatRequest, AiChatResponse
from app.services.ai_assistant import AiAssistantService

router = APIRouter(prefix="/api/v1/ai", tags=["ai"])


@router.post("/chat", response_model=AiChatResponse)
async def ai_chat(
    payload: AiChatRequest,
    principal: CurrentPrincipal,
    session: DbSession,
    settings: SettingsDep,
) -> AiChatResponse:
    service = AiAssistantService(session, settings)
    return await service.chat(
        principal.subject,
        message=payload.message,
        year=payload.year,
        month=payload.month,
    )
