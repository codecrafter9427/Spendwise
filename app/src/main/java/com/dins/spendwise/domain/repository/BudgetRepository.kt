package com.dins.spendwise.domain.repository

import com.dins.spendwise.domain.model.MonthlyBudget
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeBudget(yearMonth: YearMonth): Flow<MonthlyBudget?>
    suspend fun setBudget(yearMonth: YearMonth, amountMinor: Long): Result<Unit>
}
