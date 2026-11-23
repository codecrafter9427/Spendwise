package com.dins.spendwise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dins.spendwise.data.local.entity.MonthlyBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyBudgetDao {
    @Query("SELECT * FROM monthly_budgets WHERE year = :year AND month = :month LIMIT 1")
    fun observeByYearMonth(year: Int, month: Int): Flow<MonthlyBudgetEntity?>

    @Query("SELECT * FROM monthly_budgets WHERE year = :year AND month = :month LIMIT 1")
    suspend fun getByYearMonth(year: Int, month: Int): MonthlyBudgetEntity?

    @Query("SELECT * FROM monthly_budgets WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): MonthlyBudgetEntity?

    @Query("SELECT * FROM monthly_budgets ORDER BY year DESC, month DESC")
    suspend fun getAll(): List<MonthlyBudgetEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: MonthlyBudgetEntity): Long

    @Update
    suspend fun update(entity: MonthlyBudgetEntity): Int
}
