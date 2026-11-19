from __future__ import annotations

import os
import uuid
from collections.abc import AsyncIterator
from datetime import UTC, datetime, timedelta

import jwt
import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy import text
from sqlalchemy.ext.asyncio import (
    AsyncEngine,
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)
from sqlalchemy.pool import NullPool

# Configure test environment before importing the app.
os.environ["APP_ENV"] = "test"
os.environ["JWT_TEST_SECRET"] = "spendwise-test-secret-do-not-use-in-prod"
os.environ["SUPABASE_JWT_ISSUER"] = "https://test.supabase.co/auth/v1"
os.environ["SUPABASE_JWT_AUDIENCE"] = "authenticated"
os.environ.setdefault(
    "DATABASE_URL",
    "postgresql+asyncpg://spendwise:spendwise@localhost:5432/spendwise_test",
)

from app.core.config import get_settings  # noqa: E402
from app.db.base import Base  # noqa: E402
from app.db.session import get_db_session  # noqa: E402
from app.main import create_app  # noqa: E402
from app.models import AppUser, Category, MonthlyBudget, Transaction  # noqa: E402, F401
from app.services.seed import ensure_default_categories  # noqa: E402

ADMIN_URL = "postgresql+asyncpg://spendwise:spendwise@localhost:5432/postgres"
TEST_DB_URL = os.environ["DATABASE_URL"]
TEST_DB_NAME = "spendwise_test"
JWT_SECRET = os.environ["JWT_TEST_SECRET"]
JWT_ISSUER = os.environ["SUPABASE_JWT_ISSUER"]
JWT_AUDIENCE = os.environ["SUPABASE_JWT_AUDIENCE"]

_db_ready = False


async def _ensure_test_database() -> None:
    global _db_ready
    if _db_ready:
        return
    engine = create_async_engine(ADMIN_URL, isolation_level="AUTOCOMMIT")
    async with engine.connect() as conn:
        exists = await conn.execute(
            text("SELECT 1 FROM pg_database WHERE datname = :name"),
            {"name": TEST_DB_NAME},
        )
        if exists.scalar() is None:
            await conn.execute(text(f'CREATE DATABASE "{TEST_DB_NAME}"'))
    await engine.dispose()
    _db_ready = True


@pytest_asyncio.fixture
async def engine() -> AsyncIterator[AsyncEngine]:
    get_settings.cache_clear()
    await _ensure_test_database()
    eng = create_async_engine(TEST_DB_URL, poolclass=NullPool, pool_pre_ping=True)
    async with eng.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
        await conn.run_sync(Base.metadata.create_all)
    async with async_sessionmaker(bind=eng, class_=AsyncSession, expire_on_commit=False)() as session:
        await ensure_default_categories(session)
        await session.commit()
    yield eng
    async with eng.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
    await eng.dispose()
    get_settings.cache_clear()


@pytest_asyncio.fixture
async def session_factory(engine: AsyncEngine) -> async_sessionmaker[AsyncSession]:
    return async_sessionmaker(bind=engine, class_=AsyncSession, expire_on_commit=False)


@pytest_asyncio.fixture
async def client(
    engine: AsyncEngine,
    session_factory: async_sessionmaker[AsyncSession],
) -> AsyncIterator[AsyncClient]:
    get_settings.cache_clear()
    app = create_app()

    async def override_db_session() -> AsyncIterator[AsyncSession]:
        async with session_factory() as session:
            try:
                yield session
                await session.commit()
            except Exception:
                await session.rollback()
                raise

    app.dependency_overrides[get_db_session] = override_db_session

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac

    app.dependency_overrides.clear()
    get_settings.cache_clear()


def make_access_token(
    subject: uuid.UUID,
    *,
    expires_in_seconds: int = 3600,
    issuer: str = JWT_ISSUER,
    audience: str = JWT_AUDIENCE,
    secret: str = JWT_SECRET,
    algorithm: str = "HS256",
) -> str:
    now = datetime.now(UTC)
    payload = {
        "sub": str(subject),
        "iss": issuer,
        "aud": audience,
        "iat": int(now.timestamp()),
        "exp": int((now + timedelta(seconds=expires_in_seconds)).timestamp()),
        "role": "authenticated",
    }
    return jwt.encode(payload, secret, algorithm=algorithm)


def auth_headers(subject: uuid.UUID, **kwargs: object) -> dict[str, str]:
    token = make_access_token(subject, **kwargs)  # type: ignore[arg-type]
    return {"Authorization": f"Bearer {token}"}
