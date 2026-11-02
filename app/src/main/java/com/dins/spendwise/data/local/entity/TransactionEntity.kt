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
        Index(value = ["transactionDate", "type"])
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
    val updatedAt: Long
)
