package com.dins.spendwise.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.dins.spendwise.data.local.entity.TransactionEntity
import com.dins.spendwise.data.local.entity.TransactionWithCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Transaction
    @Query(
        """
        SELECT * FROM transactions
        ORDER BY transactionDate DESC, createdAt DESC
        """
    )
    fun observeAllWithCategory(): Flow<List<TransactionWithCategoryEntity>>

    @Transaction
    @Query(
        """
        SELECT * FROM transactions
        WHERE transactionDate BETWEEN :startEpochDay AND :endEpochDay
        ORDER BY transactionDate DESC, createdAt DESC
        """
    )
    fun observeByMonthWithCategory(
        startEpochDay: Long,
        endEpochDay: Long
    ): Flow<List<TransactionWithCategoryEntity>>

    @Transaction
    @Query(
        """
        SELECT * FROM transactions
        WHERE transactionDate BETWEEN :startEpochDay AND :endEpochDay
        ORDER BY transactionDate DESC, createdAt DESC
        LIMIT :limit
        """
    )
    fun observeRecentByMonthWithCategory(
        startEpochDay: Long,
        endEpochDay: Long,
        limit: Int
    ): Flow<List<TransactionWithCategoryEntity>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun observeByIdWithCategory(id: Long): Flow<TransactionWithCategoryEntity?>

    @Query(
        """
        SELECT COALESCE(SUM(amountMinor), 0) FROM transactions
        WHERE type = :type
          AND transactionDate BETWEEN :startEpochDay AND :endEpochDay
        """
    )
    fun observeSumByTypeAndMonth(
        type: String,
        startEpochDay: Long,
        endEpochDay: Long
    ): Flow<Long>

    @Insert
    suspend fun insert(entity: TransactionEntity): Long

    @Update
    suspend fun update(entity: TransactionEntity): Int

    @Delete
    suspend fun delete(entity: TransactionEntity): Int

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?
}
