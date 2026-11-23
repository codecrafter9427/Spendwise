package com.dins.spendwise.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "outbox_operations",
    indices = [
        Index(value = ["operationId"], unique = true),
        Index(value = ["status"]),
        Index(value = ["entityType", "entityId"])
    ]
)
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val operationId: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val baseVersion: Int?,
    val payloadJson: String?,
    val status: String = STATUS_PENDING,
    val attempts: Int = 0,
    val lastError: String? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_CONFLICT = "CONFLICT"
        const val STATUS_REJECTED = "REJECTED"
        const val ENTITY_TRANSACTION = "transaction"
        const val ENTITY_BUDGET = "budget"
        const val OP_UPSERT = "upsert"
        const val OP_DELETE = "delete"
    }
}
