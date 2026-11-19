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
}
