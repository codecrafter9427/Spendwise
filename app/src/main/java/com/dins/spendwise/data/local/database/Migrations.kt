package com.dins.spendwise.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN remoteId TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN serverVersion INTEGER NOT NULL DEFAULT 1")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_remoteId ON transactions(remoteId)"
            )

            db.execSQL("ALTER TABLE monthly_budgets ADD COLUMN remoteId TEXT")
            db.execSQL(
                "ALTER TABLE monthly_budgets ADD COLUMN serverVersion INTEGER NOT NULL DEFAULT 1"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_monthly_budgets_remoteId ON monthly_budgets(remoteId)"
            )

            db.execSQL("ALTER TABLE categories ADD COLUMN remoteId TEXT")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_categories_remoteId ON categories(remoteId)"
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN deletedAt INTEGER")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_transactions_deletedAt ON transactions(deletedAt)"
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS outbox_operations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    operationId TEXT NOT NULL,
                    entityType TEXT NOT NULL,
                    entityId TEXT NOT NULL,
                    operation TEXT NOT NULL,
                    baseVersion INTEGER,
                    payloadJson TEXT,
                    status TEXT NOT NULL,
                    attempts INTEGER NOT NULL,
                    lastError TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_outbox_operations_operationId ON outbox_operations(operationId)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_outbox_operations_status ON outbox_operations(status)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_outbox_operations_entityType_entityId ON outbox_operations(entityType, entityId)"
            )
        }
    }
}
