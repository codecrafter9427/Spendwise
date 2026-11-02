package com.dins.spendwise.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["type"])]
)
data class CategoryEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val type: String,
    val icon: String,
    val isDefault: Boolean
)
