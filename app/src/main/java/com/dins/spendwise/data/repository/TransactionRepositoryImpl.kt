package com.dins.spendwise.data.repository

import com.dins.spendwise.core.utils.DateUtils
import com.dins.spendwise.data.local.dao.CategoryDao
import com.dins.spendwise.data.local.dao.TransactionDao
import com.dins.spendwise.data.mapper.toDomain
import com.dins.spendwise.data.mapper.toEntity
import com.dins.spendwise.data.remote.AuthRepository
import com.dins.spendwise.data.remote.SpendWiseApi
import com.dins.spendwise.data.remote.dto.TransactionCreateDto
import com.dins.spendwise.data.remote.dto.TransactionUpdateDto
import com.dins.spendwise.domain.model.Transaction
import com.dins.spendwise.domain.model.TransactionType
import com.dins.spendwise.domain.model.TransactionWithCategory
import com.dins.spendwise.domain.repository.TransactionRepository
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val authRepository: AuthRepository,
    private val api: SpendWiseApi
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
        val remoteId = transaction.remoteId ?: UUID.randomUUID().toString()
        val localId = transactionDao.insert(
            transaction.copy(remoteId = remoteId, serverVersion = 1).toEntity()
        )
        pushCreateIfSignedIn(localId, remoteId)
        localId
    }

    override suspend fun update(transaction: Transaction): Result<Unit> = runCatching {
        val existing = transactionDao.getById(transaction.id) ?: error("Transaction not found")
        val remoteId = transaction.remoteId ?: existing.remoteId ?: UUID.randomUUID().toString()
        val rows = transactionDao.update(
            transaction.copy(remoteId = remoteId).toEntity()
        )
        if (rows == 0) error("Transaction not found")
        pushUpdateIfSignedIn(transaction.id)
    }

    override suspend fun delete(id: Long): Result<Unit> = runCatching {
        val entity = transactionDao.getById(id) ?: error("Transaction not found")
        if (authRepository.validAccessToken() != null && !entity.remoteId.isNullOrBlank()) {
            runCatching {
                api.deleteTransaction(entity.remoteId, entity.serverVersion)
            }
        }
        val rows = transactionDao.delete(entity)
        if (rows == 0) error("Failed to delete transaction")
    }

    private suspend fun pushCreateIfSignedIn(localId: Long, remoteId: String) {
        if (authRepository.validAccessToken() == null) return
        val entity = transactionDao.getById(localId) ?: return
        val categoryRemote = categoryDao.getById(entity.categoryId)?.remoteId ?: return
        val response = api.createTransaction(
            TransactionCreateDto(
                id = remoteId,
                type = entity.type.lowercase(),
                amountMinor = entity.amountMinor,
                categoryId = categoryRemote,
                description = entity.description,
                transactionDate = java.time.LocalDate.ofEpochDay(entity.transactionDate).toString()
            )
        )
        transactionDao.update(
            entity.copy(remoteId = response.id, serverVersion = response.version)
        )
    }

    private suspend fun pushUpdateIfSignedIn(localId: Long) {
        if (authRepository.validAccessToken() == null) return
        val entity = transactionDao.getById(localId) ?: return
        val remoteId = entity.remoteId ?: return
        val categoryRemote = categoryDao.getById(entity.categoryId)?.remoteId ?: return
        val response = api.updateTransaction(
            remoteId,
            TransactionUpdateDto(
                expectedVersion = entity.serverVersion,
                type = entity.type.lowercase(),
                amountMinor = entity.amountMinor,
                categoryId = categoryRemote,
                description = entity.description,
                transactionDate = java.time.LocalDate.ofEpochDay(entity.transactionDate).toString()
            )
        )
        transactionDao.update(
            entity.copy(remoteId = response.id, serverVersion = response.version)
        )
    }
}
