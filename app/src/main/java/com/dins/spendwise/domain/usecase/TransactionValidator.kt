package com.dins.spendwise.domain.usecase

import com.dins.spendwise.core.utils.Money
import com.dins.spendwise.domain.model.Category
import com.dins.spendwise.domain.model.TransactionType

data class TransactionFormInput(
    val type: TransactionType,
    val amountText: String,
    val category: Category?,
    val description: String,
    val dateEpochDay: Long?
)

data class TransactionValidationResult(
    val isValid: Boolean,
    val amountMinor: Long? = null,
    val trimmedDescription: String? = null,
    val amountError: String? = null,
    val categoryError: String? = null,
    val descriptionError: String? = null,
    val dateError: String? = null
)

object TransactionValidator {
    const val MAX_DESCRIPTION_LENGTH = 120

    fun validate(input: TransactionFormInput): TransactionValidationResult {
        val amountResult = Money.parseToMinor(input.amountText)
        val amountError = amountResult.exceptionOrNull()?.message
        val amountMinor = amountResult.getOrNull()

        val categoryError = when {
            input.category == null -> "Select a category"
            input.category.type != input.type -> "Category must match the transaction type"
            else -> null
        }

        val trimmed = input.description.trim()
        val descriptionError = when {
            trimmed.length > MAX_DESCRIPTION_LENGTH ->
                "Description must be at most $MAX_DESCRIPTION_LENGTH characters"
            else -> null
        }

        val dateError = if (input.dateEpochDay == null) "Select a date" else null

        val isValid = amountError == null &&
            categoryError == null &&
            descriptionError == null &&
            dateError == null &&
            amountMinor != null

        return TransactionValidationResult(
            isValid = isValid,
            amountMinor = amountMinor,
            trimmedDescription = trimmed.ifEmpty { null },
            amountError = amountError,
            categoryError = categoryError,
            descriptionError = descriptionError,
            dateError = dateError
        )
    }
}
