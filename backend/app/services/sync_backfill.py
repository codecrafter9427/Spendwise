from __future__ import annotations

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.budget import MonthlyBudget
from app.models.sync import SyncChange
from app.models.transaction import Transaction
from app.services.sync_payloads import budget_payload, transaction_payload


async def backfill_sync_changes(session: AsyncSession) -> dict[str, int]:
    """Emit changelog rows for existing entities that predate Stage 5 sync logging."""
    existing = await session.execute(select(SyncChange.entity_id, SyncChange.entity_type))
    already = {(row.entity_type, row.entity_id) for row in existing.all()}

    tx_added = 0
    budget_added = 0

    transactions = (await session.execute(select(Transaction))).scalars().all()
    for tx in transactions:
        key = ("transaction", tx.id)
        if key in already:
            continue
        session.add(
            SyncChange(
                user_id=tx.user_id,
                entity_type="transaction",
                entity_id=tx.id,
                operation="delete" if tx.deleted_at is not None else "upsert",
                version=tx.version,
                payload=transaction_payload(tx),
            )
        )
        tx_added += 1

    budgets = (await session.execute(select(MonthlyBudget))).scalars().all()
    for budget in budgets:
        key = ("budget", budget.id)
        if key in already:
            continue
        session.add(
            SyncChange(
                user_id=budget.user_id,
                entity_type="budget",
                entity_id=budget.id,
                operation="upsert",
                version=budget.version,
                payload=budget_payload(budget),
            )
        )
        budget_added += 1

    await session.flush()
    return {"transactions": tx_added, "budgets": budget_added}
