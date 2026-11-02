package com.dins.spendwise.domain.usecase

import com.dins.spendwise.domain.model.Category
import com.dins.spendwise.domain.model.Transaction
import com.dins.spendwise.domain.model.TransactionType
import com.dins.spendwise.domain.model.TransactionWithCategory
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionFiltersTest {

    private val food = Category(1, "Food & Dining", TransactionType.EXPENSE, "restaurant", true)
    private val salary = Category(101, "Salary", TransactionType.INCOME, "work", true)

    private fun item(
        id: Long,
        type: TransactionType,
        category: Category,
        description: String?,
        day: Int,
        createdAt: Long
    ) = TransactionWithCategory(
        transaction = Transaction(
            id = id,
            type = type,
            amountMinor = 100,
            categoryId = category.id,
            description = description,
            transactionDate = LocalDate.of(2026, 11, day),
            createdAt = createdAt,
            updatedAt = createdAt
        ),
        category = category
    )

    @Test
    fun filtersSearchTypeCategoryAndSorts() {
        val items = listOf(
            item(1, TransactionType.EXPENSE, food, "Lunch", 10, 100),
            item(2, TransactionType.EXPENSE, food, "Dinner", 10, 200),
            item(3, TransactionType.INCOME, salary, "November pay", 5, 50),
            item(4, TransactionType.EXPENSE, food, "Coffee", 1, 10).let {
                it.copy(
                    transaction = it.transaction.copy(transactionDate = LocalDate.of(2026, 10, 1))
                )
            }
        )

        val filtered = TransactionFilters.filter(
            items = items,
            yearMonth = YearMonth.of(2026, 11),
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            query = "din"
        )
        assertEquals(listOf(2L, 1L), filtered.map { it.transaction.id })
    }

    @Test
    fun caseInsensitiveSearchOnCategoryAndDescription() {
        val items = listOf(
            item(1, TransactionType.EXPENSE, food, "Groceries run", 2, 1),
            item(2, TransactionType.INCOME, salary, null, 2, 2)
        )
        val byCategory = TransactionFilters.filter(items, null, null, null, "food")
        assertEquals(1, byCategory.size)
        val byDescription = TransactionFilters.filter(items, null, null, null, "GROCERIES")
        assertEquals(1, byDescription.size)
    }
}
