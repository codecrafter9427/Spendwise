package com.dins.spendwise.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dins.spendwise.core.utils.DefaultCategoryRemoteIds
import com.dins.spendwise.data.local.dao.CategoryDao
import com.dins.spendwise.data.local.dao.MonthlyBudgetDao
import com.dins.spendwise.data.local.dao.OutboxDao
import com.dins.spendwise.data.local.dao.TransactionDao
import com.dins.spendwise.data.local.entity.CategoryEntity
import com.dins.spendwise.data.local.entity.MonthlyBudgetEntity
import com.dins.spendwise.data.local.entity.OutboxEntity
import com.dins.spendwise.data.local.entity.TransactionEntity
import com.dins.spendwise.domain.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        MonthlyBudgetEntity::class,
        OutboxEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SpendWiseDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao
    abstract fun outboxDao(): OutboxDao

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
                        } else {
                            backfillCategoryRemoteIds(dao)
                        }
                    }
                }
            }

        private suspend fun backfillCategoryRemoteIds(dao: CategoryDao) {
            dao.observeAllOnce().forEach { category ->
                if (category.remoteId.isNullOrBlank()) {
                    val type = runCatching { TransactionType.valueOf(category.type) }.getOrNull()
                        ?: return@forEach
                    dao.update(
                        category.copy(
                            remoteId = DefaultCategoryRemoteIds.remoteId(type, category.name)
                        )
                    )
                }
            }
        }
    }
}
