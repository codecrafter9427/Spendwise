package com.dins.spendwise.data.repository

import com.dins.spendwise.data.local.dao.MonthlyBudgetDao
import com.dins.spendwise.data.local.entity.MonthlyBudgetEntity
import com.dins.spendwise.data.mapper.toDomain
import com.dins.spendwise.data.sync.OutboxWriter
import com.dins.spendwise.domain.model.MonthlyBudget
import com.dins.spendwise.domain.repository.BudgetRepository
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val monthlyBudgetDao: MonthlyBudgetDao,
    private val outboxWriter: OutboxWriter
) : BudgetRepository {

    override fun observeBudget(yearMonth: YearMonth): Flow<MonthlyBudget?> =
        monthlyBudgetDao.observeByYearMonth(yearMonth.year, yearMonth.monthValue)
            .map { it?.toDomain() }

    override suspend fun setBudget(yearMonth: YearMonth, amountMinor: Long): Result<Unit> =
        runCatching {
            val now = System.currentTimeMillis()
            val existing = monthlyBudgetDao.getByYearMonth(yearMonth.year, yearMonth.monthValue)
            val local = if (existing == null) {
                val id = monthlyBudgetDao.insert(
                    MonthlyBudgetEntity(
                        year = yearMonth.year,
                        month = yearMonth.monthValue,
                        amountMinor = amountMinor,
                        createdAt = now,
                        updatedAt = now,
                        remoteId = UUID.randomUUID().toString(),
                        serverVersion = 1
                    )
                )
                monthlyBudgetDao.getByYearMonth(yearMonth.year, yearMonth.monthValue)
                    ?: error("Budget insert failed ($id)")
            } else {
                monthlyBudgetDao.update(
                    existing.copy(
                        amountMinor = amountMinor,
                        updatedAt = now,
                        remoteId = existing.remoteId ?: UUID.randomUUID().toString()
                    )
                )
                monthlyBudgetDao.getByYearMonth(yearMonth.year, yearMonth.monthValue)
                    ?: error("Budget update failed")
            }

            val remoteId = local.remoteId ?: return@runCatching
            outboxWriter.enqueueBudgetUpsert(
                remoteId = remoteId,
                baseVersion = existing?.serverVersion,
                payload = mapOf(
                    "year" to local.year,
                    "month" to local.month,
                    "amount_minor" to local.amountMinor,
                    "currency" to "INR"
                )
            )
        }
}
