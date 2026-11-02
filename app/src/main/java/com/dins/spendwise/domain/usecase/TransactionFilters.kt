package com.dins.spendwise.domain.usecase

import com.dins.spendwise.domain.model.TransactionType
import com.dins.spendwise.domain.model.TransactionWithCategory
import java.time.YearMonth

object TransactionFilters {

    fun filter(
        items: List<TransactionWithCategory>,
        yearMonth: YearMonth?,
        type: TransactionType?,
        categoryId: Long?,
        query: String
    ): List<TransactionWithCategory> {
        val normalizedQuery = query.trim()
        return items
            .asSequence()
            .filter { item ->
                yearMonth == null || YearMonth.from(item.transaction.transactionDate) == yearMonth
            }
            .filter { item ->
                type == null || item.transaction.type == type
            }
            .filter { item ->
                categoryId == null || item.transaction.categoryId == categoryId
            }
            .filter { item ->
                if (normalizedQuery.isEmpty()) {
                    true
                } else {
                    val desc = item.transaction.description.orEmpty()
                    item.category.name.contains(normalizedQuery, ignoreCase = true) ||
                        desc.contains(normalizedQuery, ignoreCase = true)
                }
            }
            .sortedWith(
                compareByDescending<TransactionWithCategory> { it.transaction.transactionDate }
                    .thenByDescending { it.transaction.createdAt }
            )
            .toList()
    }
}
