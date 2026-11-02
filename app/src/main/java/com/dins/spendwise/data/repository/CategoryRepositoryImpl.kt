package com.dins.spendwise.data.repository

import com.dins.spendwise.data.local.dao.CategoryDao
import com.dins.spendwise.data.mapper.toDomain
import com.dins.spendwise.domain.model.Category
import com.dins.spendwise.domain.model.TransactionType
import com.dins.spendwise.domain.repository.CategoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {
    override fun observeAll(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeByType(type: TransactionType): Flow<List<Category>> =
        categoryDao.observeByType(type.name).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Category? =
        categoryDao.getById(id)?.toDomain()
}
