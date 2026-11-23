package com.dins.spendwise.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dins.spendwise.data.local.dao.CategoryDao
import com.dins.spendwise.data.local.dao.MonthlyBudgetDao
import com.dins.spendwise.data.local.dao.TransactionDao
import com.dins.spendwise.data.local.database.CategorySeeder
import com.dins.spendwise.data.local.database.SpendWiseDatabase
import com.dins.spendwise.data.local.entity.MonthlyBudgetEntity
import com.dins.spendwise.data.local.entity.TransactionEntity
import com.dins.spendwise.domain.model.TransactionType
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpendWiseDatabaseTest {

    private lateinit var db: SpendWiseDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var budgetDao: MonthlyBudgetDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SpendWiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        categoryDao = db.categoryDao()
        transactionDao = db.transactionDao()
        budgetDao = db.monthlyBudgetDao()
        runBlocking {
            categoryDao.insertAll(CategorySeeder.defaultCategories())
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertObserveUpdateDeleteTransaction() = runBlocking {
        val date = LocalDate.of(2026, 11, 2).toEpochDay()
        val id = transactionDao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amountMinor = 25000,
                categoryId = 1L,
                description = "Lunch",
                transactionDate = date,
                createdAt = 1L,
                updatedAt = 1L
            )
        )
        val observed = transactionDao.observeByMonthWithCategory(date, date).first()
        assertEquals(1, observed.size)
        assertEquals("Lunch", observed.first().transaction.description)

        transactionDao.update(
            observed.first().transaction.copy(description = "Dinner", updatedAt = 2L)
        )
        val updated = transactionDao.getById(id)
        assertEquals("Dinner", updated?.description)

        val now = System.currentTimeMillis()
        transactionDao.update(updated!!.copy(deletedAt = now, updatedAt = now))
        assertEquals(0, transactionDao.observeByMonthWithCategory(date, date).first().size)
        assertNotNull(transactionDao.getById(id)?.deletedAt)
    }

    @Test
    fun budgetUniquenessByYearMonth() = runBlocking {
        val now = System.currentTimeMillis()
        budgetDao.insert(
            MonthlyBudgetEntity(
                year = 2026,
                month = 11,
                amountMinor = 50_000_00,
                createdAt = now,
                updatedAt = now
            )
        )
        val existing = budgetDao.getByYearMonth(2026, 11)
        assertNotNull(existing)
        budgetDao.update(existing!!.copy(amountMinor = 60_000_00, updatedAt = now + 1))
        assertEquals(60_000_00L, budgetDao.getByYearMonth(2026, 11)?.amountMinor)
    }

    @Test
    fun sumByTypeAndMonth() = runBlocking {
        val start = LocalDate.of(2026, 11, 1).toEpochDay()
        val end = LocalDate.of(2026, 11, 30).toEpochDay()
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amountMinor = 100,
                categoryId = 1L,
                description = null,
                transactionDate = LocalDate.of(2026, 11, 5).toEpochDay(),
                createdAt = 1,
                updatedAt = 1
            )
        )
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.INCOME.name,
                amountMinor = 500,
                categoryId = 101L,
                description = null,
                transactionDate = LocalDate.of(2026, 11, 5).toEpochDay(),
                createdAt = 2,
                updatedAt = 2
            )
        )
        assertEquals(
            100L,
            transactionDao.observeSumByTypeAndMonth(TransactionType.EXPENSE.name, start, end).first()
        )
        assertEquals(
            500L,
            transactionDao.observeSumByTypeAndMonth(TransactionType.INCOME.name, start, end).first()
        )
    }
}
