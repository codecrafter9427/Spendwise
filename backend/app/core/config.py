from functools import lru_cache

from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_env: str = Field(default="development", alias="APP_ENV")
    app_name: str = Field(default="spendwise-api", alias="APP_NAME")
    log_level: str = Field(default="INFO", alias="LOG_LEVEL")

    database_url: str = Field(
        default="postgresql+asyncpg://spendwise:spendwise@localhost:5432/spendwise",
        alias="DATABASE_URL",
    )

    supabase_url: str = Field(default="", alias="SUPABASE_URL")
    supabase_jwks_url: str = Field(default="", alias="SUPABASE_JWKS_URL")
    supabase_jwt_issuer: str = Field(default="", alias="SUPABASE_JWT_ISSUER")
    supabase_jwt_audience: str = Field(default="authenticated", alias="SUPABASE_JWT_AUDIENCE")

    # Stored as a raw comma-separated string so dotenv does not JSON-decode it.
    cors_origins_raw: str = Field(default="http://localhost:3000", alias="CORS_ORIGINS")

    # HS256 secret for APP_ENV=test only. Never configure in production.
    jwt_test_secret: str | None = Field(default=None, alias="JWT_TEST_SECRET")

    # Optional OpenAI-compatible provider. Without a key, AI uses deterministic tools only.
    ai_feature_enabled: bool = Field(default=True, alias="AI_FEATURE_ENABLED")
    ai_provider_api_key: str | None = Field(default=None, alias="AI_PROVIDER_API_KEY")
    ai_provider_base_url: str = Field(
        default="https://api.openai.com/v1",
        alias="AI_PROVIDER_BASE_URL",
    )
    ai_model: str = Field(default="gpt-4o-mini", alias="AI_MODEL")

    @field_validator("cors_origins_raw", mode="before")
    @classmethod
    def coerce_cors_origins_raw(cls, value: object) -> str:
        if value is None:
            return "http://localhost:3000"
        if isinstance(value, list):
            return ",".join(str(item) for item in value)
        return str(value)

    @property
    def cors_origins(self) -> list[str]:
        origins = [item.strip() for item in self.cors_origins_raw.split(",") if item.strip()]
        return origins or ["http://localhost:3000"]

    @property
    def is_test(self) -> bool:
        return self.app_env.lower() == "test"

    @property
    def is_production(self) -> bool:
        return self.app_env.lower() == "production"


@lru_cache
def get_settings() -> Settings:
    return Settings()
