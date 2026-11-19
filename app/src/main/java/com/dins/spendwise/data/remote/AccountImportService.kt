package com.dins.spendwise.data.remote

import com.dins.spendwise.data.local.dao.CategoryDao
import com.dins.spendwise.data.local.dao.MonthlyBudgetDao
import com.dins.spendwise.data.local.dao.TransactionDao
import com.dins.spendwise.data.remote.dto.BudgetUpsertDto
import com.dins.spendwise.data.remote.dto.TransactionCreateDto
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ImportResult(
    val importedTransactions: Int,
    val skippedTransactions: Int,
    val failedTransactions: Int,
    val importedBudgets: Int,
    val failedBudgets: Int
)

@Singleton
class AccountImportService @Inject constructor(
    private val tokenStore: AuthTokenStore,
    private val api: SpendWiseApi,
    private val transactionDao: TransactionDao,
    private val budgetDao: MonthlyBudgetDao,
    private val categoryDao: CategoryDao
) {
    suspend fun importIfNeeded(force: Boolean = false): Result<ImportResult> = runCatching {
        if (!force && tokenStore.isImportDone()) {
            return@runCatching ImportResult(0, 0, 0, 0, 0)
        }
        // Ensure profile row exists server-side.
        api.getMe()

        var importedTx = 0
        var skippedTx = 0
        var failedTx = 0
        val categories = categoryDao.observeAllOnce().associateBy { it.id }

        transactionDao.getAll().forEach { entity ->
            val category = categories[entity.categoryId]
            val categoryRemoteId = category?.remoteId
            if (categoryRemoteId.isNullOrBlank()) {
                failedTx += 1
                return@forEach
            }
            val remoteId = entity.remoteId ?: UUID.randomUUID().toString()
            if (entity.remoteId == null) {
                transactionDao.update(entity.copy(remoteId = remoteId))
            }
            try {
                val response = api.createTransaction(
                    TransactionCreateDto(
                        id = remoteId,
                        type = entity.type.lowercase(),
                        amountMinor = entity.amountMinor,
                        categoryId = categoryRemoteId,
                        description = entity.description,
                        transactionDate = java.time.LocalDate.ofEpochDay(entity.transactionDate)
                            .toString()
                    )
                )
                transactionDao.update(
                    entity.copy(
                        remoteId = response.id,
                        serverVersion = response.version
                    )
                )
                importedTx += 1
            } catch (_: Exception) {
                // Idempotent replay of same id counts as skip.
                skippedTx += 1
            }
        }

        var importedBudgets = 0
        var failedBudgets = 0
        budgetDao.getAll().forEach { budget ->
            try {
                val response = api.upsertBudget(
                    year = budget.year,
                    month = budget.month,
                    body = BudgetUpsertDto(
                        amountMinor = budget.amountMinor,
                        expectedVersion = budget.remoteId?.let { budget.serverVersion }
                    )
                )
                budgetDao.update(
                    budget.copy(
                        remoteId = response.id,
                        serverVersion = response.version,
                        amountMinor = response.amountMinor
                    )
                )
                importedBudgets += 1
            } catch (_: Exception) {
                failedBudgets += 1
            }
        }

        tokenStore.setImportDone(true)
        ImportResult(
            importedTransactions = importedTx,
            skippedTransactions = skippedTx,
            failedTransactions = failedTx,
            importedBudgets = importedBudgets,
            failedBudgets = failedBudgets
        )
    }
}
