package com.dins.spendwise.core.utils

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val displayDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

    fun yearMonthOf(date: LocalDate): YearMonth = YearMonth.from(date)

    fun startEpochDay(yearMonth: YearMonth): Long = yearMonth.atDay(1).toEpochDay()

    fun endEpochDayInclusive(yearMonth: YearMonth): Long = yearMonth.atEndOfMonth().toEpochDay()

    fun formatMonthYear(yearMonth: YearMonth): String = yearMonth.format(monthYearFormatter)

    fun formatDisplayDate(date: LocalDate): String = date.format(displayDateFormatter)

    fun isInMonth(date: LocalDate, yearMonth: YearMonth): Boolean =
        YearMonth.from(date) == yearMonth
}
