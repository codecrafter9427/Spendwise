package com.dins.spendwise.data.local.database

import com.dins.spendwise.core.utils.DefaultCategoryRemoteIds
import com.dins.spendwise.data.local.entity.CategoryEntity
import com.dins.spendwise.domain.model.TransactionType

object CategorySeeder {
    fun defaultCategories(): List<CategoryEntity> {
        val expense = listOf(
            seed(1, "Food & Dining", TransactionType.EXPENSE, "restaurant"),
            seed(2, "Transportation", TransactionType.EXPENSE, "directions_car"),
            seed(3, "Shopping", TransactionType.EXPENSE, "shopping_bag"),
            seed(4, "Bills & Utilities", TransactionType.EXPENSE, "receipt"),
            seed(5, "Health", TransactionType.EXPENSE, "favorite"),
            seed(6, "Entertainment", TransactionType.EXPENSE, "sports_esports"),
            seed(7, "Education", TransactionType.EXPENSE, "school"),
            seed(8, "Travel", TransactionType.EXPENSE, "flight"),
            seed(9, "Groceries", TransactionType.EXPENSE, "shopping_basket"),
            seed(10, "Housing", TransactionType.EXPENSE, "home"),
            seed(11, "Personal Care", TransactionType.EXPENSE, "person"),
            seed(12, "Other", TransactionType.EXPENSE, "more_horiz")
        )
        val income = listOf(
            seed(101, "Salary", TransactionType.INCOME, "work"),
            seed(102, "Freelance", TransactionType.INCOME, "laptop"),
            seed(103, "Business", TransactionType.INCOME, "business"),
            seed(104, "Investment Returns", TransactionType.INCOME, "trending_up"),
            seed(105, "Gift", TransactionType.INCOME, "card_giftcard"),
            seed(106, "Other Income", TransactionType.INCOME, "account_balance_wallet")
        )
        return expense + income
    }

    private fun seed(
        id: Long,
        name: String,
        type: TransactionType,
        icon: String
    ): CategoryEntity = CategoryEntity(
        id = id,
        name = name,
        type = type.name,
        icon = icon,
        isDefault = true,
        remoteId = DefaultCategoryRemoteIds.remoteId(type, name)
    )
}
