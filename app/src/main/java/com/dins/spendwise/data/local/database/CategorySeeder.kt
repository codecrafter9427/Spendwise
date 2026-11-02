package com.dins.spendwise.data.local.database

import com.dins.spendwise.data.local.entity.CategoryEntity
import com.dins.spendwise.domain.model.TransactionType

object CategorySeeder {
    fun defaultCategories(): List<CategoryEntity> {
        val expense = listOf(
            CategoryEntity(1, "Food & Dining", TransactionType.EXPENSE.name, "restaurant", true),
            CategoryEntity(2, "Transportation", TransactionType.EXPENSE.name, "directions_car", true),
            CategoryEntity(3, "Shopping", TransactionType.EXPENSE.name, "shopping_bag", true),
            CategoryEntity(4, "Bills & Utilities", TransactionType.EXPENSE.name, "receipt", true),
            CategoryEntity(5, "Health", TransactionType.EXPENSE.name, "favorite", true),
            CategoryEntity(6, "Entertainment", TransactionType.EXPENSE.name, "sports_esports", true),
            CategoryEntity(7, "Education", TransactionType.EXPENSE.name, "school", true),
            CategoryEntity(8, "Travel", TransactionType.EXPENSE.name, "flight", true),
            CategoryEntity(9, "Groceries", TransactionType.EXPENSE.name, "shopping_basket", true),
            CategoryEntity(10, "Housing", TransactionType.EXPENSE.name, "home", true),
            CategoryEntity(11, "Personal Care", TransactionType.EXPENSE.name, "person", true),
            CategoryEntity(12, "Other", TransactionType.EXPENSE.name, "more_horiz", true)
        )
        val income = listOf(
            CategoryEntity(101, "Salary", TransactionType.INCOME.name, "work", true),
            CategoryEntity(102, "Freelance", TransactionType.INCOME.name, "laptop", true),
            CategoryEntity(103, "Business", TransactionType.INCOME.name, "business", true),
            CategoryEntity(104, "Investment Returns", TransactionType.INCOME.name, "trending_up", true),
            CategoryEntity(105, "Gift", TransactionType.INCOME.name, "card_giftcard", true),
            CategoryEntity(106, "Other Income", TransactionType.INCOME.name, "account_balance_wallet", true)
        )
        return expense + income
    }
}
