package com.dins.spendwise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dins.spendwise.data.local.entity.OutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: OutboxEntity): Long

    @Update
    suspend fun update(entity: OutboxEntity): Int

    @Query(
        """
        SELECT * FROM outbox_operations
        WHERE status = :status
        ORDER BY createdAt ASC
        LIMIT :limit
        """
    )
    suspend fun getByStatus(status: String, limit: Int): List<OutboxEntity>

    @Query("SELECT * FROM outbox_operations WHERE operationId = :operationId LIMIT 1")
    suspend fun getByOperationId(operationId: String): OutboxEntity?

    @Query("DELETE FROM outbox_operations WHERE operationId = :operationId")
    suspend fun deleteByOperationId(operationId: String): Int

    @Query("SELECT COUNT(*) FROM outbox_operations WHERE status = :status")
    fun observeCountByStatus(status: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM outbox_operations WHERE status = :status")
    suspend fun countByStatus(status: String): Int
}
