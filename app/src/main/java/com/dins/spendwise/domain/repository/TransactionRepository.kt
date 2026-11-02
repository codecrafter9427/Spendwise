package com.dins.spendwise.domain.repository

import com.dins.spendwise.domain.model.Transaction
import com.dins.spendwise.domain.model.TransactionType
import com.dins.spendwise.domain.model.TransactionWithCategory
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeAllWithCategory(): Flow<List<TransactionWithCategory>>
    fun observeByMonthWithCategory(yearMonth: YearMonth): Flow<List<TransactionWithCategory>>
    fun observeRecentByMonth(yearMonth: YearMonth, limit: Int): Flow<List<TransactionWithCategory>>
    fun observeById(id: Long): Flow<TransactionWithCategory?>
    fun observeSumByTypeAndMonth(type: TransactionType, yearMonth: YearMonth): Flow<Long>
    suspend fun add(transaction: Transaction): Result<Long>
    suspend fun update(transaction: Transaction): Result<Unit>
    suspend fun delete(id: Long): Result<Unit>
}
