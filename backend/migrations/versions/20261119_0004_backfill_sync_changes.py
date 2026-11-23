"""backfill sync_changes for preexisting finance rows

Revision ID: 20261119_0004
Revises: 20261119_0003
Create Date: 2026-11-19

"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "20261119_0004"
down_revision: Union[str, None] = "20261119_0003"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Insert changelog rows for transactions/budgets that never got sync_changes
    # (created before Stage 5 logging was active).
    op.execute(
        sa.text(
            """
            INSERT INTO sync_changes (
                user_id, entity_type, entity_id, operation, version, payload, changed_at
            )
            SELECT
                t.user_id,
                'transaction',
                t.id,
                CASE WHEN t.deleted_at IS NULL THEN 'upsert' ELSE 'delete' END,
                t.version,
                jsonb_build_object(
                    'id', t.id::text,
                    'type', t.type,
                    'amount_minor', t.amount_minor,
                    'currency', t.currency,
                    'category_id', t.category_id::text,
                    'description', t.description,
                    'transaction_date', t.transaction_date::text,
                    'version', t.version,
                    'deleted_at', CASE WHEN t.deleted_at IS NULL THEN NULL ELSE t.deleted_at::text END,
                    'created_at', t.created_at::text,
                    'updated_at', t.updated_at::text
                ),
                COALESCE(t.updated_at, now())
            FROM transactions t
            WHERE NOT EXISTS (
                SELECT 1 FROM sync_changes s
                WHERE s.entity_type = 'transaction' AND s.entity_id = t.id
            )
            """
        )
    )
    op.execute(
        sa.text(
            """
            INSERT INTO sync_changes (
                user_id, entity_type, entity_id, operation, version, payload, changed_at
            )
            SELECT
                b.user_id,
                'budget',
                b.id,
                'upsert',
                b.version,
                jsonb_build_object(
                    'id', b.id::text,
                    'year', b.year,
                    'month', b.month,
                    'amount_minor', b.amount_minor,
                    'currency', b.currency,
                    'version', b.version,
                    'created_at', b.created_at::text,
                    'updated_at', b.updated_at::text
                ),
                COALESCE(b.updated_at, now())
            FROM monthly_budgets b
            WHERE NOT EXISTS (
                SELECT 1 FROM sync_changes s
                WHERE s.entity_type = 'budget' AND s.entity_id = b.id
            )
            """
        )
    )


def downgrade() -> None:
    pass
