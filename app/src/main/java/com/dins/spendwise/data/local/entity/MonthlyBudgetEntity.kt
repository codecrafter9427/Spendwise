package com.dins.spendwise.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "monthly_budgets",
    indices = [Index(value = ["year", "month"], unique = true)]
)
data class MonthlyBudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val year: Int,
    val month: Int,
    val amountMinor: Long,
    val createdAt: Long,
    val updatedAt: Long
)
