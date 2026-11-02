package com.dins.spendwise.data.repository

import com.dins.spendwise.core.utils.DateUtils
import com.dins.spendwise.data.local.dao.TransactionDao
import com.dins.spendwise.data.mapper.toDomain
import com.dins.spendwise.data.mapper.toEntity
import com.dins.spendwise.domain.model.Transaction
import com.dins.spendwise.domain.model.TransactionType
import com.dins.spendwise.domain.model.TransactionWithCategory
import com.dins.spendwise.domain.repository.TransactionRepository
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun observeAllWithCategory(): Flow<List<TransactionWithCategory>> =
        transactionDao.observeAllWithCategory().map { list -> list.map { it.toDomain() } }

    override fun observeByMonthWithCategory(yearMonth: YearMonth): Flow<List<TransactionWithCategory>> =
        transactionDao.observeByMonthWithCategory(
            DateUtils.startEpochDay(yearMonth),
            DateUtils.endEpochDayInclusive(yearMonth)
        ).map { list -> list.map { it.toDomain() } }

    override fun observeRecentByMonth(
        yearMonth: YearMonth,
        limit: Int
    ): Flow<List<TransactionWithCategory>> =
        transactionDao.observeRecentByMonthWithCategory(
            DateUtils.startEpochDay(yearMonth),
            DateUtils.endEpochDayInclusive(yearMonth),
            limit
        ).map { list -> list.map { it.toDomain() } }

    override fun observeById(id: Long): Flow<TransactionWithCategory?> =
        transactionDao.observeByIdWithCategory(id).map { it?.toDomain() }

    override fun observeSumByTypeAndMonth(
        type: TransactionType,
        yearMonth: YearMonth
    ): Flow<Long> =
        transactionDao.observeSumByTypeAndMonth(
            type.name,
            DateUtils.startEpochDay(yearMonth),
            DateUtils.endEpochDayInclusive(yearMonth)
        )

    override suspend fun add(transaction: Transaction): Result<Long> = runCatching {
        transactionDao.insert(transaction.toEntity())
    }

    override suspend fun update(transaction: Transaction): Result<Unit> = runCatching {
        val rows = transactionDao.update(transaction.toEntity())
        if (rows == 0) error("Transaction not found")
    }

    override suspend fun delete(id: Long): Result<Unit> = runCatching {
        val entity = transactionDao.getById(id) ?: error("Transaction not found")
        val rows = transactionDao.delete(entity)
        if (rows == 0) error("Failed to delete transaction")
    }
}
