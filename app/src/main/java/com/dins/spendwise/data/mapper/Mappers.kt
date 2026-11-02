package com.dins.spendwise.data.mapper

import com.dins.spendwise.data.local.entity.CategoryEntity
import com.dins.spendwise.data.local.entity.MonthlyBudgetEntity
import com.dins.spendwise.data.local.entity.TransactionEntity
import com.dins.spendwise.data.local.entity.TransactionWithCategoryEntity
import com.dins.spendwise.domain.model.Category
import com.dins.spendwise.domain.model.MonthlyBudget
import com.dins.spendwise.domain.model.Transaction
import com.dins.spendwise.domain.model.TransactionType
import com.dins.spendwise.domain.model.TransactionWithCategory
import java.time.LocalDate

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    type = TransactionType.valueOf(type),
    icon = icon,
    isDefault = isDefault
)

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    type = TransactionType.valueOf(type),
    amountMinor = amountMinor,
    categoryId = categoryId,
    description = description,
    transactionDate = LocalDate.ofEpochDay(transactionDate),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    type = type.name,
    amountMinor = amountMinor,
    categoryId = categoryId,
    description = description,
    transactionDate = transactionDate.toEpochDay(),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TransactionWithCategoryEntity.toDomain(): TransactionWithCategory =
    TransactionWithCategory(
        transaction = transaction.toDomain(),
        category = category.toDomain()
    )

fun MonthlyBudgetEntity.toDomain(): MonthlyBudget = MonthlyBudget(
    id = id,
    year = year,
    month = month,
    amountMinor = amountMinor,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MonthlyBudget.toEntity(): MonthlyBudgetEntity = MonthlyBudgetEntity(
    id = id,
    year = year,
    month = month,
    amountMinor = amountMinor,
    createdAt = createdAt,
    updatedAt = updatedAt
)
