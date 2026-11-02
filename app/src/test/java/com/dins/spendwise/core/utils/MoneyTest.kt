package com.dins.spendwise.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    @Test
    fun parseValidAmountToPaise() {
        assertEquals(1050L, Money.parseToMinor("10.50").getOrThrow())
        assertEquals(10000L, Money.parseToMinor("100").getOrThrow())
        assertEquals(1L, Money.parseToMinor("0.01").getOrThrow())
    }

    @Test
    fun rejectBlankZeroNegativeMalformedAndOverflow() {
        assertTrue(Money.parseToMinor("").isFailure)
        assertTrue(Money.parseToMinor("0").isFailure)
        assertTrue(Money.parseToMinor("-5").isFailure)
        assertTrue(Money.parseToMinor("12.345").isFailure)
        assertTrue(Money.parseToMinor("abc").isFailure)
        assertTrue(Money.parseToMinor("1000000000").isFailure)
    }

    @Test
    fun formatAndRoundTripInput() {
        val formatted = Money.formatInr(123456L)
        assertTrue(formatted.contains("1,234.56") || formatted.contains("1234.56"))
        assertEquals("12.5", Money.minorToInputString(1250L))
        assertEquals("10", Money.minorToInputString(1000L))
    }

    @Test
    fun formatNegativeNet() {
        val formatted = Money.formatInr(-500L)
        assertTrue(formatted.startsWith("-"))
    }
}
