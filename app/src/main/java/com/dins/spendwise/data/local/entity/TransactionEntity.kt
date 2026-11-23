package com.dins.spendwise.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["transactionDate"]),
        Index(value = ["categoryId"]),
        Index(value = ["type"]),
        Index(value = ["transactionDate", "type"]),
        Index(value = ["remoteId"], unique = true),
        Index(value = ["deletedAt"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String,
    val amountMinor: Long,
    val categoryId: Long,
    val description: String?,
    val transactionDate: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val remoteId: String? = null,
    val serverVersion: Int = 1,
    val deletedAt: Long? = null
)
