package com.dins.spendwise.domain.usecase

import com.dins.spendwise.domain.model.Category
import com.dins.spendwise.domain.model.TransactionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionValidatorTest {

    private val expenseCategory = Category(1, "Food", TransactionType.EXPENSE, "restaurant", true)
    private val incomeCategory = Category(101, "Salary", TransactionType.INCOME, "work", true)

    @Test
    fun acceptsValidExpense() {
        val result = TransactionValidator.validate(
            TransactionFormInput(
                type = TransactionType.EXPENSE,
                amountText = "120.50",
                category = expenseCategory,
                description = "  Lunch  ",
                dateEpochDay = 20_000L
            )
        )
        assertTrue(result.isValid)
        assertEqualsAmount(12050L, result.amountMinor)
        assertEqualsDesc("Lunch", result.trimmedDescription)
    }

    @Test
    fun rejectsMismatchedCategoryAndInvalidAmount() {
        val mismatch = TransactionValidator.validate(
            TransactionFormInput(
                type = TransactionType.EXPENSE,
                amountText = "10",
                category = incomeCategory,
                description = "",
                dateEpochDay = 1L
            )
        )
        assertFalse(mismatch.isValid)
        assertTrue(mismatch.categoryError != null)

        val invalid = TransactionValidator.validate(
            TransactionFormInput(
                type = TransactionType.INCOME,
                amountText = "0",
                category = incomeCategory,
                description = "x".repeat(121),
                dateEpochDay = null
            )
        )
        assertFalse(invalid.isValid)
        assertTrue(invalid.amountError != null)
        assertTrue(invalid.descriptionError != null)
        assertTrue(invalid.dateError != null)
    }

    @Test
    fun optionalEmptyDescriptionBecomesNull() {
        val result = TransactionValidator.validate(
            TransactionFormInput(
                type = TransactionType.INCOME,
                amountText = "1",
                category = incomeCategory,
                description = "   ",
                dateEpochDay = 1L
            )
        )
        assertTrue(result.isValid)
        assertNull(result.trimmedDescription)
    }

    private fun assertEqualsAmount(expected: Long, actual: Long?) {
        org.junit.Assert.assertEquals(expected, actual)
    }

    private fun assertEqualsDesc(expected: String?, actual: String?) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
