package com.dins.spendwise.domain.model

data class MonthlyBudget(
    val id: Long = 0L,
    val year: Int,
    val month: Int,
    val amountMinor: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val remoteId: String? = null,
    val serverVersion: Int = 1
)
