"""add ai_enabled consent flag on app_users

Revision ID: 20261123_0005
Revises: 20261119_0004
Create Date: 2026-11-23

"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "20261123_0005"
down_revision: Union[str, None] = "20261119_0004"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "app_users",
        sa.Column("ai_enabled", sa.Boolean(), nullable=False, server_default=sa.text("false")),
    )


def downgrade() -> None:
    op.drop_column("app_users", "ai_enabled")
