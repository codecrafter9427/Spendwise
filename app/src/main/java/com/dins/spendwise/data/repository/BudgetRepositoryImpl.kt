package com.dins.spendwise.data.repository

import com.dins.spendwise.data.local.dao.MonthlyBudgetDao
import com.dins.spendwise.data.local.entity.MonthlyBudgetEntity
import com.dins.spendwise.data.mapper.toDomain
import com.dins.spendwise.data.remote.AuthRepository
import com.dins.spendwise.data.remote.SpendWiseApi
import com.dins.spendwise.data.remote.dto.BudgetUpsertDto
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
    private val authRepository: AuthRepository,
    private val api: SpendWiseApi
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

            if (authRepository.validAccessToken() != null) {
                val response = api.upsertBudget(
                    year = local.year,
                    month = local.month,
                    body = BudgetUpsertDto(
                        amountMinor = local.amountMinor,
                        expectedVersion = local.remoteId?.let { local.serverVersion }
                    )
                )
                monthlyBudgetDao.update(
                    local.copy(
                        remoteId = response.id,
                        serverVersion = response.version,
                        amountMinor = response.amountMinor,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            Unit
        }
}
