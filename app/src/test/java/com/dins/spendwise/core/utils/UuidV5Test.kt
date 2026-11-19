package com.dins.spendwise.core.utils

import com.dins.spendwise.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class UuidV5Test {
    @Test
    fun defaultCategoryIdsMatchBackendNamespaceFormula() {
        val food = DefaultCategoryRemoteIds.remoteId(TransactionType.EXPENSE, "Food & Dining")
        val salary = DefaultCategoryRemoteIds.remoteId(TransactionType.INCOME, "Salary")
        // Stable UUID5 values for the shared namespace + name keys.
        assertEquals(
            UuidV5.from(
                java.util.UUID.fromString("7c3f9e2a-4b1d-4f6a-9e8c-1a2b3c4d5e6f"),
                "expense:Food & Dining"
            ).toString(),
            food
        )
        assertEquals(
            UuidV5.from(
                java.util.UUID.fromString("7c3f9e2a-4b1d-4f6a-9e8c-1a2b3c4d5e6f"),
                "income:Salary"
            ).toString(),
            salary
        )
    }
}
