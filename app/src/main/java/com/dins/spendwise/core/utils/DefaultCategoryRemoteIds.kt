package com.dins.spendwise.core.utils

import com.dins.spendwise.domain.model.TransactionType
import java.util.UUID

/**
 * Stable UUID5 ids matching backend `app.domain.default_categories`.
 */
object DefaultCategoryRemoteIds {
    private val NAMESPACE = UUID.fromString("7c3f9e2a-4b1d-4f6a-9e8c-1a2b3c4d5e6f")

    fun remoteId(type: TransactionType, name: String): String {
        val apiType = type.name.lowercase()
        return UuidV5.from(NAMESPACE, "$apiType:$name").toString()
    }

    fun remoteIdForLocalSeed(localId: Long, type: TransactionType, name: String): String =
        remoteId(type, name)
}
