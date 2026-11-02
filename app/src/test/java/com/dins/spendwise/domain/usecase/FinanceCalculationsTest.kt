package com.dins.spendwise.domain.usecase

import com.dins.spendwise.domain.model.BudgetStatus
import com.dins.spendwise.domain.model.Category
import com.dins.spendwise.domain.model.Transaction
import com.dins.spendwise.domain.model.TransactionType
import com.dins.spendwise.domain.model.TransactionWithCategory
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceCalculationsTest {

    private val month = YearMonth.of(2026, 11)

    private fun tx(
        type: TransactionType,
        amount: Long,
        day: Int,
        categoryId: Long = 1L
    ) = Transaction(
        id = day.toLong(),
        type = type,
        amountMinor = amount,
        categoryId = categoryId,
        description = null,
        transactionDate = LocalDate.of(2026, 11, day),
        createdAt = day * 1000L,
        updatedAt = day * 1000L
    )

    @Test
    fun calculatesIncomeExpenseAndNet() {
        val summary = FinanceCalculations.monthlySummary(
            listOf(
                tx(TransactionType.INCOME, 10_000_00, 1),
                tx(TransactionType.EXPENSE, 2_500_00, 2),
                tx(TransactionType.EXPENSE, 500_00, 3),
                tx(TransactionType.INCOME, 1_000_00, 1).copy(
                    transactionDate = LocalDate.of(2026, 10, 1)
                )
            ),
            month
        )
        assertEquals(10_000_00L, summary.totalIncomeMinor)
        assertEquals(3_000_00L, summary.totalExpenseMinor)
        assertEquals(7_000_00L, summary.netMinor)
    }

    @Test
    fun budgetProgressStatusesAndZeroSafety() {
        assertEquals(BudgetStatus.UNSET, FinanceCalculations.budgetProgress(null, 100).status)
        assertEquals(BudgetStatus.UNSET, FinanceCalculations.budgetProgress(0, 100).status)
        assertNull(FinanceCalculations.budgetProgress(null, 100).utilizationPercent)

        assertEquals(BudgetStatus.WITHIN, FinanceCalculations.budgetProgress(1000, 600).status)
        assertEquals(BudgetStatus.APPROACHING, FinanceCalculations.budgetProgress(1000, 750).status)
        assertEquals(BudgetStatus.NEAR_LIMIT, FinanceCalculations.budgetProgress(1000, 950).status)
        val exceeded = FinanceCalculations.budgetProgress(1000, 1200)
        assertEquals(BudgetStatus.EXCEEDED, exceeded.status)
        assertEquals(-200L, exceeded.remainingMinor)
        assertEquals(120.0, exceeded.utilizationPercent!!, 0.01)
    }

    @Test
    fun categoryAggregationPercentages() {
        val food = Category(1, "Food", TransactionType.EXPENSE, "restaurant", true)
        val travel = Category(2, "Travel", TransactionType.EXPENSE, "flight", true)
        val shares = FinanceCalculations.categoryExpenseShares(
            listOf(
                TransactionWithCategory(tx(TransactionType.EXPENSE, 750, 1, 1), food),
                TransactionWithCategory(tx(TransactionType.EXPENSE, 250, 2, 2), travel),
                TransactionWithCategory(
                    tx(TransactionType.INCOME, 5000, 3, 101),
                    Category(101, "Salary", TransactionType.INCOME, "work", true)
                )
            ),
            month
        )
        assertEquals(2, shares.size)
        assertEquals(75.0, shares.first { it.category.id == 1L }.percentOfTotal, 0.01)
        assertEquals(25.0, shares.first { it.category.id == 2L }.percentOfTotal, 0.01)
        assertTrue(FinanceCalculations.categoryExpenseShares(emptyList(), month).isEmpty())
    }
}
