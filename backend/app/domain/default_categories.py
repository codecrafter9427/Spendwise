"""Stable default category catalog mapped from Android CategorySeeder."""

from __future__ import annotations

import uuid
from dataclasses import dataclass

from app.domain.enums import TransactionType

# Fixed namespace so default UUIDs are stable across environments.
_DEFAULT_NAMESPACE = uuid.UUID("7c3f9e2a-4b1d-4f6a-9e8c-1a2b3c4d5e6f")


@dataclass(frozen=True, slots=True)
class DefaultCategorySeed:
    name: str
    type: TransactionType
    icon_key: str
    android_local_id: int

    @property
    def id(self) -> uuid.UUID:
        return uuid.uuid5(_DEFAULT_NAMESPACE, f"{self.type.value}:{self.name}")


DEFAULT_CATEGORIES: tuple[DefaultCategorySeed, ...] = (
    DefaultCategorySeed("Food & Dining", TransactionType.EXPENSE, "restaurant", 1),
    DefaultCategorySeed("Transportation", TransactionType.EXPENSE, "directions_car", 2),
    DefaultCategorySeed("Shopping", TransactionType.EXPENSE, "shopping_bag", 3),
    DefaultCategorySeed("Bills & Utilities", TransactionType.EXPENSE, "receipt", 4),
    DefaultCategorySeed("Health", TransactionType.EXPENSE, "favorite", 5),
    DefaultCategorySeed("Entertainment", TransactionType.EXPENSE, "sports_esports", 6),
    DefaultCategorySeed("Education", TransactionType.EXPENSE, "school", 7),
    DefaultCategorySeed("Travel", TransactionType.EXPENSE, "flight", 8),
    DefaultCategorySeed("Groceries", TransactionType.EXPENSE, "shopping_basket", 9),
    DefaultCategorySeed("Housing", TransactionType.EXPENSE, "home", 10),
    DefaultCategorySeed("Personal Care", TransactionType.EXPENSE, "person", 11),
    DefaultCategorySeed("Other", TransactionType.EXPENSE, "more_horiz", 12),
    DefaultCategorySeed("Salary", TransactionType.INCOME, "work", 101),
    DefaultCategorySeed("Freelance", TransactionType.INCOME, "laptop", 102),
    DefaultCategorySeed("Business", TransactionType.INCOME, "business", 103),
    DefaultCategorySeed("Investment Returns", TransactionType.INCOME, "trending_up", 104),
    DefaultCategorySeed("Gift", TransactionType.INCOME, "card_giftcard", 105),
    DefaultCategorySeed("Other Income", TransactionType.INCOME, "account_balance_wallet", 106),
)


def default_category_by_android_id(android_local_id: int) -> DefaultCategorySeed | None:
    for item in DEFAULT_CATEGORIES:
        if item.android_local_id == android_local_id:
            return item
    return None
