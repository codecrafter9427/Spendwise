package com.dins.spendwise.data.sync

import com.dins.spendwise.data.local.entity.OutboxEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class OutboxEntityTest {
    @Test
    fun statusAndEntityConstantsAreStable() {
        assertEquals("PENDING", OutboxEntity.STATUS_PENDING)
        assertEquals("CONFLICT", OutboxEntity.STATUS_CONFLICT)
        assertEquals("transaction", OutboxEntity.ENTITY_TRANSACTION)
        assertEquals("budget", OutboxEntity.ENTITY_BUDGET)
        assertEquals("upsert", OutboxEntity.OP_UPSERT)
        assertEquals("delete", OutboxEntity.OP_DELETE)
    }
}
