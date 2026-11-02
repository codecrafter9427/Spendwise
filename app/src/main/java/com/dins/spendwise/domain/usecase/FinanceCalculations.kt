package com.dins.spendwise.domain.usecase

import com.dins.spendwise.domain.model.BudgetStatus
import com.dins.spendwise.domain.model.Category
import com.dins.spendwise.domain.model.Transaction
import com.dins.spendwise.domain.model.TransactionType
import com.dins.spendwise.domain.model.TransactionWithCategory
import java.time.YearMonth

data class MonthlySummary(
    val totalIncomeMinor: Long,
    val totalExpenseMinor: Long,
    val netMinor: Long
)

data class BudgetProgress(
    val budgetMinor: Long?,
    val expenseMinor: Long,
    val remainingMinor: Long?,
    val utilizationPercent: Double?,
    val status: BudgetStatus
)

data class CategoryExpenseShare(
    val category: Category,
    val amountMinor: Long,
    val percentOfTotal: Double
)

object FinanceCalculations {

    fun monthlySummary(
        transactions: List<Transaction>,
        yearMonth: YearMonth
    ): MonthlySummary {
        val inMonth = transactions.filter { YearMonth.from(it.transactionDate) == yearMonth }
        val income = inMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
        val expense = inMonth.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
        return MonthlySummary(
            totalIncomeMinor = income,
            totalExpenseMinor = expense,
            netMinor = income - expense
        )
    }

    fun budgetProgress(budgetMinor: Long?, expenseMinor: Long): BudgetProgress {
        if (budgetMinor == null || budgetMinor <= 0L) {
            return BudgetProgress(
                budgetMinor = budgetMinor,
                expenseMinor = expenseMinor,
                remainingMinor = null,
                utilizationPercent = null,
                status = BudgetStatus.UNSET
            )
        }
        val remaining = budgetMinor - expenseMinor
        val utilization = (expenseMinor.toDouble() / budgetMinor.toDouble()) * 100.0
        val status = when {
            utilization < 70.0 -> BudgetStatus.WITHIN
            utilization < 90.0 -> BudgetStatus.APPROACHING
            utilization <= 100.0 -> BudgetStatus.NEAR_LIMIT
            else -> BudgetStatus.EXCEEDED
        }
        return BudgetProgress(
            budgetMinor = budgetMinor,
            expenseMinor = expenseMinor,
            remainingMinor = remaining,
            utilizationPercent = utilization,
            status = status
        )
    }

    fun categoryExpenseShares(
        transactions: List<TransactionWithCategory>,
        yearMonth: YearMonth
    ): List<CategoryExpenseShare> {
        val expenses = transactions.filter {
            it.transaction.type == TransactionType.EXPENSE &&
                YearMonth.from(it.transaction.transactionDate) == yearMonth
        }
        val total = expenses.sumOf { it.transaction.amountMinor }
        if (total == 0L) return emptyList()

        return expenses
            .groupBy { it.category.id }
            .map { (_, items) ->
                val category = items.first().category
                val amount = items.sumOf { it.transaction.amountMinor }
                CategoryExpenseShare(
                    category = category,
                    amountMinor = amount,
                    percentOfTotal = (amount.toDouble() / total.toDouble()) * 100.0
                )
            }
            .sortedByDescending { it.amountMinor }
    }
}
