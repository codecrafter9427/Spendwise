package com.dins.spendwise.data.sync

import com.dins.spendwise.data.local.dao.OutboxDao
import com.dins.spendwise.data.local.entity.OutboxEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutboxWriter @Inject constructor(
    private val outboxDao: OutboxDao,
    private val moshi: Moshi,
    private val syncScheduler: SyncScheduler
) {
    private val mapAdapter = moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )

    suspend fun enqueueTransactionUpsert(
        remoteId: String,
        baseVersion: Int?,
        payload: Map<String, Any?>
    ) {
        enqueue(
            entityType = OutboxEntity.ENTITY_TRANSACTION,
            entityId = remoteId,
            operation = OutboxEntity.OP_UPSERT,
            baseVersion = baseVersion,
            payload = payload
        )
    }

    suspend fun enqueueTransactionDelete(
        remoteId: String,
        baseVersion: Int
    ) {
        enqueue(
            entityType = OutboxEntity.ENTITY_TRANSACTION,
            entityId = remoteId,
            operation = OutboxEntity.OP_DELETE,
            baseVersion = baseVersion,
            payload = null
        )
    }

    suspend fun enqueueBudgetUpsert(
        remoteId: String,
        baseVersion: Int?,
        payload: Map<String, Any?>
    ) {
        enqueue(
            entityType = OutboxEntity.ENTITY_BUDGET,
            entityId = remoteId,
            operation = OutboxEntity.OP_UPSERT,
            baseVersion = baseVersion,
            payload = payload
        )
    }

    private suspend fun enqueue(
        entityType: String,
        entityId: String,
        operation: String,
        baseVersion: Int?,
        payload: Map<String, Any?>?
    ) {
        val now = System.currentTimeMillis()
        outboxDao.insert(
            OutboxEntity(
                operationId = UUID.randomUUID().toString(),
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                baseVersion = baseVersion,
                payloadJson = payload?.let { mapAdapter.toJson(it) },
                status = OutboxEntity.STATUS_PENDING,
                createdAt = now,
                updatedAt = now
            )
        )
        syncScheduler.enqueueSync()
    }
}
