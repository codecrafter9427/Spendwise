package com.dins.spendwise.domain.model

import java.time.LocalDate

data class Transaction(
    val id: Long = 0L,
    val type: TransactionType,
    val amountMinor: Long,
    val categoryId: Long,
    val description: String?,
    val transactionDate: LocalDate,
    val createdAt: Long,
    val updatedAt: Long,
    val remoteId: String? = null,
    val serverVersion: Int = 1
)

data class TransactionWithCategory(
    val transaction: Transaction,
    val category: Category
)
