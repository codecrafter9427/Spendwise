package com.dins.spendwise.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dins.spendwise.data.local.dao.CategoryDao
import com.dins.spendwise.data.local.dao.MonthlyBudgetDao
import com.dins.spendwise.data.local.dao.TransactionDao
import com.dins.spendwise.data.local.entity.CategoryEntity
import com.dins.spendwise.data.local.entity.MonthlyBudgetEntity
import com.dins.spendwise.data.local.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        MonthlyBudgetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SpendWiseDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao

    companion object {
        const val NAME = "spendwise.db"

        fun createCallback(scope: CoroutineScope, databaseProvider: () -> SpendWiseDatabase) =
            object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    scope.launch(Dispatchers.IO) {
                        databaseProvider().categoryDao().insertAll(CategorySeeder.defaultCategories())
                    }
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    scope.launch(Dispatchers.IO) {
                        val dao = databaseProvider().categoryDao()
                        if (dao.count() == 0) {
                            dao.insertAll(CategorySeeder.defaultCategories())
                        }
                    }
                }
            }
    }
}
