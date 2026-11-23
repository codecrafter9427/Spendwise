from __future__ import annotations

import hashlib
import json
from datetime import date
from uuid import UUID

from fastapi import HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.domain.enums import TransactionType
from app.models.sync import IdempotencyRecord, SyncChange
from app.schemas.budget import BudgetUpsert
from app.schemas.sync import (
    SyncChangeRead,
    SyncPullResponse,
    SyncPushItemResult,
    SyncPushOperation,
    SyncPushRequest,
    SyncPushResponse,
)
from app.schemas.transaction import TransactionCreate, TransactionUpdate
from app.services.budgets import BudgetService
from app.services.transactions import TransactionService
from app.services.users import UserService


class SyncService:
    def __init__(self, session: AsyncSession) -> None:
        self._session = session
        self._users = UserService(session)
        self._transactions = TransactionService(session)
        self._budgets = BudgetService(session)

    async def pull(
        self,
        user_id: UUID,
        *,
        cursor: str | None,
        limit: int = 100,
    ) -> SyncPullResponse:
        await self._users.get_or_provision(user_id)
        after = 0
        if cursor:
            try:
                after = int(cursor)
            except ValueError as exc:
                raise HTTPException(
                    status_code=422,
                    detail={"code": "INVALID_CURSOR", "message": "cursor must be an integer"},
                ) from exc

        result = await self._session.execute(
            select(SyncChange)
            .where(SyncChange.user_id == user_id, SyncChange.sequence > after)
            .order_by(SyncChange.sequence.asc())
            .limit(limit + 1)
        )
        rows = list(result.scalars().all())
        has_more = len(rows) > limit
        if has_more:
            rows = rows[:limit]
        return SyncPullResponse(
            changes=[
                SyncChangeRead(
                    sequence=row.sequence,
                    entity_type=row.entity_type,
                    entity_id=row.entity_id,
                    operation=row.operation,
                    version=row.version,
                    payload=row.payload,
                    changed_at=row.changed_at,
                )
                for row in rows
            ],
            next_cursor=str(rows[-1].sequence) if rows else cursor,
            has_more=has_more,
        )

    async def push(self, user_id: UUID, request: SyncPushRequest) -> SyncPushResponse:
        await self._users.get_or_provision(user_id)
        results: list[SyncPushItemResult] = []
        for op in request.operations:
            results.append(await self._apply_operation(user_id, op))
        return SyncPushResponse(results=results)

    async def _apply_operation(
        self,
        user_id: UUID,
        op: SyncPushOperation,
    ) -> SyncPushItemResult:
        request_hash = hashlib.sha256(
            json.dumps(op.model_dump(mode="json"), sort_keys=True).encode("utf-8")
        ).hexdigest()

        existing = await self._session.execute(
            select(IdempotencyRecord).where(
                IdempotencyRecord.user_id == user_id,
                IdempotencyRecord.idempotency_key == op.operation_id,
            )
        )
        record = existing.scalar_one_or_none()
        if record is not None:
            if record.request_hash != request_hash:
                return SyncPushItemResult(
                    operation_id=op.operation_id,
                    status="conflict",
                    error_code="IDEMPOTENCY_PAYLOAD_MISMATCH",
                    error_message="Same operation_id with different payload",
                )
            body = record.response_body or {}
            entity_id = UUID(body["entity_id"]) if body.get("entity_id") else op.entity_id
            return SyncPushItemResult(
                operation_id=op.operation_id,
                status="duplicate",
                entity_id=entity_id,
                version=body.get("version"),
            )

        entity_id: UUID | None = op.entity_id
        version: int | None = None
        try:
            entity_id, version = await self._execute(user_id, op)
            status_name = "accepted"
            error_code = None
            error_message = None
            response_status = 200
        except HTTPException as exc:
            detail = exc.detail if isinstance(exc.detail, dict) else {}
            code = str(detail.get("code") or "REJECTED")
            status_name = "conflict" if exc.status_code == 409 else "rejected"
            details = detail.get("details")
            if isinstance(details, dict) and "current_version" in details:
                version = int(details["current_version"])
            error_code = code
            error_message = str(detail.get("message") or exc.detail)
            response_status = exc.status_code

        response_body = {
            "entity_id": str(entity_id) if entity_id else None,
            "version": version,
            "status": status_name,
            "error_code": error_code,
            "error_message": error_message,
        }
        self._session.add(
            IdempotencyRecord(
                user_id=user_id,
                idempotency_key=op.operation_id,
                request_hash=request_hash,
                response_status=response_status,
                response_body=response_body,
            )
        )
        await self._session.flush()
        return SyncPushItemResult(
            operation_id=op.operation_id,
            status=status_name,  # type: ignore[arg-type]
            entity_id=entity_id,
            version=version,
            error_code=error_code,
            error_message=error_message,
        )

    async def _execute(
        self,
        user_id: UUID,
        op: SyncPushOperation,
    ) -> tuple[UUID, int]:
        payload = op.payload or {}
        if op.entity_type == "transaction":
            if op.operation == "delete":
                tx = await self._transactions.soft_delete(
                    user_id,
                    op.entity_id,
                    expected_version=op.base_version or 1,
                )
                return tx.id, tx.version

            existing = await self._transactions.get_including_deleted(user_id, op.entity_id)
            if existing is None:
                create = TransactionCreate(
                    id=op.entity_id,
                    type=TransactionType(str(payload["type"]).lower()),
                    amount_minor=int(payload["amount_minor"]),
                    currency=str(payload.get("currency", "INR")),
                    category_id=UUID(str(payload["category_id"])),
                    description=payload.get("description"),
                    transaction_date=date.fromisoformat(str(payload["transaction_date"])),
                )
                tx = await self._transactions.create(user_id, create)
                return tx.id, tx.version

            update = TransactionUpdate(
                expected_version=op.base_version or existing.version,
                type=TransactionType(str(payload["type"]).lower()) if "type" in payload else None,
                amount_minor=int(payload["amount_minor"]) if "amount_minor" in payload else None,
                currency=str(payload["currency"]) if "currency" in payload else None,
                category_id=UUID(str(payload["category_id"])) if "category_id" in payload else None,
                description=payload.get("description") if "description" in payload else None,
                transaction_date=date.fromisoformat(str(payload["transaction_date"]))
                if "transaction_date" in payload
                else None,
            )
            tx = await self._transactions.update(user_id, op.entity_id, update)
            return tx.id, tx.version

        if op.entity_type == "budget" and op.operation == "upsert":
            budget = await self._budgets.upsert(
                user_id,
                int(payload["year"]),
                int(payload["month"]),
                BudgetUpsert(
                    amount_minor=int(payload["amount_minor"]),
                    currency=str(payload.get("currency", "INR")),
                    expected_version=op.base_version,
                ),
            )
            return budget.id, budget.version

        raise HTTPException(
            status_code=422,
            detail={"code": "UNSUPPORTED_OPERATION", "message": "Unsupported sync operation"},
        )
