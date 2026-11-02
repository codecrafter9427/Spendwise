package com.dins.spendwise.data.repository

import com.dins.spendwise.data.local.dao.MonthlyBudgetDao
import com.dins.spendwise.data.local.entity.MonthlyBudgetEntity
import com.dins.spendwise.data.mapper.toDomain
import com.dins.spendwise.domain.model.MonthlyBudget
import com.dins.spendwise.domain.repository.BudgetRepository
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val monthlyBudgetDao: MonthlyBudgetDao
) : BudgetRepository {

    override fun observeBudget(yearMonth: YearMonth): Flow<MonthlyBudget?> =
        monthlyBudgetDao.observeByYearMonth(yearMonth.year, yearMonth.monthValue)
            .map { it?.toDomain() }

    override suspend fun setBudget(yearMonth: YearMonth, amountMinor: Long): Result<Unit> =
        runCatching {
            val now = System.currentTimeMillis()
            val existing = monthlyBudgetDao.getByYearMonth(yearMonth.year, yearMonth.monthValue)
            if (existing == null) {
                monthlyBudgetDao.insert(
                    MonthlyBudgetEntity(
                        year = yearMonth.year,
                        month = yearMonth.monthValue,
                        amountMinor = amountMinor,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            } else {
                monthlyBudgetDao.update(
                    existing.copy(
                        amountMinor = amountMinor,
                        updatedAt = now
                    )
                )
            }
            Unit
        }
}
