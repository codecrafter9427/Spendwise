package com.dins.spendwise.domain.model

data class Category(
    val id: Long,
    val name: String,
    val type: TransactionType,
    val icon: String,
    val isDefault: Boolean,
    val remoteId: String? = null
)
