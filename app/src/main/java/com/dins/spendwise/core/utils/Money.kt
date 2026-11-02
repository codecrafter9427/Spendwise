package com.dins.spendwise.core.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Money is stored as integer paise (minor units). Max supported amount is
 * ₹99,99,99,999.99 → [MAX_AMOUNT_MINOR] paise.
 */
object Money {
    /** ₹99,99,99,999.99 in paise */
    const val MAX_AMOUNT_MINOR: Long = 99_999_999_999L
    private const val SCALE = 2
    private val HUNDRED = BigDecimal(100)

    fun parseToMinor(input: String): Result<Long> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Amount is required"))
        }
        if (!trimmed.matches(Regex("""^\d+(\.\d{0,2})?$"""))) {
            return Result.failure(IllegalArgumentException("Enter a valid amount with up to 2 decimal places"))
        }
        return try {
            val value = BigDecimal(trimmed).setScale(SCALE, RoundingMode.UNNECESSARY)
            if (value.signum() <= 0) {
                return Result.failure(IllegalArgumentException("Amount must be greater than zero"))
            }
            val minor = value.multiply(HUNDRED).longValueExact()
            if (minor > MAX_AMOUNT_MINOR) {
                return Result.failure(IllegalArgumentException("Amount exceeds the supported maximum"))
            }
            Result.success(minor)
        } catch (_: ArithmeticException) {
            Result.failure(IllegalArgumentException("Enter a valid amount with up to 2 decimal places"))
        } catch (_: NumberFormatException) {
            Result.failure(IllegalArgumentException("Enter a valid amount"))
        }
    }

    fun formatInr(amountMinor: Long, withSign: Boolean = false): String {
        val absMinor = kotlin.math.abs(amountMinor)
        val rupees = BigDecimal(absMinor).divide(HUNDRED, SCALE, RoundingMode.UNNECESSARY)
        val formatter = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()).apply {
            currency = Currency.getInstance("INR")
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
        val formatted = formatter.format(rupees)
        return when {
            withSign && amountMinor > 0 -> "+$formatted"
            amountMinor < 0 -> "-$formatted"
            else -> formatted
        }
    }

    fun minorToInputString(amountMinor: Long): String {
        val value = BigDecimal(amountMinor).divide(HUNDRED, SCALE, RoundingMode.UNNECESSARY)
        return value.stripTrailingZeros().toPlainString()
    }
}
