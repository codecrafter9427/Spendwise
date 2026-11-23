package com.dins.spendwise.data.sync

import com.dins.spendwise.data.local.dao.CategoryDao
import com.dins.spendwise.data.local.dao.MonthlyBudgetDao
import com.dins.spendwise.data.local.dao.OutboxDao
import com.dins.spendwise.data.local.dao.TransactionDao
import com.dins.spendwise.data.local.entity.CategoryEntity
import com.dins.spendwise.data.local.entity.MonthlyBudgetEntity
import com.dins.spendwise.data.local.entity.OutboxEntity
import com.dins.spendwise.data.local.entity.TransactionEntity
import com.dins.spendwise.data.remote.AuthRepository
import com.dins.spendwise.data.remote.SpendWiseApi
import com.dins.spendwise.data.remote.dto.BudgetDto
import com.dins.spendwise.data.remote.dto.SyncChangeDto
import com.dins.spendwise.data.remote.dto.SyncPushOperationDto
import com.dins.spendwise.data.remote.dto.SyncPushRequestDto
import com.dins.spendwise.data.remote.dto.TransactionDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

data class SyncRunResult(
    val pushed: Int,
    val pulled: Int,
    val conflicts: Int,
    val rejected: Int
)

@Singleton
class SyncEngine @Inject constructor(
    private val api: SpendWiseApi,
    private val authRepository: AuthRepository,
    private val outboxDao: OutboxDao,
    private val transactionDao: TransactionDao,
    private val monthlyBudgetDao: MonthlyBudgetDao,
    private val categoryDao: CategoryDao,
    private val cursorStore: SyncCursorStore,
    private val moshi: Moshi
) {
    private val mapAdapter = moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )

    suspend fun syncOnce(batchSize: Int = 50): SyncRunResult {
        if (authRepository.validAccessToken() == null) {
            return SyncRunResult(0, 0, 0, 0)
        }
        val pushStats = pushPending(batchSize)
        // Snapshot covers rows created before sync_changes existed.
        val hydrated = hydrateFromRest()
        val pulled = pullAll()
        return SyncRunResult(
            pushed = pushStats.accepted,
            pulled = hydrated + pulled,
            conflicts = pushStats.conflicts,
            rejected = pushStats.rejected
        )
    }

    private data class PushStats(val accepted: Int, val conflicts: Int, val rejected: Int)

    private suspend fun pushPending(batchSize: Int): PushStats {
        val pending = outboxDao.getByStatus(OutboxEntity.STATUS_PENDING, batchSize)
        if (pending.isEmpty()) return PushStats(0, 0, 0)

        val operations = pending.map { row ->
            SyncPushOperationDto(
                operationId = row.operationId,
                entityType = row.entityType,
                entityId = row.entityId,
                operation = row.operation,
                baseVersion = row.baseVersion,
                payload = row.payloadJson?.let { mapAdapter.fromJson(it) }
            )
        }
        val response = api.syncPush(SyncPushRequestDto(operations))
        var accepted = 0
        var conflicts = 0
        var rejected = 0
        val now = System.currentTimeMillis()

        response.results.forEach { result ->
            val row = pending.firstOrNull { it.operationId == result.operationId } ?: return@forEach
            when (result.status) {
                "accepted", "duplicate" -> {
                    accepted += 1
                    applyAck(row, result.version)
                    outboxDao.deleteByOperationId(row.operationId)
                }
                "conflict" -> {
                    conflicts += 1
                    outboxDao.update(
                        row.copy(
                            status = OutboxEntity.STATUS_CONFLICT,
                            attempts = row.attempts + 1,
                            lastError = result.errorMessage ?: result.errorCode,
                            updatedAt = now
                        )
                    )
                }
                else -> {
                    rejected += 1
                    outboxDao.update(
                        row.copy(
                            status = OutboxEntity.STATUS_REJECTED,
                            attempts = row.attempts + 1,
                            lastError = result.errorMessage ?: result.errorCode,
                            updatedAt = now
                        )
                    )
                }
            }
        }
        return PushStats(accepted, conflicts, rejected)
    }

    private suspend fun applyAck(row: OutboxEntity, version: Int?) {
        if (version == null) return
        when (row.entityType) {
            OutboxEntity.ENTITY_TRANSACTION -> {
                val local = transactionDao.getByRemoteId(row.entityId) ?: return
                transactionDao.update(local.copy(serverVersion = version, updatedAt = System.currentTimeMillis()))
            }
            OutboxEntity.ENTITY_BUDGET -> {
                val local = monthlyBudgetDao.getByRemoteId(row.entityId) ?: return
                monthlyBudgetDao.update(local.copy(serverVersion = version, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    private suspend fun hydrateFromRest(): Int {
        var count = 0
        ensureRemoteCategories()
        var cursor: String? = null
        var pages = 0
        while (pages < 20) {
            pages += 1
            val page = api.listTransactions(limit = 100, cursor = cursor)
            page.items.forEach { dto ->
                applyTransactionDto(dto)
                count += 1
            }
            cursor = page.nextCursor
            if (cursor.isNullOrBlank() || page.items.isEmpty()) break
        }
        api.listBudgets().forEach { dto ->
            applyBudgetDto(dto)
            count += 1
        }
        return count
    }

    private suspend fun pullAll(): Int {
        var pulled = 0
        var safety = 0
        while (safety < 20) {
            safety += 1
            val cursor = cursorStore.getCursor()
            val page = api.syncPull(cursor = cursor, limit = 100)
            page.changes.forEach { change ->
                applyPulledChange(change)
                pulled += 1
            }
            if (page.nextCursor != null) {
                cursorStore.setCursor(page.nextCursor)
            } else if (cursor == null && page.changes.isEmpty()) {
                // Mark caught-up so later changelog rows (sequence > 0) are pulled.
                cursorStore.setCursor("0")
            }
            if (!page.hasMore || page.changes.isEmpty()) break
        }
        return pulled
    }

    private suspend fun applyPulledChange(change: SyncChangeDto) {
        when (change.entityType) {
            OutboxEntity.ENTITY_TRANSACTION -> applyTransactionChange(change)
            OutboxEntity.ENTITY_BUDGET -> applyBudgetChange(change)
        }
    }

    private suspend fun applyTransactionChange(change: SyncChangeDto) {
        val existing = transactionDao.getByRemoteId(change.entityId)
        if (existing != null && existing.serverVersion > change.version) return
        if (existing != null && existing.serverVersion == change.version && change.operation != "delete") {
            return
        }

        val payload = change.payload.orEmpty()
        if (change.operation == "delete") {
            val now = System.currentTimeMillis()
            if (existing != null) {
                transactionDao.update(
                    existing.copy(
                        deletedAt = existing.deletedAt ?: now,
                        serverVersion = change.version,
                        updatedAt = now
                    )
                )
            }
            return
        }

        val categoryRemote = payload["category_id"]?.toString() ?: return
        val category = ensureCategory(categoryRemote) ?: return
        val amount = numberAsLong(payload["amount_minor"]) ?: return
        val type = payload["type"]?.toString()?.uppercase() ?: return
        val date = payload["transaction_date"]?.toString()?.let { LocalDate.parse(it) } ?: return
        val description = payload["description"] as? String
        val deletedAt = payload["deleted_at"]?.toString()?.let { parseIsoMillis(it) }
        upsertLocalTransaction(
            remoteId = change.entityId,
            type = type,
            amountMinor = amount,
            categoryId = category.id,
            description = description,
            transactionDate = date.toEpochDay(),
            serverVersion = change.version,
            deletedAt = deletedAt,
            existing = existing
        )
    }

    private suspend fun applyTransactionDto(dto: TransactionDto) {
        val existing = transactionDao.getByRemoteId(dto.id)
        if (existing != null && existing.serverVersion > dto.version) return
        val category = ensureCategory(dto.categoryId) ?: return
        val date = LocalDate.parse(dto.transactionDate)
        upsertLocalTransaction(
            remoteId = dto.id,
            type = dto.type.uppercase(),
            amountMinor = dto.amountMinor,
            categoryId = category.id,
            description = dto.description,
            transactionDate = date.toEpochDay(),
            serverVersion = dto.version,
            deletedAt = dto.deletedAt?.let { parseIsoMillis(it) },
            existing = existing
        )
    }

    private suspend fun upsertLocalTransaction(
        remoteId: String,
        type: String,
        amountMinor: Long,
        categoryId: Long,
        description: String?,
        transactionDate: Long,
        serverVersion: Int,
        deletedAt: Long?,
        existing: TransactionEntity?
    ) {
        val now = System.currentTimeMillis()
        if (existing == null) {
            transactionDao.insert(
                TransactionEntity(
                    type = type,
                    amountMinor = amountMinor,
                    categoryId = categoryId,
                    description = description,
                    transactionDate = transactionDate,
                    createdAt = now,
                    updatedAt = now,
                    remoteId = remoteId,
                    serverVersion = serverVersion,
                    deletedAt = deletedAt
                )
            )
        } else {
            transactionDao.update(
                existing.copy(
                    type = type,
                    amountMinor = amountMinor,
                    categoryId = categoryId,
                    description = description,
                    transactionDate = transactionDate,
                    updatedAt = now,
                    serverVersion = serverVersion,
                    deletedAt = deletedAt
                )
            )
        }
    }

    private suspend fun applyBudgetChange(change: SyncChangeDto) {
        val payload = change.payload.orEmpty()
        val year = numberAsInt(payload["year"]) ?: return
        val month = numberAsInt(payload["month"]) ?: return
        val amount = numberAsLong(payload["amount_minor"]) ?: return
        upsertLocalBudget(
            remoteId = change.entityId,
            year = year,
            month = month,
            amountMinor = amount,
            serverVersion = change.version
        )
    }

    private suspend fun applyBudgetDto(dto: BudgetDto) {
        upsertLocalBudget(
            remoteId = dto.id,
            year = dto.year,
            month = dto.month,
            amountMinor = dto.amountMinor,
            serverVersion = dto.version
        )
    }

    private suspend fun upsertLocalBudget(
        remoteId: String,
        year: Int,
        month: Int,
        amountMinor: Long,
        serverVersion: Int
    ) {
        val now = System.currentTimeMillis()
        val byRemote = monthlyBudgetDao.getByRemoteId(remoteId)
        val byMonth = monthlyBudgetDao.getByYearMonth(year, month)
        val existing = byRemote ?: byMonth
        if (existing != null && existing.serverVersion > serverVersion) return

        if (existing == null) {
            monthlyBudgetDao.insert(
                MonthlyBudgetEntity(
                    year = year,
                    month = month,
                    amountMinor = amountMinor,
                    createdAt = now,
                    updatedAt = now,
                    remoteId = remoteId,
                    serverVersion = serverVersion
                )
            )
        } else {
            monthlyBudgetDao.update(
                existing.copy(
                    year = year,
                    month = month,
                    amountMinor = amountMinor,
                    updatedAt = now,
                    remoteId = remoteId,
                    serverVersion = serverVersion
                )
            )
        }
    }

    private suspend fun ensureRemoteCategories() {
        api.listCategories().forEach { remote ->
            val existing = categoryDao.getByRemoteId(remote.id)
            if (existing != null) return@forEach
            val byName = categoryDao.observeAllOnce()
                .firstOrNull {
                    it.name.equals(remote.name, ignoreCase = true) &&
                        it.type.equals(remote.type, ignoreCase = true)
                }
            if (byName != null) {
                if (byName.remoteId.isNullOrBlank()) {
                    categoryDao.update(byName.copy(remoteId = remote.id))
                }
            } else {
                val nextId = categoryDao.maxId() + 1
                categoryDao.insert(
                    CategoryEntity(
                        id = nextId,
                        name = remote.name,
                        type = remote.type.uppercase(),
                        icon = remote.iconKey,
                        isDefault = remote.isDefault,
                        remoteId = remote.id
                    )
                )
            }
        }
    }

    private suspend fun ensureCategory(remoteId: String): CategoryEntity? {
        categoryDao.getByRemoteId(remoteId)?.let { return it }
        ensureRemoteCategories()
        return categoryDao.getByRemoteId(remoteId)
    }

    private fun numberAsLong(value: Any?): Long? = when (value) {
        null -> null
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }

    private fun numberAsInt(value: Any?): Int? = numberAsLong(value)?.toInt()

    private fun parseIsoMillis(value: String): Long? =
        runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrNull()
            ?: runCatching { java.time.Instant.parse(value).toEpochMilli() }.getOrNull()
}
