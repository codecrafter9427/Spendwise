from __future__ import annotations

from typing import Any

from app.models.budget import MonthlyBudget
from app.models.transaction import Transaction


def transaction_payload(tx: Transaction) -> dict[str, Any]:
    return {
        "id": str(tx.id),
        "type": tx.type,
        "amount_minor": tx.amount_minor,
        "currency": tx.currency,
        "category_id": str(tx.category_id),
        "description": tx.description,
        "transaction_date": tx.transaction_date.isoformat(),
        "version": tx.version,
        "deleted_at": tx.deleted_at.isoformat() if tx.deleted_at else None,
        "created_at": tx.created_at.isoformat() if tx.created_at else None,
        "updated_at": tx.updated_at.isoformat() if tx.updated_at else None,
    }


def budget_payload(budget: MonthlyBudget) -> dict[str, Any]:
    return {
        "id": str(budget.id),
        "year": budget.year,
        "month": budget.month,
        "amount_minor": budget.amount_minor,
        "currency": budget.currency,
        "version": budget.version,
        "created_at": budget.created_at.isoformat() if budget.created_at else None,
        "updated_at": budget.updated_at.isoformat() if budget.updated_at else None,
    }
