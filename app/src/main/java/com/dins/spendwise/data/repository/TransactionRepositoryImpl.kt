package com.dins.spendwise.data.repository

import com.dins.spendwise.core.utils.DateUtils
import com.dins.spendwise.data.local.dao.CategoryDao
import com.dins.spendwise.data.local.dao.TransactionDao
import com.dins.spendwise.data.mapper.toDomain
import com.dins.spendwise.data.mapper.toEntity
import com.dins.spendwise.data.sync.OutboxWriter
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
    private val outboxWriter: OutboxWriter
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
            transaction.copy(remoteId = remoteId, serverVersion = 1, deletedAt = null).toEntity()
        )
        enqueueUpsert(localId)
        localId
    }

    override suspend fun update(transaction: Transaction): Result<Unit> = runCatching {
        val existing = transactionDao.getById(transaction.id) ?: error("Transaction not found")
        val remoteId = transaction.remoteId ?: existing.remoteId ?: UUID.randomUUID().toString()
        val rows = transactionDao.update(
            transaction.copy(remoteId = remoteId, deletedAt = existing.deletedAt).toEntity()
        )
        if (rows == 0) error("Transaction not found")
        enqueueUpsert(transaction.id)
    }

    override suspend fun delete(id: Long): Result<Unit> = runCatching {
        val entity = transactionDao.getById(id) ?: error("Transaction not found")
        val remoteId = entity.remoteId ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val rows = transactionDao.update(
            entity.copy(
                remoteId = remoteId,
                deletedAt = now,
                updatedAt = now
            )
        )
        if (rows == 0) error("Failed to delete transaction")
        outboxWriter.enqueueTransactionDelete(
            remoteId = remoteId,
            baseVersion = entity.serverVersion
        )
    }

    private suspend fun enqueueUpsert(localId: Long) {
        val entity = transactionDao.getById(localId) ?: return
        val remoteId = entity.remoteId ?: return
        val categoryRemote = categoryDao.getById(entity.categoryId)?.remoteId ?: return
        // New local rows start at serverVersion=1 before first ack; omit base until synced once.
        val baseVersion = entity.serverVersion.takeIf { entity.updatedAt > entity.createdAt || entity.serverVersion > 1 }
        outboxWriter.enqueueTransactionUpsert(
            remoteId = remoteId,
            baseVersion = baseVersion,
            payload = mapOf(
                "type" to entity.type.lowercase(),
                "amount_minor" to entity.amountMinor,
                "currency" to "INR",
                "category_id" to categoryRemote,
                "description" to entity.description,
                "transaction_date" to java.time.LocalDate.ofEpochDay(entity.transactionDate).toString()
            )
        )
    }
}
