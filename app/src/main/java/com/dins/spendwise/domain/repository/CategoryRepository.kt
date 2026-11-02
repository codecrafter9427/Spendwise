package com.dins.spendwise.domain.repository

import com.dins.spendwise.domain.model.Category
import com.dins.spendwise.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
    fun observeByType(type: TransactionType): Flow<List<Category>>
    suspend fun getById(id: Long): Category?
}
