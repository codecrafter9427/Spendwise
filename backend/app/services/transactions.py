from __future__ import annotations

from datetime import UTC, date, datetime
from uuid import UUID

from fastapi import HTTPException, status
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.transaction import Transaction
from app.repositories.category import CategoryRepository
from app.repositories.transaction import TransactionRepository
from app.schemas.transaction import TransactionCreate, TransactionUpdate
from app.services.sync_log import SyncLogService
from app.services.sync_payloads import transaction_payload
from app.services.users import UserService


class TransactionService:
    def __init__(self, session: AsyncSession) -> None:
        self._session = session
        self._repo = TransactionRepository(session)
        self._categories = CategoryRepository(session)
        self._users = UserService(session)
        self._sync = SyncLogService(session)

    async def _require_active_category(
        self,
        user_id: UUID,
        category_id: UUID,
        expected_type: str,
    ):
        category = await self._categories.get_accessible(category_id, user_id)
        if category is None or category.archived_at is not None:
            raise HTTPException(
                status_code=422,
                detail={
                    "code": "INVALID_CATEGORY",
                    "message": "Category is missing, archived, or inaccessible",
                },
            )
        if category.type != expected_type:
            raise HTTPException(
                status_code=422,
                detail={
                    "code": "CATEGORY_TYPE_MISMATCH",
                    "message": "Category type must match transaction type",
                },
            )
        return category

    async def create(self, user_id: UUID, payload: TransactionCreate) -> Transaction:
        await self._users.get_or_provision(user_id)
        await self._require_active_category(user_id, payload.category_id, payload.type.value)

        existing = await self._repo.get_owned(payload.id, user_id, include_deleted=True)
        if existing is not None:
            if existing.deleted_at is not None:
                raise HTTPException(
                    status_code=status.HTTP_409_CONFLICT,
                    detail={
                        "code": "TRANSACTION_DELETED",
                        "message": "Transaction id belongs to a deleted record",
                    },
                )
            # Idempotent replay of the same create id.
            return existing

        transaction = Transaction(
            id=payload.id,
            user_id=user_id,
            type=payload.type.value,
            amount_minor=payload.amount_minor,
            currency=payload.currency,
            category_id=payload.category_id,
            description=payload.description,
            transaction_date=payload.transaction_date,
            version=1,
        )
        try:
            created = await self._repo.add(transaction)
        except IntegrityError as exc:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail={
                    "code": "TRANSACTION_EXISTS",
                    "message": "Transaction id already exists",
                },
            ) from exc
        await self._sync.record(
            user_id=user_id,
            entity_type="transaction",
            entity_id=created.id,
            operation="upsert",
            version=created.version,
            payload=transaction_payload(created),
        )
        return created

    async def get(self, user_id: UUID, transaction_id: UUID) -> Transaction:
        await self._users.get_or_provision(user_id)
        transaction = await self._repo.get_owned(transaction_id, user_id)
        if transaction is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail={"code": "TRANSACTION_NOT_FOUND", "message": "Transaction not found"},
            )
        return transaction

    async def get_including_deleted(
        self,
        user_id: UUID,
        transaction_id: UUID,
    ) -> Transaction | None:
        await self._users.get_or_provision(user_id)
        return await self._repo.get_owned(transaction_id, user_id, include_deleted=True)

    async def list_page(
        self,
        user_id: UUID,
        *,
        limit: int,
        cursor: str | None,
        start_date: date | None,
        end_date: date | None,
        type_filter: str | None,
        category_id: UUID | None,
        query: str | None,
    ) -> tuple[list[Transaction], str | None]:
        await self._users.get_or_provision(user_id)
        try:
            return await self._repo.list_page(
                user_id,
                limit=limit,
                cursor=cursor,
                start_date=start_date,
                end_date=end_date,
                type_filter=type_filter,
                category_id=category_id,
                query=query,
            )
        except ValueError as exc:
            raise HTTPException(
                status_code=422,
                detail={"code": "INVALID_CURSOR", "message": str(exc)},
            ) from exc

    async def update(
        self,
        user_id: UUID,
        transaction_id: UUID,
        payload: TransactionUpdate,
    ) -> Transaction:
        await self._users.get_or_provision(user_id)
        transaction = await self._repo.get_owned(transaction_id, user_id)
        if transaction is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail={"code": "TRANSACTION_NOT_FOUND", "message": "Transaction not found"},
            )
        if transaction.version != payload.expected_version:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail={
                    "code": "VERSION_CONFLICT",
                    "message": "Stale transaction version",
                    "details": {"current_version": transaction.version},
                },
            )

        data = payload.model_dump(exclude_unset=True)
        data.pop("expected_version", None)
        next_type = data.get("type", transaction.type)
        if hasattr(next_type, "value"):
            next_type = next_type.value
        next_category_id = data.get("category_id", transaction.category_id)
        if "type" in data or "category_id" in data:
            await self._require_active_category(user_id, next_category_id, next_type)

        for key, value in data.items():
            if key == "type" and hasattr(value, "value"):
                setattr(transaction, key, value.value)
            else:
                setattr(transaction, key, value)
        transaction.version += 1
        saved = await self._repo.save(transaction)
        await self._sync.record(
            user_id=user_id,
            entity_type="transaction",
            entity_id=saved.id,
            operation="upsert",
            version=saved.version,
            payload=transaction_payload(saved),
        )
        return saved

    async def soft_delete(
        self,
        user_id: UUID,
        transaction_id: UUID,
        expected_version: int,
    ) -> Transaction:
        await self._users.get_or_provision(user_id)
        transaction = await self._repo.get_owned(transaction_id, user_id)
        if transaction is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail={"code": "TRANSACTION_NOT_FOUND", "message": "Transaction not found"},
            )
        if transaction.version != expected_version:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail={
                    "code": "VERSION_CONFLICT",
                    "message": "Stale transaction version",
                    "details": {"current_version": transaction.version},
                },
            )
        transaction.deleted_at = datetime.now(UTC)
        transaction.version += 1
        saved = await self._repo.save(transaction)
        await self._sync.record(
            user_id=user_id,
            entity_type="transaction",
            entity_id=saved.id,
            operation="delete",
            version=saved.version,
            payload=transaction_payload(saved),
        )
        return saved
